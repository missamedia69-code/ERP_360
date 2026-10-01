package com.missa.b360

import com.missa.b360.core.domain.model.ClientLedgerItem
import com.missa.b360.core.domain.model.ClientMetricsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientOpenInvoicesTest {
    private val day = ClientMetricsRules.DAY_MS
    private val now = 2_000_000_000_000L

    @Test fun `factures ouvertes triees par echeance avec leur reste a payer`() {
        val open = ClientMetricsRules.openInvoices(
            listOf(
                ClientLedgerItem(100.0, 0.0, now - 5 * day, recordId = 2),
                ClientLedgerItem(80.0, 30.0, now - 50 * day, recordId = 1),
                ClientLedgerItem(40.0, 40.0, now - 10 * day, recordId = 3),
            ),
            paymentDays = 30,
        )
        assertEquals(listOf<Long?>(1L, 2L), open.map { it.recordId })
        assertEquals(50.0, open[0].outstanding, 1e-9)
        assertEquals(now - 50 * day + 30 * day, open[0].dueAt)
        assertTrue(open[0].dueAt < open[1].dueAt)
    }

    @Test fun `une facture soldee par un avoir nest plus ouverte`() {
        val open = ClientMetricsRules.openInvoices(
            listOf(
                ClientLedgerItem(100.0, 0.0, now - 5 * day, recordId = 1),
                ClientLedgerItem(100.0, 0.0, now, creditNote = true, sourceRecordId = 1),
            ),
            paymentDays = 30,
        )
        assertTrue(open.isEmpty())
    }

    @Test fun `joursRetardMax suit la plus ancienne echeance impayee`() {
        val metrics = ClientMetricsRules.calculate(
            listOf(
                ClientLedgerItem(100.0, 0.0, now - 40 * day),
                ClientLedgerItem(100.0, 0.0, now - 100 * day),
            ),
            paymentDays = 30,
            now = now,
        )
        assertEquals(70, metrics.joursRetardMax)
    }

    @Test fun `joursRetardMax est nul sans echeance depassee`() {
        val metrics = ClientMetricsRules.calculate(
            listOf(ClientLedgerItem(100.0, 0.0, now - 5 * day)),
            paymentDays = 30,
            now = now,
        )
        assertEquals(0, metrics.joursRetardMax)
        assertEquals(0, ClientMetricsRules.calculate(emptyList(), 30, now).joursRetardMax)
    }
}
