package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.util.DateUtils

/** Journal (RA-18) : consultation immuable + purge des entrées > 12 mois. */
@Composable
fun AdminJournalScreen(
    onBack: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())

    AdminScaffold(titreRes = R.string.admin_journal, onBack = onBack) {
        OutlinedButton(
            onClick = viewModel::purger,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.adm_journal_purge))
        }
        if (viewModel.purgeOk) {
            Text(
                stringResource(R.string.adm_journal_purge_ok),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.adm_journal_vide),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Spacer(Modifier.height(12.dp))
            entries.forEach { entry ->
                JournalRow(entry)
            }
        }
    }
}

@Composable
private fun JournalRow(entry: JournalEntryEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                "${entry.module} · ${entry.action}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                entry.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                DateUtils.formatDateHeure(entry.horodatage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}