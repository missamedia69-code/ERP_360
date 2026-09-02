package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.OperationRecordEntity
import kotlinx.coroutines.flow.Flow

/** Accès offline aux pièces des modules Stock → Projets. */
@Dao
interface OperationRecordDao {
    @Query("SELECT * FROM operation_records WHERE module = :module ORDER BY createdAt DESC")
    fun observeByModule(module: String): Flow<List<OperationRecordEntity>>

    @Query("SELECT * FROM operation_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OperationRecordEntity>>

    @Query("SELECT * FROM operation_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OperationRecordEntity?

    @Insert
    suspend fun insert(record: OperationRecordEntity): Long

    @Update
    suspend fun update(record: OperationRecordEntity)
}
