package com.missa.b360.core.domain.usecase
import androidx.room.withTransaction

import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.math.abs

/** Tolérance de comparaison des quantités (jamais de test d'égalité stricte sur des Double). */
const val QUANTITE_EPSILON = 1e-9

/**
 * Règles pures des mouvements de stock (spec §11/§13) — couvertes par les tests.
 */
object StockValidation {
    /** ENTRÉE / SORTIE : quantité strictement positive et finie. */
    fun quantiteEntrSortieEstValide(quantite: Double): Boolean =
        quantite.isFinite() && quantite > 0.0

    /** AJUSTEMENT : écart signé, non nul, fini. */
    fun ecartAjustementEstValide(ecart: Double): Boolean =
        ecart.isFinite() && ecart.abs() >= QUANTITE_EPSILON

    /** Transfert (spec §13) : source ≠ destination, quantité positive. */
    fun transfertEstValide(sourceId: Long?, destId: Long?, quantite: Double): Boolean =
        sourceId != null && destId != null && sourceId != destId &&
            quantite.isFinite() && quantite > 0.0

    /** Stock après application — interdit d'aller sous zéro (spec §43). */
    fun stockApresEstValide(avant: Double, delta: Double): Boolean =
        avant + delta >= -QUANTITE_EPSILON
}

/** Résultat d'une écriture de mouvement, avec avant/après pour le résumé UI (spec §11). */
sealed class StockMovementResult {
    data class Succes(val stockAvant: Double, val stockApres: Double) : StockMovementResult()
    data object LectureSeule : StockMovementResult()
    data object Invalid : StockMovementResult()
    data object ProduitIntrouvable : StockMovementResult()
    /** Aucun site de sortie résolvable (ni site principal ni stock ailleurs). */
    data object SiteIntrouvable : StockMovementResult()
    data class StockInsuffisant(val disponible: Double, val demande: Double) : StockMovementResult()
}

/**
 * Écriture d'un mouvement de stock (ENTRÉE / SORTIE / AJUSTERT) — transactionnelle :
 * relecture du stock, vérification de disponibilité, mise à jour de la ligne de
 * stock et insertion du mouvement dans une seule transaction Room (spec §43/§44).
 * Le TRANSFERT passe par [TransferStockUseCase] (paire de mouvements).
 */
class RecordStockMovementUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(
        produitId: Long,
        type: StockMovementType,
        quantite: Double,
        motif: String,
        reference: String? = null,
        commentaire: String? = null,
        now: Long = System.currentTimeMillis(),
    ): StockMovementResult {
        val estTypeDirect = type in setOf(
            StockMovementType.ENTREE,
            StockMovementType.SORTIE,
            StockMovementType.AJUSTEMENT,
        )
        if (!estTypeDirect) return StockMovementResult.Invalid
        if (!when (type) {
            StockMovementType.ENTREE -> StockValidation.quantiteEntrSortieEstValide(quantite)
            StockMovementType.SORTIE -> StockValidation.quantiteEntrSortieEstValide(quantite)
            StockMovementType.AJUSTEMENT -> StockValidation.ecartAjustementEstValide(quantite)
            else -> false
        }) {
            return StockMovementResult.Invalid
        }
        if (licenceManager.isReadOnly()) return StockMovementResult.LectureSeule
        val produit = productDao.getById(produitId)
        if (produit == null || !produit.active) return StockMovementResult.ProduitIntrouvable
        val motifNormalise = motif.trim().ifBlank { "AUTRE" }

        return database.withTransaction {
            // Relecture juste avant commit : le stock affiché peut avoir changé (§43).
            val siteId = produit.siteId
                ?: stockDao.siteAvecPlusDeStock(produitId)
                ?: return@withTransaction StockMovementResult.SiteIntrouvable
            val avant = stockDao.quantite(produitId, siteId) ?: 0.0
            val delta = when (type) {
                StockMovementType.ENTREE, StockMovementType.AJUSTEMENT -> quantite
                StockMovementType.SORTIE -> -quantite
                else -> 0.0
            }
            if (!StockValidation.stockApresEstValide(avant, delta)) {
                return@withTransaction StockMovementResult.StockInsuffisant(avant, quantite)
            }
            val apres = (avant + delta).coerceAtLeast(0.0)
            stockDao.ensureRow(produitId, siteId)
            stockDao.remplacer(ProductStockEntity(produitId, siteId, apres))
            movementDao.insert(
                StockMovementEntity(
                    produitId = produitId,
                    siteId = siteId,
                    type = type,
                    quantite = quantite,
                    motif = motifNormalise,
                    reference = reference?.trim()?.ifBlank { null },
                    commentaire = commentaire?.trim()?.ifBlank { null },
                    horodatage = now,
                ),
            )
            journalManager.log(
                "STOCK",
                "MOUVEMENT_STOCK",
                "${produit.code} ${type.name} ${quantite} — $motifNormalise (stock $avant → $apres)",
            )
            StockMovementResult.Succes(avant, apres)
        }
    }
}

