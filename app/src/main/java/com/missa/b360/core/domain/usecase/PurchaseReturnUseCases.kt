package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.FournisseurDao
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
import com.missa.b360.core.domain.model.InventoryRules
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.PurchaseStockEffects
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.ReturnRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject
import kotlin.math.abs

/**
 * Persistance **transactionnelle** d'une facture fournisseur (spec §6 ACHATS) :
 *
 * 1. vérification fournisseur et cohérence des montants ;
 * 2. création (ou validation d'un brouillon) de la pièce Achat ;
 * 3. à la validation : **mouvements d'entrée de stock** par produit rattaché
 *    (réception = le passif fournisseur est `total − paidAmount`, sans écriture
 *    de caisse — le règlement ultérieur est une opération Finance) ;
 * 4. journal d'audit.
 *
 * Un brouillon n'a aucun effet sur le stock (spec §3 BROUILLON).
 */
class SavePurchaseUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val fournisseurDao: FournisseurDao,
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
        data object FournisseurIntrouvable : Result()
        data object BrouillonIntrouvable : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        payload: PurchaseRecordPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lines.isEmpty() || payload.total <= 0.0) return Result.DonneesInvalides
        val subtotal = payload.lines.sumOf { it.total }.coerceAtLeast(0.0)
        if (abs(subtotal - payload.total) > 0.01) return Result.DonneesInvalides
        if (payload.paidAmount < -QUANTITE_EPSILON || payload.paidAmount > payload.total + QUANTITE_EPSILON) {
            return Result.DonneesInvalides
        }
        if (fournisseurDao.getById(payload.supplierId) == null) return Result.FournisseurIntrouvable

        if (draft) {
            // Brouillon : pièce seule, aucune entrée de stock (spec §3).
            return when (val id = recordId) {
                null -> {
                    val reference = sequenceManager.next(DocType.FACTURE_FOURNISSEUR)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.ACHATS.name,
                            reference = reference,
                            title = "Facture fournisseur — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = payload.total,
                            status = OperationStatus.DRAFT.name,
                            notes = PurchaseRecordCodec.encode(payload),
                            createdAt = now,
                        ),
                    )
                    journalManager.log("ACHATS", "BROUILLON_ACHAT", "Brouillon $reference — ${payload.supplierName}")
                    Result.Succes(newId, reference)
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null || existant.module != OperationModule.ACHATS.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            title = "Facture fournisseur — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = payload.total,
                            notes = PurchaseRecordCodec.encode(payload),
                        ),
                    )
                    journalManager.log("ACHATS", "BROUILLON_ACHAT", "Brouillon ${existant.reference} mis à jour")
                    Result.Succes(id, existant.reference)
                }
            }
        }

        return database.withTransaction {
            // Résolution du site de réception : site principal produit, sinon le site
            // qui détient déjà ce produit, sinon le site principal de l'entreprise.
            val receptions = mutableListOf<Triple<Long, Long, Double>>() // produit, site, quantité
            for ((produitId, quantite) in PurchaseStockEffects.besoinsParProduit(payload.lines)) {
                val produit = productDao.getById(produitId)
                if (produit == null || !produit.active) return@withTransaction Result.DonneesInvalides
                val siteId = produit.siteId
                    ?: stockDao.siteAvecPlusDeStock(produitId)
                    ?: siteDao.idPrincipal()
                    ?: return@withTransaction Result.FournisseurIntrouvable
                receptions += Triple(produitId, siteId, quantite)
            }

            val (recordIdFinal, reference) = when (val id = recordId) {
                null -> {
                    val ref = sequenceManager.next(DocType.FACTURE_FOURNISSEUR)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.ACHATS.name,
                            reference = ref,
                            title = "Facture fournisseur — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = payload.total,
                            direction = OperationDirection.NONE.name,
                            status = OperationStatus.VALIDATED.name,
                            notes = PurchaseRecordCodec.encode(payload),
                            createdAt = now,
                        ),
                    )
                    newId to ref
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null || existant.module != OperationModule.ACHATS.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) {
                        return@withTransaction Result.BrouillonIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            title = "Facture fournisseur — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = payload.total,
                            status = OperationStatus.VALIDATED.name,
                            notes = PurchaseRecordCodec.encode(payload),
                        ),
                    )
                    id to existant.reference
                }
            }

            for ((produitId, siteId, quantite) in receptions) {
                val avant = stockDao.quantite(produitId, siteId) ?: 0.0
                val apres = avant + quantite
                stockDao.ensureRow(produitId, siteId)
                stockDao.remplacer(ProductStockEntity(produitId, siteId, apres))
                movementDao.insert(
                    StockMovementEntity(
                        produitId = produitId,
                        siteId = siteId,
                        type = StockMovementType.ENTREE,
                        quantite = quantite,
                        motif = "ACHAT",
                        reference = reference,
                        horodatage = now,
                    ),
                )
            }

            val passif = (payload.total - payload.paidAmount).coerceAtLeast(0.0)
            journalManager.log(
                "ACHATS",
                "ACHAT_VALIDATE",
                "Achat $reference — ${payload.supplierName} (${payload.total} réglé ${payload.paidAmount}, passif $passif)",
            )
            Result.Succes(recordIdFinal, reference)
        }
    }
}

