package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Blue40
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen

/**
 * Écran 6 — Sécurisez votre accès.
 *
 * Le code PIN (RA-01) est saisi **deux fois** sur le pavé de la maquette :
 * quatre chiffres tapés une seule fois, c'est une faute de frappe qui enferme
 * l'utilisateur dehors dès la première ouverture. L'email de récupération qui
 * crée le Propriétaire (RA-03 / D1) n'est plus saisi ici : il l'est sur
 * l'écran entreprise, avec les coordonnées — sans e-mail valide, aucun compte
 * ne peut être créé (la validation reste portée par le ViewModel).
 *
 * Refonte : emblème cadenas en dégradé de marque, points de saisie 30 dp
 * ombrés, pavé compact centré sur une carte blanche (grille 3 × 4, le 0
 * centré sur sa colonne, suppression à droite) et retour visuel à la touche
 * pressée — l'écran tient la promesse « code d'accès » sans fioritures.
 */
@Composable
internal fun OnbPinStep(viewModel: OnboardingViewModel) {
    OnbScaffold(
        titreRes = R.string.obn_pin_titre,
        sousTitreRes = R.string.obn_pin_sous,
        viewModel = viewModel,
        boutonActive = viewModel.pinEcranValide(),
        onRetour = viewModel::precedent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (viewModel.pinDejaConfigure) {
                OnbPinBandeau(
                    texteRes = R.string.obn_pin_deja,
                    couleur = ProfileGreen,
                )
            } else {
                OnbPinPave(viewModel)
            }

            // --- Note de sécurité, sur une ligne ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            ) {
                Icon(
                    painter = painterResource(Iv.Lock),
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.obn_pin_note),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Le pavé et ses quatre points, avec l'étape en cours : création du code,
 * puis confirmation. Une fois les deux saisies concordantes, le pavé cède la
 * place à l'emblème vert de confirmation et au bouton « Recommencer ».
 */
@Composable
private fun OnbPinPave(viewModel: OnboardingViewModel) {
    val premiereSaisie = viewModel.pinEnPremiereSaisie
    val saisieCourante = if (premiereSaisie) viewModel.pin else viewModel.pinConfirmation
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Emblème : cadenas en dégradé de marque, ou coche verte une fois le
        // code confirmé — l'ancrage visuel de l'écran.
        if (viewModel.pinConfirme) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ProfileGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.CheckCircle),
                    contentDescription = null,
                    tint = ProfileGreen,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 9.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = BrandBlue.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF3E7BFA), Blue40),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Lock),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Text(
            text = stringResource(
                when {
                    viewModel.pinConfirme -> R.string.obn_pin_confirme
                    premiereSaisie -> R.string.ob_pin_title
                    else -> R.string.ob_pin_confirmer
                },
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (viewModel.pinConfirme) ProfileGreen else MissaInk,
        )

        // Points de saisie : 30 dp, remplis en bleu de marque (vert une fois
        // confirmé), bordure claire pour les vides.
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            for (index in 0 until OnboardingViewModel.PIN_LONGUEUR) {
                val rempli = index < saisieCourante.length
                val couleur = when {
                    viewModel.pinConfirme -> ProfileGreen
                    rempli -> BrandBlue
                    else -> MissaSurface
                }
                val ombre = if (rempli || viewModel.pinConfirme) {
                    (if (viewModel.pinConfirme) ProfileGreen else BrandBlue).copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .shadow(
                            elevation = if (rempli || viewModel.pinConfirme) 3.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = ombre,
                        )
                        .clip(CircleShape)
                        .border(
                            if (rempli || viewModel.pinConfirme) 0.dp else 1.5.dp,
                            if (rempli || viewModel.pinConfirme) Color.Transparent else MissaBorder,
                            CircleShape,
                        )
                        .background(couleur, CircleShape),
                )
            }
        }

        // Le premier code reste marqué comme acquis pendant la confirmation.
        AnimatedVisibility(visible = !premiereSaisie) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Iv.CheckCircle),
                    contentDescription = null,
                    tint = ProfileGreen,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.obn_pin_premier_ok),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
            }
        }

        if (viewModel.pinConfirme) {
            TextButton(onClick = viewModel::reinitialiserPin) {
                Icon(
                    painter = painterResource(Iv.Restore),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.obn_pin_recommencer), fontSize = 12.5.sp)
            }
        } else {
            // Pavé compact centré sur une carte blanche : plus net qu'un
            // pavé pleine largeur, le clavier garde sa silhouette.
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OnbPinLignes(
                        onChiffre = viewModel::saisirChiffrePin,
                        onEffacer = viewModel::effacerChiffrePin,
                        actif = !viewModel.enregistrementEnCours,
                    )
                }
            }
        }
    }
}

/**
 * Les douze touches, disposées comme sur un clavier professionnel : grille
 * 3 × 4 parfaite, le 0 centré sur sa colonne et la suppression à droite.
 */
@Composable
private fun OnbPinLignes(
    onChiffre: (String) -> Unit,
    onEffacer: () -> Unit,
    actif: Boolean,
) {
    val effacerDescription = stringResource(R.string.obn_pin_effacer)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (ligne in listOf("123", "456", "789").map { it.map(Char::toString) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (chiffre in ligne) {
                    OnbTouche(
                        onClick = { onChiffre(chiffre) },
                        actif = actif,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = chiffre,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaInk,
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Emplacement vide : le 0 reste centré sur sa colonne, la touche
            // de suppression à droite — le tracé du clavier reste impeccable.
            Box(modifier = Modifier.weight(1f))
            OnbTouche(
                onClick = { onChiffre("0") },
                actif = actif,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "0",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
            }
            OnbTouche(
                onClick = onEffacer,
                actif = actif,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = effacerDescription },
            ) {
                Icon(
                    painter = painterResource(Iv.Backspace),
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/**
 * Touche du pavé : tuile blanche 58 dp à bords arrondis ; à la pression,
 * fond tonal bleu de marque et léger retrait — retour tactile visible.
 */
@Composable
private fun OnbTouche(
    onClick: () -> Unit,
    actif: Boolean,
    modifier: Modifier = Modifier,
    contenu: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = actif
    Box(
        modifier = modifier
            .height(58.dp)
            .graphicsLayer { scale = if (active && pressed) 0.95f else 1f }
            .clip(RoundedCornerShape(16.dp))
            .background(if (active && pressed) MissaSoftBlue else MissaSurface)
            .border(
                width = 1.dp,
                color = if (active && pressed) BrandBlue.copy(alpha = 0.5f) else MissaBorder.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                enabled = active,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .then(if (active) Modifier else Modifier.alpha(0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        contenu()
    }
}

/** Bandeau d'état pleine largeur (code déjà configuré). */
@Composable
private fun OnbPinBandeau(texteRes: Int, couleur: Color) {
    Surface(
        color = couleur.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Iv.CheckCircle),
                contentDescription = null,
                tint = couleur,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(texteRes),
                fontSize = 12.5.sp,
                color = MissaInk,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
