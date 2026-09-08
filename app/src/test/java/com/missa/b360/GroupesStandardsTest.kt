package com.missa.b360

import com.missa.b360.core.data.entity.MethodeValorisation
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.model.GroupesStandards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Groupes d'articles livrés à l'installation. */
class GroupesStandardsTest {

    private val maintenant = 1_000_000_000_000L

    private fun modeles(referentiel: String?) = GroupesStandards.modeles(referentiel, maintenant)
    private fun parCode(referentiel: String?, code: String) =
        modeles(referentiel).first { it.groupe.code == code }

    @Test
    fun `les sept familles courantes sont livrees`() {
        val codes = modeles("SYSCOHADA").map { it.groupe.code }
        assertEquals(
            listOf("MARCH", "MP", "PF", "SE", "CONSO", "SERV", "EQUIP"),
            codes,
        )
    }

    @Test
    fun `chaque groupe porte ses six extensions`() {
        modeles("SYSCOHADA").forEach { modele ->
            assertNotNull(modele.groupe.code, modele.stock)
            assertNotNull(modele.groupe.code, modele.achat)
            assertNotNull(modele.groupe.code, modele.vente)
            assertNotNull(modele.groupe.code, modele.production)
            assertNotNull(modele.groupe.code, modele.maintenance)
            assertNotNull(modele.groupe.code, modele.comptabilite)
        }
    }

    @Test
    fun `une matiere premiere s achete sans se vendre`() {
        val mp = parCode("SYSCOHADA", "MP")
        assertTrue(mp.achat.achetable)
        assertFalse(mp.vente.vendable)
        assertTrue(mp.groupe.stocke)
    }

    @Test
    fun `un produit fini se fabrique et se vend mais ne s achete pas`() {
        val pf = parCode("SYSCOHADA", "PF")
        assertTrue(pf.production.produisible)
        assertTrue(pf.production.nomenclatureRequise)
        assertTrue(pf.vente.vendable)
        assertFalse(pf.achat.achetable)
    }

    @Test
    fun `une prestation ne tient aucun stock`() {
        val serv = parCode("SYSCOHADA", "SERV")
        assertFalse(serv.groupe.stocke)
        assertFalse(serv.groupe.valorise)
        assertTrue(serv.vente.service)
        assertFalse(serv.vente.livraisonRequise)
        assertNull(serv.groupe.methodeValorisation)
    }

    @Test
    fun `un equipement est une immobilisation amortissable et maintenable`() {
        val equip = parCode("SYSCOHADA", "EQUIP")
        assertTrue(equip.groupe.immobilisation)
        assertTrue(equip.maintenance.equipementMaintenable)
        assertTrue(equip.comptabilite.amortissable)
        assertTrue(equip.comptabilite.dureeAmortissementAnnees > 0)
        assertTrue(equip.achat.immobilisable)
        assertFalse(equip.groupe.stocke)
    }

    @Test
    fun `les comptes suivent le referentiel comptable du pays`() {
        // SYSCOHADA classe les marchandises en 311, le plan français en 370.
        assertEquals("311", parCode("SYSCOHADA", "MARCH").stock.compteStock)
        assertEquals("370", parCode("PCG", "MARCH").stock.compteStock)
        assertEquals("601", parCode("SYSCOHADA", "MARCH").achat.compteCharge)
        assertEquals("607", parCode("PCG", "MARCH").achat.compteCharge)
    }

    @Test
    fun `un referentiel inconnu retombe sur SYSCOHADA`() {
        assertEquals(
            parCode("SYSCOHADA", "MARCH").stock.compteStock,
            parCode("REFERENTIEL_INCONNU", "MARCH").stock.compteStock,
        )
        assertEquals("311", parCode(null, "MARCH").stock.compteStock)
    }

    @Test
    fun `tout groupe valorise declare une methode`() {
        modeles("SYSCOHADA").forEach { modele ->
            if (modele.groupe.valorise) {
                assertEquals(
                    modele.groupe.code,
                    MethodeValorisation.CUMP.name,
                    modele.groupe.methodeValorisation,
                )
            } else {
                assertNull(modele.groupe.code, modele.groupe.methodeValorisation)
            }
        }
    }

    @Test
    fun `un groupe non stocke n est jamais valorise`() {
        modeles("SYSCOHADA")
            .filterNot { it.groupe.stocke }
            .forEach { assertFalse(it.groupe.code, it.groupe.valorise) }
    }

    @Test
    fun `chaque nature historique retrouve son groupe`() {
        val codes = modeles("SYSCOHADA").map { it.groupe.code }.toSet()
        GroupesStandards.natureParCode.forEach { (code, nature) ->
            assertTrue("groupe absent : $code", code in codes)
            assertNotNull(nature)
        }
        // Les cinq natures héritées sont toutes couvertes.
        assertEquals(ProductType.entries.size, GroupesStandards.natureParCode.size)
    }

    @Test
    fun `un groupe stocke porte un compte de stock et un compte d ecart`() {
        modeles("SYSCOHADA")
            .filter { it.groupe.stocke }
            .forEach {
                assertNotNull(it.groupe.code, it.stock.compteStock)
                assertNotNull(it.groupe.code, it.stock.compteEcartInventaire)
            }
    }
}
