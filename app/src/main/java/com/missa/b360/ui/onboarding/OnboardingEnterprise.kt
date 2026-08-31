package com.missa.b360.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        // Le site principal reprend le nom de l'entreprise tant que l'utilisateur ne l'a
        // pas personnalisé. Un premier démarrage ne peut donc plus rester bloqué parce
        // que ce champ situé en bas du formulaire n'a pas encore été rempli.
        var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
        OutlinedTextField(
            value = viewModel.nomEntreprise,
            onValueChange = { nom ->
                val ancienNom = viewModel.nomEntreprise
                viewModel.nomEntreprise = nom
                if (!siteModifieManuellement || viewModel.nomSitePrincipal == ancienNom) {
                    viewModel.nomSitePrincipal = nom
                }
            },
            label = { Text(stringResource(R.string.ob_nom_entreprise)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

        // Catalogue ISO complet : la saisie sert de recherche et un choix renseigne le taux connu.
        val locale = LocalConfiguration.current.locales[0]
        val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
        var paysOuvert by remember { mutableStateOf(false) }
        var saisiePaysManuelle by remember { mutableStateOf(false) }
        // La requête reste distincte du pays choisi : rouvrir la liste montre tous les pays.
        var recherchePays by remember { mutableStateOf("") }
        val paysFiltres = remember(paysListe, recherchePays) {
            val requete = recherchePays.trim()
            if (requete.isEmpty()) {
                paysListe
            } else {
                paysListe.filter { pays ->
                    pays.nom.contains(requete, ignoreCase = true) ||
                        pays.code.contains(requete, ignoreCase = true)
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = paysOuvert,
            onExpandedChange = { ouvert ->
                paysOuvert = ouvert
                if (ouvert) recherchePays = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = if (paysOuvert) recherchePays else viewModel.pays,
                onValueChange = {
                    recherchePays = it
                    paysOuvert = true
                },
                label = { Text(stringResource(R.string.ob_pays)) },
                placeholder = { Text(stringResource(R.string.ob_pays_recherche)) },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paysOuvert) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = paysOuvert,
                onDismissRequest = {
                    paysOuvert = false
                    recherchePays = ""
                },
            ) {
                if (paysFiltres.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ob_pays_aucun_resultat)) },
                        onClick = {},
                        enabled = false,
                    )
                } else {
                    paysFiltres.forEach { pays ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${pays.nom} (${pays.code}) — " +
                                        "${stringResource(R.string.ob_tva_gst)} ${pays.libelleTaxe}",
                                )
                            },
                            onClick = {
                                viewModel.choisirPays(pays.nom, pays.tauxTaxeSuggere)
                                recherchePays = ""
                                paysOuvert = false
                            },
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = { saisiePaysManuelle = !saisiePaysManuelle },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.ob_pays_saisie_manuelle))
        }
        if (saisiePaysManuelle) {
            OutlinedTextField(
                value = viewModel.pays,
                onValueChange = { viewModel.pays = it },
                label = { Text(stringResource(R.string.ob_pays_personnalise)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Text(
                stringResource(R.string.ob_pays_saisie_manuelle_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val tauxTaxeInvalide = !viewModel.tauxTaxeEstValide()
        OutlinedTextField(
            value = viewModel.tauxTaxeTexte,
            onValueChange = viewModel::modifierTauxTaxe,
            label = { Text(stringResource(R.string.ob_taux_taxe)) },
            singleLine = true,
            isError = tauxTaxeInvalide,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = if (tauxTaxeInvalide) {
                { Text(stringResource(R.string.ob_erreur_taux_taxe)) }
            } else {
                null
            },
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
            onValueChange = {
                siteModifieManuellement = true
                viewModel.nomSitePrincipal = it
            },
            label = { Text(stringResource(R.string.ob_site_principal)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}
