package com.missa.b360.ui.clients.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.missa.b360.ui.clients.components.ClientAvatar
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

/** Identité, statut, risque, jauge de crédit et indicateurs d'un client (valeurs de `client_balances`). */
@Composable
internal fun ClientDetailHeader(etat: ClientDetailUiState) {
    val client = etat.client ?: return
    val evaluation = etat.evaluation
    val compte = etat.balance
    val risque = evaluation?.risque ?: RiskLevel.NORMAL
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ClientAvatar(client.nom, taille = 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(client.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${client.code} · ${client.telephone}", color = MissaMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ClientStatusChip(client.statut)
                        RiskBadge(risque)
                    }
                }
            }
            CreditGauge(evaluation?.utilisationPct, risque)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Indicateur(
                    stringResource(R.string.cli_kpi_encours), clientMoney(compte?.encours ?: 0.0, etat.devise),
                    MissaInk, Modifier.weight(1f),
                )
                Indicateur(
                    stringResource(R.string.cli_kpi_en_retard), clientMoney(compte?.enRetard ?: 0.0, etat.devise),
                    if ((compte?.enRetard ?: 0.0) > 0.0) RisqueCouleurs.Eleve else MissaInk, Modifier.weight(1f),
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
}

@Composable
private fun Indicateur(libelle: String, valeur: String, couleur: Color, modifier: Modifier) {
    Column(modifier) {
        Text(libelle, color = MissaMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valeur, color = couleur, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Six actions à un geste : cibles de 56 dp minimum, icône et libellé toujours ensemble. */
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionClient(Iv.Call, R.string.cli_appeler, onAppeler, Modifier.weight(1f))
            ActionClient(Iv.Chat, R.string.cli_whatsapp, onWhatsApp, Modifier.weight(1f))
            ActionClient(Iv.Smartphone, R.string.cli_sms, onSms, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionClient(icone: Int, libelle: Int, onClick: () -> Unit, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = modifier.heightIn(min = 64.dp).clickable(onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Icon(painterResource(icone), contentDescription = null, tint = MissaInk, modifier = Modifier.size(24.dp))
            Text(
                stringResource(libelle), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
