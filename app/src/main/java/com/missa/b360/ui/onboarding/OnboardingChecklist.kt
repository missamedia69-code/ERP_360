package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.ProfileGreen

/** Étape de fin : récapitule les prérequis et rend les quatre verrous clairement visibles. */
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
        subtitle = stringResource(R.string.ob_checklist_subtitle),
        viewModel = viewModel,
        boutonSuivantRes = R.string.ob_terminer,
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        Surface(
            shape = CircleShape,
            color = ProfileGreen.copy(alpha = 0.12f),
            modifier = Modifier.size(92.dp),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = ProfileGreen,
                    modifier = Modifier.size(50.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.ob_checklist_bienvenue),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(12.dp))
        OnboardingFormCard {
            autres.forEach { item -> CompletionRow(item, verrouille = false) }
            verrous.forEach { item -> CompletionRow(item, verrouille = true) }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandBlue.copy(alpha = 0.06f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.ob_checklist_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun CompletionRow(itemRes: Int, verrouille: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (verrouille) Icons.Outlined.Lock else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (verrouille) BrandBlue else ProfileGreen,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                text = stringResource(itemRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (verrouille) {
                Text(
                    text = stringResource(R.string.check_verrouille),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
