package com.missa.b360.core.data.repository

import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.NonConformiteDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.domain.model.TresorerieRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HomeRepository — agrège les DAO utilisés par l'accueil.
 *
 * Objectif revue : HomeViewModel ne doit pas dépendre directement des DAO,
 * mais d'un repository testable. Centralise aussi la logique de solde trésorerie.
 */
@Singleton
class HomeRepository @Inject constructor(
    private val compteTresorerieDao: CompteTresorerieDao,
    private val mouvementTresorerieDao: MouvementTresorerieDao,
    private val productDao: ProductDao,
    private val productStockDao: ProductStockDao,
    private val stockMovementDao: StockMovementDao,
    private val nonConformiteDao: NonConformiteDao,
    private val taskDao: TaskDao,
) {
    fun observeTaches(): Flow<List<TaskEntity>> = taskDao.observeAll()

    fun observeStockQuantites(): Flow<List<ProductStockEntity>> = productStockDao.observeToutes()

    fun observeStockMouvements(): Flow<List<StockMovementEntity>> = stockMovementDao.observeRecent(500)

    fun observeProduits(): Flow<List<ProductEntity>> = productDao.observeAll()

    fun observeNonConformites(): Flow<List<NonConformiteEntity>> = nonConformiteDao.observeAll()

    fun observeSoldeTresorerie(): Flow<Double> = combine(
        compteTresorerieDao.observeAll(),
        mouvementTresorerieDao.observeAll(),
    ) { comptes, mouvements -> TresorerieRules.soldeGlobal(comptes, mouvements) }
}
