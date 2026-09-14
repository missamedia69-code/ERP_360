package com.missa.b360.ui.sales

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ReturnSaleScreen(onBack: () -> Unit, recordId: Long? = null) {
    PlaceholderScreen(titleRes = R.string.module_vente, onBack = onBack)
}
