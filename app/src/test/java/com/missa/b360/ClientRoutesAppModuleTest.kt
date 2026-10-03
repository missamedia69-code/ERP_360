package com.missa.b360

import com.missa.b360.ui.clients.ClientRoutes
import com.missa.b360.ui.home.AccueilActionKeys
import com.missa.b360.ui.home.HomeNavigation
import com.missa.b360.ui.navigation.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Régression : la liste est enregistrée sous `ClientRoutes.LISTE`, alors que la barre de modules,
 * l'Accueil, Ventes et les notifications naviguent vers `AppModule.CLIENTS.route`. Si les deux
 * divergent, toucher « Nouveau client » ferme l'application (route introuvable dans le graphe).
 */
class ClientRoutesAppModuleTest {
    @Test fun `la racine du module Clients est celle de la barre de modules`() {
        assertEquals(AppModule.CLIENTS.route, ClientRoutes.RACINE)
    }

    @Test fun `la liste accepte la navigation de la barre de modules`() {
        assertTrue(ClientRoutes.LISTE.startsWith(AppModule.CLIENTS.route + "?"))
        assertEquals(AppModule.CLIENTS.route, ClientRoutes.liste())
    }

    @Test fun `l action client de l Accueil ouvre la creation par la route de la liste`() {
        assertEquals(ClientRoutes.liste(creer = true), HomeNavigation.quickAction(AccueilActionKeys.CLIENT))
    }
}
