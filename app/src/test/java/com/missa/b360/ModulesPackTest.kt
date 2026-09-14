package com.missa.b360

import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ProfilConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles du « pack » de modules : ce que le profil impose, ce que l'utilisateur
 * peut ajouter, et ce qui survit à un changement de profil ou d'effectif.
 *
 * Ces règles décident de la tête qu'aura l'application entière après
 * l'installation ; elles méritaient un filet, d'autant qu'elles ne dépendent
 * d'aucune API Android.
 */
class ModulesPackTest {

    @Test
    fun `le pack d'un profil reprend exactement ses modules metier`() {
        for (profil in ProfilActivite.entries - ProfilActivite.CUSTOM) {
            val pack = ModulesSocle.metierDuPack(profil)
            assertEquals(
                "pack métier de $profil",
                ModulesSocle.filtrerMetier(ProfilConfiguration.modulesPourProfil(profil)).toSet(),
                pack,
            )
            assertTrue("$profil doit apporter au moins un module métier", pack.isNotEmpty())
        }
    }

    @Test
    fun `le profil personnalise n'impose aucun module`() {
        assertTrue(ModulesSocle.metierDuPack(ProfilActivite.CUSTOM).isEmpty())
        assertTrue(ModulesSocle.metierDuPack(null).isEmpty())
    }

    @Test
    fun `un module ajoute a la main s'ajoute au pack sans le remplacer`() {
        val pack = ModulesSocle.metierDuPack(ProfilActivite.SER)
        val actifs = ModulesSocle.metierActifs(ProfilActivite.SER, pack + ModuleCode.STK)
        assertTrue("le pack reste entier", actifs.containsAll(pack))
        assertTrue("l'ajout est pris en compte", ModuleCode.STK in actifs)
    }

    @Test
    fun `retirer un module du pack de la selection ne le desactive pas`() {
        // L'interface verrouille ces modules ; le domaine doit tenir la même
        // ligne même si on lui passe une sélection amputée.
        val pack = ModulesSocle.metierDuPack(ProfilActivite.ASV)
        val actifs = ModulesSocle.metierActifs(ProfilActivite.ASV, emptySet())
        assertEquals(pack, actifs.toSet())
    }

    @Test
    fun `le profil personnalise n'active que ce qui est coche`() {
        val actifs = ModulesSocle.metierActifs(
            ProfilActivite.CUSTOM,
            setOf(ModuleCode.VEN, ModuleCode.CPT),
        )
        assertEquals(listOf(ModuleCode.VEN), actifs)
    }

    @Test
    fun `comptabilite et reporting sont recommandes quel que soit le profil`() {
        for (profil in ProfilActivite.entries) {
            val recommandes = ModulesSocle.recommandes(
                profil,
                PalierTaille.P1,
                ModulesSocle.metierDuPack(profil),
            )
            assertTrue("comptabilité pour $profil", ModuleCode.CPT in recommandes)
            assertTrue("reporting pour $profil", ModuleCode.REP in recommandes)
        }
    }

    @Test
    fun `la production entraine qualite et maintenance`() {
        val recommandes = ModulesSocle.recommandes(
            ProfilActivite.APSV,
            PalierTaille.P2,
            listOf(ModuleCode.PRO, ModuleCode.VEN),
        )
        assertTrue(ModuleCode.QUA in recommandes)
        assertTrue(ModuleCode.MAI in recommandes)
    }

    @Test
    fun `les ressources humaines n'arrivent qu'avec l'effectif ou les projets`() {
        val petiteEquipe = ModulesSocle.recommandes(
            ProfilActivite.AV,
            PalierTaille.P2,
            listOf(ModuleCode.ACH, ModuleCode.VEN),
        )
        assertFalse(ModuleCode.RH in petiteEquipe)

        val grandeEquipe = ModulesSocle.recommandes(
            ProfilActivite.AV,
            PalierTaille.P4,
            listOf(ModuleCode.ACH, ModuleCode.VEN),
        )
        assertTrue(ModuleCode.RH in grandeEquipe)
    }

    @Test
    fun `les recommandations ne contiennent que des modules support`() {
        for (profil in ProfilActivite.entries) {
            for (palier in PalierTaille.entries) {
                val recommandes = ModulesSocle.recommandes(
                    profil,
                    palier,
                    ModulesSocle.metierDuPack(profil),
                )
                assertTrue(
                    "$profil / $palier ne doit recommander que du support",
                    ModulesSocle.support.containsAll(recommandes),
                )
            }
        }
    }

    @Test
    fun `sans profil rien n'est active`() {
        assertTrue(ModulesSocle.recommandes(null, PalierTaille.P3, emptyList()).isEmpty())
        assertTrue(ModulesPersonnalises.modulesActifs(null, emptySet(), emptySet()).isEmpty())
    }

    @Test
    fun `le profil complet active les quatorze modules`() {
        val actifs = ModulesPersonnalises.modulesActifs(
            ProfilActivite.FULL,
            ModulesSocle.metierDuPack(ProfilActivite.FULL),
            ModulesSocle.support,
        )
        assertEquals(ModuleCode.entries.size, actifs.size)
    }

    @Test
    fun `les modules actifs sont l'union du metier et du support, sans doublon`() {
        val actifs = ModulesPersonnalises.modulesActifs(
            ProfilActivite.ASV,
            ModulesSocle.metierDuPack(ProfilActivite.ASV) + ModuleCode.PRJ,
            setOf(ModuleCode.CPT, ModuleCode.REP, ModuleCode.CRM),
        )
        assertEquals("aucun doublon", actifs.size, actifs.distinct().size)
        assertTrue(ModuleCode.PRJ in actifs)
        assertTrue(ModuleCode.CRM in actifs)
        assertTrue(actifs.containsAll(ModulesSocle.metierDuPack(ProfilActivite.ASV)))
    }
}
