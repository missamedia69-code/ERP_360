package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurPaiementPlanifieEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import kotlinx.coroutines.flow.Flow

/** Situation de compte dérivée (convention C7 : jamais de DELETE, on remplace la ligne). */
@Dao
interface FournisseurBalanceDao {
    @Query("SELECT * FROM fournisseur_balances")
    fun observeAll(): Flow<List<FournisseurBalanceEntity>>

    @Query("SELECT * FROM fournisseur_balances WHERE fournisseurId = :fournisseurId")
    fun observe(fournisseurId: Long): Flow<FournisseurBalanceEntity?>

    @Query("SELECT * FROM fournisseur_balances WHERE fournisseurId = :fournisseurId")
    suspend fun get(fournisseurId: Long): FournisseurBalanceEntity?

    @Query("SELECT * FROM fournisseur_balances")
    suspend fun getAll(): List<FournisseurBalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: FournisseurBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(balances: List<FournisseurBalanceEntity>)

    @Query("SELECT COUNT(*) FROM fournisseur_balances")
    suspend fun count(): Int
}

/** Fiabilité mesurée dérivée des réceptions. */
@Dao
interface FournisseurScoreDao {
    @Query("SELECT * FROM fournisseur_scores")
    fun observeAll(): Flow<List<FournisseurScoreEntity>>

    @Query("SELECT * FROM fournisseur_scores WHERE fournisseurId = :fournisseurId")
    fun observe(fournisseurId: Long): Flow<FournisseurScoreEntity?>

    @Query("SELECT * FROM fournisseur_scores WHERE fournisseurId = :fournisseurId")
    suspend fun get(fournisseurId: Long): FournisseurScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: FournisseurScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(scores: List<FournisseurScoreEntity>)

    @Query("SELECT COUNT(*) FROM fournisseur_scores")
    suspend fun count(): Int
}

/** Échéancier de paiements : insertion et changement de statut uniquement. */
@Dao
interface FournisseurPaiementPlanifieDao {
    @Insert
    suspend fun insert(plan: FournisseurPaiementPlanifieEntity): Long

    @Update
    suspend fun update(plan: FournisseurPaiementPlanifieEntity)

    @Query("SELECT * FROM fournisseur_paiements_planifies WHERE id = :id")
    suspend fun getById(id: Long): FournisseurPaiementPlanifieEntity?

    @Query("SELECT * FROM fournisseur_paiements_planifies WHERE factureRecordId = :factureRecordId ORDER BY datePrevue, id")
    suspend fun getParFacture(factureRecordId: Long): List<FournisseurPaiementPlanifieEntity>

    @Query("SELECT * FROM fournisseur_paiements_planifies WHERE fournisseurId = :fournisseurId ORDER BY datePrevue, id")
    fun observeParFournisseur(fournisseurId: Long): Flow<List<FournisseurPaiementPlanifieEntity>>

    @Query("SELECT * FROM fournisseur_paiements_planifies WHERE statut = 'PLANIFIE' ORDER BY datePrevue, id")
    fun observePlanifies(): Flow<List<FournisseurPaiementPlanifieEntity>>
}
