package com.missa.b360.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

/** Ligne réellement ajoutée à une vente. Les montants restent dans la devise de l'entreprise. */
@Serializable
data class SaleLine(
    val id: Long,
    val name: String,
    val unitPrice: Double,
    val quantity: Double,
    val productId: Long? = null,
    val sku: String? = null,
    val unit: String = "unité",
    val discountPct: Double = 0.0,
    val freeProduct: Boolean = false,
) {
    val total: Double get() = unitPrice * quantity
}

/** Montants calculés localement pour le panier de vente. Les prix sont considérés TTC. */
data class SaleTotals(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val delivery: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
)

/** Règles de calcul pures afin d'éviter toute divergence entre l'écran et la sauvegarde. */
object SaleCalculator {
    fun calculate(
        lines: List<SaleLine>,
        discount: Double,
        delivery: Double,
        taxRate: Double,
    ): SaleTotals {
        val subtotal = lines.sumOf { it.total }.coerceAtLeast(0.0)
        val safeDiscount = discount.coerceIn(0.0, subtotal)
        val safeDelivery = delivery.coerceAtLeast(0.0)
        val total = max(0.0, subtotal - safeDiscount + safeDelivery)
        // Les prix affichés en vente sont TTC ; la TVA affichée est donc la part incluse.
        val safeTaxRate = taxRate.coerceAtLeast(0.0)
        val taxAmount = if (safeTaxRate == 0.0) 0.0 else total * safeTaxRate / (100.0 + safeTaxRate)
        return SaleTotals(
            subtotal = subtotal,
            discount = safeDiscount,
            delivery = safeDelivery,
            taxAmount = taxAmount,
            total = total,
        )
    }
}

/**
 * Détail d'une facture stocké avec la pièce Vente. Ceci conserve un panier persistant sans
 * données de démonstration, tout en restant compatible avec la table de pièces existante.
 */
@Serializable
data class SaleRecordPayload(
    val schemaVersion: Int = 2,
    val saleId: Long? = null,
    val reference: String? = null,
    val clientId: Long?,
    val clientName: String,
    val lines: List<SaleLine>,
    val subtotal: Double,
    val discount: Double,
    val delivery: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String,
    val paidAmount: Double,
    val note: String? = null,
    /** Représentation monétaire exacte persistée (centimes) pour les ventes réelles. */
    val subtotalCents: Long = 0,
    val discountCents: Long = 0,
    val deliveryCents: Long = 0,
    val taxAmountCents: Long = 0,
    val totalCents: Long = 0,
    val paidCents: Long = 0,
    val remainingCents: Long = 0,
    val changeCents: Long = 0,
    val isCredit: Boolean = false,
)

object SaleRecordCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: SaleRecordPayload): String = json.encodeToString(SaleRecordPayload.serializer(), payload)

    fun decode(value: String?): SaleRecordPayload? = value?.let {
        runCatching { json.decodeFromString(SaleRecordPayload.serializer(), it) }.getOrNull()
    }
}
