package com.missa.b360.core.domain.usecase
import androidx.room.withTransaction

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
import kotlin.math.max

/**
 * Cycle commercial (spec §20) : devis → commande → facture, pour les ventes
 * (D/C) ET les prestations de service (DP/OS).
 *
 * **Aucun effet stock, aucun effet trésorerie** : ce sont des pièces
 * commerciales. Seule la conversion commande → facture déclenche les
 * mouvements (via [SaveSaleUseCase], autorité sur le stock ; les lignes de
 * prestation sans produit du catalogue n'ont d'ailleurs aucun effet).
 */

/** Famille commerciale — dérive le module, le type de document et le libellé. */
sealed interface CommercialTarget {
    val module: OperationModule
    val docType: DocType
    val label: String
    val isDevis: Boolean

    data object Devis : CommercialTarget {
        override val module = OperationModule.DEVIS
        override val docType = DocType.DEVIS
        override val label = "Devis"
        override val isDevis = true
    }
    data object Commande : CommercialTarget {
        override val module = OperationModule.COMMANDE
        override val docType = DocType.COMMANDE_CLIENT
        override val label = "Commande"
        override val isDevis = false
    }
    data object DevisPrestation : CommercialTarget {
        override val module = OperationModule.SERVICES
        override val docType = DocType.DEVIS_PRESTATION
        override val label = "Devis prestation"
        override val isDevis = true
    }
    data object CommandePrestation : CommercialTarget {
        override val module = OperationModule.SERVICES
        override val docType = DocType.ORDRE_SERVICE
        override val label = "Commande prestation"
        override val isDevis = false
    }
}

/** Résolution de la cible d'une pièce à partir de sa pièce stockée. */
object CommercialTargets {
    fun fromRecord(record: OperationRecordEntity): CommercialTarget? = when (record.module) {
        OperationModule.DEVIS.name -> CommercialTarget.Devis
        OperationModule.COMMANDE.name -> CommercialTarget.Commande
        OperationModule.SERVICES.name -> when {
            record.reference.startsWith("DP") -> CommercialTarget.DevisPrestation
            record.reference.startsWith("OS") -> CommercialTarget.CommandePrestation
            else -> null
        }
        else -> null
    }

    fun fromTarget(target: CommercialTarget): CommercialTarget = target
}

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

    suspend operator fun invoke(
        target: CommercialTarget,
        recordId: Long?,
        payload: SaleRecordPayload,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (payload.lines.isEmpty() || payload.total <= 0.0) return Result.DonneesInvalides
        // Même règle que SaleCalculator (prix TTC) : total = max(0, sous-total − remise + livraison).
        val subtotal = payload.lines.sumOf { it.total }.coerceAtLeast(0.0)
        val attendu = max(
            0.0,
            subtotal - payload.discount.coerceIn(0.0, subtotal) + payload.delivery.coerceAtLeast(0.0),
        )
        if (abs(attendu - payload.total) > 0.01) return Result.DonneesInvalides
        if (clientDao.getById(payload.clientId) == null) return Result.ClientIntrouvable

        if (recordId == null) {
            val reference = sequenceManager.next(target.docType)
            val id = operationDao.insert(
                OperationRecordEntity(
                    module = target.module.name,
                    reference = reference,
                    title = "${target.label} $reference — ${payload.clientName}",
                    counterpart = payload.clientName,
                    amount = payload.total,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = SaleRecordCodec.encode(payload),
                    createdAt = now,
                ),
            )
            journalManager.log(
                target.module.name,
                if (target.isDevis) "DEVIS_CREER" else "COMMANDE_CREER",
                "${target.label} $reference — ${payload.clientName} (${payload.total})",
            )
            return Result.Succes(id, reference)
        }

        val existant = operationDao.getById(recordId)
        val cible = CommercialTargets.fromRecord(existant ?: return Result.PiecIntrouvable)
        if (existant == null || cible != target || existant.status == OperationStatus.CANCELLED.name) {
            return Result.PiecIntrouvable
        }
        operationDao.update(
            existant.copy(
                title = "${target.label} ${existant.reference} — ${payload.clientName}",
                counterpart = payload.clientName,
                amount = payload.total,
                notes = SaleRecordCodec.encode(payload),
            ),
        )
        journalManager.log(target.module.name, "PIECE_MODIFIEE", "${existant.reference} mis à jour")
        return Result.Succes(recordId, existant.reference)
    }
}

/**
 * Conversion devis → commande (ventes ET prestations) : la commande reprend
 * exactement le contenu du devis, liée par `sourceRecordId`.
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

    /**
     * @param targetDevis la famille source (ventes ou prestations).
     */
    suspend operator fun invoke(
        devisRecordId: Long,
        targetDevis: CommercialTarget,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val targetOrder: CommercialTarget = if (targetDevis == CommercialTarget.Devis) {
            CommercialTarget.Commande
        } else {
            CommercialTarget.CommandePrestation
        }

        return database.withTransaction {
            val devis = operationDao.getById(devisRecordId)
            if (CommercialTargets.fromRecord(devis ?: return@withTransaction Result.Introuvable) != targetDevis) {
                return@withTransaction Result.Introuvable
            }
            if (devis.status == OperationStatus.CANCELLED.name) {
                return@withTransaction Result.DejaAnnulee
            }
            val payload = SaleRecordCodec.decode(devis.notes)
                ?: return@withTransaction Result.DonneesInvalides

            val reference = sequenceManager.next(targetOrder.docType)
            val commandeId = operationDao.insert(
                OperationRecordEntity(
                    module = targetOrder.module.name,
                    reference = reference,
                    title = "${targetOrder.label} $reference — ${payload.clientName}",
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
                targetOrder.module.name,
                "DEVIS_CONVERTI",
                "$reference créée depuis ${devis.reference}",
            )
            Result.Succes(commandeId, reference)
        }
    }
}

/**
 * Conversion commande → facture (ventes ET prestations) : délègue à
 * [SaveSaleUseCase] (autorité transactionnelle sur stock + caisse). La
 * commande déjà facturationnée (pièce de vente rattachée via
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
            val cible = CommercialTargets.fromRecord(commande ?: return@withTransaction Result.Introuvable)
            // Seules les commandes (ventes ou prestations) sont facturables.
            if (cible != CommercialTarget.Commande && cible != CommercialTarget.CommandePrestation) {
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
                        cible.module.name,
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
