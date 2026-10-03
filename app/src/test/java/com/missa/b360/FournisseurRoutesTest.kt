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
        assertEquals("fournisseur_dossier/{id}", FournisseurRoutes.DOSSIER)
        assertEquals("fournisseur_compte/{id}", FournisseurRoutes.COMPTE)
        assertEquals("fournisseur_conformite/{id}", FournisseurRoutes.CONFORMITE)
        assertEquals("fournisseurs_echeancier", FournisseurRoutes.ECHEANCIER)
        assertEquals("fournisseurs_comparateur?produit={produit}", FournisseurRoutes.COMPARATEUR)
        assertEquals("fournisseurs_documents", FournisseurRoutes.DOCUMENTS)
    }

    @Test fun `les routes avec identifiant sont construites sans accolade`() {
        assertEquals("fournisseur_fiche/42", FournisseurRoutes.fiche(42))
        assertEquals("fournisseur_edition/42", FournisseurRoutes.edition(42))
        assertEquals("fournisseur_edition/0", FournisseurRoutes.creation())
        assertEquals("fournisseur_dossier/42", FournisseurRoutes.dossier(42))
        assertEquals("fournisseur_compte/42", FournisseurRoutes.compte(42))
        assertEquals("fournisseur_conformite/42", FournisseurRoutes.conformite(42))
        assertEquals("fournisseurs_comparateur?produit=7", FournisseurRoutes.comparateur(7))
        assertFalse(FournisseurRoutes.comparateur().contains('{'))
        listOf(
            FournisseurRoutes.fiche(1), FournisseurRoutes.edition(1), FournisseurRoutes.creation(),
            FournisseurRoutes.dossier(1), FournisseurRoutes.compte(1), FournisseurRoutes.conformite(1),
        )
            .forEach { assertFalse(it, it.contains('{')) }
    }
}
