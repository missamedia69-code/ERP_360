package com.missa.b360

import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Test

/** Les totaux de vente sont calculés localement à partir des seules lignes saisies. */
class SaleCalculatorTest {

    @Test
    fun `calcule le total TTC avec remise livraison et part de TVA incluse`() {
        val totals = SaleCalculator.calculate(
            lines = listOf(
                SaleLine(id = 1, name = "Article A", unitPrice = 12_500.0, quantity = 2.0),
                SaleLine(id = 2, name = "Article B", unitPrice = 1_850.0, quantity = 3.0),
            ),
            discount = 500.0,
            delivery = 1_000.0,
            taxRate = 19.25,
        )

        assertEquals(30_550.0, totals.subtotal, 0.001)
        assertEquals(31_050.0, totals.total, 0.001)
        assertEquals(5_012.264, totals.taxAmount, 0.001)
    }

    @Test
    fun `le détail d une facture est sérialisé puis relu pour reprendre un brouillon`() {
        val payload = SaleRecordPayload(
            clientId = 42,
            clientName = "Client test",
            lines = listOf(SaleLine(id = 7, name = "Service", unitPrice = 2_500.0, quantity = 2.0)),
            subtotal = 5_000.0,
            discount = 0.0,
            delivery = 0.0,
            taxRate = 19.25,
            taxAmount = 808.0,
            total = 5_000.0,
            paymentMethod = "Espèces",
            paidAmount = 5_000.0,
            note = "À remettre demain",
        )

        val restored = SaleRecordCodec.decode(SaleRecordCodec.encode(payload))

        assertEquals(payload, restored)
    }

    @Test
    fun `borne la remise et ignore les montants négatifs`() {
        val totals = SaleCalculator.calculate(
            lines = listOf(SaleLine(id = 1, name = "Article", unitPrice = 100.0, quantity = 1.0)),
            discount = 500.0,
            delivery = -10.0,
            taxRate = 0.0,
        )

        assertEquals(100.0, totals.discount, 0.001)
        assertEquals(0.0, totals.delivery, 0.001)
        assertEquals(0.0, totals.total, 0.001)
    }
}
