package com.missa.b360.ui.logistique

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun LogistiqueScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_logistique, onBack = onBack)
}
