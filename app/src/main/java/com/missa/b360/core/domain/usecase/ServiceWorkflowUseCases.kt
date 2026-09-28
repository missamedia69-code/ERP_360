package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.ServiceWorkflowDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.CustomerServiceAssetEntity
import com.missa.b360.core.data.entity.ServiceAttachmentEntity
import com.missa.b360.core.data.entity.ServiceContractEntity
import com.missa.b360.core.data.entity.ServiceReportEntity
import com.missa.b360.core.data.entity.ServiceRequestEntity
import com.missa.b360.core.data.entity.ServiceTimesheetEntity
import com.missa.b360.core.data.entity.ServiceWorkOrderEntity
import com.missa.b360.core.domain.model.ServiceBillingMethod
import com.missa.b360.core.domain.model.ServicePriority
import com.missa.b360.core.domain.model.ServiceReportStatus
import com.missa.b360.core.domain.model.ServiceRequestChannel
import com.missa.b360.core.domain.model.ServiceRequestStatus
import com.missa.b360.core.domain.model.ServiceRequestType
import com.missa.b360.core.domain.model.ServiceWorkOrderStatus
import com.missa.b360.core.domain.model.ServiceWorkflowRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import com.missa.b360.core.permissions.PermissionChecker
import com.missa.b360.core.data.repository.ProfilActivationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Services owns requests, contracts, dispatch, field reports and service history.
 * This class deliberately has no Stock DAO dependency: stock reservations/movements
 * must be delegated to the Stock module's public use cases in a later integration.
 */