/**
 * Retour de vente (spec §22) — **transactionnel** :
 *
 * 1. relecture de la facture d'origine et des avoirs précédents ;
 * 2. vérification : quantité retournée ≤ quantité encore disponible par ligne ;
 * 3. création de l'**avoir** (numérotation AVOIR, rattaché à la facture via
 *    `sourceRecordId`) ;
 * 4. si retour en stock : **mouvements d'entrée** par produit rattaché (jamais de
 *    stock négatif — l'entrée ne fait que recomposer) ;
 * 5. journal d'audit.
 *
 * L'avoir est une pièce VENTE (il apparaît dans l'historique client et réduit son
 * solde — voir [outstandingBalance]) ; le remboursement en espèces, s'il y a lieu,
 * est une opération Finance distincte.
 */
class ReturnSaleUseCase @Inject constructor(
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
        data object Introuvable : Result()
        data object DejaAnnulee : Result()
        data object Brouillon : Result()
        data object LignesInvalides : Result()
    }

    /** Avoirs précédents de la facture (détail décodé, statut validé). */
    suspend fun avoirsPrecedents(saleRecordId: Long): List<SaleRecordPayload> =
        operationDao.getByModule(OperationModule.VENTE.name)
            .mapNotNull { SaleRecordCodec.decode(it.notes) }
            .filter { it.sourceRecordId == saleRecordId && it.total > 0.0 }

    suspend operator fun invoke(
        saleRecordId: Long,
        returnedLines: List<SaleLine>,
        motif: String,
        retourStock: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (returnedLines.isEmpty()) return Result.LignesInvalides

        return database.withTransaction {
            val vente = operationDao.getById(saleRecordId)
            if (vente == null || vente.module != OperationModule.VENTE.name) {
                return@withTransaction Result.Introuvable
            }
            when (vente.status) {
                OperationStatus.CANCELLED.name -> return@withTransaction Result.DejaAnnulee
                OperationStatus.DRAFT.name -> return@withTransaction Result.Brouillon
                else -> Unit
            }
            val original = SaleRecordCodec.decode(vente.notes)
                ?: return@withTransaction Result.LignesInvalides
            val avoirs = avoirsPrecedents(saleRecordId)

            // Quantités demandées agrégées par ligne (même produit ou même libellé).
            val demande = returnedLines
                .filter { it.quantity > 0.0 }
                .groupBy { ReturnRules.lineKey(it) }
                .mapValues { (_, group) -> group.sumOf { it.quantity } }
            if (!ReturnRules.retourEstValide(original, avoirs, demande)) {
                return@withTransaction Result.LignesInvalides
            }

            // Lignes de l'avoir : prix unitaire de la facture d'origine.
            val lignesAvoir = returnedLines.map { line ->
                val source = original.lines.firstOrNull { ReturnRules.lineKey(it) == ReturnRules.lineKey(line) }
                line.copy(unitPrice = source?.unitPrice ?: line.unitPrice)
            }
            val totalAvoir = lignesAvoir.sumOf { it.total }.coerceAtLeast(0.0)
            if (totalAvoir <= 0.0) return@withTransaction Result.LignesInvalides

            val reference = sequenceManager.next(DocType.AVOIR)
            val recordId = operationDao.insert(
                OperationRecordEntity(
                    module = OperationModule.VENTE.name,
                    reference = reference,
                    title = "Avoir $reference — ${vente.reference}",
                    counterpart = original.clientName,
                    amount = totalAvoir,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = SaleRecordCodec.encode(
                        SaleRecordPayload(
                            clientId = original.clientId,
                            clientName = original.clientName,
                            lines = lignesAvoir,
                            subtotal = totalAvoir,
                            discount = 0.0,
                            delivery = 0.0,
                            taxRate = original.taxRate,
                            taxAmount = 0.0,
                            total = totalAvoir,
                            paymentMethod = original.paymentMethod,
                            paidAmount = 0.0,
                            note = motif.trim().ifBlank { null },
                            sourceRecordId = saleRecordId,
                        ),
                    ),
                    createdAt = now,
                ),
            )

            if (retourStock) {
                for ((produitId, quantite) in returnedLines
                    .filter { it.productId != null && it.quantity > 0.0 }
                    .groupBy { it.productId!! }
                    .mapValues { (_, group) -> group.sumOf { it.quantity } }
                ) {
                    val produit = productDao.getById(produitId) ?: continue
                    val siteId = produit.siteId
                        ?: stockDao.siteAvecPlusDeStock(produitId)
                        ?: siteDao.idPrincipal()
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
                            motif = "RETOUR_VENTE",
                            reference = reference,
                            horodatage = now,
                        ),
                    )
                }
            }

            journalManager.log(
                "VENTE",
                "RETOUR_VENTE",
                "Retour $reference sur $vente.reference — ${totalAvoir} (stock : ${if (retourStock) "oui" else "non"}, motif : ${motif.trim().ifBlank { "AUTRE" }})",
            )
            Result.Succes(recordId, reference)
        }
    }
}

