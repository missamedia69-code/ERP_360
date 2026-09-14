package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.journal.JournalManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** D4 / RA-19 — Lecture de l'entreprise (réglages 9.1). */
class GetEnterpriseUseCase @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
) {
    suspend operator fun invoke(): EnterpriseEntity? = enterpriseDao.get()

    /** Permet aux écrans de synthèse de refléter immédiatement toute mise à jour entreprise. */
    fun observer(): Flow<EnterpriseEntity?> = enterpriseDao.observe()
}

/** D4 / RA-19 — Mise à jour des infos entreprise (devise verrouillée). */
class UpdateEnterpriseUseCase @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
    private val settingsStore: SettingsStore,
    private val activationRepository: ProfilActivationRepository,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(
        secteur: String? = null,
        adresse: String? = null,
        telephone: String? = null,
        email: String? = null,
        numeroFiscal: String? = null,
        registreCommerce: String? = null,
        profilActivite: String? = null,
        palierTaille: String? = null,
    ): Boolean {
        val entreprise = enterpriseDao.get() ?: return false

        // Si profil ou palier change, passer par le repository d'activation
        // qui recalcule les modules et éléments effectifs dans toute l'app
        if (profilActivite != null || palierTaille != null) {
            val nouveauProfil = profilActivite?.let { runCatching { ProfilActivite.valueOf(it) }.getOrNull() }
                ?: entreprise.profilActivite?.let { runCatching { ProfilActivite.valueOf(it) }.getOrNull() }
            val nouveauPalier = palierTaille?.let { runCatching { PalierTaille.valueOf(it) }.getOrNull() }
                ?: entreprise.palierTaille?.let { runCatching { PalierTaille.valueOf(it) }.getOrNull() }

            if (nouveauProfil != null) {
                activationRepository.mettreAJourProfil(
                    profil = nouveauProfil,
                    palier = nouveauPalier,
                )
            } else {
                // Pas de profil, juste mettre à jour les champs entreprise
                if (palierTaille != null) {
                    settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, palierTaille)
                }
            }
        }

        enterpriseDao.upsert(
            entreprise.copy(
                // null signifie « ne pas toucher au champ » ; une chaîne vide provenant
                // du formulaire signifie au contraire que l'utilisateur veut l'effacer.
                secteur = texteMisAJour(secteur, entreprise.secteur),
                adresse = texteMisAJour(adresse, entreprise.adresse),
                telephone = texteMisAJour(telephone, entreprise.telephone),
                email = texteMisAJour(email, entreprise.email),
                numeroFiscal = texteMisAJour(numeroFiscal, entreprise.numeroFiscal),
                registreCommerce = texteMisAJour(registreCommerce, entreprise.registreCommerce),
                profilActivite = profilActivite ?: entreprise.profilActivite,
                palierTaille = palierTaille ?: entreprise.palierTaille,
            ),
        )
        journalManager.log("ADMIN", "REGLAGES_MODIFIES", "Informations entreprise mises à jour")
        return true
    }

    private fun texteMisAJour(nouveau: String?, actuel: String?): String? = when (nouveau) {
        null -> actuel
        else -> nouveau.ifBlank { null }
    }
}
