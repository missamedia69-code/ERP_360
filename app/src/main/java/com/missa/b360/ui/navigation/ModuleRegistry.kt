package com.missa.b360.ui.navigation

import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.outlined.TrendingUp
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
    /** true = présent par défaut dans la barre du bas (personnalisable par le Propriétaire). */
    val bottomBarDefault: Boolean = false,
) {
    VENTE("module_vente", R.string.module_vente, Icons.Outlined.PointOfSale, ModuleCode.VEN, bottomBarDefault = true),
    STOCK("module_stock", R.string.module_stock, Icons.Outlined.Inventory2, ModuleCode.STK, bottomBarDefault = true),
    CLIENTS("module_clients", R.string.module_clients, Icons.Outlined.Group, ModuleCode.VEN, bottomBarDefault = true),
    FINANCES("module_finances", R.string.module_finances, Icons.Outlined.TrendingUp, ModuleCode.CPT, bottomBarDefault = true),
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
    }
}
