package com.missa.b360.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Ordre de production (spec §Production, doc OP).
 *
 * Le lancement (validation) est **transactionnel** : sortie des composants
 * (contrôle de stock) + entrée du produit fini — dans la même transaction.
 * Brouillon = aucun effet stock.
 */
@Serializable
data class ProductionComponent(
    val productId: Long,
    val nom: String,
    val quantite: Double,
)

@Serializable
data class ProductionRecordPayload(
    val schemaVersion: Int = 1,
    val produitId: Long,
    val produitNom: String,
    val quantite: Double,
    val composants: List<ProductionComponent>,
    /** Coût matière déterminé par Stock lors de la réception du produit fini. */
    val coutMatieres: Double? = null,
    val siteDestinationId: Long? = null,
)

object ProductionCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: ProductionRecordPayload): String =
        json.encodeToString(ProductionRecordPayload.serializer(), payload)

    fun decode(value: String?): ProductionRecordPayload? = value?.let {
        runCatching { json.decodeFromString(ProductionRecordPayload.serializer(), it) }.getOrNull()
    }
}

/** Besoins de composants par produit — agrégation du panier de l'ordre. */
data class ProductionStockLocation(val siteId: Long, val quantity: Double)

data class ProductionLocationAllocation(val siteId: Long, val quantity: Double, val beforeQuantity: Double)

object ProductionRules {
    /** Stock-prioritized allocation that can split one material need across depots. */
    fun allocateAcrossSites(
        locations: List<ProductionStockLocation>,
        required: Double,
        preferredSiteId: Long?,
    ): List<ProductionLocationAllocation>? {
        if (!required.isFinite() || required <= 0.0 || locations.any {
                it.siteId <= 0 || !it.quantity.isFinite() || it.quantity < 0.0
            } || locations.map { it.siteId }.distinct().size != locations.size
        ) return null
        val total = locations.sumOf { it.quantity }
        if (!total.isFinite() || total < required) return null
        var remaining = required
        val allocations = mutableListOf<ProductionLocationAllocation>()
        locations.filter { it.quantity > 0.0 }
            .sortedWith(compareByDescending<ProductionStockLocation> { it.siteId == preferredSiteId }.thenByDescending { it.quantity })
            .forEach { location ->
                if (remaining > 0.0) {
                    val quantity = minOf(location.quantity, remaining)
                    allocations += ProductionLocationAllocation(location.siteId, quantity, location.quantity)
                    remaining -= quantity
                }
            }
        return allocations.takeIf { remaining <= 0.0 }
    }

    fun payloadIsValid(payload: ProductionRecordPayload): Boolean =
        payload.produitId > 0 && payload.produitNom.isNotBlank() &&
            payload.quantite.isFinite() && payload.quantite > 0.0 && payload.composants.isNotEmpty() &&
            (payload.coutMatieres == null || payload.coutMatieres.isFinite() && payload.coutMatieres >= 0.0) &&
            (payload.siteDestinationId == null || payload.siteDestinationId > 0) &&
            payload.composants.none {
                it.productId <= 0 || it.productId == payload.produitId || it.nom.isBlank() ||
                    !it.quantite.isFinite() || it.quantite <= 0.0
            }

    fun besoinsParComposant(payload: ProductionRecordPayload): Map<Long, Double> =
        payload.composants
            .filter { it.quantite > 0.0 }
            .groupBy { it.productId }
            .mapValues { (_, group) -> group.sumOf { it.quantite } }
}
