package com.missa.b360.core.domain.usecase
import androidx.room.withTransaction

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleValidation
import com.missa.b360.core.domain.model.SaleStockEffects
import com.missa.b360.core.domain.model.TresorerieRules
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
    private val clientDao: ClientDao,
    private val comptesTresorerieDao: CompteTresorerieDao,
    private val mouvementsTresorerieDao: MouvementTresorerieDao,
    private val productDao: ProductDao,
    private val stockService: StockService,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val checkCreditLimit: CheckCreditLimitUseCase,
    private val clientBalance: ClientBalanceUseCase,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object BrouillonIntrouvable : Result()
        data object ClientNonEligible : Result()
        data object ValidationCreditRequise : Result()
        data object CompteEncaissementRequis : Result()
        data object ModuleStockInactif : Result()
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
        if (!SaleValidation.lignesValides(payload.lines) || payload.clientId < 0L || payload.clientName.isBlank()) {
            return Result.DonneesInvalides
        }
        if (!payload.discount.isFinite() || payload.discount < 0.0 ||
            !payload.delivery.isFinite() || payload.delivery < 0.0 ||
            !payload.taxRate.isFinite() || payload.taxRate !in 0.0..100.0 ||
            !totals.total.isFinite() || totals.total <= 0.0 || !payload.total.isFinite() ||
            !payload.paidAmount.isFinite() || payload.paidAmount < 0.0 ||
            payload.paidAmount > totals.total + QUANTITE_EPSILON
        ) return Result.DonneesInvalides
        if (payload.discount > totals.subtotal + QUANTITE_EPSILON || abs(totals.total - payload.total) > 0.01) {
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
                            tiersId = payload.clientId.takeIf { it > 0 },
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
                            tiersId = payload.clientId.takeIf { it > 0 },
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
            val client = if (payload.clientId == 0L) null else clientDao.getById(payload.clientId)
            if (payload.clientId > 0L && client == null) return@withTransaction Result.ClientNonEligible
            if (client == null) {
                // Client comptant/vente anonyme : aucun crédit sans fiche maître.
                if (!SaleValidation.venteComptantSansClientAutorisee(payload.clientId, totals.total, payload.paidAmount)) {
                    return@withTransaction Result.ClientNonEligible
                }
            } else if (!ClientLifecycleRules.venteAutorisee(client, totals.total, payload.paidAmount)) {
                return@withTransaction Result.ClientNonEligible
            }
            // Aucun montant ne peut être marqué payé sans destination Finance active.
            val compteEncaissement = if (payload.paidAmount > QUANTITE_EPSILON) {
                TresorerieRules.compteCible(payload.paymentMethod, comptesTresorerieDao.getAll())
                    ?: return@withTransaction Result.CompteEncaissementRequis
            } else null
            val nouvelleCreance = (totals.total - payload.paidAmount).coerceAtLeast(0.0)
            val limiteCredit = client?.limiteCredit
            if (nouvelleCreance > QUANTITE_EPSILON && limiteCredit != null) {
                // Solde dérivé des ventes validées et avoirs, en cohérence avec la fiche client.
                val soldeActuel = operationDao.getByModule(OperationModule.VENTE.name)
                    .asSequence()
                    .filter { it.status == OperationStatus.VALIDATED.name }
                    .mapNotNull { SaleRecordCodec.decode(it.notes) }
                    .filter { it.clientId == client.id }
                    .sumOf { payloadVente ->
                        val restant = (payloadVente.total - payloadVente.paidAmount).coerceAtLeast(0.0)
                        if (payloadVente.sourceRecordId != null) -restant else restant
                    }
                    .coerceAtLeast(0.0)
                val verdictCredit = checkCreditLimit(
                    soldeActuel = soldeActuel,
                    montantNouvelleVente = nouvelleCreance,
                    limiteCredit = limiteCredit,
                )
                if (verdictCredit != CheckCreditLimitUseCase.Verdict.AUTORISE) {
                    return@withTransaction Result.ValidationCreditRequise
                }
            }
            // Revalider le catalogue côté serveur ; l'indicateur de stock du client n'est pas fiable.
            val produitsCatalogue = mutableMapOf<Long, com.missa.b360.core.data.entity.ProductEntity>()
            for (produitId in payload.lines.mapNotNull { it.productId }.distinct()) {
                val produit = productDao.getById(produitId)
                    ?: return@withTransaction Result.DonneesInvalides
                if (!produit.active || !produit.vendable || !ProduitRules.estVendable(produit.type)) {
                    return@withTransaction Result.DonneesInvalides
                }
                produitsCatalogue[produitId] = produit
            }
            // Normaliser depuis le catalogue : garantit aussi la compatibilité des brouillons antérieurs.
            val lignesValidees = payload.lines.map { line ->
                val productId = line.productId
                val stockable = productId?.let { produitsCatalogue[it] }
                    ?.let { it.stockable && ProduitRules.estStockable(it.type) } ?: false
                line.copy(stockTracked = stockable)
            }
            val detailValide = SaleRecordCodec.encode(
                payload.copy(
                    lines = lignesValidees,
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    delivery = totals.delivery,
                    taxAmount = totals.taxAmount,
                    total = totals.total,
                ),
            )
            val stockPlan = when (val plan = stockService.planifierSortieVente(SaleStockEffects.besoinsParProduit(lignesValidees))) {
                is StockService.SortieResultat.Pret -> plan.lignes
                is StockService.SortieResultat.StockInsuffisant ->
                    return@withTransaction Result.StockInsuffisant(plan.produitNom, plan.disponible, plan.demande)
                StockService.SortieResultat.DonneesInvalides -> return@withTransaction Result.DonneesInvalides
                StockService.SortieResultat.ModuleInactif -> return@withTransaction Result.ModuleStockInactif
            }

            val existant = recordId?.let { operationDao.getById(it) }
            if (recordId != null && (existant == null || existant.module != OperationModule.VENTE.name || existant.status != OperationStatus.DRAFT.name)) {
                return@withTransaction Result.BrouillonIntrouvable
            }
            val reference = existant?.reference ?: sequenceManager.next(DocType.FACTURE)
            when (val sortie = stockService.enregistrerSortieVente(stockPlan, reference, now)) {
                is StockService.SortieResultat.Pret -> Unit
                is StockService.SortieResultat.StockInsuffisant ->
                    return@withTransaction Result.StockInsuffisant(sortie.produitNom, sortie.disponible, sortie.demande)
                StockService.SortieResultat.DonneesInvalides -> return@withTransaction Result.DonneesInvalides
                StockService.SortieResultat.ModuleInactif -> return@withTransaction Result.ModuleStockInactif
            }

            val recordIdFinal = when (val id = recordId) {
                null -> operationDao.insert(
                    OperationRecordEntity(
                        module = OperationModule.VENTE.name,
                        reference = reference,
                        title = payload.clientName,
                        counterpart = payload.clientName,
                        tiersId = payload.clientId.takeIf { it > 0 },
                        amount = totals.total,
                        status = OperationStatus.VALIDATED.name,
                        notes = detailValide,
                        createdAt = now,
                    ),
                )
                else -> {
                    operationDao.update(
                        existant!!.copy(
                            title = payload.clientName,
                            counterpart = payload.clientName,
                            tiersId = payload.clientId.takeIf { it > 0 },
                            amount = totals.total,
                            status = OperationStatus.VALIDATED.name,
                            notes = detailValide,
                        ),
                    )
                    id
                }
            }

            // La somme réellement encaissée entre en trésorerie, sur le premier
            // compte ouvert, avec la référence de la facture comme garde-fou :
            // rouvrir puis revalider la vente ne crédite jamais deux fois.
            // Une référence financière dédiée protège des doubles encaissements ;
            // les validations déjà réglées n'écrivent aucune seconde entrée.
            val referenceEncaissement = TresorerieRules.referenceEncaissement(reference)
            val montantEncaisse = TresorerieRules.encaissementAEnregistrer(
                montantPaye = payload.paidAmount,
                dejaEnregistre = mouvementsTresorerieDao
                    .compterParReference(referenceEncaissement) > 0,
                compteDisponible = compteEncaissement != null,
            )
            if (montantEncaisse != null && compteEncaissement != null) {
                mouvementsTresorerieDao.insert(
                    MouvementTresorerieEntity(
                        compteId = compteEncaissement.id,
                        date = now,
                        sens = SensMouvement.IN.name,
                        montant = montantEncaisse,
                        categorie = CategorieTresorerie.VENTE.name,
                        libelle = payload.clientName,
                        tiers = payload.clientName,
                        modePaiement = payload.paymentMethod,
                        reference = referenceEncaissement,
                        createdAt = now,
                    ),
                )
            }

            // Le compte client est écrit dans la même transaction que la pièce de vente.
            if (payload.clientId > 0L) clientBalance.recalculer(payload.clientId, now)

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
 * Garde de compatibilité : une facture validée est immuable. La correction passe par
 * un avoir dédié ; une annulation directe ne peut pas contourner les compensations
 * de trésorerie et les écritures réglementaires.
 */
class ReverseSaleStockUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val stockService: StockService,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
        data object DejaAnnulee : Result()
        /** Une facture validée reste immuable : utiliser un avoir plutôt qu'une annulation directe. */
        data object AvoirRequis : Result()
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
                OperationStatus.VALIDATED.name -> return@withTransaction Result.AvoirRequis
                else -> return@withTransaction Result.Introuvable
            }
            if (!stockService.compenserSortieVente(record.reference, now)) {
                return@withTransaction Result.Introuvable
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
            var disponible = stockDao.quantite(produitId, siteId) ?: 0.0
            if (disponible < demande - QUANTITE_EPSILON) {
                val autreSite = stockDao.siteAvecPlusDeStock(produitId)
                if (autreSite != null) disponible = stockDao.quantite(produitId, autreSite) ?: 0.0
            }
            if (disponible < demande - QUANTITE_EPSILON) return Verdict(produit.nom, disponible, demande)
        }
        return null
    }
}
