package com.missa.b360.ui.clients.list

import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.missa.b360.ui.clients.components.ClientCarte
import com.missa.b360.ui.clients.components.ClientHero
import androidx.compose.foundation.layout.width
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

/** Carte « Vue d'ensemble » : dégradé bleu nuit, point vert « en temps réel » et trois chiffres (clients, encours, retard). */
@Composable
internal fun ClientListSummary(etat: ClientListUiState) {
    ClientHero {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cli_resume_titre),
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.cli_resume_temps_reel), color = Color(0xFFC3CBE0), fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chiffre(stringResource(R.string.cli_resume_clients), etat.compteurs.total.toString(), Color.White, Modifier.weight(0.72f), grande = true)
            Chiffre(
                stringResource(R.string.cli_resume_encours), clientMoney(etat.encoursTotal, etat.devise),
                Color.White, Modifier.weight(1.1f),
            )
            Chiffre(
                stringResource(R.string.cli_resume_en_retard), clientMoney(etat.enRetardTotal, etat.devise),
                if (etat.enRetardTotal > 0.0) Color(0xFFFFB4A8) else Color.White, Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
private fun Chiffre(libelle: String, valeur: String, couleur: Color, modifier: Modifier, grande: Boolean = false) {
    val forme = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(forme)
            .background(Color.White.copy(alpha = 0.09f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), forme)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(libelle, color = Color(0xFFC3CBE0), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valeur, color = couleur, fontSize = if (grande) 18.sp else 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Filtres rapides : pastille violette pleine pour le filtre courant, contour gris sinon, compteur dans un rond. */
@Composable
internal fun ClientFilterChips(etat: ClientListUiState, onFiltre: (ClientListFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        items(ClientListFilter.entries.size) { index ->
            val filtre = ClientListFilter.entries[index]
            val actif = etat.filtre == filtre
            val forme = RoundedCornerShape(999.dp)
            Box(
                Modifier.heightIn(min = 48.dp).clip(forme).clickable(role = Role.Tab) { onFiltre(filtre) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .heightIn(min = 33.dp)
                        .clip(forme)
                        .background(if (actif) ClientCouleurs.Violet else Color.White)
                        .then(if (actif) Modifier else Modifier.border(BorderStroke(1.dp, ClientCouleurs.Trait), forme))
                        .padding(start = 10.dp, end = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        stringResource(filtre.libelle()),
                        color = if (actif) Color.White else ClientCouleurs.NeutreTexte, fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (actif) Color(0x40000000).copy(alpha = 0.25f) else Color(0xFFEEF0F4)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            etat.compteurs.pour(filtre).toString(),
                            color = if (actif) Color.White else Color(0xFF667188), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

/** Ligne « 1 client » à gauche, menu « Trier : … » à droite. */
@Composable
internal fun ClientListeEntete(etat: ClientListUiState, onTri: (ClientListSort) -> Unit) {
    var menuOuvert by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val nombre = etat.lignes.size
        Text(
            if (nombre == 1) stringResource(R.string.cli_nb_client_un) else stringResource(R.string.cli_nb_clients_n, nombre),
            color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            BoutonClient(onClick = { menuOuvert = true }, plein = false) {
                Text(stringResource(R.string.cli_trier_par), color = MissaMuted, fontSize = 10.sp)
                Text(stringResource(etat.tri.libelle()), color = MissaInk, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Icon(painterResource(Iv.ExpandMore), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(16.dp))
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

/** Carte client : avatar violet, nom, code, téléphone, pied (statut, risque, encours) et chevron. Le toucher ouvre la fiche. */
@Composable
internal fun ClientListRow(
    ligne: ClientListItem,
    devise: String,
    onClick: () -> Unit,
) {
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
    ClientCarte(onClick = onClick, modifier = Modifier.heightIn(min = 64.dp)) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ClientAvatar(client.nom, taille = 47.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(client.nom, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(client.code, color = MissaMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(painterResource(Iv.Call), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(13.dp))
                    Text(client.telephone, color = Color(0xFF59657B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider(color = Color(0xFFEFF1F5), modifier = Modifier.padding(top = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClientStatusChip(client.statut)
                    if (risque != RiskLevel.NORMAL) RiskBadge(risque)
                    Spacer(Modifier.weight(1f))
                    val encours = ligne.encours
                    Text(
                        stringResource(R.string.cli_kpi_encours) + " ",
                        color = MissaMuted, fontSize = 10.sp, maxLines = 1,
                    )
                    Text(
                        clientMoney(encours, devise),
                        color = if (encours > 0.0) RisqueCouleurs.Eleve else MissaInk,
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    )
                }
                val retard = compte?.joursRetardMax ?: 0
                if (retard > 0) {
                    Text(stringResource(R.string.cli_retard_jours, retard), color = RisqueCouleurs.Eleve, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ligne.aRelancer) {
                    Icon(
                        painterResource(Iv.Notifications), contentDescription = null, tint = ClientCouleurs.Alerte,
                        modifier = Modifier.size(16.dp).semantics { contentDescription = aRelancerDescription },
                    )
                }
                Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = Color(0xFF8993A6), modifier = Modifier.size(16.dp))
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
                    .background(ClientCouleurs.Violet).padding(horizontal = 20.dp),
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
