package com.missa.b360.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ReturnRules
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.ReturnSaleUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** Ligne retournable : agrégation par clé (produit ou libellé) de la facture d'origine. */
data class ReturnLineItem(
    val key: String,
    val name: String,
    val productId: Long?,
    val unitPrice: Double,
    val vendu: Double,
    val restant: Double,
    val isCatalog: Boolean,
)

@HiltViewModel
class ReturnSaleViewModel @Inject constructor(
    operations: OperationUseCases,
    getEnterprise: GetEnterpriseUseCase,
    private val returnSale: ReturnSaleUseCase,
) : ViewModel() {

    sealed interface Result {
        data class Succes(val reference: String) : Result
        data object LectureSeule : Result
        data object Introuvable : Result
        data object DejaAnnulee : Result
        data object Brouillon : Result
        data object LignesInvalides : Result
        data object Error : Result
    }

    /** Ventes validées avec détail de facture — candidates au retour. */
    val returnableSales: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.VENTE)
        .map { records ->
            records.filter {
                it.status == OperationStatus.VALIDATED.name &&
                    SaleRecordCodec.decode(it.notes)?.let { p -> p.sourceRecordId == null && p.total > 0.0 } == true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    private val _vente = MutableStateFlow<OperationRecordEntity?>(null)
    val vente: StateFlow<OperationRecordEntity?> = _vente
    private val _original = MutableStateFlow<SaleRecordPayload?>(null)
    val original: StateFlow<SaleRecordPayload?> = _original
    private val _items = MutableStateFlow<List<ReturnLineItem>>(emptyList())
    val items: StateFlow<List<ReturnLineItem>> = _items
    private val _avoirsCount = MutableStateFlow(0)
    val avoirsCount: StateFlow<Int> = _avoirsCount

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    val retourStock: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var motif by mutableStateOf("QUALITE")
    private val returnedQty = mutableStateMapOf<String, String>()

    /** Lecture UI de la quantité retournée saisie pour une ligne. */
    fun returnedQtyView(key: String): String = returnedQty[key].orEmpty()

    fun load(vente: OperationRecordEntity) {
        viewModelScope.launch {
            val original = SaleRecordCodec.decode(vente.notes) ?: return@launch
            val avoirs = returnSale.avoirsPrecedents(vente.id)
            val restant = ReturnRules.restantParLigne(original, avoirs)
            val lines = original.lines
                .groupBy { ReturnRules.lineKey(it) }
                .map { (key, group) ->
                    ReturnLineItem(
                        key = key,
                        name = group.first().name,
                        productId = group.first().productId,
                        unitPrice = group.first().unitPrice,
                        vendu = group.sumOf { it.quantity },
                        restant = restant[key] ?: 0.0,
                        isCatalog = group.first().productId != null,
                    )
                }
                .filter { it.vendu > 0.0 }
            _vente.value = vente
            _original.value = original
            _items.value = lines
            _avoirsCount.value = avoirs.size
            returnedQty.clear()
            motif = "QUALITE"
            retourStock.value = true
        }
    }

    fun setReturned(key: String, text: String) {
        val item = _items.value.firstOrNull { it.key == key } ?: return
        val cleaned = text.filter { it.isDigit() || it == ',' || it == '.' }
        val value = cleaned.replace(',', '.').toDoubleOrNull()
        if (value != null && value > item.restant) {
            // On plafonne à la quantité encore retournable : jamais au-dessus (spec §22).
            returnedQty[key] = item.restant.saleQtyText()
        } else {
            returnedQty[key] = cleaned
        }
    }

    fun soldeAvoir(): Double =
        _items.value.sumOf { item ->
            val qty = (returnedQty[item.key] ?: "").replace(',', '.').toDoubleOrNull() ?: 0.0
            item.unitPrice * qty
        }

    fun save() {
        if (_busy.value) return
        val vente = _vente.value ?: return
        val lines = _items.value.mapNotNull { item ->
            val qty = (returnedQty[item.key] ?: "").replace(',', '.').toDoubleOrNull() ?: 0.0
            if (qty > 0.0) {
                SaleLine(
                    id = -1L,
                    name = item.name,
                    unitPrice = item.unitPrice,
                    quantity = qty,
                    productId = item.productId,
                )
            } else {
                null
            }
        }
        if (lines.isEmpty()) {
            _result.value = Result.LignesInvalides
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = returnSale(
                    saleRecordId = vente.id,
                    returnedLines = lines,
                    motif = motif,
                    retourStock = retourStock.value,
                )) {
                    is ReturnSaleUseCase.Result.Succes -> _result.value = Result.Succes(r.reference)
                    ReturnSaleUseCase.Result.LectureSeule -> _result.value = Result.LectureSeule
                    ReturnSaleUseCase.Result.Introuvable -> _result.value = Result.Introuvable
                    ReturnSaleUseCase.Result.DejaAnnulee -> _result.value = Result.DejaAnnulee
                    ReturnSaleUseCase.Result.Brouillon -> _result.value = Result.Brouillon
                    ReturnSaleUseCase.Result.LignesInvalides -> _result.value = Result.LignesInvalides
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

    fun clearResult() {
        _result.value = null
    }
}

/**
 * Retour de vente (spec §22) — facture d'origine, quantités retournables, motif,
 * avoir total, et option de retour en stock. Jamais de quantité au-dessus du restant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnSaleScreen(
    onBack: () -> Unit,
    recordId: Long? = null,
    viewModel: ReturnSaleViewModel = hiltViewModel(),
) {
    val sales by viewModel.returnableSales.collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    val vente by viewModel.vente.collectAsState(initial = null)
    val items by viewModel.items.collectAsState(initial = emptyList())
    val avoirsCount by viewModel.avoirsCount.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val result by viewModel.result.collectAsState()
    var retourStock by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // Réactif au chargement de la liste : l'ouverture directe (recordId > 0) passe
    // au formulaire dès que la facture est visible dans les ventes retournables.
    LaunchedEffect(sales, recordId) {
        if (vente == null) {
            recordId?.takeIf { it > 0L }?.let { id ->
                sales.firstOrNull { it.id == id }?.let { viewModel.load(it) }
            }
        }
    }
    // Synchronise le switch UI avec l'état du VM (reset à chaque facture chargée).
    LaunchedEffect(vente) {
        retourStock = viewModel.retourStock.value
    }
    LaunchedEffect(result) {
        when (val current = result) {
            is ReturnSaleViewModel.Result.Succes -> {
                snackbar.showSnackbar(context.getString(R.string.return_success, current.reference))
                viewModel.clearResult()
                onBack()
            }
            ReturnSaleViewModel.Result.LectureSeule -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only)); viewModel.clearResult()
            }
            ReturnSaleViewModel.Result.Introuvable -> {
                snackbar.showSnackbar(context.getString(R.string.return_not_found)); viewModel.clearResult()
            }
            ReturnSaleViewModel.Result.DejaAnnulee -> {
                snackbar.showSnackbar(context.getString(R.string.return_already_cancelled)); viewModel.clearResult()
            }
            ReturnSaleViewModel.Result.Brouillon -> {
                snackbar.showSnackbar(context.getString(R.string.return_draft_unavailable)); viewModel.clearResult()
            }
            ReturnSaleViewModel.Result.LignesInvalides -> {
                snackbar.showSnackbar(context.getString(R.string.return_lines_invalid)); viewModel.clearResult()
            }
            ReturnSaleViewModel.Result.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error)); viewModel.clearResult()
            }
            null -> Unit
        }
    }

    if (vente == null) {
        // Mode liste : factures de vente candidates au retour.
        Scaffold(
            containerColor = MissaCanvas,
            topBar = { MissaTopAppBar(title = stringResource(R.string.return_title_list), onBack = onBack) },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (sales.isEmpty()) {
                    item {
                        MissaEmptyState(
                            icon = Icons.Outlined.ReceiptLong,
                            title = stringResource(R.string.return_list_empty),
                        )
                    }
                } else {
                    items(sales, key = { it.id }) { record ->
                        ReturnSaleRow(record = record, devise = devise, onOpen = { viewModel.load(record) })
                    }
                }
            }
        }
    } else {
        ReturnFormContent(
            vente = vente,
            items = items,
            avoirsCount = avoirsCount,
            busy = busy,
            devise = devise,
            retourStock = retourStock,
            onRetourStockChange = { retourStock = it; viewModel.retourStock.value = it },
            motif = viewModel.motif,
            onMotifChange = { viewModel.motif = it },
            onBack = onBack,
            onConfirm = viewModel::save,
        )
    }
}

@Composable
private fun ReturnSaleRow(record: OperationRecordEntity, devise: String, onOpen: () -> Unit) {
    val payload = SaleRecordCodec.decode(record.notes) ?: return
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
                        "${payload.clientName} · ${DateUtils.formatDateHeure(record.createdAt)}",
                        color = MissaMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(saleMoney(payload.total, devise), color = BrandBlue, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    stringResource(R.string.return_paid, saleMoney(payload.paidAmount, devise)),
                    color = MissaMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.return_balance, saleMoney((payload.total - payload.paidAmount).coerceAtLeast(0.0), devise)),
                    color = if (payload.total - payload.paidAmount > 0.0) Red40 else Green60,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnFormContent(
    vente: OperationRecordEntity,
    items: List<ReturnLineItem>,
    avoirsCount: Int,
    busy: Boolean,
    devise: String,
    retourStock: Boolean,
    onRetourStockChange: (Boolean) -> Unit,
    motif: String,
    onMotifChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<ReturnSaleViewModel>()
    val original = SaleRecordCodec.decode(vente.notes)
    val soldeAvoir = viewModel.soldeAvoir()
    val hasSelection = soldeAvoir > 0.0
    val snackbar = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MissaCanvas,
            topBar = { MissaTopAppBar(title = stringResource(R.string.return_title_form, vente.reference), onBack = onBack) },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(stringResource(R.string.ops_cancel)) }
                        Button(
                            onClick = onConfirm,
                            enabled = hasSelection && !busy,
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
                            Text(stringResource(R.string.return_confirm), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // --- FACTURE D'ORIGINE ---
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MissaBorder)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        original?.let { p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(vente.reference, fontWeight = FontWeight.Bold, color = MissaInk)
                                    Text(
                                        "${p.clientName} · ${DateUtils.formatDateHeure(vente.createdAt)}",
                                        color = MissaMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(saleMoney(p.total, devise), color = BrandBlue, fontWeight = FontWeight.Bold)
                            }
                            if (avoirsCount > 0) {
                                Text(stringResource(R.string.return_previous_avoirs, avoirsCount), color = MissaMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // --- LIGNES RETOURNABLES ---
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MissaBorder)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.return_lines), color = BrandBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        items.forEach { item ->
                            ReturnLineRow(
                                item = item,
                                onQuantity = { viewModel.setReturned(item.key, it) },
                                current = viewModel.returnedQtyView(item.key),
                            )
                        }
                    }
                }

                // --- MOTIF ---
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MissaBorder)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.return_motif), color = BrandBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "QUALITE" to R.string.return_motif_qualite,
                                "CASSE" to R.string.return_motif_casse,
                                "ERREUR" to R.string.return_motif_erreur,
                                "RETRAIT" to R.string.return_motif_retrait,
                                "AUTRE" to R.string.return_motif_autre,
                            ).forEach { (key, res) ->
                                FilterChip(
                                    selected = motif == key,
                                    onClick = { onMotifChange(key) },
                                    label = { Text(stringResource(res), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.return_to_stock), modifier = Modifier.weight(1f), color = MissaInk)
                            Switch(checked = retourStock, onCheckedChange = onRetourStockChange)
                        }
                        Text(
                            stringResource(R.string.return_to_stock_hint),
                            color = MissaMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // --- TOTAL AVOIR ---
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MissaSoftBlue)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.return_total_avoir), color = MissaInk, fontWeight = FontWeight.SemiBold)
                        Text(saleMoney(soldeAvoir, devise), color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReturnLineRow(
    item: ReturnLineItem,
    current: String,
    onQuantity: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = MissaInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.return_line_remaining,
                    item.vendu.saleQtyText(),
                    item.restant.saleQtyText(),
                ),
                color = if (item.restant <= 0.0) Red40 else MissaMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedTextField(
            value = current,
            onValueChange = onQuantity,
            modifier = Modifier.width(84.dp),
            label = { Text(stringResource(R.string.return_qty_label), textAlign = TextAlign.Center) },
            placeholder = { Text("0", textAlign = TextAlign.Center) },
            singleLine = true,
            enabled = item.restant > 0.0,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

private fun Double.saleQtyText(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)
