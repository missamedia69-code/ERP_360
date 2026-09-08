package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.missa.b360.core.data.entity.GroupeAchatEntity
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.GroupeComptabiliteEntity
import com.missa.b360.core.data.entity.GroupeMaintenanceEntity
import com.missa.b360.core.data.entity.GroupeProductionEntity
import com.missa.b360.core.data.entity.GroupeStockEntity
import com.missa.b360.core.data.entity.GroupeVenteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Groupe complet : la fiche centrale et ses extensions métier.
 *
 * Room assemble les six tables en une seule lecture, ce qui évite d'écrire la
 * jointure à la main partout où les règles d'un groupe sont consultées.
 */
data class GroupeArticleComplet(
    @Embedded val groupe: GroupeArticleEntity,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val stock: GroupeStockEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val achat: GroupeAchatEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val vente: GroupeVenteEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val production: GroupeProductionEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val maintenance: GroupeMaintenanceEntity? = null,
    @Relation(parentColumn = "id", entityColumn = "itemGroupId")
    val comptabilite: GroupeComptabiliteEntity? = null,
)

@Dao
interface GroupeArticleDao {

    @Transaction
    @Query("SELECT * FROM item_groups ORDER BY actif DESC, nom ASC")
    fun observerComplets(): Flow<List<GroupeArticleComplet>>

    @Transaction
    @Query("SELECT * FROM item_groups ORDER BY actif DESC, nom ASC")
    suspend fun listerComplets(): List<GroupeArticleComplet>

    @Transaction
    @Query("SELECT * FROM item_groups WHERE id = :id LIMIT 1")
    suspend fun complet(id: Long): GroupeArticleComplet?

    @Query("SELECT * FROM item_groups WHERE code = :code LIMIT 1")
    suspend fun parCode(code: String): GroupeArticleEntity?

    @Query("SELECT COUNT(*) FROM item_groups")
    suspend fun compter(): Int

    @Insert
    suspend fun inserer(groupe: GroupeArticleEntity): Long

    @Update
    suspend fun mettreAJour(groupe: GroupeArticleEntity)

    @Insert suspend fun insererStock(extension: GroupeStockEntity)
    @Insert suspend fun insererAchat(extension: GroupeAchatEntity)
    @Insert suspend fun insererVente(extension: GroupeVenteEntity)
    @Insert suspend fun insererProduction(extension: GroupeProductionEntity)
    @Insert suspend fun insererMaintenance(extension: GroupeMaintenanceEntity)
    @Insert suspend fun insererComptabilite(extension: GroupeComptabiliteEntity)

    @Update suspend fun majStock(extension: GroupeStockEntity)
    @Update suspend fun majAchat(extension: GroupeAchatEntity)
    @Update suspend fun majVente(extension: GroupeVenteEntity)
    @Update suspend fun majProduction(extension: GroupeProductionEntity)
    @Update suspend fun majMaintenance(extension: GroupeMaintenanceEntity)
    @Update suspend fun majComptabilite(extension: GroupeComptabiliteEntity)
}
