package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import javax.inject.Inject

/**
 * Rappel de paiement client (spec §22) — pièce FINANCES **direction NONE** :
 * trace le relance dans l'historique financier sans aucun effet de trésorerie
 * (le règlement effectif reste une opération Finance IN/OUT dédiée).
 */
class RappelPaiementUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val clientDao: ClientDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String, val solde: Double) : Result()
        data object LectureSeule : Result()
        data object ClientIntrouvable : Result()
        data object AucunSolde : Result()
    }

    /** Solde dû par le client — même règle que l'écran Ventes (`outstandingBalance`). */
    suspend fun soldeClient(clientId: Long): Double = operationDao.getByModule(OperationModule.VENTE.name)
        .asSequence()
        .filter { it.status == OperationStatus.VALIDATED.name }
        .mapNotNull { SaleRecordCodec.decode(it.notes) }
        .filter { it.clientId == clientId }
        .sumOf {
            val partiel = (it.total - it.paidAmount).coerceAtLeast(0.0)
            if (it.sourceRecordId != null) -partiel else partiel
        }

    suspend operator fun invoke(clientId: Long, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val client = clientDao.getById(clientId) ?: return Result.ClientIntrouvable

        return database.withTransaction {
            // Solde client : factures validées − payé, moins les avoirs (même règle
            // que l'écran Ventes — `outstandingBalance`).
            val solde = operationDao.getByModule(OperationModule.VENTE.name)
                .asSequence()
                .filter { it.status == OperationStatus.VALIDATED.name }
                .mapNotNull { SaleRecordCodec.decode(it.notes) }
                .filter { it.clientId == clientId }
                .sumOf {
                    val partiel = (it.total - it.paidAmount).coerceAtLeast(0.0)
                    if (it.sourceRecordId != null) -partiel else partiel
                }
            if (solde <= 0.001) return@withTransaction Result.AucunSolde

            val reference = sequenceManager.next(DocType.RAPPEL)
            val id = operationDao.insert(
                OperationRecordEntity(
                    module = OperationModule.FINANCES.name,
                    reference = reference,
                    title = "Rappel paiement — ${client.nom}",
                    counterpart = client.nom,
                    amount = solde,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = "Rappel de paiement — solde client $solde",
                    createdAt = now,
                ),
            )
            journalManager.log(
                OperationModule.FINANCES.name,
                "RAPPEL_PAIEMENT",
                "$reference — ${client.nom} (solde $solde)",
            )
            Result.Succes(id, reference, solde)
        }
    }
}
