package com.missa.b360

import com.missa.b360.core.notifications.NotificationRoutes
import com.missa.b360.ui.clients.ClientRoutes
import com.missa.b360.ui.navigation.AppModule
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientNotificationRoutesTest {
    @Test fun `une promesse non tenue ouvre l ecran des relances`() {
        assertEquals(ClientRoutes.RELANCES, NotificationRoutes.pourType("CLIENT_PROMESSE"))
    }

    @Test fun `un depassement de limite ouvre la liste des clients`() {
        assertEquals(AppModule.CLIENTS.route, NotificationRoutes.pourType("CLIENT_LIMITE"))
    }

    @Test fun `les routes du module Clients portent l identifiant`() {
        assertEquals("clients/42", ClientRoutes.fiche(42))
        assertEquals("clients/42/compte", ClientRoutes.compte(42))
        assertEquals("clients/42/activite", ClientRoutes.activite(42))
        assertEquals("clients/42/edit", ClientRoutes.edition(42))
        assertEquals("clients/relances", ClientRoutes.RELANCES)
        assertEquals("clients?create=true", ClientRoutes.liste(creer = true))
    }
}
