package com.missa.b360.ui.clients.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.ClientOnglets
import com.missa.b360.ui.clients.components.ClientReminderDialog
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.clients.components.appeler
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.clients.components.envoyerSms
import com.missa.b360.ui.clients.components.libelle
import com.missa.b360.ui.clients.components.ouvrirWhatsApp
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk

/** Route `clients/{id}` : fiche 360 ; l'identifiant vient des arguments de navigation. */
@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onModifier: (Long) -> Unit,
    onCompte: (Long) -> Unit,
    onActivite: (Long) -> Unit,
    onVendre: (Long) -> Unit,
    viewModel: ClientDetailViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    val contexte = LocalContext.current
    var relanceOuverte by rememberSaveable { mutableStateOf(false) }
    var noteOuverte by rememberSaveable { mutableStateOf(false) }
    var statutDemande by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOuvert by rememberSaveable { mutableStateOf(false) }
    val client = etat.client
    val produits = if (etat.onglet == ClientTab.CONDITIONS) viewModel.produits.collectAsState().value else emptyList()

    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)

    Scaffold(
        containerColor = ClientCouleurs.Fond,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hote) },
        topBar = {
            ClientTopBar(titre = stringResource(R.string.cli_fiche_titre), onBack = onBack) {
                if (client != null) {
                    IconButton(onClick = { onModifier(client.id) }) {
                        Icon(painterResource(Iv.Edit), stringResource(R.string.clients_modifier), tint = ClientCouleurs.Violet)
                    }
                    if (etat.transitions.isNotEmpty()) {
                        IconButton(onClick = { menuOuvert = true }) {
                            Icon(painterResource(Iv.MoreVert), stringResource(R.string.cli_changer_statut), tint = MissaInk)
                        }
                        DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                            etat.transitions.forEach { vers ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(vers.libelle())) },
                                    onClick = {
                                        menuOuvert = false
                                        if (ClientLifecycleRules.confirmationRequise(vers)) statutDemande = vers.name
                                        else viewModel.changerStatut(vers)
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::reessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            etat.introuvable || client == null -> EtatErreur(
                onReessayer = onBack,
                message = stringResource(R.string.cli_fiche_introuvable),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ClientDetailHeader(etat) }
                item { ClientDetailMetriques(etat) }
                item {
                    ClientDetailActions(
                        onVendre = { onVendre(client.id) },
                        onEncaisser = { onCompte(client.id) },
                        onRelancer = { relanceOuverte = true },
                        onAppeler = {
                            contexte.appeler(client.telephone)
                            viewModel.enregistrerAppel()
                        },
                        onWhatsApp = { contexte.ouvrirWhatsApp(client.telephone) },
                        onSms = { contexte.envoyerSms(client.telephone) },
                    )
                }
                item {
                    ClientOnglets(
                        onglets = ClientTab.entries,
                        courant = etat.onglet,
                        libelle = { it.libelle() },
                        onChoix = { onglet -> viewModel.changerOnglet(onglet) },
                    )
                }
                ongletClient(
                    etat = etat,
                    client = client,
                    onVoirActivite = { onActivite(client.id) },
                    onVoirCompte = { onCompte(client.id) },
                    onNote = { noteOuverte = true },
                    onAppelerContact = { contexte.appeler(it) },
                    produits = produits,
                    onPrixDefini = viewModel::definirPrix,
                    onPrixRetire = viewModel::retirerPrix,
                )
            }
        }
    }

    if (relanceOuverte && client != null) {
        val montant = etat.balance?.let { if (it.enRetard > 0.0) it.enRetard else it.encours } ?: 0.0
        ClientReminderDialog(
            telephone = client.telephone,
            messageInitial = stringResource(R.string.cli_relance_modele, client.nom, clientMoney(montant, etat.devise)),
            onEnvoyer = { canal, message ->
                relanceOuverte = false
                viewModel.enregistrerRelance(canal, message)
            },
            onDismiss = { relanceOuverte = false },
        )
    }
    if (noteOuverte) {
        var texte by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noteOuverte = false },
            title = { Text(stringResource(R.string.cli_ajouter_note)) },
            text = {
                MissaChampTexte(valeur = texte, onValeur = { texte = it.take(500) }, libelle = stringResource(R.string.cli_message), lignes = 4)
            },
            confirmButton = {
                TextButton(
                    enabled = texte.isNotBlank(),
                    onClick = {
                        viewModel.ajouterNote(texte)
                        noteOuverte = false
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { Text(stringResource(R.string.cli_enregistrer)) }
            },
            dismissButton = { TextButton(onClick = { noteOuverte = false }) { Text(stringResource(R.string.cli_annuler)) } },
        )
    }
    val cible = statutDemande?.let { nom -> ClientStatus.entries.firstOrNull { it.name == nom } }
    if (cible != null) {
        AlertDialog(
            onDismissRequest = { statutDemande = null },
            title = { Text(stringResource(R.string.cli_confirmer_statut_titre, stringResource(cible.libelle()))) },
            text = { Text(stringResource(cible.consequence())) },
            confirmButton = {
                TextButton(onClick = {
                    statutDemande = null
                    viewModel.changerStatut(cible)
                }) { Text(stringResource(R.string.cli_confirmer)) }
            },
            dismissButton = { TextButton(onClick = { statutDemande = null }) { Text(stringResource(R.string.cli_annuler)) } },
        )
    }
}

private fun ClientTab.libelle(): Int = when (this) {
    ClientTab.ACTIVITE -> R.string.cli_onglet_activite
    ClientTab.COMPTE -> R.string.cli_onglet_compte
    ClientTab.CONTACTS -> R.string.cli_onglet_contacts
    ClientTab.CONDITIONS -> R.string.cli_onglet_conditions
    ClientTab.NOTES -> R.string.cli_onglet_notes
}

/** Conséquence expliquée avant de confirmer un changement de statut sensible. */
private fun ClientStatus.consequence(): Int = when (this) {
    ClientStatus.BLOQUE_CREDIT, ClientStatus.BLOQUE_ADMINISTRATIF -> R.string.cli_consequence_blocage
    ClientStatus.INACTIF, ClientStatus.DESACTIVE -> R.string.cli_consequence_inactif
    else -> R.string.cli_consequence_archive
}
