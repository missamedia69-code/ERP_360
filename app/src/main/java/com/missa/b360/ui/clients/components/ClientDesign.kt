package com.missa.b360.ui.clients.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/**
 * Palette « bleu nuit · violet » du module Clients (refonte d'octobre 2026) : fond gris clair, cartes blanches
 * finement contournées, bleu nuit pour l'identité et les cartes d'en-tête, violet pour les actions principales,
 * les icônes, la sélection et les états actifs. Vert et orange restent réservés aux états métier.
 * Référence complète : `docs/DESIGN_CLIENTS.md`.
 */
internal object ClientCouleurs {
    val Nuit = Color(0xFF172247)
    val Nuit2 = Color(0xFF24335F)
    val Violet = Color(0xFF7B3FE4)
    val VioletProfond = Color(0xFF682ED0)
    val VioletPale = Color(0xFFF1EAFE)
    val VioletBord = Color(0xFFE8DAFC)
    val Fond = Color(0xFFF4F6FA)
    val Surface = Color(0xFFF8F9FC)
    val Trait = Color(0xFFE1E5EE)
    val TraitFort = Color(0xFFCDD4E0)
    val Succes = Color(0xFF188252)
    val SuccesPale = Color(0xFFEAF7F0)
    val Alerte = Color(0xFFA85A12)
    val AlertePale = Color(0xFFFFF3E4)
    val Neutre = Color(0xFFEDF0F5)
    val NeutreTexte = Color(0xFF59647A)

    // Anciens jetons : conservés pour les écrans secondaires, alignés sur la nouvelle palette.
    val Carte = Color.White
    val CarteBord = Trait
    val Tuile = Color(0xFFEDF0F6)
    val TuileClaire = Surface
    val Pastille = Neutre
}

/** Bouton de la fiche : cible ≥ 48 dp ; plein = bouton d'action violet par défaut (bleu nuit si demandé), sinon contour blanc. */
@Composable
internal fun BoutonClient(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plein: Boolean = true,
    couleur: Color = ClientCouleurs.Violet,
    contenu: @Composable RowScope.() -> Unit,
) {
    val forme = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(forme)
            .background(if (plein) couleur else Color.White)
            .then(if (plein) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.TraitFort), forme))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ligne = this
        ProvideTextStyle(
            TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (plein) Color.White else MissaInk),
        ) { ligne.contenu() }
    }
}

/** Pastille sélectionnable : violette quand elle est sélectionnée, blanche à contour gris sinon (zone tactile ≥ 48 dp). */
@Composable
internal fun ClientPuce(
    libelle: String,
    actif: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab,
) {
    val forme = RoundedCornerShape(999.dp)
    Box(
        modifier.heightIn(min = 48.dp).clip(forme).clickable(role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .heightIn(min = 36.dp)
                .clip(forme)
                .background(if (actif) ClientCouleurs.Violet else Color.White)
                .then(if (actif) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.Trait), forme))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                libelle,
                color = if (actif) Color.White else MissaInk,
                fontSize = 12.sp,
                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/** Onglets de la fiche : texte gras violet souligné pour l'onglet courant, gris pour les autres, filet continu dessous. */
@Composable
internal fun <T> ClientOnglets(
    onglets: List<T>,
    courant: T,
    libelle: (T) -> Int,
    onChoix: (T) -> Unit,
) {
    val trait = ClientCouleurs.Trait
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(trait, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx()) }
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onglets.forEach { onglet ->
            val actif = onglet == courant
            Column(
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Tab) { onChoix(onglet) }
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.heightIn(min = 46.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(libelle(onglet)),
                        color = if (actif) ClientCouleurs.Violet else ClientCouleurs.NeutreTexte,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (actif) ClientCouleurs.Violet else Color.Transparent))
            }
        }
    }
}

/** État vide du module : cadre en pointillés, pastille d'icône, titre et description. */
@Composable
internal fun ClientEtatVide(icone: Int, titre: String, description: String, modifier: Modifier = Modifier) {
    val trait = ClientCouleurs.Trait
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = ClientCouleurs.TraitFort,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(ClientCouleurs.VioletPale),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icone), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(titre, color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            if (description.isNotBlank()) Text(description, color = MissaMuted, fontSize = 10.sp)
        }
    }
}

/** État vide de l'activité d'une fiche. */
@Composable
internal fun ClientVideActivite() {
    ClientEtatVide(
        icone = Iv.Description,
        titre = stringResource(R.string.cli_aucune_activite),
        description = stringResource(R.string.cli_aucune_activite_desc),
    )
}

/**
 * Bouton d'action du module Clients : fond violet (action principale) ou bleu nuit (action de suivi), texte blanc en gras.
 * Mêmes paramètres que `BoutonMissa`, importé sous le nom `Button` dans les écrans Clients.
 */
@Composable
internal fun BoutonClientPlein(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    couleur: Color = ClientCouleurs.Violet,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = couleur,
            contentColor = Color.White,
            disabledContainerColor = couleur.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.9f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val ligne = this
        ProvideTextStyle(TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)) { ligne.content() }
    }
}
