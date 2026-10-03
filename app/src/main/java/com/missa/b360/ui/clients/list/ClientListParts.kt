package com.missa.b360.ui.clients.list

import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.Role
import com.missa.b360.ui.clients.components.BoutonClient
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

/** Résumé repliable « En temps réel » : nombre de clients, encours total et part en retard. */
@Composable
internal fun ClientListSummary(etat: ClientListUiState) {
    var ouvert by rememberSaveable { mutableStateOf(true) }
    val forme = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forme)
            .background(ClientCouleurs.Carte)
            .border(BorderStroke(1.dp, ClientCouleurs.CarteBord), forme)
            .padding(horizontal = 14.dp, vertical = 2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { ouvert = !ouvert },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.cli_resume_titre),
                color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.cli_resume_temps_reel), color = MissaMuted, fontSize = 11.sp)
            Spacer(Modifier.width(6.dp))
            Icon(
                painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
                contentDescription = null, tint = MissaMuted, modifier = Modifier.size(20.dp),
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

@Composable
private fun Chiffre(libelle: String, valeur: String, couleur: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(libelle, color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valeur, color = couleur, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Filtres rapides : pastille violette pour le filtre courant, contour gris sinon, compteur en pastille. */
@Composable
internal fun ClientFilterChips(etat: ClientListUiState, onFiltre: (ClientListFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        items(ClientListFilter.entries.size) { index ->
            val filtre = ClientListFilter.entries[index]
            val actif = etat.filtre == filtre
            val forme = RoundedCornerShape(12.dp)
            Box(
                Modifier.heightIn(min = 48.dp).clip(forme).clickable(role = Role.Tab) { onFiltre(filtre) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .heightIn(min = 36.dp)
                        .clip(forme)
                        .background(if (actif) ClientCouleurs.Violet else Color.White)
                        .then(if (actif) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.Trait), forme))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(filtre.libelle()),
                        color = if (actif) Color.White else MissaInk, fontSize = 12.sp,
                        fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (actif) ClientCouleurs.VioletProfond else ClientCouleurs.Pastille)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            etat.compteurs.pour(filtre).toString(),
                            color = if (actif) Color.White else MissaInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/** Ligne « 1 client » à gauche, menu « Trier par … » à droite. */
@Composable
internal fun ClientListeEntete(etat: ClientListUiState, onTri: (ClientListSort) -> Unit) {
    var menuOuvert by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val nombre = etat.lignes.size
        Text(
            if (nombre == 1) stringResource(R.string.cli_nb_client_un) else stringResource(R.string.cli_nb_clients_n, nombre),
            color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            BoutonClient(onClick = { menuOuvert = true }, plein = false) {
                Text(stringResource(R.string.cli_trier_par), color = MissaMuted, fontSize = 12.sp)
                Text(stringResource(etat.tri.libelle()), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(painterResource(Iv.ExpandMore), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                ClientListSort.entries.forEach { choix ->
                    DropdownMenuItem(
                        text = { Text(stringResource(choix.libelle())) },
                        onClick = {
                            menuOuvert = false
                            onTri(choix)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientListRow(
    ligne: ClientListItem,
    devise: String,
    onClick: () -> Unit,
    onAppeler: () -> Unit = {},
    onWhatsApp: () -> Unit = {},
) {
    val client = ligne.client
    val compte = ligne.balance
    var detailOuvert by rememberSaveable(client.id) { mutableStateOf(false) }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClientCouleurs.Carte),
        border = BorderStroke(1.dp, ClientCouleurs.CarteBord),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Column(Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClientAvatar(client.nom, taille = 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(client.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(client.code, color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(painterResource(Iv.Call), contentDescription = null, tint = MissaMuted, modifier = Modifier.size(12.dp))
                        Text(client.telephone, color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ClientStatusChip(client.statut)
                        if (risque != RiskLevel.NORMAL) RiskBadge(risque)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { detailOuvert = !detailOuvert }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painterResource(if (detailOuvert) Iv.ExpandLess else Iv.ExpandMore),
                            contentDescription = null, tint = MissaMuted, modifier = Modifier.size(20.dp),
                        )
                    }
                    if (ligne.encours > 0.0) {
                        Text(clientMoney(ligne.encours, devise), color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(end = 8.dp))
                        val retard = compte?.joursRetardMax ?: 0
                        if (retard > 0) Text(stringResource(R.string.cli_retard_jours, retard), color = RisqueCouleurs.Eleve, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                    }
                    if (ligne.aRelancer) {
                        Icon(
                            painterResource(Iv.Notifications), contentDescription = null, tint = RisqueCouleurs.Attention,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp).semantics { contentDescription = aRelancerDescription },
                        )
                    }
                }
            }
            if (detailOuvert) {
                Column(Modifier.padding(top = 4.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chiffre(stringResource(R.string.cli_kpi_encours), clientMoney(compte?.encours ?: 0.0, devise), MissaInk, Modifier.weight(1f))
                        Chiffre(
                            stringResource(R.string.cli_kpi_en_retard), clientMoney(compte?.enRetard ?: 0.0, devise),
                            if ((compte?.enRetard ?: 0.0) > 0.0) RisqueCouleurs.Eleve else MissaInk, Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoutonClient(onClick = onAppeler, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(Iv.Call), contentDescription = null, tint = MissaInk, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.cli_appeler), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        BoutonClient(onClick = onWhatsApp, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(Iv.Chat), contentDescription = null, tint = MissaInk, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.cli_whatsapp), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
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
        ClientListRow(ligne, devise, onClick, onAppeler, onWhatsApp)
    }
}
