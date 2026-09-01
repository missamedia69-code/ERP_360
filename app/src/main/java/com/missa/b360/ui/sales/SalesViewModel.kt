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
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationStatus
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

data class SaleReceipt(
    val reference: String,
    val clientName: String,
    val total: Double,
    val paidAmount: Double,
    val paymentMethod: String,
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val operations: OperationUseCases,
    observeClients: ObserveClientsUseCase,
    taxDao: TaxDao,
    paymentMethodDao: PaymentMethodDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    sealed interface SaveResult {
        data class Saved(val receipt: SaleReceipt, val shouldPrint: Boolean) : SaveResult
        data object MissingClient : SaveResult
        data object EmptyCart : SaveResult
        data object InvalidAmount : SaveResult
        data object ReadOnly : SaveResult
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

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult
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

    fun save(paymentMethod: String, draft: Boolean) {
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
                val params = OperationUseCases.CreateParams(
                    module = OperationModule.VENTE,
                    title = client.nom,
                    counterpart = client.nom,
                    amount = totals.total,
                    notes = SaleRecordCodec.encode(payload),
                )

                suspend fun completeSave(id: Long, reference: String) {
                    if (!draft && !operations.setStatus(id, OperationStatus.VALIDATED)) {
                        _saveResult.value = SaveResult.Error
                        return
                    }
                    _saveResult.value = SaveResult.Saved(
                        receipt = SaleReceipt(
                            reference = reference,
                            clientName = client.nom,
                            total = totals.total,
                            paidAmount = paidAmount,
                            paymentMethod = paymentMethod,
                        ),
                        shouldPrint = !draft,
                    )
                    clearCart()
                }

                val draftId = sale.editingRecordId
                if (draftId == null) {
                    when (val created = operations.create(params)) {
                        is OperationUseCases.CreateResult.Success -> completeSave(created.id, created.reference)
                        OperationUseCases.CreateResult.Invalid -> _saveResult.value = SaveResult.InvalidAmount
                        OperationUseCases.CreateResult.ReadOnly -> _saveResult.value = SaveResult.ReadOnly
                    }
                } else {
                    when (val updated = operations.updateDraft(draftId, params)) {
                        is OperationUseCases.UpdateDraftResult.Success -> completeSave(updated.id, updated.reference)
                        OperationUseCases.UpdateDraftResult.Invalid -> _saveResult.value = SaveResult.InvalidAmount
                        OperationUseCases.UpdateDraftResult.ReadOnly -> _saveResult.value = SaveResult.ReadOnly
                        OperationUseCases.UpdateDraftResult.NotDraft -> _saveResult.value = SaveResult.Error
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaveResult.Error
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}

/** Total encore dû sur les ventes validées ayant un détail de facture structuré. */
fun outstandingBalance(records: List<OperationRecordEntity>, clientId: Long?): Double {
    if (clientId == null) return 0.0
    return records
        .asSequence()
        .filter { it.status == OperationStatus.VALIDATED.name }
        .mapNotNull { SaleRecordCodec.decode(it.notes) }
        .filter { it.clientId == clientId }
        .sumOf { (it.total - it.paidAmount).coerceAtLeast(0.0) }
}

private fun String.toMoneyOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() }

private fun String.toMoneyOrZero(): Double = toMoneyOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun String.filterMoneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }

private fun Double.toInputAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
