package com.missa.b360.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleTotals
import com.missa.b360.core.domain.usecase.CheckSaleStockUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.usecase.ReverseSaleStockUseCase
import com.missa.b360.core.domain.usecase.SaveSaleUseCase
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
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
import kotlin.math.abs

/** État d'un panier de vente en cours, jamais prérempli avec des articles fictifs. */
data class SalesUiState(
    val selectedClient: ClientEntity? = null,
    val lines: List<SaleLine> = emptyList(),
    val discountInput: String = "0",
    val deliveryInput: String = "0",
    val paidInput: String = "",
    val note: String = "",
    /** Id de la pièce brouillon en cours de reprise, null pour une nouvelle vente. */
    val editingRecordId: Long? = null,
) {
    fun totals(taxRate: Double): SaleTotals = SaleCalculator.calculate(
        lines = lines,
        discount = discountInput.toMoneyOrZero(),
        delivery = deliveryInput.toMoneyOrZero(),
        taxRate = taxRate,
    )
}

/** Facture créée ou reprise, avec les éléments nécessaires aux écrans de succès et aperçu. */
data class SaleReceipt(
    val recordId: Long,
    val reference: String,
    val payload: SaleRecordPayload,
    /** Date de la pièce persistante, pas la date à laquelle la facture est rouverte. */
    val createdAt: Long = System.currentTimeMillis(),
) {
    val clientName: String get() = payload.clientName
    val total: Double get() = payload.total
    val paidAmount: Double get() = payload.paidAmount
    val paymentMethod: String get() = payload.paymentMethod
}

