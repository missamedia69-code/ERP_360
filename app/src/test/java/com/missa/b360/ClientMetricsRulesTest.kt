package com.missa.b360

import com.missa.b360.core.domain.model.ClientLedgerItem
import com.missa.b360.core.domain.model.ClientMetricsRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientMetricsRulesTest {
    @Test
    fun `calcule encours echeances et avoirs sans compter deux fois les paiements`() {
        val now = 2_000_000_000_000L
        val result = ClientMetricsRules.calculate(
            items = listOf(
                ClientLedgerItem(total = 100.0, paid = 0.0, issuedAt = now - 40 * ClientMetricsRules.DAY_MS),
                ClientLedgerItem(total = 80.0, paid = 20.0, issuedAt = now - 25 * ClientMetricsRules.DAY_MS),
                ClientLedgerItem(total = 30.0, paid = 0.0, issuedAt = now, creditNote = true),
            ),
            paymentDays = 30,
            now = now,
        )
        assertEquals(150.0, result.salesTotal, 1e-9)
        assertEquals(130.0, result.outstanding, 1e-9)
        assertEquals(70.0, result.overdueAmount, 1e-9)
        assertEquals(1, result.overdueCount)
        assertEquals(1, result.dueSoonCount)
        assertEquals(now - 25 * ClientMetricsRules.DAY_MS, result.lastSaleAt)
    }

    @Test
    fun `un avoir est rapproche de sa facture source avant le calcul des retards`() {
        val now = 2_000_000_000_000L
        val result = ClientMetricsRules.calculate(
            items = listOf(
                ClientLedgerItem(100.0, 0.0, now - 50 * ClientMetricsRules.DAY_MS, recordId = 10),
                ClientLedgerItem(200.0, 0.0, now - 20 * ClientMetricsRules.DAY_MS, recordId = 20),
                ClientLedgerItem(150.0, 0.0, now, creditNote = true, sourceRecordId = 20),
            ),
            paymentDays = 30,
            now = now,
        )
        assertEquals(150.0, result.outstanding, 1e-9)
        assertEquals(100.0, result.overdueAmount, 1e-9)
        assertEquals(1, result.overdueCount)
    }

    @Test
    fun `avoirs soldent dabord les echeances les plus anciennes et les valeurs invalides sont ignorees`() {
        val now = 2_000_000_000_000L
        val result = ClientMetricsRules.calculate(
            items = listOf(
                ClientLedgerItem(50.0, 0.0, now - 40 * ClientMetricsRules.DAY_MS),
                ClientLedgerItem(20.0, 0.0, now - 25 * ClientMetricsRules.DAY_MS),
                ClientLedgerItem(60.0, 0.0, now, creditNote = true),
                ClientLedgerItem(Double.NaN, 0.0, now),
            ),
            paymentDays = 30,
            now = now,
        )
        assertEquals(10.0, result.outstanding, 1e-9)
        assertEquals(0.0, result.overdueAmount, 1e-9)
        assertEquals(1, result.dueSoonCount)
        assertEquals(now - 25 * ClientMetricsRules.DAY_MS, result.lastSaleAt)
    }
}
