package com.missa.b360.ui.fournisseurs

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun FournisseursScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_fournisseurs, onBack = onBack)
}
