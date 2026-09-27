package com.missa.b360.ui.home

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes

/** Route contract for every dashboard shortcut and destination-bearing summary row. */
internal object HomeNavigation {
    enum class Rappel { FACTURES_EN_RETARD, COMMANDES_FOURNISSEUR, RUPTURES_STOCK, NON_CONFORMITES }

    fun quickAction(key: String): String? = when (key) {
        AccueilActionKeys.VENTE -> "${AppModule.VENTE.route}?create=true"
        AccueilActionKeys.ACHAT -> "${AppModule.ACHATS.route}?create=true"
        AccueilActionKeys.CLIENT -> "${AppModule.CLIENTS.route}?create=true"
        AccueilActionKeys.FOURNISSEUR -> "${AppModule.FOURNISSEURS.route}?create=true"
        AccueilActionKeys.PRODUIT -> Routes.STOCK_PRODUCT_FORM
        AccueilActionKeys.LIVRAISON -> "${AppModule.LIVRAISON.route}?create=true"
        AccueilActionKeys.DEVIS -> "${Routes.DEVIS_COMMANDE}?create=true"
        AccueilActionKeys.FACTURE -> "${AppModule.VENTE.route}?create=true"
        else -> null
    }

    fun operation(moduleName: String): String = when (moduleName) {
        OperationModule.STOCK.name -> AppModule.STOCK.route
        OperationModule.DEVIS.name, OperationModule.COMMANDE.name -> Routes.DEVIS_COMMANDE
        OperationModule.VENTE.name -> AppModule.VENTE.route
        OperationModule.ACHATS.name -> AppModule.ACHATS.route
        OperationModule.FINANCES.name -> AppModule.TRESORERIE.route
        OperationModule.LIVRAISON.name -> AppModule.LIVRAISON.route
        OperationModule.PRODUCTION.name -> AppModule.PRODUCTION.route
        OperationModule.SERVICES.name -> AppModule.SERVICES.route
        OperationModule.RH.name -> AppModule.RH.route
        OperationModule.PROJETS.name -> AppModule.PROJETS.route
        else -> Routes.HOME
    }

    fun rappel(rappel: Rappel): String = when (rappel) {
        Rappel.FACTURES_EN_RETARD -> AppModule.VENTE.route
        Rappel.COMMANDES_FOURNISSEUR -> AppModule.ACHATS.route
        Rappel.RUPTURES_STOCK -> Routes.STOCK_ALERTES
        Rappel.NON_CONFORMITES -> AppModule.QUALITE.route
    }
}
