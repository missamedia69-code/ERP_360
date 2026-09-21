package com.missa.b360.ui.clients

import androidx.compose.runtime.Composable

/**
 * Point d'entrée du module Clients.
 * Délègue directement à l'implémentation complète [ClientsScreen] de [ClientFlowScreen].
 */
@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    onNavigate: (String) -> Unit = {},
) {
    com.missa.b360.ui.clients.ClientsScreen(
        onBack = onBack,
        openCreate = openCreate,
        onNavigate = onNavigate,
    )
}
