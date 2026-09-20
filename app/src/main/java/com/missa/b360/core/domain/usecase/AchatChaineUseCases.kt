package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.CompteTresorerieDao
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
import com.missa.b360.core.domain.model.AchatCommandeRules
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseStockEffects
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject

/**
 * Chaîne d'achat (spec §6) — la séparation des rôles :
 *
 * - [SaveCommandeAchatUseCase] : ACHATS décide et documente (bon de commande) ;
 * - [SaveReceptionAchatUseCase] : STOCK réceptionne et valorise (bon de réception) ;
 * - [ReglerAchatUseCase] : TRÉSORERIE règle le fournisseur (règlements numérotés) ;
 * - [AnnulerAchatUseCase] : contre-passation transactionnelle, jamais de suppression.
 *
 * La COMPTABILITÉ n'écrit rien : elle relit les pièces et la trésorerie.
 */

/** Bon de commande fournisseur — aucun effet stock ni trésorerie à ce stade. */
class SaveCommandeAchatUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val fournisseurDao: FournisseurDao,
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
        payload: CommandeAchatPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lines.isEmpty() || payload.lines.none { it.quantity > 0.0 }) {
            return Result.DonneesInvalides
        }
        if (fournisseurDao.getById(payload.supplierId) == null) return Result.FournisseurIntrouvable

        val statut = if (draft) OperationStatus.DRAFT.name else OperationStatus.VALIDATED.name
        val total = payload.lines.sumOf { it.total }.coerceAtLeast(0.0)
        return when (val id = recordId) {
            null -> {
                val reference = sequenceManager.next(DocType.BON_COMMANDE)
                val newId = operationDao.insert(
                    OperationRecordEntity(
                        module = OperationModule.ACHATS.name,
                        reference = reference,
                        title = "Bon de commande — ${payload.supplierName}",
                        counterpart = payload.supplierName,
                        amount = total,
                        status = statut,
                        notes = CommandeAchatCodec.encode(payload),
                        createdAt = now,
                    ),
                )
                journalManager.log(
                    "ACHATS",
                    if (draft) "BROUILLON_COMMANDE" else "COMMANDE_VALIDEE",
                    "Commande $reference — ${payload.supplierName} ($total)",
                )
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
                        title = "Bon de commande — ${payload.supplierName}",
                        counterpart = payload.supplierName,
                        amount = total,
                        status = statut,
                        notes = CommandeAchatCodec.encode(payload),
                    ),
                )
                journalManager.log(
                    "ACHATS",
                    if (draft) "BROUILLON_COMMANDE" else "COMMANDE_VALIDEE",
                    "Commande ${existant.reference} — ${payload.supplierName} ($total)",
                )
                Result.Succes(id, existant.reference)
            }
        }
    }
}

/**
 * Bon de réception — **transactionnel** : entrées de stock avec traçabilité
 * lot/série/péremption, valorisation au prix d'achat catalogue (CUMP),
 * garde-fou de dépassement de commande, notification qualité.
 */
class SaveReceptionAchatUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val fournisseurDao: FournisseurDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val siteDao: SiteDao,
    private val groupeDao: GroupeArticleDao,
    private val appNotifier: AppNotifier,
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
        data object CommandeIntrouvable : Result()
        data object DepasseCommande : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        payload: ReceptionPayload,
        draft: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lignes.none { it.quantiteRecue > 0.0 }) return Result.DonneesInvalides
        if (fournisseurDao.getById(payload.supplierId) == null) return Result.FournisseurIntrouvable

        if (draft) {
            val reference = recordId?.let { operationDao.getById(it)?.reference }
                ?: sequenceManager.next(DocType.BON_RECEPTION)
            val statut = OperationStatus.DRAFT.name
            val notes = ReceptionCodec.encode(payload)
            return if (recordId == null) {
                val newId = operationDao.insert(
                    OperationRecordEntity(
                        module = OperationModule.ACHATS.name,
                        reference = reference,
                        title = "Bon de réception — ${payload.supplierName}",
                        counterpart = payload.supplierName,
                        amount = 0.0,
                        status = statut,
                        notes = notes,
                        createdAt = now,
                    ),
                )
                Result.Succes(newId, reference)
            } else {
                val existant = operationDao.getById(recordId)
                if (existant == null || existant.status != OperationStatus.DRAFT.name) {
                    return Result.CommandeIntrouvable
                }
                operationDao.update(existant.copy(notes = notes))
                Result.Succes(recordId, existant.reference)
            }
        }

        return database.withTransaction {
            // Garde-fou commande : jamais plus que le reste à recevoir par produit.
            if (payload.commandeRecordId != null) {
                val commande = operationDao.getById(payload.commandeRecordId)
                val commandePayload = CommandeAchatCodec.decode(commande?.notes)
                if (commande == null || commande.module != OperationModule.ACHATS.name ||
                    commande.status != OperationStatus.VALIDATED.name || commandePayload == null
                ) {
                    return@withTransaction Result.CommandeIntrouvable
                }
                val commandee = commandePayload.lines
                    .filter { it.productId != null && it.quantity > 0.0 }
                    .groupBy { it.productId }
                    .mapNotNull { (k, v) -> k?.let { it to v.sumOf { it.quantity } } }
                    .toMap()
                val dejaRecu = receptionsDe(operationDao, payload.commandeRecordId)
                    .flatMap { it.lignes }
                    .filter { it.quantiteRecue > 0.0 }
                    .groupBy { it.productId }
                    .mapValues { (_, l) -> l.sumOf { it.quantiteRecue } }
                if (!AchatCommandeRules.receptionEstValide(commandee, dejaRecu, payload.lignes)) {
                    return@withTransaction Result.DepasseCommande
                }
            }

            val (recordIdFinal, reference) = when (val id = recordId) {
                null -> {
                    val ref = sequenceManager.next(DocType.BON_RECEPTION)
                    val newId = operationDao.insert(
                        OperationRecordEntity(
                            module = OperationModule.ACHATS.name,
                            reference = ref,
                            title = "Bon de réception — ${payload.supplierName}",
                            counterpart = payload.supplierName,
                            amount = 0.0,
                            direction = OperationDirection.NONE.name,
                            status = OperationStatus.VALIDATED.name,
                            notes = ReceptionCodec.encode(payload),
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
                        return@withTransaction Result.CommandeIntrouvable
                    }
                    operationDao.update(
                        existant.copy(
                            status = OperationStatus.VALIDATED.name,
                            notes = ReceptionCodec.encode(payload),
                        ),
                    )
                    id to existant.reference
                }
            }

            val groupes = groupeDao.listerComplets()
            var articlesRecus = 0
            for (ligne in payload.lignes.filter { it.quantiteRecue > 0.0 }) {
                val produit = productDao.getById(ligne.productId)
                if (produit == null || !produit.active) return@withTransaction Result.DonneesInvalides
                // Un article non stockable ne se réceptionne pas physiquement.
                if (!ReglesGroupesArticles.estStocke(produit, groupes)) continue
                val siteId = produit.siteId
                    ?: stockDao.siteAvecPlusDeStock(ligne.productId)
                    ?: siteDao.idPrincipal()
                    ?: return@withTransaction Result.FournisseurIntrouvable
                val avant = stockDao.quantite(ligne.productId, siteId) ?: 0.0
                stockDao.ensureRow(ligne.productId, siteId)
                stockDao.remplacer(ligne.productId, siteId, avant + ligne.quantiteRecue)
                movementDao.insert(
                    StockMovementEntity(
                        produitId = ligne.productId,
                        siteId = siteId,
                        type = StockMovementType.ENTREE,
                        quantite = ligne.quantiteRecue,
                        motif = "RECEPTION",
                        reference = reference,
                        commentaire = ligne.name,
                        lot = ligne.lot?.trim()?.ifBlank { null },
                        numeroSerie = ligne.numeroSerie?.trim()?.ifBlank { null },
                        datePeremption = ligne.datePeremption,
                        horodatage = now,
                    ),
                )
                // Valorisation au prix d'achat catalogue : le CUMP se recalcule.
                if (ReglesGroupesArticles.estValorise(produit, groupes)) {
                    val cout = (produit.prixAchat ?: 0.0) * ligne.quantiteRecue
                    if (cout > 0.0) stockDao.ajouterValeur(ligne.productId, siteId, cout)
                }
                articlesRecus++
            }

            if (articlesRecus > 0) {
                appNotifier.notifier(
                    type = "QUA",
                    titre = "Réception à contrôler",
                    message = "$reference — ${payload.supplierName} : $articlesRecus article(s) reçu(s)",
                    date = now,
                )
            }
            journalManager.log(
                "ACHATS",
                "RECEPTION_VALIDEE",
                "Réception $reference — ${payload.supplierName} ($articlesRecus article(s)" +
                    (payload.commandeReference?.let { ", commande $it" } ?: "") + ")",
            )
            Result.Succes(recordIdFinal, reference)
        }
    }

    private suspend fun receptionsDe(
        operationDao: OperationRecordDao,
        commandeRecordId: Long,
    ): List<ReceptionPayload> =
        operationDao.getByModule(OperationModule.ACHATS.name)
            .filter { it.status == OperationStatus.VALIDATED.name && it.id != commandeRecordId }
            .mapNotNull { ReceptionCodec.decode(it.notes) }
            .filter { it.commandeRecordId == commandeRecordId }
}

