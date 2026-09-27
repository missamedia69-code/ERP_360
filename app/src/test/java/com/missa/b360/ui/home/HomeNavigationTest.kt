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
        assertEquals("${AppModule.LIVRAISON.route}?create=true", HomeNavigation.quickAction(AccueilActionKeys.LIVRAISON))
        assertEquals(Routes.STOCK_PRODUCT_FORM, HomeNavigation.quickAction(AccueilActionKeys.PRODUIT))
        assertEquals("${Routes.DEVIS_COMMANDE}?create=true", HomeNavigation.quickAction(AccueilActionKeys.DEVIS))
        assertEquals("${AppModule.VENTE.route}?create=true", HomeNavigation.quickAction(AccueilActionKeys.FACTURE))
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
        assertEquals(AppModule.VENTE.route, HomeNavigation.rappel(HomeNavigation.Rappel.FACTURES_EN_RETARD))
        assertEquals(AppModule.ACHATS.route, HomeNavigation.rappel(HomeNavigation.Rappel.COMMANDES_FOURNISSEUR))
        assertEquals(Routes.STOCK_ALERTES, HomeNavigation.rappel(HomeNavigation.Rappel.RUPTURES_STOCK))
        assertEquals(AppModule.QUALITE.route, HomeNavigation.rappel(HomeNavigation.Rappel.NON_CONFORMITES))
    }
}
