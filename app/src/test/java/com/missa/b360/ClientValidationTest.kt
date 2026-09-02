package com.missa.b360

import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.Iso4217
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles partagées par la création et l'édition du module Clients. */
class ClientValidationTest {

    @Test
    fun `la normalisation évite de contourner un doublon par des espaces`() {
        assertEquals("690000000", ClientValidation.normaliseTelephone("  690000000  "))
        assertEquals("Client test", ClientValidation.normaliseNom(" Client test "))
    }

    @Test
    fun `le téléphone accepte les formats internationaux et rejette les lettres`() {
        assertTrue(ClientValidation.telephoneEstValide("690 00-00-00"))
        assertTrue(ClientValidation.telephoneEstValide("+237 (690) 00-00-00"))
        assertEquals("+237690000000", ClientValidation.normaliseTelephone("+237 (690) 00-00-00"))
        assertEquals("690000000", ClientValidation.filtrerTelephonePourSaisie("690abc000000"))
        assertFalse(ClientValidation.telephoneEstValide("690abc000000"))
        assertFalse(ClientValidation.telephoneEstValide("12345"))
        assertFalse(ClientValidation.telephoneEstValide("1234567890123456"))
    }

    @Test
    fun `un indicatif pays par défaut est appliqué au numéro local`() {
        assertEquals("+237", Iso4217.indicatifTelephone("CM"))
        assertEquals("CM", Iso4217.codePaysDepuisNom("Cameroun"))
        assertEquals("CM", Iso4217.codePaysDepuisTelephone("+237690000000"))
        assertEquals(
            "+237690000000",
            ClientValidation.telephoneAvecIndicatif("690 00-00-00", "+237"),
        )
        assertEquals(
            "690000000",
            ClientValidation.telephoneSansIndicatif("+237690000000", "+237"),
        )
        assertTrue(Iso4217.paysAvecIndicatif(Locale.FRENCH).size >= 240)
    }

    @Test
    fun `email facultatif doit être valide lorsqu il est renseigné`() {
        assertTrue(ClientValidation.emailEstValide(null))
        assertTrue(ClientValidation.emailEstValide(" contact@example.com "))
        assertEquals("contact@example.com", ClientValidation.normaliseEmail(" Contact@Example.com "))
        assertFalse(ClientValidation.emailEstValide("contact@"))
        assertFalse(ClientValidation.emailEstValide("contact exemple.com"))
        assertFalse(ClientValidation.emailEstValide("contact@@example.com"))
        assertFalse(ClientValidation.emailEstValide(".contact@example.com"))
    }

    @Test
    fun `nom téléphone remise et limite doivent être valides`() {
        assertTrue(
            ClientValidation.coordonneesEtConditionsSontValides(
                nom = "Client test",
                telephone = "690000000",
                remiseDefautPct = 12.5,
                limiteCredit = 50_000.0,
            ),
        )
        assertFalse(
            ClientValidation.coordonneesEtConditionsSontValides(
                nom = " ",
                telephone = "690000000",
                remiseDefautPct = 0.0,
                limiteCredit = null,
            ),
        )
        assertFalse(
            ClientValidation.coordonneesEtConditionsSontValides(
                nom = "Client test",
                telephone = " ",
                remiseDefautPct = 0.0,
                limiteCredit = null,
            ),
        )
        assertFalse(
            ClientValidation.coordonneesEtConditionsSontValides(
                nom = "Client test",
                telephone = "690000000",
                remiseDefautPct = 100.01,
                limiteCredit = null,
            ),
        )
        assertFalse(
            ClientValidation.coordonneesEtConditionsSontValides(
                nom = "Client test",
                telephone = "690000000",
                remiseDefautPct = 0.0,
                limiteCredit = -1.0,
            ),
        )
    }

    @Test
    fun `les champs texte facultatifs sont normalisés avant persistance`() {
        assertEquals("contact@example.com", ClientValidation.normaliseTexte(" contact@example.com "))
        assertNull(ClientValidation.normaliseTexte("   "))
        assertNull(ClientValidation.normaliseTexte(null))
    }
}
