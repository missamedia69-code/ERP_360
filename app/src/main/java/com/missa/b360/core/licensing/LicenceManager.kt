package com.missa.b360.core.licensing

import android.provider.Settings
import com.missa.b360.core.data.dao.LicenceDao
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.LicenceStatus
import com.missa.b360.core.journal.JournalManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LicenceManager (RA-04, RA-05, RA-06 — D3) :
 * - essai **7 jours** → verrouillage ;
 * - code d'activation : **1 appareil / 1 an** ;
 * - licence expirée → **lecture seule** (consultation + export autorisés) ;
 * - désassociation d'appareil : **3/an** max.
 */
@Singleton
class LicenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val licenceDao: LicenceDao,
    private val journalManager: JournalManager,
) {
    companion object {
        const val DUREE_ESSAI_MS: Long = 7L * 24 * 60 * 60 * 1000
        const val DUREE_LICENCE_MS: Long = 365L * 24 * 60 * 60 * 1000
        const val DESASSOCIATIONS_MAX_PAR_AN = 3

        /** Format attendu : MB360-XXXX-XXXX-XXXX (X = A–Z0–9). */
        private val CODE_REGEX = Regex("^MB360-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")

        const val MODULE = "ADMIN"
    }

    fun appareilId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "inconnu"

    /** Crée la licence d'essai au premier lancement si absente (RA-04). */
    suspend fun ensureTrialStarted(now: Long = System.currentTimeMillis()) {
        if (licenceDao.get() == null) {
            licenceDao.upsert(LicenceEntity(dateDebutEssai = now, appareilId = appareilId()))
        }
    }

    /** Statut effectif : TRIAL expiré → EXPIRED ; ACTIVE expirée → EXPIRED. */
    suspend fun statut(now: Long = System.currentTimeMillis()): LicenceStatus {
        val licence = licenceDao.get() ?: return LicenceStatus.TRIAL
        return when (licence.statut) {
            LicenceStatus.TRIAL ->
                if (now > licence.dateDebutEssai + DUREE_ESSAI_MS) LicenceStatus.EXPIRED
                else LicenceStatus.TRIAL
            LicenceStatus.ACTIVE -> {
                val expire = licence.dateExpiration ?: 0L
                if (now > expire) LicenceStatus.EXPIRED else LicenceStatus.ACTIVE
            }
            LicenceStatus.EXPIRED -> LicenceStatus.EXPIRED
        }
    }

    /**
     * RA-05 — licence expirée → **lecture seule** : consultation + export autorisés,
     * toute écriture métier refusée.
     */
    suspend fun isReadOnly(now: Long = System.currentTimeMillis()): Boolean =
        statut(now) == LicenceStatus.EXPIRED

    /** Active la licence avec un code (1 code = 1 appareil / 1 an). */
    suspend fun activer(code: String, now: Long = System.currentTimeMillis()): Boolean {
        if (!CODE_REGEX.matches(code.trim().uppercase())) return false
        val licence = licenceDao.get() ?: LicenceEntity(dateDebutEssai = now, appareilId = appareilId())
        val desassoc = licence.desassociationsUtilisees
        licenceDao.upsert(
            licence.copy(
                statut = LicenceStatus.ACTIVE,
                dateActivation = now,
                dateExpiration = now + DUREE_LICENCE_MS,
                code = code.trim().uppercase(),
                appareilId = appareilId(),
                desassociationsUtilisees = desassoc,
            ),
        )
        journalManager.log(MODULE, "LICENCE_ACTIVEE", "Code activé (appareil ${appareilId()})")
        return true
    }

    /** Désassociation d'appareil : 3/an max (RA-06). */
    suspend fun desassocierAppareil(now: Long = System.currentTimeMillis()): Boolean {
        val licence = licenceDao.get() ?: return false
        if (licence.desassociationsUtilisees >= DESASSOCIATIONS_MAX_PAR_AN) return false
        licenceDao.upsert(
            licence.copy(desassociationsUtilisees = licence.desassociationsUtilisees + 1),
        )
        journalManager.log(MODULE, "LICENCE_DESASSOCIEE", "Désassociation n°${licence.desassociationsUtilisees + 1}")
        return true
    }
}
