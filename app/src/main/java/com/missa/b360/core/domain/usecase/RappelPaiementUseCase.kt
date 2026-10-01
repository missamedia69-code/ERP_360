package com.missa.b360.core.domain.usecase
import androidx.room.withTransaction

import com.missa.b360.core.data.dao.ClientBalanceDao
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
    private val balanceDao: ClientBalanceDao,
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

    /** Solde dû par le client : encours de `client_balances`, la source de la liste, de la fiche et du compte. */
    suspend fun soldeClient(clientId: Long): Double = balanceDao.get(clientId)?.encours ?: 0.0

    suspend operator fun invoke(clientId: Long, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val client = clientDao.getById(clientId) ?: return Result.ClientIntrouvable

        return database.withTransaction {
            val solde = soldeClient(clientId)
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
