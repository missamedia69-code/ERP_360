package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.DevisCommandeRules
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject
import kotlin.math.abs

/**
 * Cycle commercial devis → commande → facture (spec §20).
 *
 * **Aucun effet stock, aucun effet trésorerie** : un devis ou une commande est
 * une pièce commerciale. Seule la conversion commande → facture déclenche les
 * mouvements (via [SaveSaleUseCase], qui garde l'autorité sur le stock).
 * Aucune écriture de caisse non plus — le paiement de la facture est sa propre
 * opération (spec §6/§44).
 */
class SaveDevisCommandeUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val clientDao: ClientDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object ClientIntrouvable : Result()
        data object PiecIntrouvable : Result()
    }

    /** module : [OperationModule.DEVIS] ou [OperationModule.COMMANDE]. */
    suspend operator fun invoke(
        module: OperationModule,
        recordId: Long?,
        payload: SaleRecordPayload,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lines.isEmpty() || payload.total <= 0.0) return Result.DonneesInvalides
        val subtotal = payload.lines.sumOf { it.total }.coerceAtLeast(0.0)
        if (abs(subtotal - payload.total) > 0.01) return Result.DonneesInvalides
        if (clientDao.getById(payload.clientId) == null) return Result.ClientIntrouvable

        val prefix = if (module == OperationModule.DEVIS) "Devis" else "Commande"
        val docType = if (module == OperationModule.DEVIS) DocType.DEVIS else DocType.COMMANDE_CLIENT

        if (recordId == null) {
            val reference = sequenceManager.next(docType)
            val id = operationDao.insert(
                OperationRecordEntity(
                    module = module.name,
                    reference = reference,
                    title = "$prefix $reference — ${payload.clientName}",
                    counterpart = payload.clientName,
                    amount = payload.total,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = SaleRecordCodec.encode(payload),
                    createdAt = now,
                ),
            )
            journalManager.log(
                module.name,
                if (module == OperationModule.DEVIS) "DEVIS_CREER" else "COMMANDE_CREER",
                "$prefix $reference — ${payload.clientName} (${payload.total})",
            )
            return Result.Succes(id, reference)
        }

        val existant = operationDao.getById(recordId)
        if (existant == null || existant.module != module.name ||
            existant.status == OperationStatus.CANCELLED.name
        ) {
            return Result.PiecIntrouvable
        }
        operationDao.update(
            existant.copy(
                title = "$prefix ${existant.reference} — ${payload.clientName}",
                counterpart = payload.clientName,
                amount = payload.total,
                notes = SaleRecordCodec.encode(payload),
            ),
        )
        journalManager.log(module.name, "PIECE_MODIFIEE", "${existant.reference} mis à jour")
        return Result.Succes(recordId, existant.reference)
    }
}

/**
 * Conversion devis → commande (spec §20) : la commande reprend exactement le
 * contenu du devis (même client, mêmes lignes, mêmes montants), liée par
 * `sourceRecordId`. Transactionnelle, journalisée.
 */
class ConvertDevisToOrderUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
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
        data object DonneesInvalides : Result()
    }

    suspend operator fun invoke(
        devisRecordId: Long,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        return database.withTransaction {
            val devis = operationDao.getById(devisRecordId)
            if (devis == null || devis.module != OperationModule.DEVIS.name) {
                return@withTransaction Result.Introuvable
            }
            if (devis.status == OperationStatus.CANCELLED.name) {
                return@withTransaction Result.DejaAnnulee
            }
            val payload = SaleRecordCodec.decode(devis.notes)
                ?: return@withTransaction Result.DonneesInvalides

            val reference = sequenceManager.next(DocType.COMMANDE_CLIENT)
            val commandeId = operationDao.insert(
                OperationRecordEntity(
                    module = OperationModule.COMMANDE.name,
                    reference = reference,
                    title = "Commande $reference — ${payload.clientName}",
                    counterpart = payload.clientName,
                    amount = payload.total,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = SaleRecordCodec.encode(
                        DevisCommandeRules.payloadCopie(payload, sourceRecordId = devisRecordId),
                    ),
                    createdAt = now,
                ),
            )
            journalManager.log(
                OperationModule.COMMANDE.name,
                "DEVIS_CONVERTI",
                "$reference créée depuis ${devis.reference}",
            )
            Result.Succes(commandeId, reference)
        }
    }
}

/**
 * Conversion commande → facture (spec §20) : délègue à [SaveSaleUseCase] qui
 * reste l'autorité transactionnelle sur le stock, les mouvements et la caisse.
 * La commande déjà facturationnée (pièce de vente rattachée via
 * `sourceRecordId`) refuse la double facturation.
 */
class ConvertOrderToSaleUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val saveSale: SaveSaleUseCase,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
        data object DejaAnnulee : Result()
        data object DejaFacturee : Result()
        data object DonneesInvalides : Result()
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : Result()
    }

    suspend operator fun invoke(
        commandeRecordId: Long,
        paymentMethod: String,
        paidAmount: Double,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (paymentMethod.isBlank() || paidAmount < 0.0) return Result.DonneesInvalides

        return database.withTransaction {
            val commande = operationDao.getById(commandeRecordId)
            if (commande == null || commande.module != OperationModule.COMMANDE.name) {
                return@withTransaction Result.Introuvable
            }
            if (commande.status == OperationStatus.CANCELLED.name) {
                return@withTransaction Result.DejaAnnulee
            }
            val payload = SaleRecordCodec.decode(commande.notes)
                ?: return@withTransaction Result.DonneesInvalides
            if (paidAmount > payload.total + QUANTITE_EPSILON) {
                return@withTransaction Result.DonneesInvalides
            }

            // Interdiction de double facturation (spec §20).
            val ventes = operationDao.getByModule(OperationModule.VENTE.name)
                .mapNotNull { SaleRecordCodec.decode(it.notes) }
            if (DevisCommandeRules.estFacturee(ventes, commandeRecordId)) {
                return@withTransaction Result.DejaFacturee
            }

            val facturation = saveSale(
                recordId = null,
                payload = DevisCommandeRules.payloadCopie(
                    payload.copy(paymentMethod = paymentMethod, paidAmount = paidAmount),
                    sourceRecordId = commandeRecordId,
                ),
                draft = false,
                now = now,
            )
            when (facturation) {
                is SaveSaleUseCase.Result.Succes -> {
                    journalManager.log(
                        OperationModule.COMMANDE.name,
                        "COMMANDE_FACTUREE",
                        "${commande.reference} → ${facturation.reference}",
                    )
                    Result.Succes(facturation.recordId, facturation.reference)
                }
                SaveSaleUseCase.Result.LectureSeule -> Result.LectureSeule
                SaveSaleUseCase.Result.DonneesInvalides -> Result.DonneesInvalides
                SaveSaleUseCase.Result.BrouillonIntrouvable -> Result.DonneesInvalides
                is SaveSaleUseCase.Result.StockInsuffisant ->
                    Result.StockInsuffisant(facturation.produitNom, facturation.disponible, facturation.demande)
            }
        }
    }
}
