package com.missa.b360

import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.ui.sales.SalesUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesClientRulesTest {
    @Test
    fun `applies negotiated default discount and client specific tax rate`() {
        val state = SalesUiState(
            selectedClient = ClientEntity(
                id = 8,
                code = "CLI-8",
                nom = "Client test",
                telephone = "699000000",
                remiseDefautPct = 10.0,
                remiseMaxPct = 8.0,
                tauxTva = 5.0,
                createdAt = 1,
            ),
            lines = listOf(SaleLine(id = 1, name = "Article", unitPrice = 100.0, quantity = 2.0)),
        )

        val totals = state.totals(taxRate = 5.0)

        assertEquals(200.0, totals.subtotal, 0.001)
        assertEquals(16.0, totals.discount, 0.001)
        assertEquals(184.0, totals.total, 0.001)
        assertEquals(8.7619, totals.taxAmount, 0.001)
    }

    @Test
    fun `explicit discount amount overrides client default discount`() {
        val state = SalesUiState(
            selectedClient = ClientEntity(
                id = 9,
                code = "CLI-9",
                nom = "Client test",
                telephone = "699000001",
                remiseDefautPct = 15.0,
                createdAt = 1,
            ),
            lines = listOf(SaleLine(id = 1, name = "Article", unitPrice = 100.0, quantity = 1.0)),
            discountInput = "5",
            discountManual = true,
        )

        assertEquals(5.0, state.totals(taxRate = 0.0).discount, 0.001)
        assertEquals(95.0, state.totals(taxRate = 0.0).total, 0.001)
    }
}
