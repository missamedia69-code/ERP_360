package com.missa.b360.ui.purchases

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun PurchasesScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_achats, onBack = onBack)
}
