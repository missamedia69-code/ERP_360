package com.missa.b360.ui.clients.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Carte blanche du module Clients : rayon 16, filet fin, ombre légère ; cliquable si [onClick] est fourni. */
@Composable
internal fun ClientCarte(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    val forme = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(1.dp, forme, clip = false)
            .clip(forme)
            .background(Color.White)
            .border(BorderStroke(1.dp, ClientCouleurs.Trait), forme)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        content = contenu,
    )
}

/** Carte d'en-tête bleu nuit en dégradé, avec un grand cercle violet translucide en haut à droite. */
@Composable
internal fun ClientHero(modifier: Modifier = Modifier, contenu: @Composable ColumnScope.() -> Unit) {
    val forme = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(forme)
            .background(Brush.linearGradient(listOf(ClientCouleurs.Nuit, ClientCouleurs.Nuit2)))
            .drawBehind {
                drawCircle(
                    color = ClientCouleurs.Violet.copy(alpha = 0.28f),
                    radius = 64.dp.toPx(),
                    center = Offset(size.width - 6.dp.toPx(), -12.dp.toPx()),
                )
            }
            .padding(14.dp),
        content = contenu,
    )
}

/** Symbole de section : carré arrondi violet pâle, icône violette. */
@Composable
internal fun ClientSymbole(icone: Int, modifier: Modifier = Modifier, taille: androidx.compose.ui.unit.Dp = 27.dp) {
    Box(
        modifier.size(taille).clip(RoundedCornerShape(9.dp)).background(ClientCouleurs.VioletPale),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icone), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(taille * 0.58f))
    }
}

/** Titre de section avec symbole violet et zone libre à droite (compteur, lien). */
@Composable
internal fun ClientTitreSection(
    icone: Int,
    titre: String,
    modifier: Modifier = Modifier,
    fin: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ClientSymbole(icone)
        Text(titre, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        fin?.invoke()
    }
}

/** Compteur arrondi (nombre de factures, d'encaissements…). */
@Composable
internal fun ClientCompteur(texte: String) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(ClientCouleurs.Neutre).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(texte, color = ClientCouleurs.NeutreTexte, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Pastille d'état arrondie (statut, risque) : texte coloré sur fond pâle, icône facultative. */
@Composable
internal fun ClientPastille(
    texte: String,
    fond: Color,
    couleurTexte: Color,
    modifier: Modifier = Modifier,
    icone: Int? = null,
    bord: Color? = null,
) {
    val forme = RoundedCornerShape(999.dp)
    Row(
        modifier
            .clip(forme)
            .background(fond)
            .then(if (bord != null) Modifier.border(BorderStroke(1.dp, bord), forme) else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icone != null) Icon(painterResource(icone), contentDescription = null, tint = couleurTexte, modifier = Modifier.size(13.dp))
        Text(texte, color = couleurTexte, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Bouton « doux » : fond violet pâle, texte violet (Promesse de paiement). */
@Composable
internal fun BoutonClientDoux(onClick: () -> Unit, modifier: Modifier = Modifier, contenu: @Composable RowScope.() -> Unit) {
    val forme = RoundedCornerShape(13.dp)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(forme)
            .background(ClientCouleurs.VioletPale)
            .border(BorderStroke(1.dp, ClientCouleurs.VioletBord), forme)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) { contenu() }
}

/**
 * Feuille basse du module Clients (promesse, encaissement, note) : poignée, titre, sous-titre, croix, contenu et deux boutons.
 * [actions] reçoit une ligne : Annuler (contour) puis le bouton principal violet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientFeuille(
    titre: String,
    onFermer: () -> Unit,
    sousTitre: String? = null,
    actions: @Composable RowScope.() -> Unit,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onFermer,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titre, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    if (sousTitre != null) Text(sousTitre, color = MissaMuted, fontSize = 12.sp)
                }
                IconButton(
                    onClick = onFermer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(ClientCouleurs.Surface)
                            .border(BorderStroke(1.dp, ClientCouleurs.Trait), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(Iv.Close), stringResource(R.string.cli_annuler), tint = MissaInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
            contenu()
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

/** Encadré d'information (ex. « Reste dû ») : fond gris très clair, filet fin. */
@Composable
internal fun ClientEncadre(contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ClientCouleurs.Surface)
            .border(BorderStroke(1.dp, ClientCouleurs.Trait), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = contenu,
    )
}
