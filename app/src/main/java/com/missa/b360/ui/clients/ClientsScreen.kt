package com.missa.b360.ui.clients

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

/**
 * Écran Clients **en attente de reconstruction** (version « Accueil seul »).
 *
 * Le nom portait jusqu'ici `ClientsScreen`, exactement comme la fonction réelle
 * de [ClientFlowScreen], appelée avec les mêmes trois paramètres : le
 * compilateur résolvait vers ce placeholder, si bien que les 1 430 lignes du
 * parcours client réel étaient inatteignables sans que rien ne le signale.
 * Le placeholder porte donc désormais un nom qui dit ce qu'il est, et le
 * parcours complet reste lisible dans `ClientFlowScreen.kt` pour la
 * reconstruction du module.
 */
@Composable
fun ClientsPlaceholderScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    onNavigate: (String) -> Unit = {},
) {
    PlaceholderScreen(titleRes = R.string.module_clients, onBack = onBack)
}
