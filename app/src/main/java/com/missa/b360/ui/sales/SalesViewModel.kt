package com.missa.b360.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.StockDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.SaleEntity
import com.missa.b360.core.data.entity.SaleStatus
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.domain.model.SaleCalculation
import com.missa.b360.core.domain.model.SaleFormCalculator
import com.missa.b360.core.domain.model.SaleFormError
import com.missa.b360.core.domain.model.SaleFormInput
import com.missa.b360.core.domain.model.SaleFormLine
import com.missa.b360.core.domain.model.SaleMoney
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleSaveOutcome
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaleMutationResult
import com.missa.b360.core.domain.usecase.SaleUseCases
import com.missa.b360.core.data.entity.OperationModule
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

/** Facture/vente enregistrée, avec les éléments nécessaires au succès et à l'aperçu PDF. */
data class SaleReceipt(
    val recordId: Long,
    val reference: String,
    val payload: SaleRecordPayload,
    val createdAt: Long = System.currentTimeMillis(),
    val completed: List<String> = emptyList(),
) {
    val clientName: String get() = payload.clientName
    val total: Double get() = payload.total
    val paidAmount: Double get() = payload.paidAmount
    val paymentMethod: String get() = payload.paymentMethod
}

/** Élément de l'historique : vente transactionnelle ou pièce héritée de l'ancien module. */
data class SaleHistoryItem(
    val id: Long,
    val sale: SaleEntity?,
    val record: OperationRecordEntity?,
    val reference: String,
    val clientName: String,
    val totalCents: Long?,
    val status: String,
    val createdAt: Long,
    val payload: SaleRecordPayload?,
) {
    val isDraft: Boolean get() = status == SaleStatus.DRAFT.code
    val isCancelled: Boolean get() = status == SaleStatus.CANCELLED.code
    val total: Double get() = totalCents?.let { SaleMoney.toDouble(it) } ?: payload?.total ?: 0.0
}

/** Règles d'activation des boutons, déterminées par le ViewModel (aucun calcul dans l'UI). */
data class SaleFormActions(
    val canSave: Boolean = false,
    val canDraft: Boolean = false,
    val canPrint: Boolean = false,
    val stockIssue: SaleFormLine? = null,
)

