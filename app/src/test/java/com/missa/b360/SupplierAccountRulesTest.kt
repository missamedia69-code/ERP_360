package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.domain.model.SupplierAccountRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierAccountRulesTest {
    private val base = FournisseurCompteBancaireEntity(
        id = 1, fournisseurId = 7, titulaire = "Société Alpha", banque = "Banque X",
        iban = "CM21 1000 2000 3000 4000 5000 678", numeroCompte = "12345678901",
    )

    @Test fun `un changement de coordonnees de paiement est detecte`() {
        assertTrue(SupplierAccountRules.coordonneesModifiees(base, base.copy(titulaire = "Société Beta")))
        assertTrue(SupplierAccountRules.coordonneesModifiees(base, base.copy(iban = "CM21 1000 2000 3000 4000 5000 999")))
        assertTrue(SupplierAccountRules.coordonneesModifiees(base, base.copy(numeroCompte = "00000000000")))
        assertTrue(SupplierAccountRules.coordonneesModifiees(base, base.copy(numeroMobile = "670000000")))
    }

    @Test fun `banque notes principal et mise en forme ne comptent pas`() {
        assertFalse(SupplierAccountRules.coordonneesModifiees(base, base.copy(banque = "Autre banque", notes = "n", principal = true)))
        assertFalse(SupplierAccountRules.coordonneesModifiees(base, base.copy(iban = "cm21100020003000400050006 78".replace(" ", ""))))
        assertFalse(SupplierAccountRules.coordonneesModifiees(base, base.copy(titulaire = " société alpha ")))
    }

    @Test fun `un compte exige un titulaire et un numero`() {
        assertTrue(SupplierAccountRules.valide(base))
        assertFalse(SupplierAccountRules.valide(base.copy(titulaire = " ")))
        assertFalse(SupplierAccountRules.valide(base.copy(iban = null, numeroCompte = " ", numeroMobile = null)))
        assertTrue(SupplierAccountRules.valide(base.copy(iban = null, numeroCompte = null, numeroMobile = "670000000")))
    }

    @Test fun `le masquage ne laisse que les quatre derniers caracteres`() {
        assertEquals("•••• 5678", SupplierAccountRules.masquer("CM21 1000 2000 5678"))
        assertEquals("••••", SupplierAccountRules.masquer("1234"))
        assertEquals("", SupplierAccountRules.masquer(null))
        assertEquals("", SupplierAccountRules.masquer("   "))
        assertFalse(SupplierAccountRules.masquer("CM2110002000").contains("1000"))
    }
}
