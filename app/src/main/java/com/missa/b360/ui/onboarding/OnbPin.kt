package com.missa.b360.ui.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen

private const val PIN_LONGUEUR = 4

/**
 * Écran 6 — Sécurisez votre accès : pavé numérique de la maquette pour un code
 * PIN à 4 chiffres (RA-01), suivi du contact de récupération qui crée le
 * Propriétaire (RA-03 / D1) avec le hash du PIN déjà écrit.
 */
@Composable
internal fun OnbPinStep(viewModel: OnboardingViewModel) {
    var detailsVisibles by remember { mutableStateOf(false) }
    val emailInvalide = viewModel.emailSecours.isNotBlank() && !viewModel.emailEstValide()

    OnbScaffold(
        titreRes = R.string.obn_pin_titre,
        sousTitreRes = R.string.obn_pin_sous,
        viewModel = viewModel,
        boutonActive = viewModel.pinEcranValide() && viewModel.emailEstValide(),
        onRetour = viewModel::precedent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (viewModel.pinDejaConfigure) {
                Surface(
                    color = ProfileGreen.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = ProfileGreen,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.obn_pin_deja),
                            fontSize = 13.sp,
                            color = MissaInk,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            } else {
                // --- Points du code ---
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (index in 0 until PIN_LONGUEUR) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .border(
                                    if (index < viewModel.pin.length) 0.dp else 1.5.dp,
                                    if (index < viewModel.pin.length) Color.Transparent else MissaBorder,
                                    CircleShape,
                                )
                                .background(
                                    if (index < viewModel.pin.length) BrandBlue else MissaSurface,
                                    CircleShape,
                                ),
                        )
                    }
                }

                // --- Pavé numérique ---
                val taper = { chiffre: String ->
                    if (viewModel.pin.length < PIN_LONGUEUR) {
                        viewModel.pin = viewModel.pin + chiffre
                    }
                }
                val effacer = { viewModel.pin = viewModel.pin.dropLast(1) }
                val touches = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (ligne in touches.chunked(3)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                        ) {
                            for (chiffre in ligne) {
                                OnbTouche(
                                    texte = chiffre,
                                    onClick = { taper(chiffre) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                    ) {
                        Spacer(Modifier.weight(1f))
                        OnbTouche(
                            texte = "0",
                            onClick = { taper("0") },
                            modifier = Modifier.weight(1f),
                        )
                        OnbTouche(
                            texte = "⌫",
                            onClick = effacer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // --- Note de sécurité ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.obn_pin_note),
                    fontSize = 12.sp,
                    color = MissaMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }

            // --- Contact de récupération (propriétaire) ---
            TextButton(
                onClick = { detailsVisibles = !detailsVisibles },
            ) {
                Text(
                    stringResource(
                        if (detailsVisibles) R.string.ob_profil_masquer_details else R.string.obn_detaux,
                    ),
                    fontSize = 13.sp,
                )
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MissaBorder),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.obn_pin_recup_titre),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                    )
                    Text(
                        text = stringResource(R.string.obn_pin_recup_sous),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                    OutlinedTextField(
                        value = viewModel.votreNom,
                        onValueChange = { viewModel.votreNom = it },
                        label = { Text(stringResource(R.string.ob_votre_nom)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandBlue) },
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = BrandBlue) },
                        supportingText = if (emailInvalide) {
                            { Text(stringResource(R.string.ob_email_invalide)) }
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Touche carrée du pavé numérique de la maquette. */
@Composable
private fun OnbTouche(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MissaBorder),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(
            text = texte,
            fontSize = 19.sp,
            color = MissaInk,
        )
    }
}
