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
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        ecart.isFinite() && abs(ecart) >= QUANTITE_EPSILON

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
    data object ModuleInactif : StockMovementResult()
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
    private val activationRepository: ProfilActivationRepository,
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
        // Activation profil : Stock doit être actif
        val activation = activationRepository.getActivation()
        if (activation.modulesActifs.isNotEmpty() && !activation.isModuleActif(ModuleCode.STK)) {
            return StockMovementResult.ModuleInactif
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
            stockDao.remplacer(produitId, siteId, apres)
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
 * Port d'écriture Stock consommé par les ventes : le module Vente transmet ses
 * besoins métier, Stock choisit le dépôt effectif, vérifie la disponibilité et
 * possède les écritures de balance et de mouvements.
 *
 * Les mutations doivent être appelées dans la transaction AppDatabase du document
 * commercial afin que facture et mouvement soient atomiques.
 */
class StockService @Inject constructor(
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val journalManager: JournalManager,
    private val activationRepository: ProfilActivationRepository,
) {
    data class SortiePlanifiee(val produitId: Long, val siteId: Long, val quantite: Double)
    sealed interface SortieResultat {
        data class Pret(val lignes: List<SortiePlanifiee>) : SortieResultat
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : SortieResultat
        data object DonneesInvalides : SortieResultat
        data object ModuleInactif : SortieResultat
    }

    /** Lecture/validation seulement : aucune écriture tant que tous les articles ne passent pas. */
    suspend fun planifierSortieVente(besoins: Map<Long, Double>): SortieResultat {
        if (besoins.isEmpty()) return SortieResultat.Pret(emptyList())
        if (!stockModuleActif()) return SortieResultat.ModuleInactif
        val plan = mutableListOf<SortiePlanifiee>()
        for ((produitId, demande) in besoins.toSortedMap()) {
            if (!demande.isFinite() || demande <= 0.0) return SortieResultat.DonneesInvalides
            val produit = productDao.getById(produitId)
                ?: return SortieResultat.StockInsuffisant("Article introuvable", 0.0, demande)
            if (!produit.active) return SortieResultat.DonneesInvalides

            val sitePrefere = produit.siteId
            val stockPrefere = sitePrefere?.let { stockDao.quantite(produitId, it) } ?: 0.0
            val siteEffectif = if (sitePrefere != null && stockPrefere >= demande - QUANTITE_EPSILON) {
                sitePrefere
            } else {
                stockDao.siteAvecPlusDeStock(produitId) ?: sitePrefere
            } ?: return SortieResultat.StockInsuffisant(produit.nom, 0.0, demande)
            val disponible = stockDao.quantite(produitId, siteEffectif) ?: 0.0
            if (disponible < demande - QUANTITE_EPSILON) {
                return SortieResultat.StockInsuffisant(produit.nom, disponible, demande)
            }
            plan += SortiePlanifiee(produitId, siteEffectif, demande)
        }
        return SortieResultat.Pret(plan)
    }

    /** Relecture groupée avant toute écriture, puis stock + mouvements dans la transaction parente. */
    suspend fun enregistrerSortieVente(
        plan: List<SortiePlanifiee>,
        reference: String,
        now: Long,
    ): SortieResultat {
        if (plan.isNotEmpty() && !stockModuleActif()) return SortieResultat.ModuleInactif
        if (plan.isNotEmpty() && movementDao.getByReference(reference).any {
                it.type == StockMovementType.SORTIE && it.motif == "VENTE"
            }) return SortieResultat.DonneesInvalides
        for (ligne in plan) {
            val disponible = stockDao.quantite(ligne.produitId, ligne.siteId) ?: 0.0
            if (disponible < ligne.quantite - QUANTITE_EPSILON) {
                val nom = productDao.getById(ligne.produitId)?.nom ?: "Article"
                return SortieResultat.StockInsuffisant(nom, disponible, ligne.quantite)
            }
        }
        for (ligne in plan) {
            val avant = stockDao.quantite(ligne.produitId, ligne.siteId) ?: 0.0
            stockDao.ensureRow(ligne.produitId, ligne.siteId)
            stockDao.remplacer(ligne.produitId, ligne.siteId, (avant - ligne.quantite).coerceAtLeast(0.0))
            movementDao.insert(
                StockMovementEntity(
                    produitId = ligne.produitId,
                    siteId = ligne.siteId,
                    type = StockMovementType.SORTIE,
                    quantite = ligne.quantite,
                    motif = "VENTE",
                    reference = reference,
                    horodatage = now,
                ),
            )
        }
        if (plan.isNotEmpty()) journalManager.log("STOCK", "SORTIE_VENTE", "Vente $reference — ${plan.size} ligne(s) sortie(s)")
        return SortieResultat.Pret(plan)
    }

    /** Entrée Stock pour un retour accepté ; Vente transmet seulement les besoins métier. */
    suspend fun enregistrerRetourVente(
        besoins: Map<Long, Double>,
        reference: String,
        now: Long,
        sourceReference: String,
    ): Boolean {
        if (besoins.isNotEmpty() && !stockModuleActif()) return false
        if (movementDao.getByReference(reference).any {
                it.type == StockMovementType.ENTREE && it.motif == "RETOUR_VENTE"
            }) return false
        val sortiesOriginales = movementDao.getByReference(sourceReference)
            .filter { it.type == StockMovementType.SORTIE && it.motif == "VENTE" }
        val lignes = mutableListOf<SortiePlanifiee>()
        for ((produitId, quantite) in besoins.toSortedMap()) {
            if (!quantite.isFinite() || quantite <= 0.0) return false
            val produit = productDao.getById(produitId) ?: return false
            val siteId = sortiesOriginales.firstOrNull { it.produitId == produitId }?.siteId
                ?: produit.siteId
                ?: stockDao.siteAvecPlusDeStock(produitId)
                ?: return false
            lignes += SortiePlanifiee(produitId, siteId, quantite)
        }
        // Tout valider avant d'écrire pour éviter un retour partiellement enregistré.
        if (lignes.any { !it.quantite.isFinite() || it.quantite <= 0.0 }) return false
        for (ligne in lignes) {
            val avant = stockDao.quantite(ligne.produitId, ligne.siteId) ?: 0.0
            stockDao.ensureRow(ligne.produitId, ligne.siteId)
            stockDao.remplacer(ligne.produitId, ligne.siteId, avant + ligne.quantite)
            movementDao.insert(
                StockMovementEntity(
                    produitId = ligne.produitId,
                    siteId = ligne.siteId,
                    type = StockMovementType.ENTREE,
                    quantite = ligne.quantite,
                    motif = "RETOUR_VENTE",
                    reference = reference,
                    commentaire = "Retour client accepté",
                    horodatage = now,
                ),
            )
        }
        if (lignes.isNotEmpty()) journalManager.log("STOCK", "RETOUR_VENTE", "Avoir $reference — ${lignes.size} entrée(s) de stock")
        return true
    }

    private suspend fun stockModuleActif(): Boolean {
        val activation = activationRepository.getActivation()
        return activation.modulesActifs.isEmpty() || activation.isModuleActif(ModuleCode.STK)
    }

    /** Compensation fidèle aux sites des sorties d'origine (pas au site produit actuel). */
    suspend fun compenserSortieVente(reference: String, now: Long): Boolean {
        val mouvements = movementDao.getByReference(reference)
        // Une compensation précédente est un no-op idempotent.
        if (mouvements.any { it.type == StockMovementType.ENTREE && it.motif == "ANNULATION_VENTE" }) return true
        val sorties = mouvements
            .filter { it.type == StockMovementType.SORTIE && it.motif == "VENTE" }
            .groupBy { it.produitId to it.siteId }
            .map { (cle, lignes) -> SortiePlanifiee(cle.first, cle.second, lignes.sumOf { it.quantite }) }
        for (ligne in sorties) {
            if (!ligne.quantite.isFinite() || ligne.quantite <= 0.0) return false
        }
        for (ligne in sorties) {
            val avant = stockDao.quantite(ligne.produitId, ligne.siteId) ?: 0.0
            stockDao.ensureRow(ligne.produitId, ligne.siteId)
            stockDao.remplacer(ligne.produitId, ligne.siteId, avant + ligne.quantite)
            movementDao.insert(
                StockMovementEntity(
                    produitId = ligne.produitId,
                    siteId = ligne.siteId,
                    type = StockMovementType.ENTREE,
                    quantite = ligne.quantite,
                    motif = "ANNULATION_VENTE",
                    reference = reference,
                    horodatage = now,
                ),
            )
        }
        if (sorties.isNotEmpty()) journalManager.log("STOCK", "ANNULATION_SORTIE_VENTE", "Vente $reference — stock compensé")
        return true
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
    private val activationRepository: ProfilActivationRepository,
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
        data object ModuleInactif : Result()
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
        val activation = activationRepository.getActivation()
        if (activation.modulesActifs.isNotEmpty() && !activation.isModuleActif(ModuleCode.STK)) {
            return Result.ModuleInactif
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
            stockDao.remplacer(produitId, siteSourceId, apresSource)
            stockDao.ensureRow(produitId, siteDestId)
            stockDao.remplacer(produitId, siteDestId, apresDest)
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

/**
 * Suppression d'un article = désactivation douce : l'article disparaît des listes
 * (requêtes filtrées sur active = 1) mais l'historique des mouvements et les
 * références restent intacts. Bloquée tant que le stock n'est pas nul.
 */
class SupprimerProduitUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val observeStock: ObserveProductStockUseCase,
    private val licenceManager: LicenceManager,
) {
    sealed class Result {
        data object Supprime : Result()
        data class StockNonNul(val quantite: Double) : Result()
        data object LectureSeule : Result()
    }

    suspend operator fun invoke(produitId: Long): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val stock = observeStock().first().filter { it.produitId == produitId }.sumOf { it.quantite }
        if (abs(stock) >= QUANTITE_EPSILON) return Result.StockNonNul(stock)
        productDao.desactiver(produitId)
        return Result.Supprime
    }
}
