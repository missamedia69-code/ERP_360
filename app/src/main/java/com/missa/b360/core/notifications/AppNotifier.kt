package com.missa.b360.core.notifications

import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppNotifier (RA-23) — notifications **locales** (offline), badge non-lues,
 * réglage on/off par type (Phase C).
 */
@Singleton
class AppNotifier @Inject constructor(
    private val notificationDao: NotificationDao,
) {
    suspend fun notifier(type: String, titre: String, message: String, date: Long = System.currentTimeMillis()) {
        notificationDao.insert(
            NotificationEntity(type = type, titre = titre, message = message, date = date),
        )
    }

    fun observeAll(): Flow<List<NotificationEntity>> = notificationDao.observeAll()

    /** Badge cloche : recalculé après lecture (implémenté par flow réactif). */
    fun observeNonLues(): Flow<Int> = notificationDao.observeNonLues()

    suspend fun marquerLue(id: Long) = notificationDao.marquerLue(id)

    suspend fun marquerToutesLues() = notificationDao.marquerToutesLues()
}
