package com.missa.b360

import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.domain.model.ReferentielPackPays
import com.missa.b360.core.domain.model.TypeTaxe
import com.missa.b360.core.util.Iso4217
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cohérence du référentiel « pack pays » : il complète le socle sans le
 * contredire ni redéclarer ce qui vit déjà ailleurs.
 */
class PackPaysTest {

    @Test
    fun `chaque pack a un taux de taxe dans le catalogue`() {
        // Le taux standard n'est pas dupliqué dans le pack : il doit donc exister
        // dans le catalogue, seule source de vérité.
        val sansTaux = ReferentielPackPays.paysCouverts.filterNot { code ->
            Iso4217.TAXES_SUGGEREES.containsKey(code)
        }
        assertTrue("Pays sans taux au catalogue : $sansTaux", sansTaux.isEmpty())
    }

    @Test
    fun `un pays sans taxe a un taux nul et inversement`() {
        ReferentielPackPays.TABLE.values.forEach { pack ->
            val taux = Iso4217.TAXES_SUGGEREES.getValue(pack.code).tauxParDefaut
            if (pack.typeTaxe == TypeTaxe.AUCUNE) {
                assertEquals("Taxe inattendue pour ${pack.code}", 0.0, taux, 0.0)
                assertTrue(pack.tauxReduits.isEmpty())
            }
        }
    }

    @Test
    fun `chaque pack a une devise et des identifiants legaux`() {
        ReferentielPackPays.TABLE.values.forEach { pack ->
            assertNotNull("Devise absente pour ${pack.code}", Iso4217.deviseDuPays(pack.code))
            val regles = ReferentielFiscal.regles(pack.code, "Fiscal", "Registre")
            assertTrue("Aucun identifiant pour ${pack.code}", regles.isNotEmpty())
        }
    }

    @Test
    fun `les taux du pack restent dans des bornes plausibles`() {
        ReferentielPackPays.TABLE.values.forEach { pack ->
            assertTrue("IS hors bornes pour ${pack.code}", pack.impotSocietes in 0.0..60.0)
            assertTrue("IR hors bornes pour ${pack.code}", pack.impotRevenuMax in 0.0..60.0)
            pack.tauxReduits.forEach { reduit ->
                assertTrue("Taux réduit hors bornes pour ${pack.code}", reduit in 0.0..30.0)
            }
            pack.impotSocietesMinimum?.let { minimum ->
                assertTrue("Minimum hors bornes pour ${pack.code}", minimum in 0.0..10.0)
            }
        }
    }

    @Test
    fun `le pack camerounais porte le detail attendu`() {
        val cm = ReferentielPackPays.pack("cm")
        assertNotNull(cm)
        requireNotNull(cm)
        assertEquals(TypeTaxe.TVA, cm.typeTaxe)
        assertEquals(19.25, Iso4217.TAXES_SUGGEREES.getValue("CM").tauxParDefaut, 0.0)
        assertEquals(33.0, cm.impotSocietes, 0.0)
        assertEquals("XAF", Iso4217.deviseDuPays("CM"))
    }

    @Test
    fun `les dispositifs de facturation electronique sont nommes`() {
        ReferentielPackPays.TABLE.values.mapNotNull { it.eFacturation }.forEach { eFacture ->
            assertTrue(eFacture.systeme.isNotBlank())
        }
    }
}
