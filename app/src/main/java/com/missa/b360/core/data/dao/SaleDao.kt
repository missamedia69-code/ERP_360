package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.SaleEntity
import com.missa.b360.core.data.entity.SaleLineEntity
import com.missa.b360.core.data.entity.SalePaymentEntity
import com.missa.b360.core.data.entity.SaleReceivableEntity
import kotlinx.coroutines.flow.Flow

/** Accès offline transactionnel aux ventes, lignes, paiements et créances. */
@Dao
interface SaleDao {

    @Query("SELECT * FROM sales ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE status = 'DRAFT' ORDER BY createdAt DESC, id DESC")
    fun observeDrafts(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE reference = :reference LIMIT 1")
    suspend fun getByReference(reference: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE operationRecordId = :recordId LIMIT 1")
    suspend fun getByOperationRecordId(recordId: Long): SaleEntity?

    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    // --- Lignes ---
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    suspend fun getLines(saleId: Long): List<SaleLineEntity>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    fun observeLines(saleId: Long): Flow<List<SaleLineEntity>>

    @Insert
    suspend fun insertLines(lines: List<SaleLineEntity>)

    // --- Paiements ---
    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId ORDER BY id")
    suspend fun getPayments(saleId: Long): List<SalePaymentEntity>

    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId ORDER BY id")
    fun observePayments(saleId: Long): Flow<List<SalePaymentEntity>>

    @Insert
    suspend fun insertPayments(payments: List<SalePaymentEntity>)

    // --- Créances ---
    @Query("SELECT * FROM sale_receivables WHERE saleId = :saleId LIMIT 1")
    suspend fun getReceivable(saleId: Long): SaleReceivableEntity?

    @Query("SELECT * FROM sale_receivables WHERE saleId = :saleId LIMIT 1")
    fun observeReceivable(saleId: Long): Flow<SaleReceivableEntity?>

    @Insert
    suspend fun insertReceivable(receivable: SaleReceivableEntity)

    @Update
    suspend fun updateReceivable(receivable: SaleReceivableEntity)

    /** Conversion en CANCELLED sans suppression physique. */
    @Query("UPDATE sales SET status = 'CANCELLED', cancelledAt = :at WHERE id = :id")
    suspend fun cancelById(id: Long, at: Long)

    @Query("UPDATE sale_receivables SET status = 'CANCELLED', settledAt = :at WHERE saleId = :saleId")
    suspend fun cancelReceivable(saleId: Long, at: Long)
}
