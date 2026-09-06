package com.missa.b360

import com.missa.b360.core.domain.model.ReferentielPackPays
import com.missa.b360.core.util.Iso4217
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce que le choix d'un pays doit entraîner : l'indicatif du champ téléphone
 * suit, y compris quand un numéro est déjà saisi.
 */
class IndicatifPaysTest {

    @Test
    fun `un champ vide recoit l'indicatif du pays choisi`() {
        assertEquals("+33 ", Iso4217.remplacerIndicatif("", null, "+33"))
        assertEquals("+33 ", Iso4217.remplacerIndicatif("   ", "+237", "+33"))
    }

    @Test
    fun `l'indicatif d'un numero deja saisi est echange, le reste est garde`() {
        assertEquals(
            "+33 690 12 34 56",
            Iso4217.remplacerIndicatif("+237 690 12 34 56", "+237", "+33"),
        )
    }

    @Test
    fun `un numero saisi avec un autre indicatif n'est pas touche`() {
        // L'utilisateur a saisi un numéro belge alors que le pays était la
        // France : changer de pays ne doit pas corrompre sa saisie.
        assertEquals(
            "+32 470 11 22 33",
            Iso4217.remplacerIndicatif("+32 470 11 22 33", "+33", "+49"),
        )
    }

    @Test
    fun `sans indicatif connu pour le nouveau pays le numero reste intact`() {
        assertEquals("+237 690 12 34 56", Iso4217.remplacerIndicatif("+237 690 12 34 56", "+237", null))
    }

    @Test
    fun `deux pays partageant un indicatif ne modifient pas le numero`() {
        // Canada et États-Unis sont tous deux en +1.
        assertEquals("+1 202 555 0100", Iso4217.remplacerIndicatif("+1 202 555 0100", "+1", "+1"))
    }

    @Test
    fun `un indicatif seul n'est pas un numero de telephone`() {
        assertTrue(Iso4217.estIndicatifSeul("+237"))
        assertTrue(Iso4217.estIndicatifSeul(" +33 "))
        assertTrue(Iso4217.estIndicatifSeul("+225"))
        assertFalse(Iso4217.estIndicatifSeul("+237 690 12 34 56"))
        assertFalse(Iso4217.estIndicatifSeul(""))
        assertFalse(Iso4217.estIndicatifSeul("690123456"))
    }

    @Test
    fun `chaque pays du referentiel fiscal a un indicatif`() {
        val sansIndicatif = ReferentielPackPays.paysCouverts
            .filter { Iso4217.indicatifTelephone(it) == null }
        assertTrue("pays sans indicatif : $sansIndicatif", sansIndicatif.isEmpty())
    }

    @Test
    fun `l'indicatif est retrouve quelle que soit la casse`() {
        assertEquals("+237", Iso4217.indicatifTelephone("cm"))
        assertEquals("+237", Iso4217.indicatifTelephone(" CM "))
        assertEquals(null, Iso4217.indicatifTelephone("ZZ"))
        assertEquals(null, Iso4217.indicatifTelephone(null))
    }
}
