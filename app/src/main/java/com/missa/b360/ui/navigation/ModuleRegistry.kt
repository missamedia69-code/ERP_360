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
     * true = épinglé d'office dans la barre du bas (personnalisable par le
     * Propriétaire). Trois au maximum : l'accueil et « Plus » occupent déjà
     * deux des cinq places. Le choix d'usine retient les flux consultés chaque
     * jour — ce que je vends, ce que j'ai en stock, ce que j'ai encaissé.
     */
    val bottomBarDefault: Boolean = false,
) {
    VENTE("module_vente", R.string.module_vente, Icons.Outlined.PointOfSale, ModuleCode.VEN, bottomBarDefault = true),
    STOCK("module_stock", R.string.module_stock, Icons.Outlined.Inventory2, ModuleCode.STK, bottomBarDefault = true),
    CLIENTS("module_clients", R.string.module_clients, Icons.Outlined.Group, ModuleCode.VEN),
    FINANCES("module_finances", R.string.module_finances, Icons.AutoMirrored.Outlined.TrendingUp, ModuleCode.CPT, bottomBarDefault = true),
    ACHATS("module_achats", R.string.module_achats, Icons.Outlined.ShoppingCart, ModuleCode.ACH),
    FOURNISSEURS("module_fournisseurs", R.string.module_fournisseurs, Icons.Outlined.Handshake, ModuleCode.ACH),
    LIVRAISON("module_livraison", R.string.module_livraison, Icons.Outlined.LocalShipping, ModuleCode.LOG),
    PRODUCTION("module_production", R.string.module_production, Icons.Outlined.LineWeight, ModuleCode.PRO),
    SERVICES("module_services", R.string.module_services, Icons.Outlined.RequestQuote, ModuleCode.SER),
    RH("module_rh", R.string.module_rh, Icons.Outlined.Person, ModuleCode.RH),
    PROJETS("module_projets", R.string.module_projets, Icons.Outlined.Workspaces, ModuleCode.PRJ),
    COMPTABILITE("module_comptabilite", R.string.module_comptabilite, Icons.Outlined.Savings, ModuleCode.CPT),
    TRESORERIE("module_tresorerie", R.string.module_tresorerie, Icons.Outlined.Savings, ModuleCode.TRE),
    CRM("module_crm", R.string.module_crm, Icons.Outlined.Campaign, ModuleCode.CRM),
    QUALITE("module_qualite", R.string.module_qualite, Icons.Outlined.Build, ModuleCode.QUA),
    MAINTENANCE("module_maintenance", R.string.module_maintenance, Icons.Outlined.Build, ModuleCode.MAI),
    LOGISTIQUE("module_logistique", R.string.module_logistique, Icons.Outlined.LocalShipping, ModuleCode.LOG),
    REPORTING("module_reporting", R.string.module_reporting, Icons.Outlined.Analytics, ModuleCode.REP),
    ;

    companion object {
        /** Modules visibles dans la barre du bas (personnalisable, RA-22). */
        fun modulesBarreBas(): List<AppModule> = entries.filter { it.bottomBarDefault }

        /** Modules actifs absents de la barre → accessibles via ➕ « Plus de modules ». */
        fun modulesSecondaires(): List<AppModule> = entries.filterNot { it.bottomBarDefault }

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
         * modules marqués par défaut. Un épinglage devenu inactif — le pack a
         * changé — est simplement ignoré plutôt que d'ouvrir un écran vide.
         */
        fun barreBas(actifs: List<ModuleCode>, epingles: List<String> = emptyList()): List<AppModule> {
            val disponibles = visibles(actifs)
            val choisis = epingles.mapNotNull { nom ->
                disponibles.firstOrNull { it.name == nom }
            }
            return choisis.ifEmpty { disponibles.filter { it.bottomBarDefault } }
        }

        /** Modules qu'il est permis d'épingler : tous ceux du pack. */
        fun epinglables(actifs: List<ModuleCode>): List<AppModule> = visibles(actifs)

        /** Nombre maximal d'onglets, l'accueil et « Plus » occupant déjà deux places. */
        const val MAX_ONGLETS = 3

        /**
         * Modules dont la route ouvre un **formulaire de saisie** et non une
         * liste : la barre de navigation n'y aurait pas sa place.
         *
         * L'ensemble est vide aujourd'hui : chaque module épinglable mène à un
         * écran où la barre reste visible. Une première version en excluait six
         * sur la foi d'une mauvaise association écran/route — `module_stock`
         * mène à `StockScreen` et non à `InventoryScreen` — ce qui faisait
         * disparaître la barre sur les onglets les plus utilisés.
         */
        private val SANS_BARRE = emptySet<AppModule>()

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

        /** Modules actifs hors barre du bas → menu « Plus de modules ». */
        fun secondaires(actifs: List<ModuleCode>): List<AppModule> =
            visibles(actifs).filterNot { it.bottomBarDefault }
    }
}
