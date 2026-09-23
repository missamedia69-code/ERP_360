package com.missa.b360.ui.components

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.missa.b360.R
import com.missa.b360.ui.theme.MissaCanvas

/** État d'attente cohérent avec les modules livrés, sans écran visuellement brut. */
@Composable
fun PlaceholderScreen(titleRes: Int, subtitleRes: Int = R.string.module_placeholder, onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = { MissaTopAppBar(title = stringResource(titleRes), onBack = onBack) },
        containerColor = MissaCanvas,
        // Insets gérés par l'échafaudage global + la barre du bas (voir AdminScaffold).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MissaCanvas)
                .padding(padding)
                .padding(MissaLayout.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MissaEmptyState(
                icon = Iv.Construction,
                title = stringResource(titleRes),
                description = stringResource(subtitleRes),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
