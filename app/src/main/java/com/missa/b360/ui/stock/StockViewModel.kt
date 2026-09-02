package com.missa.b360.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockInventoryEntity
import com.missa.b360.core.data.entity.StockInventoryLineEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockWarehouseEntity
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.StockUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel du module Stock : agrégats live depuis la base et écritures métier. */
@HiltViewModel
class StockViewModel @Inject constructor(
    private val stock: StockUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    val products: Flow<List<StockProductEntity>> = stock.observeProducts()
    val categories: Flow<List<StockCategoryEntity>> = stock.observeCategories()
    val warehouses: Flow<List<StockWarehouseEntity>> = stock.observeWarehouses()
    val movements: Flow<List<StockMovementEntity>> = stock.observeMovements()
    val inventories: Flow<List<StockInventoryEntity>> = stock.observeInventories()

    fun product(id: Long): Flow<StockProductEntity?> = stock.observeProduct(id)
    fun productMovements(id: Long): Flow<List<StockMovementEntity>> = stock.observeMovementsByProduct(id)
    fun inventoryLines(id: Long): Flow<List<StockInventoryLineEntity>> = stock.observeInventoryLines(id)

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    private val _result = MutableStateFlow<StockUiResult?>(null)
    val result: StateFlow<StockUiResult?> = _result

    init {
        viewModelScope.launch {
            try {
                stock.ensureDefaults()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Les défauts (entrepôt + catégories) sont non bloquants ; l'utilisateur peut en créer.
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }

    fun createProduct(input: StockUseCases.ProductInput) {
        runWrite { stock.createProduct(input) }
    }

    fun updateProduct(id: Long, input: StockUseCases.ProductInput) {
        runWrite { stock.updateProduct(id, input) }
    }

    fun createEntry(
        productId: Long,
        warehouseId: Long?,
        quantity: Double,
        price: Double?,
        counterpart: String?,
        date: Long,
        notes: String?,
    ) = runWrite {
        stock.createEntry(productId, warehouseId, quantity, price, counterpart, date, notes)
    }

    fun createExit(
        productId: Long,
        warehouseId: Long?,
        quantity: Double,
        price: Double?,
        counterpart: String?,
        date: Long,
        notes: String?,
    ) = runWrite {
        stock.createExit(productId, warehouseId, quantity, price, counterpart, date, notes)
    }

    fun createTransfer(
        productId: Long,
        sourceWarehouseId: Long?,
        targetWarehouseId: Long?,
        quantity: Double,
        date: Long,
        notes: String?,
    ) = runWrite {
        stock.createTransfer(productId, sourceWarehouseId, targetWarehouseId, quantity, date, notes)
    }

    fun createAdjustment(
        productId: Long,
        warehouseId: Long?,
        countedQuantity: Double,
        date: Long,
        notes: String?,
    ) = runWrite {
        stock.createAdjustment(productId, warehouseId, countedQuantity, date, notes)
    }

    fun createInventory(
        warehouseId: Long?,
        date: Long,
        notes: String?,
        lines: List<StockUseCases.InventoryLineInput>,
    ) = runWrite {
        stock.createInventory(warehouseId, date, notes, lines)
    }

    fun validateInventory(id: Long) = runWrite { stock.validateInventory(id) }
    fun completeInventory(id: Long) = runWrite { stock.completeInventory(id) }

    fun addWarehouse(nom: String, adresse: String?) = runWrite { stock.addWarehouse(nom, adresse) }
    fun addCategorie(nom: String, couleur: String) = runWrite { stock.addCategorie(nom, couleur) }

    private fun runWrite(block: suspend () -> StockUseCases.Result) {
        viewModelScope.launch {
            try {
                _result.value = when (val r = block()) {
                    is StockUseCases.Result.Success -> StockUiResult.Success(r.reference)
                    StockUseCases.Result.ReadOnly -> StockUiResult.ReadOnly
                    StockUseCases.Result.Invalid -> StockUiResult.Invalid
                    StockUseCases.Result.Missing -> StockUiResult.Missing
                    StockUseCases.Result.Error -> StockUiResult.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = StockUiResult.Error
            }
        }
    }
}

sealed interface StockUiResult {
    data class Success(val reference: String) : StockUiResult
    data object ReadOnly : StockUiResult
    data object Invalid : StockUiResult
    data object Missing : StockUiResult
    data object Error : StockUiResult
}
