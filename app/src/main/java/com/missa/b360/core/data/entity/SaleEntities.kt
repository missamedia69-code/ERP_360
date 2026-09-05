package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Statut d'une vente : brouillon, validée ou annulée (compensation, jamais supprimée). */
enum class SaleStatus(val code: String) {
    DRAFT("DRAFT"),
    VALIDATED("VALIDATED"),
    CANCELLED("CANCELLED"),
}

/** Statut de la créance cliente liée à une vente partielle/à crédit. */
enum class SaleReceivableStatus(val code: String) {
    OPEN("OPEN"),
    PAID("PAID"),
    CANCELLED("CANCELLED"),
}

/**
 * Vente transactionnelle du module VENTE. Les montants sont stockés en centimes
 * (`*Cents`) dans la devise verrouillée de l'entreprise ; aucun calcul métier
 * n'est fait dans l'UI, et aucune vente n'est supprimée physiquement.
 */
@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["clientId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["operationRecordId"]),
    ],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Lien vers la pièce générique `operation_records` pour l'audit/le moteur commun. */
    val operationRecordId: Long? = null,
    val reference: String,
    val clientId: Long? = null,
    val clientName: String,
    val walkIn: Boolean = false,
    val status: String = SaleStatus.DRAFT.code,
    val subtotalCents: Long = 0,
    val discountCents: Long = 0,
    val deliveryCents: Long = 0,
    val taxRate: Double = 0.0,
    val taxAmountCents: Long = 0,
    val totalCents: Long = 0,
    val paymentMethod: String = "",
    val isCredit: Boolean = false,
    val paidCents: Long = 0,
    val changeCents: Long = 0,
    val remainingCents: Long = 0,
    val note: String? = null,
    val internalReference: String? = null,
    val sellerName: String? = null,
    val siteName: String? = null,
    val devise: String = "XAF",
    val createdAt: Long = System.currentTimeMillis(),
    val validatedAt: Long? = null,
    val cancelledAt: Long? = null,
)

/** Ligne de panier réellement persistée pour une vente. */
@Entity(
    tableName = "sale_lines",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"]),
    ],
)
data class SaleLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long? = null,
    val sku: String? = null,
    val name: String,
    val unit: String = "unité",
    val unitPriceCents: Long,
    val quantity: Double,
    val discountPct: Double = 0.0,
    val netCents: Long,
    val freeProduct: Boolean = false,
)

/** Paiement enregistré avec une vente (une ligne par méthode utilisée). */
@Entity(
    tableName = "sale_payments",
    indices = [Index(value = ["saleId"])],
)
data class SalePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val method: String,
    val amountCents: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Créance client issue d'une vente partielle ou à crédit. */
@Entity(
    tableName = "sale_receivables",
    indices = [
        Index(value = ["saleId"], unique = true),
        Index(value = ["clientId"]),
        Index(value = ["status"]),
    ],
)
data class SaleReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val clientId: Long? = null,
    val totalCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val status: String = SaleReceivableStatus.OPEN.code,
    val createdAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null,
)
