package com.missa.b360

import com.missa.b360.core.notifications.NotificationRoutes
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRoutesTest {
    @Test
    fun `chaque racine metier ouvre son module`() {
        val cas = mapOf(
            "VENTE" to AppModule.VENTE.route,
            "CLIENT_ALERTE" to AppModule.VENTE.route,
            "ACHATS" to AppModule.ACHATS.route,
            "FOURNISSEUR" to AppModule.FOURNISSEURS.route,
            "STOCK_ALERTE" to AppModule.STOCK.route,
            "PRODUCTION" to AppModule.PRODUCTION.route,
            "SERVICES" to AppModule.SERVICES.route,
            "PROJETS" to AppModule.PROJETS.route,
            "RH" to AppModule.RH.route,
            "FINANCES" to AppModule.COMPTABILITE.route,
            "TRESORERIE" to AppModule.TRESORERIE.route,
            "QUA" to AppModule.QUALITE.route,
            "MAINTENANCE" to AppModule.MAINTENANCE.route,
            "LOGISTIQUE" to AppModule.LOGISTIQUE.route,
            "REP" to AppModule.REPORTING.route,
        )
        cas.forEach { (type, route) -> assertEquals(type, route, NotificationRoutes.pourType(type)) }
    }

    @Test
    fun `un type inconnu retourne a l'accueil`() {
        assertEquals(Routes.HOME, NotificationRoutes.pourType("INCONNU"))
    }
}
