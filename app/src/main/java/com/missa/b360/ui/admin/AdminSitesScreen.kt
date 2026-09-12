package com.missa.b360.ui.admin

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

/**
 * Placeholder coherent — module en reconstruction.
 * L'accueil reste la reference design ; ce module sera reconstruit
 * dans la meme charte (MissaCanvas, cartes blanches 14dp, bord E2E8F0).
 */
@Composable
fun AdminSitesScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.home_sites_sales, onBack = onBack)
}
