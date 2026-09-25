package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnbConfigCard
import com.missa.b360.ui.theme.ProfileGreen

/**
 * Écran 6 — Sécurisez votre accès.
 *
 * Le code PIN (RA-01) est saisi **deux fois** sur le pavé de la maquette :
 * quatre chiffres tapés une seule fois, c'est une faute de frappe qui enferme
 * l'utilisateur dehors dès la première ouverture. L'email de récupération qui
 * crée le Propriétaire (RA-03 / D1) n'est plus saisi ici : il l'est sur l'écran
 * entreprise, avec les coordonnées — sans e-mail valide, aucun compte ne peut
 * être créé (la validation reste portée par le ViewModel).
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
 * Le pavé et ses quatre points, avec l'étape en cours : création du code, puis
 * confirmation. Une fois les deux saisies concordantes, le pavé cède la place à
 * un bandeau vert et au bouton « Recommencer ».
 */
@Composable
private fun OnbPinPave(viewModel: OnboardingViewModel) {
    val premiereSaisie = viewModel.pinEnPremiereSaisie
    val saisieCourante = if (premiereSaisie) viewModel.pin else viewModel.pinConfirmation
    Card(
        shape = RoundedCornerShape(16.dp),
        border = if (viewModel.pinConfirme) {
            BorderStroke(1.5.dp, ProfileGreen)
        } else {
            BorderStroke(1.dp, MissaBorder.copy(alpha = 0.5f))
        },
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    when {
                        viewModel.pinConfirme -> R.string.obn_pin_confirme
                        premiereSaisie -> R.string.ob_pin_title
                        else -> R.string.ob_pin_confirmer
                    },
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (viewModel.pinConfirme) ProfileGreen else MissaInk,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (index in 0 until OnboardingViewModel.PIN_LONGUEUR) {
                    val rempli = index < saisieCourante.length
                    val couleur = when {
                        viewModel.pinConfirme -> ProfileGreen
                        rempli -> BrandBlue
                        else -> MissaSurface
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                if (rempli || viewModel.pinConfirme) 0.dp else 1.5.dp,
                                if (rempli || viewModel.pinConfirme) Color.Transparent else MissaBorder,
                                CircleShape,
                            )
                            .background(couleur, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
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
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
            }

            if (viewModel.pinConfirme) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = viewModel::reinitialiserPin) {
                    Icon(
                        painter = painterResource(Iv.Restore),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.obn_pin_recommencer), fontSize = 12.sp)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                OnbPinTouches(
                    onChiffre = viewModel::saisirChiffrePin,
                    onEffacer = viewModel::effacerChiffrePin,
                    actif = !viewModel.enregistrementEnCours,
                )
            }
        }
    }
}

/**
 * Les douze touches, disposées comme sur un clavier professionnel : grille
 * 3 × 4 parfaite, le 0 centré sur sa colonne et la suppression à droite.
 * Tuiles blanches à bords arrondis sur la carte bleue.
 */
@Composable
private fun OnbPinTouches(
    onChiffre: (String) -> Unit,
    onEffacer: () -> Unit,
    actif: Boolean,
) {
    val effacerDescription = stringResource(R.string.obn_pin_effacer)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (ligne in listOf("123", "456", "789").map { it.map(Char::toString) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaInk,
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    fontSize = 19.sp,
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
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Touche du pavé : tuile blanche à bords arrondis, fine bordure, 54dp pour le pouce. */
@Composable
private fun OnbTouche(
    onClick: () -> Unit,
    actif: Boolean,
    modifier: Modifier = Modifier,
    contenu: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .border(1.dp, MissaBorder.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
            .clickable(enabled = actif, onClick = onClick)
            .then(if (actif) Modifier else Modifier.alpha(0.45f)),
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
