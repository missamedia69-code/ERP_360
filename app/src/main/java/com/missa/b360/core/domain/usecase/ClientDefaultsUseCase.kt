package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.util.Iso4217
import javax.inject.Inject

/** Devise de l'entreprise et pays par défaut (indicatif téléphonique) partagés par les écrans Clients. */
class ClientDefaultsUseCase @Inject constructor(
    private val getEnterprise: GetEnterpriseUseCase,
    private val settingsStore: SettingsStore,
) {
    data class Defaults(
        val devise: String = "",
        val codePays: String? = null,
        val entreprise: EnterpriseEntity? = null,
    )

    suspend operator fun invoke(): Defaults {
        val entreprise = getEnterprise()
        val enregistre = settingsStore.get(SettingsStore.Keys.PAYS)?.takeIf { Iso4217.indicatifTelephone(it) != null }
        return Defaults(
            devise = entreprise?.devise.orEmpty(),
            codePays = enregistre ?: Iso4217.codePaysDepuisNom(entreprise?.pays),
            entreprise = entreprise,
        )
    }
}
