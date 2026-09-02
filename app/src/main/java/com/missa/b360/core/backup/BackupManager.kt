package com.missa.b360.core.backup

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.journal.JournalManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackupManager (RA-13) — export local (Drive/iCloud en Phase C).
 * Sauvegarde cohérente via `VACUUM INTO` (snapshot atomique de la base).
 * Aucune donnée de démo dans les sauvegardes : uniquement les données réelles.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val backupDao: BackupDao,
    private val journalManager: JournalManager,
) {
    /** Sauvegarde locale complète. @return le fichier créé, ou null en cas d'échec. */
    suspend fun sauvegarderLocalement(type: String = "MANUAL"): File? {
        return try {
            val dir = File(context.filesDir, "backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val fichier = File(dir, "missa_b360_$stamp.db")
            database.openHelper.writableDatabase.query(
                SimpleSQLiteQuery("VACUUM INTO ?", arrayOf(fichier.absolutePath)),
            ).use { it.moveToFirst() }
            backupDao.insert(
                BackupEntity(
                    date = System.currentTimeMillis(),
                    type = type,
                    chemin = fichier.absolutePath,
                    plateforme = "LOCAL",
                ),
            )
            journalManager.log("ADMIN", "SAUVEGARDE", "Sauvegarde locale : ${fichier.name}")
            fichier
        } catch (e: Exception) {
            journalManager.log("ADMIN", "SAUVEGARDE_ECHEC", "Erreur : ${e.message}")
            null
        }
    }

    fun observeHistorique() = backupDao.observeAll()
}
