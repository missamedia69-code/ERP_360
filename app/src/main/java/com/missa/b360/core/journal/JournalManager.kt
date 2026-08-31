package com.missa.b360.core.journal

import com.missa.b360.core.data.dao.JournalDao
import com.missa.b360.core.data.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JournalManager (RA-18) — journal transversal, **immuable** (écriture seule).
 * Chaque action sensible appelle [log]. Purge automatique 12 mois via WorkManager.
 */
@Singleton
class JournalManager @Inject constructor(
    private val journalDao: JournalDao,
) {
    suspend fun log(
        module: String,
        action: String,
        details: String,
        userId: Long? = null,
        horodatage: Long = System.currentTimeMillis(),
    ) {
        journalDao.insert(
            JournalEntryEntity(
                horodatage = horodatage,
                userId = userId,
                module = module,
                action = action,
                details = details,
            ),
        )
    }

    fun observeRecent(limit: Int = 200): Flow<List<JournalEntryEntity>> =
        journalDao.observeRecent(limit)

    /** Purge des entrées de plus de 12 mois (RA-18). @return nombre d'entrées supprimées. */
    suspend fun purgePlusDe12Mois(now: Long = System.currentTimeMillis()): Int =
        journalDao.purgeAvant(now - DUREE_RETENTION_MS)

    companion object {
        const val DUREE_RETENTION_MS: Long = 365L * 24 * 60 * 60 * 1000 // 12 mois
    }
}
