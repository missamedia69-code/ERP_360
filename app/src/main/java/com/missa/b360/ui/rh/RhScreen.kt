package com.missa.b360.ui.rh

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun RhScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_rh, onBack = onBack)
}
