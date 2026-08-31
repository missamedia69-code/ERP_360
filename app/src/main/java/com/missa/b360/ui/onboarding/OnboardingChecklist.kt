package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.CheckCircle
import com.missa.b360.R

/**
 * Étape 7 — checklist de fin de configuration (RA-11) : récapitulatif,
 * les 4 verrous d'amont affichés 🔒 (non masquables), skippable via « Suivant ».
 */
@Composable
internal fun ChecklistStep(viewModel: OnboardingViewModel) {
    val verrous = listOf(
        R.string.check_devise,
        R.string.check_taxes,
        R.string.check_numerotation,
        R.string.check_paiements,
    )
    val autres = listOf(
        R.string.check_entreprise,
        R.string.check_utilisateur,
        R.string.check_licence,
    )
    StepScaffold(
        title = stringResource(R.string.ob_checklist_title),
        viewModel = viewModel,
        boutonSuivantRes = R.string.ob_terminer,
    ) {
        autres.forEach { item ->
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = { Text(stringResource(item)) },
            )
        }
        // Les 4 verrous d'amont : non masquables (RA-11).
        verrous.forEach { item ->
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                },
                headlineContent = { Text(stringResource(item)) },
                supportingContent = { Text(stringResource(R.string.check_verrouille)) },
            )
        }
        Row(Modifier.padding(top = 8.dp)) {
            Icon(
                Icons.Outlined.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                stringResource(R.string.ob_checklist_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
