package com.missa.b360.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.components.MissaSectionPliable
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.Red40

/**
 * Écran 6 — Sécurisez votre accès.
 *
 * Le code PIN (RA-01) est saisi **deux fois** sur le pavé de la maquette :
 * quatre chiffres tapés une seule fois, c'est une faute de frappe qui enferme
 * l'utilisateur dehors dès la première ouverture. Le contact de récupération,
 * qui crée le Propriétaire (RA-03 / D1), suit dans une section repliable de
 * même facture que l'écran entreprise — il n'est pas facultatif : sans e-mail
 * valide, aucun compte ne peut être créé.
 */
@Composable
internal fun OnbPinStep(viewModel: OnboardingViewModel) {
    val emailValide = viewModel.emailEstValide()
    val emailInvalide = viewModel.emailSecours.isNotBlank() && !emailValide

    OnbScaffold(
        titreRes = R.string.obn_pin_titre,
        sousTitreRes = R.string.obn_pin_sous,
        viewModel = viewModel,
        boutonActive = viewModel.pinEcranValide() && emailValide,
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
                    imageVector = Icons.Outlined.Lock,
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

            // --- Contact de récupération : obligatoire, donc ouvert ---
            MissaSectionPliable(
                titre = stringResource(R.string.obn_pin_recup_titre),
                icone = Icons.Outlined.Person,
                resume = listOf(viewModel.votreNom, viewModel.emailSecours)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { stringResource(R.string.obn_pin_recup_sous) },
                etiquette = if (emailValide) {
                    null
                } else {
                    stringResource(R.string.obn_section_a_completer)
                },
                etiquetteEnErreur = emailInvalide,
                ouvertParDefaut = true,
                ouvrirDOffice = !emailValide,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = viewModel.votreNom,
                        onValueChange = { viewModel.votreNom = it },
                        label = { Text(stringResource(R.string.ob_votre_nom)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = viewModel.emailSecours,
                        onValueChange = { viewModel.emailSecours = it },
                        label = { Text(stringResource(R.string.ob_email)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        isError = emailInvalide,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = if (emailInvalide) Red40 else BrandBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        supportingText = {
                            Text(
                                text = stringResource(
                                    if (emailInvalide) {
                                        R.string.ob_email_invalide
                                    } else {
                                        R.string.obn_pin_recup_sous
                                    },
                                ),
                                fontSize = 11.sp,
                                color = if (emailInvalide) Red40 else MissaMuted,
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
        border = BorderStroke(1.dp, if (viewModel.pinConfirme) ProfileGreen else MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                for (index in 0 until OnboardingViewModel.PIN_LONGUEUR) {
                    val rempli = index < saisieCourante.length
                    val couleur = when {
                        viewModel.pinConfirme -> ProfileGreen
                        rempli -> BrandBlue
                        else -> MissaSurface
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
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
                        imageVector = Icons.Outlined.CheckCircle,
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
                        imageVector = Icons.Outlined.Restore,
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

/** Les douze touches, disposées comme sur un clavier téléphonique. */
@Composable
private fun OnbPinTouches(
    onChiffre: (String) -> Unit,
    onEffacer: () -> Unit,
    actif: Boolean,
) {
    val effacerDescription = stringResource(R.string.obn_pin_effacer)
    Column(modifier = Modifier.fillMaxWidth()) {
        for (ligne in listOf("123", "456", "789").map { it.map(Char::toString) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            ) {
                for (chiffre in ligne) {
                    OnbTouche(
                        onClick = { onChiffre(chiffre) },
                        actif = actif,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = chiffre, fontSize = 18.sp, color = MissaInk)
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        ) {
            Spacer(Modifier.weight(1f))
            OnbTouche(
                onClick = { onChiffre("0") },
                actif = actif,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "0", fontSize = 18.sp, color = MissaInk)
            }
            OnbTouche(
                onClick = onEffacer,
                actif = actif,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = effacerDescription },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

/** Touche du pavé : carrée, sobre, assez haute pour le pouce. */
@Composable
private fun OnbTouche(
    onClick: () -> Unit,
    actif: Boolean,
    modifier: Modifier = Modifier,
    contenu: @Composable () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = actif,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MissaBorder),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        content = { contenu() },
    )
}

/** Bandeau d'état pleine largeur (code déjà configuré). */
@Composable
private fun OnbPinBandeau(texteRes: Int, couleur: Color) {
    Surface(
        color = couleur.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
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
