package com.missa.b360.ui.clients.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
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
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import com.missa.b360.ui.components.MissaCarteSection
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
    val pret = !etat.erreur && !etat.chargement && !etat.introuvable
    val aCompleter = stringResource(R.string.obn_section_a_completer)
    val invalide = stringResource(R.string.form_valeur_invalide)
    val piedEnregistrer: @Composable ColumnScope.() -> Unit = {
        Button(
            onClick = viewModel::enregistrer,
            enabled = !etat.enCours,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.cli_enregistrer)) }
        SnackbarHost(hote)
    }
    ClientSheet(
        titre = stringResource(R.string.clients_modifier),
        onDismiss = {
            viewModel.abandonner()
            onClose()
        },
        pied = if (pret) piedEnregistrer else null,
    ) {
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::charger, modifier = Modifier.heightIn(max = 240.dp))
            etat.chargement -> EtatChargement(Modifier.heightIn(max = 160.dp))
            etat.introuvable -> Text(stringResource(R.string.cli_fiche_introuvable))
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Même grammaire que « Informations sur votre entreprise » : cartes numérotées, toujours ouvertes.
                ClientSection.entries.forEachIndexed { index, section ->
                    val enErreur = etat.erreurs.any { it.section == section }
                    MissaCarteSection(
                        titre = stringResource(section.titre()),
                        numero = index + 1,
                        etiquette = when {
                            enErreur -> invalide
                            section.essentielManque(etat.draft) -> aCompleter
                            else -> null
                        },
                        etiquetteEnErreur = enErreur,
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
            }
        }
    }
}

/** Pastille « À compléter » : l'essentiel de la section manque (jamais bloquant). */
private fun ClientSection.essentielManque(d: ClientDraft): Boolean = when (this) {
    ClientSection.IDENTITE -> d.nom.isBlank() || (d.telephoneLocal.isBlank() && d.email.isBlank())
    ClientSection.FISCALITE -> {
        val type = ClientType.entries.firstOrNull { it.name == d.type } ?: ClientType.PARTICULIER
        ClientLifecycleRules.informationsFiscalesRequises(type) && d.nif.isBlank()
    }
    else -> false
}

private fun ClientSection.titre(): Int = when (this) {
    ClientSection.IDENTITE -> R.string.cli_section_identite
    ClientSection.FISCALITE -> R.string.cli_section_fiscalite
    ClientSection.CONDITIONS -> R.string.cli_section_conditions
    ClientSection.CONTACTS -> R.string.cli_onglet_contacts
    ClientSection.NOTES -> R.string.cli_onglet_notes
}
