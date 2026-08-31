package com.missa.b360

import com.missa.b360.core.domain.usecase.ClientValidation
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
