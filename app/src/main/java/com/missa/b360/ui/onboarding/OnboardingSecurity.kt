package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.OnboardingBorder
import com.missa.b360.ui.theme.ProfileGreen

/** Étape 4 — création du PIN : les 4–6 chiffres restent strictement validés par le ViewModel. */
@Composable
internal fun PinSetupStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_pin_title),
        subtitle = stringResource(R.string.ob_pin_subtitle),
        viewModel = viewModel,
        suivantActive = viewModel.pinEstValide() && viewModel.pin == viewModel.pinConfirmation,
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        OnboardingFormCard {
            SecurityHeader()
            OutlinedTextField(
                value = viewModel.pin,
                onValueChange = { viewModel.pin = it.filter(Char::isDigit).take(6) },
                label = { Text(stringResource(R.string.ob_nouveau_pin)) },
                singleLine = true,
                enabled = !viewModel.enregistrementEnCours,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandBlue)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.pinConfirmation,
                onValueChange = { viewModel.pinConfirmation = it.filter(Char::isDigit).take(6) },
                label = { Text(stringResource(R.string.ob_pin_confirmer)) },
                singleLine = true,
                enabled = !viewModel.enregistrementEnCours,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandBlue)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SecurityHeader() {
    Surface(
        color = ProfileGreen.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = ProfileGreen.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = ProfileGreen,
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = stringResource(R.string.ob_pin_securite_titre),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.ob_pin_securite_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Étape 5 — contact de récupération : nom et email restent contrôlés avant l'enregistrement. */
@Composable
internal fun EmailStep(viewModel: OnboardingViewModel) {
    val emailInvalide = viewModel.emailSecours.isNotBlank() && !viewModel.emailEstValide()
    StepScaffold(
        title = stringResource(R.string.ob_email_title),
        subtitle = stringResource(R.string.ob_email_subtitle),
        viewModel = viewModel,
        suivantActive = viewModel.emailEstValide(),
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        OnboardingFormCard {
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

/** Étape 6 — essai 7 jours et activation éventuelle, avec la hiérarchie visuelle de la maquette. */
@Composable
internal fun LicenceStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_licence_title),
        subtitle = stringResource(R.string.ob_licence_subtitle),
        viewModel = viewModel,
        navigationActive = !viewModel.enregistrementEnCours,
        showSkip = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                color = BrandBlue.copy(alpha = 0.07f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = BrandBlue,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(94.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "7",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 42.sp,
                            )
                            Text(
                                text = stringResource(R.string.ob_jours).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            if (viewModel.licenceActive) R.string.ob_licence_active else R.string.ob_licence_hero_title,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!viewModel.licenceActive) {
                        LicenceBenefit(R.string.ob_licence_avantage_fonctions)
                        LicenceBenefit(R.string.ob_licence_avantage_sans_carte)
                        LicenceBenefit(R.string.ob_licence_avantage_securite)
                    }
                }
            }

            OnboardingFormCard {
                Text(
                    text = stringResource(
                        if (viewModel.licenceActive) R.string.ob_licence_active else R.string.ob_licence_essai,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = viewModel.codeLicence,
                    onValueChange = { viewModel.codeLicence = it.uppercase() },
                    label = { Text(stringResource(R.string.ob_code_licence)) },
                    singleLine = true,
                    enabled = !viewModel.enregistrementEnCours,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = viewModel::activerCode,
                    enabled = viewModel.codeLicence.isNotBlank() && !viewModel.enregistrementEnCours,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandBlue),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.ob_activer), color = BrandBlue)
                }
                Text(
                    text = stringResource(R.string.ob_licence_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LicenceBenefit(textRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = ProfileGreen,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Carte neutre de saisie, partagée par les écrans PIN, email et licence. */
@Composable
internal fun OnboardingFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OnboardingBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
