package com.missa.b360.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.missa.b360.core.domain.usecase.ClientBalanceUseCase
import com.missa.b360.core.domain.usecase.ClientFollowupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Tient les comptes clients à jour : reconstruction après la migration, puis rafraîchissement
 * quotidien des échéances (un retard grandit sans qu'aucune vente ne soit saisie).
 * Planifié au démarrage et chaque jour dans [com.missa.b360.MissaApp].
 */
@HiltWorker
class ClientBalanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val clientBalance: ClientBalanceUseCase,
    private val followups: ClientFollowupUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        clientBalance.assurerInitialisation()
        clientBalance.rafraichirEcheances()
        followups.reevaluerPromesses()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        const val WORK_NAME_QUOTIDIEN = "client_balances_quotidien"
        const val WORK_NAME_DEMARRAGE = "client_balances_demarrage"
    }
}
