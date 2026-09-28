package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import com.missa.b360.core.permissions.PermissionChecker
import javax.inject.Inject

/**
 * Production document use case. All physical quantity/value changes are delegated
 * to Stock's production posting contract; this class never injects a Stock DAO.
 */
class SaveProductionOrderUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val groupeArticleDao: GroupeArticleDao,
    private val stockPosting: ProductionStockUseCase,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val activationRepository: ProfilActivationRepository,
    private val settings: SettingsStore,
    private val userDao: UserDao,
    private val permissions: PermissionChecker,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String, val materialCost: Double = 0.0) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object ProduitIntrouvable : Result()
        data object ComposantIntrouvable : Result()
        data object SiteIntrouvable : Result()
        data object BrouillonIntrouvable : Result()
        data object NonAutorise : Result()
        data object ModuleInactif : Result()
        data object MouvementDejaEnregistre : Result()
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        payload: ProductionRecordPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val permission = when {
            draft && recordId == null -> PermissionChecker.Action.CREATE
            draft -> PermissionChecker.Action.EDIT
            else -> PermissionChecker.Action.VALIDATE
        }
        val actor = actor(permission) ?: return Result.NonAutorise
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (!moduleActif()) return Result.ModuleInactif
        if (!ProductionRules.payloadIsValid(payload)) return Result.DonneesInvalides

        val groups = groupeArticleDao.listerComplets()
        val finished = productDao.getById(payload.produitId)
        if (finished == null || !finished.active || !ReglesGroupesArticles.estProduisible(finished, groups)) return Result.ProduitIntrouvable
        val canonicalComponents = mutableListOf<ProductionComponent>()
        for (component in payload.composants) {
            val product = productDao.getById(component.productId)
                ?: return Result.ComposantIntrouvable
            if (!product.active || !ProduitRules.estComposant(product.type)) return Result.ComposantIntrouvable
            canonicalComponents += component.copy(nom = product.nom)
        }
        val normalized = payload.copy(produitNom = finished.nom, composants = canonicalComponents)
        val title = "Ordre de production — ${finished.nom}"

        if (draft) {
            return saveDraft(recordId, normalized, title, actor, now)
        }

        val oldDraft = recordId?.let { operationDao.getById(it) }
        if (recordId != null && (oldDraft == null || oldDraft.module != OperationModule.PRODUCTION.name || oldDraft.status != OperationStatus.DRAFT.name)) {
            return Result.BrouillonIntrouvable
        }
        val reference = oldDraft?.reference ?: sequenceManager.next(DocType.ORDRE_PRODUCTION)
        return try {
            database.withTransaction {
                val currentDraft = if (recordId != null) operationDao.getById(recordId) else null
                if (recordId != null && (currentDraft == null || currentDraft.module != OperationModule.PRODUCTION.name || currentDraft.status != OperationStatus.DRAFT.name)) {
                    return@withTransaction Result.BrouillonIntrouvable
                }
                val productionRecordId = currentDraft?.id ?: operationDao.insert(
                    OperationRecordEntity(
                        module = OperationModule.PRODUCTION.name,
                        reference = reference,
                        title = title,
                        counterpart = finished.nom,
                        amount = null,
                        quantity = normalized.quantite,
                        direction = OperationDirection.NONE.name,
                        status = OperationStatus.DRAFT.name,
                        notes = ProductionCodec.encode(normalized),
                        createdAt = now,
                    ),
                )
                val stockResult = stockPosting.postBatch(
                    reference = reference,
                    orderId = productionRecordId,
                    finishedProductId = normalized.produitId,
                    goodQuantity = normalized.quantite,
                    materials = ProductionRules.besoinsParComposant(normalized).map { (id, quantity) ->
                        ProductionMaterialNeed(id, quantity)
                    },
                    now = now,
                )
                when (stockResult) {
                    is ProductionStockResult.Success -> {
                        val order = currentDraft ?: operationDao.getById(productionRecordId)
                            ?: throw IllegalStateException("Production document disappeared in transaction")
                        operationDao.update(
                            order.copy(
                                title = title,
                                counterpart = finished.nom,
                                amount = null,
                                quantity = normalized.quantite,
                                status = OperationStatus.VALIDATED.name,
                                notes = ProductionCodec.encode(normalized.copy(
                                    coutMatieres = stockResult.materialCost,
                                    siteDestinationId = stockResult.destinationSiteId,
                                )),
                            ),
                        )
                        journalManager.log(
                            OperationModule.PRODUCTION.name,
                            "OF_TERMINE",
                            "$reference — ${finished.nom} × ${normalized.quantite}; coût matière ${stockResult.materialCost}",
                            actor,
                            now,
                        )
                        Result.Succes(productionRecordId, reference, stockResult.materialCost)
                    }
                    else -> throw ProductionStockFailure(stockResult)
                }
            }
        } catch (failure: ProductionStockFailure) {
            when (val stock = failure.result) {
                is ProductionStockResult.InsufficientStock -> Result.StockInsuffisant(stock.productName, stock.available, stock.requested)
                ProductionStockResult.AlreadyPosted -> Result.MouvementDejaEnregistre
                ProductionStockResult.ModuleInactive -> Result.ModuleInactif
                ProductionStockResult.ReadOnly -> Result.LectureSeule
                ProductionStockResult.FinishedProductNotFound -> Result.ProduitIntrouvable
                ProductionStockResult.ComponentNotFound -> Result.ComposantIntrouvable
                ProductionStockResult.SiteNotFound -> Result.SiteIntrouvable
                ProductionStockResult.Invalid, ProductionStockResult.NotStockable -> Result.DonneesInvalides
                is ProductionStockResult.Success -> Result.DonneesInvalides
            }
        }
    }

    private suspend fun saveDraft(
        recordId: Long?,
        payload: ProductionRecordPayload,
        title: String,
        actor: Long,
        now: Long,
    ): Result {
        return when (recordId) {
            null -> {
                val reference = sequenceManager.next(DocType.ORDRE_PRODUCTION)
                database.withTransaction {
                    val id = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.PRODUCTION.name,
                            reference = reference,
                            title = title,
                            counterpart = payload.produitNom,
                            quantity = payload.quantite,
                            direction = OperationDirection.NONE.name,
                            status = OperationStatus.DRAFT.name,
                            notes = ProductionCodec.encode(payload),
                            createdAt = now,
                        ),
                    )
                    journalManager.log(OperationModule.PRODUCTION.name, "BROUILLON_OF_CREE", "$reference — ${payload.produitNom}", actor, now)
                    Result.Succes(id, reference)
                }
            }
            else -> database.withTransaction {
                val existing = operationDao.getById(recordId)
                if (existing == null || existing.module != OperationModule.PRODUCTION.name || existing.status != OperationStatus.DRAFT.name) {
                    return@withTransaction Result.BrouillonIntrouvable
                }
                operationDao.update(existing.copy(title = title, counterpart = payload.produitNom, quantity = payload.quantite, notes = ProductionCodec.encode(payload)))
                journalManager.log(OperationModule.PRODUCTION.name, "BROUILLON_OF_MODIFIE", existing.reference, actor, now)
                Result.Succes(recordId, existing.reference)
            }
        }
    }

    private suspend fun actor(action: PermissionChecker.Action): Long? {
        val id = settings.getLong(SettingsStore.Keys.CURRENT_USER_ID) ?: return null
        val user = userDao.getById(id) ?: return null
        if (!user.actif || !permissions.hasPermission(user.roleId, MODULE, action)) return null
        return id
    }

    private suspend fun moduleActif(): Boolean {
        val active = activationRepository.getActivation().modulesActifs
        return active.isEmpty() || ModuleCode.PRO in active
    }

    private class ProductionStockFailure(val result: ProductionStockResult) : RuntimeException(null, null, false, false)

    private companion object { const val MODULE = "PRODUCTION" }
}
