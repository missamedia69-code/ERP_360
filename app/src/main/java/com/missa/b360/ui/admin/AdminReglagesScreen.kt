package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle

/** Réglages structurés en panneaux courts : langue, profil, taille puis coordonnées. */
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(title = stringResource(R.string.adm_langue))
            langues.forEach { (code, labelRes) ->
                SettingsChoice(
                    selected = state.langue == code,
                    onSelect = { viewModel.changerLangue(code) },
                    label = stringResource(labelRes),
                )
            }
        }

        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(title = stringResource(R.string.adm_profil))
            ProfilActivite.entries.forEach { profil ->
                SettingsChoice(
                    selected = state.profil == profil,
                    onSelect = { viewModel.changerProfil(profil) },
                    label = stringResource(profil.labelRes),
                )
            }
        }

        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(title = stringResource(R.string.adm_palier))
            PalierTaille.entries.forEach { palier ->
                SettingsChoice(
                    selected = state.palier == palier,
                    onSelect = { viewModel.changerPalier(palier) },
                    label = stringResource(palier.labelRes),
                )
            }
        }

        MissaPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
            MissaSectionTitle(
                title = stringResource(R.string.adm_infos_entreprise),
                subtitle = "${stringResource(R.string.home_title)} — ${state.nomEntreprise}",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(
                    "${stringResource(R.string.adm_devise_locked)} : ${state.devise}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.pays?.let {
                Text("${stringResource(R.string.ob_pays)} : $it", style = MaterialTheme.typography.bodySmall)
            }
            SettingField(state.secteur, viewModel::changerSecteur, R.string.adm_secteur)
            SettingField(state.adresse, viewModel::changerAdresse, R.string.adm_adresse)
            SettingField(state.telephone, viewModel::changerTelephone, R.string.adm_telephone)
            SettingField(state.email, viewModel::changerEmail, R.string.adm_email)
            Button(onClick = viewModel::sauvegarderInfos, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.adm_sauvegarder), fontWeight = FontWeight.Bold)
            }
            state.sauvegardeMsg?.let {
                Text(
                    stringResource(if (it == "ok") R.string.adm_save_ok else R.string.adm_save_err),
                    color = if (it == "ok") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsChoice(selected: Boolean, onSelect: () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingField(value: String, onValueChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
