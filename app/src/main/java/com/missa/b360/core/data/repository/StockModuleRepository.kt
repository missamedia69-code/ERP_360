package com.missa.b360.core.data.repository

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.InventaireDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductEquipementDao
import com.missa.b360.core.data.dao.ProductExtrasDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.InventaireEntity
import com.missa.b360.core.data.entity.KitComposantEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductConsignationEntity
import com.missa.b360.core.data.entity.ProductDechetEntity
import com.missa.b360.core.data.entity.ProductEmballageEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductEquipementEntity
import com.missa.b360.core.data.entity.ProductKitEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StatutEquipement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Projection de lecture du détail d'article ; la UI ne connaît pas les DAO. */
data class StockProductDetailProjection(
    val product: ProductEntity? = null,
    val stocks: List<ProductStockEntity> = emptyList(),
    val sites: List<SiteEntity> = emptyList(),
    val category: ProductCategoryEntity? = null,
    val supplier: FournisseurEntity? = null,
    val movements: List<StockMovementView> = emptyList(),
    val equipment: ProductEquipementEntity? = null,
    val waste: ProductDechetEntity? = null,
    val packaging: ProductEmballageEntity? = null,
    val consignment: ProductConsignationEntity? = null,
    val kit: ProductKitEntity? = null,
    val kitComponents: List<KitComposantEntity> = emptyList(),
)

/** Extensions transverses éditables depuis le formulaire produit. */
data class StockProductExtensions(
    val waste: ProductDechetEntity? = null,
    val packaging: ProductEmballageEntity? = null,
    val consignment: ProductConsignationEntity? = null,
    val kit: ProductKitEntity? = null,
    val kitComponents: List<KitComposantEntity> = emptyList(),
)

/**
 * Point d'accès données des écrans Stock qui nécessitent plusieurs tables.
 * Il agrège les flux et garde les DAO dans la couche data ; les règles métier
 * et les transactions de documents restent dans les use cases spécialisés.
 */
@Singleton
class StockModuleRepository @Inject constructor(
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val siteDao: SiteDao,
    private val supplierDao: FournisseurDao,
    private val equipmentDao: ProductEquipementDao,
    private val extrasDao: ProductExtrasDao,
    private val inventoryDao: InventaireDao,
    private val database: AppDatabase,
) {
    fun observeCurrentInventory(): Flow<InventaireEntity?> = inventoryDao.observeEnCours()

    fun observeProductCategories(): Flow<List<ProductCategoryEntity>> = productDao.observeCategories()

    fun observeSuppliers(): Flow<List<FournisseurEntity>> = supplierDao.observeAll()

    fun observeAllEquipment(): Flow<List<ProductEquipementEntity>> = equipmentDao.observeAll()

    fun observeProductDetail(productId: Long): Flow<StockProductDetailProjection> {
        val articleAndStock = combine(
            productDao.observeById(productId),
            stockDao.observeToutes(),
            siteDao.observeAll(),
            movementDao.observeJointsPourProduit(productId, 1_500),
        ) { product, allStocks, sites, movements ->
            CoreDetail(product, allStocks, sites, movements)
        }
        val references = combine(
            productDao.observeCategories(),
            supplierDao.observeAll(),
            equipmentDao.observeById(productId),
        ) { categories, suppliers, equipment ->
            DetailReferences(categories, suppliers, equipment)
        }
        val extensions = combine(
            extrasDao.observeDechet(productId),
            extrasDao.observeEmballage(productId),
            extrasDao.observeConsignation(productId),
            extrasDao.observeKit(productId),
            extrasDao.observeComposants(productId),
        ) { waste, packaging, consignment, kit, components ->
            StockProductExtensions(waste, packaging, consignment, kit, components)
        }
        return combine(articleAndStock, references, extensions) { core, refs, extras ->
            val product = core.product
            StockProductDetailProjection(
                product = product,
                stocks = core.stocks.filter { it.produitId == productId },
                sites = core.sites,
                category = product?.categorieId?.let { id -> refs.categories.firstOrNull { it.id == id } },
                supplier = product?.fournisseurId?.let { id -> refs.suppliers.firstOrNull { it.id == id } },
                movements = core.movements.filter { it.produitId == productId },
                equipment = refs.equipment,
                waste = extras.waste,
                packaging = extras.packaging,
                consignment = extras.consignment,
                kit = extras.kit,
                kitComponents = extras.kitComponents,
            )
        }
    }

    fun observeProductExtensions(productId: Long): Flow<StockProductExtensions> = combine(
        extrasDao.observeDechet(productId),
        extrasDao.observeEmballage(productId),
        extrasDao.observeConsignation(productId),
        extrasDao.observeKit(productId),
        extrasDao.observeComposants(productId),
    ) { waste, packaging, consignment, kit, components ->
        StockProductExtensions(waste, packaging, consignment, kit, components)
    }

    suspend fun saveProductExtensions(
        productId: Long,
        equipment: ProductEquipementEntity?,
        extensions: StockProductExtensions,
    ) = database.withTransaction {
        equipment?.let { equipmentDao.upsert(it.copy(produitId = productId)) }
        extensions.waste?.let { extrasDao.upsertDechet(it.copy(produitId = productId)) }
        extensions.packaging?.let { extrasDao.upsertEmballage(it.copy(produitId = productId)) }
        extensions.consignment?.let { extrasDao.upsertConsignation(it.copy(produitId = productId)) }
        extensions.kit?.let { extrasDao.upsertKit(it.copy(produitId = productId)) }
        if (extensions.kit != null) {
            extensions.kitComponents.forEach { component ->
                extrasDao.upsertComposant(component.copy(kitId = productId))
            }
        }
    }

    suspend fun updateProductPhoto(productId: Long, photoPath: String?) = database.withTransaction {
        productDao.getById(productId)?.let { product ->
            productDao.update(product.copy(photoPath = photoPath))
        }
    }

    suspend fun setEquipmentStatus(productId: Long, status: StatutEquipement) {
        equipmentDao.setStatut(productId, status.name)
    }

    private data class CoreDetail(
        val product: ProductEntity?,
        val stocks: List<ProductStockEntity>,
        val sites: List<SiteEntity>,
        val movements: List<StockMovementView>,
    )

    private data class DetailReferences(
        val categories: List<ProductCategoryEntity>,
        val suppliers: List<FournisseurEntity>,
        val equipment: ProductEquipementEntity?,
    )
}
