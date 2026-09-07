package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import kotlinx.coroutines.flow.Flow

/** Comptes de trésorerie (caisse, banque, mobile money). */
@Dao
interface CompteTresorerieDao {
    @Query("SELECT * FROM tresorerie_comptes ORDER BY actif DESC, nom ASC")
    fun observeAll(): Flow<List<CompteTresorerieEntity>>

    @Query("SELECT * FROM tresorerie_comptes ORDER BY actif DESC, nom ASC")
    suspend fun getAll(): List<CompteTresorerieEntity>

    @Query("SELECT * FROM tresorerie_comptes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CompteTresorerieEntity?

    @Query("SELECT COUNT(*) FROM tresorerie_comptes WHERE nom = :nom AND id <> :saufId")
    suspend fun compterHomonymes(nom: String, saufId: Long = 0): Int

    @Insert
    suspend fun insert(compte: CompteTresorerieEntity): Long

    @Update
    suspend fun update(compte: CompteTresorerieEntity)
}

/** Mouvements de trésorerie ; aucune suppression, seulement des ajouts. */
@Dao
interface MouvementTresorerieDao {
    @Query("SELECT * FROM tresorerie_mouvements ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<MouvementTresorerieEntity>>

    @Query("SELECT * FROM tresorerie_mouvements WHERE compteId = :compteId ORDER BY date DESC, id DESC")
    fun observeByCompte(compteId: Long): Flow<List<MouvementTresorerieEntity>>

    @Query("SELECT * FROM tresorerie_mouvements ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<MouvementTresorerieEntity>

    @Query("SELECT * FROM tresorerie_mouvements WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MouvementTresorerieEntity?

    /** Anti-doublon des encaissements automatiques : une facture, un mouvement. */
    @Query("SELECT COUNT(*) FROM tresorerie_mouvements WHERE reference = :reference")
    suspend fun compterParReference(reference: String): Int

    @Insert
    suspend fun insert(mouvement: MouvementTresorerieEntity): Long

    @Update
    suspend fun update(mouvement: MouvementTresorerieEntity)

    /** Pointage du rapprochement bancaire, sans relire l'entité entière. */
    @Query("UPDATE tresorerie_mouvements SET rapproche = :rapproche WHERE id = :id")
    suspend fun marquerRapproche(id: Long, rapproche: Boolean)
}
