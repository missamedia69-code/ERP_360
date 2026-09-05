package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import java.util.TimeZone

/**
 * Écran — Configuration initiale : langue, fuseau horaire, format de date,
 * style des nombres et sauvegardes automatiques. Chaque choix est appliqué
 * immédiatement à toute l'application (FormatPrefs / locale) puis conservé.
 */
@Composable
internal fun OnbConfigurationStep(viewModel: OnboardingViewModel) {
    OnbScaffold(
        titreRes = R.string.obn_config_titre,
        sousTitreRes = R.string.obn_config_sous,
        viewModel = viewModel,
        onRetour = viewModel::precedent,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MissaBorder),
            colors = CardDefaults.cardColors(containerColor = MissaSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val langues = listOf(
                    "fr" to stringResource(R.string.langue_fr),
                    "en" to stringResource(R.string.langue_en),
                    "es" to stringResource(R.string.langue_es),
                    "ar" to stringResource(R.string.langue_ar),
                    "zh" to stringResource(R.string.langue_zh),
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_langue,
                    options = langues,
                    selectedKey = viewModel.langue,
                    optionKey = { it.first },
                    onPick = { viewModel.appliquerLangue(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val fuseaux = OnbZones()
                OnbConfigLigne(
                    labelRes = R.string.obn_fuseau,
                    options = fuseaux,
                    selectedKey = viewModel.fuseau,
                    optionKey = { it },
                    optionLabel = { OnbZoneLibelle(it) },
                    onPick = { viewModel.appliquerFuseau(it) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val formatsJours = listOf(
                    "dd/MM/yyyy" to "31/12/2025",
                    "MM/dd/yyyy" to "12/31/2025",
                    "dd.MM.yyyy" to "31.12.2025",
                    "yyyy-MM-dd" to "2025-12-31",
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_format_date,
                    options = formatsJours,
                    selectedKey = viewModel.formatJours,
                    optionKey = { it.first },
                    optionLabel = { it.second },
                    onPick = { viewModel.appliquerFormatDate(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val formatsNombres = listOf(
                    "fr" to "1 234 567,89",
                    "en" to "1,234,567.89",
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_format_nombres,
                    options = formatsNombres,
                    selectedKey = viewModel.formatNombres,
                    optionKey = { it.first },
                    optionLabel = { it.second },
                    onPick = { viewModel.appliquerFormatNombres(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.obn_sauvegardes),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.obn_sauvegardes_sous),
                            fontSize = 11.5.sp,
                            color = MissaMuted,
                        )
                    }
                    Switch(
                        checked = viewModel.sauvegardesActives,
                        onCheckedChange = { viewModel.appliquerSauvegardes(it) },
                        enabled = !viewModel.enregistrementEnCours,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedTrackColor = BrandBlue,
                        ),
                    )
                }
            }
        }
    }
}

/** Zone horaire proposée : identifiant IANA + libellé « UTC +1 (Ville) ». */
private fun OnbZones(): List<String> {
    val parDefaut = TimeZone.getDefault().id
    val fixes = listOf(
        "UTC",
        "Africa/Douala",
        "Africa/Abidjan",
        "Africa/Casablanca",
        "Africa/Nairobi",
        "Europe/Paris",
        "Asia/Dubai",
        "Asia/Shanghai",
        "America/New_York",
    )
    return (listOf(parDefaut) + fixes).distinct()
}

private fun OnbZoneLibelle(id: String): String {
    val ville = id.substringAfter('/', missingDelimiterValue = "UTC")
    val heures = TimeZone.getTimeZone(id).rawOffset / 3_600_000
    val minutes = Math.abs(TimeZone.getTimeZone(id).rawOffset % 3_600_000) / 60_000
    val decalage = if (minutes == 0) {
        if (heures >= 0) "+$heures" else "$heures"
    } else {
        if (heures >= 0) "+$heures:${minutes.toString().padStart(2, '0')}"
        else "$heures:${minutes.toString().padStart(2, '0')}"
    }
    return "UTC $decalage ($ville)"
}

/**
 * Ligne de réglage de la maquette : libellé au-dessus de la valeur courante,
 * chevron de droite et menu déroulant ancré sur la ligne (DropdownMenu
 * standard, ancré sur le conteneur de la ligne).
 */
@Composable
private fun <T> OnbConfigLigne(
    labelRes: Int,
    options: List<T>,
    selectedKey: String,
    optionKey: (T) -> String,
    optionLabel: ((T) -> String)? = null,
    onPick: (T) -> Unit,
    enabled: Boolean,
) {
    var ouvert by remember { mutableStateOf(false) }
    val choisie = options.firstOrNull { optionKey(it) == selectedKey }
    val libelle = if (choisie != null) (optionLabel?.invoke(choisie) ?: optionKey(choisie)) else selectedKey
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp)
                .then(if (enabled) Modifier.clickable { ouvert = !ouvert } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(labelRes),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = libelle,
                    fontSize = 13.5.sp,
                    color = MissaMuted,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = ouvert,
            onDismissRequest = { ouvert = false },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            (optionLabel?.invoke(option) ?: optionKey(option)),
                            fontSize = 13.sp,
                        )
                    },
                    onClick = {
                        onPick(option)
                        ouvert = false
                    },
                )
            }
        }
    }
}
