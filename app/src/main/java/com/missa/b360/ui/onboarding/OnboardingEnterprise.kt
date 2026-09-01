package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.OnboardingBorder

/**
 * Étape identité entreprise : les verrous métier D4/D5 et le catalogue pays restent
 * inchangés, présentés dans une carte de formulaire compacte de la nouvelle maquette.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EnterpriseStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_entreprise_title),
        subtitle = stringResource(R.string.ob_entreprise_subtitle),
        viewModel = viewModel,
        suivantActive = !viewModel.enregistrementEnCours,
        navigationActive = !viewModel.enregistrementEnCours,
    ) {
        var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
        val locale = LocalConfiguration.current.locales[0]
        val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
        var deviseOuvert by remember { mutableStateOf(false) }
        var paysOuvert by remember { mutableStateOf(false) }
        var saisiePaysManuelle by remember { mutableStateOf(false) }
        var recherchePays by remember { mutableStateOf("") }
        val deviseChoisie = Iso4217.COMMUNES.firstOrNull { it.code == viewModel.devise }
        val paysFiltres = remember(paysListe, recherchePays) {
            val requete = recherchePays.trim()
            if (requete.isEmpty()) paysListe else paysListe.filter { pays ->
                pays.nom.contains(requete, ignoreCase = true) ||
                    pays.code.contains(requete, ignoreCase = true)
            }
        }
        val tauxTaxeInvalide = !viewModel.tauxTaxeEstValide()

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OnboardingBorder),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.ob_informations_entreprise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
                    enabled = !viewModel.enregistrementEnCours,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = deviseOuvert,
                    onExpandedChange = { deviseOuvert = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = deviseChoisie?.let { "${it.code} · ${it.nom}" } ?: viewModel.devise,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.ob_devise)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviseOuvert) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = deviseOuvert,
                        onDismissRequest = { deviseOuvert = false },
                    ) {
                        Iso4217.COMMUNES.forEach { devise ->
                            DropdownMenuItem(
                                text = { Text("${devise.code} · ${devise.nom}") },
                                onClick = {
                                    viewModel.devise = devise.code
                                    deviseOuvert = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = paysOuvert,
                    onExpandedChange = { ouvert ->
                        paysOuvert = ouvert
                        if (ouvert) recherchePays = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                                            "${pays.nom} (${pays.code}) · " +
                                                "${stringResource(R.string.ob_tva_gst)} ${pays.libelleTaxe}",
                                        )
                                    },
                                    onClick = {
                                        viewModel.choisirPays(pays.nom, pays.code, pays.tauxTaxeSuggere)
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
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text(stringResource(R.string.ob_pays_saisie_manuelle))
                }
                if (saisiePaysManuelle) {
                    OutlinedTextField(
                        value = viewModel.pays,
                        onValueChange = viewModel::modifierPaysManuel,
                        label = { Text(stringResource(R.string.ob_pays_personnalise)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.ob_pays_saisie_manuelle_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = viewModel.nomSitePrincipal,
                    onValueChange = {
                        siteModifieManuellement = true
                        viewModel.nomSitePrincipal = it
                    },
                    label = { Text(stringResource(R.string.ob_site_principal)) },
                    singleLine = true,
                    enabled = !viewModel.enregistrementEnCours,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandBlue.copy(alpha = 0.06f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = BrandBlue,
                )
                Text(
                    text = stringResource(R.string.ob_verrous_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}
