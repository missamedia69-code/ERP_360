package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject

/**
 * Ordre de production (spec §Production, doc OP).
 *
 * **Brouillon** : pièce seule, aucun effet stock.
 * **Lancement (validation)** : transaction — sortie des composants (contrôle
 * de disponibilité) + entrée du produit fini, mêmes références OP, journal.
 */
class SaveProductionOrderUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val siteDao: SiteDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object ProduitIntrouvable : Result()
        data object ComposantIntrouvable : Result()
        data object SiteIntrouvable : Result()
        data object BrouillonIntrouvable : Result()
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        payload: ProductionRecordPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.produitId <= 0L || payload.quantite <= 0.0 || !payload.quantite.isFinite()) {
            return Result.DonneesInvalides
        }
        if (payload.composants.any { it.quantite <= 0.0 || !it.quantite.isFinite() }) {
            return Result.DonneesInvalides
        }
        if (productDao.getById(payload.produitId) == null) return Result.ProduitIntrouvable

        val titre = "Ordre de production — ${payload.produitNom}"

        if (draft) {
            return when (val id = recordId) {
                null -> {
                    val reference = sequenceManager.next(DocType.ORDRE_PRODUCTION)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.PRODUCTION.name,
                            reference = reference,
                            title = titre,
                            counterpart = payload.produitNom,
                            amount = null,
                            direction = OperationDirection.NONE.name,
                            status = OperationStatus.DRAFT.name,
                            notes = ProductionCodec.encode(payload),
                            createdAt = now,
                        ),
                    )
                    journalManager.log(OperationModule.PRODUCTION.name, "BROUILLON_OP", "Brouillon $reference — ${payload.produitNom}")
                    Result.Succes(newId, reference)
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null || existant.module != OperationModule.PRODUCTION.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(title = titre, counterpart = payload.produitNom, notes = ProductionCodec.encode(payload)),
                    )
                    journalManager.log(OperationModule.PRODUCTION.name, "BROUILLON_OP", "Brouillon ${existant.reference} mis à jour")
                    Result.Succes(id, existant.reference)
                }
            }
        }

        return database.withTransaction {
            val (recordIdFinal, reference) = when (val id = recordId) {
                null -> {
                    val ref = sequenceManager.next(DocType.ORDRE_PRODUCTION)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.PRODUCTION.name,
                            reference = ref,
                            title = titre,
                            counterpart = payload.produitNom,
                            amount = null,
                            direction = OperationDirection.NONE.name,
                            status = OperationStatus.VALIDATED.name,
                            notes = ProductionCodec.encode(payload),
                            createdAt = now,
                        ),
                    )
                    newId to ref
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null || existant.module != OperationModule.PRODUCTION.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return@withTransaction Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            title = titre,
                            counterpart = payload.produitNom,
                            status = OperationStatus.VALIDATED.name,
                            notes = ProductionCodec.encode(payload),
                        ),
                    )
                    id to existant.reference
                }
            }

            // Résolution du site de production (site produit principal).
            val produit = productDao.getById(payload.produitId)
            if (produit == null || !produit.active) return@withTransaction Result.ProduitIntrouvable
            val siteId = produit.siteId
                ?: stockDao.siteAvecPlusDeStock(payload.produitId)
                ?: siteDao.idPrincipal()
                ?: return@withTransaction Result.SiteIntrouvable

            // Sortie des composants (contrôle transactionnel, jamais de stock négatif).
            for ((composantId, besoin) in ProductionRules.besoinsParComposant(payload)) {
                val composant = productDao.getById(composantId)
                    ?: return@withTransaction Result.ComposantIntrouvable
                if (!composant.active) return@withTransaction Result.ComposantIntrouvable
                val compSite = composant.siteId
                    ?: stockDao.siteAvecPlusDeStock(composantId)
                    ?: siteDao.idPrincipal()
                    ?: return@withTransaction Result.SiteIntrouvable
                val disponible = stockDao.quantite(composantId, compSite) ?: 0.0
                if (disponible < besoin - QUANTITE_EPSILON) {
                    return@withTransaction Result.StockInsuffisant(composant.nom, disponible, besoin)
                }
                stockDao.ensureRow(composantId, compSite)
                stockDao.remplacer(ProductStockEntity(composantId, compSite, disponible - besoin))
                movementDao.insert(
                    StockMovementEntity(
                        produitId = composantId,
                        siteId = compSite,
                        type = StockMovementType.SORTIE,
                        quantite = besoin,
                        motif = "PRODUCTION",
                        reference = reference,
                        horodatage = now,
                    ),
                )
            }

            // Entrée du produit fini.
            val avant = stockDao.quantite(payload.produitId, siteId) ?: 0.0
            stockDao.ensureRow(payload.produitId, siteId)
            stockDao.remplacer(ProductStockEntity(payload.produitId, siteId, avant + payload.quantite))
            movementDao.insert(
                StockMovementEntity(
                    produitId = payload.produitId,
                    siteId = siteId,
                    type = StockMovementType.ENTREE,
                    quantite = payload.quantite,
                    motif = "PRODUCTION",
                    reference = reference,
                    horodatage = now,
                ),
            )

            journalManager.log(
                OperationModule.PRODUCTION.name,
                "OP_LANCE",
                "$reference — ${payload.produitNom} × ${payload.quantite} " +
                    "(${payload.composants.size} composant(s))",
            )
            Result.Succes(recordIdFinal, reference)
        }
    }
}
