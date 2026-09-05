package com.missa.b360

import com.missa.b360.core.domain.usecase.ProductInput
import com.missa.b360.core.domain.usecase.ProductValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles de saisie du formulaire produit (spec §7) — logique pure, sans base. */
class ProductValidationTest {

    private fun input(
        nom: String = "Bocal 33 cl",
        prixAchat: Double? = 120.0,
        prixVente: Double? = 250.0,
        remiseMaxPct: Double = 0.0,
        stockMin: Double = 0.0,
        stockSecurite: Double = 0.0,
    ) = ProductInput(
        nom = nom,
        prixAchat = prixAchat,
        prixVente = prixVente,
        remiseMaxPct = remiseMaxPct,
        stockMin = stockMin,
        stockSecurite = stockSecurite,
    )

    @Test
    fun `un produit valide est accepté`() {
        assertTrue(ProductValidation.inputEstValide(input()))
        assertTrue(ProductValidation.inputEstValide(input(nom = "Coca 33 cl", prixVente = null)))
    }

    @Test
    fun `le nom doit faire entre 2 et 120 caractères`() {
        assertFalse(ProductValidation.nomEstValide(" "))
        assertFalse(ProductValidation.nomEstValide("A"))
        assertTrue(ProductValidation.nomEstValide("AB"))
        assertTrue(ProductValidation.nomEstValide("x".repeat(120)))
        assertFalse(ProductValidation.nomEstValide("x".repeat(121)))
    }

    @Test
    fun `les montants doivent être nuls ou positifs et finis`() {
        assertTrue(ProductValidation.montantEstValide(null))
        assertTrue(ProductValidation.montantEstValide(0.0))
        assertTrue(ProductValidation.montantEstValide(1250.5))
        assertFalse(ProductValidation.montantEstValide(-1.0))
        assertFalse(ProductValidation.montantEstValide(Double.NaN))
        assertFalse(ProductValidation.montantEstValide(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `la remise maximale est bornée entre 0 et 100`() {
        assertTrue(ProductValidation.remiseEstValide(0.0))
        assertTrue(ProductValidation.remiseEstValide(100.0))
        assertFalse(ProductValidation.remiseEstValide(100.01))
        assertFalse(ProductValidation.remiseEstValide(-0.1))
    }

    @Test
    fun `les seuils de stock doivent être nuls ou positifs`() {
        assertTrue(ProductValidation.seuilEstValide(null))
        assertTrue(ProductValidation.seuilEstValide(0.0))
        assertTrue(ProductValidation.seuilEstValide(24.0))
        assertFalse(ProductValidation.seuilEstValide(-0.5))
    }

    @Test
    fun `un input avec prix négatif ou remise hors bornes est rejeté`() {
        assertFalse(ProductValidation.inputEstValide(input(prixAchat = -10.0)))
        assertFalse(ProductValidation.inputEstValide(input(remiseMaxPct = 150.0)))
        assertFalse(ProductValidation.inputEstValide(input(stockMin = -1.0)))
        assertFalse(ProductValidation.inputEstValide(input(nom = " ")))
    }
}
