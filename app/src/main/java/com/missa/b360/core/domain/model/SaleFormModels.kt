package com.missa.b360.core.domain.model

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Représentation monétaire exacte du module Vente : les montants sont manipulés en
 * centimes (`Long`) dans la couche métier, puis convertis uniquement pour l'échange
 * avec l'existant (types hérités en Double) et pour le rendu centralisé en devise.
 *
 * L'infrastructure historique de l'application stocke certains montants en Double ;
 * [fromDouble]/[toDouble] garantissent qu'aucun arrondi n'est introduit deux fois.
 */
object SaleMoney {
    const val SCALE = 100L

    /** Convertit une valeur entière (majeur) en centimes. */
    fun fromMajor(value: Long): Long = value * SCALE

    /** Convertit une valeur Double historique en centimes (arrondi monétaire). */
    fun fromDouble(value: Double): Long = (((value * SCALE)).roundToLong()).coerceAtLeast(0L)

    /** Convertit des centimes vers la représentation historique double (lecture uniquement). */
    fun toDouble(cents: Long): Double = cents / SCALE.toDouble()

    /**
     * Parse une saisie utilisateur ("100 000", "1500.50").
     * La représentation interne est toujours en centimes : 1 unité monétaire = 100 centimes,
     * y compris pour XAF/XOF. [decimals] sert uniquement à limiter la précision d'affichage.
     * @return centimes, ou null si la saisie n'est pas un nombre positif valide.
     */
    fun parse(input: String, decimals: Int = 2): Long? {
        val normalized = input.trim().replace(" ", "").replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value < 0.0) return null
        return ((value * SCALE).roundToLong()).coerceAtLeast(0L)
    }

    /** Arrondit un total centimes (garde-fou contre les valeurs négatives). */
    fun nonNegative(cents: Long): Long = cents.coerceAtLeast(0L)

    /** Quantité décimale d'une ligne (l'argent reste en centimes, les quantités peuvent être fractionnaires). */
    fun qty(value: Double): Double = if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0

    /** Pourcentage saisie (0..100). */
    fun pct(value: Double): Double = value.takeIf { it.isFinite() }?.coerceIn(0.0, 100.0) ?: 0.0

    /** Nombre de décimales monétaires courantes pour l'affichage. */
    fun decimalsFor(devise: String): Int = when (devise.uppercase(Locale.ROOT)) {
        "XAF", "XOF" -> 0
        else -> 2
    }
}

/** Erreurs métier renvoyées par le calcul/la sauvegarde d'une vente (codes stables pour l'UI). */
enum class SaleErrorCode {
    CLIENT_REQUIRED,
    EMPTY_CART,
    INVALID_LINE,
    STOCK_INSUFFICIENT,
    DISCOUNT_INVALID,
    PAYMENT_INVALID,
    PERMISSION_DENIED,
    READ_ONLY,
    NOT_DRAFT,
    INTERNAL,
}

/** Erreur compréhensible côté UI (jamais de message technique brut). */
data class SaleFormError(
    val code: SaleErrorCode,
    val productName: String? = null,
    val available: Long? = null,
    val key: String = "",
)

/** Ligne de panier du formulaire unique de vente, avec les informations produit réelles. */
@kotlinx.serialization.Serializable
data class SaleFormLine(
    val id: Long,
    val productId: Long? = null,
    val sku: String? = null,
    val name: String,
    val unit: String = "unité",
    val unitPriceCents: Long,
    val quantity: Double,
    val discountPct: Double = 0.0,
    val stockAvailable: Double? = null,
    val freeProduct: Boolean = false,
) {
    val grossCents: Long
        get() = SaleMoney.nonNegative((unitPriceCents * quantity).roundToLong())

    val discountCents: Long
        get() = SaleMoney.nonNegative((grossCents * SaleMoney.pct(discountPct) / 100.0).toLong())

    val netCents: Long
        get() = SaleMoney.nonNegative(grossCents - discountCents)
}

/** Résultat de calcul pur, en centimes, produit par [SaleFormCalculator] (couche métier). */
data class SaleCalculation(
    val subtotalCents: Long = 0,
    val discountCents: Long = 0,
    val perLineDiscountCents: Long = 0,
    val deliveryCents: Long = 0,
    val taxRate: Double = 0.0,
    val taxAmountCents: Long = 0,
    val totalCents: Long = 0,
    val paidCents: Long = 0,
    val changeCents: Long = 0,
    val remainingCents: Long = 0,
    val isCredit: Boolean = false,
) {
    val canPayInFull: Boolean get() = paidCents >= totalCents
}

