package com.missa.b360.ui.clients.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.FollowupChannel
import com.missa.b360.core.domain.usecase.ClientFollowupUseCase
import com.missa.b360.ui.components.MissaChampTexte

/**
 * Relance en un geste : message modifiable, envoi par WhatsApp ou SMS, puis l'appelant enregistre
 * automatiquement la relance dans le journal de suivi.
 */
@Composable
internal fun ClientReminderDialog(
    telephone: String,
    messageInitial: String,
    onEnvoyer: (FollowupChannel, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var message by rememberSaveable { mutableStateOf(messageInitial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cli_relancer_titre)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MissaChampTexte(
                    valeur = message,
                    onValeur = { message = it.take(ClientFollowupUseCase.LONGUEUR_MESSAGE_MAX) },
                    libelle = stringResource(R.string.cli_message),
                    lignes = 5,
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            context.ouvrirWhatsApp(telephone, message)
                            onEnvoyer(FollowupChannel.WHATSAPP, message)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.cli_whatsapp)) }
                    OutlinedButton(
                        onClick = {
                            context.envoyerSms(telephone, message)
                            onEnvoyer(FollowupChannel.SMS, message)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.cli_sms)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cli_annuler))
            }
        },
    )
}
