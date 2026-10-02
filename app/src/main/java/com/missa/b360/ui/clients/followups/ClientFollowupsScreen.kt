package com.missa.b360.ui.clients.followups

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.ui.clients.account.libelleReleve
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.ClientReminderDialog
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.appeler
import com.missa.b360.ui.clients.components.clientDate
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Route `clients/relances` : qui me doit de l'argent, depuis quand, qui relancer aujourd'hui. */
@Composable
fun ClientFollowupsScreen(
    onBack: () -> Unit,
    onOuvrirClient: (Long) -> Unit,
    viewModel: ClientFollowupsViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    val contexte = LocalContext.current
    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)

    Scaffold(
        containerColor = MissaCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hote) },
        topBar = { ClientTopBar(titre = stringResource(R.string.cli_ouvrir_relances), onBack = onBack) },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::reessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            etat.vide -> MissaEmptyState(
                icon = Iv.CheckCircle,
                title = stringResource(R.string.cli_relances_vide_titre),
                description = stringResource(R.string.cli_relances_vide_desc),
                modifier = Modifier.padding(padding).padding(12.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.cli_total_en_retard, clientMoney(etat.totalEnRetard, etat.devise)),
                        color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    )
                }
                if (etat.nonTenues.isNotEmpty()) {
                    item { Titre(stringResource(R.string.cli_promesses_non_tenues)) }
                    items(etat.nonTenues.size) { index ->
                        val promesse = etat.nonTenues[index]
                        val date = promesse.promesseDate
                        val montant = promesse.promesseMontant
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
                            border = BorderStroke(1.dp, RisqueCouleurs.Eleve),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOuvrirClient(promesse.clientId) },
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(etat.noms[promesse.clientId].orEmpty(), color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                if (date != null && montant != null) {
                                    Text(
                                        stringResource(R.string.cli_promesse_ligne, clientMoney(montant, etat.devise), clientDate(date)),
                                        color = RisqueCouleurs.Eleve, fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                for ((tranche, clients) in etat.groupes) {
                    item { Titre(stringResource(tranche.libelleReleve()) + " (" + clients.size + ")") }
                    items(clients.size) { index ->
                        val ligne = clients[index]
                        LigneRelance(
                            ligne = ligne,
                            devise = etat.devise,
                            onOuvrir = { onOuvrirClient(ligne.client.id) },
                            onRelancer = { viewModel.ouvrirRelance(ligne.client.id) },
                            onAppeler = {
                                contexte.appeler(ligne.client.telephone)
                                viewModel.enregistrerAppel(ligne.client.id)
                            },
                        )
                    }
                }
            }
        }
    }

    val cible = etat.relance
    if (cible != null) {
        ClientReminderDialog(
            telephone = cible.client.telephone,
            messageInitial = stringResource(R.string.cli_relance_modele, cible.client.nom, clientMoney(cible.enRetard, etat.devise)),
            onEnvoyer = viewModel::enregistrerRelance,
            onDismiss = viewModel::fermerRelance,
        )
    }
}

@Composable
private fun Titre(texte: String) {
    Text(texte, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun LigneRelance(
    ligne: ClientListItem,
    devise: String,
    onOuvrir: () -> Unit,
    onRelancer: () -> Unit,
    onAppeler: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        modifier = Modifier.fillMaxWidth(),
        onClick = onOuvrir,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ligne.client.nom, color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    clientMoney(ligne.enRetard, devise) + " · " + stringResource(R.string.cli_retard_jours, ligne.balance?.joursRetardMax ?: 0),
                    color = RisqueCouleurs.Eleve, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                )
                if (!ligne.aRelancer) Text(stringResource(R.string.cli_deja_suivi), color = MissaMuted, fontSize = 13.sp)
            }
            IconButton(onClick = onAppeler, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(Iv.Call), stringResource(R.string.cli_appeler), tint = MissaInk)
            }
            Button(onClick = onRelancer, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cli_relancer)) }
        }
    }
}
