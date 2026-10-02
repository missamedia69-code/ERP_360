package com.missa.b360.ui.clients.list

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ClientListFilter
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListSort
import com.missa.b360.core.domain.model.CreditInput
import com.missa.b360.core.domain.model.CreditPolicy
import com.missa.b360.core.domain.model.RiskLevel
import com.missa.b360.ui.clients.components.ClientAvatar
import com.missa.b360.ui.clients.components.ClientStatusChip
import com.missa.b360.ui.clients.components.RiskBadge
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private fun ClientListFilter.libelle(): Int = when (this) {
    ClientListFilter.TOUS -> R.string.cli_filtre_tous
    ClientListFilter.A_RELANCER -> R.string.cli_filtre_a_relancer
    ClientListFilter.BLOQUES -> R.string.cli_filtre_bloques
    ClientListFilter.SANS_ACHAT_90J -> R.string.cli_filtre_sans_achat
    ClientListFilter.NOUVEAUX -> R.string.cli_filtre_nouveaux
    ClientListFilter.INCOMPLETS -> R.string.cli_filtre_incomplets
}

private fun ClientListSort.libelle(): Int = when (this) {
    ClientListSort.ENCOURS -> R.string.cli_tri_encours
    ClientListSort.NOM -> R.string.cli_tri_nom
    ClientListSort.DERNIERE_VENTE -> R.string.cli_tri_derniere_vente
}

/** Résumé repliable : nombre de clients, encours total et part en retard. */
@Composable
internal fun ClientListSummary(etat: ClientListUiState) {
    var ouvert by rememberSaveable { mutableStateOf(true) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ouvert = !ouvert },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.cli_resume_titre),
                    color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
                    contentDescription = null, tint = MissaMuted, modifier = Modifier.size(24.dp),
                )
            }
            if (ouvert) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chiffre(stringResource(R.string.cli_resume_clients), etat.compteurs.total.toString(), MissaInk, Modifier.weight(1f))
                    Chiffre(
                        stringResource(R.string.cli_resume_encours), clientMoney(etat.encoursTotal, etat.devise),
                        MissaInk, Modifier.weight(1.4f),
                    )
                    Chiffre(
                        stringResource(R.string.cli_resume_en_retard), clientMoney(etat.enRetardTotal, etat.devise),
                        if (etat.enRetardTotal > 0.0) RisqueCouleurs.Eleve else MissaInk, Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Chiffre(libelle: String, valeur: String, couleur: Color, modifier: Modifier) {
    Column(modifier) {
        Text(libelle, color = MissaMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valeur, color = couleur, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ClientFilterChips(etat: ClientListUiState, onFiltre: (ClientListFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ClientListFilter.entries.size) { index ->
            val filtre = ClientListFilter.entries[index]
            FilterChip(
                selected = etat.filtre == filtre,
                onClick = { onFiltre(filtre) },
                label = {
                    Text(
                        stringResource(R.string.cli_chip_avec_compteur, stringResource(filtre.libelle()), etat.compteurs.pour(filtre)),
                        fontSize = 14.sp,
                    )
                },
            )
        }
    }
}

@Composable
internal fun ClientSortChips(tri: ClientListSort, onTri: (ClientListSort) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        item { Text(stringResource(R.string.cli_trier_par), color = MissaMuted, fontSize = 14.sp) }
        items(ClientListSort.entries.size) { index ->
            val choix = ClientListSort.entries[index]
            FilterChip(
                selected = tri == choix,
                onClick = { onTri(choix) },
                label = { Text(stringResource(choix.libelle()), fontSize = 14.sp) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientListRow(ligne: ClientListItem, devise: String, onClick: () -> Unit) {
    val client = ligne.client
    val compte = ligne.balance
    val risque = remember(client.statut, client.limiteCredit, compte) {
        CreditPolicy.evaluate(
            CreditInput(
                statut = client.statut,
                limiteCredit = client.limiteCredit,
                encours = compte?.encours ?: 0.0,
                enRetard = compte?.enRetard ?: 0.0,
                joursRetardMax = compte?.joursRetardMax ?: 0,
            ),
        ).risque
    }
    val aRelancerDescription = stringResource(R.string.cli_a_relancer_desc)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClientAvatar(client.nom)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(client.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${client.code} · ${client.telephone}", color = MissaMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClientStatusChip(client.statut)
                    if (risque != RiskLevel.NORMAL) RiskBadge(risque)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (ligne.encours > 0.0) {
                    Text(clientMoney(ligne.encours, devise), color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    val retard = compte?.joursRetardMax ?: 0
                    if (retard > 0) Text(stringResource(R.string.cli_retard_jours, retard), color = RisqueCouleurs.Eleve, fontSize = 13.sp)
                } else {
                    Text("—", color = MissaMuted, fontSize = 16.sp)
                }
                if (ligne.aRelancer) {
                    Icon(
                        painterResource(Iv.Notifications), contentDescription = null, tint = RisqueCouleurs.Attention,
                        modifier = Modifier.size(20.dp).semantics { contentDescription = aRelancerDescription },
                    )
                }
            }
        }
    }
}

/**
 * Ligne glissante : vers la droite on appelle, vers la gauche on écrit sur WhatsApp. La ligne
 * revient toujours à sa place ; le toucher simple ouvre la fiche.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientSwipeRow(
    ligne: ClientListItem,
    devise: String,
    onClick: () -> Unit,
    onAppeler: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val etat = rememberSwipeToDismissBoxState(
        confirmValueChange = { valeur ->
            when (valeur) {
                SwipeToDismissBoxValue.StartToEnd -> onAppeler()
                SwipeToDismissBoxValue.EndToStart -> onWhatsApp()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = etat,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            val appel = etat.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    .background(if (appel) RisqueCouleurs.Normal else Color(0xFF128C7E)).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (appel) Arrangement.Start else Arrangement.End,
            ) {
                Icon(
                    painterResource(if (appel) Iv.Call else Iv.Chat),
                    contentDescription = stringResource(if (appel) R.string.cli_appeler else R.string.cli_whatsapp),
                    tint = Color.White, modifier = Modifier.size(28.dp),
                )
            }
        },
    ) {
        ClientListRow(ligne, devise, onClick)
    }
}