/**
 * Transfert de stock entre deux entrepôts (spec §13) — transactionnel :
 * vérification source ≠ destination, quantité > 0, quantité ≤ stock disponible,
 * mise à jour des deux lignes de stock et **paire de mouvements** partageant la
 * même référence TRF (numérotation RA-09).
 */
class TransferStockUseCase @Inject constructor(
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
        data class Succes(
            val reference: String,
            val stockAvantSource: Double,
            val stockApresSource: Double,
            val stockApresDestination: Double,
        ) : Result()
        data object LectureSeule : Result()
        data object Invalid : Result()
        data object ProduitIntrouvable : Result()
        data class StockInsuffisant(val disponible: Double, val demande: Double) : Result()
    }

    suspend operator fun invoke(
        produitId: Long,
        siteSourceId: Long,
        siteDestId: Long,
        quantite: Double,
        motif: String,
        commentaire: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (!StockValidation.transfertEstValide(siteSourceId, siteDestId, quantite)) {
            return Result.Invalid
        }
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val produit = productDao.getById(produitId)
        if (produit == null || !produit.active) return Result.ProduitIntrouvable
        val sourceNom = siteDao.getNomById(siteSourceId) ?: "Source"
        val destNom = siteDao.getNomById(siteDestId) ?: "Destination"
        val motifNormalise = motif.trim().ifBlank { "TRANSFERT" }
        val commentaireNormalise = commentaire?.trim()?.ifBlank { null }

        return database.withTransaction {
            // Relecture juste avant commit (§43).
            val avantSource = stockDao.quantite(produitId, siteSourceId) ?: 0.0
            if (avantSource < quantite - QUANTITE_EPSILON) {
                return@withTransaction Result.StockInsuffisant(avantSource, quantite)
            }
            val apresSource = (avantSource - quantite).coerceAtLeast(0.0)
            val avantDest = stockDao.quantite(produitId, siteDestId) ?: 0.0
            val apresDest = avantDest + quantite
            stockDao.ensureRow(produitId, siteSourceId)
            stockDao.remplacer(ProductStockEntity(produitId, siteSourceId, apresSource))
            stockDao.ensureRow(produitId, siteDestId)
            stockDao.remplacer(ProductStockEntity(produitId, siteDestId, apresDest))
            val reference = sequenceManager.next(DocType.TRANSFERT)
            movementDao.insert(
                StockMovementEntity(
                    produitId = produitId,
                    siteId = siteSourceId,
                    type = StockMovementType.TRANSFERT_SORTIE,
                    quantite = quantite,
                    motif = motifNormalise,
                    reference = reference,
                    commentaire = "Transfert vers $destNom",
                    horodatage = now,
                ),
            )
            movementDao.insert(
                StockMovementEntity(
                    produitId = produitId,
                    siteId = siteDestId,
                    type = StockMovementType.TRANSFERT_ENTREE,
                    quantite = quantite,
                    motif = motifNormalise,
                    reference = reference,
                    commentaire = "Transfert depuis $sourceNom",
                    horodatage = now,
                ),
            )
            journalManager.log(
                "STOCK",
                "TRANSFERT_STOCK",
                "$reference — ${produit.code} × $quantite : $sourceNom → $destNom",
            )
            Result.Succes(reference, avantSource, apresSource, apresDest)
        }
    }
}

/** Historique des mouvements pour l'onglet « Mouvements » du module Stock. */
class ObserveStockMovementsUseCase @Inject constructor(
    private val movementDao: StockMovementDao,
) {
    operator fun invoke(limit: Int = 200): Flow<List<StockMovementView>> = movementDao.observeJoints(limit)
}
