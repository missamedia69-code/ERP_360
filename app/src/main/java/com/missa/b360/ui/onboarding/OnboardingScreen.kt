package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite

/**
 * Hôte de l'onboarding (Phase B) : langue → profil/effectif → entreprise →
 * PIN → email de secours → licence → checklist (RA-01..RA-11).
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val termine = viewModel.onboardingTermine
    LaunchedEffect(termine) {
        if (termine) onFinished()
    }

    Surface(Modifier.fillMaxSize()) {
        when (viewModel.step) {
            OnboardingStep.LANGUE -> LanguageStep(viewModel)
            OnboardingStep.PROFIL -> ProfileStep(viewModel)
            OnboardingStep.ENTREPRISE -> EnterpriseStep(viewModel)
            OnboardingStep.PIN -> PinSetupStep(viewModel)
            OnboardingStep.EMAIL -> EmailStep(viewModel)
            OnboardingStep.LICENCE -> LicenceStep(viewModel)
            OnboardingStep.CHECKLIST -> ChecklistStep(viewModel)
        }
    }
}

/** Gabarit commun : titre + contenu + navigation Précédent / Suivant. */
@Composable
internal fun StepScaffold(
    title: String,
    viewModel: OnboardingViewModel,
    boutonSuivantRes: Int = R.string.ob_suivant,
    suivantActive: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        viewModel.erreurRes?.let { erreur ->
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(erreur),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.step != OnboardingStep.LANGUE) {
                OutlinedButton(onClick = viewModel::precedent) {
                    Text(stringResource(R.string.ob_retour))
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = viewModel::suivant, enabled = suivantActive) {
                Text(stringResource(boutonSuivantRes))
            }
        }
    }
}

/** Étape 1 — langue (RA-12 : 5 langues, français affiché en premier). */
@Composable
private fun LanguageStep(viewModel: OnboardingViewModel) {
    val langues = listOf(
        "fr" to R.string.langue_fr,
        "en" to R.string.langue_en,
        "es" to R.string.langue_es,
        "ar" to R.string.langue_ar,
        "zh" to R.string.langue_zh,
    )
    StepScaffold(title = stringResource(R.string.ob_langue_title), viewModel = viewModel) {
        langues.forEach { (code, labelRes) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = viewModel.langue == code,
                    onClick = { viewModel.choisirLangue(code) },
                )
                Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Étape 2 — profil d'activité (A–H) + palier d'effectif (P1–P6) — RA-20. */
@Composable
private fun ProfileStep(viewModel: OnboardingViewModel) {
    Column(Modifier.fillMaxSize()) {
        StepScaffold(title = stringResource(R.string.ob_profil_title), viewModel = viewModel) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(280.dp)) {
                items(ProfilActivite.entries) { profil ->
                    Card(
                        onClick = { viewModel.choisirProfil(profil) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.profil == profil) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Text(
                            stringResource(profil.labelRes),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.ob_palier_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            PalierTaille.entries.forEach { palier ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    RadioButton(
                        selected = viewModel.palier == palier,
                        onClick = { viewModel.choisirPalier(palier) },
                    )
                    Text(stringResource(palier.labelRes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
