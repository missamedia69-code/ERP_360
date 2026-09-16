package com.missa.b360.core.data.repository

import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.model.StockHub
import com.missa.b360.core.domain.model.StockHubRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StockHubRepository — agrège les DAO utilisés par l'écran hub Stock.
 * Évite l'injection directe de 5 DAO dans le ViewModel (défaut d'architecture v4).
 */
@Singleton
class StockHubRepository @Inject constructor(
    private val siteDao: SiteDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val groupeDao: GroupeArticleDao,
) {
    fun observeSites(): Flow<List<SiteEntity>> = siteDao.observeAll()

    fun observeGroupes(): Flow<List<GroupeArticleEntity>> =
        groupeDao.observerComplets().map { complets -> complets.map { it.groupe } }



    fun observeStockHub(
        depotId: Flow<Long?>,
        limiteMouvements: Int = 300,
    ): Flow<StockHub> {
        val sources = combine(
            productDao.observeAll(),
            stockDao.observeToutes(),
            movementDao.observeJoints(limiteMouvements),
            observeGroupes(),
        ) { produits, stocks, mouvements, groupes ->
            StockSources(produits, stocks, mouvements, groupes)
        }
        return combine(
            sources,
            depotId,
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
        }
    }

    private data class StockSources(
        val produits: List<ProductEntity>,
        val stocks: List<ProductStockEntity>,
        val mouvements: List<StockMovementView>,
        val groupes: List<GroupeArticleEntity>,
    )
}
