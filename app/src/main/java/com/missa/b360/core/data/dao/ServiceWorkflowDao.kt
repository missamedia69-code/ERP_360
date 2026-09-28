package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.CustomerServiceAssetEntity
import com.missa.b360.core.data.entity.ServiceAttachmentEntity
import com.missa.b360.core.data.entity.ServiceContractEntity
import com.missa.b360.core.data.entity.ServiceReportEntity
import com.missa.b360.core.data.entity.ServiceRequestEntity
import com.missa.b360.core.data.entity.ServiceTimesheetEntity
import com.missa.b360.core.data.entity.ServiceWorkOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceWorkflowDao {
    @Query("SELECT * FROM service_requests ORDER BY CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END, createdAt DESC")
    fun observeRequests(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_work_orders ORDER BY CASE status WHEN 'IN_PROGRESS' THEN 0 WHEN 'EN_ROUTE' THEN 1 WHEN 'SCHEDULED' THEN 2 WHEN 'TO_PLAN' THEN 3 ELSE 4 END, plannedStartAt IS NULL, plannedStartAt, createdAt DESC")
    fun observeWorkOrders(): Flow<List<ServiceWorkOrderEntity>>

    @Query("SELECT * FROM service_contracts ORDER BY startAt DESC")
    fun observeContracts(): Flow<List<ServiceContractEntity>>

    @Query("SELECT * FROM service_reports ORDER BY updatedAt DESC")
    fun observeReports(): Flow<List<ServiceReportEntity>>

    @Query("SELECT * FROM customer_service_assets WHERE customerId = :customerId AND status = 'ACTIF' ORDER BY name")
    fun observeAssets(customerId: Long): Flow<List<CustomerServiceAssetEntity>>

    @Query("SELECT * FROM service_work_orders WHERE id = :id LIMIT 1")
    suspend fun getWorkOrder(id: Long): ServiceWorkOrderEntity?

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    suspend fun getRequest(id: Long): ServiceRequestEntity?

    @Query("SELECT * FROM service_contracts WHERE id = :id LIMIT 1")
    suspend fun getContract(id: Long): ServiceContractEntity?

    @Query("SELECT * FROM customer_service_assets WHERE id = :id LIMIT 1")
    suspend fun getAsset(id: Long): CustomerServiceAssetEntity?

    @Query("SELECT * FROM service_reports WHERE workOrderId = :workOrderId LIMIT 1")
    suspend fun getReport(workOrderId: Long): ServiceReportEntity?

    @Query("SELECT * FROM service_timesheets WHERE workOrderId = :workOrderId ORDER BY activityDate DESC, id DESC")
    fun observeTimesheets(workOrderId: Long): Flow<List<ServiceTimesheetEntity>>

    @Query("SELECT * FROM service_attachments WHERE workOrderId = :workOrderId ORDER BY capturedAt DESC")
    fun observeAttachments(workOrderId: Long): Flow<List<ServiceAttachmentEntity>>

    @Query("SELECT COUNT(*) FROM service_work_orders WHERE technicianId = :technicianId AND id != :excludingId AND status NOT IN ('CANCELLED', 'CLOSED') AND plannedStartAt < :endAt AND plannedEndAt > :startAt")
    suspend fun countOverlappingAssignments(technicianId: Long, startAt: Long, endAt: Long, excludingId: Long): Int

    @Insert
    suspend fun insertRequest(request: ServiceRequestEntity): Long

    @Update
    suspend fun updateRequest(request: ServiceRequestEntity)

    @Insert
    suspend fun insertWorkOrder(workOrder: ServiceWorkOrderEntity): Long

    @Update
    suspend fun updateWorkOrder(workOrder: ServiceWorkOrderEntity)

    @Insert
    suspend fun insertContract(contract: ServiceContractEntity): Long

    @Insert
    suspend fun insertAsset(asset: CustomerServiceAssetEntity): Long

    @Insert
    suspend fun insertTimesheet(timesheet: ServiceTimesheetEntity): Long

    @Insert
    suspend fun insertReport(report: ServiceReportEntity): Long

    @Update
    suspend fun updateReport(report: ServiceReportEntity)

    @Insert
    suspend fun insertAttachment(attachment: ServiceAttachmentEntity): Long
}
