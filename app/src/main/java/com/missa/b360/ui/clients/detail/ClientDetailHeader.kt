package com.missa.b360.ui.clients.detail

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.RiskLevel
import com.missa.b360.ui.clients.components.ClientCarte
import com.missa.b360.ui.clients.components.ClientHero
import com.missa.b360.ui.clients.components.ClientSymbole
import com.missa.b360.ui.clients.components.ClientAvatar
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientStatusChip
import com.missa.b360.ui.clients.components.CreditGauge
import com.missa.b360.ui.clients.components.RiskBadge
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.clientDate
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Héros bleu nuit : identité, statut, risque et utilisation du crédit d'un client (valeurs de `client_balances`). */
@Composable
internal fun ClientDetailHeader(etat: ClientDetailUiState) {
    val client = etat.client ?: return
    val evaluation = etat.evaluation
    val risque = evaluation?.risque ?: RiskLevel.NORMAL
    ClientHero {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ClientAvatar(client.nom, taille = 53.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(client.nom, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${client.code} · ${client.telephone}", color = Color(0xFFC3CBE0), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClientStatusChip(client.statut)
                    RiskBadge(risque)
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 12.dp))
        CreditGauge(evaluation?.utilisationPct, risque, surFonce = true)
    }
}

/** Grille 2 × 2 des indicateurs : encours, retard, chiffre d'affaires sur 12 mois, dernière vente. */
@Composable
internal fun ClientDetailMetriques(etat: ClientDetailUiState) {
    val compte = etat.balance
    val enRetard = (compte?.enRetard ?: 0.0) > 0.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Indicateur(
                stringResource(R.string.cli_kpi_encours), clientMoney(compte?.encours ?: 0.0, etat.devise),
                MissaInk, Modifier.weight(1f),
            )
            Indicateur(
                stringResource(R.string.cli_kpi_en_retard), clientMoney(compte?.enRetard ?: 0.0, etat.devise),
                if (enRetard) RisqueCouleurs.Eleve else MissaInk, Modifier.weight(1f),
                alerte = enRetard,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Indicateur(
                stringResource(R.string.cli_kpi_ca12), clientMoney(compte?.ca12Mois ?: 0.0, etat.devise),
                MissaInk, Modifier.weight(1f),
            )
            Indicateur(
                stringResource(R.string.cli_kpi_derniere_vente),
                compte?.derniereVenteAt?.let { clientDate(it) } ?: stringResource(R.string.cli_jamais),
                MissaInk, Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Indicateur(libelle: String, valeur: String, couleur: Color, modifier: Modifier, alerte: Boolean = false) {
    ClientCarte(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(libelle, color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (alerte) Icon(painterResource(Iv.Warning), contentDescription = null, tint = ClientCouleurs.Alerte, modifier = Modifier.size(13.dp))
            }
            Text(valeur, color = couleur, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Trois actions carrées (Vendre, Encaisser, Relancer) puis la bande de contact (Appeler, WhatsApp, SMS). */
@Composable
internal fun ClientDetailActions(
    onVendre: () -> Unit,
    onEncaisser: () -> Unit,
    onRelancer: () -> Unit,
    onAppeler: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionClient(Iv.ShoppingCart, R.string.cli_vendre, onVendre, Modifier.weight(1f))
            ActionClient(Iv.Payments, R.string.cli_encaisser, onEncaisser, Modifier.weight(1f))
            ActionClient(Iv.Send, R.string.cli_relancer, onRelancer, Modifier.weight(1f))
        }
        val forme = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(forme)
                .background(ClientCouleurs.Surface)
                .border(BorderStroke(1.dp, ClientCouleurs.Trait), forme),
        ) {
            ContactClient(Iv.Call, R.string.cli_appeler, onAppeler, Modifier.weight(1f))
            ContactClient(Iv.Chat, R.string.cli_whatsapp, onWhatsApp, Modifier.weight(1f))
            ContactClient(Iv.Sms, R.string.cli_sms, onSms, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionClient(icone: Int, libelle: Int, onClick: () -> Unit, modifier: Modifier) {
    ClientCarte(modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            ClientSymbole(icone, taille = 36.dp)
            Text(
                stringResource(libelle), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContactClient(icone: Int, libelle: Int, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icone), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(18.dp))
        Text(stringResource(libelle), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
