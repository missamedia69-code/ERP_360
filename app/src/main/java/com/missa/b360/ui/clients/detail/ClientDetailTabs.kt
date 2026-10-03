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
import com.missa.b360.ui.clients.components.ClientEtatVide
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
    onModifier: () -> Unit = {},
) {
    item {
        val (icone, titre) = when (etat.onglet) {
            ClientTab.ACTIVITE -> Iv.History to R.string.cli_onglet_activite
            ClientTab.COMPTE -> Iv.Payments to R.string.cli_onglet_compte
            ClientTab.CONTACTS -> Iv.Group to R.string.cli_onglet_contacts
            ClientTab.CONDITIONS -> Iv.Description to R.string.cli_onglet_conditions
            ClientTab.NOTES -> Iv.Edit to R.string.cli_onglet_notes
        }
        ClientTitreSection(icone, stringResource(titre)) {
            when (etat.onglet) {
                ClientTab.CONDITIONS -> IconButton(onClick = onModifier, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(Iv.Edit), stringResource(R.string.clients_modifier), tint = ClientCouleurs.Violet, modifier = Modifier.size(17.dp))
                }
                ClientTab.NOTES -> BoutonClient(onClick = onNote) {
                    Icon(painterResource(Iv.Add), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.cli_ajouter_note_court), color = Color.White)
                }
                else -> Unit
            }
        }
    }
    when (etat.onglet) {
        ClientTab.ACTIVITE -> {
            if (etat.suivis.isEmpty()) {
                item { ClientVideActivite() }
            } else {
                items(etat.suivis.take(APERCU_ACTIVITE).size) { index ->
                    ClientFollowupRow(etat.suivis[index], etat.devise)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    BoutonClient(onClick = onNote, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(Iv.Add), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.cli_ajouter_note), color = Color.White)
                    }
                    BoutonClient(onClick = onVoirActivite, modifier = Modifier.fillMaxWidth(), plein = false) {
                        Text(stringResource(R.string.cli_voir_activite), color = MissaInk)
                        Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(15.dp))
                    }
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
                    Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
        ClientTab.CONTACTS -> {
            if (etat.contacts.isEmpty() && etat.adresses.isEmpty()) {
                item { ClientEtatVide(Iv.Group, stringResource(R.string.cli_aucun_contact), "") }
            }
            items(etat.contacts.size) { index ->
                val contact = etat.contacts[index]
                CarteClient {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.nom, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            listOfNotNull(contact.fonction, contact.telephone, contact.email)
                                .filter { it.isNotBlank() }
                                .forEach { Text(it, color = MissaMuted, fontSize = 10.sp) }
                            if (contact.principal) Text(stringResource(R.string.cli_contact_principal), color = ClientCouleurs.VioletProfond, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            if (adresse.libelle.isNotBlank()) Text(adresse.libelle, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            Text(listOfNotNull(adresse.adresse, adresse.ville).joinToString(", "), color = MissaMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    BoutonClient(onClick = onModifier, modifier = Modifier.weight(1f), plein = false) {
                        Icon(painterResource(Iv.Add), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(15.dp))
                        Text(stringResource(R.string.cli_ajouter_contact), color = MissaInk, maxLines = 1)
                    }
                    BoutonClient(onClick = onModifier, modifier = Modifier.weight(1f), plein = false) {
                        Icon(painterResource(Iv.Add), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(15.dp))
                        Text(stringResource(R.string.cli_ajouter_adresse), color = MissaInk, maxLines = 1)
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
                val notes = client.notes
                if (notes.isNullOrBlank()) {
                    ClientEtatVide(Iv.Description, stringResource(R.string.cli_aucune_note), stringResource(R.string.cli_form_note_notes))
                } else {
                    CarteClient { Text(notes, color = MissaInk, fontSize = 12.sp) }
                }
            }
        }
    }
}
