package com.missa.b360.core.domain.usecase
import androidx.room.withTransaction

import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.AchatTresorerieRules
import com.missa.b360.core.domain.model.DecaissementAchat
import com.missa.b360.core.domain.model.InventoryRules
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.domain.model.PaymentDecision
import com.missa.b360.core.domain.model.PaymentGuardReason
import com.missa.b360.core.domain.model.SupplierPaymentDecision
import com.missa.b360.core.domain.model.PurchaseStockEffects
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.ReturnRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.notifications.AppNotifier
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
    private val compteFournisseurDao: FournisseurCompteBancaireDao,
    private val productDao: ProductDao,
    private val siteDao: SiteDao,
    private val groupeDao: GroupeArticleDao,
    private val stockService: StockService,
    private val comptesTresorerieDao: CompteTresorerieDao,
    private val mouvementsTresorerieDao: MouvementTresorerieDao,
    private val appNotifier: AppNotifier,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val fournisseurCache: FournisseurCacheUseCase,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object FournisseurIntrouvable : Result()
        data object FournisseurNonActif : Result()
        data object StockModuleInactif : Result()
        data object SiteIntrouvable : Result()
        data object ReceptionRequise : Result()
        data object BrouillonIntrouvable : Result()
        data object SoldeInsuffisant : Result()

        /** Le montant réglé à la validation exige une confirmation (compte non vérifié, plafond dépassé). */
        data class ConfirmationPaiementRequise(val motif: PaymentGuardReason) : Result()

        /** Le montant réglé à la validation est refusé : compte absent, rejeté ou récent, fournisseur bloqué. */
        data class PaiementRefuse(val motif: PaymentGuardReason) : Result()
    }

    private class TransactionRefusee(val resultat: Result) : RuntimeException()

    suspend operator fun invoke(
        recordId: Long?,
        payload: PurchaseRecordPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
        confirmePaiement: Boolean = false,
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lines.isEmpty() || !payload.total.isFinite() || payload.total <= 0.0 ||
            !payload.taxRate.isFinite() || payload.taxRate < 0.0 ||
            !payload.taxAmount.isFinite() || payload.taxAmount < 0.0 ||
            !payload.paidAmount.isFinite() ||
            payload.lines.any {
                !it.quantity.isFinite() || it.quantity <= 0.0 || !it.unitPrice.isFinite() ||
                    it.unitPrice < 0.0 || !it.total.isFinite()
            } ||
            payload.lines.map { it.id }.distinct().size != payload.lines.size
        ) return Result.DonneesInvalides
        val subtotal = payload.lines.sumOf { it.total }.coerceAtLeast(0.0)
        if (abs(subtotal - payload.total) > 0.01) return Result.DonneesInvalides
        if (payload.paidAmount < -QUANTITE_EPSILON || payload.paidAmount > payload.total + QUANTITE_EPSILON) {
            return Result.DonneesInvalides
        }
        val fournisseur = fournisseurDao.getById(payload.supplierId) ?: return Result.FournisseurIntrouvable
        if (!draft && !FournisseurRules.peutCommander(fournisseur.statut)) {
            return Result.FournisseurNonActif
        }

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

        return try {
            database.withTransaction {
            val groupes = groupeDao.listerComplets()
            data class ReceptionStock(val ligne: PurchaseLine, val siteId: Long)
            val receptions = mutableListOf<ReceptionStock>()
            var imputationsDirectes = 0
            if (payload.receptionRecordId != null) {
                // Une facture rapprochée d'une réception déjà validée ne recrée aucun stock.
                val receptionRecord = operationDao.getById(payload.receptionRecordId)
                val receptionPayload = ReceptionCodec.decode(receptionRecord?.notes)
                if (receptionRecord == null || receptionRecord.module != OperationModule.ACHATS.name ||
                    receptionRecord.status != OperationStatus.VALIDATED.name ||
                    receptionPayload?.supplierId != payload.supplierId ||
                    (payload.commandeRecordId != null && receptionPayload.commandeRecordId != payload.commandeRecordId)
                ) return@withTransaction Result.DonneesInvalides
                val receptionDetail = receptionPayload ?: return@withTransaction Result.DonneesInvalides
                val recues = receptionDetail.lignes.filter { it.quantiteRecue > 0.0 }
                    .groupBy { it.productId }.mapValues { (_, lignes) -> lignes.sumOf { it.quantiteRecue } }
                val facturees = payload.lines.filter { it.productId != null }
                    .groupBy { it.productId!! }.mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
                if (facturees.any { (id, quantite) -> quantite > (recues[id] ?: 0.0) + 1e-9 }) {
                    return@withTransaction Result.DonneesInvalides
                }
            } else {
                if (payload.commandeRecordId != null) {
                    val commandeRecord = operationDao.getById(payload.commandeRecordId)
                    val commandePayload = CommandeAchatCodec.decode(commandeRecord?.notes)
                    if (commandeRecord == null || commandeRecord.module != OperationModule.ACHATS.name ||
                        commandeRecord.status != OperationStatus.VALIDATED.name ||
                        commandePayload?.supplierId != payload.supplierId
                    ) return@withTransaction Result.DonneesInvalides
                    val commandee = commandePayload?.lines.orEmpty().filter { it.productId != null }
                        .groupBy { it.productId!! }
                        .mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
                    val facturees = payload.lines.filter { it.productId != null }
                        .groupBy { it.productId!! }
                        .mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
                    if (facturees.any { (id, quantite) -> quantite > (commandee[id] ?: 0.0) + 1e-9 }) {
                        return@withTransaction Result.DonneesInvalides
                    }
                    for (productId in facturees.keys) {
                        val produit = productDao.getById(productId)
                            ?: return@withTransaction Result.DonneesInvalides
                        if (ReglesGroupesArticles.estStocke(produit, groupes)) {
                            return@withTransaction Result.ReceptionRequise
                        }
                    }
                }
                for (ligne in payload.lines.filter { it.productId != null }) {
                    val produitId = ligne.productId ?: continue
                    val produit = productDao.getById(produitId)
                    if (produit == null || !produit.active ||
                        ReglesGroupesArticles.achetables(listOf(produit), groupes).isEmpty()
                    ) return@withTransaction Result.DonneesInvalides
                    if (!ReglesGroupesArticles.estStocke(produit, groupes)) {
                        imputationsDirectes++
                        continue
                    }
                    val siteId = ligne.siteId ?: produit.siteId ?: siteDao.idPrincipal()
                        ?: return@withTransaction Result.SiteIntrouvable
                    receptions += ReceptionStock(ligne.copy(siteId = siteId), siteId)
                }
            }
            val lignesAvecDepot = payload.lines.map { ligne ->
                val site = receptions.firstOrNull { it.ligne.id == ligne.id }?.siteId
                if (site == null) ligne else ligne.copy(siteId = site)
            }
            val payloadPersisted = payload.copy(lines = lignesAvecDepot)

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
                            notes = PurchaseRecordCodec.encode(payloadPersisted),
                            createdAt = now,
                        ),
                    )
                    newId to ref
                }
                else -> {
                    val existant = operationDao.getById(id)
                    if (existant == null || existant.module != OperationModule.ACHATS.name ||
                        existant.status != OperationStatus.DRAFT.name
                    ) return@withTransaction Result.BrouillonIntrouvable
                    operationDao.update(
                        existant.copy(
                            title = "Facture fournisseur — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = payload.total,
                            status = OperationStatus.VALIDATED.name,
                            notes = PurchaseRecordCodec.encode(payloadPersisted),
                        ),
                    )
                    id to existant.reference
                }
            }

            for ((index, reception) in receptions.withIndex()) {
                val ligne = reception.ligne
                val productId = ligne.productId ?: continue
                when (stockService.recordInbound(
                    productId = productId,
                    siteId = reception.siteId,
                    quantity = ligne.quantity,
                    unitCost = ligne.unitPrice,
                    sourceModule = "ACH",
                    sourceDocumentType = "PURCHASE_INVOICE",
                    sourceDocumentId = recordIdFinal,
                    sourceLineId = ligne.id.takeIf { it >= 0 } ?: index.toLong(),
                    reference = reference,
                    lotNumber = ligne.lot,
                    serialNumber = ligne.numeroSerie,
                    expiryDate = ligne.datePeremption,
                    reason = "PURCHASE_INVOICE_IN",
                    now = now,
                )) {
                    is StockService.MouvementAchatResultat.Succes -> Unit
                    StockService.MouvementAchatResultat.ModuleInactif ->
                        throw TransactionRefusee(Result.StockModuleInactif)
                    StockService.MouvementAchatResultat.SiteIntrouvable ->
                        throw TransactionRefusee(Result.SiteIntrouvable)
                    else -> throw TransactionRefusee(Result.DonneesInvalides)
                }
            }

            // 4. Ce qui est réglé à la validation SORT de la trésorerie : la garde de paiement et le
            //    solde du compte s'appliquent comme pour un règlement ultérieur. Le contrôle précède
            //    toute écriture : un refus annule la transaction (aucune pièce, aucun stock).
            val referenceDecaissement = TresorerieRules.referenceEncaissement(reference)
            val compteDecaissement = TresorerieRules.compteCible(
                payload.paymentMethod,
                comptesTresorerieDao.getAll(),
            )
            if (payload.paidAmount > QUANTITE_EPSILON) {
                when (
                    val decision = SupplierPaymentDecision.decider(
                        fournisseur = fournisseur,
                        montant = payload.paidAmount,
                        modePaiement = payload.paymentMethod,
                        comptes = compteFournisseurDao.listeParFournisseur(fournisseur.id),
                        compteId = null,
                        confirme = confirmePaiement,
                        now = now,
                    )
                ) {
                    is PaymentDecision.Proceed -> decision.confirmeMotif?.let {
                        journalManager.log(
                            "ACHATS",
                            "REGLEMENT_CONFIRME",
                            "Facture $reference confirmée malgré : $it",
                        )
                    }
                    is PaymentDecision.NeedConfirmation ->
                        throw TransactionRefusee(Result.ConfirmationPaiementRequise(decision.motif))
                    is PaymentDecision.Reject ->
                        throw TransactionRefusee(Result.PaiementRefuse(decision.motif))
                }
            }
            val decaissement = AchatTresorerieRules.decaissementALaValidation(
                montantPaye = payload.paidAmount,
                dejaEnregistre = mouvementsTresorerieDao.compterParReference(referenceDecaissement) > 0,
                compteId = compteDecaissement?.id,
                soldeCourant = compteDecaissement?.let { comptesTresorerieDao.soldeCourant(it.id) },
            )
            when (decaissement) {
                DecaissementAchat.Aucun -> Unit
                DecaissementAchat.SoldeInsuffisant -> throw TransactionRefusee(Result.SoldeInsuffisant)
                is DecaissementAchat.Sortie -> mouvementsTresorerieDao.insert(
                    MouvementTresorerieEntity(
                        compteId = decaissement.compteId,
                        date = now,
                        sens = decaissement.sens.name,
                        montant = decaissement.montant,
                        categorie = CategorieTresorerie.ACHAT.name,
                        libelle = payload.supplierName,
                        tiers = payload.supplierName,
                        modePaiement = payload.paymentMethod,
                        reference = referenceDecaissement,
                        createdAt = now,
                    ),
                )
            }

            // 5. Événement qualité : une réception physique est à contrôler.
            if (receptions.isNotEmpty()) {
                appNotifier.notifier(
                    type = "QUA",
                    titre = "Réception à contrôler",
                    message = "$reference — ${payload.supplierName} : " +
                        "${receptions.size} article(s) reçu(s)",
                    date = now,
                )
            }

            val passif = (payload.total - payload.paidAmount).coerceAtLeast(0.0)
            appNotifier.notifier(
                type = "ACHATS",
                titre = "Facture enregistrée",
                message = "$reference — ${payload.supplierName} : ${payload.total}",
                date = now,
            )
            journalManager.log(
                "ACHATS",
                "ACHAT_VALIDATE",
                "Achat $reference — ${payload.supplierName} (${payload.total} réglé " +
                    "${payload.paidAmount}, passif $passif, réceptions ${receptions.size}, " +
                    "imputations directes $imputationsDirectes)",
            )
            fournisseurCache.rafraichir(payload.supplierId, now)
            Result.Succes(recordIdFinal, reference)
            }
        } catch (refusee: TransactionRefusee) {
            refusee.resultat
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
    private val stockService: StockService,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val clientBalance: ClientBalanceUseCase,
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
            if (retourStock) {
                val besoinsStock = lignesAvoir
                    .filter { it.productId != null && it.stockTracked && it.quantity > 0.0 }
                    .groupBy { it.productId!! }
                    .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
                if (!stockService.enregistrerRetourVente(besoinsStock, reference, now, vente.reference)) {
                    return@withTransaction Result.LignesInvalides
                }
            }
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

            // L'avoir réduit l'encours : compte client mis à jour dans la même transaction.
            if (original.clientId > 0L) clientBalance.recalculer(original.clientId, now)

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
                stockDao.remplacer(lecture.produitId, siteId, apres)
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
