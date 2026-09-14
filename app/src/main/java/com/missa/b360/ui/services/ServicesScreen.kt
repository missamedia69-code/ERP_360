package com.missa.b360.ui.services

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ServicesScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}, openCreate: Boolean = false) {
    PlaceholderScreen(titleRes = R.string.module_services, onBack = onBack)
}
