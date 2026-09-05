package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleStockEffects
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject
import kotlin.math.abs

/**
 * Persistance **transactionnelle** d'une vente (spec §44) :
 *
 * 1. recalcul des totaux ;
 * 2. relecture des stocks au moment de l'enregistrement ;
 * 3. vérification de la disponibilité par produit ;
 * 4. création (ou validation d'un brouillon) de la pièce Vente ;
 * 5. mouvements de sortie de stock + mise à jour des lignes de stock ;
 * 6. journal d'audit.
 *
 * Le tout dans une seule transaction Room : il est impossible d'obtenir une vente
 * enregistrée sans ses mouvements, ou des mouvements sans leur vente.
 * Un **brouillon** n'a aucun effet sur le stock ni sur la finance (spec §3 BROUILLON).
 */
class SaveSaleUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object BrouillonIntrouvable : Result()
        /** Stock insuffisant re-lu transactionnellement (§43/§44). */
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        payload: SaleRecordPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule

        // Recalcul des totaux (spec §44) — l'écran ne fait pas foi.
        val totals = SaleCalculator.calculate(
            lines = payload.lines,
            discount = payload.discount,
            delivery = payload.delivery,
            taxRate = payload.taxRate,
        )
        if (payload.lines.isEmpty() || totals.total <= 0.0) return Result.DonneesInvalides
        if (abs(totals.total - payload.total) > 0.01) return Result.DonneesInvalides
        if (payload.paidAmount < -QUANTITE_EPSILON || payload.paidAmount > totals.total + QUANTITE_EPSILON) {
            return Result.DonneesInvalides
        }
        val detail = SaleRecordCodec.encode(
            payload.copy(
                subtotal = totals.subtotal,
                discount = totals.discount,
                delivery = totals.delivery,
                taxAmount = totals.taxAmount,
                total = totals.total,
            ),
        )

        if (draft) {
            // Brouillon : pièce seule, aucun mouvement de stock, aucun paiement (spec §3).
            return when (val id = recordId) {
                null -> {
                    val reference = sequenceManager.next(DocType.FACTURE)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.VENTE.name,
                            reference = reference,
                            title = payload.clientName,
                            counterpart = payload.clientName,
                            amount = totals.total,
                            status = OperationStatus.DRAFT.name,
                            notes = detail,
                            createdAt = now,
                        ),
                    )
                    journalManager.log("VENTE", "BROUILLON_VENTE", "Brouillon $reference — ${payload.clientName}")
                    Result.Succes(newId, reference)
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null ||
                        existant.module != OperationModule.VENTE.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            title = payload.clientName,
                            counterpart = payload.clientName,
                            amount = totals.total,
                            notes = detail,
                        ),
                    )
                    journalManager.log("VENTE", "BROUILLON_VENTE", "Brouillon ${existant.reference} mis à jour")
                    Result.Succes(id, existant.reference)
                }
            }
        }

        // Vente validée : toutes les vérifications de lecture précèdent toute écriture,
        // afin qu'un échec ne laisse aucun état partiel (§44).
        return database.withTransaction {
            val besoins = SaleStockEffects.besoinsParProduit(payload.lines)
            val sorties = mutableListOf<Triple<Long, Long, Double>>() // produit, site de sortie, quantité
            for ((produitId, demande) in besoins) {
                val produit = productDao.getById(produitId)
                if (produit == null || !produit.active) return@withTransaction Result.DonneesInvalides
                val siteId = produit.siteId
                    ?: stockDao.siteAvecPlusDeStock(produitId)
                    ?: return@withTransaction Result.StockInsuffisant(produit.nom, 0.0, demande)
                val disponible = stockDao.quantite(produitId, siteId) ?: 0.0
                if (disponible < demande - QUANTITE_EPSILON) {
                    return@withTransaction Result.StockInsuffisant(produit.nom, disponible, demande)
                }
                sorties += Triple(produitId, siteId, demande)
            }

            val (recordIdFinal, reference) = when (val id = recordId) {
                null -> {
                    val ref = sequenceManager.next(DocType.FACTURE)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.VENTE.name,
                            reference = ref,
                            title = payload.clientName,
                            counterpart = payload.clientName,
                            amount = totals.total,
                            status = OperationStatus.VALIDATED.name,
                            notes = detail,
                            createdAt = now,
                        ),
                    )
                    newId to ref
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null ||
                        existant.module != OperationModule.VENTE.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return@withTransaction Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            title = payload.clientName,
                            counterpart = payload.clientName,
                            amount = totals.total,
                            status = OperationStatus.VALIDATED.name,
                            notes = detail,
                        ),
                    )
                    id to existant.reference
                }
            }

            for ((produitId, siteId, demande) in sorties) {
                // Relecture juste avant commit (§43) — entre la vérification et le commit,
                // rien d'autre ne s'exécute dans la même transaction, mais on re-lit
                // pour rester cohérent avec la règle du cahier de charges.
                val avant = stockDao.quantite(produitId, siteId) ?: 0.0
                val apres = (avant - demande).coerceAtLeast(0.0)
                stockDao.ensureRow(produitId, siteId)
                stockDao.remplacer(ProductStockEntity(produitId, siteId, apres))
                movementDao.insert(
                    StockMovementEntity(
                        produitId = produitId,
                        siteId = siteId,
                        type = StockMovementType.SORTIE,
                        quantite = demande,
                        motif = "VENTE",
                        reference = reference,
                        horodatage = now,
                    ),
                )
            }

            journalManager.log(
                "VENTE",
                "VENTE_VALIDEE",
                "Vente $reference — ${payload.clientName} (${totals.total} ${if (payload.paidAmount >= totals.total) "réglée" else "partiellement réglée"})",
            )
            Result.Succes(recordIdFinal, reference)
        }
    }
}

