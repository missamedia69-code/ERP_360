package com.missa.b360.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.model.StockHub
import com.missa.b360.core.domain.model.StockHubRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val siteDao: SiteDao,
    productDao: ProductDao,
    stockDao: ProductStockDao,
    movementDao: StockMovementDao,
    groupeDao: GroupeArticleDao,
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

    val depots: StateFlow<List<SiteEntity>> = siteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val groupes: StateFlow<List<GroupeArticleEntity>> = groupeDao.observerComplets()
        .map { complets -> complets.map { it.groupe } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sources = combine(
        productDao.observeAll(),
        stockDao.observeToutes(),
        movementDao.observeJoints(LIMITE_MOUVEMENTS),
        groupes,
    ) { produits, stocks, mouvements, listes ->
        StockSources(produits, stocks, mouvements, listes)
    }

    val etat: StateFlow<StockHub> = combine(
        sources,
        _depotChoisi,
        siteDao.observeAll(),
    ) { src, depot, sites ->
        StockHubRules.construire(
            produits = src.produits,
            stocks = src.stocks,
            mouvements = src.mouvements,
            groupes = src.groupes,
            sites = sites,
            depotId = depot,
            maintenant = System.currentTimeMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockHub())

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

    private data class StockSources(
        val produits: List<ProductEntity>,
        val stocks: List<ProductStockEntity>,
        val mouvements: List<com.missa.b360.core.data.dao.StockMovementView>,
        val groupes: List<GroupeArticleEntity>,
    )

    private companion object {
        /** Fenêtre de mouvements relus : au-delà, c'est l'écran Mouvements qui prend le relais. */
        const val LIMITE_MOUVEMENTS = 300
        const val DUREE_RETOUR_VISUEL = 450L
    }
}
