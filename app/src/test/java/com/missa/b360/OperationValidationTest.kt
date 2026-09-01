package com.missa.b360

import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.OperationValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles de saisie des modules opérationnels sans accès réseau ni base de démonstration. */
class OperationValidationTest {

    @Test
    fun `une vente exige un libellé et un montant strictement positif`() {
        assertTrue(valid(OperationModule.VENTE, amount = 12_500.0))
        assertFalse(valid(OperationModule.VENTE))
        assertFalse(valid(OperationModule.VENTE, amount = 0.0))
        assertFalse(valid(OperationModule.VENTE, amount = Double.NaN))
        assertFalse(valid(OperationModule.VENTE, title = " ", amount = 1.0))
    }

    @Test
    fun `stock livraison production et services exigent une quantité positive`() {
        listOf(
            OperationModule.STOCK,
            OperationModule.LIVRAISON,
            OperationModule.PRODUCTION,
            OperationModule.SERVICES,
        ).forEach { module ->
            assertTrue(valid(module, quantity = 2.5))
            assertFalse(valid(module))
            assertFalse(valid(module, quantity = 0.0))
        }
    }

    @Test
    fun `une finance exige un sens de trésorerie et un montant`() {
        assertTrue(valid(OperationModule.FINANCES, amount = 8_000.0, direction = OperationDirection.OUT))
        assertFalse(valid(OperationModule.FINANCES, amount = 8_000.0))
        assertFalse(valid(OperationModule.FINANCES, direction = OperationDirection.IN))
    }

    @Test
    fun `un projet peut être suivi avant que son budget soit fixé`() {
        assertTrue(valid(OperationModule.PROJETS))
        assertFalse(valid(OperationModule.PROJETS, amount = -1.0))
    }

    private fun valid(
        module: OperationModule,
        title: String = "Opération test",
        amount: Double? = null,
        quantity: Double? = null,
        direction: OperationDirection = OperationDirection.NONE,
    ): Boolean = OperationValidation.isValid(
        OperationUseCases.CreateParams(
            module = module,
            title = title,
            amount = amount,
            quantity = quantity,
            direction = direction,
        ),
    )
}
