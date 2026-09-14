package com.missa.b360.ui.qualite

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun QualiteScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_qualite, onBack = onBack)
}
