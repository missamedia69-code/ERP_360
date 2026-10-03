package com.missa.b360.ui.clients.detail

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.ui.clients.components.BoutonClient
import com.missa.b360.ui.clients.components.ClientCarte
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientTitreSection
import com.missa.b360.ui.clients.components.ClientVideActivite
import com.missa.b360.ui.clients.components.ClientPricesCard
import com.missa.b360.ui.clients.components.ClientFollowupRow
import com.missa.b360.ui.clients.components.LigneInfo
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private const val APERCU_ACTIVITE = 5

@Composable
private fun CarteClient(contenu: @Composable () -> Unit) {
    ClientCarte { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenu() } }
}

@Composable
private fun Vide(message: Int) {
    Text(stringResource(message), color = MissaMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
}

/** Contenu de l'onglet courant, ajouté comme éléments de la liste verticale de la fiche. */
internal fun LazyListScope.ongletClient(
    etat: ClientDetailUiState,
    client: ClientEntity,
    onVoirActivite: () -> Unit,
    onVoirCompte: () -> Unit,
    onNote: () -> Unit,
    onAppelerContact: (String) -> Unit,
    produits: List<ProductEntity>,
    onPrixDefini: (Long, Double) -> Unit,
    onPrixRetire: (Long) -> Unit,
) {
    item {
        val (icone, titre) = when (etat.onglet) {
            ClientTab.ACTIVITE -> Iv.History to R.string.cli_onglet_activite
            ClientTab.COMPTE -> Iv.Payments to R.string.cli_onglet_compte
            ClientTab.CONTACTS -> Iv.Group to R.string.cli_onglet_contacts
            ClientTab.CONDITIONS -> Iv.Description to R.string.cli_onglet_conditions
            ClientTab.NOTES -> Iv.Edit to R.string.cli_onglet_notes
        }
        ClientTitreSection(icone, stringResource(titre))
    }
    when (etat.onglet) {
        ClientTab.ACTIVITE -> {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BoutonClient(onClick = onNote, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(Iv.Add), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.cli_ajouter_note), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    BoutonClient(onClick = onVoirActivite, modifier = Modifier.weight(1f), plein = false) {
                        Text(stringResource(R.string.cli_voir_activite), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Icon(painterResource(Iv.ExpandMore), contentDescription = null, tint = MissaInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (etat.suivis.isEmpty()) {
                item { ClientVideActivite() }
            } else {
                items(etat.suivis.take(APERCU_ACTIVITE).size) { index ->
                    ClientFollowupRow(etat.suivis[index], etat.devise)
                }
            }
        }
        ClientTab.COMPTE -> item {
            val compte = etat.balance
            CarteClient {
                LigneInfo(stringResource(R.string.cli_kpi_encours), clientMoney(compte?.encours ?: 0.0, etat.devise))
                LigneInfo(stringResource(R.string.cli_kpi_en_retard), clientMoney(compte?.enRetard ?: 0.0, etat.devise))
                LigneInfo(stringResource(R.string.cli_kpi_retard_max), stringResource(R.string.cli_jours, compte?.joursRetardMax ?: 0))
                LigneInfo(stringResource(R.string.cli_kpi_nb_ventes), (compte?.nbVentes ?: 0).toString())
                Button(onClick = onVoirCompte, couleur = ClientCouleurs.Nuit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cli_voir_compte))
                }
            }
        }
        ClientTab.CONTACTS -> {
            if (etat.contacts.isEmpty() && etat.adresses.isEmpty()) item { Vide(R.string.cli_aucun_contact) }
            items(etat.contacts.size) { index ->
                val contact = etat.contacts[index]
                CarteClient {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.nom, color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            listOfNotNull(contact.fonction, contact.telephone, contact.email)
                                .filter { it.isNotBlank() }
                                .forEach { Text(it, color = MissaMuted, fontSize = 14.sp) }
                            if (contact.principal) Text(stringResource(R.string.cli_contact_principal), color = MissaMuted, fontSize = 13.sp)
                        }
                        val tel = contact.telephone
                        if (!tel.isNullOrBlank()) {
                            IconButton(onClick = { onAppelerContact(tel) }, modifier = Modifier.size(48.dp)) {
                                Icon(painterResource(Iv.Call), stringResource(R.string.cli_appeler), tint = ClientCouleurs.Violet)
                            }
                        }
                    }
                }
            }
            items(etat.adresses.size) { index ->
                val adresse = etat.adresses[index]
                CarteClient {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(Iv.Place), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(24.dp))
                        Column {
                            if (adresse.libelle.isNotBlank()) Text(adresse.libelle, color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(listOfNotNull(adresse.adresse, adresse.ville).joinToString(", "), color = MissaMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        ClientTab.CONDITIONS -> {
            item {
                CarteClient {
                LigneInfo(stringResource(R.string.cli_cond_delai), stringResource(R.string.cli_jours, client.conditionPaiementJours))
                LigneInfo(
                    stringResource(R.string.clients_limite_credit),
                    client.limiteCredit?.let { clientMoney(it, etat.devise) } ?: stringResource(R.string.clients_flow_unlimited),
                )
                LigneInfo(stringResource(R.string.cli_cond_remise), "${client.remiseDefautPct} %")
                client.nif?.takeIf { it.isNotBlank() }?.let { LigneInfo(stringResource(R.string.cli_cond_nif), it) }
                client.commercial?.takeIf { it.isNotBlank() }?.let { LigneInfo(stringResource(R.string.cli_cond_commercial), it) }
                client.conditionsPaiement?.takeIf { it.isNotBlank() }?.let { LigneInfo(stringResource(R.string.cli_cond_conditions), it) }
                }
            }
            item { ClientPricesCard(etat.prix, produits, etat.devise, onPrixDefini, onPrixRetire) }
        }
        ClientTab.NOTES -> {
            item {
                CarteClient {
                    val notes = client.notes
                    if (notes.isNullOrBlank()) Vide(R.string.cli_aucune_note)
                    else Text(notes, color = MissaInk, fontSize = 15.sp)
                }
            }
        }
    }
}
