package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.LicenceDao
import com.missa.b360.core.data.entity.LicenceStatus
import com.missa.b360.core.licensing.LicenceManager
import javax.inject.Inject

/** RA-04..06 / D3 — Statut de licence + échéances pour l'écran Licence (9.1). */
class GetLicenceInfoUseCase @Inject constructor(
    private val licenceDao: LicenceDao,
    private val licenceManager: LicenceManager,
) {
    data class LicenceInfo(
        val statut: LicenceStatus,
        val dateDebutEssai: Long? = null,
        val dateExpiration: Long? = null,
        val code: String? = null,
        val appareilId: String? = null,
        val desassociationsUtilisees: Int = 0,
        val desassociationsMax: Int = LicenceManager.DESASSOCIATIONS_MAX_PAR_AN,
    )

    suspend operator fun invoke(): LicenceInfo {
        val licence = licenceDao.get()
        val statut = licenceManager.statut()
        return LicenceInfo(
            statut = statut,
            dateDebutEssai = licence?.dateDebutEssai,
            dateExpiration = licence?.dateExpiration,
            code = licence?.code,
            appareilId = licence?.appareilId,
            desassociationsUtilisees = licence?.desassociationsUtilisees ?: 0,
        )
    }
}

/** RA-06 — Désassociation d'appareil (3/an max) depuis l'écran Licence. */
class DissociateDeviceUseCase @Inject constructor(
    private val licenceManager: LicenceManager,
) {
    sealed class Result {
        data object Success : Result()
        data object MaxAtteint : Result()
        data object PasDeLicence : Result()
    }

    suspend operator fun invoke(): Result =
        if (licenceManager.desassocierAppareil()) Result.Success else Result.MaxAtteint
}