package com.missa.b360.ui.clients.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.ActivityEntry
import com.missa.b360.core.domain.model.ActivityFilter
import com.missa.b360.core.domain.model.ActivityKind
import com.missa.b360.ui.clients.components.ClientFollowupRow
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.clientDate
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private fun ActivityFilter.libelle(): Int = when (this) {
    ActivityFilter.TOUT -> R.string.cli_filtre_tout
    ActivityFilter.VENTES -> R.string.cli_activite_ventes
    ActivityFilter.ENCAISSEMENTS -> R.string.cli_encaissements
    ActivityFilter.SUIVI -> R.string.cli_activite_suivi
}

private fun ActivityKind.libelle(): Int = when (this) {
    ActivityKind.VENTE -> R.string.cli_activite_vente
    ActivityKind.AVOIR -> R.string.cli_activite_avoir
    ActivityKind.ENCAISSEMENT -> R.string.cli_activite_encaissement
    ActivityKind.RELANCE -> R.string.cli_type_relance
    ActivityKind.APPEL -> R.string.cli_type_appel
    ActivityKind.PROMESSE -> R.string.cli_type_promesse
    ActivityKind.NOTE -> R.string.cli_type_note
}

/** Route `clients/{id}/activite` : chronologie complète des ventes, encaissements et suivis. */
@Composable
fun ClientActivityScreen(
    onBack: () -> Unit,
    viewModel: ClientActivityViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    Scaffold(
        containerColor = MissaCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ClientTopBar(titre = stringResource(R.string.cli_onglet_activite), onBack = onBack) },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::reessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            etat.introuvable -> EtatErreur(
                onReessayer = onBack,
                message = stringResource(R.string.cli_fiche_introuvable),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(etat.nomClient, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ActivityFilter.entries.size) { index ->
                            val filtre = ActivityFilter.entries[index]
                            FilterChip(
                                selected = etat.filtre == filtre,
                                onClick = { viewModel.choisirFiltre(filtre) },
                                label = { Text(stringResource(filtre.libelle())) },
                            )
                        }
                    }
                }
                if (etat.entrees.isEmpty()) {
                    item {
                        MissaEmptyState(
                            icon = Iv.History,
                            title = stringResource(R.string.cli_activite_vide_titre),
                            description = stringResource(R.string.cli_activite_vide_desc),
                        )
                    }
                } else {
                    items(etat.entrees.size) { index ->
                        LigneActivite(etat.entrees[index], etat.devise)
                    }
                }
            }
        }
    }
}

@Composable
private fun LigneActivite(entree: ActivityEntry, devise: String) {
    val suivi = entree.followup
    if (suivi != null) {
        ClientFollowupRow(suivi, devise)
        return
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(stringResource(entree.kind.libelle()), color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val reference = entree.reference.orEmpty()
                Text(
                    clientDate(entree.date) + if (reference.isNotBlank()) " · $reference" else "",
                    color = MissaMuted, fontSize = 13.sp,
                )
            }
            val montant = entree.montant
            if (montant != null) {
                val signe = if (entree.kind == ActivityKind.VENTE) "" else "−"
                Text(
                    signe + clientMoney(montant, devise),
                    color = if (entree.kind == ActivityKind.VENTE) MissaInk else RisqueCouleurs.Normal,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                )
            }
        }
    }
}
