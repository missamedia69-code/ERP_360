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

    /** Lecture ponctuelle (calculs de solde, retours) — pas de Flow pour rester léger. */
    @Query("SELECT * FROM operation_records WHERE module = :module ORDER BY createdAt DESC")
    suspend fun getByModule(module: String): List<OperationRecordEntity>

    /** Ventes et avoirs validés, base du calcul des comptes clients. */
    @Query("SELECT * FROM operation_records WHERE module = 'VENTE' AND status = 'VALIDATED'")
    suspend fun getVentesValidees(): List<OperationRecordEntity>

    /**
     * Ventes validées d'un client. Les avoirs de retour n'ont pas de `tiersId` : on les retrouve
     * par le `clientId` de leur détail JSON (clé suivie d'une virgule, pour ne pas confondre 5
     * et 52). L'appelant filtre ensuite sur le client du détail décodé.
     */
    @Query(
        "SELECT * FROM operation_records WHERE module = 'VENTE' AND status = 'VALIDATED' " +
            "AND (tiersId = :clientId OR (tiersId IS NULL AND notes LIKE '%\"clientId\":' || :clientId || ',%'))",
    )
    suspend fun getVentesValideesPourClient(clientId: Long): List<OperationRecordEntity>

    @Insert
    suspend fun insert(record: OperationRecordEntity): Long

    @Update
    suspend fun update(record: OperationRecordEntity)
}
