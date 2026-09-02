package com.missa.b360.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType

/** Écran Clients (9.2) : liste + recherche + catégories + badges + désactivation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val categories by viewModel.categoriesFlow.collectAsState(initial = emptyList())
    val badges by viewModel.badgesFlow.collectAsState(initial = emptyList())

    var recherche by remember { mutableStateOf("") }
    var formVisible by remember { mutableStateOf(false) }
    var catVisible by remember { mutableStateOf(false) }
    var badgeVisible by remember { mutableStateOf(false) }
    var clientEdite by remember { mutableStateOf<ClientEntity?>(null) }

    val filtres = clients.filter {
        recherche.isBlank() ||
            it.nom.contains(recherche, ignoreCase = true) ||
            it.telephone.contains(recherche)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.module_clients)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    clientEdite = null
                    formVisible = true
                },
            ) {
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
                label = { Text(stringResource(R.string.clients_recherche)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    stringResource(R.string.clients_total, filtres.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { catVisible = true }) {
                    Text(stringResource(R.string.clients_categories))
                }
                TextButton(onClick = { badgeVisible = true }) {
                    Text(stringResource(R.string.clients_badges))
                }
            }
            if (filtres.isEmpty()) {
                Text(
                    stringResource(R.string.clients_vide),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                LazyColumn {
                    items(filtres, key = { it.id }) { client ->
                        ClientCard(
                            client = client,
                            nomCategorie = categories.firstOrNull { it.id == client.categorieId }?.nom,
                            badge = badges.firstOrNull { it.id == client.badgeId },
                            onClick = { /* TODO: ouvrir fiche client */ }
                        ) { viewModel.desactiver(client.id) }
                    }
                }
            }
        }
    }

    if (formVisible) {
        val client = clientEdite
        ClientFormDialog(
            client = client,
            categories = categories,
            badges = badges,
            onDismiss = { formVisible = false }
        ) { nom, tel, type, email, adresse, catId, remise, limite, badgeId, notes ->
            if (client == null) {
                viewModel.creer(nom, tel, type, doublonConfirme = true)
            } else {
                viewModel.modifier(
                    client.id, nom, tel, type, email, adresse, catId, remise, limite, badgeId, notes,
                )
            }
            formVisible = false
        }
    }
    if (catVisible) CategoriesDialog(
        categories = categories,
        onCreer = { viewModel.creerCategorie(it) },
        onSupprimer = { id -> viewModel.supprimerCategorie(id) }
    ) { catVisible = false }
    if (badgeVisible) BadgesDialog(
        badges = badges,
        onCreer = { nom, remise -> viewModel.creerBadge(nom, remise) }
    ) { badgeVisible = false }
}

@Composable
private fun ClientCard(
    client: ClientEntity,
    nomCategorie: String?,
    badge: BadgeLoyaltyEntity?,
    onClick: () -> Unit,
    onDesactiver: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(client.nom, fontWeight = FontWeight.Bold)
                Text(
                    "${client.code} · ${client.telephone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                val dettails = buildList {
                    nomCategorie?.let { add(it) }
                    if (client.type != ClientType.PARTICULIER) add(client.type.name)
                }.joinToString(" · ")
                if (dettails.isNotEmpty()) {
                    Text(dettails, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (client.prospect) {
                    Text(
                        stringResource(R.string.clients_prospect),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            badge?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${it.remisePct}%", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (client.statut == ClientStatus.DESACTIVE) return@Card
            TextButton(onClick = onDesactiver) {
                Text(stringResource(R.string.clients_desactiver))
            }
        }
    }
}