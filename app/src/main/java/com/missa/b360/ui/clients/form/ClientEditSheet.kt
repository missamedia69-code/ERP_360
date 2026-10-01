package com.missa.b360.ui.clients.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur

/** Route `clients/{id}/edit` : sections pliables, le brouillon survit à la rotation et au redémarrage du processus. */
@Composable
fun ClientEditSheet(
    onClose: () -> Unit,
    onSauve: () -> Unit,
    viewModel: ClientEditViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)
    LaunchedEffect(etat.sauve) { if (etat.sauve) onSauve() }
    ClientSheet(
        titre = stringResource(R.string.clients_modifier),
        onDismiss = {
            viewModel.abandonner()
            onClose()
        },
    ) {
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::charger, modifier = Modifier.heightIn(max = 240.dp))
            etat.chargement -> EtatChargement(Modifier.heightIn(max = 160.dp))
            etat.introuvable -> Text(stringResource(R.string.cli_fiche_introuvable))
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (section in ClientSection.entries) {
                    SectionPliable(
                        titre = stringResource(section.titre()),
                        ouverte = section in etat.ouvertes,
                        enErreur = etat.erreurs.any { it.section == section },
                        onBascule = { viewModel.basculerSection(section) },
                    ) {
                        when (section) {
                            ClientSection.IDENTITE -> SectionIdentite(etat, viewModel::modifier)
                            ClientSection.FISCALITE -> SectionFiscalite(etat, viewModel::modifier)
                            ClientSection.CONDITIONS -> SectionConditions(etat, viewModel::modifier)
                            ClientSection.CONTACTS -> SectionContacts(etat, viewModel::modifier)
                            ClientSection.NOTES -> SectionNotes(etat, viewModel::modifier)
                        }
                    }
                }
                Button(
                    onClick = viewModel::enregistrer,
                    enabled = !etat.enCours,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 4.dp),
                ) { Text(stringResource(R.string.cli_enregistrer)) }
                SnackbarHost(hote)
            }
        }
    }
}

private fun ClientSection.titre(): Int = when (this) {
    ClientSection.IDENTITE -> R.string.cli_section_identite
    ClientSection.FISCALITE -> R.string.cli_section_fiscalite
    ClientSection.CONDITIONS -> R.string.cli_section_conditions
    ClientSection.CONTACTS -> R.string.cli_onglet_contacts
    ClientSection.NOTES -> R.string.cli_onglet_notes
}
