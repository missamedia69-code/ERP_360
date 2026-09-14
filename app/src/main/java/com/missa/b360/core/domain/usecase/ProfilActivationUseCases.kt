package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observe l'activation effective du profil dans toute l'application.
 * C'est le UseCase central qui doit être utilisé partout où l'on a besoin
 * de savoir quels modules et éléments sont actifs.
 */
class ObserveActivationProfilUseCase @Inject constructor(
    private val repository: ProfilActivationRepository,
) {
    operator fun invoke(): Flow<ActivationProfil> = repository.observeActivation()
}

/**
 * Récupère l'activation actuelle de manière synchrone
 */
class GetActivationProfilUseCase @Inject constructor(
    private val repository: ProfilActivationRepository,
) {
    suspend operator fun invoke(): ActivationProfil = repository.getActivation()
}

/**
 * Met à jour le profil et recalcule les modules/éléments actifs
 */
class MettreAJourActivationProfilUseCase @Inject constructor(
    private val repository: ProfilActivationRepository,
) {
    suspend operator fun invoke(
        profil: ProfilActivite,
        palier: PalierTaille? = null,
        venteSansStock: Boolean? = null,
        modulesPersonnalises: Set<ModuleCode>? = null,
        extrasSupport: Set<ModuleCode>? = null,
        elementsPersonnalises: Map<ModuleCode, Set<String>>? = null,
    ) {
        repository.mettreAJourProfil(
            profil = profil,
            palier = palier,
            venteSansStock = venteSansStock,
            modulesPersonnalises = modulesPersonnalises,
            extrasSupport = extrasSupport,
            elementsPersonnalises = elementsPersonnalises,
        )
    }
}

/**
 * Vérifie si un module est actif (pour les guards dans toute l'app)
 */
class IsModuleActifUseCase @Inject constructor(
    private val repository: ProfilActivationRepository,
) {
    suspend fun isActif(module: ModuleCode): Boolean {
        val activation = repository.getActivation()
        return activation.isModuleActif(module)
    }

    fun observeIsActif(module: ModuleCode): Flow<Boolean> =
        repository.observeActivation().let { flow ->
            kotlinx.coroutines.flow.map(flow) { it.isModuleActif(module) }
        }
}

/**
 * Vérifie si un élément (fonction) d'un module est actif
 */
class IsElementActifUseCase @Inject constructor(
    private val repository: ProfilActivationRepository,
) {
    suspend fun isActif(module: ModuleCode, element: String): Boolean {
        val activation = repository.getActivation()
        return activation.isElementActif(module, element)
    }
}
