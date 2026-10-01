package com.missa.b360

import com.missa.b360.ui.clients.ClientRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientRoutesTest {
    @Test fun `les routes du brief sont exposees`() {
        assertEquals("clients", ClientRoutes.RACINE)
        assertEquals("clients/{id}", ClientRoutes.FICHE)
        assertEquals("clients/{id}/compte", ClientRoutes.COMPTE)
        assertEquals("clients/{id}/activite", ClientRoutes.ACTIVITE)
        assertEquals("clients/relances", ClientRoutes.RELANCES)
        assertEquals("clients/import", ClientRoutes.IMPORT)
        assertEquals("clients/new", ClientRoutes.NOUVEAU)
        assertEquals("clients/{id}/edit", ClientRoutes.EDITION)
    }

    @Test fun `la route historique de l accueil est conservee`() {
        assertEquals("clients?create=true", ClientRoutes.liste(creer = true))
        assertEquals("clients", ClientRoutes.liste())
        assertEquals("clients?create={create}", ClientRoutes.LISTE)
    }

    @Test fun `les routes avec identifiant sont construites sans accolade`() {
        assertEquals("clients/42", ClientRoutes.fiche(42))
        assertEquals("clients/42/compte", ClientRoutes.compte(42))
        assertEquals("clients/42/activite", ClientRoutes.activite(42))
        assertEquals("clients/42/edit", ClientRoutes.edition(42))
        listOf(ClientRoutes.fiche(1), ClientRoutes.compte(1), ClientRoutes.activite(1), ClientRoutes.edition(1))
            .forEach { assertTrue(it, !it.contains('{')) }
    }

    @Test fun `les routes litterales ne ressemblent pas a un identifiant numerique`() {
        listOf("relances", "import", "new").forEach { segment ->
            assertTrue(segment.toLongOrNull() == null)
        }
    }
}
