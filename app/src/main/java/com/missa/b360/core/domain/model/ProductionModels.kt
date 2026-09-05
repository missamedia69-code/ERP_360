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
object ProductionRules {
    fun besoinsParComposant(payload: ProductionRecordPayload): Map<Long, Double> =
        payload.composants
            .filter { it.quantite > 0.0 }
            .groupBy { it.productId }
            .mapValues { (_, group) -> group.sumOf { it.quantite } }
}
