package com.missa.b360.ui.maintenance

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun MaintenanceScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_maintenance, onBack = onBack)
}
