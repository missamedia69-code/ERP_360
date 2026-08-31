package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217

/**
 * Étape 3 — identité entreprise : nom, devise ISO 4217 (D4, défaut USD),
 * pays → taux de taxe suggéré (D5, ex. 19,25 % Cameroun), site principal.
 * La validation pose les 4 verrous d'amont (RA-19) — plus modifiables ensuite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EnterpriseStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_entreprise_title),
        viewModel = viewModel,
        suivantActive = !viewModel.enregistrementEnCours,
    ) {
        OutlinedTextField(
            value = viewModel.nomEntreprise,
            onValueChange = { viewModel.nomEntreprise = it },
            label = { Text(stringResource(R.string.ob_nom_entreprise)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

        // Devise ISO 4217 (verrou d'amont — D4)
        var deviseOuvert by remember { mutableStateOf(false) }
        val deviseChoisie = Iso4217.COMMUNES.firstOrNull { it.code == viewModel.devise }
        ExposedDropdownMenuBox(
            expanded = deviseOuvert,
            onExpandedChange = { deviseOuvert = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = deviseChoisie?.let { "${it.code} — ${it.nom}" } ?: viewModel.devise,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.ob_devise)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviseOuvert) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = deviseOuvert, onDismissRequest = { deviseOuvert = false }) {
                Iso4217.COMMUNES.forEach { devise ->
                    DropdownMenuItem(
                        text = { Text("${devise.code} — ${devise.nom}") },
                        onClick = {
                            viewModel.devise = devise.code
                            deviseOuvert = false
                        },
                    )
                }
            }
        }

        // Pays → taxe suggérée (verrou d'amont — D5)
        var paysOuvert by remember { mutableStateOf(false) }
        val paysListe = Iso4217.TAXES_SUGGEREES.keys.toList()
        ExposedDropdownMenuBox(
            expanded = paysOuvert,
            onExpandedChange = { paysOuvert = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = viewModel.pays,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.ob_pays)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paysOuvert) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = paysOuvert, onDismissRequest = { paysOuvert = false }) {
                paysListe.forEach { pays ->
                    DropdownMenuItem(
                        text = { Text("$pays — TVA ${Iso4217.TAXES_SUGGEREES[pays]} %") },
                        onClick = {
                            viewModel.pays = pays
                            viewModel.tauxTaxe = Iso4217.TAXES_SUGGEREES[pays] ?: 0.0
                            paysOuvert = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = if (viewModel.tauxTaxe == 0.0) "" else viewModel.tauxTaxe.toString(),
            onValueChange = { viewModel.tauxTaxe = it.replace(',', '.').toDoubleOrNull() ?: 0.0 },
            label = { Text(stringResource(R.string.ob_taux_taxe)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.ob_verrous_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp),
        )

        OutlinedTextField(
            value = viewModel.nomSitePrincipal,
            onValueChange = { viewModel.nomSitePrincipal = it },
            label = { Text(stringResource(R.string.ob_site_principal)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}
