package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.domain.model.ProductionStockLocation
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import javax.inject.Inject

/** Result of the Stock-owned production posting operation. */
sealed interface ProductionStockResult {
    data class Success(val materialCost: Double, val destinationSiteId: Long) : ProductionStockResult
    data object Invalid : ProductionStockResult
    data object ModuleInactive : ProductionStockResult
    data object ReadOnly : ProductionStockResult
    data object FinishedProductNotFound : ProductionStockResult
    data object ComponentNotFound : ProductionStockResult
    data object SiteNotFound : ProductionStockResult
    data object NotStockable : ProductionStockResult
    data object AlreadyPosted : ProductionStockResult
    data class InsufficientStock(val productName: String, val available: Double, val requested: Double) : ProductionStockResult
}

/** Input DTO; Production sends business needs, Stock chooses one or more source sites. */
data class ProductionMaterialNeed(val productId: Long, val quantity: Double)

/**
 * Stock-owned inter-module contract for posting one complete production batch.
 * Call inside the production document transaction. It validates every input
 * before applying any movement, posts all component issues plus the finished
 * goods receipt atomically, and owns quantity and value updates.
 *
 * This first-stage valuation carries only the weighted-average material cost;
 * labour, machine and overhead costs are not yet included.
 */
class ProductionStockUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val siteDao: SiteDao,
    private val groupDao: GroupeArticleDao,
    private val database: AppDatabase,
    private val activation: ProfilActivationRepository,
    private val licence: LicenceManager,
    private val journal: JournalManager,
) {
    private data class PlannedIssue(
        val productId: Long,
        val name: String,
        val siteId: Long,
        val quantity: Double,
        val beforeQuantity: Double,
        val beforeValue: Double,
        val valued: Boolean,
        val unitCost: Double,
    )

    suspend fun postBatch(
        reference: String,
        orderId: Long,
        finishedProductId: Long,
        goodQuantity: Double,
        materials: List<ProductionMaterialNeed>,
        now: Long = System.currentTimeMillis(),
    ): ProductionStockResult {
        if (reference.isBlank() || orderId <= 0 || finishedProductId <= 0 ||
            !goodQuantity.isFinite() || goodQuantity <= 0.0 || materials.isEmpty() ||
            materials.any { it.productId <= 0 || it.productId == finishedProductId || !it.quantity.isFinite() || it.quantity <= 0.0 }
        ) return ProductionStockResult.Invalid
        val currentActivation = activation.getActivation()
        if (currentActivation.modulesActifs.isNotEmpty() && !currentActivation.isModuleActif(ModuleCode.STK)) {
            return ProductionStockResult.ModuleInactive
        }
        if (licence.isReadOnly()) return ProductionStockResult.ReadOnly

        return database.withTransaction {
            val prior = movementDao.getByReference(reference)
            if (prior.any { it.motif == MOTIF_OUTPUT || it.motif == MOTIF_CONSUMPTION }) {
                return@withTransaction ProductionStockResult.AlreadyPosted
            }

            val groups = groupDao.listerComplets()
            val finished = productDao.getById(finishedProductId)
                ?: return@withTransaction ProductionStockResult.FinishedProductNotFound
            if (!finished.active || !ReglesGroupesArticles.estProduisible(finished, groups)) {
                return@withTransaction ProductionStockResult.FinishedProductNotFound
            }
            if (!ReglesGroupesArticles.estStocke(finished, groups)) return@withTransaction ProductionStockResult.NotStockable
            val destinationSiteId = finished.siteId ?: siteDao.idPrincipal()
                ?: return@withTransaction ProductionStockResult.SiteNotFound
            if (siteDao.getNomById(destinationSiteId) == null) return@withTransaction ProductionStockResult.SiteNotFound

            val planned = mutableListOf<PlannedIssue>()
            for (need in materials.groupBy { it.productId }.map { (id, rows) -> ProductionMaterialNeed(id, rows.sumOf { it.quantity }) }.sortedBy { it.productId }) {
                val component = productDao.getById(need.productId)
                    ?: return@withTransaction ProductionStockResult.ComponentNotFound
                if (!component.active || !ProduitRules.estComposant(component.type)) {
                    return@withTransaction ProductionStockResult.ComponentNotFound
                }
                if (!ReglesGroupesArticles.estStocke(component, groups)) return@withTransaction ProductionStockResult.NotStockable
                val valued = ReglesGroupesArticles.estValorise(component, groups)
                val preferredSite = component.siteId
                val locations = stockDao.lignesPourProduit(need.productId)
                if (locations.any { !it.quantite.isFinite() || it.quantite < 0.0 }) {
                    return@withTransaction ProductionStockResult.Invalid
                }
                val available = locations.sumOf { it.quantite }
                if (!available.isFinite()) return@withTransaction ProductionStockResult.Invalid
                if (available < need.quantity) {
                    return@withTransaction ProductionStockResult.InsufficientStock(component.nom, available, need.quantity)
                }
                val allocations = ProductionRules.allocateAcrossSites(
                    locations.map { ProductionStockLocation(it.siteId, it.quantite) },
                    need.quantity,
                    preferredSite,
                ) ?: return@withTransaction ProductionStockResult.InsufficientStock(component.nom, available, need.quantity)
                for (allocation in allocations) {
                    if (siteDao.getNomById(allocation.siteId) == null) return@withTransaction ProductionStockResult.SiteNotFound
                    val rawValue = if (valued) stockDao.valeur(need.productId, allocation.siteId) else 0.0
                    if (!rawValue.isFinite() || rawValue < -EPSILON) return@withTransaction ProductionStockResult.Invalid
                    val value = rawValue.coerceAtLeast(0.0)
                    val unitCost = if (valued && allocation.beforeQuantity > 0.0) value / allocation.beforeQuantity else 0.0
                    planned += PlannedIssue(
                        need.productId,
                        component.nom,
                        allocation.siteId,
                        allocation.quantity,
                        allocation.beforeQuantity,
                        value,
                        valued,
                        unitCost,
                    )
                }
            }

            // All checks precede writes: an insufficient line cannot leave a partial batch.
            val materialCost = planned.sumOf { it.unitCost * it.quantity }
            if (!materialCost.isFinite()) return@withTransaction ProductionStockResult.Invalid
            val beforeOutput = stockDao.quantite(finishedProductId, destinationSiteId) ?: 0.0
            if (!beforeOutput.isFinite() || beforeOutput < 0.0 || !(beforeOutput + goodQuantity).isFinite()) {
                return@withTransaction ProductionStockResult.Invalid
            }
            val outputValueBefore = if (ReglesGroupesArticles.estValorise(finished, groups)) stockDao.valeur(finishedProductId, destinationSiteId) else 0.0
            if (!outputValueBefore.isFinite() || outputValueBefore < -EPSILON || !(outputValueBefore + materialCost).isFinite()) {
                return@withTransaction ProductionStockResult.Invalid
            }
            for (line in planned) {
                stockDao.ensureRow(line.productId, line.siteId)
                stockDao.remplacer(line.productId, line.siteId, (line.beforeQuantity - line.quantity).coerceAtLeast(0.0))
                if (line.valued) {
                    val valueOut = (line.unitCost * line.quantity).coerceIn(0.0, line.beforeValue)
                    stockDao.ajouterValeur(line.productId, line.siteId, -valueOut)
                }
                movementDao.insert(
                    StockMovementEntity(
                        produitId = line.productId,
                        siteId = line.siteId,
                        type = StockMovementType.SORTIE,
                        quantite = line.quantity,
                        motif = MOTIF_CONSUMPTION,
                        reference = reference,
                        commentaire = "$SOURCE_KEY$orderId|${line.productId}|${line.name}",
                        horodatage = now,
                    ),
                )
            }

            stockDao.ensureRow(finishedProductId, destinationSiteId)
            stockDao.remplacer(finishedProductId, destinationSiteId, beforeOutput + goodQuantity)
            if (ReglesGroupesArticles.estValorise(finished, groups)) {
                stockDao.ajouterValeur(finishedProductId, destinationSiteId, materialCost)
            }
            movementDao.insert(
                StockMovementEntity(
                    produitId = finishedProductId,
                    siteId = destinationSiteId,
                    type = StockMovementType.ENTREE,
                    quantite = goodQuantity,
                    motif = MOTIF_OUTPUT,
                    reference = reference,
                    commentaire = "$SOURCE_KEY$orderId|${finished.nom}",
                    horodatage = now,
                ),
            )
            journal.log(
                "STOCK",
                "PRODUCTION_COMPTABILISEE",
                "$reference — ${planned.size} sortie(s), $goodQuantity produit(s), coût matière $materialCost",
                horodatage = now,
            )
            ProductionStockResult.Success(materialCost, destinationSiteId)
        }
    }

    private companion object {
        const val EPSILON = 1e-9
        const val SOURCE_KEY = "PRODUCTION|OF|"
        const val MOTIF_CONSUMPTION = "PRODUCTION_CONSUMPTION"
        const val MOTIF_OUTPUT = "PRODUCTION_OUTPUT"
    }
}
