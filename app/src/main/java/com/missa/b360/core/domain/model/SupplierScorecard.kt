package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SupplierScoreStatut { SIGNIFICATIF, NON_SIGNIFICATIF }

/**
 * Fiabilité **mesurée** d'un fournisseur sur ses réceptions. Chaque pilier vaut 0–100 ou `null`
 * s'il n'est pas mesurable ; [score] est `null` sous [SupplierScorecard.MIN_COMMANDES_MESUREES]
 * commandes mesurées.
 */
data class SupplierScore(
    val statut: SupplierScoreStatut = SupplierScoreStatut.NON_SIGNIFICATIF,
    val ponctualite: Double? = null,
    val conformite: Double? = null,
    val prix: Double? = null,
    val score: Int? = null,
    val nbCommandesMesurees: Int = 0,
    /** Moyenne des jours entre la commande et sa réception complète. */
    val delaiMoyenReelJours: Double? = null,
)

object SupplierScorecard {

    const val FENETRE_JOURS = 365
    const val MIN_COMMANDES_MESUREES = 3
    const val TOLERANCE_QUANTITE = 0.02
    private const val JOUR_MS = 86_400_000L
    private const val EPS = 1e-9
    private const val POIDS_PONCTUALITE = 0.40
    private const val POIDS_CONFORMITE = 0.40
    private const val POIDS_PRIX = 0.20
    private const val FACTEUR_PENALITE_PRIX = 500.0

    private class Mesure(
        val commande: OperationRecordEntity,
        val payload: CommandeAchatPayload,
        val commandeParProduit: Map<Long, Double>,
        val receptions: List<Pair<OperationRecordEntity, ReceptionPayload>>,
        val recueParProduit: Map<Long, Double>,
        val receptionCompleteLe: Long,
    )

    fun calculer(
        fournisseurId: Long,
        pieces: List<OperationRecordEntity>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): SupplierScore {
        val validees = pieces.filter {
            it.module == OperationModule.ACHATS.name && it.status == OperationStatus.VALIDATED.name
        }
        val receptions = validees.mapNotNull { piece ->
            if (FournisseurAchatMetrics.nature(piece) != PieceAchatNature.RECEPTION) return@mapNotNull null
            val reception = ReceptionCodec.decode(piece.notes) ?: return@mapNotNull null
            piece to reception
        }.groupBy { (_, r) -> r.commandeRecordId }

        val debutFenetre = now - FENETRE_JOURS * JOUR_MS
        val mesures = validees.mapNotNull { piece ->
            if (FournisseurAchatMetrics.nature(piece) != PieceAchatNature.COMMANDE) return@mapNotNull null
            val commande = CommandeAchatCodec.decode(piece.notes) ?: return@mapNotNull null
            if (commande.supplierId != fournisseurId) return@mapNotNull null
            mesurer(piece, commande, receptions[piece.id].orEmpty())
        }.filter { it.receptionCompleteLe >= debutFenetre }

        val nb = mesures.size
        if (nb == 0) return SupplierScore()

        val ponctualite = ponctualite(mesures, zone)
        val conformite = conformite(mesures)
        val prix = prix(mesures)
        val delai = mesures.map { (it.receptionCompleteLe - it.commande.createdAt).coerceAtLeast(0L).toDouble() / JOUR_MS }
            .average()

        val piliers = listOf(
            ponctualite to POIDS_PONCTUALITE,
            conformite to POIDS_CONFORMITE,
            prix to POIDS_PRIX,
        ).filter { it.first != null }
        val significatif = nb >= MIN_COMMANDES_MESUREES && piliers.isNotEmpty()
        val score = if (significatif) {
            val poids = piliers.sumOf { it.second }
            Math.round(piliers.sumOf { (it.first ?: 0.0) * it.second } / poids).toInt().coerceIn(0, 100)
        } else {
            null
        }
        return SupplierScore(
            statut = if (significatif) SupplierScoreStatut.SIGNIFICATIF else SupplierScoreStatut.NON_SIGNIFICATIF,
            ponctualite = ponctualite,
            conformite = conformite,
            prix = prix,
            score = score,
            nbCommandesMesurees = nb,
            delaiMoyenReelJours = delai,
        )
    }

