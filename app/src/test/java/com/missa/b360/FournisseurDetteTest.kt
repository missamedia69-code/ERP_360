package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.FournisseurAchatMetrics
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FournisseurDetteTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun payload(fournisseur: Long, total: Double, paye: Double, echeanceJour: Long? = null) = PurchaseRecordPayload(
        supplierId = fournisseur, supplierName = "F$fournisseur",
        lines = listOf(PurchaseLine(id = 1, name = "x", unitPrice = total, quantity = 1.0)),
        subtotal = total, taxRate = 0.0, taxAmount = 0.0, total = total,
        paymentMethod = "Virement", paidAmount = paye, dateEcheance = echeanceJour?.let { it * jour },
    )

    private fun piece(id: Long, jourCreation: Long, p: PurchaseRecordPayload, statut: String = OperationStatus.VALIDATED.name) =
        OperationRecordEntity(
            id = id, module = OperationModule.ACHATS.name, reference = "FFR-$id", title = "FFR-$id",
            status = statut, notes = PurchaseRecordCodec.encode(p), createdAt = jourCreation * jour,
        )

    private val pieces = listOf(
        piece(1, 900, payload(7, 100.0, 40.0)),
        piece(2, 950, payload(7, 200.0, 0.0, echeanceJour = 1_010)),
        piece(3, 960, payload(7, 50.0, 80.0)),
        piece(4, 970, payload(7, 70.0, 0.0), OperationStatus.DRAFT.name),
        piece(5, 800, payload(8, 30.0, 0.0), OperationStatus.CANCELLED.name),
        piece(6, 990, payload(8, 25.0, 0.0, echeanceJour = 995)),
    )

    @Test
    fun `reste du ignore les valeurs invalides et plafonne le regle`() {
        assertEquals(60.0, FournisseurAchatMetrics.resteDu(payload(1, 100.0, 40.0)), 0.0)
        assertEquals(0.0, FournisseurAchatMetrics.resteDu(payload(1, 50.0, 80.0)), 0.0)
        assertEquals(100.0, FournisseurAchatMetrics.resteDu(payload(1, 100.0, -20.0)), 0.0)
        assertEquals(100.0, FournisseurAchatMetrics.resteDu(payload(1, 100.0, Double.NaN)), 0.0)
        assertEquals(0.0, FournisseurAchatMetrics.resteDu(payload(1, Double.NaN, 0.0)), 0.0)
        assertEquals(0.0, FournisseurAchatMetrics.resteDu(payload(1, Double.POSITIVE_INFINITY, 0.0)), 0.0)
        assertEquals(0.0, FournisseurAchatMetrics.resteDu(payload(1, -10.0, 0.0)), 0.0)
    }

    @Test
    fun `dette d un fournisseur avec echeance saisie ou deduite`() {
        val d = FournisseurAchatMetrics.dette(pieces, now, fournisseurId = 7, joursEcheance = { 30 })
        assertEquals(260.0, d.dette, 0.001)
        assertEquals(60.0, d.enRetard, 0.001)
        assertEquals(2, d.nbFacturesOuvertes)
        assertEquals(70, d.joursRetardMax)
        assertEquals(1_010L * jour, d.prochaineEcheanceAt)
        assertEquals(60.0, d.balance.montant(AgingBucket.JOURS_61_90), 0.001)
        assertEquals(200.0, d.balance.montant(AgingBucket.NON_ECHU), 0.001)
        assertEquals(listOf(1L, 2L), d.factures.map { it.recordId })
        assertEquals(930L * jour, d.factures[0].dueAt)
    }

    @Test
    fun `dette globale somme tous les fournisseurs et exclut brouillons et annulees`() {
        val d = FournisseurAchatMetrics.dette(pieces, now, joursEcheance = { 30 })
        assertEquals(285.0, d.dette, 0.001)
        assertEquals(3, d.nbFacturesOuvertes)
        assertEquals(285.0, FournisseurAchatMetrics.soldeTotal(pieces), 0.001)
    }

    @Test
    fun `echeance saisie depassee compte les jours de retard`() {
        val d = FournisseurAchatMetrics.dette(pieces, now, fournisseurId = 8)
        assertEquals(1, d.nbFacturesOuvertes)
        assertEquals(5, d.joursRetardMax)
        assertEquals(25.0, d.enRetard, 0.001)
        assertNull(d.prochaineEcheanceAt)
    }

    @Test
    fun `aucune facture ouverte donne une dette vide`() {
        val d = FournisseurAchatMetrics.dette(emptyList(), now)
        assertEquals(0.0, d.dette, 0.0)
        assertEquals(0, d.nbFacturesOuvertes)
        assertEquals(0, d.joursRetardMax)
        assertNull(d.prochaineEcheanceAt)
    }

    @Test
    fun `le resume fournisseur et la dette donnent le meme solde`() {
        val resume = FournisseurAchatMetrics.pourFournisseur(7, pieces)
        assertEquals(FournisseurAchatMetrics.dette(pieces, now, fournisseurId = 7).dette, resume.solde, 0.001)
    }
}