/**
 * Inventaire (spec §12) — **transactionnel** : pour chaque produit compté,
 * écart signé (compté − théorique) ; les écarts nuls ne génèrent aucun mouvement,
 * chaque écart non nul produit un **AJUSTEMENT** portant la même référence INV,
 * le stock après ajustement ne peut jamais être négatif, et tout est journalisé.
 */
class SaveInventoryUseCase @Inject constructor(
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
        data class Succes(val reference: String, val ajustements: Int) : Result()
        data object LectureSeule : Result()
        data object AucuneLecture : Result()
        data object ProduitIntrouvable : Result()
        data object SiteIntrouvable : Result()
    }

    data class Lecture(val produitId: Long, val quantiteComptee: Double)

    suspend operator fun invoke(
        lectures: List<Lecture>,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (lectures.isEmpty()) return Result.AucuneLecture

        return database.withTransaction {
            val reference = sequenceManager.next(DocType.INVENTAIRE)
            var ajustements = 0
            for (lecture in lectures) {
                val produit = productDao.getById(lecture.produitId)
                if (produit == null || !produit.active) return@withTransaction Result.ProduitIntrouvable
                val siteId = produit.siteId
                    ?: stockDao.siteAvecPlusDeStock(produit.id)
                    ?: siteDao.idPrincipal()
                    ?: return@withTransaction Result.SiteIntrouvable
                val theorique = stockDao.quantite(lecture.produitId, siteId) ?: 0.0
                val ecart = InventoryRules.ecart(theorique, lecture.quantiteComptee)
                if (!InventoryRules.ecartRequiertAjustement(ecart)) continue
                if (!InventoryRules.stockApresEstValide(theorique, ecart)) {
                    return@withTransaction Result.AucuneLecture
                }
                val apres = theorique + ecart
                stockDao.ensureRow(lecture.produitId, siteId)
                stockDao.remplacer(ProductStockEntity(lecture.produitId, siteId, apres))
                movementDao.insert(
                    StockMovementEntity(
                        produitId = lecture.produitId,
                        siteId = siteId,
                        type = StockMovementType.AJUSTEMENT,
                        quantite = ecart,
                        motif = "INVENTAIRE",
                        reference = reference,
                        horodatage = now,
                    ),
                )
                ajustements++
            }
            journalManager.log(
                "STOCK",
                "INVENTAIRE",
                "$reference — ${lectures.size} produit(s) compté(s), $ajustements ajustement(s)",
            )
            Result.Succes(reference, ajustements)
        }
    }
}
