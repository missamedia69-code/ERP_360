package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.FournisseurEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Fournisseurs (module 9.3).
 * Convention C7 : aucune suppression physique — statut `DESACTIVE` uniquement.
 */
@Dao
interface FournisseurDao {
    @Query("SELECT * FROM fournisseurs WHERE statut = 'ACTIF' ORDER BY nom")
    fun observeAll(): Flow<List<FournisseurEntity>>

    @Query("SELECT * FROM fournisseurs WHERE id = :id")
    suspend fun getById(id: Long): FournisseurEntity?

    @Query("SELECT * FROM fournisseurs WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): FournisseurEntity?

    /** Détection de doublons RF-01 : même téléphone OU nom identique. */
    @Query(
        "SELECT * FROM fournisseurs WHERE statut = 'ACTIF' AND (telephone = :telephone " +
            "OR LOWER(TRIM(nom)) = LOWER(TRIM(:nom)))",
    )
    suspend fun findDoublonsPotentiels(telephone: String, nom: String): List<FournisseurEntity>

    @Insert
    suspend fun insert(fournisseur: FournisseurEntity): Long

    @Update
    suspend fun update(fournisseur: FournisseurEntity)

    /** Désactivation (jamais de DELETE — C7). */
    @Query("UPDATE fournisseurs SET statut = 'DESACTIVE' WHERE id = :id")
    suspend fun desactiver(id: Long)

    @Query("SELECT COUNT(*) FROM fournisseurs")
    suspend fun count(): Int
}