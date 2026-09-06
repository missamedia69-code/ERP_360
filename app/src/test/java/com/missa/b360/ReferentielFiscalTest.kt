package com.missa.b360

import com.missa.b360.core.domain.model.CleIdentifiant
import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.domain.model.ZoneFiscale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Référentiel fiscal : zones, libellés d'identifiants et contrôles de format. */
class ReferentielFiscalTest {

    private fun regles(code: String?) =
        ReferentielFiscal.regles(code, "Numéro fiscal", "Registre du commerce")

    @Test
    fun `zone deduite de la table puis de l appartenance`() {
        assertEquals(ZoneFiscale.CEMAC, ReferentielFiscal.zone("CM"))
        assertEquals(ZoneFiscale.UEMOA, ReferentielFiscal.zone("ci"))
        assertEquals(ZoneFiscale.UE, ReferentielFiscal.zone("FR"))
        // Pays de l'UE absent de la table détaillée : repli par appartenance.
        assertEquals(ZoneFiscale.UE, ReferentielFiscal.zone("SE"))
        assertEquals(ZoneFiscale.GCC, ReferentielFiscal.zone("QA"))
        assertEquals(ZoneFiscale.AUTRE, ReferentielFiscal.zone(null))
    }

    @Test
    fun `libelles d identifiants propres au pays`() {
        val cameroun = regles("CM")
        assertEquals("NIU", cameroun.first { it.cle == CleIdentifiant.FISCAL }.libelle)
        assertEquals("RCCM", cameroun.first { it.cle == CleIdentifiant.REGISTRE }.libelle)

        val maroc = regles("MA")
        assertEquals("ICE", maroc.first { it.cle == CleIdentifiant.REGISTRE }.libelle)

        val france = regles("FR")
        assertEquals("SIRET", france.first { it.cle == CleIdentifiant.REGISTRE }.libelle)
    }

    @Test
    fun `pays sans fiche detaillee recoit les libelles de sa zone`() {
        val suede = regles("SE")
        assertEquals(
            "N° TVA intracommunautaire",
            suede.first { it.cle == CleIdentifiant.FISCAL }.libelle,
        )
        val benin = regles("BJ")
        assertEquals("RCCM", benin.first { it.cle == CleIdentifiant.REGISTRE }.libelle)
    }

    @Test
    fun `controle de format tolerant sur une valeur vide`() {
        val ice = regles("MA").first { it.cle == CleIdentifiant.REGISTRE }
        assertTrue(ice.estValide(""))
        assertTrue(ice.estValide("   "))
        assertTrue(ice.estValide("001234567000089"))
        assertFalse(ice.estValide("12345"))
    }

    @Test
    fun `exemples fournis conformes a leur propre motif`() {
        listOf("CM", "CI", "SN", "MA", "FR", "AE", "SA", "US", "IN", "BR").forEach { code ->
            regles(code).forEach { regle ->
                assertTrue(
                    "Exemple non conforme pour $code / ${regle.libelle}",
                    regle.estValide(regle.exemple),
                )
            }
        }
    }

    @Test
    fun `table chargee sans perte de lignes`() {
        assertEquals(76, ReferentielFiscal.paysDetailles)
    }
}
