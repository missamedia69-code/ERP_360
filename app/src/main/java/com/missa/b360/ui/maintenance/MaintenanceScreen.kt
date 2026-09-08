package com.missa.b360.ui.maintenance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention
import com.missa.b360.core.domain.model.EtatEquipement
import com.missa.b360.core.domain.model.QualiteMaintenanceRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaFondFiligrane
import com.missa.b360.ui.components.sectionFonctionsModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40

/**
 * Module Maintenance (MAI) — parc d'équipements, échéances préventives et
 * interventions.
 *
 * L'échéance se calcule depuis la dernière intervention **préventive** : une
 * réparation d'urgence ne remet pas le plan d'entretien à zéro, sans quoi une
 * machine qui tombe souvent en panne semblerait toujours à jour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val message by viewModel.message.collectAsState()
    val enCours by viewModel.enCours.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var dialogueEquipement by remember { mutableStateOf(false) }
    var dialogueIntervention by remember { mutableStateOf(false) }

    val textes = mapOf(
        MaintenanceViewModel.Message.Enregistre to R.string.mai_msg_ok,
        MaintenanceViewModel.Message.LectureSeule to R.string.tre_msg_lecture_seule,
        MaintenanceViewModel.Message.Invalide to R.string.tre_msg_invalide,
        MaintenanceViewModel.Message.NomDejaPris to R.string.mai_msg_nom_pris,
        MaintenanceViewModel.Message.Erreur to R.string.tre_msg_erreur,
    )
    val texte = message?.let { textes[it] }?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (texte != null) {
            snackbar.showSnackbar(texte)
            viewModel.effacerMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_maintenance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { dialogueEquipement = true }) {
                        Icon(
                            Icons.Outlined.PrecisionManufacturing,
                            contentDescription = stringResource(R.string.mai_nouvel_equipement),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (etat.equipements.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { dialogueIntervention = true },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mai_intervention), fontSize = 13.sp)
                }
            }
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.MAI),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { MaiSyntheseCarte(etat, devise) }

            item {
                Text(
                    text = stringResource(R.string.mai_parc),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (etat.parc.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.mai_aucun_equipement),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.parc, key = { it.equipement.id }) { equipement ->
                    MaiEquipementLigne(equipement, devise)
                }
            }

            // Sommaire des fonctionnalités du module, disponibles et prévues.
            sectionFonctionsModule(ModuleCode.MAI) { route -> onNaviguer(route) }
        }
        }
    }

    if (dialogueEquipement) {
        MaiEquipementDialogue(
            enCours = enCours,
            onFermer = { dialogueEquipement = false },
            onValider = { nom, type, code, periodicite ->
                viewModel.ajouterEquipement(nom, type, code, periodicite)
                dialogueEquipement = false
            },
        )
    }

    if (dialogueIntervention) {
        MaiInterventionDialogue(
            equipements = etat.equipements.map { it.id to it.nom },
            enCours = enCours,
            onFermer = { dialogueIntervention = false },
            onValider = { id, type, description, cout, duree, technicien ->
                viewModel.enregistrerIntervention(id, type, description, cout, duree, technicien)
                dialogueIntervention = false
            },
        )
    }
}

@Composable
private fun MaiSyntheseCarte(etat: MaintenanceViewModel.EtatMaintenance, devise: String) {
    val alerte = etat.enRetard > 0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alerte) ProfileOrange.copy(alpha = 0.12f) else MissaSoftBlue,
        ),
        border = BorderStroke(
            1.dp,
            if (alerte) ProfileOrange else BrandBlue.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MaiChiffre(R.string.mai_equipements, etat.parc.size.toString(), Modifier.weight(0.8f))
                MaiChiffre(
                    R.string.mai_taux_preventif,
                    Iso4217.formatPourcentage(etat.tauxPreventif),
                    Modifier.weight(1f),
                )
                MaiChiffre(
                    R.string.mai_cout_total,
                    MoneyUtils.format(etat.coutTotal, devise),
                    Modifier.weight(1.3f),
                )
            }
            if (alerte) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.mai_alerte_retard, etat.enRetard),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileOrange,
                )
            }
        }
    }
}

@Composable
private fun MaiChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valeur,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(libelleRes), fontSize = 10.sp, color = MissaMuted)
    }
}

/** Un équipement : échéance, pannes, coût cumulé. */
@Composable
private fun MaiEquipementLigne(etat: EtatEquipement, devise: String) {
    val type = QualiteMaintenanceRules.typeEquipement(etat.equipement.type)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                tint = if (etat.enRetard) ProfileOrange else BrandBlue,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etat.equipement.nom,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(QualiteMaintenanceRules.libelleTypeEquipement(type)),
                        etat.prochaineEcheance?.let {
                            if (etat.enRetard) {
                                stringResource(R.string.mai_retard_jours, etat.joursDeRetard)
                            } else {
                                stringResource(R.string.mai_prochaine, DateUtils.formatDate(it))
                            }
                        } ?: stringResource(R.string.mai_sans_preventif),
                        etat.pannes.takeIf { it > 0 }
                            ?.let { stringResource(R.string.mai_pannes, it) },
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = when {
                        etat.enRetard -> ProfileOrange
                        etat.pannes > 0 -> Red40
                        else -> MissaMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (etat.coutCumule > 0) {
                Text(
                    text = MoneyUtils.format(etat.coutCumule, devise),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
            } else if (etat.interventions == 0) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = ProfileGreen,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun MaiEquipementDialogue(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, TypeEquipement, String, String) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeEquipement.MACHINE) }
    var code by remember { mutableStateOf("") }
    var periodicite by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.mai_nouvel_equipement), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MaiChamp(nom, { nom = it }, R.string.mai_champ_nom)
                MissaSelecteurBleu(
                    label = stringResource(R.string.mai_champ_type),
                    options = TypeEquipement.entries.map {
                        MissaOption(
                            cle = it.name,
                            titre = stringResource(
                                QualiteMaintenanceRules.libelleTypeEquipement(it),
                            ),
                        )
                    },
                    selectionCle = type.name,
                    onSelection = { cle ->
                        type = QualiteMaintenanceRules.typeEquipement(cle)
                    },
                )
                MaiChamp(code, { code = it }, R.string.mai_champ_code)
                MaiChamp(
                    periodicite,
                    { periodicite = it },
                    R.string.mai_champ_periodicite,
                    numerique = true,
                )
                Text(
                    text = stringResource(R.string.mai_periodicite_aide),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(nom, type, code, periodicite) },
                enabled = TresorerieRules.libelleValide(nom) && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun MaiInterventionDialogue(
    equipements: List<Pair<Long, String>>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, TypeIntervention, String, String, String, String) -> Unit,
) {
    var equipementId by remember { mutableStateOf(equipements.firstOrNull()?.first ?: 0L) }
    var type by remember { mutableStateOf(TypeIntervention.PREVENTIVE) }
    var description by remember { mutableStateOf("") }
    var cout by remember { mutableStateOf("") }
    var duree by remember { mutableStateOf("") }
    var technicien by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.mai_intervention), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MissaSelecteurBleu(
                    label = stringResource(R.string.mai_champ_equipement),
                    options = equipements.map { MissaOption(cle = it.first.toString(), titre = it.second) },
                    selectionCle = equipementId.takeIf { it != 0L }?.toString(),
                    onSelection = { equipementId = it.toLongOrNull() ?: 0L },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TypeIntervention.entries.forEach { candidat ->
                        FilterChip(
                            selected = type == candidat,
                            onClick = { type = candidat },
                            label = {
                                Text(
                                    stringResource(
                                        QualiteMaintenanceRules.libelleTypeIntervention(candidat),
                                    ),
                                    fontSize = 11.5.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                MaiChamp(description, { description = it }, R.string.mai_champ_description)
                MaiChamp(cout, { cout = it }, R.string.mai_champ_cout, numerique = true)
                MaiChamp(duree, { duree = it }, R.string.mai_champ_duree, numerique = true)
                MaiChamp(technicien, { technicien = it }, R.string.mai_champ_technicien)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(equipementId, type, description, cout, duree, technicien) },
                enabled = TresorerieRules.libelleValide(description) &&
                    equipementId != 0L &&
                    !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun MaiChamp(
    valeur: String,
    onValeur: (String) -> Unit,
    labelRes: Int,
    numerique: Boolean = false,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie ->
            onValeur(
                if (numerique) saisie.filter { it.isDigit() || it == ',' || it == '.' } else saisie,
            )
        },
        label = { Text(stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
