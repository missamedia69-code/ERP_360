package com.missa.b360.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaskStatus
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Teinte violette spécifique au module Tâches. */
private val VioletTasks: Color = Color(0xFF7C3AED)

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun TasksScreen(
    onBack: () -> Unit,
    vm: TasksViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()
    var dialogueNouvelleTache by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.tasks_title),
            onBack = onBack,
            couleurFond = VioletTasks.copy(alpha = 0.2f),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Hero Synthèse Tâches ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VioletTasks.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.tasks_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.tasks_en_attente_total, etat.aFaireCount + etat.enCoursCount),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.tasks_urgentes_count, etat.urgentesCount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (etat.urgentesCount > 0) Color(0xFFDC2626) else VioletTasks,
                            )
                            Text(
                                stringResource(R.string.tasks_terminees_count, etat.faitesCount),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.tasks_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.Checklist,
                        titre = stringResource(R.string.tasks_tuile_toutes),
                        sousTitre = stringResource(R.string.tasks_nb_count, etat.tasks.size),
                        estActif = filtre == null,
                        onClick = { vm.filtrer(null) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Schedule,
                        titre = stringResource(R.string.tasks_statut_a_faire),
                        sousTitre = stringResource(R.string.tasks_nb_count, etat.aFaireCount),
                        estActif = filtre == TaskStatus.A_FAIRE,
                        onClick = { vm.filtrer(TaskStatus.A_FAIRE) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Build,
                        titre = stringResource(R.string.tasks_statut_en_cours),
                        sousTitre = stringResource(R.string.tasks_nb_count, etat.enCoursCount),
                        estActif = filtre == TaskStatus.EN_COURS,
                        onClick = { vm.filtrer(TaskStatus.EN_COURS) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Add,
                        titre = stringResource(R.string.tasks_nouvelle_tache),
                        sousTitre = stringResource(R.string.st_creer),
                        estActif = false,
                        onClick = { dialogueNouvelleTache = true },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileTasks(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Registre des Tâches ---
            item {
                Text(
                    stringResource(R.string.tasks_titre_registre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (etat.tasks.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Checklist,
                        title = stringResource(R.string.tasks_aucune),
                        description = stringResource(R.string.tasks_aucune_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.tasks, key = { it.id }) { task ->
                    CarteTask(
                        task = task,
                        onChangerStatut = { s -> vm.changerStatut(task, s) },
                    )
                }
            }
        }
    }

    if (dialogueNouvelleTache) {
        DialogueNouvelleTask(
            onFermer = { dialogueNouvelleTache = false },
            onValider = { titre, notes ->
                vm.creer(titre, notes)
                dialogueNouvelleTache = false
            },
        )
    }
}

@Composable
private fun TuileTasks(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) VioletTasks.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) VioletTasks else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CarteTask(
    task: TaskEntity,
    onChangerStatut: (TaskStatus) -> Unit,
) {
    val dateStr = remember(task.createdAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(task.createdAt))
    }

    val (statutTexte, badgeCouleur, badgeFond) = when (task.statut) {
        TaskStatus.A_FAIRE.name -> Triple(stringResource(R.string.tasks_statut_a_faire), Color(0xFFD97706), Color(0xFFFEF3C7))
        TaskStatus.EN_COURS.name -> Triple(stringResource(R.string.tasks_statut_en_cours), VioletTasks, VioletTasks.copy(alpha = 0.15f))
        else -> Triple(stringResource(R.string.tasks_statut_faite), Color(0xFF15803D), Color(0xFFDCFCE7))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeFond,
                ) {
                    Text(
                        statutTexte,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeCouleur,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            task.notes?.let { n ->
                Spacer(Modifier.height(4.dp))
                Text(n, fontSize = 11.5.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateStr, fontSize = 10.sp, color = MissaMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.statut == TaskStatus.A_FAIRE.name) {
                        Button(
                            onClick = { onChangerStatut(TaskStatus.EN_COURS) },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletTasks, contentColor = Color.White),
                        ) {
                            Text(stringResource(R.string.tasks_action_demarrer), fontSize = 11.sp, color = Color.White)
                        }
                    } else if (task.statut == TaskStatus.EN_COURS.name) {
                        Button(
                            onClick = { onChangerStatut(TaskStatus.FAITE) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D), contentColor = Color.White),
                        ) {
                            Text(stringResource(R.string.tasks_action_terminer), fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueNouvelleTask(
    onFermer: () -> Unit,
    onValider: (String, String?) -> Unit,
) {
    var titre by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.tasks_nouvelle_tache), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    label = { Text(stringResource(R.string.tasks_champ_titre), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.tasks_champ_notes), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(titre, notes) },
                enabled = titre.isNotBlank(),
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
