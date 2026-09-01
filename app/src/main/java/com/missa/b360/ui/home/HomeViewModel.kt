package com.missa.b360.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseursUseCase
import com.missa.b360.core.domain.usecase.UserAdminUseCases
import com.missa.b360.core.notifications.AppNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Données de synthèse disponibles dès les modules actuellement implémentés. */
data class HomeUiState(
    val entrepriseNom: String = "",
    val devise: String = "XAF",
    val entrepriseLogoUri: String? = null,
    val profilActivite: String? = null,
    val palierTaille: String? = null,
    val prenomUtilisateur: String? = null,
    val nombreClients: Int = 0,
    val nombreFournisseurs: Int = 0,
)

/**
 * ViewModel de l'accueil : badge (RA-23) et résumé réactif de l'entreprise.
 * Les montants Vente/Achat/Trésorerie restent volontairement à zéro tant que leurs
 * modules transactionnels ne sont pas encore livrés ; aucune donnée fictive n'est affichée.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appNotifier: AppNotifier,
    getEnterprise: GetEnterpriseUseCase,
    users: UserAdminUseCases,
    observeClients: ObserveClientsUseCase,
    observeFournisseurs: ObserveFournisseursUseCase,
) : ViewModel() {

    val notificationsNonLues: Flow<Int> = appNotifier.observeNonLues()

    val uiState: StateFlow<HomeUiState> = combine(
        getEnterprise.observer(),
        users.observerUtilisateurs(),
        observeClients(),
        observeFournisseurs(),
    ) { entreprise, utilisateurs, clients, fournisseurs ->
        HomeUiState(
            entrepriseNom = entreprise?.nom.orEmpty(),
            devise = entreprise?.devise ?: "XAF",
            entrepriseLogoUri = entreprise?.logoUri,
            profilActivite = entreprise?.profilActivite,
            palierTaille = entreprise?.palierTaille,
            prenomUtilisateur = utilisateurs.firstOrNull { it.actif }
                ?.nom
                ?.trim()
                ?.substringBefore(' ')
                ?.takeIf { it.isNotBlank() },
            nombreClients = clients.size,
            nombreFournisseurs = fournisseurs.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
