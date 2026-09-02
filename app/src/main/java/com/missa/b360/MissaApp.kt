package com.missa.b360

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.missa.b360.core.workers.JournalPurgeWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application Missa Business 360.
 *
 * - @HiltAndroidApp : injection de dépendances (cahier de charge §3).
 * - Planifie la purge du journal (12 mois — RA-18) via WorkManager.
 */
@HiltAndroidApp
class MissaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleJournalPurge()
    }

    /** Purge automatique du journal : entrées de plus de 12 mois supprimées (RA-18). */
    private fun scheduleJournalPurge() {
        val request = PeriodicWorkRequestBuilder<JournalPurgeWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            JournalPurgeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
