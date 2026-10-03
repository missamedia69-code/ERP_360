package com.missa.b360.ui.clients.form

import com.missa.b360.ui.components.LocalCouleurSelection
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk

/**
 * Feuille modale ancrée en bas, affichée dans une destination `dialog(...)` : la liste ou la fiche
 * reste visible derrière. Toucher en dehors ou le bouton de fermeture revient en arrière.
 */
@Composable
internal fun ClientSheet(
    titre: String,
    onDismiss: () -> Unit,
    pied: (@Composable ColumnScope.() -> Unit)? = null,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).navigationBarsPadding()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            Column {
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(titre, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(Iv.Close), stringResource(R.string.cli_fermer), tint = LocalCouleurSelection.current ?: MissaInk)
                        }
                    }
                    contenu()
                }
                // Le bouton principal reste visible, comme le « Suivant » de la configuration.
                if (pied != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        content = pied,
                    )
                }
            }
        }
    }
}
