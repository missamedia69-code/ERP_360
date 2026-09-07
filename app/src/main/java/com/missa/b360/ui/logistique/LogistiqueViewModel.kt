package com.missa.b360.ui.logistique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.domain.model.LogistiqueRules
import com.missa.b360.core.domain.model.StockDuSite
import com.missa.b360.core.domain.model.SuiviLivraisons
import com.missa.b360.core.domain.model.Transfert
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * État de l'écran Logistique : implantation du stock par site, transferts en
 * cours et suivi des livraisons. Vue transverse uniquement — la saisie reste
 * dans les modules Stock et Ventes.
 */
@HiltViewModel
class LogistiqueViewModel @Inject constructor(
    siteDao: SiteDao,
    stockDao: ProductStockDao,
    productDao: ProductDao,
    movementDao: StockMovementDao,
    operationDao: OperationRecordDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatLogistique(
        val sites: List<StockDuSite> = emptyList(),
        val transferts: List<Transfert> = emptyList(),
        val enTransit: Int = 0,
        val livraisons: SuiviLivraisons = SuiviLivraisons(),
        val valeurTotale: Double = 0.0,
        val concentration: Double = 0.0,
    )

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatLogistique> = combine(
        siteDao.observeAll(),
        stockDao.observeToutes(),
        productDao.observeAll(),
        movementDao.observeRecent(LIMITE_MOUVEMENTS),
        operationDao.observeAll(),
    ) { sites, stocks, produits, mouvements, pieces ->
        val parSite = LogistiqueRules.stockParSite(sites, stocks, produits)
        val transferts = LogistiqueRules.transferts(mouvements)
        EtatLogistique(
            sites = parSite,
            transferts = transferts,
            enTransit = LogistiqueRules.enTransit(transferts).size,
            livraisons = LogistiqueRules.suiviLivraisons(pieces),
            valeurTotale = parSite.sumOf { it.valeur },
            concentration = LogistiqueRules.concentration(parSite),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatLogistique())

    private companion object {
        /** Fenêtre de mouvements relus : au-delà, l'historique appartient au module Stock. */
        const val LIMITE_MOUVEMENTS = 300
    }
}
