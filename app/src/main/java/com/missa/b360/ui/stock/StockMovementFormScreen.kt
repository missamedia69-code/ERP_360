package com.missa.b360.ui.stock

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun StockMovementFormScreen(onBack: () -> Unit, initialDirection: com.missa.b360.core.data.entity.StockMovementType = com.missa.b360.core.data.entity.StockMovementType.ENTREE, onOpenTransfer: () -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_stock, onBack = onBack)
}

@Composable
fun StockTransferFormScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.module_stock, onBack = onBack)
}
