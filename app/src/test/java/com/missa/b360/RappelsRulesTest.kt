package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.RappelsRules
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class RappelsRulesTest {
    @Test
    fun overdueInvoicesExcludePaidDraftCancelledAndNotYetDueDocuments() {
        val now = 100L * DAY_MS
        val pieces = listOf(
            invoice(id = 1, createdAt = 1L * DAY_MS, status = OperationStatus.VALIDATED.name, paid = 25.0),
            invoice(id = 2, createdAt = 1L * DAY_MS, status = OperationStatus.VALIDATED.name, paid = 100.0),
            invoice(id = 3, createdAt = 1L * DAY_MS, status = OperationStatus.DRAFT.name, paid = 0.0),
            invoice(id = 4, createdAt = 1L * DAY_MS, status = OperationStatus.CANCELLED.name, paid = 0.0),
            invoice(id = 5, createdAt = 90L * DAY_MS, status = OperationStatus.VALIDATED.name, paid = 0.0),
        )

        val overdue = RappelsRules.facturesEnRetard(pieces, now)

        assertEquals(listOf(1L), overdue.map { it.first.id })
        assertEquals(75.0, overdue.single().second, 0.001)
    }

    private fun invoice(id: Long, createdAt: Long, status: String, paid: Double) =
        OperationRecordEntity(
            id = id,
            module = OperationModule.VENTE.name,
            reference = "FAC-$id",
            title = "Facture test",
            amount = 100.0,
            status = status,
            notes = SaleRecordCodec.encode(
                SaleRecordPayload(
                    clientId = 1L,
                    clientName = "Client test",
                    lines = listOf(SaleLine(id = 1L, name = "Article", unitPrice = 100.0, quantity = 1.0)),
                    subtotal = 100.0,
                    discount = 0.0,
                    delivery = 0.0,
                    taxRate = 0.0,
                    taxAmount = 0.0,
                    total = 100.0,
                    paymentMethod = "Espèces",
                    paidAmount = paid,
                ),
            ),
            createdAt = createdAt,
        )

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
