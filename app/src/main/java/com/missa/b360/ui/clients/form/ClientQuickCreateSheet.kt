package com.missa.b360.ui.clients.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.ClientPhoneField
import com.missa.b360.ui.components.MissaCarteSection
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaMuted

/** Route `clients/new` : deux champs (nom, téléphone), puis la fiche du client s'ouvre. */
@Composable
fun ClientQuickCreateSheet(
    onClose: () -> Unit,
    onCree: (Long) -> Unit,
    onOuvrirExistant: (Long) -> Unit,
    viewModel: ClientQuickCreateViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)
    LaunchedEffect(etat.creeId) {
        val id = etat.creeId
        if (id != null) {
            viewModel.creeConsomme()
            onCree(id)
        }
    }
    val piedCreer: @Composable ColumnScope.() -> Unit = {
        Button(
            onClick = { viewModel.enregistrer() },
            enabled = !etat.enCours,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.cli_creer_client)) }
        SnackbarHost(hote)
    }
    ClientSheet(titre = stringResource(R.string.clients_nouveau), onDismiss = onClose, pied = piedCreer) {
        MissaCarteSection(
            titre = stringResource(R.string.cli_section_identite),
            numero = 1,
            etiquette = stringResource(R.string.obn_section_a_completer)
                .takeIf { etat.nom.isBlank() || etat.telephoneLocal.isBlank() },
        ) {
            MissaChampTexte(
                valeur = etat.nom,
                onValeur = viewModel::changerNom,
                libelle = stringResource(R.string.clients_nom),
                icone = Iv.Person,
                requis = true,
                erreur = if (etat.erreurNom) stringResource(R.string.clients_nom_invalide) else null,
            )
            ClientPhoneField(
                codePays = etat.codePays,
                telephoneLocal = etat.telephoneLocal,
                onCodePays = viewModel::changerCodePays,
                onTelephone = viewModel::changerTelephone,
                enErreur = etat.erreurTelephone,
            )
        }
        Text(stringResource(R.string.cli_creation_rapide_aide), color = MissaMuted, fontSize = 13.sp)
        etat.detailErreur?.let { Text(it, color = MissaMuted, fontSize = 12.sp) }
    }
    val doublon = etat.doublon
    if (doublon != null) {
        AlertDialog(
            onDismissRequest = viewModel::fermerDoublon,
            title = { Text(stringResource(R.string.clients_doublon_titre)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.cli_doublon_existant, doublon.nom, doublon.code))
                }
            },
            confirmButton = {
                TextButton(onClick = { onOuvrirExistant(doublon.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cli_ouvrir_fiche))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.fermerDoublon()
                        viewModel.enregistrer(doublonConfirme = true)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.clients_creer_malgre_doublon)) }
            },
        )
    }
}
