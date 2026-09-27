package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.FournisseurAchatMetrics
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class FournisseurAchatMetricsTest {
    private fun piece(
        id: Long,
        reference: String,
        notes: String,
        status: String = OperationStatus.VALIDATED.name,
    ) = OperationRecordEntity(
        id = id,
        module = OperationModule.ACHATS.name,
        reference = reference,
        title = reference,
        status = status,
        notes = notes,
        createdAt = id * 100L,
    )

    private fun facture(fournisseurId: Long, total: Double, paye: Double) = PurchaseRecordPayload(
        supplierId = fournisseurId,
        supplierName = "Fournisseur $fournisseurId",
        lines = listOf(PurchaseLine(id = 1, name = "Matière", unitPrice = total, quantity = 1.0)),
        subtotal = total,
        taxRate = 0.0,
        taxAmount = 0.0,
        total = total,
        paymentMethod = "Virement",
        paidAmount = paye,
    )

    @Test
    fun `les indicateurs facture utilisent le contenu plutot que le prefixe`() {
        val pieces = listOf(
            piece(1, "FFR-2026-0001", PurchaseRecordCodec.encode(facture(7, 120.0, 20.0))),
            piece(2, "FA-OLD-0002", PurchaseRecordCodec.encode(facture(8, 500.0, 0.0))),
            piece(3, "FFR-2026-0003", PurchaseRecordCodec.encode(facture(7, 40.0, 40.0)), OperationStatus.DRAFT.name),
        )

        assertEquals(600.0, FournisseurAchatMetrics.soldeTotal(pieces), 0.001)
        val resume = FournisseurAchatMetrics.pourFournisseur(7, pieces)
        assertEquals(120.0, resume.montantAchete, 0.001)
        assertEquals(100.0, resume.solde, 0.001)
        assertEquals(100L, resume.derniereFacture ?: -1L)
    }

    @Test
    fun `une commande partiellement recue reste ouverte puis se solde`() {
        val commande = piece(
            id = 10,
            reference = "B-2026-0010",
            notes = CommandeAchatCodec.encode(
                CommandeAchatPayload(
                    supplierId = 7,
                    supplierName = "Fournisseur 7",
                    lines = listOf(CommandeAchatLigne(id = 1, name = "Matière", quantity = 10.0, unitPrice = 2.0, productId = 55)),
                ),
            ),
        )
        fun reception(quantite: Double, id: Long) = piece(
            id = id,
            reference = "R-2026-$id",
            notes = ReceptionCodec.encode(
                ReceptionPayload(
                    commandeRecordId = 10,
                    commandeReference = "B-2026-0010",
                    supplierId = 7,
                    supplierName = "Fournisseur 7",
                    lignes = listOf(ReceptionLigne(productId = 55, name = "Matière", quantiteCommandee = 10.0, quantiteRecue = quantite)),
                ),
            ),
        )

        assertEquals(1, FournisseurAchatMetrics.commandesOuvertes(listOf(commande, reception(4.0, 11))))
        assertEquals(0, FournisseurAchatMetrics.commandesOuvertes(listOf(commande, reception(4.0, 11), reception(6.0, 12))))
    }
}