/**
 * Annulation d'une vente validée (spec §3 ANNULATION / C7) — **compensation**,
 * jamais de suppression : la pièce passe au statut ANNULÉ et le stock sortant est
 * recomposé par des mouvements d'entrée portant la même référence.
 */
class ReverseSaleStockUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
        data object DejaAnnulee : Result()
        /** Une vente brouillon ne s'annule pas : on la laisse en brouillon ou on la valide. */
        data object Brouillon : Result()
    }

    suspend operator fun invoke(recordId: Long, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        return database.withTransaction {
            val record = operationDao.getById(recordId)
            if (record == null || record.module != OperationModule.VENTE.name) {
                return@withTransaction Result.Introuvable
            }
            when (record.status) {
                OperationStatus.CANCELLED.name -> return@withTransaction Result.DejaAnnulee
                OperationStatus.DRAFT.name -> return@withTransaction Result.Brouillon
                else -> Unit
            }
            val payload = SaleRecordCodec.decode(record.notes)
            if (payload != null) {
                for ((produitId, quantite) in SaleStockEffects.besoinsParProduit(payload.lines)) {
                    val produit = productDao.getById(produitId) ?: continue
                    val siteId = produit.siteId
                        ?: stockDao.siteAvecPlusDeStock(produitId)
                        ?: continue
                    val avant = stockDao.quantite(produitId, siteId) ?: 0.0
                    stockDao.ensureRow(produitId, siteId)
                    stockDao.remplacer(ProductStockEntity(produitId, siteId, avant + quantite))
                    movementDao.insert(
                        StockMovementEntity(
                            produitId = produitId,
                            siteId = siteId,
                            type = StockMovementType.ENTREE,
                            quantite = quantite,
                            motif = "ANNULATION_VENTE",
                            reference = record.reference,
                            horodatage = now,
                        ),
                    )
                }
            }
            operationDao.update(record.copy(status = OperationStatus.CANCELLED.name))
            journalManager.log(
                "VENTE",
                "ANNULATION_VENTE",
                "Vente ${record.reference} annulée — stock recomposé par compensation",
            )
            Result.Succes
        }
    }
}

/**
 * Contrôle de disponibilité **UI** (avant ouverture de la transaction) : permet
 * d'afficher « Stock insuffisant pour X » sans épuiser la base. Le contrôle
 * transactionnel de [SaveSaleUseCase] reste l'autorité finale (§44).
 */
class CheckSaleStockUseCase @Inject constructor(
    private val stockDao: ProductStockDao,
    private val productDao: ProductDao,
) {
    data class Verdict(val produitNom: String, val disponible: Double, val demande: Double)

    /** @return le premier produit dont le stock serait insuffisant, ou null si tout est disponible. */
    suspend fun premierDeficit(payload: com.missa.b360.core.domain.model.SaleRecordPayload): Verdict? {
        for ((produitId, demande) in SaleStockEffects.besoinsParProduit(payload.lines)) {
            val produit = productDao.getById(produitId) ?: continue
            val siteId = produit.siteId ?: stockDao.siteAvecPlusDeStock(produitId) ?: return Verdict(produit.nom, 0.0, demande)
            val disponible = stockDao.quantite(produitId, siteId) ?: 0.0
            if (disponible < demande - QUANTITE_EPSILON) return Verdict(produit.nom, disponible, demande)
        }
        return null
    }
}
