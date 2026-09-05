package com.missa.b360

import com.missa.b360.core.domain.model.SaleErrorCode
import com.missa.b360.core.domain.model.SaleFormCalculator
import com.missa.b360.core.domain.model.SaleFormInput
import com.missa.b360.core.domain.model.SaleFormLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests purs du formule unique de vente : totaux, remise, change, crédit et validations. */
class SaleFormCalculatorTest {

    private fun line(
        id: Long,
        unitPriceCents: Long,
        quantity: Double,
        discountPct: Double = 0.0,
        stock: Double? = 10.0,
    ) = SaleFormLine(
        id = id,
        productId = null,
        name = "Article $id",
        unitPriceCents = unitPriceCents,
        quantity = quantity,
        discountPct = discountPct,
        stockAvailable = stock,
    )

    @Test
    fun `calcule sous total remise livraison tvae et total`() {
        val input = SaleFormInput(
            clientId = 1,
            clientName = "Client",
            walkIn = false,
            lines = listOf(line(1, 10000, 2), line(2, 15000, 3)),
            discountInput = "50",
            discountPercentMode = false,
            deliveryInput = "25",
            taxRate = 19.25,
            paymentMethod = "Espèces",
            isCredit = false,
            receivedInput = "1000",
            paidInput = "",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        // sous-total = (100×2 + 150×3) = 65000 centimes
        assertEquals(65000, calc.subtotalCents)
        assertEquals(5000, calc.discountCents)
        assertEquals(2500, calc.deliveryCents)
        assertEquals(62500, calc.totalCents)
        assertEquals(100000, calc.paidCents)
        // 100000 - 62500 = 37500 centimes rendus
        assertEquals(37500, calc.changeCents)
        assertEquals(0, calc.remainingCents)
        assertNull(SaleFormCalculator.validate(input, calc))
    }

    @Test
    fun `le montant recu insuffisant est refusé en espèces`() {
        val input = SaleFormInput(
            clientId = 1,
            clientName = "Client",
            walkIn = false,
            lines = listOf(line(1, 10000, 2)),
            discountInput = "0",
            discountPercentMode = false,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "Espèces",
            isCredit = false,
            receivedInput = "100",
            paidInput = "",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        assertEquals(20000, calc.totalCents)
        val error = SaleFormCalculator.validate(input, calc)
        assertNotNull(error)
        assertEquals(SaleErrorCode.PAYMENT_INVALID, error?.code)
    }

    @Test
    fun `le paiement partiel et le crédit calculent le reste à payer`() {
        val input = SaleFormInput(
            clientId = 1,
            clientName = "Client",
            walkIn = false,
            lines = listOf(line(1, 10000, 4)),
            discountInput = "0",
            discountPercentMode = false,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "Crédit",
            isCredit = true,
            receivedInput = "",
            paidInput = "150",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        assertEquals(40000, calc.totalCents)
        assertEquals(15000, calc.paidCents)
        assertEquals(25000, calc.remainingCents)
        assertNull(SaleFormCalculator.validate(input, calc))
    }

    @Test
    fun `un surpaiement au crédit est refusé`() {
        val input = SaleFormInput(
            clientId = 1,
            clientName = "Client",
            walkIn = false,
            lines = listOf(line(1, 10000, 2)),
            discountInput = "0",
            discountPercentMode = false,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "Crédit",
            isCredit = true,
            receivedInput = "",
            paidInput = "500",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        val error = SaleFormCalculator.validate(input, calc)
        assertNotNull(error)
        assertEquals(SaleErrorCode.PAYMENT_INVALID, error?.code)
    }

    @Test
    fun `un panier vide est refusé`() {
        val input = SaleFormInput(
            clientId = 1,
            clientName = "Client",
            walkIn = false,
            lines = emptyList(),
            discountInput = "0",
            discountPercentMode = false,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "Espèces",
            isCredit = false,
            receivedInput = "0",
            paidInput = "",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        assertEquals(SaleErrorCode.EMPTY_CART, SaleFormCalculator.validate(input, calc)?.code)
    }

    @Test
    fun `une remise en pourcentage est bornée à cent pour cent`() {
        val input = SaleFormInput(
            clientId = null,
            walkIn = true,
            lines = listOf(line(1, 10000, 3)),
            discountInput = "120",
            discountPercentMode = true,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "Espèces",
            isCredit = false,
            receivedInput = "300",
            paidInput = "",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        assertEquals(30000, calc.subtotalCents)
        assertEquals(30000, calc.discountCents)
        assertEquals(0, calc.totalCents)
    }

    @Test
    fun `un brouillon sans client ni mode de paiement est accepté`() {
        val input = SaleFormInput(
            clientId = null,
            walkIn = false,
            lines = listOf(line(1, 10000, 1)),
            discountInput = "0",
            discountPercentMode = false,
            deliveryInput = "0",
            taxRate = 0.0,
            paymentMethod = "",
            isCredit = false,
            receivedInput = "",
            paidInput = "",
            devise = "XAF",
        )
        val calc = SaleFormCalculator.calculate(input)
        assertNull(SaleFormCalculator.validate(input, calc, allowIncomplete = true))
    }
}
