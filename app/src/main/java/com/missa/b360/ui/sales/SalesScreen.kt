package com.missa.b360.ui.sales

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Screen constants apparentées au module Vente. Le formulaire transactionnel complet
 * est implémenté dans [SalesFlowScreen] ; ce fichier centralise uniquement le formatage
 * monétaire partagé par le panier, l'aperçu et le PDF.
 */
internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}
