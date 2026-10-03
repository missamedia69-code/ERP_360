package com.missa.b360.ui.clients.account

import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Brush
import com.missa.b360.ui.clients.components.ClientCarte
import com.missa.b360.ui.clients.components.BoutonClient
import com.missa.b360.ui.clients.components.ClientTitreSection
import com.missa.b360.ui.icons.Iv
import androidx.compose.material3.CardDefaults
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.AgedBalance
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.usecase.ClientOpenInvoiceLine
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.clientDate
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Balance âgée en barres horizontales (jamais un tableau défilant) : une ligne par tranche, barre violette. */
@Composable
internal fun AgedBalanceBars(balance: AgedBalance, devise: String) {
    val maximum = AgingBucket.entries.maxOf { balance.montant(it) }.takeIf { it > 0.0 } ?: 1.0
    ClientCarte {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClientTitreSection(Iv.Schedule, stringResource(R.string.cli_balance_agee)) {
                Text(clientMoney(balance.total, devise), color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            for (tranche in AgingBucket.entries) {
                val montant = balance.montant(tranche)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ClientCouleurs.Surface)
                        .border(BorderStroke(1.dp, Color(0xFFEFF1F5)), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row {
                        Text(stringResource(tranche.libelleReleve()), color = MissaMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(clientMoney(montant, devise), color = MissaInk, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE5E9F1))) {
                        if (montant > 0.0) {
                            Box(
                                Modifier.fillMaxWidth((montant / maximum).toFloat().coerceIn(0.03f, 1f)).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(ClientCouleurs.Violet),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Facture ouverte : référence, dates, reste à payer, retard en jours et encaissement ciblé. */
@Composable
internal fun OpenInvoiceRow(facture: ClientOpenInvoiceLine, devise: String, onEncaisser: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ClientCouleurs.Carte),
        border = BorderStroke(1.dp, ClientCouleurs.CarteBord),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        facture.reference.ifBlank { clientDate(facture.issuedAt) },
                        color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.cli_echeance_le, clientDate(facture.dueAt)),
                        color = if (facture.joursRetard > 0) RisqueCouleurs.Eleve else MissaMuted, fontSize = 10.sp,
                    )
                    if (facture.joursRetard > 0) {
                        Text(stringResource(R.string.cli_retard_jours, facture.joursRetard), color = RisqueCouleurs.Eleve, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(clientMoney(facture.outstanding, devise), color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(stringResource(R.string.cli_sur_total, clientMoney(facture.total, devise)), color = MissaMuted, fontSize = 10.sp)
                }
            }
            if (facture.recordId != null) {
                BoutonClient(onClick = onEncaisser, modifier = Modifier.fillMaxWidth(), plein = false) {
                    Text(stringResource(R.string.cli_encaisser_facture), color = MissaInk)
                }
            }
        }
    }
}
