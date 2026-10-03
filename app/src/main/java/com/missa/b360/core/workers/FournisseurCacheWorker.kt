package com.missa.b360.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.missa.b360.core.domain.usecase.FournisseurCacheUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Tient les soldes et scores fournisseurs à jour : reconstruction après la migration, puis
 * recalcul quotidien (un retard grandit sans qu'aucun achat ne soit saisi) et correction
 * journalisée de tout solde qui s'écarterait des pièces.
 */
@HiltWorker
class FournisseurCacheWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cache: FournisseurCacheUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        cache.assurerInitialisation()
        cache.verifierCoherence()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        const val WORK_NAME_QUOTIDIEN = "fournisseur_cache_quotidien"
        const val WORK_NAME_DEMARRAGE = "fournisseur_cache_demarrage"
    }
}