@HiltViewModel
class SalesViewModel @Inject constructor(
    operations: OperationUseCases,
    observeClients: ObserveClientsUseCase,
    taxDao: TaxDao,
    paymentMethodDao: PaymentMethodDao,
    getEnterprise: GetEnterpriseUseCase,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    private val saveSale: SaveSaleUseCase,
    private val reverseSaleStock: ReverseSaleStockUseCase,
    private val checkSaleStock: CheckSaleStockUseCase,
) : ViewModel() {

    sealed interface SaveResult {
        data class Saved(val receipt: SaleReceipt, val shouldPrint: Boolean) : SaveResult
        data object MissingClient : SaveResult
        data object EmptyCart : SaveResult
        data object InvalidAmount : SaveResult
        data object ReadOnly : SaveResult
        /** Stock insuffisant (contrôle UI ou transactionnel — spec §43/§44). */
        data class StockInsuffisant(val produitNom: String, val disponible: Double, val demande: Double) : SaveResult
        data object Cancelled : SaveResult
        data object Error : SaveResult
    }

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState

    val clients: Flow<List<ClientEntity>> = observeClients()
    val taxRate: StateFlow<Double> = taxDao.observeAll()
        .map { taxes -> taxes.firstOrNull { it.parDefaut }?.taux ?: taxes.firstOrNull()?.taux ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val paymentMethods: StateFlow<List<String>> = paymentMethodDao.observeAll()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devise: StateFlow<String> = getEnterprise.observer()
        .map { enterprise -> enterprise?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")
    val history: Flow<List<OperationRecordEntity>> = operations.observe(OperationModule.VENTE)
    /** Catalogue produits avec stock courant (spec §9 : ajout par recherche). */
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult
    /** Anti double-soumission : sauvegarde et annulation en cours (spec §3 SAUVEGARDE). */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _cancelling = MutableStateFlow(false)
    val cancelling: StateFlow<Boolean> = _cancelling
    private var nextLineId = 1L

    fun selectClient(client: ClientEntity) {
        _uiState.value = _uiState.value.copy(selectedClient = client)
    }

    /** Reprend un brouillon persistant dans le panier sans créer de deuxième facture. */
    fun loadDraft(record: OperationRecordEntity, availableClients: List<ClientEntity>): Boolean {
        if (record.status != OperationStatus.DRAFT.name) return false
        val payload = SaleRecordCodec.decode(record.notes) ?: return false
        val client = availableClients.firstOrNull { it.id == payload.clientId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = SalesUiState(
            selectedClient = client,
            lines = payload.lines,
            discountInput = payload.discount.toInputAmount(),
            deliveryInput = payload.delivery.toInputAmount(),
            paidInput = payload.paidAmount.toInputAmount(),
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    /** Produit du catalogue : ajout rattaché au produit (génère la sortie de stock). */
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
                    lines = current.lines + SaleLine(
                        id = nextLineId++,
                        name = product.nom,
                        unitPrice = product.prixVente ?: 0.0,
                        quantity = 1.0,
                        productId = product.product.id,
                    ),
                )
            }
        }
    }

    /** Produit libre (non rattaché au stock) — conservation du comportement existant. */
    fun addFreeProduct(name: String, unitPriceText: String, quantityText: String): Boolean {
        val productName = name.trim()
        val unitPrice = unitPriceText.toMoneyOrNull()
        val quantity = quantityText.toMoneyOrNull()
        if (productName.length !in 2..120 || unitPrice == null || unitPrice <= 0.0 ||
            quantity == null || quantity <= 0.0
        ) {
            return false
        }
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines + SaleLine(
                    id = nextLineId++,
                    name = productName,
                    unitPrice = unitPrice,
                    quantity = quantity,
                ),
            )
        }
        return true
    }

    /** Modification de ligne (quantité et prix unitaire — spec §9 PANIER). */
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

    fun removeLine(lineId: Long) {
        updateKeepingFullPayment { current ->
            current.copy(lines = current.lines.filterNot { it.id == lineId })
        }
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            lines = emptyList(),
            note = "",
            discountInput = "0",
            deliveryInput = "0",
            paidInput = "",
            editingRecordId = null,
        )
    }

    fun updateDiscount(value: String) {
        updateKeepingFullPayment { it.copy(discountInput = value.filterMoneyInput()) }
    }

    fun updateDelivery(value: String) {
        updateKeepingFullPayment { it.copy(deliveryInput = value.filterMoneyInput()) }
    }

    fun updatePaid(value: String) {
        _uiState.value = _uiState.value.copy(paidInput = value.filterMoneyInput())
    }

    /** Garde le paiement au total lorsque l'utilisateur n'a pas choisi un paiement partiel. */
    private fun updateKeepingFullPayment(transform: (SalesUiState) -> SalesUiState) {
        val current = _uiState.value
        val totalBefore = current.totals(taxRate = 0.0).total
        val paidBefore = current.paidInput.toMoneyOrNull()
        val paymentWasFull = current.paidInput.isBlank() ||
            (paidBefore != null && abs(paidBefore - totalBefore) < 0.001)
        val updated = transform(current)
        _uiState.value = if (paymentWasFull) {
            updated.copy(paidInput = updated.totals(taxRate = 0.0).total.toInputAmount())
        } else {
            updated
        }
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value.take(500))
    }

    /**
     * Enregistre la vente (brouillon ou validée) — la persistance est **transactionnelle**
     * (spec §44) : pièce + mouvements de stock + journal dans une seule transaction.
     * Un brouillon n'a aucun effet sur le stock ni sur la finance (spec §3).
     */
    fun save(paymentMethod: String, draft: Boolean) {
        if (_saving.value) return
        val sale = _uiState.value
        val client = sale.selectedClient ?: run {
            _saveResult.value = SaveResult.MissingClient
            return
        }
        if (sale.lines.isEmpty()) {
            _saveResult.value = SaveResult.EmptyCart
            return
        }
        val totals = sale.totals(taxRate.value)
        val paidAmount = sale.paidInput.toMoneyOrNull() ?: totals.total
        if (paidAmount < 0.0 || paidAmount > totals.total || paymentMethod.isBlank()) {
            _saveResult.value = SaveResult.InvalidAmount
            return
        }

        viewModelScope.launch {
            _saving.value = true
            try {
                val payload = SaleRecordPayload(
                    clientId = client.id,
                    clientName = client.nom,
                    lines = sale.lines,
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    delivery = totals.delivery,
                    taxRate = taxRate.value,
                    taxAmount = totals.taxAmount,
                    total = totals.total,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    note = sale.note.trim().ifBlank { null },
                )
                // Contrôle de disponibilité UI (l'autorité reste le contrôle transactionnel).
                if (!draft) {
                    checkSaleStock.premierDeficit(payload)?.let { deficit ->
                        _saveResult.value = SaveResult.StockInsuffisant(
                            deficit.produitNom,
                            deficit.disponible,
                            deficit.demande,
                        )
                        return@launch
                    }
                }
                when (val result = saveSale(recordId = sale.editingRecordId, payload = payload, draft = draft)) {
                    is SaveSaleUseCase.Result.Succes -> {
                        _saveResult.value = SaveResult.Saved(
                            receipt = SaleReceipt(
                                recordId = result.recordId,
                                reference = result.reference,
                                payload = payload,
                            ),
                            shouldPrint = !draft,
                        )
                        clearCart()
                    }
                    SaveSaleUseCase.Result.LectureSeule -> _saveResult.value = SaveResult.ReadOnly
                    SaveSaleUseCase.Result.DonneesInvalides -> _saveResult.value = SaveResult.InvalidAmount
                    SaveSaleUseCase.Result.BrouillonIntrouvable -> _saveResult.value = SaveResult.Error
                    is SaveSaleUseCase.Result.StockInsuffisant -> _saveResult.value = SaveResult.StockInsuffisant(
                        result.produitNom,
                        result.disponible,
                        result.demande,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaveResult.Error
            } finally {
                _saving.value = false
            }
        }
    }

    /** Prépare une nouvelle vente à partir d'une facture existante sans réutiliser sa référence. */
    fun duplicate(payload: SaleRecordPayload, availableClients: List<ClientEntity>): Boolean {
        val client = availableClients.firstOrNull { it.id == payload.clientId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = SalesUiState(
            selectedClient = client,
            lines = payload.lines.map { it.copy(id = nextLineId++) },
            discountInput = payload.discount.toInputAmount(),
            deliveryInput = payload.delivery.toInputAmount(),
            paidInput = payload.paidAmount.toInputAmount(),
            note = payload.note.orEmpty(),
        )
        return true
    }

    /**
     * Annulation d'une vente validée — **compensation** (C7) : statut ANNULÉ et
     * recomposition du stock par des mouvements d'entrée.
     */
    fun cancelSale(id: Long) {
        if (_cancelling.value) return
        viewModelScope.launch {
            _cancelling.value = true
            try {
                when (reverseSaleStock(id)) {
                    is ReverseSaleStockUseCase.Result.Succes -> _saveResult.value = SaveResult.Cancelled
                    else -> _saveResult.value = SaveResult.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaveResult.Error
            } finally {
                _cancelling.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}

/**
 * Total encore dû sur les ventes validées ayant un détail de facture structuré.
 * Les avoirs de retour (`sourceRecordId` renseigné) **réduisent** le solde (spec §22).
 */
fun outstandingBalance(records: List<OperationRecordEntity>, clientId: Long?): Double {
    if (clientId == null) return 0.0
    return records
        .asSequence()
        .filter { it.status == OperationStatus.VALIDATED.name }
        .mapNotNull { SaleRecordCodec.decode(it.notes) }
        .filter { it.clientId == clientId }
        .sumOf {
            val solde = (it.total - it.paidAmount).coerceAtLeast(0.0)
            if (it.sourceRecordId != null) -solde else solde
        }
}

internal fun String.toMoneyOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() }

private fun String.toMoneyOrZero(): Double = toMoneyOrNull()?.coerceAtLeast(0.0) ?: 0.0

internal fun String.filterMoneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }

internal fun Double.toInputAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
