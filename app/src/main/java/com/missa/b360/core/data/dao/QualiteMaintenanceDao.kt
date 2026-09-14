package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.NonConformiteEntity
import kotlinx.coroutines.flow.Flow

/** Registre des non-conformités qualité. */
@Dao
interface NonConformiteDao {
    @Query("SELECT * FROM qualite_non_conformites ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<NonConformiteEntity>>

    @Query("SELECT * FROM qualite_non_conformites WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NonConformiteEntity?

    @Insert
    suspend fun insert(nc: NonConformiteEntity): Long

    @Update
    suspend fun update(nc: NonConformiteEntity)
}

/** Parc d'équipements suivis en maintenance. */
@Dao
interface EquipementDao {
    @Query("SELECT * FROM maintenance_equipements ORDER BY actif DESC, nom ASC")
    fun observeAll(): Flow<List<EquipementEntity>>

    @Query("SELECT * FROM maintenance_equipements WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EquipementEntity?

    @Query("SELECT COUNT(*) FROM maintenance_equipements WHERE nom = :nom AND id <> :saufId")
    suspend fun compterHomonymes(nom: String, saufId: Long = 0): Int

    @Insert
    suspend fun insert(equipement: EquipementEntity): Long

    @Update
    suspend fun update(equipement: EquipementEntity)
}

/** Interventions de maintenance, préventives et correctives. */
@Dao
interface InterventionDao {
    @Query("SELECT * FROM maintenance_interventions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<InterventionEntity>>

    @Query("SELECT * FROM maintenance_interventions WHERE equipementId = :equipementId ORDER BY date DESC")
    fun observeByEquipement(equipementId: Long): Flow<List<InterventionEntity>>

    @Insert
    suspend fun insert(intervention: InterventionEntity): Long

    @Update
    suspend fun update(intervention: InterventionEntity)
}
