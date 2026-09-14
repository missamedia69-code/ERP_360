package com.missa.b360.ui.navigation

import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModuleSousElements

/**
 * Ce qu'une fonctionnalité de module sait faire aujourd'hui.
 *
 * Le catalogue [ModuleSousElements] décrit la cible fonctionnelle complète —
 * 132 fonctionnalités réparties sur les 14 modules. Toutes ne sont pas encore
 * réalisées, et prétendre le contraire par un menu qui ne mène nulle part
 * serait pire que de l'annoncer.
 */
data class FonctionModule(
    val libelle: String,
    /** Route à ouvrir, ou `null` tant que la fonctionnalité n'est pas livrée. */
    val route: String? = null,
) {
    val disponible: Boolean get() = route != null
}

/**
 * Table des fonctionnalités réellement accessibles, module par module.
 *
 * Une fonctionnalité n'y figure que si elle ouvre un écran qui fonctionne. Le
 * reste du catalogue reste affiché sur la page du module, mais présenté comme
 * prévu et non comme disponible.
 */
object DestinationsFonctions {

    private val routes: Map<String, String> = mapOf(
        // --- Achats ---
        "Fournisseurs" to AppModule.FOURNISSEURS.route,
        "Commandes fournisseurs" to AppModule.ACHATS.route,
        "Réceptions" to AppModule.ACHATS.route,
        "Factures fournisseurs" to AppModule.ACHATS.route,

        // --- Ventes ---
        "Clients" to AppModule.CLIENTS.route,
        "Prospects" to AppModule.CRM.route,
        "Devis" to Routes.DEVIS_COMMANDE,
        "Commandes clients" to Routes.DEVIS_COMMANDE,
        "Livraisons" to AppModule.LIVRAISON.route,
        "Factures clients" to AppModule.VENTE.route,
        "Retours clients" to Routes.SALES_RETURN,
        "Relances clients" to AppModule.CRM.route,

        // --- Stock ---
        "Articles" to AppModule.STOCK.route,
        "Catégories d'articles" to AppModule.STOCK.route,
        "Entrepôts" to Routes.ADMIN_MULTISITE,
        "Mouvements de stock" to Routes.STOCK_MOVEMENT_FORM,
        "Entrées" to Routes.STOCK_MOVEMENT_FORM,
        "Sorties" to Routes.STOCK_MOVEMENT_FORM,
        "Transferts" to Routes.STOCK_TRANSFER_FORM,
        "Inventaires" to Routes.STOCK_INVENTORY,
        "Valorisation de stock" to AppModule.LOGISTIQUE.route,
        "Seuils d'alerte" to AppModule.REPORTING.route,

        // --- Production ---
        "Ordres de fabrication" to AppModule.PRODUCTION.route,
        "Déclarations de production" to AppModule.PRODUCTION.route,

        // --- Services ---
        "Interventions" to AppModule.SERVICES.route,
        "Demandes d'intervention" to AppModule.SERVICES.route,
        "Planning des interventions" to AppModule.SERVICES.route,
        "Facturation des prestations" to AppModule.SERVICES.route,

        // --- Projets ---
        "Projets" to AppModule.PROJETS.route,
        "Tâches" to Routes.TASKS,
        "Budgets projet" to AppModule.PROJETS.route,
        "Suivi d'avancement" to AppModule.PROJETS.route,

        // --- RH ---
        "Employés" to AppModule.RH.route,
        "Congés et absences" to AppModule.RH.route,
        "Paie" to AppModule.RH.route,
        "Notes de frais" to AppModule.RH.route,

        // --- Comptabilité ---
        "Journaux comptables" to AppModule.COMPTABILITE.route,
        "Écritures comptables" to AppModule.COMPTABILITE.route,
        "Grand livre" to AppModule.COMPTABILITE.route,
        "Compte de résultat" to AppModule.COMPTABILITE.route,
        "TVA" to AppModule.COMPTABILITE.route,
        "Plan comptable" to Routes.ADMIN_REFERENTIELS,

        // --- Trésorerie ---
        "Comptes bancaires" to AppModule.TRESORERIE.route,
        "Règlements clients" to AppModule.TRESORERIE.route,
        "Règlements fournisseurs" to AppModule.TRESORERIE.route,
        "Prévisions de trésorerie" to AppModule.TRESORERIE.route,
        "Rapprochements bancaires" to AppModule.TRESORERIE.route,
        "Échéanciers" to AppModule.TRESORERIE.route,

        // --- CRM ---
        "Pipeline commercial" to AppModule.CRM.route,
        "Opportunités" to AppModule.CRM.route,
        "Activités" to AppModule.CRM.route,
        "Historique relation client" to AppModule.CRM.route,

        // --- Qualité ---
        "Non-conformités" to AppModule.QUALITE.route,
        "Actions correctives" to AppModule.QUALITE.route,
        "Contrôles qualité" to AppModule.QUALITE.route,

        // --- Maintenance ---
        "Équipements" to AppModule.MAINTENANCE.route,
        "Maintenance préventive" to AppModule.MAINTENANCE.route,
        "Maintenance corrective" to AppModule.MAINTENANCE.route,
        "Interventions de maintenance" to AppModule.MAINTENANCE.route,

        // --- Logistique ---
        "Expéditions" to AppModule.LIVRAISON.route,
        "Bons d'expédition" to AppModule.LIVRAISON.route,
        "Transporteurs" to AppModule.LIVRAISON.route,
        "Tracking" to AppModule.LOGISTIQUE.route,

        "Coûts de maintenance" to AppModule.MAINTENANCE.route,
        "Historique des pannes" to AppModule.MAINTENANCE.route,
        "Indicateurs qualité" to AppModule.QUALITE.route,
        "Réclamations clients" to AppModule.QUALITE.route,
        "Coûts logistiques" to AppModule.LOGISTIQUE.route,
        "Rentabilité projet" to AppModule.PROJETS.route,
        "Suivi des temps" to AppModule.PROJETS.route,
        "Planning projet" to AppModule.PROJETS.route,
        "Balance" to AppModule.COMPTABILITE.route,
        "Coûts de production" to AppModule.PRODUCTION.route,
        "Consommations matières" to AppModule.PRODUCTION.route,
        "Contrats de service" to AppModule.SERVICES.route,
        "Catalogue de prestations" to AppModule.SERVICES.route,
        "Avoirs clients" to Routes.SALES_RETURN,
        "Contrôle qualité entrée" to AppModule.QUALITE.route,
        "Avoirs fournisseurs" to AppModule.ACHATS.route,
        "Lots / Séries" to AppModule.STOCK.route,
        "Réservations de stock" to AppModule.STOCK.route,

        // --- Reporting ---
        "Tableaux de bord" to AppModule.REPORTING.route,
        "Indicateurs KPI" to AppModule.REPORTING.route,
        "Alertes" to AppModule.REPORTING.route,
        "Rapports standards" to AppModule.REPORTING.route,
    )

    /** Toutes les fonctionnalités du module, disponibles ou non, dans l'ordre du catalogue. */
    fun pour(module: ModuleCode): List<FonctionModule> =
        ModuleSousElements.elements[module].orEmpty().map { libelle ->
            FonctionModule(libelle = libelle, route = routes[libelle])
        }

    fun disponibles(module: ModuleCode): List<FonctionModule> =
        pour(module).filter { it.disponible }

    fun aVenir(module: ModuleCode): List<FonctionModule> =
        pour(module).filterNot { it.disponible }
}
