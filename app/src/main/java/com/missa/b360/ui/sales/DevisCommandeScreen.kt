package com.missa.b360.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.usecase.CommercialTarget
import com.missa.b360.core.domain.usecase.CommercialTargets
import com.missa.b360.core.domain.usecase.ConvertDevisToOrderUseCase
import com.missa.b360.core.domain.usecase.ConvertOrderToSaleUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaveDevisCommandeUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Famille d'écran : « vente » (D/C) ou « prestations » (DP/OS) — même moteur. */
enum class DcFamily { VENTE, PRESTATIONS }

/**
 * Cycle commercial (spec §20) — UNE page, deux familles :
 * ventes (devis → commande → facture) et prestations (DP → OS → facture).
 * Aucune donnée fictive ; le stock n'est touché qu'à la facturation.
 */
@HiltViewModel
class DevisCommandeViewModel @Inject constructor(
    private val operations: OperationUseCases,
    observeClients: ObserveClientsUseCase,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    taxDao: TaxDao,
    paymentMethodDao: PaymentMethodDao,
    getEnterprise: GetEnterpriseUseCase,
    private val saveDevisCommande: SaveDevisCommandeUseCase,
    private val convertDevis: ConvertDevisToOrderUseCase,
    private val convertOrder: ConvertOrderToSaleUseCase,
) : ViewModel() {

    sealed interface Result {
        data class Saved(val reference: String, val isDevis: Boolean) : Result
        data object ClientRequired : Result
        data object LinesRequired : Result
        data object InvalidAmount : Result
        data object ClientMissing : Result
        data object ReadOnly : Result
        data object Error : Result
        data class OrderCreated(val reference: String) : Result
        data class Invoiced(val reference: String) : Result
        data object AlreadyInvoiced : Result
        data object AlreadyCancelled : Result
        data object PiecIntrouvable : Result
        data class StockInsuffisant(val nom: String, val dispo: Double, val demande: Double) : Result
        data object Cancelled : Result
    }

    // --- Famille ventes (D / C) ---
    val devis: StateFlow<List<OperationRecordEntity>> =
        operations.observe(com.missa.b360.core.data.entity.OperationModule.DEVIS)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commandes: StateFlow<List<OperationRecordEntity>> =
        operations.observe(com.missa.b360.core.data.entity.OperationModule.COMMANDE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Famille prestations (DP / OS, module SERVICES) ---
    private val services: StateFlow<List<OperationRecordEntity>> =
        operations.observe(com.missa.b360.core.data.entity.OperationModule.SERVICES)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val prestationsDevis: StateFlow<List<OperationRecordEntity>> = services
        .map { list -> list.filter { it.reference.startsWith("DP") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val prestationsCommandes: StateFlow<List<OperationRecordEntity>> = services
        .map { list -> list.filter { it.reference.startsWith("OS") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Commandes déjà rattachées à une facture — interdiction de double facturation. */
    val facturees: StateFlow<Set<Long>> = combine(
        commandes,
        prestationsCommandes,
        operations.observe(com.missa.b360.core.data.entity.OperationModule.VENTE),
    ) { commandees, commandesPrestations, ventes ->
        val payloads = ventes.mapNotNull { SaleRecordCodec.decode(it.notes) }
        (commandees + commandesPrestations).filter { commande ->
            com.missa.b360.core.domain.model.DevisCommandeRules.estFacturee(payloads, commande.id)
        }.map { it.id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val clients: StateFlow<List<ClientEntity>> = observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
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

    data class DcUiState(
        val target: CommercialTarget = CommercialTarget.Devis,
        val client: ClientEntity? = null,
        val lines: List<SaleLine> = emptyList(),
        val discountInput: String = "",
        val deliveryInput: String = "",
        val note: String = "",
        val editingRecordId: Long? = null,
    )

    private val _uiState = MutableStateFlow(DcUiState())
    val uiState: StateFlow<DcUiState> = _uiState

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    private var nextLineId = 1L

    fun totals(): com.missa.b360.core.domain.model.SaleTotals {
        val s = _uiState.value
        return SaleCalculator.calculate(
            lines = s.lines,
            discount = s.discountInput.toMoneyOrNull() ?: 0.0,
            delivery = s.deliveryInput.toMoneyOrNull() ?: 0.0,
            taxRate = taxRate.value,
        )
    }

    fun startNew(target: CommercialTarget) {
        _uiState.value = DcUiState(target = target)
    }

    /** Recharge une pièce dans le formulaire (édition). */
    fun loadForEdit(record: OperationRecordEntity): Boolean {
        val payload = SaleRecordCodec.decode(record.notes) ?: return false
        val target = CommercialTargets.fromRecord(record) ?: return false
        val client = clients.value.firstOrNull { it.id == payload.clientId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = DcUiState(
            target = target,
            client = client,
            lines = payload.lines,
            discountInput = payload.discount.toInputAmount(),
            deliveryInput = payload.delivery.toInputAmount(),
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    /** Duplique le contenu dans une pièce vierge (nouvelle référence). */
    fun duplicate(record: OperationRecordEntity): Boolean {
        val payload = SaleRecordCodec.decode(record.notes) ?: return false
        val target = CommercialTargets.fromRecord(record) ?: return false
        val client = clients.value.firstOrNull { it.id == payload.clientId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = DcUiState(
            target = target,
            client = client,
            lines = payload.lines,
            discountInput = payload.discount.toInputAmount(),
            deliveryInput = payload.delivery.toInputAmount(),
            note = payload.note.orEmpty(),
        )
        return true
    }

    fun selectClient(client: ClientEntity) {
        _uiState.value = _uiState.value.copy(client = client)
    }

    fun addCatalogProduct(product: ProductWithStock) {
        _uiState.value = _uiState.value.let { current ->
            val existing = current.lines.firstOrNull { it.productId == product.product.id }
            val lines = if (existing != null) {
                current.lines.map { if (it.id == existing.id) it.copy(quantity = it.quantity + 1.0) else it }
            } else {
                current.lines + SaleLine(
                    id = nextLineId++,
                    name = product.product.nom,
                    unitPrice = product.product.prixVente ?: 0.0,
                    quantity = 1.0,
                    productId = product.product.id,
                )
            }
            current.copy(lines = lines)
        }
    }

    fun addFreeLine(name: String, unitPrice: Double, quantity: Double): Boolean {
        if (name.isBlank() || unitPrice < 0.0 || quantity <= 0.0) return false
        _uiState.value = _uiState.value.copy(
            lines = _uiState.value.lines + SaleLine(
                id = nextLineId++,
                name = name.trim(),
                unitPrice = unitPrice,
                quantity = quantity,
            ),
        )
        return true
    }

    fun changeQuantity(lineId: Long, delta: Double) {
        _uiState.value = _uiState.value.copy(
            lines = _uiState.value.lines.mapNotNull { line ->
                if (line.id != lineId) line
                else line.copy(quantity = line.quantity + delta).takeIf { it.quantity > 0.0 }
            },
        )
    }

    fun removeLine(lineId: Long) {
        _uiState.value = _uiState.value.copy(lines = _uiState.value.lines.filterNot { it.id == lineId })
    }

    fun updateDiscount(value: String) {
        _uiState.value = _uiState.value.copy(discountInput = value.filterMoneyInput())
    }

    fun updateDelivery(value: String) {
        _uiState.value = _uiState.value.copy(deliveryInput = value.filterMoneyInput())
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value.take(500))
    }

    /** Enregistre la pièce commerciale (aucun effet stock/caisse). */
    fun save() {
        if (_busy.value) return
        val state = _uiState.value
        val client = state.client ?: run {
            _result.value = Result.ClientRequired
            return
        }
        if (state.lines.isEmpty()) {
            _result.value = Result.LinesRequired
            return
        }
        val totals = totals()
        if (totals.total <= 0.0) {
            _result.value = Result.InvalidAmount
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val payload = SaleRecordPayload(
                    clientId = client.id,
                    clientName = client.nom,
                    lines = state.lines,
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    delivery = totals.delivery,
                    taxRate = taxRate.value,
                    taxAmount = totals.taxAmount,
                    total = totals.total,
                    paymentMethod = "",
                    paidAmount = 0.0,
                    note = state.note.trim().ifBlank { null },
                )
                when (val r = saveDevisCommande(state.target, state.editingRecordId, payload)) {
                    is SaveDevisCommandeUseCase.Result.Succes -> {
                        _result.value = Result.Saved(r.reference, state.target.isDevis)
                        _uiState.value = DcUiState(target = state.target)
                    }
                    SaveDevisCommandeUseCase.Result.LectureSeule -> _result.value = Result.ReadOnly
                    SaveDevisCommandeUseCase.Result.DonneesInvalides -> _result.value = Result.InvalidAmount
                    SaveDevisCommandeUseCase.Result.ClientIntrouvable -> _result.value = Result.ClientMissing
                    SaveDevisCommandeUseCase.Result.PiecIntrouvable -> _result.value = Result.PiecIntrouvable
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun convertToOrder(record: OperationRecordEntity) {
        if (_busy.value) return
        val targetDevis = CommercialTargets.fromRecord(record) ?: run {
            _result.value = Result.PiecIntrouvable
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = convertDevis(record.id, targetDevis)) {
                    is ConvertDevisToOrderUseCase.Result.Succes -> _result.value = Result.OrderCreated(r.reference)
                    ConvertDevisToOrderUseCase.Result.LectureSeule -> _result.value = Result.ReadOnly
                    ConvertDevisToOrderUseCase.Result.Introuvable -> _result.value = Result.PiecIntrouvable
                    ConvertDevisToOrderUseCase.Result.DejaAnnulee -> _result.value = Result.AlreadyCancelled
                    ConvertDevisToOrderUseCase.Result.DonneesInvalides -> _result.value = Result.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun convertToInvoice(record: OperationRecordEntity, paymentMethod: String, paidAmount: Double) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = convertOrder(record.id, paymentMethod, paidAmount)) {
                    is ConvertOrderToSaleUseCase.Result.Succes -> _result.value = Result.Invoiced(r.reference)
                    ConvertOrderToSaleUseCase.Result.LectureSeule -> _result.value = Result.ReadOnly
                    ConvertOrderToSaleUseCase.Result.Introuvable -> _result.value = Result.PiecIntrouvable
                    ConvertOrderToSaleUseCase.Result.DejaAnnulee -> _result.value = Result.AlreadyCancelled
                    ConvertOrderToSaleUseCase.Result.DejaFacturee -> _result.value = Result.AlreadyInvoiced
                    ConvertOrderToSaleUseCase.Result.DonneesInvalides -> _result.value = Result.InvalidAmount
                    is ConvertOrderToSaleUseCase.Result.StockInsuffisant ->
                        _result.value = Result.StockInsuffisant(r.produitNom, r.disponible, r.demande)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelRecord(record: OperationRecordEntity) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                operations.setStatus(record.id, OperationStatus.CANCELLED)
                _result.value = Result.Cancelled
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}

private fun String.toMoneyOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() }

private fun String.filterMoneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }

private fun Double.toInputAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun Double.saleQtyText(): String =
    if (this % 1.0 == 0.0) toInt().toString() else DecimalFormat("0.##").format(this)

private fun Double.saleRateText(): String =
    DecimalFormat("0.##", DecimalFormatSymbols(Locale.getDefault())).format(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevisCommandeScreen(
    onBack: () -> Unit,
    family: DcFamily = DcFamily.VENTE,
    openCreate: Boolean = false,
    viewModel: DevisCommandeViewModel = hiltViewModel(),
) {
    val devis by viewModel.devis.collectAsState(initial = emptyList())
    val commandes by viewModel.commandes.collectAsState(initial = emptyList())
    val prestationsDevis by viewModel.prestationsDevis.collectAsState(initial = emptyList())
    val prestationsCommandes by viewModel.prestationsCommandes.collectAsState(initial = emptyList())
    val facturees by viewModel.facturees.collectAsState(initial = emptySet())
    val clients by viewModel.clients.collectAsState()
    val products by viewModel.products.collectAsState(initial = emptyList())
    val taxRate by viewModel.taxRate.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val ui by viewModel.uiState.collectAsState()
    val busy by viewModel.busy.collectAsState()

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableStateOf("DEVIS") }
    var formVisible by rememberSaveable(openCreate) { mutableStateOf(openCreate) }
    var clientSheetVisible by remember { mutableStateOf(false) }
    var clientSearch by remember { mutableStateOf("") }
    var productSearch by rememberSaveable { mutableStateOf("") }
    var freeName by rememberSaveable { mutableStateOf("") }
    var freePrice by rememberSaveable { mutableStateOf("") }
    var freeQty by rememberSaveable { mutableStateOf("1") }
    var detailRecord by remember { mutableStateOf<OperationRecordEntity?>(null) }
    var cancelVisible by remember { mutableStateOf(false) }
    var invoiceVisible by remember { mutableStateOf(false) }
    var invoicePayment by rememberSaveable { mutableStateOf("") }
    var invoicePaid by rememberSaveable { mutableStateOf("") }

    // --- Cibles selon la famille ---
    val targetDevis: CommercialTarget =
        if (family == DcFamily.VENTE) CommercialTarget.Devis else CommercialTarget.DevisPrestation
    val targetCommande: CommercialTarget =
        if (family == DcFamily.VENTE) CommercialTarget.Commande else CommercialTarget.CommandePrestation
    val targetParTab: (String) -> CommercialTarget = { if (it == "DEVIS") targetDevis else targetCommande }
    val liste = when (family) {
        DcFamily.VENTE -> if (tab == "DEVIS") devis else commandes
        DcFamily.PRESTATIONS -> if (tab == "DEVIS") prestationsDevis else prestationsCommandes
    }

    val fallbackPayment = stringResource(R.string.sales_cash)
    LaunchedEffect(paymentMethods) {
        if (invoicePayment.isBlank() && paymentMethods.isNotEmpty()) invoicePayment = paymentMethods.first()
    }
    LaunchedEffect(openCreate) {
        if (openCreate) {
            viewModel.startNew(targetParTab(tab))
            formVisible = true
        }
    }
    LaunchedEffect(viewModel.result) {
        when (val r = viewModel.result.value) {
            is DevisCommandeViewModel.Result.Saved -> {
                snackbar.showSnackbar(
                    context.getString(
                        when {
                            r.isDevis && family == DcFamily.PRESTATIONS -> R.string.prest_devis_saved
                            r.isDevis -> R.string.dc_devis_saved
                            family == DcFamily.PRESTATIONS -> R.string.prest_commande_saved
                            else -> R.string.dc_commande_saved
                        },
                        r.reference,
                    ),
                )
                formVisible = false
                viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.ClientRequired -> {
                snackbar.showSnackbar(context.getString(R.string.dc_client_required)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.LinesRequired -> {
                snackbar.showSnackbar(context.getString(R.string.dc_lines_required)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.InvalidAmount -> {
                snackbar.showSnackbar(context.getString(R.string.dc_invalid_amount)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.ClientMissing -> {
                snackbar.showSnackbar(context.getString(R.string.dc_client_missing)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only)); viewModel.clearResult()
            }
            is DevisCommandeViewModel.Result.OrderCreated -> {
                snackbar.showSnackbar(context.getString(R.string.dc_convert_order_success, r.reference))
                detailRecord = null
                viewModel.clearResult()
            }
            is DevisCommandeViewModel.Result.Invoiced -> {
                snackbar.showSnackbar(context.getString(R.string.dc_convert_sale_success, r.reference))
                invoiceVisible = false
                detailRecord = null
                viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.AlreadyInvoiced -> {
                snackbar.showSnackbar(context.getString(R.string.dc_already_invoiced)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.AlreadyCancelled -> {
                snackbar.showSnackbar(context.getString(R.string.dc_already_cancelled)); viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.PiecIntrouvable -> {
                snackbar.showSnackbar(context.getString(R.string.dc_piec_missing)); viewModel.clearResult()
            }
            is DevisCommandeViewModel.Result.StockInsuffisant -> {
                snackbar.showSnackbar(
                    context.getString(
                        R.string.sales_stock_insufficient,
                        r.nom,
                        r.dispo.saleQtyText(),
                        r.demande.saleQtyText(),
                    ),
                )
                viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.Cancelled -> {
                snackbar.showSnackbar(context.getString(R.string.dc_cancelled))
                detailRecord = null
                viewModel.clearResult()
            }
            DevisCommandeViewModel.Result.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error)); viewModel.clearResult()
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!formVisible) {
            // --- LISTE (onglets Devis / Commandes) ---
            Scaffold(
                containerColor = MissaCanvas,
                topBar = {
                    MissaTopAppBar(
                        title = stringResource(if (family == DcFamily.VENTE) R.string.dc_title else R.string.prest_title),
                        onBack = onBack,
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            viewModel.startNew(targetParTab(tab))
                            formVisible = true
                        },
                        containerColor = BrandBlue,
                        contentColor = Color.White,
                    ) { Icon(Icons.Outlined.Add, stringResource(R.string.dc_new)) }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tab == "DEVIS",
                                onClick = { tab = "DEVIS" },
                                label = {
                                    Text(
                                        stringResource(
                                            if (family == DcFamily.VENTE) R.string.dc_tab_devis else R.string.prest_tab_devis,
                                        ),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = tab == "COMMANDE",
                                onClick = { tab = "COMMANDE" },
                                label = {
                                    Text(
                                        stringResource(
                                            if (family == DcFamily.VENTE) R.string.dc_tab_commandes else R.string.prest_tab_commandes,
                                        ),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (liste.isEmpty()) {
                        item {
                            MissaEmptyState(
                                icon = Icons.Outlined.Description,
                                title = stringResource(
                                    when {
                                        tab == "DEVIS" && family == DcFamily.PRESTATIONS -> R.string.prest_devis_empty
                                        tab == "DEVIS" -> R.string.dc_devis_empty
                                        family == DcFamily.PRESTATIONS -> R.string.prest_commande_empty
                                        else -> R.string.dc_commande_empty
                                    },
                                ),
                            )
                        }
                    } else {
                        items(liste, key = { it.id }) { record ->
                            DcRecordRow(
                                record = record,
                                devise = devise,
                                estCommande = tab == "COMMANDE",
                                estFacturee = tab == "COMMANDE" && record.id in facturees,
                                onOpen = { detailRecord = record },
                            )
                        }
                    }
                }
            }
        } else {
            // --- FORMULAIRE (UNE PAGE) ---
            val totals = viewModel.totals()
            val visibleProducts = products.filter { product ->
                productSearch.isBlank() ||
                    product.product.nom.contains(productSearch, ignoreCase = true) ||
                    product.product.code.contains(productSearch, ignoreCase = true) ||
                    (product.product.reference?.contains(productSearch, ignoreCase = true) ?: false) ||
                    (product.product.barcode?.contains(productSearch, ignoreCase = true) ?: false)
            }
            Scaffold(
                containerColor = MissaCanvas,
                topBar = {
                    MissaTopAppBar(
                        title = stringResource(
                            when {
                                ui.editingRecordId != null && ui.target.isDevis && family == DcFamily.PRESTATIONS -> R.string.prest_edit_devis
                                ui.editingRecordId != null && ui.target.isDevis -> R.string.dc_edit_devis
                                ui.editingRecordId != null && family == DcFamily.PRESTATIONS -> R.string.prest_edit_commande
                                ui.editingRecordId != null -> R.string.dc_edit_commande
                                ui.target.isDevis && family == DcFamily.PRESTATIONS -> R.string.prest_new_devis
                                ui.target.isDevis -> R.string.dc_new_devis
                                family == DcFamily.PRESTATIONS -> R.string.prest_new_commande
                                else -> R.string.dc_new_commande
                            },
                        ),
                        onBack = { formVisible = false },
                    )
                },
                bottomBar = {
                    Surface(color = Color.White, shadowElevation = 6.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { formVisible = false },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(stringResource(R.string.ops_cancel)) }
                            Button(
                                onClick = viewModel::save,
                                enabled = ui.client != null && ui.lines.isNotEmpty() && !busy,
                                modifier = Modifier.weight(2f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Outlined.Save, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(stringResource(R.string.dc_save), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // --- CLIENT ---
                    DcCard(stringResource(R.string.dc_client)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { clientSheetVisible = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MissaSoftBlue,
                            border = BorderStroke(1.dp, MissaBorder),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Person, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    ui.client?.nom ?: stringResource(R.string.dc_select_client),
                                    color = if (ui.client != null) MissaInk else MissaMuted,
                                    fontWeight = if (ui.client != null) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("›", color = MissaMuted, fontSize = 20.sp)
                            }
                        }
                    }

                    // --- PRODUITS / PRESTATIONS ---
                    DcCard(stringResource(R.string.dc_products)) {
                        OutlinedTextField(
                            value = productSearch,
                            onValueChange = { productSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dc_search_products)) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        )
                        Spacer(Modifier.height(8.dp))
                        if (visibleProducts.isEmpty()) {
                            Text(
                                stringResource(
                                    if (products.isEmpty()) R.string.dc_catalog_empty else R.string.dc_no_results,
                                ),
                                color = MissaMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            )
                        } else {
                            visibleProducts.take(8).forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(7.dp), color = MissaSoftBlue) {
                                        Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.product.nom, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            stringResource(R.string.purchase_stock_available, product.stock.saleQtyText()),
                                            color = if (product.stock <= 0.0) Red40 else MissaMuted,
                                            fontSize = 9.sp,
                                        )
                                    }
                                    product.product.prixVente?.let {
                                        Text(saleMoney(it, devise), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.addCatalogProduct(product) },
                                        modifier = Modifier.size(30.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        contentPadding = PaddingValues(0.dp),
                                    ) { Icon(Icons.Outlined.Add, stringResource(R.string.purchase_add_product), tint = Color.White, modifier = Modifier.size(18.dp)) }
                                }
                                HorizontalDivider(color = MissaBorder)
                            }
                        }

                        // --- LIGNE LIBRE (ou prestation libre) ---
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = freeName,
                                onValueChange = { freeName = it },
                                modifier = Modifier.weight(1.4f),
                                label = { Text(stringResource(R.string.dc_free_name)) },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = freePrice,
                                onValueChange = { freePrice = it.filterMoneyInput() },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.dc_free_price)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            OutlinedTextField(
                                value = freeQty,
                                onValueChange = { freeQty = it.filterMoneyInput() },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.dc_free_qty)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            Button(
                                onClick = {
                                    if (viewModel.addFreeLine(freeName, freePrice.toMoneyOrNull() ?: 0.0, freeQty.toMoneyOrNull() ?: 0.0)) {
                                        freeName = ""
                                        freePrice = ""
                                        freeQty = "1"
                                    }
                                },
                                modifier = Modifier.height(48.dp).width(44.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            ) { Icon(Icons.Outlined.Add, stringResource(R.string.purchase_add_product), tint = Color.White) }
                        }

                        // --- PANIER ---
                        if (ui.lines.isEmpty()) {
                            Text(
                                stringResource(R.string.dc_cart_empty),
                                color = MissaMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        } else {
                            ui.lines.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.name, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            stringResource(R.string.purchase_line_price, saleMoney(line.unitPrice, devise)),
                                            color = MissaMuted,
                                            fontSize = 10.sp,
                                        )
                                    }
                                    TextButton(onClick = { viewModel.changeQuantity(line.id, -1.0) }, contentPadding = PaddingValues(0.dp)) { Text("−", color = BrandBlue, fontSize = 15.sp) }
                                    Text(line.quantity.saleQtyText(), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, color = MissaInk, fontSize = 12.sp)
                                    TextButton(onClick = { viewModel.changeQuantity(line.id, 1.0) }, contentPadding = PaddingValues(0.dp)) { Text("+", color = BrandBlue, fontSize = 15.sp) }
                                    Text(saleMoney(line.total, devise), modifier = Modifier.width(84.dp), textAlign = TextAlign.End, color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    IconButton(onClick = { viewModel.removeLine(line.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.purchase_remove_line), tint = Red40, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- REMISE / LIVRAISON / TOTALS ---
                    DcCard(stringResource(R.string.dc_totals)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ui.discountInput,
                                onValueChange = viewModel::updateDiscount,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.dc_discount)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            OutlinedTextField(
                                value = ui.deliveryInput,
                                onValueChange = viewModel::updateDelivery,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.dc_delivery)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }
                        DcAmountLine(stringResource(R.string.dc_subtotal), saleMoney(totals.subtotal, devise))
                        if (totals.discount > 0.0) DcAmountLine(stringResource(R.string.dc_discount), "− " + saleMoney(totals.discount, devise))
                        if (totals.delivery > 0.0) DcAmountLine(stringResource(R.string.dc_delivery), saleMoney(totals.delivery, devise))
                        if (taxRate > 0.0) DcAmountLine(stringResource(R.string.dc_tax, taxRate.saleRateText()), saleMoney(totals.taxAmount, devise))
                        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(vertical = 6.dp))
                        DcAmountLine(stringResource(R.string.dc_total), saleMoney(totals.total, devise), strong = true)
                        OutlinedTextField(
                            value = ui.note,
                            onValueChange = viewModel::updateNote,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dc_note)) },
                            minLines = 1,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // --- SÉLECTEUR CLIENT (recherche, spec §40) ---
        if (clientSheetVisible) {
            ClientPickerSheet(
                clients = clients,
                search = clientSearch,
                onSearchChange = { clientSearch = it },
                onDismiss = { clientSheetVisible = false },
                onSelect = {
                    viewModel.selectClient(it)
                    clientSheetVisible = false
                    clientSearch = ""
                },
            )
        }

        // --- DÉTAIL D'UNE PIÈCE (conversions) ---
        detailRecord?.let { record ->
            val payload = SaleRecordCodec.decode(record.notes)
            val target = CommercialTargets.fromRecord(record)
            val estCommande = target?.isDevis == false
            val isCancelled = record.status == OperationStatus.CANCELLED.name
            val estFacturee = estCommande && record.id in facturees
            ModalBottomSheet(onDismissRequest = { detailRecord = null }, containerColor = Color.White) {
                Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(record.reference, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${payload?.clientName.orEmpty()} · ${DateUtils.formatDateHeure(record.createdAt)}",
                                color = MissaMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        payload?.let { Text(saleMoney(it.total, devise), color = BrandBlue, fontWeight = FontWeight.Bold) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isCancelled) DcBadge(stringResource(R.string.ops_status_cancelled), Red40)
                        if (estFacturee) DcBadge(stringResource(R.string.dc_invoiced), Green60)
                        if (estCommande && !estFacturee && !isCancelled) DcBadge(stringResource(R.string.dc_to_invoice), BrandBlue)
                    }
                    HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = {
                            if (!isCancelled) {
                                if (estCommande) invoiceVisible = true
                                else viewModel.convertToOrder(record)
                            }
                        },
                        enabled = !isCancelled && !busy && !(estCommande && estFacturee),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    ) {
                        Icon(
                            if (estCommande) Icons.Outlined.Save else Icons.Outlined.Description,
                            null,
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(if (estCommande) R.string.dc_convert_sale else R.string.dc_convert_order),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = { if (viewModel.duplicate(record)) { detailRecord = null; formVisible = true } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) { Text(stringResource(R.string.dc_duplicate)) }
                    if (!isCancelled) {
                        OutlinedButton(
                            onClick = { cancelVisible = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(contentColor = Red40),
                        ) { Text(stringResource(R.string.dc_cancel_piece)) }
                    }
                }
            }
        }

        // --- CONVERSION COMMANDE → FACTURE ---
        if (invoiceVisible && detailRecord != null) {
            val record = detailRecord!!
            val totalCommande = SaleRecordCodec.decode(record.notes)?.total ?: 0.0
            val paidDefault = if (invoicePaid.isBlank()) totalCommande.toInputAmount() else invoicePaid
            AlertDialog(
                onDismissRequest = { invoiceVisible = false },
                title = { Text(stringResource(R.string.dc_convert_sale)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.dc_payment_method), color = MissaMuted, style = MaterialTheme.typography.bodySmall)
                        paymentMethods.ifEmpty { listOf(fallbackPayment) }.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { invoicePayment = method }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(method, color = MissaInk, modifier = Modifier.weight(1f))
                                if (method == invoicePayment) Icon(Icons.Outlined.Add, null, tint = BrandBlue)
                            }
                        }
                        OutlinedTextField(
                            value = paidDefault,
                            onValueChange = { invoicePaid = it.filterMoneyInput() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dc_paid_amount)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.convertToInvoice(record, invoicePayment, paidDefault.toMoneyOrNull() ?: totalCommande)
                        },
                        enabled = !busy,
                    ) { Text(stringResource(R.string.dc_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { invoiceVisible = false }) { Text(stringResource(R.string.ops_cancel)) }
                },
            )
        }

        // --- CONFIRMATION D'ANNULATION ---
        if (cancelVisible && detailRecord != null) {
            val record = detailRecord!!
            AlertDialog(
                onDismissRequest = { cancelVisible = false },
                title = { Text(stringResource(R.string.dc_cancel_piece)) },
                text = { Text(stringResource(R.string.dc_cancel_confirm, record.reference)) },
                confirmButton = {
                    Button(onClick = { cancelVisible = false; viewModel.cancelRecord(record) }) {
                        Text(stringResource(R.string.dc_cancel_piece), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cancelVisible = false }) { Text(stringResource(R.string.ops_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun DcCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = BrandBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun DcAmountLine(label: String, value: String, strong: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (strong) MissaInk else MissaMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = BrandBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun DcBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun DcRecordRow(
    record: OperationRecordEntity,
    devise: String,
    estCommande: Boolean,
    estFacturee: Boolean,
    onOpen: () -> Unit,
) {
    val payload = SaleRecordCodec.decode(record.notes)
    val isCancelled = record.status == OperationStatus.CANCELLED.name
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.reference, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(
                        "${payload?.clientName.orEmpty()} · ${DateUtils.formatDateHeure(record.createdAt)}",
                        color = MissaMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                payload?.let { Text(saleMoney(it.total, devise), color = BrandBlue, fontWeight = FontWeight.Bold) }
            }
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCancelled) DcBadge(stringResource(R.string.ops_status_cancelled), Red40)
                if (estCommande && estFacturee) DcBadge(stringResource(R.string.dc_invoiced), Green60)
                if (estCommande && !estFacturee && !isCancelled) DcBadge(stringResource(R.string.dc_to_invoice), BrandBlue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPickerSheet(
    clients: List<ClientEntity>,
    search: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (ClientEntity) -> Unit,
) {
    val visible = clients.filter {
        search.isBlank() || it.nom.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(stringResource(R.string.dc_select_client), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text(stringResource(R.string.dc_client_empty), color = MissaMuted, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(visible, key = { it.id }) { client ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(client) }.padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.nom, color = MissaInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(client.code, color = MissaMuted, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = MissaBorder)
                    }
                }
            }
        }
    }
}
