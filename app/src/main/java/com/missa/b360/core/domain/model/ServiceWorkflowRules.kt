package com.missa.b360.core.domain.model

/** Niveaux de priorité métier ; leur ordre est utilisé par le registre et le dispatch. */
enum class ServicePriority(val sortOrder: Int) { LOW(3), NORMAL(2), HIGH(1), URGENT(0) }
enum class ServiceRequestType { BREAKDOWN, MAINTENANCE, INSTALLATION, CUSTOMER_QUERY, WARRANTY, OTHER }
enum class ServiceRequestChannel { PHONE, EMAIL, PORTAL, WALK_IN, SALES, INTERNAL }
enum class ServiceRequestStatus { OPEN, QUALIFIED, QUOTE_REQUIRED, CONVERTED, RESOLVED, CLOSED, CANCELLED }
enum class ServiceWorkOrderStatus {
    TO_PLAN, SCHEDULED, EN_ROUTE, IN_PROGRESS, WAITING_CUSTOMER, WAITING_PARTS,
    WAITING_SUPPLIER, AWAITING_VALIDATION, READY_TO_BILL, CLOSED, CANCELLED,
}
enum class ServiceBillingMethod { FIXED, HOURLY, MIXED, WARRANTY, CONTRACT }
enum class ServiceReportStatus { DRAFT, SUBMITTED, APPROVED, REJECTED }

/** Règles centrales du workflow Services ; sans Android ni accès base de données. */
object ServiceWorkflowRules {
    private val transitions = mapOf(
        ServiceWorkOrderStatus.TO_PLAN to setOf(ServiceWorkOrderStatus.SCHEDULED),
        ServiceWorkOrderStatus.SCHEDULED to setOf(ServiceWorkOrderStatus.EN_ROUTE, ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.EN_ROUTE to setOf(ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.IN_PROGRESS to setOf(
            ServiceWorkOrderStatus.WAITING_CUSTOMER,
            ServiceWorkOrderStatus.WAITING_PARTS,
            ServiceWorkOrderStatus.WAITING_SUPPLIER,
            ServiceWorkOrderStatus.AWAITING_VALIDATION,
        ),
        ServiceWorkOrderStatus.WAITING_CUSTOMER to setOf(ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.WAITING_PARTS to setOf(ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.WAITING_SUPPLIER to setOf(ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.AWAITING_VALIDATION to setOf(ServiceWorkOrderStatus.READY_TO_BILL, ServiceWorkOrderStatus.IN_PROGRESS),
        ServiceWorkOrderStatus.READY_TO_BILL to setOf(ServiceWorkOrderStatus.CLOSED),
        ServiceWorkOrderStatus.CLOSED to emptySet(),
        ServiceWorkOrderStatus.CANCELLED to emptySet(),
    )

    fun requestIsValid(customerId: Long, description: String): Boolean =
        customerId > 0 && description.trim().length >= 4

    fun scheduleIsValid(startAt: Long, endAt: Long, now: Long = 0L): Boolean =
        startAt > 0L && endAt > startAt && (now == 0L || endAt > now)

    fun overlaps(startA: Long, endA: Long, startB: Long, endB: Long): Boolean =
        startA < endB && startB < endA

    fun mayTransition(from: ServiceWorkOrderStatus, to: ServiceWorkOrderStatus): Boolean =
        to == ServiceWorkOrderStatus.CANCELLED && from !in setOf(ServiceWorkOrderStatus.CLOSED, ServiceWorkOrderStatus.CANCELLED) ||
            to in transitions[from].orEmpty()

    fun requestMayConvert(status: ServiceRequestStatus): Boolean =
        status == ServiceRequestStatus.QUALIFIED

    fun responseDeadline(createdAt: Long, responseMinutes: Int?): Long? =
        responseMinutes?.takeIf { it > 0 }?.let { createdAt + it * 60_000L }

    fun resolutionDeadline(createdAt: Long, resolutionMinutes: Int?): Long? =
        resolutionMinutes?.takeIf { it > 0 }?.let { createdAt + it * 60_000L }

    fun isOverdue(deadlineAt: Long?, now: Long, terminal: Boolean): Boolean =
        !terminal && deadlineAt != null && deadlineAt < now

    fun timesheetIsValid(
        workMinutes: Int,
        travelMinutes: Int,
        adminMinutes: Int,
        costRate: Double,
        billRate: Double,
    ): Boolean = workMinutes > 0 && travelMinutes >= 0 && adminMinutes >= 0 &&
        costRate.isFinite() && costRate >= 0.0 && billRate.isFinite() && billRate >= 0.0

    fun reportCanBeApproved(
        status: ServiceReportStatus,
        customerSignerName: String?,
        customerSignatureUri: String?,
        signedAt: Long?,
        workOrderCreatorId: Long?,
        approverId: Long?,
    ): Boolean = status == ServiceReportStatus.SUBMITTED &&
        !customerSignerName.isNullOrBlank() && !customerSignatureUri.isNullOrBlank() && signedAt != null &&
        (workOrderCreatorId == null || approverId == null || workOrderCreatorId != approverId)

    fun status(value: String?): ServiceWorkOrderStatus =
        ServiceWorkOrderStatus.entries.firstOrNull { it.name == value } ?: ServiceWorkOrderStatus.TO_PLAN

    fun priority(value: String?): ServicePriority =
        ServicePriority.entries.firstOrNull { it.name == value } ?: ServicePriority.NORMAL
}
