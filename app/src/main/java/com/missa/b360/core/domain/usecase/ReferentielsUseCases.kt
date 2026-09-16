package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.TaxEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observation des taxes (référentiel) — exposé via UseCase pour respecter Clean Architecture. */
class ObserveTaxesUseCase @Inject constructor(
    private val taxDao: TaxDao,
) {
    operator fun invoke(): Flow<List<TaxEntity>> = taxDao.observeAll()
}

/** Observation des moyens de paiement — exposé via UseCase. */
class ObservePaymentMethodsUseCase @Inject constructor(
    private val paymentMethodDao: PaymentMethodDao,
) {
    operator fun invoke(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()
}

/** Observation des sites / dépôts — exposé via UseCase (alternative à SiteUseCases.observerSites). */
class ObserveSitesUseCase @Inject constructor(
    private val siteDao: SiteDao,
) {
    operator fun invoke(): Flow<List<SiteEntity>> = siteDao.observeAll()
}
