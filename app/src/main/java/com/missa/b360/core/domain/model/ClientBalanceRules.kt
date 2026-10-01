package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus

/** Encaissement postérieur à la facture ; [montant] négatif pour une contre-passation. */
data class ClientPaymentItem(
    val montant: Double,
    val invoiceRecordId: Long? = null,
)

/** Situation de compte calculée ; reflétée telle quelle dans `client_balances`. */
data class ClientBalanceSnapshot(
    val encours: Double = 0.0,
    val enRetard: Double = 0.0,
    val joursRetardMax: Int = 0,
    val ca12Mois: Double = 0.0,
    val derniereVenteAt: Long? = null,
    val nbVentes: Int = 0,
)

/**
 * Calcul pur et unique du compte d'un client. La liste, la fiche, le compte, la relance et le
 * contrôle de crédit lisent tous le résultat de cette fonction : leurs montants concordent.
 */
object ClientBalanceRules {
    private const val EPSILON = 1e-9
    private const val ANNEE_MS = 365L * ClientMetricsRules.DAY_MS

    /** Ventes et avoirs validés regroupés par client (détail JSON décodé, pièces illisibles ignorées). */
    fun ledgerParClient(records: List<OperationRecordEntity>): Map<Long, List<ClientLedgerItem>> =
        records.asSequence()
            .filter { it.status == OperationStatus.VALIDATED.name }
            .mapNotNull { record ->
                val payload = SaleRecordCodec.decode(record.notes) ?: return@mapNotNull null
                if (payload.clientId <= 0L || !payload.total.isFinite() || payload.total < 0.0 ||
                    !payload.paidAmount.isFinite() || payload.paidAmount < 0.0
                ) return@mapNotNull null
                payload.clientId to ClientLedgerItem(
                    total = payload.total,
                    paid = payload.paidAmount.coerceAtMost(payload.total),
                    issuedAt = record.createdAt,
                    creditNote = payload.sourceRecordId != null,
                    recordId = record.id,
                    sourceRecordId = payload.sourceRecordId,
                )
            }
            .groupBy({ it.first }, { it.second })

    /**
     * Impute les encaissements aux factures : d'abord celles qu'ils désignent, puis le reste (et
     * l'excédent d'une facture) aux échéances les plus anciennes. Une contre-passation retire
     * du montant payé, sans jamais le rendre négatif. Les avoirs ne sont pas touchés.
     */
    fun appliquerPaiements(items: List<ClientLedgerItem>, paiements: List<ClientPaymentItem>): List<ClientLedgerItem> {
        if (paiements.isEmpty()) return items
        val factures = items.indices.filter { i ->
            !items[i].creditNote && ClientMetricsRules.valides(listOf(items[i])).isNotEmpty()
        }
        if (factures.isEmpty()) return items
        val paye = DoubleArray(items.size) { items[it].paid }
        val parId = HashMap<Long, Int>()
        factures.forEach { i -> items[i].recordId?.let { parId[it] = i } }

        var libre = 0.0
        for (paiement in paiements) {
            if (!paiement.montant.isFinite() || paiement.montant == 0.0) continue
            val cible = paiement.invoiceRecordId?.let { parId[it] }
            if (cible == null) libre += paiement.montant else paye[cible] += paiement.montant
        }
        for (i in factures) {
            val total = items[i].total
            if (paye[i] > total) {
                libre += paye[i] - total
                paye[i] = total
            }
            if (paye[i] < 0.0) paye[i] = 0.0
        }
        libre = libre.coerceAtLeast(0.0)
        for (i in factures.sortedBy { items[it].issuedAt }) {
            if (libre <= EPSILON) break
            val reste = items[i].total - paye[i]
            if (reste <= EPSILON) continue
            val applique = minOf(reste, libre)
            paye[i] += applique
            libre -= applique
        }
        val concernees = factures.toSet()
        return items.mapIndexed { i, item -> if (i in concernees) item.copy(paid = paye[i]) else item }
    }

    fun calculer(
        items: List<ClientLedgerItem>,
        paiements: List<ClientPaymentItem>,
        paymentDays: Int,
        now: Long,
    ): ClientBalanceSnapshot {
        val imputes = appliquerPaiements(items, paiements)
        val metrics = ClientMetricsRules.calculate(imputes, paymentDays, now)
        val valides = ClientMetricsRules.valides(items)
        val debutFenetre = runCatching { Math.subtractExact(now, ANNEE_MS) }.getOrDefault(Long.MIN_VALUE)
        val ca12Mois = valides
            .filter { it.issuedAt > debutFenetre && it.issuedAt <= now }
            .sumOf { if (it.creditNote) -it.total else it.total }
            .coerceAtLeast(0.0)
        return ClientBalanceSnapshot(
            encours = metrics.outstanding,
            enRetard = metrics.overdueAmount,
            joursRetardMax = metrics.joursRetardMax,
            ca12Mois = ca12Mois,
            derniereVenteAt = metrics.lastSaleAt,
            nbVentes = valides.count { !it.creditNote },
        )
    }
}
