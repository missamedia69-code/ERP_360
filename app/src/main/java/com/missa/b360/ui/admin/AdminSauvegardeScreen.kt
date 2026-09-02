package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.util.DateUtils
import java.io.File

/** Sauvegarde (RA-13) : sauvegarde locale (VACUUM INTO) + historique. */
@Composable
fun AdminSauvegardeScreen(
    onBack: () -> Unit,
    viewModel: SauvegardeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val historique by viewModel.historique.collectAsState(initial = emptyList())

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

        Spacer(Modifier.height(20.dp))
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
    ListItem(
        headlineContent = { Text("${DateUtils.formatDateHeure(backup.date)} — ${backup.type}") },
        supportingContent = { Text("${stringResource(R.string.adm_sauvegarde_type)} · $nom") },
    )
}