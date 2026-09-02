package com.missa.b360.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.missa.b360.R
import com.missa.b360.ui.theme.MissaCanvas

/** État d'attente cohérent avec les modules livrés, sans écran visuellement brut. */
@Composable
fun PlaceholderScreen(titleRes: Int, subtitleRes: Int = R.string.module_placeholder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaCanvas)
            .padding(MissaLayout.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MissaEmptyState(
            icon = Icons.Outlined.Construction,
            title = stringResource(titleRes),
            description = stringResource(subtitleRes),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
