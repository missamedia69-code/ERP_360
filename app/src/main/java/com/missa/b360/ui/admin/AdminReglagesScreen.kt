package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite

/** Réglages (9.1 — D4/RA-19) : langue, profil, effectif, infos entreprise. */
@Composable
fun AdminReglagesScreen(
    onBack: () -> Unit,
    viewModel: ReglagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val langues = listOf(
        "fr" to R.string.langue_fr,
        "en" to R.string.langue_en,
        "es" to R.string.langue_es,
        "ar" to R.string.langue_ar,
        "zh" to R.string.langue_zh,
    )

    AdminScaffold(titreRes = R.string.admin_reglages, onBack = onBack) {
        Text(
            stringResource(R.string.adm_reglages_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        Titre(R.string.adm_langue)
        langues.forEach { (code, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = state.langue == code, onClick = { viewModel.changerLangue(code) })
                Text(stringResource(labelRes))
            }
        }

        Titre(R.string.adm_profil)
        ProfilActivite.entries.forEach { profil ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = state.profil == profil, onClick = { viewModel.changerProfil(profil) })
                Text(stringResource(profil.labelRes), style = MaterialTheme.typography.bodySmall)
            }
        }

        Titre(R.string.adm_palier)
        PalierTaille.entries.forEach { palier ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = state.palier == palier, onClick = { viewModel.changerPalier(palier) })
                Text(stringResource(palier.labelRes), style = MaterialTheme.typography.bodySmall)
            }
        }

        Titre(R.string.adm_infos_entreprise)
        Text(
            "${stringResource(R.string.home_title)} — ${state.nomEntreprise}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(
                "${stringResource(R.string.adm_devise_locked)} : ${state.devise}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        state.pays?.let {
            Text("${stringResource(R.string.ob_pays)} : $it", style = MaterialTheme.typography.bodyMedium)
        }

        Champ(state.secteur, viewModel::changerSecteur, R.string.adm_secteur)
        Champ(state.adresse, viewModel::changerAdresse, R.string.adm_adresse)
        Champ(state.telephone, viewModel::changerTelephone, R.string.adm_telephone)
        Champ(state.email, viewModel::changerEmail, R.string.adm_email)

        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::sauvegarderInfos, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.adm_sauvegarder))
        }
        state.sauvegardeMsg?.let {
            Text(
                stringResource(if (it == "ok") R.string.adm_save_ok else R.string.adm_save_err),
                color = if (it == "ok") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Titre(res: Int) {
    Spacer(Modifier.height(20.dp))
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Champ(value: String, onValueChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}