/** Entrée normalisée du formulaire, transmise couche métier pour calcul/validation/sauvegarde. */
data class SaleFormInput(
    val clientId: Long?,
    val clientName: String? = null,
    val walkIn: Boolean,
    val lines: List<SaleFormLine>,
    val discountInput: String,
    val discountPercentMode: Boolean,
    val deliveryInput: String,
    val taxRate: Double,
    val paymentMethod: String,
    val isCredit: Boolean,
    val receivedInput: String,
    val paidInput: String,
    val note: String?,
    val internalReference: String?,
    val sellerName: String?,
    val siteName: String?,
    val devise: String,
)

/**
 * Calculs métier purs de la vente : sous-total, remise globale, livraison, TVA incluse,
 * total, monnaie à rendre ou reste à payer. Aucun état, aucune dépendance Android.
 */
object SaleFormCalculator {
    fun calculate(input: SaleFormInput): SaleCalculation {
        val subtotal = input.lines.sumOf { it.netCents }.coerceAtLeast(0L)

        val discount = if (input.discountPercentMode) {
            SaleMoney.nonNegative((subtotal * SaleMoney.pct(input.discountInput.toDoubleOrNull() ?: 0.0) / 100.0).toLong())
        } else {
            SaleMoney.nonNegative(SaleMoney.parse(input.discountInput, SaleMoney.decimalsFor(input.devise)) ?: 0L)
        }.coerceAtMost(subtotal)

        val delivery = SaleMoney.nonNegative(SaleMoney.parse(input.deliveryInput, SaleMoney.decimalsFor(input.devise)) ?: 0L)
        val total = SaleMoney.nonNegative(subtotal - discount + delivery)
        val tax = if (input.taxRate <= 0.0) 0L
        else SaleMoney.nonNegative((total * input.taxRate / (100.0 + input.taxRate)).toLong())

        val received = SaleMoney.nonNegative(SaleMoney.parse(input.receivedInput, SaleMoney.decimalsFor(input.devise)) ?: 0L)
        val paid = if (input.isCredit) {
            SaleMoney.nonNegative(SaleMoney.parse(input.paidInput, SaleMoney.decimalsFor(input.devise)) ?: 0L)
        } else {
            SaleMoney.nonNegative(received.coerceAtMost(total))
        }

        val change = if (!input.isCredit) SaleMoney.nonNegative(received - total) else 0L
        val remaining = SaleMoney.nonNegative(total - paid)

        return SaleCalculation(
            subtotalCents = subtotal,
            discountCents = discount,
            perLineDiscountCents = input.lines.sumOf { it.discountCents }.coerceAtMost(subtotal),
            deliveryCents = delivery,
            taxRate = input.taxRate,
            taxAmountCents = tax,
            totalCents = total,
            paidCents = paid,
            changeCents = change,
            remainingCents = remaining,
            isCredit = input.isCredit,
        )
    }

    /**
     * Validation déterministe avant sauvegarde (et re-contrôle stock effectué transactionalement).
     * [allowIncomplete] autorise l'enregistrement d'un brouillon sans client ni mode de paiement.
     */
    fun validate(input: SaleFormInput, calculate: SaleCalculation, allowIncomplete: Boolean = false): SaleFormError? {
        if (!allowIncomplete && !input.walkIn && input.clientId == null) {
            return SaleFormError(SaleErrorCode.CLIENT_REQUIRED, key = "client")
        }
        if (input.lines.isEmpty()) return SaleFormError(SaleErrorCode.EMPTY_CART, key = "cart")
        if (input.lines.any {
                it.name.isBlank() || !it.quantity.isFinite() || it.quantity <= 0.0 || it.unitPriceCents <= 0L
            }
        ) {
            return SaleFormError(SaleErrorCode.INVALID_LINE, key = "line")
        }
        if (calculate.totalCents < 0L) return SaleFormError(SaleErrorCode.PAYMENT_INVALID, key = "total")
        if (allowIncomplete) return null
        if (input.paymentMethod.isBlank()) {
            return SaleFormError(SaleErrorCode.PAYMENT_INVALID, key = "payment")
        }
        if (calculate.totalCents > 0L && !input.isCredit && calculate.paidCents < calculate.totalCents) {
            return SaleFormError(SaleErrorCode.PAYMENT_INVALID, key = "not_enough")
        }
        if (input.isCredit && calculate.paidCents > calculate.totalCents) {
            return SaleFormError(SaleErrorCode.PAYMENT_INVALID, key = "overpaid")
        }
        return null
    }
}

/** Résultat transactionnel de l'enregistrement d'une vente. */
sealed interface SaleSaveOutcome {
    data class Success(
        val recordId: Long,
        val reference: String,
        val output: SaleRecordPayload,
        val draft: Boolean = false,
        /** Codes d'opérations réellement réussies : SALE, PAYMENT, STOCK, INVOICE, FINANCE. */
        val completed: List<String>,
    ) : SaleSaveOutcome

    data class Failed(val error: SaleFormError) : SaleSaveOutcome
    data object ReadOnly : SaleSaveOutcome
}
