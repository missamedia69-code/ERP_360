package com.missa.b360.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SavePurchaseUseCase
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.core.util.filterMoneyInput
import com.missa.b360.core.util.toInputAmount
import com.missa.b360.core.util.toMoneyOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Panier d'une facture fournisseur en cours — jamais prérempli (aucune donnée fictive). */
data class PurchaseUiState(
    val supplier: FournisseurEntity? = null,
    val lines: List<PurchaseLine> = emptyList(),
    val paidInput: String = "",
    val note: String = "",
    val editingRecordId: Long? = null,
)

@HiltViewModel
class PurchasesViewModel @Inject constructor(
    operations: OperationUseCases,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    private val fournisseurDao: FournisseurDao,
    taxDao: TaxDao,
    paymentMethodDao: PaymentMethodDao,
    getEnterprise: GetEnterpriseUseCase,
    private val savePurchase: SavePurchaseUseCase,
) : ViewModel() {

    sealed interface SaveResult {
        data class Saved(val reference: String, val isDraft: Boolean) : SaveResult
        data object MissingSupplier : SaveResult
        data object EmptyCart : SaveResult
        data object InvalidAmount : SaveResult
        data object ReadOnly : SaveResult
        data object FournisseurIntrouvable : SaveResult
        data object Error : SaveResult
    }

    val purchases: Flow<List<OperationRecordEntity>> = operations.observe(OperationModule.ACHATS)

    /** Catalogue produits avec stock courant — les prix affichés sont les prix d'achat. */
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suppliers: StateFlow<List<FournisseurEntity>> = fournisseurDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taxRate: StateFlow<Double> = taxDao.observeAll()
        .map { taxes -> taxes.firstOrNull { it.parDefaut }?.taux ?: taxes.firstOrNull()?.taux ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val paymentMethods: StateFlow<List<String>> = paymentMethodDao.observeAll()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult

    private var nextLineId = 1L

    fun selectSupplier(supplier: FournisseurEntity) {
        _uiState.value = _uiState.value.copy(supplier = supplier)
    }

    /** Reprend un brouillon fournisseur sans créer de deuxième pièce. */
    fun loadDraft(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = PurchaseRecordCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = PurchaseUiState(
            supplier = supplier,
            lines = payload.lines,
            paidInput = payload.paidAmount.toInputAmount(),
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            supplier = null,
            lines = emptyList(),
            paidInput = "",
            note = "",
            editingRecordId = null,
        )
    }

    fun addCatalogProduct(product: ProductWithStock) {
        updateKeepingFullPayment { current ->
            val existing = current.lines.firstOrNull { it.productId == product.product.id }
            if (existing != null) {
                current.copy(
                    lines = current.lines.map {
                        if (it.id == existing.id) it.copy(quantity = it.quantity + 1.0) else it
                    },
                )
            } else {
                current.copy(
                    lines = current.lines + PurchaseLine(
                        id = nextLineId++,
                        name = product.product.nom,
                        unitPrice = product.product.prixAchat ?: 0.0,
                        quantity = 1.0,
                        productId = product.product.id,
                    ),
                )
            }
        }
    }

    fun changeQuantity(lineId: Long, delta: Double) {
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines.mapNotNull { line ->
                    if (line.id != lineId) line
                    else line.copy(quantity = line.quantity + delta).takeIf { it.quantity > 0.0 }
                },
            )
        }
    }

    fun updateLine(lineId: Long, quantity: Double, unitPrice: Double) {
        if (!quantity.isFinite() || quantity <= 0.0 || !unitPrice.isFinite() || unitPrice < 0.0) return
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines.map {
                    if (it.id == lineId) it.copy(quantity = quantity, unitPrice = unitPrice) else it
                },
            )
        }
    }

    fun removeLine(lineId: Long) {
        updateKeepingFullPayment { current ->
            current.copy(lines = current.lines.filterNot { it.id == lineId })
        }
    }

    fun updatePaid(value: String) {
        _uiState.value = _uiState.value.copy(paidInput = value.filterMoneyInput())
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value.take(500))
    }

    private fun updateKeepingFullPayment(transform: (PurchaseUiState) -> PurchaseUiState) {
        val current = _uiState.value
        val totalBefore = current.lines.sumOf { it.total }
        val paidBefore = current.paidInput.toMoneyOrNull()
        val paymentWasFull = current.paidInput.isBlank() ||
            (paidBefore != null && kotlin.math.abs(paidBefore - totalBefore) < 0.001)
        val updated = transform(current)
        _uiState.value = if (paymentWasFull) {
            updated.copy(paidInput = updated.lines.sumOf { it.total }.toInputAmount())
        } else {
            updated
        }
    }

    fun total(): Double = _uiState.value.lines.sumOf { it.total }.coerceAtLeast(0.0)

    /**
     * Enregistre la facture fournisseur (brouillon ou validée) — la persistance est
     * **transactionnelle** (spec §6) : pièce + entrées de stock + journal.
     * Le passif fournisseur = total − réglé.
     */
    fun save(paymentMethod: String, draft: Boolean) {
        if (_busy.value) return
        val state = _uiState.value
        val supplier = state.supplier ?: run {
            _saveResult.value = SaveResult.MissingSupplier
            return
        }
        if (state.lines.isEmpty()) {
            _saveResult.value = SaveResult.EmptyCart
            return
        }
        val total = state.lines.sumOf { it.total }.coerceAtLeast(0.0)
        val taxRate = this.taxRate.value
        val paidAmount = state.paidInput.toMoneyOrNull() ?: total
        if (paidAmount < 0.0 || paidAmount > total || paymentMethod.isBlank() || total <= 0.0) {
            _saveResult.value = SaveResult.InvalidAmount
            return
        }

        viewModelScope.launch {
            _busy.value = true
            try {
                val payload = PurchaseRecordPayload(
                    supplierId = supplier.id,
                    supplierName = supplier.nom,
                    lines = state.lines,
                    subtotal = total,
                    taxRate = taxRate,
                    taxAmount = if (taxRate == 0.0) 0.0 else total * taxRate / (100.0 + taxRate),
                    total = total,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    note = state.note.trim().ifBlank { null },
                )
                when (val result = savePurchase(recordId = state.editingRecordId, payload = payload, draft = draft)) {
                    is SavePurchaseUseCase.Result.Succes -> {
                        _saveResult.value = SaveResult.Saved(result.reference, draft)
                        clearCart()
                    }
                    SavePurchaseUseCase.Result.LectureSeule -> _saveResult.value = SaveResult.ReadOnly
                    SavePurchaseUseCase.Result.DonneesInvalides -> _saveResult.value = SaveResult.InvalidAmount
                    SavePurchaseUseCase.Result.FournisseurIntrouvable -> _saveResult.value = SaveResult.FournisseurIntrouvable
                    SavePurchaseUseCase.Result.BrouillonIntrouvable -> _saveResult.value = SaveResult.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaveResult.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
