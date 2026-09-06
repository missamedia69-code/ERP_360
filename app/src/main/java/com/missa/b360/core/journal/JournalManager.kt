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
        purgeSelonRetention(RETENTION_DEFAUT_JOURS, now)

    /**
     * Purge selon la durée de rétention choisie à la configuration
     * (30 jours, 90 jours ou 12 mois). @return nombre d'entrées supprimées.
     */
    suspend fun purgeSelonRetention(
        jours: Int,
        now: Long = System.currentTimeMillis(),
    ): Int = journalDao.purgeAvant(now - jours * JOUR_MS)

    companion object {
        const val JOUR_MS: Long = 24L * 60 * 60 * 1000

        /** Rétentions proposées à la configuration initiale (en jours). */
        val RETENTIONS_DISPONIBLES = listOf(30, 90, 365)

        /** Valeur par défaut : 12 mois (RA-18). */
        const val RETENTION_DEFAUT_JOURS: Int = 365

        /** Rétention par défaut exprimée en millisecondes (12 mois). */
        const val DUREE_RETENTION_MS: Long = 365L * 24 * 60 * 60 * 1000

        /**
         * Lit la rétention enregistrée (« 30 », « 90 », « 365 ») en retombant sur
         * 12 mois si la valeur est absente, illisible ou hors catalogue.
         */
        fun retentionEnJours(valeur: String?): Int =
            valeur?.trim()?.toIntOrNull()
                ?.takeIf { it in RETENTIONS_DISPONIBLES }
                ?: RETENTION_DEFAUT_JOURS
    }
}
