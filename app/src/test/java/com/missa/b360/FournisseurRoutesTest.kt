package com.missa.b360

import com.missa.b360.ui.fournisseurs.FournisseurRoutes
import com.missa.b360.ui.navigation.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FournisseurRoutesTest {
    @Test fun `la route racine reste celle de la barre de modules`() {
        assertEquals(AppModule.FOURNISSEURS.route, FournisseurRoutes.RACINE)
        assertEquals("module_fournisseurs", FournisseurRoutes.RACINE)
    }

    @Test fun `la route historique de l accueil est conservee`() {
        assertEquals("module_fournisseurs?create={create}", FournisseurRoutes.LISTE)
    }

    @Test fun `les routes du brief livrees en F4 sont exposees`() {
        assertEquals("fournisseurs_action", FournisseurRoutes.ACTION)
        assertEquals("fournisseur_fiche/{id}", FournisseurRoutes.FICHE)
        assertEquals("fournisseur_edition/{id}", FournisseurRoutes.EDITION)
    }

    @Test fun `les routes avec identifiant sont construites sans accolade`() {
        assertEquals("fournisseur_fiche/42", FournisseurRoutes.fiche(42))
        assertEquals("fournisseur_edition/42", FournisseurRoutes.edition(42))
        assertEquals("fournisseur_edition/0", FournisseurRoutes.creation())
        listOf(FournisseurRoutes.fiche(1), FournisseurRoutes.edition(1), FournisseurRoutes.creation())
            .forEach { assertFalse(it, it.contains('{')) }
    }
}
