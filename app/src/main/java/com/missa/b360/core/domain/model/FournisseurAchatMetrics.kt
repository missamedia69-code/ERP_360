package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus

/** Agrégats achats affichés dans le hub et la fiche fournisseur. */
data class FournisseurAchatSummary(
    val montantAchete: Double = 0.0,
    val solde: Double = 0.0,
    val nombreCommandes: Int = 0,
    val derniereFacture: Long? = null,
)

/** Facture d'achat validée dont il reste un montant à régler. */
data class FournisseurFactureOuverte(
    val recordId: Long,
    val reference: String,
    val fournisseurId: Long,
    val issuedAt: Long,
    val dueAt: Long,
    val total: Double,
    val outstanding: Double,
)

/** Dette d'un fournisseur (ou de tous) : source unique des montants dus, en retard et à venir. */
data class FournisseurDette(
    val dette: Double = 0.0,
    val enRetard: Double = 0.0,
    val joursRetardMax: Int = 0,
    val nbFacturesOuvertes: Int = 0,
    val prochaineEcheanceAt: Long? = null,
    val balance: AgedBalance = AgedBalanceRules.calculer(emptyList<ClientOpenInvoice>(), 0L),
    val factures: List<FournisseurFactureOuverte> = emptyList(),
)

/** Nature d'une pièce du module Achats, déduite de son contenu typé et jamais de sa référence. */
enum class PieceAchatNature { FACTURE, RECEPTION, COMMANDE, INCONNUE }

/**
 * Calculs basés sur le contenu typé des pièces, jamais sur leur référence lisible
 * (dont les préfixes varient selon le type de pièce et la séquence configurée).
 */
object FournisseurAchatMetrics {

    const val DAY_MS: Long = 86_400_000L
    private const val EPSILON = 1e-9

    private fun piecesAchatsValidees(pieces: List<OperationRecordEntity>): List<OperationRecordEntity> =
        pieces.filter {
            it.module == OperationModule.ACHATS.name && it.status == OperationStatus.VALIDATED.name
        }

    /**
     * Nature de la pièce. Les trois contenus se décodent avec `ignoreUnknownKeys` : une facture
     * contient tous les champs obligatoires d'une commande. L'ordre facture → réception → commande
     * (identique à celui de l'écran Achats) lève donc l'ambiguïté.
     */
    fun nature(piece: OperationRecordEntity): PieceAchatNature = when {
        PurchaseRecordCodec.decode(piece.notes) != null -> PieceAchatNature.FACTURE
        ReceptionCodec.decode(piece.notes) != null -> PieceAchatNature.RECEPTION
        CommandeAchatCodec.decode(piece.notes) != null -> PieceAchatNature.COMMANDE
        else -> PieceAchatNature.INCONNUE
    }

    /**
     * Reste à régler d'une facture : `max(total − réglé, 0)`. Un total invalide (NaN, infini, ≤ 0)
     * donne 0, un règlement invalide (NaN, négatif) est ignoré, un règlement supérieur au total
     * est plafonné. **Seule définition de la dette fournisseur.**
     */
    fun resteDu(facture: PurchaseRecordPayload): Double {
        val total = facture.total
        if (!total.isFinite() || total <= 0.0) return 0.0
        val regle = facture.paidAmount.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        return (total - regle).coerceAtLeast(0.0)
    }

    /** Échéance : celle saisie sur la facture, sinon date de la pièce + délai du fournisseur. */
    fun echeance(piece: OperationRecordEntity, facture: PurchaseRecordPayload, joursEcheance: Int): Long =
        facture.dateEcheance
            ?: runCatching {
                Math.addExact(piece.createdAt, joursEcheance.coerceAtLeast(0).toLong() * DAY_MS)
            }.getOrDefault(Long.MAX_VALUE)

    /**
     * Factures ouvertes (pièce ACHATS validée, contenu de facture, reste > 0), triées par échéance
     * puis par date. [fournisseurId] filtre sur un fournisseur ; [joursEcheance] donne le délai de
     * règlement du fournisseur d'une facture sans échéance saisie.
     */
    fun facturesOuvertes(
        pieces: List<OperationRecordEntity>,
        fournisseurId: Long? = null,
        joursEcheance: (Long) -> Int = { 0 },
    ): List<FournisseurFactureOuverte> =
        piecesAchatsValidees(pieces).mapNotNull { piece ->
            val facture = PurchaseRecordCodec.decode(piece.notes) ?: return@mapNotNull null
            if (fournisseurId != null && facture.supplierId != fournisseurId) return@mapNotNull null
            val reste = resteDu(facture)
            if (reste <= EPSILON) return@mapNotNull null
            FournisseurFactureOuverte(
                recordId = piece.id,
                reference = piece.reference,
                fournisseurId = facture.supplierId,
                issuedAt = piece.createdAt,
                dueAt = echeance(piece, facture, joursEcheance(facture.supplierId)),
                total = facture.total,
                outstanding = reste,
            )
        }.sortedWith(compareBy({ it.dueAt }, { it.issuedAt }, { it.recordId }))