/**
 * Règlement ultérieur d'une facture fournisseur — **transactionnel** :
 * sortie de trésorerie numérotée (jamais deux fois la même référence) et
 * passif réduit d'autant sur la pièce.
 */
class ReglerAchatUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val comptesTresorerieDao: CompteTresorerieDao,
    private val mouvementsTresorerieDao: MouvementTresorerieDao,
    private val appNotifier: AppNotifier,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val referenceMouvement: String) : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
        data object MontantInvalide : Result()
        data object CompteIntrouvable : Result()
        data object DejaEnregistre : Result()
    }

    suspend operator fun invoke(
        factureRecordId: Long,
        montant: Double,
        modePaiement: String,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val facture = operationDao.getById(factureRecordId)
        if (facture == null || facture.module != OperationModule.ACHATS.name ||
            facture.status != OperationStatus.VALIDATED.name
        ) {
            return Result.Introuvable
        }
        val payload = PurchaseRecordCodec.decode(facture.notes) ?: return Result.Introuvable
        if (!AchatCommandeRules.reglementEstValide(payload.total, payload.paidAmount, montant)) {
            return Result.MontantInvalide
        }
        val compte = TresorerieRules.compteCible(modePaiement, comptesTresorerieDao.getAll())
            ?: return Result.CompteIntrouvable

        return database.withTransaction {
            val numero = mouvementsTresorerieDao.compterParReferenceCommencant(facture.reference) + 1
            val referenceMouvement = AchatCommandeRules.referenceReglement(facture.reference, numero)
            if (mouvementsTresorerieDao.compterParReference(referenceMouvement) > 0) {
                return@withTransaction Result.DejaEnregistre
            }
            mouvementsTresorerieDao.insert(
                MouvementTresorerieEntity(
                    compteId = compte.id,
                    date = now,
                    sens = SensMouvement.OUT.name,
                    montant = Math.round(montant * 100.0) / 100.0,
                    categorie = CategorieTresorerie.ACHAT.name,
                    libelle = payload.supplierName,
                    tiers = payload.supplierName,
                    modePaiement = modePaiement,
                    reference = referenceMouvement,
                    createdAt = now,
                ),
            )
            operationDao.update(
                facture.copy(
                    notes = PurchaseRecordCodec.encode(
                        payload.copy(paidAmount = payload.paidAmount + montant),
                    ),
                ),
            )
            appNotifier.notifier(
                type = "ACHATS",
                titre = "Règlement enregistré",
                message = "${facture.reference} — ${payload.supplierName} : $montant",
                date = now,
            )
            journalManager.log(
                "ACHATS",
                "REGLEMENT_ACHAT",
                "Règlement #$numero ${facture.reference} — ${payload.supplierName} ($montant via $modePaiement)",
            )
            Result.Succes(referenceMouvement)
        }
    }
}

/**
 * Annulation d'une pièce d'achat — **contre-passation transactionnelle**,
 * jamais de suppression :
 *
 * - facture directe : sorties de stock (retour fournisseur) + valeur CUMP décrémentée
 *   + remboursement trésorerie si elle était réglée ;
 * - réception : sorties de stock symétriques (interdite si une facture non annulée
 *   y est rattachée) ;
 * - commande : simple marquage (interdite si des réception(s) validée(s) existent).
 */
class AnnulerAchatUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val siteDao: SiteDao,
    private val groupeDao: GroupeArticleDao,
    private val comptesTresorerieDao: CompteTresorerieDao,
    private val mouvementsTresorerieDao: MouvementTresorerieDao,
    private val appNotifier: AppNotifier,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
        data object DejaAnnulee : Result()
        data object StockInsuffisant : Result()
        data object FactureLiee : Result()
        data object ReceptionLiee : Result()
    }

    suspend operator fun invoke(recordId: Long, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val piece = operationDao.getById(recordId)
        if (piece == null || piece.module != OperationModule.ACHATS.name) return Result.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Result.DejaAnnulee
        if (piece.status != OperationStatus.VALIDATED.name) return Result.Introuvable

        val achat = PurchaseRecordCodec.decode(piece.notes)
        val reception = if (achat == null) ReceptionCodec.decode(piece.notes) else null
        val commande = if (achat == null && reception == null) CommandeAchatCodec.decode(piece.notes) else null
        if (achat == null && reception == null && commande == null) return Result.Introuvable

        return database.withTransaction {
            val groupes = groupeDao.listerComplets()
            val pieces = operationDao.getByModule(OperationModule.ACHATS.name)

            if (reception != null) {
                // Une réception facturée ne s'annule pas : annuler d'abord la facture.
                val factureLiee = pieces.any {
                    it.status == OperationStatus.VALIDATED.name &&
                        PurchaseRecordCodec.decode(it.notes)?.receptionRecordId == piece.id
                }
                if (factureLiee) return@withTransaction Result.FactureLiee
                for (ligne in reception.lignes.filter { it.quantiteRecue > 0.0 }) {
                    val contre = contrePasser(
                        ligne = ligne.productId to ligne.quantiteRecue,
                        motif = "ANNULATION_RECEPTION",
                        reference = piece.reference,
                        coutUnitaire = productDao.getById(ligne.productId)?.prixAchat ?: 0.0,
                        groupes = groupes,
                        now = now,
                    )
                    if (contre == null) return@withTransaction Result.Introuvable
                    if (!contre) return@withTransaction Result.StockInsuffisant
                }
            }

            if (achat != null && achat.receptionRecordId == null) {
                // Facture directe : ce qui était entré ressort.
                val couts = PurchaseStockEffects.coutParProduit(achat.lines)
                for ((produitId, quantite) in PurchaseStockEffects.besoinsParProduit(achat.lines)) {
                    val coutUnitaire = (couts[produitId] ?: 0.0) / quantite
                    val contre = contrePasser(
                        ligne = produitId to quantite,
                        motif = "ANNULATION_ACHAT",
                        reference = piece.reference,
                        coutUnitaire = coutUnitaire,
                        groupes = groupes,
                        now = now,
                    )
                    if (contre == null) return@withTransaction Result.Introuvable
                    if (!contre) return@withTransaction Result.StockInsuffisant
                }
            }

            if (commande != null) {
                val receptionLiee = pieces.any {
                    it.status == OperationStatus.VALIDATED.name &&
                        ReceptionCodec.decode(it.notes)?.commandeRecordId == piece.id
                }
                if (receptionLiee) return@withTransaction Result.ReceptionLiee
            }

            // Remboursement : ce qui était réglé rentre en trésorerie, une seule fois.
            if (achat != null && achat.paidAmount > 0.0) {
                val referenceRetour = AchatCommandeRules.referenceAnnulation(piece.reference)
                val compte = TresorerieRules.compteCible(
                    achat.paymentMethod,
                    comptesTresorerieDao.getAll(),
                )
                if (compte != null && mouvementsTresorerieDao.compterParReference(referenceRetour) == 0) {
                    mouvementsTresorerieDao.insert(
                        MouvementTresorerieEntity(
                            compteId = compte.id,
                            date = now,
                            sens = SensMouvement.IN.name,
                            montant = achat.paidAmount,
                            categorie = CategorieTresorerie.ACHAT.name,
                            libelle = achat.supplierName,
                            tiers = achat.supplierName,
                            modePaiement = achat.paymentMethod,
                            reference = referenceRetour,
                            createdAt = now,
                        ),
                    )
                }
            }

            operationDao.update(piece.copy(status = OperationStatus.CANCELLED.name))
            appNotifier.notifier(
                type = "ACHATS",
                titre = "Pièce annulée",
                message = "${piece.reference} — ${piece.counterpart}",
                date = now,
            )
            journalManager.log(
                "ACHATS",
                "ANNULATION_ACHAT",
                "Annulation ${piece.reference} — ${piece.counterpart} (contre-passation)",
            )
            Result.Succes
        }
    }

    /**
     * Sortie symétrique d'une réception : false si le stock disponible est
     * insuffisant (refus), true si appliquée. Les articles non stockables
     * (prestations facturées directement) sont ignorés.
     */
    private suspend fun contrePasser(
        ligne: Pair<Long, Double>,
        motif: String,
        reference: String,
        coutUnitaire: Double,
        groupes: List<com.missa.b360.core.data.dao.GroupeArticleComplet>,
        now: Long,
    ): Boolean? {
        val (produitId, quantite) = ligne
        if (quantite <= 0.0) return true
        val produit = productDao.getById(produitId) ?: return null
        if (!ReglesGroupesArticles.estStocke(produit, groupes)) return true
        val siteId = produit.siteId
            ?: stockDao.siteAvecPlusDeStock(produitId)
            ?: siteDao.idPrincipal()
            ?: return null
        val avant = stockDao.quantite(produitId, siteId) ?: 0.0
        if (avant < quantite - 1e-9) return false
        stockDao.remplacer(produitId, siteId, avant - quantite)
        movementDao.insert(
            StockMovementEntity(
                produitId = produitId,
                siteId = siteId,
                type = StockMovementType.SORTIE,
                quantite = quantite,
                motif = motif,
                reference = reference,
                horodatage = now,
            ),
        )
        if (ReglesGroupesArticles.estValorise(produit, groupes)) {
            val valeur = coutUnitaire * quantite
            if (valeur > 0.0) stockDao.ajouterValeur(produitId, siteId, -valeur)
        }
        return true
    }
}
