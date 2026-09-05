package com.missa.b360.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaskStatus
import com.missa.b360.core.domain.usecase.SaveTaskUseCase
import com.missa.b360.core.domain.usecase.SetTaskStatusUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.admin.AdminScaffold
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Tâches de suivi (spec §Tâches) — ajout, édition, changement de statut.
 * Jamais de suppression : une tâche se termine en la marquant FAITE.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val saveTask: SaveTaskUseCase,
    private val setTaskStatus: SetTaskStatusUseCase,
    taskDao: TaskDao,
) : ViewModel() {

    data class UiMessage(val key: Int, val ok: Boolean = true)

    val tasks: StateFlow<List<TaskEntity>> = taskDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun save(taskId: Long?, titre: String, notes: String?, echeance: Long?) {
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = saveTask(taskId, titre, notes, echeance)) {
                    is SaveTaskUseCase.Result.Succes -> _message.value = UiMessage(R.string.tasks_saved)
                    SaveTaskUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.tasks_read_only, ok = false)
                    SaveTaskUseCase.Result.TitreInvalide -> _message.value = UiMessage(R.string.tasks_titre_requis, ok = false)
                    SaveTaskUseCase.Result.Introuvable -> _message.value = UiMessage(R.string.tasks_introuvable, ok = false)
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun setStatut(taskId: Long, statut: TaskStatus) {
        viewModelScope.launch {
            _busy.value = true
            try {
                when (setTaskStatus(taskId, statut)) {
                    SetTaskStatusUseCase.Result.Succes -> Unit
                    SetTaskStatusUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.tasks_read_only, ok = false)
                    SetTaskStatusUseCase.Result.Introuvable -> _message.value = UiMessage(R.string.tasks_introuvable, ok = false)
                }
            } finally {
                _busy.value = false
            }
        }
    }
}

/**
 * Écran Tâches — structure admin standard : formulaire + liste.
 */
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var formVisible by remember { mutableStateOf(true) }
    var editionId by remember { mutableStateOf<Long?>(null) }
    var titre by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val aujourdhui = remember { Calendar.getInstance() }
    var echeanceJour by remember { mutableStateOf("") }
    var echeanceMois by remember { mutableStateOf("") }
    var echeanceAnnee by remember { mutableStateOf("") }

    fun viderForm() {
        editionId = null; titre = ""; notes = ""
        echeanceJour = ""; echeanceMois = ""; echeanceAnnee = ""
    }

    fun charger(t: TaskEntity) {
        editionId = t.id; titre = t.titre; notes = t.notes ?: ""
        t.echeance?.let { e ->
            val cal = Calendar.getInstance().apply { timeInMillis = e }
            echeanceJour = cal.get(Calendar.DAY_OF_MONTH).toString()
            echeanceMois = (cal.get(Calendar.MONTH) + 1).toString()
            echeanceAnnee = cal.get(Calendar.YEAR).toString()
        }
        formVisible = true
    }

    fun echeance(): Long? {
        val j = echeanceJour.toIntOrNull() ?: return null
        val m = echeanceMois.toIntOrNull() ?: return null
        val a = echeanceAnnee.toIntOrNull() ?: aujourdhui.get(Calendar.YEAR)
        if (j < 1 || j > 31 || m < 1 || m > 12) return null
        return Calendar.getInstance().apply {
            clear()
            set(a, m - 1, j, 12, 0, 0)
        }.timeInMillis
    }

    LaunchedEffect(viewModel.message) {
        val msg = viewModel.message.value ?: return@LaunchedEffect
        snackbar.showSnackbar(context.getString(msg.key))
        viewModel.consumeMessage()
    }

    AdminScaffold(titreRes = R.string.tasks_title, onBack = onBack) {
        SnackbarHost(snackbar) { Snackbar(it) }

        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(
                title = if (editionId != null) stringResource(R.string.tasks_modifier) else stringResource(R.string.tasks_nouvelle),
            )
            OutlinedTextField(
                value = titre,
                onValueChange = { titre = it },
                label = { Text(stringResource(R.string.tasks_titre)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.tasks_notes)) },
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = echeanceJour,
                    onValueChange = { echeanceJour = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.tasks_echeance, "jj")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = echeanceMois,
                    onValueChange = { echeanceMois = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.tasks_echeance, "mm")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = echeanceAnnee,
                    onValueChange = { echeanceAnnee = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.tasks_echeance, "aaaa")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.4f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (editionId != null) {
                    TextButton(onClick = { viderForm() }) {
                        Text(stringResource(R.string.tasks_annuler))
                    }
                }
                Button(
                    onClick = {
                        viewModel.save(editionId, titre, notes.ifBlank { null }, echeance())
                        viderForm()
                    },
                    enabled = !busy && titre.isNotBlank(),
                ) {
                    Text(stringResource(R.string.tasks_enregistrer))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(title = stringResource(R.string.tasks_liste))
            if (tasks.isEmpty()) {
                Text(stringResource(R.string.tasks_vide), color = MissaMuted, fontSize = 12.sp)
            } else {
                tasks.forEach { t ->
                    val faîte = t.statut == TaskStatus.FAITE.name
                    val enCours = t.statut == TaskStatus.EN_COURS.name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MissaCanvas),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).alpha(if (faîte) 0.6f else 1f)) {
                                Text(
                                    t.titre,
                                    color = MissaInk,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    textDecoration = if (faîte) TextDecoration.LineThrough else null,
                                )
                                val sousTitre = buildString {
                                    append(
                                        stringResource(
                                            when {
                                                faîte -> R.string.tasks_faite
                                                enCours -> R.string.tasks_en_cours
                                                else -> R.string.tasks_a_faire
                                            },
                                        ),
                                    )
                                    t.echeance?.let { append(" · ").append(DateUtils.formatDate(it)) }
                                    if (!t.notes.isNullOrBlank()) append(" · ").append(t.notes)
                                }
                                Text(sousTitre, color = MissaMuted, fontSize = 11.sp)
                            }
                            if (!faîte) {
                                IconButton(onClick = { viewModel.setStatut(t.id, TaskStatus.FAITE) }, enabled = !busy) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.tasks_done),
                                        tint = Green60,
                                    )
                                }
                            } else {
                                TextButton(onClick = { viewModel.setStatut(t.id, TaskStatus.A_FAIRE) }, enabled = !busy) {
                                    Text(stringResource(R.string.tasks_reopen), color = MissaMuted, fontSize = 11.sp)
                                }
                            }
                            if (!enCours && !faîte) {
                                IconButton(onClick = { viewModel.setStatut(t.id, TaskStatus.EN_COURS) }, enabled = !busy) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(R.string.tasks_start),
                                        tint = BrandBlue,
                                    )
                                }
                            }
                            IconButton(onClick = { charger(t) }, enabled = !busy) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.tasks_modifier_ic), tint = MissaMuted)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
