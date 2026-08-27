package com.missabusiness.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.missabusiness.app.R

/**
 * Les onglets principaux du squelette de navigation.
 * Chaque module sera conçu (maquettes) puis développé ensuite.
 */
enum class ErpDestination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Outlined.Dashboard),
    VENTES("ventes", R.string.nav_ventes, Icons.Outlined.PointOfSale),
    STOCK("stock", R.string.nav_stock, Icons.Outlined.Inventory2),
    CLIENTS("clients", R.string.nav_clients, Icons.Outlined.Group),
    PARAMETRES("parametres", R.string.nav_parametres, Icons.Outlined.Settings),
}
