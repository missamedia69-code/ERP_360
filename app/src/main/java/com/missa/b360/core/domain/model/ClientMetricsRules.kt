package com.missa.b360.core.domain.model

/** Facture/avoir validé présenté au calcul de l'encours d'un client. */
data class ClientLedgerItem(
    val total: Double,
    val paid: Double,
    val issuedAt: Long,
    val creditNote: Boolean = false,
    val recordId: Long? = null,
    val sourceRecordId: Long? = null,
)

data class ClientLedgerMetrics(
    val salesTotal: Double = 0.0,
    val outstanding: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val lastSaleAt: Long? = null,
)

/** Calcul pur du tableau Clients : les avoirs sont d'abord rapprochés de leur facture source. */
object ClientMetricsRules {
    const val DAY_MS: Long = 86_400_000L
    private const val EPSILON = 1e-9

    private data class OpenInvoice(
        val item: ClientLedgerItem,
        val dueAt: Long,
        var outstanding: Double,
    )

    fun calculate(items: List<ClientLedgerItem>, paymentDays: Int, now: Long): ClientLedgerMetrics {
        val valid = items.filter {
            it.total.isFinite() && it.total >= 0.0 && it.paid.isFinite() && it.paid >= 0.0 &&
                it.paid <= it.total + EPSILON
        }
        val jours = paymentDays.coerceIn(0, 365)
        val invoices = valid.filterNot { it.creditNote }
            .map { item ->
                val dueAt = runCatching { Math.addExact(item.issuedAt, jours.toLong() * DAY_MS) }
                    .getOrDefault(if (item.issuedAt >= 0) Long.MAX_VALUE else Long.MIN_VALUE)
                OpenInvoice(item, dueAt, (item.total - item.paid).coerceAtLeast(0.0))
            }
            .sortedBy { it.dueAt }

        // Les avoirs sont reliés à leur facture d'origine ; seul un reliquat créditeur
        // est ensuite porté sur les autres plus anciennes créances du même client.
        val avoirs = valid.filter { it.creditNote }
            .sortedBy { it.issuedAt }
            .map { it to (it.total - it.paid).coerceAtLeast(0.0) }
        avoirs.forEach { (avoir, montantAvoir) ->
            var restant = montantAvoir
            val indexLie = avoir.sourceRecordId?.let { sourceId ->
                invoices.indexOfFirst { it.item.recordId == sourceId }.takeIf { it >= 0 }
            }
            if (indexLie != null && restant > EPSILON) {
                val invoice = invoices[indexLie]
                val applique = minOf(invoice.outstanding, restant)
                invoice.outstanding -= applique
                restant -= applique
            }
            for (invoice in invoices) {
                if (restant <= EPSILON) break
                val applique = minOf(invoice.outstanding, restant)
                invoice.outstanding -= applique
                restant -= applique
            }
        }

        val openInvoices = invoices.filter { it.outstanding > EPSILON }
        val overdue = openInvoices.filter { it.dueAt < now }
        val dueSoon = openInvoices.count { it.dueAt >= now && it.dueAt <= now + 7L * DAY_MS }
        return ClientLedgerMetrics(
            salesTotal = valid.sumOf { if (it.creditNote) -it.total else it.total },
            outstanding = openInvoices.sumOf { it.outstanding },
            overdueAmount = overdue.sumOf { it.outstanding },
            overdueCount = overdue.size,
            dueSoonCount = dueSoon,
            lastSaleAt = valid.filterNot { it.creditNote }.maxOfOrNull { it.issuedAt },
        )
    }
}
