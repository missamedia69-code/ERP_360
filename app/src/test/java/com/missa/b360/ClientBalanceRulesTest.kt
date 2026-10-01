package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ClientBalanceRules
import com.missa.b360.core.domain.model.ClientLedgerItem
import com.missa.b360.core.domain.model.ClientMetricsRules
import com.missa.b360.core.domain.model.ClientPaymentItem
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientBalanceRulesTest {
    private val day = ClientMetricsRules.DAY_MS
    private val now = 2_000_000_000_000L

    private fun facture(id: Long, total: Double, paid: Double, joursAvant: Long) =
        ClientLedgerItem(total, paid, now - joursAvant * day, recordId = id)

    private fun avoir(id: Long, total: Double, source: Long, joursAvant: Long) =
        ClientLedgerItem(total, 0.0, now - joursAvant * day, creditNote = true, recordId = id, sourceRecordId = source)

    @Test fun `sans paiement le compte reprend les regles de metriques`() {
        val items = listOf(facture(1, 100.0, 20.0, 40))
        val compte = ClientBalanceRules.calculer(items, emptyList(), 30, now)
        val metrics = ClientMetricsRules.calculate(items, 30, now)
        assertEquals(metrics.outstanding, compte.encours, 1e-9)
        assertEquals(metrics.overdueAmount, compte.enRetard, 1e-9)
        assertEquals(80.0, compte.encours, 1e-9)
        assertEquals(10, compte.joursRetardMax)
    }

    @Test fun `un paiement cible reduit la facture designee`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 20.0, 40)),
            listOf(ClientPaymentItem(30.0, invoiceRecordId = 1)),
            30, now,
        )
        assertEquals(50.0, compte.encours, 1e-9)
        assertEquals(50.0, compte.enRetard, 1e-9)
    }

    @Test fun `un paiement libre solde les echeances les plus anciennes d abord`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 0.0, 60), facture(2, 100.0, 0.0, 10)),
            listOf(ClientPaymentItem(130.0)),
            30, now,
        )
        assertEquals(70.0, compte.encours, 1e-9)
        assertEquals(0.0, compte.enRetard, 1e-9)
        assertEquals(0, compte.joursRetardMax)
    }

    @Test fun `l excedent d un paiement cible passe aux plus anciennes echeances`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 50.0, 0.0, 5), facture(2, 100.0, 0.0, 50)),
            listOf(ClientPaymentItem(80.0, invoiceRecordId = 1)),
            30, now,
        )
        assertEquals(70.0, compte.encours, 1e-9)
        assertEquals(70.0, compte.enRetard, 1e-9)
        assertEquals(20, compte.joursRetardMax)
    }

    @Test fun `une contre-passation annule l encaissement`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 0.0, 10)),
            listOf(ClientPaymentItem(100.0, 1), ClientPaymentItem(-100.0, 1)),
            30, now,
        )
        assertEquals(100.0, compte.encours, 1e-9)
    }

    @Test fun `une contre-passation ne rend jamais le paye negatif`() {
        val imputes = ClientBalanceRules.appliquerPaiements(
            listOf(facture(1, 100.0, 0.0, 10)),
            listOf(ClientPaymentItem(-40.0, 1)),
        )
        assertEquals(0.0, imputes[0].paid, 1e-9)
    }

    @Test fun `les montants non finis ou nuls sont ignores`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 0.0, 10)),
            listOf(
                ClientPaymentItem(Double.NaN),
                ClientPaymentItem(Double.POSITIVE_INFINITY, 1),
                ClientPaymentItem(0.0),
            ),
            30, now,
        )
        assertEquals(100.0, compte.encours, 1e-9)
    }

    @Test fun `un surplus de paiements ne cree pas de creance negative`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 0.0, 10)),
            listOf(ClientPaymentItem(500.0)),
            30, now,
        )
        assertEquals(0.0, compte.encours, 1e-9)
        assertEquals(0.0, compte.enRetard, 1e-9)
    }

    @Test fun `un avoir reduit l encours et le chiffre d affaires`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 0.0, 10), avoir(2, 40.0, source = 1, joursAvant = 5)),
            emptyList(), 30, now,
        )
        assertEquals(60.0, compte.encours, 1e-9)
        assertEquals(60.0, compte.ca12Mois, 1e-9)
        assertEquals(1, compte.nbVentes)
        assertEquals(now - 10 * day, compte.derniereVenteAt)
    }

    @Test fun `le chiffre d affaires 12 mois ignore les ventes de plus d un an`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 100.0, 400), facture(2, 50.0, 50.0, 10)),
            emptyList(), 30, now,
        )
        assertEquals(50.0, compte.ca12Mois, 1e-9)
        assertEquals(2, compte.nbVentes)
    }

    @Test fun `le chiffre d affaires 12 mois reste positif`() {
        val compte = ClientBalanceRules.calculer(
            listOf(facture(1, 100.0, 100.0, 400), avoir(2, 30.0, source = 1, joursAvant = 5)),
            emptyList(), 30, now,
        )
        assertEquals(0.0, compte.ca12Mois, 1e-9)
    }

    @Test fun `un client sans piece a un compte vide`() {
        val compte = ClientBalanceRules.calculer(emptyList(), emptyList(), 30, now)
        assertEquals(0.0, compte.encours, 1e-9)
        assertEquals(0, compte.nbVentes)
        assertNull(compte.derniereVenteAt)
    }

    @Test fun `les avoirs ne sont pas modifies par l imputation des paiements`() {
        val items = listOf(facture(1, 100.0, 0.0, 10), avoir(2, 40.0, source = 1, joursAvant = 5))
        val imputes = ClientBalanceRules.appliquerPaiements(items, listOf(ClientPaymentItem(30.0, 1)))
        assertEquals(40.0, imputes[1].total, 1e-9)
        assertEquals(0.0, imputes[1].paid, 1e-9)
        assertTrue(imputes[1].creditNote)
        assertEquals(30.0, imputes[0].paid, 1e-9)
    }

    private fun piece(
        id: Long,
        payload: SaleRecordPayload?,
        status: OperationStatus = OperationStatus.VALIDATED,
        notes: String? = payload?.let { SaleRecordCodec.encode(it) },
    ) = OperationRecordEntity(
        id = id,
        module = OperationModule.VENTE.name,
        reference = "V-$id",
        title = "Vente $id",
        status = status.name,
        notes = notes,
        createdAt = now - id * day,
    )

    private fun payload(clientId: Long, total: Double, paid: Double, source: Long? = null) = SaleRecordPayload(
        clientId = clientId,
        clientName = "Client $clientId",
        lines = emptyList(),
        subtotal = total,
        discount = 0.0,
        delivery = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0,
        total = total,
        paymentMethod = "CASH",
        paidAmount = paid,
        sourceRecordId = source,
    )

    @Test fun `le grand livre regroupe ventes et avoirs valides par client`() {
        val ledger = ClientBalanceRules.ledgerParClient(
            listOf(
                piece(1, payload(5, 100.0, 40.0)),
                piece(2, payload(5, 30.0, 0.0, source = 1)),
                piece(3, payload(6, 10.0, 10.0)),
                piece(4, payload(5, 999.0, 0.0), status = OperationStatus.DRAFT),
                piece(5, payload(5, 999.0, 0.0), status = OperationStatus.CANCELLED),
                piece(6, payload(0, 50.0, 50.0)),
                piece(7, null, notes = "pas du json"),
                piece(8, null, notes = null),
            ),
        )
        assertEquals(setOf(5L, 6L), ledger.keys)
        val client5 = ledger.getValue(5L)
        assertEquals(2, client5.size)
        assertEquals(1L, client5[0].recordId)
        assertFalse(client5[0].creditNote)
        assertEquals(40.0, client5[0].paid, 1e-9)
        assertTrue(client5[1].creditNote)
        assertEquals(1L, client5[1].sourceRecordId)
    }

    @Test fun `le grand livre ecarte les montants invalides`() {
        val ledger = ClientBalanceRules.ledgerParClient(
            listOf(
                piece(1, payload(5, -10.0, 0.0)),
                piece(3, payload(5, 10.0, -1.0)),
            ),
        )
        assertTrue(ledger.isEmpty())
    }
}
