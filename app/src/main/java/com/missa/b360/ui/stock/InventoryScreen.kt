package com.missa.b360.ui.stock

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun InventoryScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.module_stock, onBack = onBack)
}
