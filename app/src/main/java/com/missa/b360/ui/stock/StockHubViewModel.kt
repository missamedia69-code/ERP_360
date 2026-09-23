package com.missa.b360.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.repository.StockHubRepository
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État du hub Stock.
 *
 * Le dépôt sélectionné filtre tout l'écran : indicateurs, alertes, groupes et
 * raccourcis. Tant que la gestion des dépôts et emplacements n'est pas livrée,
 * ce sont les sites de l'entreprise qui jouent ce rôle — la notion est la même
 * vue de l'utilisateur, et la bascule se fera sans changer cet écran.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StockHubViewModel @Inject constructor(
    private val stockHubRepository: StockHubRepository,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    /** Dépôt retenu ; null = tous les dépôts confondus. */
    private val _depotChoisi = MutableStateFlow<Long?>(null)
    val depotChoisi: StateFlow<Long?> = _depotChoisi

    /** Passe à true le temps d'un rechargement demandé par l'utilisateur. */
    private val _rechargement = MutableStateFlow(false)
    val rechargement: StateFlow<Boolean> = _rechargement

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val depots: StateFlow<List<SiteEntity>> = stockHubRepository.observeSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val etat = stockHubRepository.observeStockHub(
        depotId = _depotChoisi,
        limiteMouvements = LIMITE_MOUVEMENTS,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.missa.b360.core.domain.model.StockHub())

    fun choisirDepot(siteId: Long?) {
        _depotChoisi.value = siteId
    }

    /**
     * Les flux Room se rafraîchissent d'eux-mêmes ; ce geste sert à le montrer
     * à l'utilisateur, qui attend un retour visible après avoir appuyé.
     */
    fun recharger() {
        if (_rechargement.value) return
        viewModelScope.launch {
            _rechargement.value = true
            kotlinx.coroutines.delay(DUREE_RETOUR_VISUEL)
            _rechargement.value = false
        }
    }

    /** Libellé du dépôt courant, pour le sélecteur. */
    fun nomDepot(sites: List<SiteEntity>, id: Long?): String? =
        id?.let { choisi -> sites.firstOrNull { it.id == choisi }?.nom }

    private companion object {
        /** Fenêtre de mouvements relus : au-delà, c'est l'écran Mouvements qui prend le relais. */
        const val LIMITE_MOUVEMENTS = 300
        const val DUREE_RETOUR_VISUEL = 450L
    }
}
