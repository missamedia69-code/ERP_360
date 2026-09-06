package com.missa.b360.core.domain.usecase

import android.net.Uri
import com.missa.b360.core.backup.BackupManager
import com.missa.b360.core.backup.ResultatRestauration
import com.missa.b360.core.data.dao.JournalDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.journal.JournalManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

/** RA-13 — Sauvegarde locale + liste de l'historique. */
class BackupUseCases @Inject constructor(
    private val backupManager: BackupManager,
) {
    suspend fun sauvegarder(): File? = backupManager.sauvegarderLocalement("MANUAL")
    fun historique(): Flow<List<BackupEntity>> = backupManager.observeHistorique()

    /** Restaure une sauvegarde choisie par l'utilisateur (fichier .db). */
    suspend fun restaurer(source: Uri): ResultatRestauration = backupManager.restaurerDepuis(source)
}

/** RA-18 — Journal d'audit : consultation (immuable) + purge manuelle. */
class JournalUseCases @Inject constructor(
    private val journalDao: JournalDao,
    private val journalManager: JournalManager,
    private val settingsStore: SettingsStore,
) {
    fun observer(limit: Int = 200): Flow<List<JournalEntryEntity>> = journalDao.observeRecent(limit)

    /** Purge manuelle respectant la rétention choisie (30 / 90 jours / 12 mois). */
    suspend fun purge(): Int = journalManager.purgeSelonRetention(retentionJours())

    /** Rétention active, en jours. */
    suspend fun retentionJours(): Int =
        JournalManager.retentionEnJours(settingsStore.get(SettingsStore.Keys.RETENTION_JOURNAL))
}