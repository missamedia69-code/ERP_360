package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.journal.JournalManager
import javax.inject.Inject

/** D4 / RA-19 — Lecture de l'entreprise (réglages 9.1). */
class GetEnterpriseUseCase @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
) {
    suspend operator fun invoke(): EnterpriseEntity? = enterpriseDao.get()
}

/** D4 / RA-19 — Mise à jour des infos entreprise (devise verrouillée). */
class UpdateEnterpriseUseCase @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
    private val settingsStore: SettingsStore,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(
        secteur: String? = null,
        adresse: String? = null,
        telephone: String? = null,
        email: String? = null,
        profilActivite: String? = null,
        palierTaille: String? = null,
    ): Boolean {
        val entreprise = enterpriseDao.get() ?: return false
        enterpriseDao.upsert(
            entreprise.copy(
                secteur = secteur,
                adresse = adresse,
                telephone = telephone,
                email = email,
                profilActivite = profilActivite,
                palierTaille = palierTaille,
            ),
        )
        profilActivite?.let { settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, it) }
        palierTaille?.let { settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, it) }
        journalManager.log("ADMIN", "REGLAGES_MODIFIES", "Informations entreprise mises à jour")
        return true
    }
}