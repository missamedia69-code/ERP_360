package com.missa.b360.core.domain.model

/** Tranches de la balance âgée, d'après l'échéance (date de facture + délai de règlement). */
enum class AgingBucket { NON_ECHU, JOURS_1_30, JOURS_31_60, JOURS_61_90, PLUS_90 }

/** Montants ouverts répartis par tranche d'ancienneté. */
data class AgedBalance(
    val montants: Map<AgingBucket, Double>,
    val total: Double,
    /** Part échue (toutes tranches sauf `NON_ECHU`). */
    val enRetard: Double,
) {
    fun montant(tranche: AgingBucket): Double = montants[tranche] ?: 0.0
}

/**
 * Balance âgée d'un client. Les factures ouvertes viennent de
 * [ClientMetricsRules.openInvoices], donc les avoirs sont déjà rapprochés : la balance, l'encours
 * et les retards affichés partout dans le module proviennent du même calcul.
 */
object AgedBalanceRules {
    private const val EPSILON = 1e-9

    /**
     * Retard en jours entiers, arrondi par excès : une facture échue depuis une heure a 1 jour de
     * retard. 0 si l'échéance n'est pas dépassée (le jour d'échéance lui-même n'est pas en retard).
     */
    fun joursDeRetard(dueAt: Long, now: Long): Int {
        if (dueAt >= now) return 0
        val ecart = runCatching { Math.subtractExact(now, dueAt) }.getOrDefault(Long.MAX_VALUE)
        val jours = ecart / ClientMetricsRules.DAY_MS + if (ecart % ClientMetricsRules.DAY_MS != 0L) 1 else 0
        return jours.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun tranche(dueAt: Long, now: Long): AgingBucket {
        val jours = joursDeRetard(dueAt, now)
        return when {
            jours <= 0 -> AgingBucket.NON_ECHU
            jours <= 30 -> AgingBucket.JOURS_1_30
            jours <= 60 -> AgingBucket.JOURS_31_60
            jours <= 90 -> AgingBucket.JOURS_61_90
            else -> AgingBucket.PLUS_90
        }
    }

    fun calculer(factures: List<ClientOpenInvoice>, now: Long): AgedBalance {
        val montants = AgingBucket.entries.associateWith { 0.0 }.toMutableMap()
        factures.forEach { facture ->
            if (!facture.outstanding.isFinite() || facture.outstanding <= EPSILON) return@forEach
            val tranche = tranche(facture.dueAt, now)
            montants[tranche] = (montants[tranche] ?: 0.0) + facture.outstanding
        }
        val total = montants.values.sum()
        return AgedBalance(
            montants = montants,
            total = total,
            enRetard = total - (montants[AgingBucket.NON_ECHU] ?: 0.0),
        )
    }

    fun calculer(items: List<ClientLedgerItem>, paymentDays: Int, now: Long): AgedBalance =
        calculer(ClientMetricsRules.openInvoices(items, paymentDays), now)
}
