package com.missa.b360.ui.clients.components

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.FollowupChannel
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

internal fun FollowupType.libelle(): Int = when (this) {
    FollowupType.RELANCE -> R.string.cli_type_relance
    FollowupType.APPEL -> R.string.cli_type_appel
    FollowupType.PROMESSE -> R.string.cli_type_promesse
    FollowupType.NOTE -> R.string.cli_type_note
}

internal fun FollowupType.icone(): Int = when (this) {
    FollowupType.RELANCE -> Iv.Send
    FollowupType.APPEL -> Iv.Call
    FollowupType.PROMESSE -> Iv.Handshake
    FollowupType.NOTE -> Iv.Description
}

internal fun FollowupChannel.libelle(): Int = when (this) {
    FollowupChannel.WHATSAPP -> R.string.cli_whatsapp
    FollowupChannel.SMS -> R.string.cli_sms
    FollowupChannel.APPEL -> R.string.cli_type_appel
    FollowupChannel.EMAIL -> R.string.cli_canal_email
    FollowupChannel.VISITE -> R.string.cli_canal_visite
    FollowupChannel.AUTRE -> R.string.cli_canal_autre
}

internal fun FollowupStatus.libelle(): Int = when (this) {
    FollowupStatus.OUVERT -> R.string.cli_suivi_ouvert
    FollowupStatus.TENU -> R.string.cli_suivi_tenu
    FollowupStatus.NON_TENU -> R.string.cli_suivi_non_tenu
    FollowupStatus.CLOS -> R.string.cli_suivi_clos
}

internal fun FollowupStatus.couleur(): Color = when (this) {
    FollowupStatus.OUVERT -> RisqueCouleurs.Attention
    FollowupStatus.TENU -> RisqueCouleurs.Normal
    FollowupStatus.NON_TENU -> RisqueCouleurs.Eleve
    FollowupStatus.CLOS -> MissaMuted
}

/** Une entrée du journal de suivi : type, canal, date, message et, pour une promesse, montant et état. */
@Composable
internal fun ClientFollowupRow(suivi: ClientFollowupEntity, devise: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ClientCouleurs.Carte),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(suivi.type.icone()), contentDescription = null, tint = MissaInk, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val canal = suivi.canal?.let { " · " + stringResource(it.libelle()) }.orEmpty()
                Text(
                    stringResource(suivi.type.libelle()) + canal,
                    color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                )
                Text(clientDate(suivi.createdAt), color = MissaMuted, fontSize = 13.sp)
                val date = suivi.promesseDate
                val montant = suivi.promesseMontant
                if (suivi.type == FollowupType.PROMESSE && date != null && montant != null) {
                    Text(
                        stringResource(R.string.cli_promesse_ligne, clientMoney(montant, devise), clientDate(date)),
                        color = MissaInk, fontSize = 14.sp,
                    )
                }
                suivi.message?.takeIf { it.isNotBlank() }?.let { Text(it, color = MissaInk, fontSize = 14.sp) }
            }
            if (suivi.type == FollowupType.PROMESSE) {
                Text(
                    stringResource(suivi.statut.libelle()),
                    color = suivi.statut.couleur(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                )
            }
        }
    }
}
