package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.MissaCanvas

/**
 * Gabarit commun de l'administration : même en-tête de marque que les modules métier,
 * fond bleuté discret, grille d'espacement compacte et contenu protégé des zones système.
 */
@Composable
fun AdminScaffold(
    titreRes: Int,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(titreRes),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MissaLayout.screenHorizontal,
                    vertical = MissaLayout.screenVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
            content = content,
        )
    }
}
