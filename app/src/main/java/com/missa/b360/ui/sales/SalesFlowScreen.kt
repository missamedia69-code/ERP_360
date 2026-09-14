package com.missa.b360.ui.sales

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

/**
 * Placeholder — flux de vente en reconstruction.
 * Conserve le fichier pour éviter les imports cassés ; le contenu sera
 * reconstruit avec la même charte que l'accueil.
 */
@Composable
fun SalesFlowPlaceholder(onBack: () -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_vente, onBack = onBack)
}
