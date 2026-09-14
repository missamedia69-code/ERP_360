package com.missa.b360.ui.comptabilite

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ComptabiliteScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_comptabilite, onBack = onBack)
}
