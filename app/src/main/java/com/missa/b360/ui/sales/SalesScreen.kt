package com.missa.b360.ui.sales

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun SalesScreen(onNavigate: (String) -> Unit = {}, onOpenClientCreate: () -> Unit = {}, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_vente)
}

internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { java.util.Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(java.util.Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}
