package com.missa.b360.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Chaîne d'achat en trois temps (spec §6) :
 *
 * 1. **Bon de commande** — ACHATS décide et documente (BC-…) ;
 * 2. **Bon de réception** — STOCK réceptionne et valorise les biens physiques (BR-…) ;
 * 3. **Facture fournisseur** — COMPTABILITÉ enregistre la dette et la TVA,
 *    TRÉSORERIE règle (FFR-…).
 *
 * Chaque pièce est un [com.missa.b360.core.data.entity.OperationRecordEntity] du
 * module ACHATS dont le détail est encodé ici, comme les ventes.
 */

/** Ligne d'un bon de commande fournisseur. */
@Serializable
data class CommandeAchatLigne(
    val id: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    /** Produit du catalogue rattaché — null pour une ligne libre. */
    val productId: Long? = null,
) {
    val total: Double get() = unitPrice * quantity
}

/** Détail d'un bon de commande fournisseur. */
@Serializable
data class CommandeAchatPayload(
    val schemaVersion: Int = 1,
    val supplierId: Long,
    val supplierName: String,
    val lines: List<CommandeAchatLigne>,
    val note: String? = null,
)

object CommandeAchatCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: CommandeAchatPayload): String =
        json.encodeToString(CommandeAchatPayload.serializer(), payload)

    fun decode(value: String?): CommandeAchatPayload? = value?.let {
        runCatching { json.decodeFromString(CommandeAchatPayload.serializer(), it) }.getOrNull()
    }
}

/** Ligne réceptionnée : quantité physique + traçabilité lot/série/péremption. */
@Serializable
data class ReceptionLigne(
    val productId: Long,
    val name: String,
    /** Quantité commandée — repère visuel du contrôle (0 si réception libre). */
    val quantiteCommandee: Double = 0.0,
    val quantiteRecue: Double,
    val lot: String? = null,
    val numeroSerie: String? = null,
    val datePeremption: Long? = null,
)

/** Détail d'un bon de réception, rattaché ou non à un bon de commande. */
@Serializable
data class ReceptionPayload(
    val schemaVersion: Int = 1,
    val commandeRecordId: Long? = null,
    val commandeReference: String? = null,
    val supplierId: Long,
    val supplierName: String,
    val lignes: List<ReceptionLigne>,
    val note: String? = null,
)

object ReceptionCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: ReceptionPayload): String =
        json.encodeToString(ReceptionPayload.serializer(), payload)

    fun decode(value: String?): ReceptionPayload? = value?.let {
        runCatching { json.decodeFromString(ReceptionPayload.serializer(), it) }.getOrNull()
    }
}

/**
 * Garde-fous purs de la chaîne commande → réception → facture → règlement →
 * annulation. Testables sans base.
 */
object AchatCommandeRules {

    /** Quantité encore réceptionnable d'une ligne commandée. */
    fun restantARecevoir(commandee: Double, dejaRecue: Double): Double =
        (commandee - dejaRecue).coerceAtLeast(0.0)

    /**
     * Une réception est valide si chaque ligne rattachée à une commande ne dépasse
     * pas le reste à recevoir (tolérance 1e-9), et si au moins une quantité est positive.
     */
    fun receptionEstValide(
        commandeeParProduit: Map<Long, Double>,
        dejaRecuParProduit: Map<Long, Double>,
        reception: List<ReceptionLigne>,
    ): Boolean {
        if (reception.none { it.quantiteRecue > 0.0 }) return false
        val cumule = reception
            .filter { it.quantiteRecue > 0.0 }
            .groupBy { it.productId }
            .mapValues { (_, l) -> l.sumOf { it.quantiteRecue } }
        return cumule.all { (produitId, quantite) ->
            val commandee = commandeeParProduit[produitId]
            // Ligne libre (hors commande) : toujours acceptable.
            if (commandee == null) return@all quantite.isFinite()
            val reste = restantARecevoir(commandee, dejaRecuParProduit[produitId] ?: 0.0)
            quantite <= reste + 1e-9
        }
    }

    /** Montant encore à régler d'une facture. */
    fun restantARegler(total: Double, dejaRegle: Double): Double =
        (total - dejaRegle).coerceAtLeast(0.0)

    /** Un règlement est valide s'il est positif et ne dépasse pas le reste dû. */
    fun reglementEstValide(total: Double, dejaRegle: Double, montant: Double): Boolean =
        montant.isFinite() && montant > 0.0 &&
            montant <= restantARegler(total, dejaRegle) + 0.005

    /**
     * Référence unique d'un mouvement de trésorerie : la référence de la pièce pour
     * le premier décaissement (compatible avec l'existant), `-R2`, `-R3`… pour les
     * règlements suivants, `-ANN` pour l'annulation. Jamais deux mouvements avec la
     * même référence ⇒ jamais deux fois le même règlement.
     */
    fun referenceReglement(referenceFacture: String, numeroReglement: Int): String =
        if (numeroReglement <= 1) referenceFacture.trim()
        else "${referenceFacture.trim()}-R$numeroReglement"

    fun referenceAnnulation(reference: String): String = "${reference.trim()}-ANN"

    /** Une commande est soldée quand tout le commandé a été réceptionné. */
    fun commandeSoldee(commandeeParProduit: Map<Long, Double>, recuParProduit: Map<Long, Double>): Boolean =
        commandeeParProduit.all { (produitId, quantite) ->
            (recuParProduit[produitId] ?: 0.0) >= quantite - 1e-9
        }
}

/**
 * Reporting achats — agrégations pures sur les factures validées.
 */
object AchatReportRules {

    data class LigneFournisseur(val nom: String, val total: Double, val nombre: Int)

    data class PointMois(val debut: Long, val total: Double)

    /** Dépenses validées d'une fenêtre [debut, fin[. */
    fun depenses(montantsDates: List<Pair<Long, Double>>, debut: Long, fin: Long): Double =
        montantsDates.filter { (date, _) -> date in debut until fin }.sumOf { it.second }

    /** Variation en % entre deux montants (null si la référence est nulle). */
    fun evolution(courant: Double, precedent: Double): Double? =
        if (abs(precedent) < 1e-9) null else (courant - precedent) / abs(precedent) * 100.0

    /** Top fournisseurs par dépense, trié décroissant, limité à [limite]. */
    fun topFournisseurs(
        lignes: List<Pair<String, Double>>,
        limite: Int = 5,
    ): List<LigneFournisseur> =
        lignes
            .groupBy { it.first }
            .map { (nom, valeurs) -> LigneFournisseur(nom, valeurs.sumOf { it.second }, valeurs.size) }
            .sortedByDescending { it.total }
            .take(limite)

    /**
     * Répartition par mois (fenêtres [debut, fin[ fournies du plus ancien au plus
     * récent) — pour les barres compactes des 6 derniers mois.
     */
    fun parMois(montantsDates: List<Pair<Long, Double>>, fenetres: List<Pair<Long, Long>>): List<PointMois> =
        fenetres.map { (debut, fin) -> PointMois(debut, depenses(montantsDates, debut, fin)) }
}
