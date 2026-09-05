package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Règles communes aux pièces opérationnelles : validation, licence lecture seule, référence
 * atomique et journalisation. Les modules gardent ainsi un comportement cohérent offline.
 */
class OperationUseCases @Inject constructor(
    private val dao: OperationRecordDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    data class CreateParams(
        val module: OperationModule,
        val title: String,
        val counterpart: String? = null,
        val amount: Double? = null,
        val quantity: Double? = null,
        val direction: OperationDirection = OperationDirection.NONE,
        val notes: String? = null,
    )

    sealed class CreateResult {
        data class Success(val id: Long, val reference: String) : CreateResult()
        data object ReadOnly : CreateResult()
        data object Invalid : CreateResult()
    }

    sealed class UpdateDraftResult {
        data class Success(val id: Long, val reference: String) : UpdateDraftResult()
        data object ReadOnly : UpdateDraftResult()
        data object Invalid : UpdateDraftResult()
        data object NotDraft : UpdateDraftResult()
    }

    fun observe(module: OperationModule): Flow<List<OperationRecordEntity>> =
        dao.observeByModule(module.name)

    fun observeAll(): Flow<List<OperationRecordEntity>> = dao.observeAll()

    suspend fun create(params: CreateParams): CreateResult {
        if (!OperationValidation.isValid(params)) return CreateResult.Invalid
        val title = params.title.trim()
        val amount = params.amount
        val quantity = params.quantity
        if (licenceManager.isReadOnly()) return CreateResult.ReadOnly

        val reference = sequenceManager.next(params.module.docType())
        val id = dao.insert(
            OperationRecordEntity(
                module = params.module.name,
                reference = reference,
                title = title,
                counterpart = params.counterpart?.trim()?.ifBlank { null },
                amount = amount,
                quantity = quantity,
                direction = if (params.module == OperationModule.FINANCES) {
                    params.direction.name
                } else {
                    OperationDirection.NONE.name
                },
                notes = params.notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            ),
        )
        journalManager.log(
            module = params.module.name,
            action = "CREATION_PIECE",
            details = "${params.module.name} $reference — $title",
        )
        return CreateResult.Success(id, reference)
    }

    /**
     * Met à jour un brouillon existant sans changer sa référence. Les pièces validées ou
     * annulées restent immuables : une correction doit passer par une nouvelle pièce.
     */
    suspend fun updateDraft(id: Long, params: CreateParams): UpdateDraftResult {
        if (!OperationValidation.isValid(params)) return UpdateDraftResult.Invalid
        if (licenceManager.isReadOnly()) return UpdateDraftResult.ReadOnly
        val existing = dao.getById(id) ?: return UpdateDraftResult.NotDraft
        if (existing.status != OperationStatus.DRAFT.name || existing.module != params.module.name) {
            return UpdateDraftResult.NotDraft
        }
        val title = params.title.trim()
        dao.update(
            existing.copy(
                title = title,
                counterpart = params.counterpart?.trim()?.ifBlank { null },
                amount = params.amount,
                quantity = params.quantity,
                direction = if (params.module == OperationModule.FINANCES) {
                    params.direction.name
                } else {
                    OperationDirection.NONE.name
                },
                notes = params.notes?.trim()?.ifBlank { null },
            ),
        )
        journalManager.log(
            module = params.module.name,
            action = "MODIFICATION_BROUILLON",
            details = "${params.module.name} ${existing.reference} — $title",
        )
        return UpdateDraftResult.Success(existing.id, existing.reference)
    }

    /** Validation ou annulation compensatoire sans supprimer la pièce créée. */
    suspend fun setStatus(id: Long, status: OperationStatus): Boolean {
        if (licenceManager.isReadOnly()) return false
        val record = dao.getById(id) ?: return false
        if (record.status == status.name) return true
        dao.update(record.copy(status = status.name))
        journalManager.log(
            module = record.module,
            action = "STATUT_PIECE",
            details = "${record.reference} → ${status.name}",
        )
        return true
    }

    private fun OperationModule.docType(): DocType = when (this) {
        OperationModule.STOCK -> DocType.INVENTAIRE
        OperationModule.VENTE -> DocType.FACTURE
        OperationModule.DEVIS -> DocType.DEVIS
        OperationModule.COMMANDE -> DocType.COMMANDE_CLIENT
        OperationModule.ACHATS -> DocType.FACTURE_FOURNISSEUR
        OperationModule.FINANCES -> DocType.TICKET
        OperationModule.LIVRAISON -> DocType.LIVRAISON
        OperationModule.PRODUCTION -> DocType.ORDRE_PRODUCTION
        OperationModule.SERVICES -> DocType.ORDRE_SERVICE
        OperationModule.RH -> DocType.BULLETIN_PAIE
        OperationModule.PROJETS -> DocType.PROJET
    }
}

/** Validation déterministe réutilisable et couverte par les tests unitaires des modules. */
object OperationValidation {
    fun isValid(params: OperationUseCases.CreateParams): Boolean {
        val title = params.title.trim()
        val amount = params.amount
        val quantity = params.quantity
        val requiresAmount = params.module in setOf(
            OperationModule.VENTE,
            OperationModule.ACHATS,
            OperationModule.FINANCES,
            OperationModule.RH,
        )
        val requiresQuantity = params.module in setOf(
            OperationModule.STOCK,
            OperationModule.LIVRAISON,
            OperationModule.PRODUCTION,
            OperationModule.SERVICES,
        )
        return title.length in 2..160 &&
            amount?.isFinite() != false && quantity?.isFinite() != false &&
            amount?.let { it >= 0.0 } != false && quantity?.let { it > 0.0 } != false &&
            (!requiresAmount || (amount != null && amount > 0.0)) &&
            (!requiresQuantity || quantity != null) &&
            (params.module != OperationModule.FINANCES || params.direction != OperationDirection.NONE)
    }
}