class ServiceWorkflowUseCases @Inject constructor(
    private val dao: ServiceWorkflowDao,
    private val employeeDao: EmployeeDao,
    private val absenceDao: AbsenceDao,
    private val userDao: UserDao,
    private val settings: SettingsStore,
    private val permissions: PermissionChecker,
    private val activation: ProfilActivationRepository,
    private val database: AppDatabase,
    private val sequence: SequenceManager,
    private val licence: LicenceManager,
    private val journal: JournalManager,
) {
    companion object {
        const val MODULE = "SERVICES"
        private const val DAY_MS = 86_400_000L
    }

    sealed interface Result {
        data class Success(val id: Long, val reference: String? = null) : Result
        data object Invalid : Result
        data object NotFound : Result
        data object Forbidden : Result
        data object ReadOnly : Result
        data object ModuleInactive : Result
        data object InvalidTransition : Result
        data object ScheduleConflict : Result
        data object TechnicianUnavailable : Result
        data object SignatureRequired : Result
        data object SelfApprovalNotAllowed : Result
    }

    fun observeRequests(): Flow<List<ServiceRequestEntity>> = dao.observeRequests()
    fun observeWorkOrders(): Flow<List<ServiceWorkOrderEntity>> = dao.observeWorkOrders()
    fun observeEmployees() = employeeDao.observeActifs()
    fun observeContracts(): Flow<List<ServiceContractEntity>> = dao.observeContracts()
    fun observeReports(): Flow<List<ServiceReportEntity>> = dao.observeReports()
    fun observeTimesheets(workOrderId: Long) = dao.observeTimesheets(workOrderId)
    fun observeAttachments(workOrderId: Long) = dao.observeAttachments(workOrderId)

    suspend fun createRequest(
        customerId: Long,
        customerName: String,
        description: String,
        requestType: ServiceRequestType,
        priority: ServicePriority = ServicePriority.NORMAL,
        channel: ServiceRequestChannel = ServiceRequestChannel.PHONE,
        contactName: String? = null,
        contactPhone: String? = null,
        address: String? = null,
        customerAssetId: Long? = null,
        contractId: Long? = null,
        requestedAt: Long? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.CREATE) ?: return denied()
        val customerSnapshot = customerName.trim()
        if (!ServiceWorkflowRules.requestIsValid(customerId, description) || customerSnapshot.isBlank()) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val contract = contractId?.let { dao.getContract(it) ?: return Result.NotFound }
        if (contract != null && (contract.customerId != customerId || !contract.isActiveAt(now))) return Result.Invalid
        val asset = customerAssetId?.let { dao.getAsset(it) ?: return Result.NotFound }
        if (asset != null && (asset.customerId != customerId || asset.status != "ACTIF" || (contractId != null && asset.contractId != null && asset.contractId != contractId))) return Result.Invalid

        val ref = sequence.next(DocType.DEVIS_PRESTATION)
        val request = ServiceRequestEntity(
            reference = ref,
            customerId = customerId,
            customerName = customerSnapshot,
            contactName = contactName.cleanOrNull(),
            contactPhone = contactPhone.cleanOrNull(),
            address = address.cleanOrNull(),
            requestType = requestType.name,
            channel = channel.name,
            priority = priority.name,
            description = description.trim(),
            customerAssetId = customerAssetId,
            contractId = contractId,
            requestedAt = requestedAt,
            responseDeadlineAt = ServiceWorkflowRules.responseDeadline(now, contract?.responseSlaMinutes),
            resolutionDeadlineAt = ServiceWorkflowRules.resolutionDeadline(now, contract?.resolutionSlaMinutes),
            createdAt = now,
            createdBy = actor,
        )
        val id = dao.insertRequest(request)
        journal.log(MODULE, "DEMANDE_CREEE", "$ref — ${description.trim()}", actor, now)
        return Result.Success(id, ref)
    }

    suspend fun qualifyRequest(requestId: Long, needsQuote: Boolean = false, now: Long = System.currentTimeMillis()): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val request = dao.getRequest(requestId) ?: return@withTransaction Result.NotFound
            if (request.status != ServiceRequestStatus.OPEN.name) return@withTransaction Result.InvalidTransition
            val status = if (needsQuote) ServiceRequestStatus.QUOTE_REQUIRED else ServiceRequestStatus.QUALIFIED
            dao.updateRequest(request.copy(status = status.name, qualifiedAt = now))
            Result.Success(request.id, request.reference)
        }
        if (result is Result.Success) journal.log(MODULE, "DEMANDE_QUALIFIEE", "${result.reference}${if (needsQuote) " — devis requis" else ""}", actor, now)
        return result
    }

    /** Conversion atomique de la demande en ordre d'intervention. */
    suspend fun convertRequest(requestId: Long, now: Long = System.currentTimeMillis()): Result {
        val actor = actor(PermissionChecker.Action.CREATE) ?: return denied()
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val request = dao.getRequest(requestId) ?: return Result.NotFound
        val requestStatus = enumOrNull<ServiceRequestStatus>(request.status) ?: return Result.Invalid
        if (!ServiceWorkflowRules.requestMayConvert(requestStatus) || request.convertedWorkOrderId != null) return Result.InvalidTransition
        val ref = sequence.next(DocType.ORDRE_SERVICE)
        val id = database.withTransaction {
            // Relire dans la transaction pour éviter deux conversions concurrentes.
            val fresh = dao.getRequest(requestId) ?: return@withTransaction null
            if (fresh.convertedWorkOrderId != null || !ServiceWorkflowRules.requestMayConvert(enumOrNull(fresh.status) ?: return@withTransaction null)) return@withTransaction null
            val workOrderId = dao.insertWorkOrder(
                ServiceWorkOrderEntity(
                    reference = ref,
                    requestId = fresh.id,
                    customerId = fresh.customerId,
                    customerName = fresh.customerName,
                    customerAssetId = fresh.customerAssetId,
                    contractId = fresh.contractId,
                    interventionType = fresh.requestType,
                    priority = fresh.priority,
                    status = ServiceWorkOrderStatus.TO_PLAN.name,
                    responseDeadlineAt = fresh.responseDeadlineAt,
                    resolutionDeadlineAt = fresh.resolutionDeadlineAt,
                    address = fresh.address,
                    description = fresh.description,
                    isBillable = fresh.requestType != ServiceRequestType.WARRANTY.name,
                    billingMethod = if (fresh.contractId != null) ServiceBillingMethod.CONTRACT.name else ServiceBillingMethod.MIXED.name,
                    createdAt = now,
                    createdBy = actor,
                    updatedAt = now,
                ),
            )
            dao.updateRequest(fresh.copy(status = ServiceRequestStatus.CONVERTED.name, convertedWorkOrderId = workOrderId))
            workOrderId
        } ?: return Result.InvalidTransition
        journal.log(MODULE, "DEMANDE_CONVERTIE", "${request.reference} → $ref", actor, now)
        return Result.Success(id, ref)
    }

    suspend fun createWorkOrder(
        customerId: Long,
        customerName: String,
        description: String,
        interventionType: ServiceRequestType = ServiceRequestType.OTHER,
        priority: ServicePriority = ServicePriority.NORMAL,
        address: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.CREATE) ?: return denied()
        if (!ServiceWorkflowRules.requestIsValid(customerId, description) || customerName.isBlank()) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val reference = sequence.next(DocType.ORDRE_SERVICE)
        val id = dao.insertWorkOrder(
            ServiceWorkOrderEntity(
                reference = reference,
                customerId = customerId,
                customerName = customerName.trim(),
                interventionType = interventionType.name,
                priority = priority.name,
                address = address.cleanOrNull(),
                description = description.trim(),
                createdAt = now,
                createdBy = actor,
                updatedAt = now,
            ),
        )
        journal.log(MODULE, "ORDRE_CREE", "$reference — ${description.trim()}", actor, now)
        return Result.Success(id, reference)
    }

    /** Affectation et réservation du créneau ; aucun mouvement de stock n'est écrit ici. */
    suspend fun schedule(
        workOrderId: Long,
        technicianId: Long,
        startAt: Long,
        endAt: Long,
        teamName: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        if (!ServiceWorkflowRules.scheduleIsValid(startAt, endAt, now)) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val order = dao.getWorkOrder(workOrderId) ?: return@withTransaction Result.NotFound
            if (ServiceWorkflowRules.status(order.status) != ServiceWorkOrderStatus.TO_PLAN) return@withTransaction Result.InvalidTransition
            val technician = employeeDao.getById(technicianId) ?: return@withTransaction Result.NotFound
            if (technician.statut != "ACTIF") return@withTransaction Result.TechnicianUnavailable
            if (dao.countOverlappingAssignments(technicianId, startAt, endAt, workOrderId) > 0) return@withTransaction Result.ScheduleConflict
            val overlapWithAbsence = absenceDao.byEmployee(technicianId).any { absence ->
                val absenceStart = absence.dateDebut
                val duration = (absence.dureeJours * DAY_MS).toLong().coerceAtLeast(1L)
                ServiceWorkflowRules.overlaps(startAt, endAt, absenceStart, absenceStart + duration)
            }
            if (overlapWithAbsence) return@withTransaction Result.TechnicianUnavailable
            dao.updateWorkOrder(
                order.copy(
                    technicianId = technicianId,
                    teamName = teamName.cleanOrNull(),
                    plannedStartAt = startAt,
                    plannedEndAt = endAt,
                    status = ServiceWorkOrderStatus.SCHEDULED.name,
                    updatedAt = now,
                ),
            )
            Result.Success(order.id, order.reference)
        }
        if (result is Result.Success) {
            val technician = employeeDao.getById(technicianId)
            journal.log(MODULE, "ORDRE_PLANIFIE", "${result.reference} — ${technician?.nom.orEmpty()}", actor, now)
        }
        return result
    }

    suspend fun transition(workOrderId: Long, target: ServiceWorkOrderStatus, now: Long = System.currentTimeMillis()): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val order = dao.getWorkOrder(workOrderId) ?: return@withTransaction Result.NotFound
            val current = ServiceWorkflowRules.status(order.status)
            if (!ServiceWorkflowRules.mayTransition(current, target)) return@withTransaction Result.InvalidTransition
            if (target == ServiceWorkOrderStatus.CLOSED && order.salesInvoiceId == null) return@withTransaction Result.InvalidTransition
            val report = if (target == ServiceWorkOrderStatus.AWAITING_VALIDATION) dao.getReport(workOrderId) else null
            if (target == ServiceWorkOrderStatus.AWAITING_VALIDATION && (report == null || report.status != ServiceReportStatus.SUBMITTED.name)) return@withTransaction Result.Invalid
            if (target == ServiceWorkOrderStatus.SCHEDULED && (order.technicianId == null || order.plannedStartAt == null || order.plannedEndAt == null)) return@withTransaction Result.Invalid
            dao.updateWorkOrder(
                order.copy(
                    status = target.name,
                    actualStartAt = if (target == ServiceWorkOrderStatus.IN_PROGRESS && order.actualStartAt == null) now else order.actualStartAt,
                    actualEndAt = if (target == ServiceWorkOrderStatus.AWAITING_VALIDATION) now else order.actualEndAt,
                    updatedAt = now,
                ),
            )
            Result.Success(workOrderId, order.reference)
        }
        if (result is Result.Success) journal.log(MODULE, "ORDRE_${target.name}", result.reference.orEmpty(), actor, now)
        return result
    }

    suspend fun addTimesheet(
        workOrderId: Long,
        technicianId: Long,
        activityDate: Long,
        workMinutes: Int,
        travelMinutes: Int = 0,
        adminMinutes: Int = 0,
        costRate: Double = 0.0,
        billRate: Double = 0.0,
        isBillable: Boolean = true,
        note: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        if (!ServiceWorkflowRules.timesheetIsValid(workMinutes, travelMinutes, adminMinutes, costRate, billRate)) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val order = dao.getWorkOrder(workOrderId) ?: return@withTransaction Result.NotFound
            if (ServiceWorkflowRules.status(order.status) != ServiceWorkOrderStatus.IN_PROGRESS) return@withTransaction Result.InvalidTransition
            if (employeeDao.getById(technicianId)?.statut != "ACTIF" || order.technicianId != technicianId) return@withTransaction Result.TechnicianUnavailable
            val id = dao.insertTimesheet(
                ServiceTimesheetEntity(
                    workOrderId = workOrderId,
                    technicianId = technicianId,
                    activityDate = activityDate,
                    travelMinutes = travelMinutes,
                    workMinutes = workMinutes,
                    adminMinutes = adminMinutes,
                    costRate = costRate,
                    billRate = billRate,
                    isBillable = isBillable,
                    note = note.cleanOrNull(),
                    createdAt = now,
                    createdBy = actor,
                ),
            )
            Result.Success(id, order.reference)
        }
        if (result is Result.Success) journal.log(MODULE, "TEMPS_SAISI", "${result.reference} — ${workMinutes} min", actor, now)
        return result
    }

    suspend fun submitReport(
        workOrderId: Long,
        diagnosis: String,
        workPerformed: String,
        recommendations: String? = null,
        customerResolved: Boolean,
        customerComment: String? = null,
        customerSignerName: String? = null,
        customerSignatureUri: String? = null,
        technicianSignatureUri: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        if (diagnosis.trim().length < 3 || workPerformed.trim().length < 3) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val order = dao.getWorkOrder(workOrderId) ?: return@withTransaction Result.NotFound
            if (ServiceWorkflowRules.status(order.status) != ServiceWorkOrderStatus.IN_PROGRESS) return@withTransaction Result.InvalidTransition
            val old = dao.getReport(workOrderId)
            val report = ServiceReportEntity(
                id = old?.id ?: 0,
                workOrderId = workOrderId,
                status = ServiceReportStatus.SUBMITTED.name,
                diagnosis = diagnosis.trim(),
                workPerformed = workPerformed.trim(),
                recommendations = recommendations.cleanOrNull(),
                customerResolved = customerResolved,
                customerComment = customerComment.cleanOrNull(),
                customerSignerName = customerSignerName.cleanOrNull(),
                customerSignatureUri = customerSignatureUri.cleanOrNull(),
                technicianSignatureUri = technicianSignatureUri.cleanOrNull(),
                signedAt = customerSignatureUri.cleanOrNull()?.let { now },
                submittedAt = now,
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
            )
            if (old == null) dao.insertReport(report) else dao.updateReport(report)
            dao.updateWorkOrder(
                order.copy(
                    status = ServiceWorkOrderStatus.AWAITING_VALIDATION.name,
                    actualEndAt = now,
                    diagnosis = diagnosis.trim(),
                    resolution = workPerformed.trim(),
                    updatedAt = now,
                ),
            )
            Result.Success(workOrderId, order.reference)
        }
        if (result is Result.Success) journal.log(MODULE, "RAPPORT_SOUMIS", result.reference.orEmpty(), actor, now)
        return result
    }

    suspend fun approveReport(workOrderId: Long, now: Long = System.currentTimeMillis()): Result {
        val actor = actor(PermissionChecker.Action.VALIDATE) ?: return denied()
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val result = database.withTransaction {
            val order = dao.getWorkOrder(workOrderId) ?: return@withTransaction Result.NotFound
            val report = dao.getReport(workOrderId) ?: return@withTransaction Result.NotFound
            if (!ServiceWorkflowRules.reportCanBeApproved(
                    ServiceReportStatus.entries.firstOrNull { it.name == report.status } ?: ServiceReportStatus.DRAFT,
                    report.customerSignerName,
                    report.customerSignatureUri,
                    report.signedAt,
                    order.createdBy,
                    actor,
                )
            ) {
                return@withTransaction if (report.customerSignatureUri.isNullOrBlank() || report.customerSignerName.isNullOrBlank()) Result.SignatureRequired
                else Result.SelfApprovalNotAllowed
            }
            if (ServiceWorkflowRules.status(order.status) != ServiceWorkOrderStatus.AWAITING_VALIDATION) return@withTransaction Result.InvalidTransition
            dao.updateReport(report.copy(status = ServiceReportStatus.APPROVED.name, approvedBy = actor, approvedAt = now, updatedAt = now))
            dao.updateWorkOrder(order.copy(status = ServiceWorkOrderStatus.READY_TO_BILL.name, updatedAt = now))
            Result.Success(workOrderId, order.reference)
        }
        if (result is Result.Success) journal.log(MODULE, "RAPPORT_VALIDE", result.reference.orEmpty(), actor, now)
        return result
    }

    suspend fun addAttachment(
        workOrderId: Long,
        kind: String,
        localUri: String,
        caption: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val actor = actor(PermissionChecker.Action.EDIT) ?: return denied()
        val uri = localUri.trim()
        if (uri.isBlank() || !(uri.startsWith("content://") || uri.startsWith("file://"))) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val order = dao.getWorkOrder(workOrderId) ?: return Result.NotFound
        val id = dao.insertAttachment(ServiceAttachmentEntity(workOrderId = workOrderId, kind = kind.trim().uppercase(), localUri = uri, caption = caption.cleanOrNull(), capturedAt = now, capturedBy = actor))
        journal.log(MODULE, "MEDIA_AJOUTE", "${order.reference} — ${kind.uppercase()}", actor, now)
        return Result.Success(id, order.reference)
    }

    suspend fun createContract(contract: ServiceContractEntity): Result {
        val actor = actor(PermissionChecker.Action.CREATE) ?: return denied()
        if (contract.name.trim().length < 2 || contract.startAt <= 0 || contract.endAt?.let { it <= contract.startAt } == true ||
            contract.responseSlaMinutes?.let { it < 0 } == true || contract.resolutionSlaMinutes?.let { it < 0 } == true ||
            !contract.includedHours.isFinite() || contract.includedHours < 0 || contract.includedInterventions < 0 ||
            !contract.fixedFee.isFinite() || contract.fixedFee < 0 || !contract.hourlyRate.isFinite() || contract.hourlyRate < 0) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val ref = sequence.next(DocType.CONTRAT_SERVICE)
        val id = dao.insertContract(contract.copy(reference = ref, createdAt = System.currentTimeMillis(), createdBy = actor))
        journal.log(MODULE, "CONTRAT_CREE", "$ref — ${contract.name}", actor)
        return Result.Success(id, ref)
    }

    suspend fun registerCustomerAsset(asset: CustomerServiceAssetEntity): Result {
        val actor = actor(PermissionChecker.Action.CREATE) ?: return denied()
        if (asset.name.trim().length < 2 || asset.type.isBlank() || asset.customerId <= 0) return Result.Invalid
        if (asset.warrantyEndAt != null && asset.installedAt != null && asset.warrantyEndAt < asset.installedAt) return Result.Invalid
        if (!isWritable()) return Result.ReadOnly
        if (!isModuleActive()) return Result.ModuleInactive
        val contract = asset.contractId?.let { dao.getContract(it) ?: return Result.NotFound }
        if (contract != null && contract.customerId != asset.customerId) return Result.Invalid
        val id = dao.insertAsset(asset.copy(name = asset.name.trim(), serialNumber = asset.serialNumber.cleanOrNull(), createdAt = System.currentTimeMillis()))
        journal.log(MODULE, "EQUIPEMENT_CLIENT_CREE", asset.name.trim(), actor)
        return Result.Success(id)
    }

    private suspend fun actor(action: PermissionChecker.Action): Long? {
        val id = settings.getLong(SettingsStore.Keys.CURRENT_USER_ID) ?: return null
        val user = userDao.getById(id) ?: return null
        if (!user.actif || !permissions.hasPermission(user.roleId, MODULE, action)) return null
        return id
    }

    private fun denied(): Result = Result.Forbidden
    private suspend fun isWritable(): Boolean = !licence.isReadOnly()
    private suspend fun isModuleActive(): Boolean {
        val active = activation.getActivation().modulesActifs
        return active.isEmpty() || com.missa.b360.core.domain.model.ModuleCode.SER in active
    }

    private fun ServiceContractEntity.isActiveAt(now: Long): Boolean =
        status == "ACTIVE" && startAt <= now && (endAt == null || endAt >= now)

    private fun String?.cleanOrNull(): String? = this?.trim()?.ifBlank { null }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