    /** Une commande n'est mesurée que si elle est entièrement reçue ; sinon `null`. */
    private fun mesurer(
        piece: OperationRecordEntity,
        commande: CommandeAchatPayload,
        receptions: List<Pair<OperationRecordEntity, ReceptionPayload>>,
    ): Mesure? {
        val commandees = commande.lines
            .filter { it.productId != null && it.quantity > 0.0 && it.quantity.isFinite() }
            .groupBy { it.productId!! }
            .mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
        if (commandees.isEmpty()) return null

        val ordonnees = receptions.sortedWith(compareBy({ it.first.createdAt }, { it.first.id }))
        val cumul = mutableMapOf<Long, Double>()
        var completeLe: Long? = null
        for ((p, r) in ordonnees) {
            r.lignes.filter { it.quantiteRecue > 0.0 && it.quantiteRecue.isFinite() }
                .forEach { cumul[it.productId] = (cumul[it.productId] ?: 0.0) + it.quantiteRecue }
            if (completeLe == null && AchatCommandeRules.commandeSoldee(commandees, cumul)) completeLe = p.createdAt
        }
        val date = completeLe ?: return null
        return Mesure(piece, commande, commandees, ordonnees, cumul.toMap(), date)
    }

    private fun ponctualite(mesures: List<Mesure>, zone: ZoneId): Double? {
        val avecDate = mesures.filter { it.payload.dateLivraisonPrevue != null }
        if (avecDate.isEmpty()) return null
        val aTemps = avecDate.count { m ->
            val prevue = m.payload.dateLivraisonPrevue ?: return@count false
            val finDeJour = Instant.ofEpochMilli(prevue).atZone(zone).toLocalDate().plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            m.receptionCompleteLe < finDeJour
        }
        return 100.0 * aTemps / avecDate.size
    }

    private fun conformite(mesures: List<Mesure>): Double? {
        var total = 0
        var conformes = 0
        mesures.forEach { m ->
            m.commandeParProduit.forEach { (produit, commandee) ->
                total++
                val recue = m.recueParProduit[produit] ?: 0.0
                if (abs(recue - commandee) <= TOLERANCE_QUANTITE * commandee + EPS) conformes++
            }
        }
        return if (total == 0) null else 100.0 * conformes / total
    }

    private fun prix(mesures: List<Mesure>): Double? {
        var poids = 0.0
        var penaliteTotale = 0.0
        mesures.forEach { m ->
            // Prix commandé moyen pondéré par produit.
            val prixCommande = m.payload.lines
                .filter { it.productId != null && it.quantity > 0.0 && it.quantity.isFinite() && it.unitPrice.isFinite() }
                .groupBy { it.productId!! }
                .mapValues { (_, lignes) -> lignes.sumOf { it.unitPrice * it.quantity } / lignes.sumOf { it.quantity } }
            m.receptions.forEach { (_, r) ->
                r.lignes.forEach ligne@{ ligne ->
                    val reel = ligne.prixReel ?: return@ligne
                    val commande = prixCommande[ligne.productId] ?: return@ligne
                    if (!reel.isFinite() || reel <= 0.0 || commande <= 0.0) return@ligne
                    if (!ligne.quantiteRecue.isFinite() || ligne.quantiteRecue <= 0.0) return@ligne
                    val ecart = (reel - commande) / commande
                    val penalite = min(100.0, max(ecart, 0.0) * FACTEUR_PENALITE_PRIX)
                    val montant = reel * ligne.quantiteRecue
                    penaliteTotale += penalite * montant
                    poids += montant
                }
            }
        }
        return if (poids <= 0.0) null else 100.0 - penaliteTotale / poids
    }
}
