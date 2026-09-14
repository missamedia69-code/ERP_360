package com.missa.b360.ui.livraison

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun LivraisonScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_livraison, onBack = onBack)
}
