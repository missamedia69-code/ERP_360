package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.LicenceStatus
import com.missa.b360.core.util.DateUtils

/** Licence (9.1 — RA-04..06) : statut, activation, désassociation (3/an). */
@Composable
fun AdminLicenceScreen(
    onBack: () -> Unit,
    viewModel: LicenceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AdminScaffold(titreRes = R.string.admin_licence, onBack = onBack) {
        val statutRes = when (state.statut) {
            LicenceStatus.TRIAL -> R.string.adm_licence_statut_essai
            LicenceStatus.ACTIVE -> R.string.adm_licence_statut_actif
            LicenceStatus.EXPIRED -> R.string.adm_licence_statut_expire
            null -> null
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.adm_licence_statut),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                statutRes?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (state.statut) {
                            LicenceStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            LicenceStatus.EXPIRED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                state.dateDebutEssai?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stringResource(R.string.adm_licence_date_debut)} : ${DateUtils.formatDate(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.dateExpiration?.let {
                    Text(
                        "${stringResource(R.string.adm_licence_date_expiration)} : ${DateUtils.formatDate(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.code?.let {
                    Text(
                        "${stringResource(R.string.adm_licence_code_actif)} : $it",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.appareilId?.let {
                    Text(
                        "${stringResource(R.string.adm_licence_appareil)} : $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        R.string.adm_licence_desassoc_count,
                        state.desassociationsUtilisees,
                        state.desassociationsMax,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = viewModel::desassocierAppareil,
                    enabled = state.desassociationsUtilisees < state.desassociationsMax,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.adm_licence_desassoc))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()

        Text(
            stringResource(R.string.adm_licence_code_field),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        OutlinedTextField(
            value = state.codeSaisi,
            onValueChange = viewModel::changerCode,
            label = { Text(stringResource(R.string.adm_licence_code_field)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Button(
            onClick = viewModel::activerCode,
            enabled = state.codeSaisi.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.adm_licence_activer))
        }

        when (state.message) {
            "ok" -> Message(stringResource(R.string.adm_licence_msg_ok), isError = false)
            "err" -> Message(stringResource(R.string.adm_licence_msg_err), isError = true)
            "max" -> Message(stringResource(R.string.adm_licence_msg_max), isError = true)
            "none" -> Message(stringResource(R.string.adm_licence_msg_none), isError = true)
        }
    }
}

@Composable
internal fun Message(texte: String, isError: Boolean) {
    Text(
        texte,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}