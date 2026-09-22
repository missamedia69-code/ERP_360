package com.missa.b360.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskDao: TaskDao,
) : ViewModel() {

    data class EtatTasks(
        val tasks: List<TaskEntity> = emptyList(),
        val aFaireCount: Int = 0,
        val enCoursCount: Int = 0,
        val faitesCount: Int = 0,
        val urgentesCount: Int = 0,
    )

    private val _filtre = MutableStateFlow<TaskStatus?>(null)
    val filtre: StateFlow<TaskStatus?> = _filtre

    val etat: StateFlow<EtatTasks> = combine(
        taskDao.observeAll(),
        _filtre,
    ) { toutes, f ->
        val maintenant = System.currentTimeMillis()
        EtatTasks(
            tasks = toutes.filter { f == null || it.statut == f.name }.sortedWith(
                compareBy<TaskEntity> { it.statut == TaskStatus.FAITE.name }
                    .thenBy { it.echeance ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt },
            ),
            aFaireCount = toutes.count { it.statut == TaskStatus.A_FAIRE.name },
            enCoursCount = toutes.count { it.statut == TaskStatus.EN_COURS.name },
            faitesCount = toutes.count { it.statut == TaskStatus.FAITE.name },
            urgentesCount = toutes.count { it.statut != TaskStatus.FAITE.name && it.echeance != null && it.echeance <= maintenant },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatTasks())

    fun filtrer(statut: TaskStatus?) {
        _filtre.value = if (_filtre.value == statut) null else statut
    }

    fun creer(titre: String, notes: String?, echeance: Long? = null) {
        if (titre.isBlank()) return
        viewModelScope.launch {
            taskDao.insert(
                TaskEntity(
                    titre = titre.trim(),
                    notes = notes?.trim()?.ifBlank { null },
                    statut = TaskStatus.A_FAIRE.name,
                    echeance = echeance,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun changerStatut(task: TaskEntity, nouveauStatut: TaskStatus) {
        viewModelScope.launch {
            taskDao.update(task.copy(statut = nouveauStatut.name))
        }
    }
}