/** État unique du formulaire compact « Nouvelle vente ». Aucun calcul métier dans l'UI. */
data class SaleFormUiState(
    val saleId: Long? = null,
    val selectedClient: ClientEntity? = null,
    val walkIn: Boolean = false,
    val query: String = "",
    val categoryId: Long? = null,
    val lines: List<SaleFormLine> = emptyList(),
    val discountInput: String = "0",
    val discountPercentMode: Boolean = false,
    val deliveryInput: String = "0",
    val paymentMethod: String = "",
    val isCredit: Boolean = false,
    val receivedInput: String = "",
    val paidInput: String = "",
    val note: String = "",
    val internalReference: String = "",
    val detailsExpanded: Boolean = false,
    val saving: Boolean = false,
    val lastGeneratedReference: String? = null,
) {
    /** Vraie si une donnée saisie peut être perdue en quittant le formulaire. */
    val hasUnsavedInput: Boolean
        get() = selectedClient != null || walkIn || lines.isNotEmpty() ||
            query.isNotBlank() || discountInput != "0" || deliveryInput != "0" ||
            paymentMethod.isNotBlank() || receivedInput.isNotBlank() || paidInput.isNotBlank() ||
            note.isNotBlank() || internalReference.isNotBlank()
}

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val saleUseCases: SaleUseCases,
    private val operations: OperationUseCases,
    observeClients: ObserveClientsUseCase,
    private val stockDao: StockDao,
    taxDao: TaxDao,
    paymentMethodDao: PaymentMethodDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleFormUiState())
    val uiState: StateFlow<SaleFormUiState> = _uiState

    val clients: StateFlow<List<ClientEntity>> = observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<StockProductEntity>> = stockDao.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<StockCategoryEntity>> = stockDao.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taxRate: StateFlow<Double> = taxDao.observeAll()
        .map { taxes -> taxes.firstOrNull { it.parDefaut }?.taux ?: taxes.firstOrNull()?.taux ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val paymentMethods: StateFlow<List<String>> = paymentMethodDao.observeAll()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("Espèces", "Mobile Money", "Virement", "Carte", "Crédit"))

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { enterprise -> enterprise?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    val walkInAllowed: StateFlow<Boolean> = getEnterprise.observer()
        .map { enterprise ->
            // « Client comptoir » autorisé pour les profils commerce/services, refusé pour
            // les flux de production ou de projets qui exigent une fiche client réelle.
            enterprise?.profilActivite != "E"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val enterpriseName: StateFlow<String> = getEnterprise.observer()
        .map { it?.nom.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val history: StateFlow<List<SaleHistoryItem>> = combine(
        saleUseCases.observeSales(),
        operations.observe(OperationModule.VENTE),
    ) { sales, ops ->
        val salesByRef = sales.associateBy { it.reference }
        ops.map { record ->
            val sale = salesByRef[record.reference]
            val payload = SaleRecordCodec.decode(record.notes)
            SaleHistoryItem(
                id = sale?.id ?: record.id,
                sale = sale,
                record = record,
                reference = record.reference,
                clientName = sale?.clientName ?: payload?.clientName ?: record.counterpart ?: record.title,
                totalCents = sale?.totalCents ?: payload?.totalCents?.takeIf { it > 0 },
                status = sale?.status ?: record.status,
                createdAt = sale?.createdAt ?: record.createdAt,
                payload = payload,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calculation: StateFlow<SaleCalculation> = combine(_uiState, taxRate, devise) { state, tax, currency ->
        SaleFormCalculator.calculate(buildInput(state, tax, currency))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SaleCalculation(),
    )

    val validation: StateFlow<SaleFormError?> = combine(_uiState, taxRate, devise) { state, tax, currency ->
        val input = buildInput(state, tax, currency)
        val calc = SaleFormCalculator.calculate(input)
        SaleFormCalculator.validate(input, calc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val actions: StateFlow<SaleFormActions> = combine(_uiState, validation) { state, error ->
        val stockIssue = state.lines.firstOrNull {
            it.productId != null && it.stockAvailable != null && it.quantity > it.stockAvailable
        }
        val invalidLine = state.lines.any { it.name.isBlank() || it.quantity <= 0.0 || it.unitPriceCents <= 0L }
        val hasLines = state.lines.isNotEmpty() && !invalidLine && !state.saving
        val incompleteDraftOnly = error != null &&
            (error.code == com.missa.b360.core.domain.model.SaleErrorCode.CLIENT_REQUIRED ||
                error.code == com.missa.b360.core.domain.model.SaleErrorCode.PAYMENT_INVALID)
        val fullValid = error == null && stockIssue == null
        SaleFormActions(
            canSave = hasLines && fullValid,
            canDraft = hasLines && (fullValid || incompleteDraftOnly),
            canPrint = hasLines && fullValid,
            stockIssue = stockIssue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SaleFormActions())

    private val _saveResult = MutableStateFlow<SaleSaveOutcome?>(null)
    val saveResult: StateFlow<SaleSaveOutcome?> = _saveResult

    private var nextLineId = 1L

    private fun update(transform: (SaleFormUiState) -> SaleFormUiState) {
        _uiState.value = transform(_uiState.value)
    }

    // Récupération d'un brouillon existant.
    fun loadDraft(sale: SaleEntity, availableClients: List<ClientEntity>) {
        val client = sale.clientId?.let { id -> availableClients.firstOrNull { it.id == id } }
        nextLineId = 1L
        _uiState.value = SaleFormUiState(
            saleId = sale.id,
            selectedClient = client,
            walkIn = sale.clientId == null,
            lines = emptyList(),
            discountInput = sale.discountCents.toInputAmount(),
            deliveryInput = sale.deliveryCents.toInputAmount(),
            paymentMethod = sale.paymentMethod,
            isCredit = sale.isCredit,
            receivedInput = sale.paidCents.toInputAmount(),
            paidInput = sale.paidCents.toInputAmount(),
            note = sale.note.orEmpty(),
            internalReference = sale.internalReference.orEmpty(),
            lastGeneratedReference = sale.reference,
        )
        viewModelScope.launch {
            val detail = saleUseCases.getDetail(sale.id)
            if (detail != null) {
                _uiState.value = _uiState.value.copy(
                    lines = detail.lines.map { line ->
                        SaleFormLine(
                            id = nextLineId++,
                            productId = line.productId,
                            sku = line.sku,
                            name = line.name,
                            unit = line.unit,
                            unitPriceCents = line.unitPriceCents,
                            quantity = line.quantity,
                            discountPct = line.discountPct,
                            stockAvailable = if (line.productId != null && !line.freeProduct) null else 0.0,
                            freeProduct = line.freeProduct,
                        )
                    },
                )
            }
        }
    }

    fun duplicateSale(item: SaleHistoryItem, availableClients: List<ClientEntity>): Boolean {
        val payload = item.payload ?: return false
        val client = payload.clientId?.let { id -> availableClients.firstOrNull { it.id == id } }
        nextLineId = payload.lines.maxOfOrNull { it.id }?.plus(1) ?: 1L
        _uiState.value = SaleFormUiState(
            selectedClient = client,
            lines = payload.lines.map { line ->
                SaleFormLine(
                    id = nextLineId++,
                    productId = line.productId,
                    sku = line.sku,
                    name = line.name,
                    unit = line.unit,
                    unitPriceCents = SaleMoney.fromDouble(line.unitPrice),
                    quantity = line.quantity,
                    discountPct = line.discountPct,
                    freeProduct = line.freeProduct,
                    stockAvailable = null,
                )
            },
            discountInput = payload.discount.toInputAmount(),
            deliveryInput = payload.delivery.toInputAmount(),
            paymentMethod = payload.paymentMethod,
            isCredit = payload.isCredit,
            receivedInput = payload.paidAmount.toInputAmount(),
            paidInput = payload.paidAmount.toInputAmount(),
            note = payload.note.orEmpty(),
        )
        return true
    }

    fun newSale() {
        nextLineId = 1L
        _uiState.value = SaleFormUiState()
    }

    fun clearForm() {
        nextLineId = 1L
        _uiState.value = SaleFormUiState()
    }

    fun selectClient(client: ClientEntity) {
        _uiState.value = _uiState.value.copy(
            selectedClient = client,
            walkIn = false,
        )
    }

    fun setWalkIn(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            walkIn = enabled,
            selectedClient = if (enabled) null else _uiState.value.selectedClient,
        )
    }

    fun setQuery(value: String) = update { it.copy(query = value.take(120)) }
    fun setCategory(id: Long?) = update { it.copy(categoryId = id) }
    fun toggleDetails() = update { it.copy(detailsExpanded = !it.detailsExpanded) }
    fun updateNote(value: String) = update { it.copy(note = value.take(500)) }
    fun updateInternalReference(value: String) = update { it.copy(internalReference = value.take(80)) }

    fun updateDiscount(value: String) = update { it.copy(discountInput = value.moneyInput()) }
    fun updateDelivery(value: String) = update { it.copy(deliveryInput = value.moneyInput()) }
    fun updateReceived(value: String) = update { it.copy(receivedInput = value.moneyInput()) }
    fun updatePaid(value: String) = update { it.copy(paidInput = value.moneyInput()) }
    fun toggleDiscountPercentMode() = update { it.copy(discountPercentMode = !it.discountPercentMode) }
    fun toggleCredit() = update { current ->
        if (current.isCredit) {
            val cash = paymentMethods.value.firstOrNull { !it.isCreditLabel() } ?: "Espèces"
            current.copy(isCredit = false, paymentMethod = cash)
        } else {
            current.copy(
                isCredit = true,
                paymentMethod = current.paymentMethod.ifBlank { "Crédit" },
            )
        }
    }

    fun addProduct(product: StockProductEntity) {
        val current = _uiState.value
        val price = SaleMoney.fromDouble(product.prixVente)
        if (price <= 0L) return
        val existing = current.lines.firstOrNull { it.productId == product.id }
        if (existing != null) {
            changeQuantity(existing.id, 1.0)
        } else {
            _uiState.value = current.copy(
                lines = current.lines + SaleFormLine(
                    id = nextLineId++,
                    productId = product.id,
                    sku = product.code,
                    name = product.nom,
                    unit = product.unite,
                    unitPriceCents = price,
                    quantity = 1.0,
                    stockAvailable = product.quantite,
                    freeProduct = false,
                ),
            )
        }
    }

    fun addFreeProduct(name: String, unitPriceText: String, quantityText: String): Boolean {
        val productName = name.trim()
        val unitPrice = SaleMoney.parse(unitPriceText, SaleMoney.decimalsFor(devise.value))
        val quantity = quantityText.toQuantityOrNull()
        if (productName.length !in 2..120 || unitPrice == null || unitPrice <= 0L || quantity == null || quantity <= 0.0) {
            return false
        }
        val current = _uiState.value
        _uiState.value = current.copy(
            lines = current.lines + SaleFormLine(
                id = nextLineId++,
                productId = null,
                sku = null,
                name = productName,
                unit = "unité",
                unitPriceCents = unitPrice,
                quantity = quantity,
                stockAvailable = null,
                freeProduct = true,
            ),
        )
        return true
    }

    fun changeQuantity(lineId: Long, delta: Double) {
        update { current ->
            current.copy(
                lines = current.lines.mapNotNull { line ->
                    if (line.id != lineId) line
                    else {
                        val next = line.quantity + delta
                        if (next <= 0.0) null
                        else line.copy(quantity = next)
                    }
                },
            )
        }
    }

    fun setQuantity(lineId: Long, value: String) {
        val quantity = value.toQuantityOrNull() ?: return
        if (quantity <= 0.0) return
        update { current ->
            current.copy(
                lines = current.lines.map { line ->
                    if (line.id == lineId) line.copy(quantity = quantity) else line
                },
            )
        }
    }

    fun removeLine(lineId: Long) {
        update { it.copy(lines = it.lines.filterNot { line -> line.id == lineId }) }
    }

    fun setPaymentMethod(method: String) {
        update {
            it.copy(
                paymentMethod = method,
                isCredit = method.isCreditLabel(),
            )
        }
    }

    /** Enregistre la vente (brouillon ou définitive). */
    fun save(draft: Boolean, showSuccess: Boolean = true) {
        val state = _uiState.value
        if (state.saving) return
        _uiState.value = state.copy(saving = true)
        _saveResult.value = null
        viewModelScope.launch {
            try {
                val input = buildInput(state, taxRate.value, devise.value)
                when (val outcome = saleUseCases.save(input, draft)) {
                    is SaleSaveOutcome.Success -> {
                        _saveResult.value = outcome
                        clearFormWithoutResult()
                    }
                    is SaleSaveOutcome.Failed -> {
                        _saveResult.value = outcome
                    }
                    SaleSaveOutcome.ReadOnly -> {
                        _saveResult.value = SaleSaveOutcome.ReadOnly
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaleSaveOutcome.Failed(
                    SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.INTERNAL, key = "sale"),
                )
            } finally {
                _uiState.value = _uiState.value.copy(saving = false)
            }
        }
    }

    fun cancelSale(saleId: Long) {
        viewModelScope.launch {
            _saveResult.value = when (val result = saleUseCases.cancel(saleId)) {
                is SaleMutationResult.Success -> null
                is SaleMutationResult.ReadOnly -> SaleSaveOutcome.ReadOnly
                is SaleMutationResult.Failed -> SaleSaveOutcome.Failed(result.error)
            }
        }
    }

    fun validateDraft(saleId: Long) {
        viewModelScope.launch {
            _saveResult.value = when (val result = saleUseCases.validateDraft(saleId)) {
                is SaleMutationResult.Success -> null
                is SaleMutationResult.ReadOnly -> SaleSaveOutcome.ReadOnly
                is SaleMutationResult.Failed -> SaleSaveOutcome.Failed(result.error)
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(saving = false)
        _saveResult.value = null
    }

    private fun clearFormWithoutResult() {
        nextLineId = 1L
        val lastRef = _uiState.value.lastGeneratedReference
        _uiState.value = SaleFormUiState(lastGeneratedReference = lastRef)
    }

    private fun buildInput(state: SaleFormUiState, tax: Double, currency: String): SaleFormInput {
        return SaleFormInput(
            clientId = state.selectedClient?.id,
            clientName = state.selectedClient?.nom,
            walkIn = state.walkIn,
            lines = state.lines,
            discountInput = state.discountInput,
            discountPercentMode = state.discountPercentMode,
            deliveryInput = state.deliveryInput,
            taxRate = tax,
            paymentMethod = state.paymentMethod,
            isCredit = state.isCredit,
            receivedInput = state.receivedInput,
            paidInput = state.paidInput,
            note = state.note,
            internalReference = state.internalReference,
            sellerName = null,
            siteName = null,
            devise = currency,
        )
    }
}

private fun String.moneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }
private fun String.toQuantityOrNull(): Double? = trim()
    .replace(',', '.')
    .toDoubleOrNull()
    ?.takeIf { it.isFinite() && it >= 0.0 }

private fun String.isCreditLabel(): Boolean = contains("crédit", ignoreCase = true) ||
    contains("credit", ignoreCase = true)

private fun Long.toInputAmount(): String = (this / 100.0).let { value ->
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun Double.toInputAmount(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