    /** Balance âgée d'une liste de factures ouvertes (mêmes tranches que le module Clients). */
    fun balanceAgee(factures: List<FournisseurFactureOuverte>, now: Long): AgedBalance =
        AgedBalanceRules.calculer(
            factures.map {
                ClientOpenInvoice(
                    recordId = it.recordId,
                    issuedAt = it.issuedAt,
                    dueAt = it.dueAt,
                    total = it.total,
                    outstanding = it.outstanding,
                )
            },
            now,
        )

    /** Dette complète (ou d'un seul fournisseur) à l'instant [now]. */
    fun dette(
        pieces: List<OperationRecordEntity>,
        now: Long,
        fournisseurId: Long? = null,
        joursEcheance: (Long) -> Int = { 0 },
    ): FournisseurDette {
        val factures = facturesOuvertes(pieces, fournisseurId, joursEcheance)
        val balance = balanceAgee(factures, now)
        return FournisseurDette(
            dette = balance.total,
            enRetard = balance.enRetard,
            joursRetardMax = factures.maxOfOrNull { AgedBalanceRules.joursDeRetard(it.dueAt, now) } ?: 0,
            nbFacturesOuvertes = factures.size,
            prochaineEcheanceAt = factures.map { it.dueAt }.filter { it >= now }.minOrNull(),
            balance = balance,
            factures = factures,
        )
    }

    fun soldeTotal(pieces: List<OperationRecordEntity>): Double =
        piecesAchatsValidees(pieces).sumOf { piece ->
            val facture = PurchaseRecordCodec.decode(piece.notes) ?: return@sumOf 0.0
            resteDu(facture)
        }

    fun commandesOuvertes(pieces: List<OperationRecordEntity>): Int {
        val validees = piecesAchatsValidees(pieces)
        val receptionsParCommande = validees.mapNotNull { piece ->
            val reception = ReceptionCodec.decode(piece.notes) ?: return@mapNotNull null
            val commandeId = reception.commandeRecordId ?: return@mapNotNull null
            commandeId to reception
        }.groupBy({ it.first }, { it.second })

        return validees.count { piece ->
            if (nature(piece) != PieceAchatNature.COMMANDE) return@count false
            val commande = CommandeAchatCodec.decode(piece.notes) ?: return@count false
            val commandees = commande.lines
                .filter { it.productId != null && it.quantity > 0.0 && it.quantity.isFinite() }
                .groupBy { it.productId!! }
                .mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
            val receptions = receptionsParCommande[piece.id].orEmpty()
            val recues = receptions.flatMap { it.lignes }
                .filter { it.quantiteRecue > 0.0 && it.quantiteRecue.isFinite() }
                .groupBy { it.productId }
                .mapValues { (_, lignes) -> lignes.sumOf { it.quantiteRecue } }

            // Les commandes de lignes libres ne disposent pas d'identifiants produit ;
            // faute de quantités rapprochables, elles restent ouvertes jusqu'à réception.
            if (commandees.isEmpty()) receptions.isEmpty()
            else !AchatCommandeRules.commandeSoldee(commandees, recues)
        }
    }

    fun pourFournisseur(fournisseurId: Long, pieces: List<OperationRecordEntity>): FournisseurAchatSummary {
        val validees = piecesAchatsValidees(pieces)
        val factures = validees.mapNotNull { piece ->
            val payload = PurchaseRecordCodec.decode(piece.notes) ?: return@mapNotNull null
            if (payload.supplierId != fournisseurId) return@mapNotNull null
            piece to payload
        }
        val commandes = validees.count { piece ->
            nature(piece) == PieceAchatNature.COMMANDE &&
                CommandeAchatCodec.decode(piece.notes)?.supplierId == fournisseurId
        }
        return FournisseurAchatSummary(
            montantAchete = factures.sumOf { (_, facture) -> facture.total.takeIf { it.isFinite() } ?: 0.0 },
            solde = factures.sumOf { (_, facture) -> resteDu(facture) },
            nombreCommandes = commandes,
            derniereFacture = factures.maxOfOrNull { (piece, _) -> piece.createdAt },
        )
    }
}
