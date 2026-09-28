package com.missa.b360.ui.home

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HomeNavigationTest {
    @Test
    fun everyVisibleQuickActionHasARealDestination() {
        AccueilActionKeys.ALL.forEach { key ->
            assertNotNull("No destination for quick action $key", HomeNavigation.quickAction(key))
        }
        val expected = mapOf(
            AccueilActionKeys.VENTE to "${AppModule.VENTE.route}?create=true",
            AccueilActionKeys.ACHAT to "${AppModule.ACHATS.route}?create=true",
            AccueilActionKeys.CLIENT to "${AppModule.CLIENTS.route}?create=true",
            AccueilActionKeys.FOURNISSEUR to "${AppModule.FOURNISSEURS.route}?create=true",
            AccueilActionKeys.PRODUIT to Routes.STOCK_PRODUCT_FORM,
            AccueilActionKeys.LIVRAISON to "${AppModule.LIVRAISON.route}?create=true",
            AccueilActionKeys.DEVIS to "${Routes.DEVIS_COMMANDE}?create=true",
            AccueilActionKeys.FACTURE to "${AppModule.VENTE.route}?create=true",
        )
        expected.forEach { (key, route) -> assertEquals("Wrong destination for $key", route, HomeNavigation.quickAction(key)) }
        assertEquals(null, HomeNavigation.quickAction("unknown"))
    }

    @Test
    fun recentOperationsAlwaysReturnToTheOwningModule() {
        val expected = mapOf(
            OperationModule.STOCK to AppModule.STOCK.route,
            OperationModule.VENTE to AppModule.VENTE.route,
            OperationModule.DEVIS to Routes.DEVIS_COMMANDE,
            OperationModule.COMMANDE to Routes.DEVIS_COMMANDE,
            OperationModule.ACHATS to AppModule.ACHATS.route,
            OperationModule.FINANCES to AppModule.TRESORERIE.route,
            OperationModule.LIVRAISON to AppModule.LIVRAISON.route,
            OperationModule.PRODUCTION to AppModule.PRODUCTION.route,
            OperationModule.SERVICES to AppModule.SERVICES.route,
            OperationModule.RH to AppModule.RH.route,
            OperationModule.PROJETS to AppModule.PROJETS.route,
        )
        OperationModule.entries.forEach { module ->
            assertEquals("Wrong destination for $module", expected.getValue(module), HomeNavigation.operation(module.name))
        }
        assertEquals(Routes.HOME, HomeNavigation.operation("UNKNOWN"))
    }

    @Test
    fun remindersOpenTheirOwningWorkflows() {
        assertEquals("${AppModule.VENTE.route}?overdue=true", HomeNavigation.rappel(HomeNavigation.Rappel.FACTURES_EN_RETARD))
        assertEquals("${AppModule.ACHATS.route}?pending=true", HomeNavigation.rappel(HomeNavigation.Rappel.COMMANDES_FOURNISSEUR))
        assertEquals(Routes.STOCK_ALERTES, HomeNavigation.rappel(HomeNavigation.Rappel.RUPTURES_STOCK))
        assertEquals(AppModule.QUALITE.route, HomeNavigation.rappel(HomeNavigation.Rappel.NON_CONFORMITES))
    }
}
