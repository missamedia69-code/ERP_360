package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Registre local des contrats de service et des SLA client. */
@Entity(
    tableName = "service_contracts",
    foreignKeys = [ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.NO_ACTION)],
    indices = [Index(value = ["reference"], unique = true), Index(value = ["customerId", "status"])],
)
data class ServiceContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val customerId: Long,
    val name: String,
    val type: String,
    val status: String = "ACTIVE",
    val startAt: Long,
    val endAt: Long?,
    val autoRenew: Boolean = false,
    val responseSlaMinutes: Int? = null,
    val resolutionSlaMinutes: Int? = null,
    val includedHours: Double = 0.0,
    val includedInterventions: Int = 0,
    val fixedFee: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val coversParts: Boolean = false,
    val createdAt: Long,
    val createdBy: Long? = null,
)

@Entity(
    tableName = "customer_service_assets",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ServiceContractEntity::class, parentColumns = ["id"], childColumns = ["contractId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index(value = ["customerId", "status"]), Index(value = ["contractId"]), Index(value = ["serialNumber"], unique = true)],
)
data class CustomerServiceAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val name: String,
    val type: String,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val installedAt: Long? = null,
    val warrantyEndAt: Long? = null,
    val contractId: Long? = null,
    val status: String = "ACTIF",
    val notes: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "service_requests",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ServiceContractEntity::class, parentColumns = ["id"], childColumns = ["contractId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = CustomerServiceAssetEntity::class, parentColumns = ["id"], childColumns = ["customerAssetId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index(value = ["reference"], unique = true), Index(value = ["status", "priority", "createdAt"]), Index(value = ["customerId", "createdAt"]), Index(value = ["contractId"])],
)
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val customerId: Long,
    val customerName: String,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val requestType: String,
    val channel: String,
    val priority: String,
    val status: String = "OPEN",
    val description: String,
    val customerAssetId: Long? = null,
    val contractId: Long? = null,
    val projectReference: String? = null,
    val requestedAt: Long? = null,
    val responseDeadlineAt: Long? = null,
    val resolutionDeadlineAt: Long? = null,
    val qualifiedAt: Long? = null,
    val convertedWorkOrderId: Long? = null,
    val closedAt: Long? = null,
    val createdAt: Long,
    val createdBy: Long? = null,
)

@Entity(
    tableName = "service_work_orders",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ServiceRequestEntity::class, parentColumns = ["id"], childColumns = ["requestId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ServiceContractEntity::class, parentColumns = ["id"], childColumns = ["contractId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = CustomerServiceAssetEntity::class, parentColumns = ["id"], childColumns = ["customerAssetId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["technicianId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [
        Index(value = ["reference"], unique = true), Index(value = ["status", "plannedStartAt"]),
        Index(value = ["technicianId", "plannedStartAt", "plannedEndAt"]), Index(value = ["requestId"]),
        Index(value = ["customerId", "createdAt"]),
    ],
)
data class ServiceWorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val requestId: Long? = null,
    val customerId: Long,
    val customerName: String,
    val customerAssetId: Long? = null,
    val contractId: Long? = null,
    val interventionType: String,
    val priority: String,
    val status: String = "TO_PLAN",
    val technicianId: Long? = null,
    val teamName: String? = null,
    val plannedStartAt: Long? = null,
    val plannedEndAt: Long? = null,
    val actualStartAt: Long? = null,
    val actualEndAt: Long? = null,
    val responseDeadlineAt: Long? = null,
    val resolutionDeadlineAt: Long? = null,
    val address: String? = null,
    val description: String,
    val diagnosis: String? = null,
    val resolution: String? = null,
    val isBillable: Boolean = true,
    val billingMethod: String = "MIXED",
    val fixedFee: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val salesInvoiceId: Long? = null,
    val createdAt: Long,
    val createdBy: Long? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "service_timesheets",
    foreignKeys = [
        ForeignKey(entity = ServiceWorkOrderEntity::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["technicianId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index(value = ["workOrderId", "activityDate"]), Index(value = ["technicianId", "activityDate"])],
)
data class ServiceTimesheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val technicianId: Long,
    val activityDate: Long,
    val travelMinutes: Int = 0,
    val workMinutes: Int,
    val adminMinutes: Int = 0,
    val costRate: Double = 0.0,
    val billRate: Double = 0.0,
    val isBillable: Boolean = true,
    val status: String = "SUBMITTED",
    val note: String? = null,
    val createdAt: Long,
    val createdBy: Long? = null,
)

@Entity(
    tableName = "service_reports",
    foreignKeys = [ForeignKey(entity = ServiceWorkOrderEntity::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.NO_ACTION)],
    indices = [Index(value = ["workOrderId"], unique = true), Index(value = ["status", "updatedAt"])],
)
data class ServiceReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val status: String = "DRAFT",
    val diagnosis: String,
    val workPerformed: String,
    val recommendations: String? = null,
    val customerResolved: Boolean = false,
    val customerComment: String? = null,
    val customerSignerName: String? = null,
    /** URI local durable (photo de signature ou signature numérisée), peut rester vide en brouillon. */
    val customerSignatureUri: String? = null,
    val technicianSignatureUri: String? = null,
    val signedAt: Long? = null,
    val submittedAt: Long? = null,
    val approvedBy: Long? = null,
    val approvedAt: Long? = null,
    val pdfUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "service_attachments",
    foreignKeys = [ForeignKey(entity = ServiceWorkOrderEntity::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.NO_ACTION)],
    indices = [Index(value = ["workOrderId", "capturedAt"])],
)
data class ServiceAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val kind: String,
    /** Persisted content:// URI or local app file URI; binary media stays outside Room. */
    val localUri: String,
    val caption: String? = null,
    val capturedAt: Long,
    val capturedBy: Long? = null,
)
