package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Statut d'un article de stock (dérivé de [StockProductEntity.quantite] et de son seuil). */
enum class StockStatus { STOCK, LOW_STOCK, OUT }

/** Type de mouvement de stock enregistré dans le journal `stock_movements`. */
enum class StockMovementType { ENTRY, EXIT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT }

/** Cycle de vie d'un inventaire : brouillon → validé → terminé. */
enum class StockInventoryStatus { DRAFT, VALIDATED, COMPLETED }

/** Catégorie d'article (répartition du stock, filtres produits, rapports). */
@Entity(
    tableName = "stock_categories",
    indices = [Index(value = ["nom"], unique = true)],
)
data class StockCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** Couleur hexadécimale (ex. #2563EB) pour la répartition graphique. */
    val couleur: String = "#2563EB",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Entrepôt physique ou logique dans lequel les articles sont stockés. */
@Entity(
    tableName = "stock_warehouses",
    indices = [Index(value = ["nom"], unique = true)],
)
data class StockWarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val adresse: String? = null,
    val principal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Article de stock. La quantité courante ([quantite]) est mise à jour de façon atomique
 * à chaque mouvement validé ; [quantiteInitiale] conserve la valeur saisie à la création.
 */
@Entity(
    tableName = "stock_products",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["categorieId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["nom"]),
    ],
)
data class StockProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val categorieId: Long? = null,
    val warehouseId: Long? = null,
    val unite: String = "unité",
    val prixAchat: Double = 0.0,
    val prixVente: Double = 0.0,
    val seuilMin: Double = 0.0,
    val seuilMax: Double = 0.0,
    val quantiteInitiale: Double = 0.0,
    val quantite: Double = 0.0,
    val actif: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun statut(): StockStatus = when {
        quantite <= 0.0 -> StockStatus.OUT
        quantite <= seuilMin -> StockStatus.LOW_STOCK
        else -> StockStatus.STOCK
    }
}

/**
 * Mouvement de stock immuable. [delta] est la variation signée appliquée à la quantité de
 * l'article ; [quantity] est la quantité saisie (positive) pour l'affichage.
 */
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["reference"], unique = true),
        Index(value = ["type"]),
        Index(value = ["date"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    /** Nom stable de [StockMovementType]. */
    val type: String,
    val productId: Long,
    val sourceWarehouseId: Long? = null,
    val targetWarehouseId: Long? = null,
    val quantity: Double,
    /** Variation signée appliquée à l'article (le transfert a deux lignes opposées). */
    val delta: Double,
    val price: Double? = null,
    val counterpart: String? = null,
    /** DRAFT / VALIDATED — les mouvements sont créés validés, le statut reste pour l'audit. */
    val status: String = OperationStatus.VALIDATED.name,
    val date: Long,
    val notes: String? = null,
    val createdAt: Long,
)

/** Inventaire en cours : association de comptages physiques à une date et un entrepôt. */
@Entity(
    tableName = "stock_inventories",
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["status"]),
        Index(value = ["date"]),
    ],
)
data class StockInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val warehouseId: Long? = null,
    val status: String = StockInventoryStatus.DRAFT.name,
    val date: Long,
    val notes: String? = null,
    val createdAt: Long,
    val validatedAt: Long? = null,
    val completedAt: Long? = null,
)

/** Ligne de comptage d'un inventaire : stock théorique (expected) face au comptage réel. */
@Entity(
    tableName = "stock_inventory_lines",
    indices = [
        Index(value = ["inventoryId"]),
        Index(value = ["productId"]),
    ],
)
data class StockInventoryLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryId: Long,
    val productId: Long,
    val expectedQuantity: Double,
    val countedQuantity: Double,
    val ecart: Double,
    val notes: String? = null,
)
