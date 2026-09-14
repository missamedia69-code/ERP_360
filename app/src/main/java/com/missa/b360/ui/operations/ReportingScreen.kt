package com.missa.b360.ui.operations

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ReportingScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.module_reporting, onBack = onBack)
}

@Composable
internal fun detailAlerte(alerte: com.missa.b360.core.domain.usecase.ValeurAlerte, devise: String): String {
    return ""
}
