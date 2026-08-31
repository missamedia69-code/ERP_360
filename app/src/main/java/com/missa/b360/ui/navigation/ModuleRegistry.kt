package com.missa.b360.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.ui.graphics.vector.ImageVector
import com.missa.b360.R

/**
 * ModuleRegistry (RA-22) — les 13 modules métier de Missa Business 360.
 * Chaque module = un package `ui/...` avec activation dynamique (profil A–H, 9.1).
 * La barre du bas par défaut : Vente · Stock · Clients · Finances + ➕.
 */
enum class AppModule(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    /** true = présent par défaut dans la barre du bas (personnalisable par le Propriétaire). */
    val bottomBarDefault: Boolean = false,
) {
    VENTE("module_vente", R.string.module_vente, Icons.Outlined.PointOfSale, bottomBarDefault = true),
    STOCK("module_stock", R.string.module_stock, Icons.Outlined.Inventory2, bottomBarDefault = true),
    CLIENTS("module_clients", R.string.module_clients, Icons.Outlined.Group, bottomBarDefault = true),
    FINANCES("module_finances", R.string.module_finances, Icons.Outlined.TrendingUp, bottomBarDefault = true),
    FOURNISSEURS("module_fournisseurs", R.string.module_fournisseurs, Icons.Outlined.Handshake),
    ACHATS("module_achats", R.string.module_achats, Icons.Outlined.ShoppingCart),
    LIVRAISON("module_livraison", R.string.module_livraison, Icons.Outlined.LocalShipping),
    PRODUCTION("module_production", R.string.module_production, Icons.Outlined.LineWeight),
    SERVICES("module_services", R.string.module_services, Icons.Outlined.RequestQuote),
    RH("module_rh", R.string.module_rh, Icons.Outlined.Person),
    PROJETS("module_projets", R.string.module_projets, Icons.Outlined.Workspaces),
    REPORTING("module_reporting", R.string.module_reporting, Icons.Outlined.Analytics),
    ;

    companion object {
        /** Modules visibles dans la barre du bas (personnalisable, RA-22). */
        fun modulesBarreBas(): List<AppModule> = entries.filter { it.bottomBarDefault }

        /** Modules actifs absents de la barre → accessibles via ➕ « Plus de modules ». */
        fun modulesSecondaires(): List<AppModule> = entries.filterNot { it.bottomBarDefault }
    }
}
