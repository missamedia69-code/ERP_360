package com.missa.b360.ui.maintenance

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
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention
import com.missa.b360.core.domain.model.EtatEquipement
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

/** Rouge brique caractéristique du module Maintenance — source unique : [AppModule.MAINTENANCE]. */
private val RougeMaintenance: Color get() = AppModule.MAINTENANCE.couleur

@Composable
fun MaintenanceScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: MaintenanceViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()

    var dialogueNouvelEquipement by remember { mutableStateOf(false) }
    var equipementPourIntervention by remember { mutableStateOf<EquipementEntity?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouvelEquipement = false
            equipementPourIntervention = null
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_maintenance),
            onBack = onBack,
            couleurFond = AppModule.MAINTENANCE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // --- Carte Synthèse Maintenance ---
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RougeMaintenance.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            stringResource(R.string.mai_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.mai_nb_equipements, etat.parc.size),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.mai_en_retard, etat.enRetard),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (etat.enRetard > 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                            )
                            Text(
                                stringResource(R.string.mai_cout_cumule, fmtValeur(etat.coutTotal, devise)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Action Rapide Ajouter Équipement ---
            item {
                Button(
                    onClick = { dialogueNouvelEquipement = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RougeMaintenance, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.HammerWrench), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.mai_ajouter_equipement), color = Color.White)
                }
            }

            // --- Titre Liste Équipements du Parc ---
            item {
                Text(
                    stringResource(R.string.mai_titre_parc),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (etat.parc.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.HammerWrench,
                        title = stringResource(R.string.mai_aucun_equipement),
                        description = stringResource(R.string.mai_aucun_equipement_desc),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                items(etat.parc, key = { it.equipement.id }) { itemParc ->
                    CarteEquipement(
                        parc = itemParc,
                        onIntervenir = { equipementPourIntervention = itemParc.equipement },
                    )
                }
            }
        }
    }

    if (dialogueNouvelEquipement) {
        DialogueNouvelEquipement(
            enCours = enCours,
            onFermer = { dialogueNouvelEquipement = false },
            onValider = { nom, type, code, periodicite ->
                vm.ajouterEquipement(nom, type, code, periodicite)
            },
        )
    }

    equipementPourIntervention?.let { eq ->
        DialogueIntervention(
            equipement = eq,
            enCours = enCours,
            onFermer = { equipementPourIntervention = null },
            onValider = { type, desc, cout, duree, tech ->
                vm.enregistrerIntervention(eq.id, type, desc, cout, duree, tech)
            },
        )
    }
}

@Composable
private fun CarteEquipement(
    parc: EtatEquipement,
    onIntervenir: () -> Unit,
) {
    val eq = parc.equipement
    val enRetard = parc.enRetard

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(eq.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (enRetard) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                ) {
                    Text(
                        stringResource(if (enRetard) R.string.mai_retard_maintenance else R.string.mai_a_jour),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enRetard) Color(0xFFB91C1C) else Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Type : ${eq.type}", fontSize = 11.sp, color = MissaMuted)
                Text(
                    if (eq.periodiciteJours > 0) "Entretien tous les ${eq.periodiciteJours} j" else "Curatif seul",
                    fontSize = 11.sp,
                    color = MissaInk,
                )
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onIntervenir,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RougeMaintenance, contentColor = Color.White),
            ) {
                Text(stringResource(R.string.mai_enregistrer_intervention), fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun DialogueNouvelEquipement(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, TypeEquipement, String, String) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeEquipement.MACHINE) }
    var code by remember { mutableStateOf("") }
    var periodicite by remember { mutableStateOf("30") }

    MissaFormDialogue(
        titre = stringResource(R.string.mai_ajouter_equipement),
        icone = Iv.HammerWrench,
        couleur = AppModule.MAINTENANCE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = nom.isNotBlank() && (periodicite.toIntOrNull() ?: 0) > 0,
        enCours = enCours,
        onValider = { onValider(nom.trim(), type, code.trim(), periodicite) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 1) {
            MissaRangee {
                MissaChampTexte(nom, { nom = it }, stringResource(R.string.mai_champ_nom), icone = Iv.Build, requis = true, modifier = Modifier.weight(1f))
                MissaChampTexte(code, { code = it }, stringResource(R.string.form_code_inventaire), icone = Iv.Badge, clavier = MissaClavier.MOT_CLE, modifier = Modifier.weight(1f))
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 2) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(TypeEquipement.MACHINE, stringResource(R.string.mai_type_machine), Iv.Factory),
                    MissaTuile(TypeEquipement.VEHICULE, stringResource(R.string.mai_type_vehicule), Iv.LocalShipping),
                    MissaTuile(TypeEquipement.INSTALLATION, stringResource(R.string.mai_type_installation), Iv.Construction),
                    MissaTuile(TypeEquipement.OUTILLAGE, stringResource(R.string.mai_type_outillage), Iv.HammerWrench),
                    MissaTuile(TypeEquipement.INFORMATIQUE, stringResource(R.string.mai_type_informatique), Iv.Smartphone),
                ),
                selection = type,
                onSelection = { type = it },
                colonnes = 3,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_planification), numero = 3) {
            MissaChampTexte(periodicite, { periodicite = it }, stringResource(R.string.mai_champ_periodicite_jours), icone = Iv.Schedule, clavier = MissaClavier.ENTIER, requis = true)
        }
    }
}

@Composable
private fun DialogueIntervention(
    equipement: EquipementEntity,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (TypeIntervention, String, String, String, String) -> Unit,
) {
    var type by remember { mutableStateOf(TypeIntervention.PREVENTIVE) }
    var description by remember { mutableStateOf("") }
    var cout by remember { mutableStateOf("") }
    var duree by remember { mutableStateOf("1") }
    var technicien by remember { mutableStateOf("") }

    MissaFormDialogue(
        titre = "${stringResource(R.string.mai_intervention_sur)} ${equipement.nom}",
        icone = Iv.Build,
        couleur = AppModule.MAINTENANCE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = description.isNotBlank(),
        enCours = enCours,
        onValider = { onValider(type, description.trim(), cout, duree, technicien.trim()) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 1) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(TypeIntervention.PREVENTIVE, stringResource(R.string.mai_type_preventive), Iv.Calendar),
                    MissaTuile(TypeIntervention.CORRECTIVE, stringResource(R.string.mai_type_corrective), Iv.Build),
                ),
                selection = type,
                onSelection = { type = it },
                colonnes = 2,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 2) {
            MissaChampTexte(description, { description = it }, stringResource(R.string.mai_champ_description_travaux), icone = Iv.Description, requis = true, lignes = 3)
            MissaChampTexte(technicien, { technicien = it }, stringResource(R.string.form_technicien), icone = Iv.Person)
            MissaRangee {
                MissaChampTexte(duree, { duree = it }, stringResource(R.string.form_duree_heures), modifier = Modifier.weight(1f), icone = Iv.Schedule, clavier = MissaClavier.DECIMAL)
                MissaChampTexte(cout, { cout = it }, stringResource(R.string.mai_champ_cout_pieces), modifier = Modifier.weight(1f), icone = Iv.Payments, clavier = MissaClavier.DECIMAL)
            }
        }
    }
}
