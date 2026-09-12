package com.missa.b360.ui.operations

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

/**
 * Placeholder coherent — module en reconstruction.
 * L'accueil reste la reference design ; ce module sera reconstruit
 * dans la meme charte (MissaCanvas, cartes blanches 14dp, bord E2E8F0).
 */
@Composable
fun OperationModuleScreen(module: com.missa.b360.core.data.entity.OperationModule, onBack: () -> Unit, onNavigate: (String) -> Unit = {}, openCreate: Boolean = false, initialDirection: com.missa.b360.core.data.entity.OperationDirection = com.missa.b360.core.data.entity.OperationDirection.NONE) {
    PlaceholderScreen(titleRes = R.string.module_finances)
}

@Composable
fun OperationFormScreen(module: com.missa.b360.core.data.entity.OperationModule, initialDirection: com.missa.b360.core.data.entity.OperationDirection = com.missa.b360.core.data.entity.OperationDirection.NONE, onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.module_finances)
}
