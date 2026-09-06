package com.missa.b360

import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.domain.model.MentionsLegales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bloc « émetteur » repris sur les factures, relevés et PDF. */
class MentionsLegalesTest {

    private fun entreprise(
        pays: String? = "Cameroun",
        adresse: String? = null,
        telephone: String? = null,
        email: String? = null,
        numeroFiscal: String? = null,
        registreCommerce: String? = null,
    ) = EnterpriseEntity(
        nom = "Boutique Akwa",
        devise = "XAF",
        langue = "fr",
        pays = pays,
        adresse = adresse,
        telephone = telephone,
        email = email,
        numeroFiscal = numeroFiscal,
        registreCommerce = registreCommerce,
    )

    private fun depuis(e: EnterpriseEntity?) =
        MentionsLegales.depuis(e, "Numéro fiscal", "Registre du commerce", "ERP 360")

    @Test
    fun `identifiants prefixes du libelle local du pays`() {
        val mentions = depuis(
            entreprise(numeroFiscal = "P0123456789X", registreCommerce = "RC/DLA/2024/B/1234"),
        )
        assertEquals(
            listOf("NIU : P0123456789X", "RCCM : RC/DLA/2024/B/1234"),
            mentions.identifiants,
        )
    }

    @Test
    fun `libelles francais pour une entreprise francaise`() {
        val mentions = depuis(
            entreprise(
                pays = "France",
                numeroFiscal = "FR12345678901",
                registreCommerce = "12345678900012",
            ),
        )
        assertEquals(
            listOf("N° TVA intracommunautaire : FR12345678901", "SIRET : 12345678900012"),
            mentions.identifiants,
        )
    }

    @Test
    fun `champs vides ou blancs ignores`() {
        val mentions = depuis(entreprise(adresse = "   ", telephone = "+237 690 00 00 00", email = null))
        assertEquals(listOf("+237 690 00 00 00"), mentions.coordonnees)
        assertTrue(mentions.identifiants.isEmpty())
        assertTrue(mentions.complet)
    }

    @Test
    fun `entreprise absente donne un bloc de repli`() {
        val mentions = depuis(null)
        assertEquals("ERP 360", mentions.nom)
        assertFalse(mentions.complet)
        assertEquals("ERP 360", mentions.texte())
    }

    @Test
    fun `texte assemble le nom puis les lignes`() {
        val mentions = depuis(
            entreprise(
                adresse = "Rue Njo-Njo, Bonapriso",
                telephone = "+237 690 00 00 00",
                numeroFiscal = "P0123456789X",
            ),
        )
        assertEquals(
            "Boutique Akwa | Rue Njo-Njo, Bonapriso | +237 690 00 00 00 | NIU : P0123456789X",
            mentions.texte(" | "),
        )
    }
}
