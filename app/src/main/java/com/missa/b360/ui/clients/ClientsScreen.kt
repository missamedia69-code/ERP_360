package com.missa.b360.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.util.MoneyUtils

/** Écran Clients (9.2) : liste + recherche + catégories + badges + désactivation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyClientsScreen(
    onBack: () -> Unit,
    /** Vrai lorsqu'une action rapide de l'accueil demande directement la création. */
    openCreate: Boolean = false,
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val categories by viewModel.categoriesFlow.collectAsState(initial = emptyList())
    val badges by viewModel.badgesFlow.collectAsState(initial = emptyList())
    val sites by viewModel.sitesFlow.collectAsState(initial = emptyList())
    val deviseEntreprise by viewModel.deviseEntreprise.collectAsState()
    val codePaysParDefaut by viewModel.codePaysParDefaut.collectAsState()
    val resultat by viewModel.resultat.collectAsState()
    val erreurCategorie by viewModel.erreurCategorie.collectAsState()
    val soldes by viewModel.soldes.collectAsState()
    val rappelMessage by viewModel.rappelMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var recherche by remember { mutableStateOf("") }
    var formVisible by remember { mutableStateOf(false) }
    var catVisible by remember { mutableStateOf(false) }
    var badgeVisible by remember { mutableStateOf(false) }
    var clientEdite by remember { mutableStateOf<ClientEntity?>(null) }

    LaunchedEffect(openCreate) {
        if (openCreate) {
            clientEdite = null
            formVisible = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.rafraichirSoldes()
    }

    LaunchedEffect(rappelMessage) {
        val ref = rappelMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(R.string.rappel_envoye, ref))
        viewModel.acquitterRappel()
    }

    val retourActuel = resultat
    val messageRetour = when {
        retourActuel == null || retourActuel.erreur == "doublon" -> null
        retourActuel.code == "edit" -> stringResource(R.string.clients_client_modifie)
        retourActuel.code != null -> stringResource(R.string.clients_client_cree, retourActuel.code)
        retourActuel.erreur == "licence" -> stringResource(R.string.clients_lecture_seule)
        retourActuel.erreur == "nom" -> stringResource(R.string.clients_nom_obligatoire)
        retourActuel.erreur == "nom_invalide" -> stringResource(R.string.clients_nom_invalide)
        retourActuel.erreur == "telephone" -> stringResource(R.string.clients_telephone_obligatoire)
        retourActuel.erreur == "telephone_invalide" -> stringResource(R.string.clients_telephone_invalide)
        retourActuel.erreur == "email_invalide" -> stringResource(R.string.clients_email_invalide)
        retourActuel.erreur == "donnees" -> stringResource(R.string.clients_donnees_invalides)
        else -> stringResource(R.string.clients_erreur_sauvegarde)
    }
    val messageCategorie = when (erreurCategorie) {
        "utilisee" -> stringResource(R.string.clients_categorie_utilisee)
        "licence" -> stringResource(R.string.clients_lecture_seule)
        "err" -> stringResource(R.string.clients_erreur_sauvegarde)
        else -> null
    }

    LaunchedEffect(messageRetour) {
        messageRetour ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(messageRetour)
        viewModel.acquitterResultat()
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            solde = soldes[client.id] ?: 0.0,
                            devise = deviseEntreprise ?: "XAF",
                            onClick = {
                                clientEdite = client
                                formVisible = true
                            },
                            onDesactiver = { viewModel.desactiver(client.id) },
                            onRappel = { viewModel.rappelPaiement(client.id) },
                        )
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
            sites = sites,
            deviseEntreprise = deviseEntreprise,
            codePaysParDefaut = codePaysParDefaut,
            onDismiss = { formVisible = false },
        ) { nom, tel, type, email, adresse, catId, siteId, remise, limite, badgeId, notes ->
            if (client == null) {
                viewModel.creer(
                    nom = nom,
                    telephone = tel,
                    type = type,
                    email = email,
                    adresse = adresse,
                    categorieId = catId,
                    siteId = siteId,
                    remiseDefautPct = remise,
                    limiteCredit = limite,
                    badgeId = badgeId,
                    notes = notes,
                )
            } else {
                viewModel.modifier(
                    client.id,
                    nom,
                    tel,
                    type,
                    email,
                    adresse,
                    catId,
                    siteId,
                    remise,
                    limite,
                    badgeId,
                    notes,
                )
            }
            formVisible = false
        }
    }
    if (resultat?.erreur == "doublon") {
        AlertDialog(
            onDismissRequest = viewModel::annulerDoublon,
            title = { Text(stringResource(R.string.clients_doublon_titre)) },
            text = { Text(stringResource(R.string.clients_doublon_message)) },
            confirmButton = {
                Button(onClick = viewModel::confirmerDoublon) {
                    Text(stringResource(R.string.clients_creer_malgre_doublon))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::annulerDoublon) {
                    Text(stringResource(R.string.ob_retour))
                }
            },
        )
    }
    if (catVisible) {
        CategoriesDialog(
            categories = categories,
            message = messageCategorie,
            onCreer = viewModel::creerCategorie,
            onSupprimer = viewModel::supprimerCategorie,
            onDismiss = { catVisible = false },
        )
    }
    if (badgeVisible) {
        BadgesDialog(
            badges = badges,
            onCreer = viewModel::creerBadge,
            onDismiss = { badgeVisible = false },
        )
    }
}

@Composable
private fun ClientCard(
    client: ClientEntity,
    nomCategorie: String?,
    badge: BadgeLoyaltyEntity?,
    solde: Double,
    devise: String,
    onClick: () -> Unit,
    onDesactiver: () -> Unit,
    onRappel: () -> Unit,
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
                val typeClient = stringResource(client.type.labelRes())
                val details = buildList {
                    nomCategorie?.let { add(it) }
                    if (client.type != ClientType.PARTICULIER) add(typeClient)
                }.joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (client.prospect) {
                    Text(
                        stringResource(R.string.clients_prospect),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (solde > 0.001) {
                    Text(
                        stringResource(R.string.rappel_solde, MoneyUtils.format(solde, devise)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            }
            badge?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${it.remisePct}%", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (solde > 0.001 && client.statut != ClientStatus.DESACTIVE) {
                IconButton(onClick = onRappel) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.rappel_bell_cd),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (client.statut == ClientStatus.DESACTIVE) return@Card
            TextButton(onClick = onDesactiver) {
                Text(stringResource(R.string.clients_desactiver))
            }
        }
    }
}
