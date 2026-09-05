package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaskStatus
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import javax.inject.Inject

/**
 * Tâches de suivi (spec §Tâches) — jamais de suppression : on passe en FAITE.
 */
class SaveTaskUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val taskId: Long) : Result()
        data object LectureSeule : Result()
        data object TitreInvalide : Result()
        data object Introuvable : Result()
    }

    suspend operator fun invoke(
        taskId: Long?,
        titre: String,
        notes: String?,
        echeance: Long?,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val clean = titre.trim()
        if (clean.isEmpty()) return Result.TitreInvalide

        if (taskId == null) {
            val id = taskDao.insert(
                TaskEntity(
                    titre = clean,
                    notes = notes?.trim()?.ifBlank { null },
                    echeance = echeance,
                    createdAt = now,
                ),
            )
            journalManager.log("TACHES", "TACHE_CREEE", clean)
            return Result.Succes(id)
        }

        val tache = taskDao.listAll().firstOrNull { it.id == taskId } ?: return Result.Introuvable
        taskDao.update(
            tache.copy(
                titre = clean,
                notes = notes?.trim()?.ifBlank { null },
                echeance = echeance,
            ),
        )
        journalManager.log("TACHES", "TACHE_MODIFIEE", clean)
        return Result.Succes(taskId)
    }
}

class SetTaskStatusUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
    }

    suspend operator fun invoke(taskId: Long, statut: TaskStatus): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val tache = taskDao.listAll().firstOrNull { it.id == taskId }
            ?: return Result.Introuvable
        if (tache.statut == statut.name) return Result.Succes
        taskDao.update(tache.copy(statut = statut.name))
        val evenement = when (statut) {
            TaskStatus.EN_COURS -> "TACHE_DEMARREE"
            TaskStatus.FAITE -> "TACHE_FAITE"
            TaskStatus.A_FAIRE -> "TACHE_REOUVERTE"
        }
        journalManager.log("TACHES", evenement, tache.titre)
        return Result.Succes
    }
}
