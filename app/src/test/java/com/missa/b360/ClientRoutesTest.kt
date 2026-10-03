package com.missa.b360

import com.missa.b360.ui.clients.ClientRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientRoutesTest {
    @Test fun `les routes du brief sont exposees`() {
        assertEquals("module_clients", ClientRoutes.RACINE)
        assertEquals("module_clients/{id}", ClientRoutes.FICHE)
        assertEquals("module_clients/{id}/compte", ClientRoutes.COMPTE)
        assertEquals("module_clients/{id}/activite", ClientRoutes.ACTIVITE)
        assertEquals("module_clients/relances", ClientRoutes.RELANCES)
        assertEquals("module_clients/import", ClientRoutes.IMPORT)
        assertEquals("module_clients/new", ClientRoutes.NOUVEAU)
        assertEquals("module_clients/{id}/edit", ClientRoutes.EDITION)
    }

    @Test fun `la route historique de l accueil est conservee`() {
        assertEquals("module_clients?create=true", ClientRoutes.liste(creer = true))
        assertEquals("module_clients", ClientRoutes.liste())
        assertEquals("module_clients?create={create}", ClientRoutes.LISTE)
    }

    @Test fun `les routes avec identifiant sont construites sans accolade`() {
        assertEquals("module_clients/42", ClientRoutes.fiche(42))
        assertEquals("module_clients/42/compte", ClientRoutes.compte(42))
        assertEquals("module_clients/42/activite", ClientRoutes.activite(42))
        assertEquals("module_clients/42/edit", ClientRoutes.edition(42))
        listOf(ClientRoutes.fiche(1), ClientRoutes.compte(1), ClientRoutes.activite(1), ClientRoutes.edition(1))
            .forEach { assertTrue(it, !it.contains('{')) }
    }

    @Test fun `les routes litterales ne ressemblent pas a un identifiant numerique`() {
        listOf("relances", "import", "new").forEach { segment ->
            assertTrue(segment.toLongOrNull() == null)
        }
    }
}
