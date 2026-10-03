package com.missa.b360.ui.clients.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/**
 * Palette « nuit · gris · orange » du module Clients (maquettes fiche et liste) :
 * encre nuit pour le texte, l'avatar et les actions principales, gris bleuté pour les cartes et tuiles,
 * orange réservé aux éléments sélectionnables à l'état sélectionné (onglet, filtre, barre du bas). La couleur de risque reste réservée aux états.
 * Référence complète : `docs/DESIGN_CLIENTS.md`.
 */
internal object ClientCouleurs {
    val Nuit = Color(0xFF101C43)
    val Orange = Color(0xFFFF7A00)
    val Carte = Color(0xFFEAEDF2)
    val CarteBord = Color(0xFFD5DAE3)
    val Tuile = Color(0xFFE3E6EC)
    val TuileClaire = Color(0xFFEBEDF1)
    val Trait = Color(0xFFD5DAE3)
    val Pastille = Color(0xFFDDE1E8)
}

/** Bouton rectangulaire de la fiche : cible ≥ 48 dp, fond plein ou contour blanc. */
@Composable
internal fun BoutonClient(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plein: Boolean = true,
    contenu: @Composable RowScope.() -> Unit,
) {
    val forme = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(forme)
            .background(if (plein) ClientCouleurs.Tuile else Color.White)
            .then(if (plein) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.Trait), forme))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) { contenu() }
}

/** Onglets de la fiche : pastille orange pour l'onglet courant, contour gris pour les autres. */
@Composable
internal fun <T> ClientOnglets(
    onglets: List<T>,
    courant: T,
    libelle: (T) -> Int,
    onChoix: (T) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        items(onglets) { onglet ->
            val actif = onglet == courant
            val forme = RoundedCornerShape(12.dp)
            Box(
                Modifier.heightIn(min = 48.dp).clip(forme).clickable(role = Role.Tab) { onChoix(onglet) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .heightIn(min = 38.dp)
                        .clip(forme)
                        .background(if (actif) ClientCouleurs.Orange else Color.White)
                        .then(if (actif) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.Trait), forme))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(libelle(onglet)),
                        color = if (actif) Color.White else MissaInk,
                        fontSize = 11.sp,
                        fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/** État vide de la fiche : cadre en pointillés, pastille d'icône et deux lignes de texte. */
@Composable
internal fun ClientVideActivite() {
    val trait = ClientCouleurs.Trait
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = trait,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ClientCouleurs.Tuile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(Iv.Description), contentDescription = null, tint = MissaMuted, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(stringResource(R.string.cli_aucune_activite), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.cli_aucune_activite_desc), color = MissaMuted, fontSize = 11.sp)
        }
    }
}
