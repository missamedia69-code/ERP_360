package com.missa.b360.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaPanel
import java.io.File
import kotlinx.coroutines.delay

/**
 * Sauvegarde (RA-13) : sauvegarde locale (VACUUM INTO), restauration d'un fichier
 * `.db` existant et historique des sauvegardes.
 */
@Composable
fun AdminSauvegardeScreen(
    onBack: () -> Unit,
    viewModel: SauvegardeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val historique by viewModel.historique.collectAsState(initial = emptyList())
    val contexte = LocalContext.current
    var fichierChoisi by remember { mutableStateOf<Uri?>(null) }
    val selecteur = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) fichierChoisi = uri }

    // La base restaurée n'est ouverte qu'au prochain démarrage du processus.
    LaunchedEffect(state.restaurationReussie) {
        if (state.restaurationReussie) {
            delay(1_200)
            redemarrerApplication(contexte)
        }
    }

    fichierChoisi?.let { uri ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { fichierChoisi = null },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        fichierChoisi = null
                        viewModel.restaurer(uri)
                    },
                ) { Text(stringResource(R.string.obn_restaurer_confirmer)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { fichierChoisi = null }) {
                    Text(stringResource(R.string.ops_cancel))
                }
            },
            title = { Text(stringResource(R.string.obn_restaurer_confirme_titre)) },
            text = { Text(stringResource(R.string.obn_restaurer_confirme_texte)) },
        )
    }

    AdminScaffold(titreRes = R.string.admin_sauvegarde, onBack = onBack) {
        Button(
            onClick = viewModel::sauvegarder,
            enabled = !state.enCours,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.enCours) R.string.adm_sauvegarde_en_cours else R.string.adm_sauvegarde_btn,
                ),
            )
        }
        when (state.message) {
            "ok" -> Message(stringResource(R.string.adm_sauvegarde_ok), isError = false)
            "err" -> Message(stringResource(R.string.adm_sauvegarde_err), isError = true)
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { selecteur.launch(arrayOf("application/octet-stream", "*/*")) },
            enabled = !state.restaurationEnCours && !state.restaurationReussie && !state.enCours,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.restaurationEnCours) {
                        R.string.obn_restaurer_en_cours
                    } else {
                        R.string.obn_restaurer_btn
                    },
                ),
            )
        }
        state.restaurationMessageRes?.let { messageRes ->
            Message(stringResource(messageRes), isError = !state.restaurationReussie)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.adm_sauvegarde_historique),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (historique.isEmpty()) {
            Text(
                stringResource(R.string.adm_sauvegarde_vide),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            historique.forEach { backup ->
                BackupRow(backup)
            }
        }
    }
}

@Composable
private fun BackupRow(backup: BackupEntity) {
    val nom = File(backup.chemin).name
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${DateUtils.formatDateHeure(backup.date)} — ${backup.type}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "${stringResource(R.string.adm_sauvegarde_type)} · $nom",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Relance l'application après une restauration (nouvelle base de données). */
private fun redemarrerApplication(contexte: Context) {
    val intention = contexte.packageManager.getLaunchIntentForPackage(contexte.packageName)
    if (intention != null) {
        intention.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        contexte.startActivity(intention)
    }
    Runtime.getRuntime().exit(0)
}
