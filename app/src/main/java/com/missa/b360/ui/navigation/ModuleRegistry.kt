package com.missa.b360.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.ui.graphics.vector.ImageVector
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode

/**
 * ModuleRegistry (RA-22) — les 14 modules métier de Missa Business 360.
 * Chaque module = un package `ui/...` avec activation dynamique (profil AV/CUSTOM, 9.1).
 * La barre du bas par défaut : Vente · Stock · Clients · Finances + ➕.
 */
enum class AppModule(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    val moduleCode: ModuleCode,
    /**
     * Rang de candidature à la barre du bas : 1 = le plus prioritaire, 0 = ne
     * s'y épingle jamais d'office.
     *
     * La barre suit ainsi le pack réellement choisi. Une liste figée donnait
     * « Vente · Stock · Clients » à tout le monde : un prestataire de services
     * ouvrait son application sans y trouver son métier, et devait passer par
     * « Plus ». Les trois premiers modules présents dans le pack occupent les
     * trois places disponibles — l'accueil et « Plus » prennent déjà deux des
     * cinq.
     */
    val prioriteBarre: Int = 0,
) {
    VENTE("module_vente", R.string.module_vente, Icons.Outlined.PointOfSale, ModuleCode.VEN, prioriteBarre = 1),
    STOCK("module_stock", R.string.module_stock, Icons.Outlined.Inventory2, ModuleCode.STK, prioriteBarre = 2),
    CLIENTS("module_clients", R.string.module_clients, Icons.Outlined.Group, ModuleCode.VEN, prioriteBarre = 6),
    FINANCES("module_finances", R.string.module_finances, Icons.AutoMirrored.Outlined.TrendingUp, ModuleCode.CPT),
    ACHATS("module_achats", R.string.module_achats, Icons.Outlined.ShoppingCart, ModuleCode.ACH),
    FOURNISSEURS("module_fournisseurs", R.string.module_fournisseurs, Icons.Outlined.Handshake, ModuleCode.ACH),
    LIVRAISON("module_livraison", R.string.module_livraison, Icons.Outlined.LocalShipping, ModuleCode.LOG, prioriteBarre = 8),
    PRODUCTION("module_production", R.string.module_production, Icons.Outlined.LineWeight, ModuleCode.PRO, prioriteBarre = 5),
    SERVICES("module_services", R.string.module_services, Icons.Outlined.RequestQuote, ModuleCode.SER, prioriteBarre = 3),
    RH("module_rh", R.string.module_rh, Icons.Outlined.Person, ModuleCode.RH, prioriteBarre = 9),
    PROJETS("module_projets", R.string.module_projets, Icons.Outlined.Workspaces, ModuleCode.PRJ, prioriteBarre = 4),
    COMPTABILITE("module_comptabilite", R.string.module_comptabilite, Icons.Outlined.Savings, ModuleCode.CPT),
    TRESORERIE("module_tresorerie", R.string.module_tresorerie, Icons.Outlined.Savings, ModuleCode.TRE, prioriteBarre = 7),
    CRM("module_crm", R.string.module_crm, Icons.Outlined.Campaign, ModuleCode.CRM),
    QUALITE("module_qualite", R.string.module_qualite, Icons.Outlined.Build, ModuleCode.QUA),
    MAINTENANCE("module_maintenance", R.string.module_maintenance, Icons.Outlined.Build, ModuleCode.MAI),
    LOGISTIQUE("module_logistique", R.string.module_logistique, Icons.Outlined.LocalShipping, ModuleCode.LOG),
    REPORTING("module_reporting", R.string.module_reporting, Icons.Outlined.Analytics, ModuleCode.REP),
    ;

    companion object {


        /** Retourne les modules correspondant à une liste de ModuleCode. */
        fun fromCodes(codes: List<ModuleCode>): List<AppModule> =
            entries.filter { it.moduleCode in codes }

        /**
         * Modules à présenter compte tenu du pack choisi à l'onboarding.
         *
         * Une liste vide signifie « configuration inconnue » — installation
         * antérieure au pack, ou réglage jamais écrit : on montre alors tout.
         * Masquer par défaut priverait l'utilisateur de ses modules sans qu'il
         * comprenne pourquoi.
         */
        fun visibles(actifs: List<ModuleCode>): List<AppModule> =
            if (actifs.isEmpty()) entries.toList() else entries.filter { it.moduleCode in actifs }

        /**
         * Barre du bas : les modules épinglés par le Propriétaire, à défaut les
         * mieux placés du pack.
         *
         * Le choix d'usine découle du pack et non d'une liste figée : un
         * commerçant obtient Vente et Stock, un prestataire ses Services, un
         * bureau d'études ses Projets. Un épinglage devenu inactif — le pack a
         * changé — est ignoré plutôt que d'ouvrir un écran vide, et les écrans
         * qui masquent la barre n'y sont jamais proposés.
         */
        fun barreBas(actifs: List<ModuleCode>, epingles: List<String> = emptyList()): List<AppModule> {
            val disponibles = visibles(actifs).filter { it !in SANS_BARRE }
            val choisis = epingles.mapNotNull { nom ->
                disponibles.firstOrNull { it.name == nom }
            }
            return choisis.ifEmpty {
                disponibles
                    .filter { it.prioriteBarre > 0 }
                    .sortedBy { it.prioriteBarre }
            }.take(MAX_ONGLETS)
        }

        /**
         * Modules qu'il est permis d'épingler.
         *
         * Ceux dont l'écran masque la barre en sont exclus : les proposer
         * reviendrait à offrir un onglet qui disparaît dès qu'on l'ouvre.
         */
        fun epinglables(actifs: List<ModuleCode>): List<AppModule> =
            visibles(actifs).filter { it !in SANS_BARRE }

        /** Nombre maximal d'onglets, l'accueil et « Plus » occupant déjà deux places. */
        const val MAX_ONGLETS = 3

        /**
         * Modules dont la route ouvre un **formulaire de saisie** et non une
         * liste : la barre de navigation n'y aurait pas sa place.
         *
         * Achats et Finances ouvrent leur formulaire **en surimpression** de la
         * liste, avec sa propre barre « Annuler / Valider » : la barre de
         * navigation apparaîtrait dessous, deux barres empilées. Ils sont donc
         * exclus, et ne figurent pas non plus dans la disposition d'usine — un
         * onglet dont l'écran masque la barre est une contradiction.
         */
        private val SANS_BARRE = setOf(ACHATS, FINANCES)

        /**
         * Vrai si la barre de navigation doit rester visible sur cette route.
         *
         * Le critère est la profondeur : les écrans-liste d'un module sont des
         * destinations de premier niveau, les formulaires et les écrans
         * d'administration sont des tâches dont on sort par « retour ».
         */
        fun barreVisibleSur(route: String?): Boolean {
            val racine = route?.substringBefore('?') ?: return false
            val module = entries.firstOrNull { it.route == racine } ?: return false
            return module !in SANS_BARRE
        }

        /**
         * Menu « Plus » : les modules du pack qui ne tiennent pas dans la barre.
         *
         * La barre et ce menu forment une partition du pack — aucun module
         * n'est ni absent des deux, ni présent dans les deux. Épingler un
         * module le retire donc du menu, et le dépingler l'y remet.
         */
        fun secondaires(
            actifs: List<ModuleCode>,
            epingles: List<String> = emptyList(),
        ): List<AppModule> {
            val barre = barreBas(actifs, epingles).toSet()
            return visibles(actifs).filterNot { it in barre }
        }
    }
}
