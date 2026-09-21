package com.missa.b360.ui.qualite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.data.entity.StatutNc
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pourpre caractéristique du module Qualité — source unique : [AppModule.QUALITE]. */
private val PourpreQualite: Color get() = AppModule.QUALITE.couleur

@Composable
fun QualiteScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: QualiteViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()

    var dialogueNouvelleNc by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouvelleNc = false
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_qualite),
            onBack = onBack,
            couleurFond = AppModule.QUALITE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Carte Synthèse Qualité ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PourpreQualite.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.qua_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.qua_ecarts_ouverts, etat.bilan.ouvertes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.qua_critiques, etat.bilan.critiques),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (etat.bilan.critiques > 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                            )
                            Text(
                                stringResource(R.string.qua_cout_ecarts, fmtValeur(etat.bilan.coutTotal, devise)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Action Rapide Déclarer un Écart ---
            item {
                Button(
                    onClick = { dialogueNouvelleNc = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PourpreQualite, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.QualityBadge), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.qua_declarer_ecart), color = Color.White)
                }
            }

            // --- Titre File à Traiter ---
            item {
                Text(
                    stringResource(R.string.qua_titre_file),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (etat.toutes.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.QualityBadge,
                        title = stringResource(R.string.qua_aucun_ecart),
                        description = stringResource(R.string.qua_aucun_ecart_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.toutes, key = { it.id }) { nc ->
                    CarteNonConformite(
                        nc = nc,
                        devise = devise,
                        onAvancer = { vm.avancer(nc.id) },
                    )
                }
            }
        }
    }

    if (dialogueNouvelleNc) {
        DialogueNouvelleNc(
            enCours = enCours,
            onFermer = { dialogueNouvelleNc = false },
            onValider = { titre, gravite, orig, desc, resp, cout ->
                vm.declarer(titre, gravite, orig, desc, resp, cout)
            },
        )
    }
}

@Composable
private fun CarteNonConformite(
    nc: NonConformiteEntity,
    devise: String,
    onAvancer: () -> Unit,
) {
    val dateStr = remember(nc.date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nc.date))
    }
    val estResolue = nc.statut == StatutNc.RESOLUE.name

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nc.titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (nc.gravite) {
                        GraviteNc.CRITIQUE.name -> Color(0xFFFEE2E2)
                        GraviteNc.MAJEURE.name -> Color(0xFFFEF3C7)
                        else -> Color(0xFFF3F4F6)
                    },
                ) {
                    Text(
                        nc.gravite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (nc.gravite) {
                            GraviteNc.CRITIQUE.name -> Color(0xFFB91C1C)
                            GraviteNc.MAJEURE.name -> Color(0xFFD97706)
                            else -> MissaInk
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            nc.description?.let {
                Text(it, fontSize = 11.5.sp, color = MissaInk)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Origine : ${nc.origine} · $dateStr", fontSize = 10.5.sp, color = MissaMuted)
                if (nc.cout > 0.0) {
                    Text("Coût : ${fmtValeur(nc.cout, devise)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                }
            }
            if (!estResolue) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAvancer,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PourpreQualite, contentColor = Color.White),
                ) {
                    Text(
                        if (nc.statut == StatutNc.OUVERTE.name) stringResource(R.string.qua_passer_en_cours) else stringResource(R.string.qua_marquer_resolue),
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueNouvelleNc(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, GraviteNc, OrigineNc, String, String, String) -> Unit,
) {
    var titre by remember { mutableStateOf("") }
    var gravite by remember { mutableStateOf(GraviteNc.MINEURE) }
    var origine by remember { mutableStateOf(OrigineNc.INTERNE) }
    var description by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var cout by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.qua_declarer_ecart), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    label = { Text(stringResource(R.string.qua_champ_titre), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraviteNc.entries.forEach { g ->
                        FilterChip(
                            selected = gravite == g,
                            onClick = { gravite = g },
                            label = { Text(g.name, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PourpreQualite.copy(alpha = 0.2f)),
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.qua_champ_description), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cout,
                    onValueChange = { cout = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.qua_champ_cout_estime), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(titre, gravite, origine, description, responsable, cout) },
                enabled = titre.isNotBlank() && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
