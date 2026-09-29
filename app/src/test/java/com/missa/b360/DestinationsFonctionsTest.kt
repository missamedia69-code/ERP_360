package com.missa.b360

import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.ui.navigation.DestinationsFonctions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Honnêteté du catalogue : une fonctionnalité n'est « disponible » que si elle
 * ouvre un écran qui fonctionne réellement.
 */
class DestinationsFonctionsTest {

    private fun fonction(libelle: String) =
        ModuleCode.entries.flatMap { DestinationsFonctions.pour(it) }.firstOrNull { it.libelle == libelle }

    @Test
    fun `les fonctions sans ecran reel restent presentees comme prevues`() {
        listOf(
            "Retours clients",
            "Avoirs clients",
            "Grand livre",
            "Balance",
            "Lots / Séries",
            "Réservations de stock",
            "Facturation des prestations",
            "Déclarations de production",
        ).forEach { libelle ->
            val f = fonction(libelle)
            assertTrue("$libelle absent du catalogue", f != null)
            assertFalse("$libelle ne doit pas être annoncé disponible", f!!.disponible)
        }
    }

    @Test
    fun `les fonctions livrees restent routees`() {
        listOf("Devis", "Ordres de fabrication", "Interventions", "Rapprochements bancaires", "Compte de résultat")
            .forEach { libelle -> assertTrue("$libelle doit rester disponible", fonction(libelle)?.disponible == true) }
    }
}
