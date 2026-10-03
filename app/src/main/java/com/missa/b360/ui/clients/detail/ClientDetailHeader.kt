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

/** Identité, statut, risque, utilisation du crédit et indicateurs d'un client (valeurs de `client_balances`). */
@Composable
internal fun ClientDetailHeader(etat: ClientDetailUiState) {
    val client = etat.client ?: return
    val evaluation = etat.evaluation
    val compte = etat.balance
    val risque = evaluation?.risque ?: RiskLevel.NORMAL
    val forme = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forme)
            .background(ClientCouleurs.Carte)
            .border(BorderStroke(1.dp, ClientCouleurs.CarteBord), forme)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ClientAvatar(client.nom, taille = 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(client.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${client.code} · ${client.telephone}", color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClientStatusChip(client.statut)
                    RiskBadge(risque)
                }
            }
        }
        HorizontalDivider(color = ClientCouleurs.Trait)
        CreditGauge(evaluation?.utilisationPct, risque)
        HorizontalDivider(color = ClientCouleurs.Trait)
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

@Composable
private fun Indicateur(libelle: String, valeur: String, couleur: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(libelle, color = MissaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valeur, color = couleur, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Six actions à un geste : « Vendre » en bleu nuit (action principale), les autres en tuiles grises ; icône et libellé ensemble. */
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
            ActionClient(Iv.ShoppingCart, R.string.cli_vendre, onVendre, Modifier.weight(1f), principale = true)
            ActionClient(Iv.Payments, R.string.cli_encaisser, onEncaisser, Modifier.weight(1f))
            ActionClient(Iv.Send, R.string.cli_relancer, onRelancer, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionClient(Iv.Call, R.string.cli_appeler, onAppeler, Modifier.weight(1f), claire = true)
            ActionClient(Iv.Chat, R.string.cli_whatsapp, onWhatsApp, Modifier.weight(1f), claire = true)
            ActionClient(Iv.Sms, R.string.cli_sms, onSms, Modifier.weight(1f), claire = true)
        }
    }
}

@Composable
private fun ActionClient(
    icone: Int,
    libelle: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    principale: Boolean = false,
    claire: Boolean = false,
) {
    val forme = RoundedCornerShape(14.dp)
    val fond = when {
        principale -> ClientCouleurs.Nuit
        claire -> ClientCouleurs.TuileClaire
        else -> ClientCouleurs.Tuile
    }
    val contenu = if (principale) Color.White else MissaInk
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(forme)
            .background(fond)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(icone), contentDescription = null, tint = contenu, modifier = Modifier.size(20.dp))
        Text(
            stringResource(libelle), color = contenu, fontSize = 12.sp,
            fontWeight = if (claire) FontWeight.Medium else FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
