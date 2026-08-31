package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.missa.b360.R

/** Étape 4 — création du PIN (RA-01 : 4–6 chiffres, demandé à chaque ouverture). */
@Composable
internal fun PinSetupStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_pin_title),
        viewModel = viewModel,
        suivantActive = viewModel.pinEstValide() && viewModel.pin == viewModel.pinConfirmation,
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        Text(
            stringResource(R.string.ob_pin_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = viewModel.pin,
            onValueChange = { viewModel.pin = it.filter(Char::isDigit).take(6) },
            label = { Text(stringResource(R.string.ob_pin_title)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        OutlinedTextField(
            value = viewModel.pinConfirmation,
            onValueChange = { viewModel.pinConfirmation = it.filter(Char::isDigit).take(6) },
            label = { Text(stringResource(R.string.ob_pin_confirmer)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

/** Étape 5 — email de secours obligatoire (RA-03), nom de l'utilisateur (D1). */
@Composable
internal fun EmailStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_email_title),
        viewModel = viewModel,
        suivantActive = viewModel.emailSecours.isNotBlank(),
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        Text(
            stringResource(R.string.ob_email_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = viewModel.votreNom,
            onValueChange = { viewModel.votreNom = it },
            label = { Text(stringResource(R.string.ob_votre_nom)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        OutlinedTextField(
            value = viewModel.emailSecours,
            onValueChange = { viewModel.emailSecours = it },
            label = { Text(stringResource(R.string.ob_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

/** Étape 6 — licence : essai 7 j démarré (RA-04) ou code d'activation (RA-05). */
@Composable
internal fun LicenceStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_licence_title),
        viewModel = viewModel,
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        Text(
            stringResource(
                if (viewModel.licenceActive) R.string.ob_licence_active else R.string.ob_licence_essai,
            ),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = viewModel.codeLicence,
            onValueChange = { viewModel.codeLicence = it.uppercase() },
            label = { Text(stringResource(R.string.ob_code_licence)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        OutlinedButton(
            onClick = viewModel::activerCode,
            enabled = viewModel.codeLicence.isNotBlank() && !viewModel.enregistrementEnCours,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.ob_activer))
        }
        Text(
            stringResource(R.string.ob_licence_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
