package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.StockDao
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockInventoryEntity
import com.missa.b360.core.data.entity.StockInventoryLineEntity
import com.missa.b360.core.data.entity.StockInventoryStatus
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockWarehouseEntity
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Règles métier du module Stock : articles, seuils, mouvements et inventaires.
 * Toute écriture est refusée en licence expirée (lecture seule) et journalisée.
 */
class StockUseCases @Inject constructor(
    private val dao: StockDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    data class ProductInput(
        val nom: String,
        val categorieId: Long? = null,
        val warehouseId: Long? = null,
        val unite: String = "unité",
        val prixAchat: Double = 0.0,
        val prixVente: Double = 0.0,
        val seuilMin: Double = 0.0,
        val seuilMax: Double = 0.0,
        val quantiteInitiale: Double = 0.0,
    )

    data class InventoryLineInput(
        val productId: Long,
        val expected: Double,
        val counted: Double,
    )

    sealed interface Result {
        data class Success(val reference: String) : Result
        data object ReadOnly : Result
        data object Invalid : Result
        data object Missing : Result
        data object Error : Result
    }

    // --- Flux observables ---
    fun observeCategories(): Flow<List<StockCategoryEntity>> = dao.observeCategories()
    fun observeWarehouses(): Flow<List<StockWarehouseEntity>> = dao.observeWarehouses()
    fun observeProducts(): Flow<List<StockProductEntity>> = dao.observeProducts()
    fun observeProduct(id: Long): Flow<StockProductEntity?> = dao.observeProduct(id)
    fun observeMovements(): Flow<List<StockMovementEntity>> = dao.observeMovements()
    fun observeMovementsByProduct(id: Long): Flow<List<StockMovementEntity>> = dao.observeMovementsByProduct(id)
    fun observeInventories(): Flow<List<StockInventoryEntity>> = dao.observeInventories()
    fun observeInventoryLines(inventoryId: Long): Flow<List<StockInventoryLineEntity>> =
        dao.observeInventoryLines(inventoryId)

    /** Entrepôt principal et catégories de départ (jamais d'article fictif). */
    suspend fun ensureDefaults() {
        if (dao.countWarehouses() == 0) {
            dao.insertWarehouse(
                StockWarehouseEntity(
                    nom = "Entrepôt principal",
                    principal = true,
                ),
            )
        }
        if (dao.countCategories() == 0) {
            val defauts = listOf(
                StockCategoryEntity(nom = "Produits finis", couleur = "#1554E8"),
                StockCategoryEntity(nom = "Matières premières", couleur = "#16803C"),
                StockCategoryEntity(nom = "Consommables", couleur = "#F28A16"),
                StockCategoryEntity(nom = "Fournitures", couleur = "#7047E8"),
                StockCategoryEntity(nom = "Sous-traité", couleur = "#00A5A5"),
            )
            for (categorie in defauts) {
                dao.insertCategorie(categorie)
            }
        }
    }

    suspend fun createProduct(input: ProductInput): Result {
        if (!StockValidation.productValide(input)) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        val code = sequenceManager.next(DocType.PRODUIT)
        return try {
            dao.insertProduct(
                StockProductEntity(
                    code = code,
                    nom = input.nom.trim(),
                    categorieId = input.categorieId,
                    warehouseId = input.warehouseId,
                    unite = input.unite.trim().ifBlank { "unité" },
                    prixAchat = input.prixAchat,
                    prixVente = input.prixVente,
                    seuilMin = input.seuilMin,
                    seuilMax = input.seuilMax,
                    quantiteInitiale = input.quantiteInitiale,
                    quantite = input.quantiteInitiale,
                ),
            )
            journalManager.log("STOCK", "CREATION_PRODUIT", "PRD $code — ${input.nom.trim()}")
            Result.Success(code)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun updateProduct(id: Long, input: ProductInput): Result {
        if (!StockValidation.productValide(input)) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        val existing = dao.getProduct(id) ?: return Result.Missing
        val baseDelta = input.quantiteInitiale - existing.quantiteInitiale
        return try {
            dao.updateProduct(
                existing.copy(
                    nom = input.nom.trim(),
                    categorieId = input.categorieId,
                    warehouseId = input.warehouseId,
                    unite = input.unite.trim().ifBlank { "unité" },
                    prixAchat = input.prixAchat,
                    prixVente = input.prixVente,
                    seuilMin = input.seuilMin,
                    seuilMax = input.seuilMax,
                    quantiteInitiale = input.quantiteInitiale,
                    quantite = existing.quantite + baseDelta,
                ),
            )
            journalManager.log("STOCK", "MODIFICATION_PRODUIT", "${existing.code} — ${input.nom.trim()}")
            Result.Success(existing.code)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun createEntry(
        productId: Long,
        warehouseId: Long?,
        quantity: Double,
        price: Double?,
        counterpart: String?,
        date: Long,
        notes: String?,
    ): Result = createValidatedMovement(
        type = StockMovementType.ENTRY,
        docType = DocType.BON_RECEPTION,
        productId = productId,
        quantity = quantity,
        delta = quantity,
        sourceWarehouseId = null,
        targetWarehouseId = warehouseId,
        price = price,
        counterpart = counterpart,
        date = date,
        notes = notes,
    )

    suspend fun createExit(
        productId: Long,
        warehouseId: Long?,
        quantity: Double,
        price: Double?,
        counterpart: String?,
        date: Long,
        notes: String?,
    ): Result = createValidatedMovement(
        type = StockMovementType.EXIT,
        docType = DocType.LIVRAISON,
        productId = productId,
        quantity = quantity,
        delta = -quantity,
        sourceWarehouseId = warehouseId,
        targetWarehouseId = null,
        price = price,
        counterpart = counterpart,
        date = date,
        notes = notes,
    )

    suspend fun createAdjustment(
        productId: Long,
        warehouseId: Long?,
        countedQuantity: Double,
        date: Long,
        notes: String?,
    ): Result {
        val product = dao.getProduct(productId) ?: return Result.Missing
        if (countedQuantity < 0.0 || !countedQuantity.isFinite()) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        val delta = countedQuantity - product.quantite
        return createValidatedMovement(
            type = StockMovementType.ADJUSTMENT,
            docType = DocType.INVENTAIRE,
            productId = productId,
            quantity = countedQuantity,
            delta = delta,
            sourceWarehouseId = warehouseId,
            targetWarehouseId = null,
            price = null,
            counterpart = null,
            date = date,
            notes = notes,
        )
    }

    suspend fun createTransfer(
        productId: Long,
        sourceWarehouseId: Long?,
        targetWarehouseId: Long?,
        quantity: Double,
        date: Long,
        notes: String?,
    ): Result {
        if (quantity <= 0.0 || !quantity.isFinite()) return Result.Invalid
        if (sourceWarehouseId == targetWarehouseId) return Result.Invalid
        if (dao.getProduct(productId) == null) return Result.Missing
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            val reference = sequenceManager.next(DocType.TRANSFERT)
            val out = StockMovementEntity(
                reference = "$reference-1",
                type = StockMovementType.TRANSFER_OUT.name,
                productId = productId,
                sourceWarehouseId = sourceWarehouseId,
                targetWarehouseId = targetWarehouseId,
                quantity = quantity,
                delta = -quantity,
                status = "VALIDATED",
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            )
            val inbound = out.copy(
                id = 0,
                reference = "$reference-2",
                type = StockMovementType.TRANSFER_IN.name,
                delta = quantity,
            )
            dao.insertTransferMovements(out, inbound)
            journalManager.log(
                "STOCK",
                "TRANSFERT",
                "$reference — ${dao.getProduct(productId)?.nom ?: "produit"}, −$quantity / +$quantity",
            )
            Result.Success(reference)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun createInventory(
        warehouseId: Long?,
        date: Long,
        notes: String?,
        lines: List<InventoryLineInput>,
    ): Result {
        if (lines.isEmpty()) return Result.Invalid
        if (lines.any { it.counted < 0.0 || !it.counted.isFinite() || it.expected < 0.0 }) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            val reference = sequenceManager.next(DocType.INVENTAIRE)
            val inventory = StockInventoryEntity(
                reference = reference,
                warehouseId = warehouseId,
                status = StockInventoryStatus.DRAFT.name,
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            )
            val entities = lines.map { line ->
                StockInventoryLineEntity(
                    inventoryId = 0,
                    productId = line.productId,
                    expectedQuantity = line.expected,
                    countedQuantity = line.counted,
                    ecart = line.counted - line.expected,
                )
            }
            dao.createInventoryWithLines(inventory, entities)
            journalManager.log("STOCK", "CREATION_INVENTAIRE", "$reference — ${entities.size} article(s)")
            Result.Success(reference)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun validateInventory(id: Long): Result {
        val inventory = dao.getInventory(id) ?: return Result.Missing
        if (inventory.status != StockInventoryStatus.DRAFT.name) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            val lines = dao.getInventoryLines(id)
            val movements = lines.mapIndexedNotNull { index, line ->
                if (kotlin.math.abs(line.ecart) < 0.0001) null
                else StockMovementEntity(
                    reference = "${inventory.reference}-A${index + 1}",
                    type = StockMovementType.ADJUSTMENT.name,
                    productId = line.productId,
                    sourceWarehouseId = inventory.warehouseId,
                    targetWarehouseId = null,
                    quantity = line.countedQuantity,
                    delta = line.ecart,
                    status = "VALIDATED",
                    date = System.currentTimeMillis(),
                    notes = "Ajustement inventaire ${inventory.reference}",
                    createdAt = System.currentTimeMillis(),
                )
            }
            dao.validateInventoryWithAdjustments(inventory.id, movements)
            journalManager.log("STOCK", "VALIDATION_INVENTAIRE", "${inventory.reference} — ${movements.size} ajustement(s)")
            Result.Success(inventory.reference)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun completeInventory(id: Long): Result {
        val inventory = dao.getInventory(id) ?: return Result.Missing
        if (inventory.status != StockInventoryStatus.VALIDATED.name) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            dao.completeInventory(id, StockInventoryStatus.COMPLETED.name, System.currentTimeMillis())
            journalManager.log("STOCK", "CLOTURE_INVENTAIRE", inventory.reference)
            Result.Success(inventory.reference)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun addWarehouse(nom: String, adresse: String?): Result {
        val valid = nom.trim().length in 2..80
        if (!valid) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            dao.insertWarehouse(StockWarehouseEntity(nom = nom.trim(), adresse = adresse?.trim()?.ifBlank { null }))
            journalManager.log("STOCK", "CREATION_ENTREPOT", nom.trim())
            Result.Success(nom.trim())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    suspend fun addCategorie(nom: String, couleur: String): Result {
        val valid = nom.trim().length in 2..80
        if (!valid) return Result.Invalid
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            dao.insertCategorie(StockCategoryEntity(nom = nom.trim(), couleur = couleur.trim()))
            journalManager.log("STOCK", "CREATION_CATEGORIE", nom.trim())
            Result.Success(nom.trim())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }

    private suspend fun createValidatedMovement(
        type: StockMovementType,
        docType: DocType,
        productId: Long,
        quantity: Double,
        delta: Double,
        sourceWarehouseId: Long?,
        targetWarehouseId: Long?,
        price: Double?,
        counterpart: String?,
        date: Long,
        notes: String?,
    ): Result {
        val quantityValide = if (type == StockMovementType.ADJUSTMENT) quantity >= 0.0 else quantity > 0.0
        if (!quantityValide || !quantity.isFinite()) return Result.Invalid
        if (dao.getProduct(productId) == null) return Result.Missing
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        return try {
            val reference = sequenceManager.next(docType)
            val movement = StockMovementEntity(
                reference = reference,
                type = type.name,
                productId = productId,
                sourceWarehouseId = sourceWarehouseId,
                targetWarehouseId = targetWarehouseId,
                quantity = quantity,
                delta = delta,
                price = price?.takeIf { it.isFinite() && it >= 0.0 },
                counterpart = counterpart?.trim()?.ifBlank { null },
                status = "VALIDATED",
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            )
            dao.insertValidatedMovementAndUpdateStock(movement)
            journalManager.log(
                "STOCK",
                "MOUVEMENT_${type.name}",
                "$reference — ${dao.getProduct(productId)?.nom ?: "produit"} (${delta})",
            )
            Result.Success(reference)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.Error
        }
    }
}

/** Validation déterministe des champs du module Stock. */
object StockValidation {
    fun productValide(input: StockUseCases.ProductInput): Boolean {
        val nom = input.nom.trim()
        return nom.length in 2..120 &&
            input.prixAchat.isFinite() && input.prixVente.isFinite() &&
            input.seuilMin.isFinite() && input.seuilMax.isFinite() &&
            input.quantiteInitiale.isFinite() &&
            input.prixAchat >= 0.0 && input.prixVente >= 0.0 &&
            input.seuilMin >= 0.0 && input.seuilMax >= 0.0 &&
            input.quantiteInitiale >= 0.0 &&
            (input.seuilMax == 0.0 || input.seuilMax >= input.seuilMin)
    }
}
