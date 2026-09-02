package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.SiteEntity

/** Multi-site (RA-21) : liste des sites + ajout. Le site principal est protégé. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSitesScreen(
    onBack: () -> Unit,
    viewModel: SitesViewModel = hiltViewModel(),
) {
    val sites by viewModel.sites.collectAsState(initial = emptyList())
    val message by viewModel.message.collectAsState()

    var nom by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Boutique") }

    val types = listOf(
        stringResource(R.string.adm_site_type_boutique),
        stringResource(R.string.adm_site_type_entrepot),
        stringResource(R.string.adm_site_type_usine),
        stringResource(R.string.adm_site_type_bureau),
    )

    AdminScaffold(titreRes = R.string.admin_multisite, onBack = onBack) {
        Text(
            stringResource(R.string.adm_sites_ajouter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it },
            label = { Text(stringResource(R.string.adm_sites_nom)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = adresse,
            onValueChange = { adresse = it },
            label = { Text(stringResource(R.string.adm_sites_adresse)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        var typeOuvert by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeOuvert,
            onExpandedChange = { typeOuvert = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.adm_sites_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeOuvert) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = typeOuvert, onDismissRequest = { typeOuvert = false }) {
                types.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t) },
                        onClick = {
                            type = t
                            typeOuvert = false
                        },
                    )
                }
            }
        }

        Button(
            onClick = {
                if (nom.isNotBlank()) viewModel.ajouter(nom, type, adresse.ifBlank { null })
                nom = ""
                adresse = ""
            },
            enabled = nom.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.adm_sites_ajouter))
        }
        when (message) {
            "ok" -> Message(stringResource(R.string.adm_sites_msg_ok), isError = false)
            "principal" -> Message(stringResource(R.string.adm_sites_msg_principal), isError = true)
            "supprime" -> Message(stringResource(R.string.adm_sites_msg_supprime), isError = false)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.admin_multisite),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        sites.forEach { site ->
            SiteCard(site, onDelete = { viewModel.supprimer(site) })
        }
    }
}

@Composable
private fun SiteCard(site: SiteEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(site.nom, fontWeight = FontWeight.Bold)
                Text(
                    "${site.type} · ${
                        if (site.principal) stringResource(R.string.adm_sites_principal)
                        else stringResource(R.string.adm_sites_secondaire)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!site.principal) {
                OutlinedButton(onClick = onDelete) {
                    Text(stringResource(R.string.adm_sites_supprimer))
                }
            }
        }
    }
}