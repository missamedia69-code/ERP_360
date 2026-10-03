package com.missa.b360

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.missa.b360.core.workers.ClientBalanceWorker
import com.missa.b360.core.workers.FournisseurCacheWorker
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
        scheduleClientBalances()
        scheduleFournisseurCache()
    }

    /** Soldes et scores fournisseurs : reconstruction au démarrage (après migration) puis recalcul quotidien. */
    private fun scheduleFournisseurCache() {
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork(
            FournisseurCacheWorker.WORK_NAME_DEMARRAGE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FournisseurCacheWorker>().build(),
        )
        workManager.enqueueUniquePeriodicWork(
            FournisseurCacheWorker.WORK_NAME_QUOTIDIEN,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FournisseurCacheWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /** Comptes clients : reconstruction au démarrage (après migration) puis rafraîchissement quotidien. */
    private fun scheduleClientBalances() {
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork(
            ClientBalanceWorker.WORK_NAME_DEMARRAGE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ClientBalanceWorker>().build(),
        )
        workManager.enqueueUniquePeriodicWork(
            ClientBalanceWorker.WORK_NAME_QUOTIDIEN,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ClientBalanceWorker>(1, TimeUnit.DAYS).build(),
        )
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
