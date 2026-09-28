package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.entity.InventaireEntity
import com.missa.b360.core.data.entity.KitComposantEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductConsignationEntity
import com.missa.b360.core.data.entity.ProductDechetEntity
import com.missa.b360.core.data.entity.ProductEmballageEntity
import com.missa.b360.core.data.entity.ProductEquipementEntity
import com.missa.b360.core.data.entity.ProductKitEntity
import com.missa.b360.core.data.entity.StatutEquipement
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.repository.StockModuleRepository
import com.missa.b360.core.data.repository.StockProductDetailProjection
import com.missa.b360.core.data.repository.StockProductExtensions
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Cas d'accès aux projections Stock multi-tables utilisées par les écrans du module. */
class StockModuleUseCases @Inject constructor(
    private val repository: StockModuleRepository,
) {
    fun observeCurrentInventory(): Flow<InventaireEntity?> = repository.observeCurrentInventory()

    fun observeProductCategories(): Flow<List<ProductCategoryEntity>> = repository.observeProductCategories()

    fun observeSuppliers(): Flow<List<FournisseurEntity>> = repository.observeSuppliers()

    fun observeAllEquipment(): Flow<List<ProductEquipementEntity>> = repository.observeAllEquipment()

    fun observeProductDetail(productId: Long): Flow<StockProductDetailProjection> =
        repository.observeProductDetail(productId)

    fun observeProductExtensions(productId: Long): Flow<StockProductExtensions> =
        repository.observeProductExtensions(productId)

    suspend fun saveProductExtensions(
        productId: Long,
        equipment: ProductEquipementEntity?,
        waste: ProductDechetEntity?,
        packaging: ProductEmballageEntity?,
        consignment: ProductConsignationEntity?,
        kit: ProductKitEntity?,
        kitComponents: List<KitComposantEntity>,
    ) = repository.saveProductExtensions(
        productId = productId,
        equipment = equipment,
        extensions = StockProductExtensions(waste, packaging, consignment, kit, kitComponents),
    )

    suspend fun updateProductPhoto(productId: Long, photoPath: String?) =
        repository.updateProductPhoto(productId, photoPath)

    suspend fun setEquipmentStatus(productId: Long, status: StatutEquipement) =
        repository.setEquipmentStatus(productId, status)
}
