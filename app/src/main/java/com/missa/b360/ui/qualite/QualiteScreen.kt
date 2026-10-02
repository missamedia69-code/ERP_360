package com.missa.b360.ui.qualite

import com.missa.b360.ui.theme.OnbConfigCard
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
import com.missa.b360.ui.components.*

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
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // --- Carte Synthèse Qualité ---
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PourpreQualite.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            stringResource(R.string.qua_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.qua_ecarts_ouverts, etat.bilan.ouvertes),
                            fontSize = 16.sp,
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
                        modifier = Modifier.padding(12.dp),
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
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
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
                Spacer(Modifier.height(6.dp))
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

    MissaFormDialogue(
        titre = stringResource(R.string.qua_declarer_ecart),
        icone = Iv.QualityBadge,
        couleur = AppModule.QUALITE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = titre.isNotBlank(),
        enCours = enCours,
        onValider = { onValider(titre.trim(), gravite, origine, description.trim(), responsable.trim(), cout) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 1) {
            MissaChampTexte(titre, { titre = it }, stringResource(R.string.qua_champ_titre), icone = Iv.Warning, requis = true)
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(GraviteNc.MINEURE, stringResource(R.string.qua_gravite_mineure), Iv.Info),
                    MissaTuile(GraviteNc.MAJEURE, stringResource(R.string.qua_gravite_majeure), Iv.Warning),
                    MissaTuile(GraviteNc.CRITIQUE, stringResource(R.string.qua_gravite_critique), Iv.Prohibit),
                ),
                selection = gravite,
                onSelection = { gravite = it },
                colonnes = 3,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_origine), numero = 2) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(OrigineNc.PRODUCTION, stringResource(R.string.qua_origine_production), Iv.Factory),
                    MissaTuile(OrigineNc.RECEPTION, stringResource(R.string.qua_origine_reception), Iv.LocalShipping),
                    MissaTuile(OrigineNc.CLIENT, stringResource(R.string.qua_origine_client), Iv.Person),
                    MissaTuile(OrigineNc.INTERNE, stringResource(R.string.qua_origine_interne), Iv.Business),
                ),
                selection = origine,
                onSelection = { origine = it },
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 3) {
            MissaChampTexte(description, { description = it }, stringResource(R.string.qua_champ_description), icone = Iv.Description, lignes = 3)
            MissaRangee {
                MissaChampTexte(responsable, { responsable = it }, stringResource(R.string.form_responsable), icone = Iv.Person, modifier = Modifier.weight(1f))
                MissaChampTexte(cout, { cout = it }, stringResource(R.string.qua_champ_cout_estime), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
            }
        }
    }
}
