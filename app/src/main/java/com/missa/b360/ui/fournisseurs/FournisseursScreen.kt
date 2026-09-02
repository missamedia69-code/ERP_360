package com.missa.b360.ui.fournisseurs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurEntity

/** Écran Fournisseurs (9.3) : liste + recherche + formulaire (RF-01 doublons). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FournisseursScreen(
    onBack: () -> Unit,
    viewModel: FournisseursViewModel = hiltViewModel(),
) {
    val fournisseurs by viewModel.fournisseurs.collectAsState(initial = emptyList())
    var recherche by remember { mutableStateOf("") }
    var formVisible by remember { mutableStateOf(false) }
    var fournisseurEdite by remember { mutableStateOf<FournisseurEntity?>(null) }

    val filtres = fournisseurs.filter {
        recherche.isBlank() ||
            it.nom.contains(recherche, ignoreCase = true) ||
            it.telephone.contains(recherche)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.module_fournisseurs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                fournisseurEdite = null
                formVisible = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = recherche,
                onValueChange = { recherche = it },
                label = { Text(stringResource(R.string.fournisseurs_recherche)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Text(
                stringResource(R.string.fournisseurs_total, filtres.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (filtres.isEmpty()) {
                Text(
                    stringResource(R.string.fournisseurs_vide),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                LazyColumn {
                    items(filtres, key = { it.id }) { fournisseur ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    fournisseurEdite = fournisseur
                                    formVisible = true
                                },
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(fournisseur.nom, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${fournisseur.code} · ${fournisseur.telephone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                TextButton(onClick = { viewModel.desactiver(fournisseur.id) }) {
                                    Text(stringResource(R.string.fournisseurs_desactiver))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (formVisible) {
        val fournisseur = fournisseurEdite
        FournisseurFormDialog(
            fournisseur = fournisseur,
            onDismiss = { formVisible = false },
            onConfirm = { nom, tel ->
                if (fournisseur == null) {
                    viewModel.creer(nom, tel, doublonConfirme = true)
                } else {
                    viewModel.modifier(fournisseur.id, nom, tel)
                }
                formVisible = false
            },
        )
    }
}