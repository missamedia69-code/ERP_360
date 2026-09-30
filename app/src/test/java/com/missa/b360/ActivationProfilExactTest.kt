package com.missa.b360

import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModuleSousElements
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ProfilConfiguration
import com.missa.b360.ui.navigation.DestinationsFonctions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrat : un pack connu n'affiche ni plus ni moins que sa configuration effective. */
class ActivationProfilExactTest {

    private fun activation(
        profil: ProfilActivite,
        metier: Set<ModuleCode> = emptySet(),
        elements: Map<ModuleCode, Set<String>> = emptyMap(),
    ) = ActivationProfil.calculer(
        profil = profil,
        palier = PalierTaille.P2,
        venteSansStock = profil == ProfilActivite.AV,
        modulesPersonnalises = metier,
        extrasSupport = emptySet(),
        elementsPersonnalises = elements,
    )

    @Test
    fun `chaque pack standard active exactement son metier et son socle`() {
        val standards = listOf(
            ProfilActivite.AV,
            ProfilActivite.ASV,
            ProfilActivite.APSV,
            ProfilActivite.SER,
            ProfilActivite.PRJ,
        )
        standards.forEach { profil ->
            val packMetier = ModulesSocle.metierActifs(
                profil,
                emptySet(),
                autoriserVenteSansStock = profil == ProfilActivite.AV,
            ).toSet()
            val support = ModulesSocle.recommandes(profil, PalierTaille.P2, packMetier)
            assertEquals("modules de $profil", packMetier + support, activation(profil).modulesActifs)
        }
    }

    @Test
    fun `full active tout et personnel ne divulgue aucun module entreprise`() {
        assertEquals(ModuleCode.entries.toSet(), activation(ProfilActivite.FULL).modulesActifs)
        val personnel = activation(ProfilActivite.PERSONNEL)
        assertTrue(personnel.modulesActifs.isEmpty())
        assertFalse(personnel.profil == null)
    }

    @Test
    fun `les fonctions par defaut correspondent exactement au pack`() {
        val profil = ProfilActivite.ASV
        val actif = activation(profil)
        actif.modulesActifs.forEach { module ->
            val attendus = ProfilConfiguration.sousElementsPourModule(profil, module).toSet()
                .ifEmpty { ModuleSousElements.pourModule(module).toSet() }
            assertEquals("fonctions $profil / $module", attendus, actif.elementsActifsPour(module))
        }
    }

    @Test
    fun `une personnalisation remplace les fonctions et accepte aucune fonction`() {
        val seulementClients = activation(
            ProfilActivite.ASV,
            elements = mapOf(ModuleCode.VEN to setOf("Clients")),
        )
        assertEquals(setOf("Clients"), seulementClients.elementsActifsPour(ModuleCode.VEN))

        val aucuneVente = activation(
            ProfilActivite.ASV,
            elements = mapOf(ModuleCode.VEN to emptySet()),
        )
        assertTrue(aucuneVente.elementsActifsPour(ModuleCode.VEN).isEmpty())
        assertTrue(aucuneVente.isModuleActif(ModuleCode.VEN))
    }

    @Test
    fun `le catalogue affiche seulement les fonctions selectionnees`() {
        val actif = activation(
            ProfilActivite.ASV,
            elements = mapOf(ModuleCode.VEN to setOf("Clients", "Devis")),
        )
        assertEquals(
            setOf("Clients", "Devis"),
            DestinationsFonctions.pour(ModuleCode.VEN, actif).map { it.libelle }.toSet(),
        )
        assertTrue(DestinationsFonctions.pour(ModuleCode.PRO, actif).isEmpty())
    }

    @Test
    fun `custom n'active que les modules explicitement choisis`() {
        val actif = activation(ProfilActivite.CUSTOM, setOf(ModuleCode.SER, ModuleCode.PRJ))
        assertEquals(setOf(ModuleCode.SER, ModuleCode.PRJ), actif.modulesActifs)
    }
}
