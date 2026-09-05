package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockInventoryEntity
import com.missa.b360.core.data.entity.StockInventoryLineEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockWarehouseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accès offline au module Stock : articles, catégories, entrepôts, mouvements et inventaires.
 * Aucune suppression physique — les pièces sont conservées et seulement validées/clôturées.
 */
@Dao
interface StockDao {

    // --- Catégories ---
    @Query("SELECT * FROM stock_categories ORDER BY nom")
    fun observeCategories(): Flow<List<StockCategoryEntity>>

    @Query("SELECT * FROM stock_categories WHERE id = :id LIMIT 1")
    suspend fun getCategorie(id: Long): StockCategoryEntity?

    @Query("SELECT COUNT(*) FROM stock_categories")
    suspend fun countCategories(): Int

    @Insert
    suspend fun insertCategorie(categorie: StockCategoryEntity): Long

    @Update
    suspend fun updateCategorie(categorie: StockCategoryEntity)

    // --- Entrepôts ---
    @Query("SELECT * FROM stock_warehouses ORDER BY principal DESC, nom")
    fun observeWarehouses(): Flow<List<StockWarehouseEntity>>

    @Query("SELECT * FROM stock_warehouses WHERE id = :id LIMIT 1")
    suspend fun getWarehouse(id: Long): StockWarehouseEntity?

    @Query("SELECT COUNT(*) FROM stock_warehouses")
    suspend fun countWarehouses(): Int

    @Insert
    suspend fun insertWarehouse(warehouse: StockWarehouseEntity): Long

    @Update
    suspend fun updateWarehouse(warehouse: StockWarehouseEntity)

    // --- Produits ---
    @Query("SELECT * FROM stock_products ORDER BY nom")
    fun observeProducts(): Flow<List<StockProductEntity>>

    @Query("SELECT * FROM stock_products WHERE id = :id")
    fun observeProduct(id: Long): Flow<StockProductEntity?>

    @Query("SELECT * FROM stock_products WHERE id = :id LIMIT 1")
    suspend fun getProduct(id: Long): StockProductEntity?

    @Query("SELECT * FROM stock_products WHERE code = :code LIMIT 1")
    suspend fun getProductByCode(code: String): StockProductEntity?

    @Query("SELECT COUNT(*) FROM stock_products")
    suspend fun countProducts(): Int

    @Insert
    suspend fun insertProduct(product: StockProductEntity): Long

    @Update
    suspend fun updateProduct(product: StockProductEntity)

    @Query("UPDATE stock_products SET quantite = quantite + :delta WHERE id = :productId")
    suspend fun updateProductQuantity(productId: Long, delta: Double)

    @Query("UPDATE stock_products SET quantite = :quantity WHERE id = :productId")
    suspend fun setProductQuantity(productId: Long, quantity: Double)

    // --- Mouvements ---
    @Query("SELECT * FROM stock_movements ORDER BY date DESC, id DESC")
    fun observeMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY date DESC, id DESC")
    fun observeMovementsByProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE id = :id LIMIT 1")
    suspend fun getMovement(id: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE reference = :reference LIMIT 1")
    suspend fun getMovementByReference(reference: String): StockMovementEntity?

    @Insert
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("UPDATE stock_movements SET status = :status WHERE id = :id")
    suspend fun setMovementStatus(id: Long, status: String)

    // --- Inventaires ---
    @Query("SELECT * FROM stock_inventories ORDER BY date DESC, id DESC")
    fun observeInventories(): Flow<List<StockInventoryEntity>>

    @Query("SELECT * FROM stock_inventories WHERE id = :id LIMIT 1")
    suspend fun getInventory(id: Long): StockInventoryEntity?

    @Insert
    suspend fun insertInventory(inventory: StockInventoryEntity): Long

    @Query("UPDATE stock_inventories SET status = :status, validatedAt = :validatedAt WHERE id = :id")
    suspend fun setInventoryStatus(id: Long, status: String, validatedAt: Long?)

    @Query("UPDATE stock_inventories SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun completeInventory(id: Long, status: String, completedAt: Long)

    @Query("SELECT * FROM stock_inventory_lines WHERE inventoryId = :inventoryId ORDER BY id")
    fun observeInventoryLines(inventoryId: Long): Flow<List<StockInventoryLineEntity>>

    @Query("SELECT * FROM stock_inventory_lines WHERE inventoryId = :inventoryId ORDER BY id")
    suspend fun getInventoryLines(inventoryId: Long): List<StockInventoryLineEntity>

    @Insert
    suspend fun insertInventoryLines(lines: List<StockInventoryLineEntity>)

    /** Crée un inventaire et ses lignes de comptage en une transaction. */
    @Transaction
    suspend fun createInventoryWithLines(
        inventory: StockInventoryEntity,
        lines: List<StockInventoryLineEntity>,
    ): Long {
        val inventoryId = insertInventory(inventory)
        if (lines.isNotEmpty()) {
            insertInventoryLines(lines.map { it.copy(id = 0, inventoryId = inventoryId) })
        }
        return inventoryId
    }

    /**
     * Valide un inventaire : chaque écart non nul devient un mouvement d'ajustement validé
     * et met à jour la quantité de l'article de façon atomique.
     */
    @Transaction
    suspend fun validateInventoryWithAdjustments(
        inventoryId: Long,
        movements: List<StockMovementEntity>,
    ) {
        movements.forEach { movement ->
            insertMovement(movement)
            updateProductQuantity(movement.productId, movement.delta)
        }
        setInventoryStatus(inventoryId, VALIDATED_STATUS, System.currentTimeMillis())
    }

    /** Enregistre une entrée/sortie validée et applique son delta à l'article. */
    @Transaction
    suspend fun insertValidatedMovementAndUpdateStock(movement: StockMovementEntity): Long {
        val id = insertMovement(movement)
        updateProductQuantity(movement.productId, movement.delta)
        return id
    }

    /** Enregistre les deux lignes opposées d'un transfert (sortie source + entrée cible). */
    @Transaction
    suspend fun insertTransferMovements(
        outMovement: StockMovementEntity,
        inMovement: StockMovementEntity,
    ): Long {
        val outId = insertMovement(outMovement)
        insertMovement(inMovement)
        return outId
    }

    /** Conversion de statut immuable sans écrire de DELETE. */
    @Query("UPDATE stock_products SET actif = 0 WHERE id = :id")
    suspend fun disableProduct(id: Long)

    companion object {
        private const val VALIDATED_STATUS = "VALIDATED"
    }
}
