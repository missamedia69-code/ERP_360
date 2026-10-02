package com.missa.b360.ui.clients.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.model.ClientAdvancedFilter
import com.missa.b360.ui.clients.components.libelle
import com.missa.b360.ui.clients.labelRes
import com.missa.b360.ui.theme.MissaInk

/** Filtres avancés en feuille modale : statut, type, avec encours, en retard. Appliqués en direct. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ClientFilterSheet(
    filtre: ClientAdvancedFilter,
    onChange: (ClientAdvancedFilter) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color.White) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.cli_filtres_titre), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(stringResource(R.string.cli_filtre_statut), color = MissaInk, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (statut in ClientStatus.entries) {
                    FilterChip(
                        selected = statut in filtre.statuts,
                        onClick = {
                            val suite = if (statut in filtre.statuts) filtre.statuts - statut else filtre.statuts + statut
                            onChange(filtre.copy(statuts = suite))
                        },
                        label = { Text(stringResource(statut.libelle())) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            Text(stringResource(R.string.cli_filtre_type), color = MissaInk, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (type in ClientType.entries) {
                    FilterChip(
                        selected = type in filtre.types,
                        onClick = {
                            val suite = if (type in filtre.types) filtre.types - type else filtre.types + type
                            onChange(filtre.copy(types = suite))
                        },
                        label = { Text(stringResource(type.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            InterrupteurLigne(stringResource(R.string.cli_filtre_avec_encours), filtre.avecEncours) { onChange(filtre.copy(avecEncours = it)) }
            InterrupteurLigne(stringResource(R.string.cli_filtre_en_retard), filtre.enRetard) { onChange(filtre.copy(enRetard = it)) }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onChange(ClientAdvancedFilter()) },
                    enabled = filtre.actifs > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.cli_filtre_reinitialiser)) }
                Button(onClick = onClose, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cli_filtre_appliquer))
                }
            }
        }
    }
}

@Composable
private fun InterrupteurLigne(libelle: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, color = MissaInk, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = valeur, onCheckedChange = onChange)
    }
}
