package com.missa.b360.ui.production

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ProductionScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_production, onBack = onBack)
}
