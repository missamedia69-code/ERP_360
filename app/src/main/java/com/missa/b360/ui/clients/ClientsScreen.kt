package com.missa.b360.ui.clients

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun ClientsScreen(onBack: () -> Unit, openCreate: Boolean = false, onNavigate: (String) -> Unit = {}) {
    PlaceholderScreen(titleRes = R.string.module_clients, onBack = onBack)
}
