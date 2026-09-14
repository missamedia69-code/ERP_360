package com.missa.b360.ui.operations

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.ui.components.PlaceholderScreen

/**
 * Écrans d'opération encore en attente (spec §3.2).
 *
 * Les deux écrans annonçaient « Finances » quel que soit le module réellement
 * ouvert : un mouvement de stock ou une livraison se présentait donc comme une
 * écriture financière. Le titre suit désormais le module reçu.
 *
 * Les paramètres `onNavigate`, `openCreate` et `initialDirection` sont
 * conservés tels quels : les routes du graphe de navigation les passent déjà et
 * les reprendre à la reconstruction du module évitera de retoucher les appels.
 */
@Composable
fun OperationModuleScreen(
    module: OperationModule,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    openCreate: Boolean = false,
    initialDirection: OperationDirection = OperationDirection.NONE,
) {
    PlaceholderScreen(titleRes = titreModule(module), onBack = onBack)
}

@Composable
fun OperationFormScreen(
    module: OperationModule,
    initialDirection: OperationDirection = OperationDirection.NONE,
    onBack: () -> Unit,
) {
    PlaceholderScreen(titleRes = titreModule(module), onBack = onBack)
}

/** Titre affiché selon le module d'opération — devis et commandes relèvent de la Vente. */
private fun titreModule(module: OperationModule): Int = when (module) {
    OperationModule.STOCK -> R.string.module_stock
    OperationModule.VENTE,
    OperationModule.DEVIS,
    OperationModule.COMMANDE,
    -> R.string.module_vente
    OperationModule.ACHATS -> R.string.module_achats
    OperationModule.FINANCES -> R.string.module_finances
    OperationModule.LIVRAISON -> R.string.module_livraison
    OperationModule.PRODUCTION -> R.string.module_production
    OperationModule.SERVICES -> R.string.module_services
    OperationModule.RH -> R.string.module_rh
    OperationModule.PROJETS -> R.string.module_projets
}
