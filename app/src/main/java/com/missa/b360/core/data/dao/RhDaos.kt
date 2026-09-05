package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY nom")
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE statut = 'ACTIF' ORDER BY nom")
    fun observeActifs(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE statut = 'ACTIF' ORDER BY nom")
    suspend fun listActifs(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Insert
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    /** Désactivation — jamais de DELETE (C7). */
    @Query("UPDATE employees SET statut = 'DESACTIVE' WHERE id = :id")
    suspend fun desactiver(id: Long)
}

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences ORDER BY dateDebut DESC")
    fun observeAll(): Flow<List<AbsenceEntity>>

    @Query("SELECT * FROM absences WHERE employeeId = :employeeId")
    suspend fun byEmployee(employeeId: Long): List<AbsenceEntity>

    @Insert
    suspend fun insert(absence: AbsenceEntity): Long

    @Query("DELETE FROM absences WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY echeance IS NULL, echeance, id")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY echeance IS NULL, echeance, id")
    suspend fun listAll(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)
}
