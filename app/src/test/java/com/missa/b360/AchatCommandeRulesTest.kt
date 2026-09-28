package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.AchatCommandeRules
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class AchatCommandeRulesTest {
    @Test
    fun pendingReminderOnlyCountsDraftSupplierPurchaseOrders() {
        val orderPayload = CommandeAchatCodec.encode(
            CommandeAchatPayload(
                supplierId = 1L,
                supplierName = "Fournisseur",
                lines = listOf(CommandeAchatLigne(id = 1L, name = "Article", quantity = 1.0, unitPrice = 5.0)),
            ),
        )
        val invoicePayload = PurchaseRecordCodec.encode(
            PurchaseRecordPayload(
                supplierId = 1L,
                supplierName = "Fournisseur",
                lines = listOf(PurchaseLine(id = 1L, name = "Article", unitPrice = 5.0, quantity = 1.0)),
                subtotal = 5.0,
                taxRate = 0.0,
                taxAmount = 0.0,
                total = 5.0,
                paymentMethod = "Espèces",
                paidAmount = 0.0,
            ),
        )
        val pieces = listOf(
            piece(1L, "B2026-0001", OperationModule.ACHATS, OperationStatus.DRAFT, orderPayload),
            // Les deux payloads sont décodables l'un par l'autre : la référence distingue le type.
            piece(2L, "FFR2026-0001", OperationModule.ACHATS, OperationStatus.DRAFT, invoicePayload),
            piece(3L, "B2026-0002", OperationModule.ACHATS, OperationStatus.VALIDATED, orderPayload),
            piece(4L, "B2026-0003", OperationModule.VENTE, OperationStatus.DRAFT, orderPayload),
        )

        assertEquals(listOf(1L), AchatCommandeRules.commandesEnAttente(pieces).map { it.id })
    }

    private fun piece(
        id: Long,
        reference: String,
        module: OperationModule,
        status: OperationStatus,
        notes: String,
    ) = OperationRecordEntity(
        id = id,
        module = module.name,
        reference = reference,
        title = "Pièce test",
        status = status.name,
        notes = notes,
        createdAt = 0L,
    )
}
