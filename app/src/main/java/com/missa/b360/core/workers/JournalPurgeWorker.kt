package com.missa.b360.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.missa.b360.core.backup.BackupManager
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.journal.JournalManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Purge automatique du journal : suppression des entrées plus anciennes que la
 * rétention choisie à la configuration initiale — 30 jours, 90 jours ou 12 mois
 * par défaut (RA-18). Planifié quotidiennement dans [com.missa.b360.MissaApp].
 */
@HiltWorker
class JournalPurgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val journalManager: JournalManager,
    private val backupManager: BackupManager,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val jours = JournalManager.retentionEnJours(
            settingsStore.get(SettingsStore.Keys.RETENTION_JOURNAL),
        )
        journalManager.purgeSelonRetention(jours)
        // Sauvegarde automatique quotidienne (défaut — fréquence au choix en Phase C).
        backupManager.sauvegarderLocalement(type = "AUTO")
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "journal_purge_quotidienne"
    }
}
