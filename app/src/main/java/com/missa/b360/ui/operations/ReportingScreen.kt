package com.missa.b360.ui.operations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.missa.b360.R
import com.missa.b360.core.domain.usecase.ValeurAlerte
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.PlaceholderScreen
import com.missa.b360.ui.theme.MissaCanvas
import java.util.Locale

/**
 * Placeholder coherent — module en reconstruction.
 */
@Composable
fun ReportingScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.module_reporting, onBack = onBack)
}

/** Détail chiffré d'une alerte, formaté selon sa nature (nombre, montant, taux). */
@Composable
internal fun detailAlerte(alerte: ValeurAlerte, devise: String): String {
    val exemple = alerte.exemple
    return if (alerte.code.avecMontant) {
        val montant = alerte.montant ?: 0.0
        val texte = if (alerte.code.detailRes == R.string.alerte_marge_d) {
            String.format(Locale.getDefault(), "%.1f %%", montant)
        } else {
            MoneyUtils.format(montant, devise)
        }
        stringResource(alerte.code.detailRes, texte)
    } else if (exemple != null) {
        stringResource(alerte.code.detailRes, alerte.nombre, exemple)
    } else {
        stringResource(alerte.code.detailRes, alerte.nombre)
    }
}
