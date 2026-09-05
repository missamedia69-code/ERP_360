package com.missa.b360.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Ligne d'une facture fournisseur (spec §6 ACHATS). Les montants restent dans la
 * devise verrouillée de l'entreprise.
 */
@Serializable
data class PurchaseLine(
    val id: Long,
    val name: String,
    val unitPrice: Double,
    val quantity: Double,
    /** Produit du catalogue rattaché — null pour une ligne libre. */
    val productId: Long? = null,
) {
    val total: Double get() = unitPrice * quantity
}

/**
 * Détail d'une facture fournisseur stocké avec la pièce Achat. Même convention que la
 * vente : un panier persistant sans données de démonstration, et seules les lignes
 * rattachées au catalogue génèrent des mouvements de stock à la validation.
 */
@Serializable
data class PurchaseRecordPayload(
    val schemaVersion: Int = 1,
    val supplierId: Long,
    val supplierName: String,
    val lines: List<PurchaseLine>,
    val subtotal: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String,
    /** Montant réglé à la validation — le reste est un passif fournisseur (spec §6). */
    val paidAmount: Double,
    val note: String? = null,
)

object PurchaseRecordCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: PurchaseRecordPayload): String =
        json.encodeToString(PurchaseRecordPayload.serializer(), payload)

    fun decode(value: String?): PurchaseRecordPayload? = value?.let {
        runCatching { json.decodeFromString(PurchaseRecordPayload.serializer(), it) }.getOrNull()
    }
}

/**
 * Besoins de stock d'une facture fournisseur : les entrées générées à la validation.
 * Règle identique à la vente — agrégation pure, testable sans base.
 */
object PurchaseStockEffects {
    fun besoinsParProduit(lines: List<PurchaseLine>): Map<Long, Double> =
        lines
            .filter { it.productId != null && it.quantity > 0.0 }
            .groupBy { it.productId!! }
            .mapValues { (_, group) -> group.sumOf { it.quantity } }
}

/**
 * Règles pures du retour de vente (spec §22) — testables sans base.
 *
 * Un retour ne peut pas dépasser la quantité encore disponible pour chaque ligne de la
 * facture d'origine : le disponible = quantité vendue − sommes des retours précédents
 * (les avoirs de la même facture).
 */
object ReturnRules {
    /** Clé stable d'une ligne : produit du catalogue, sinon le nom normalisé. */
    fun lineKey(line: SaleLine): String =
        line.productId?.let { "P:$it" } ?: "N:${line.name.trim().lowercase()}"

    /** Quantité encore retournable par ligne (0 si déjà entièrement retournée). */
    fun restantParLigne(
        original: SaleRecordPayload,
        returns: List<SaleRecordPayload>,
    ): Map<String, Double> =
        original.lines
            .groupBy { lineKey(it) }
            .mapValues { (key, group) ->
                val vendu = group.sumOf { it.quantity }
                val deja = returns
                    .flatMap { it.lines }
                    .filter { lineKey(it) == key }
                    .sumOf { it.quantity }
                (vendu - deja).coerceAtLeast(0.0)
            }

    /**
     * @return true si chaque quantité demandée ≤ la quantité encore retournable ;
     *         une ligne inconnue de la facture d'origine est un refus.
     */
    fun retourEstValide(
        original: SaleRecordPayload,
        returns: List<SaleRecordPayload>,
        demande: Map<String, Double>,
    ): Boolean {
        val restant = restantParLigne(original, returns)
        if (demande.isEmpty()) return false
        return demande.all { (key, qty) ->
            qty > 0.0 && qty.isFinite() && restant[key]?.let { qty <= it + 1e-9 } == true
        }
    }

    /** Ajustement cohérent du montant de l'avoir par rapport au solde de la facture. */
    fun soldeFacture(
        original: SaleRecordPayload,
        returns: List<SaleRecordPayload>,
    ): Double =
        (original.total - returns.sumOf { it.total } - original.paidAmount).coerceAtLeast(0.0)

    fun montantEstCoherent(total: Double, tolere: Double): Boolean = abs(total - tolere) <= 0.01
}

/**
 * Règles pures de l'inventaire (spec §12) : l'écart est signé
 * (compté − théorique) et un écart nul ne génère aucun mouvement.
 */
object InventoryRules {
    const val TOLERANCE_ECART = 1e-9

    fun ecart(theorique: Double, compte: Double): Double = compte - theorique

    fun ecartRequiertAjustement(ecart: Double): Boolean = abs(ecart) >= TOLERANCE_ECART

    /** Le stock après ajustement ne peut jamais être négatif. */
    fun stockApresEstValide(theorique: Double, ecart: Double): Boolean =
        theorique + ecart >= -TOLERANCE_ECART
}
