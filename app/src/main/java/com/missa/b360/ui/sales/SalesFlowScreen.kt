package com.missa.b360.ui.sales

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Rollback
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleTotals
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.theme.Blue40
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes

private enum class SalesStep { LIST, CLIENT, PRODUCTS, CART, PAYMENT, SUMMARY, SUCCESS, INVOICE, OPTIONS, PRINT }

private val FlowBlue = BrandBlue
private val FlowBlueDark = Blue40
private val FlowBlueSoft = MissaSoftBlue
private val FlowGreen = Green60
private val FlowGreenSoft = Green90
private val FlowInk = MissaInk
private val FlowMuted = MissaMuted
private val FlowBorder = MissaBorder
private val FlowBackground = MissaCanvas
private val FlowRed = Red40

/**
 * Parcours de vente en dix écrans : liste, client, produits, panier, paiement, résumé,
 * succès, aperçu, options et impression. Les contenus affichés proviennent uniquement de
 * la base locale ; aucun article ou client de démonstration n'est injecté.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onNavigate: (String) -> Unit,
    onOpenClientCreate: () -> Unit,
    openCreate: Boolean = false,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val sale by viewModel.uiState.collectAsState()
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val products by viewModel.products.collectAsState(initial = emptyList())
    val taxRate by viewModel.taxRate.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val saving by viewModel.saving.collectAsState()
    val cancelling by viewModel.cancelling.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var stepName by rememberSaveable(openCreate) {
        mutableStateOf(if (openCreate) SalesStep.CLIENT.name else SalesStep.LIST.name)
    }
    var activeReceipt by remember { mutableStateOf<SaleReceipt?>(null) }
    var customerPickerVisible by remember { mutableStateOf(false) }
    var freeProductVisible by remember { mutableStateOf(false) }
    var paymentPickerVisible by remember { mutableStateOf(false) }
    var cancelVisible by remember { mutableStateOf(false) }
    var editLine by remember { mutableStateOf<com.missa.b360.core.domain.model.SaleLine?>(null) }
    var productSearch by rememberSaveable { mutableStateOf("") }
    var paymentMethod by rememberSaveable { mutableStateOf("") }
    var scannerUnavailable by remember { mutableStateOf(false) }
    val currentStep = runCatching { SalesStep.valueOf(stepName) }.getOrDefault(SalesStep.LIST)
    val fallbackPayment = stringResource(R.string.sales_cash)
    val selectedPayment = paymentMethod.ifBlank { paymentMethods.firstOrNull() ?: fallbackPayment }
    val totals = sale.totals(taxRate)

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = result.data?.getStringExtra("SCAN_RESULT").orEmpty()
        if (scanned.isNotBlank()) productSearch = scanned
    }
    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val receipt = activeReceipt
        if (uri != null && receipt != null) {
            context.writeInvoicePdf(uri, receipt, devise)
        }
    }

    LaunchedEffect(paymentMethods) {
        if (paymentMethod.isBlank() && paymentMethods.isNotEmpty()) paymentMethod = paymentMethods.first()
    }
    LaunchedEffect(scannerUnavailable) {
        if (scannerUnavailable) {
            snackbar.showSnackbar(context.getString(R.string.sales_scanner_unavailable))
            scannerUnavailable = false
        }
    }
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is SalesViewModel.SaveResult.Saved -> {
                activeReceipt = result.receipt
                if (result.shouldPrint) {
                    stepName = SalesStep.SUCCESS.name
                } else {
                    snackbar.showSnackbar(context.getString(R.string.sales_draft_saved, result.receipt.reference))
                    stepName = SalesStep.LIST.name
                }
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.MissingClient -> {
                snackbar.showSnackbar(context.getString(R.string.sales_customer_required))
                stepName = SalesStep.CLIENT.name
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.EmptyCart -> {
                snackbar.showSnackbar(context.getString(R.string.sales_cart_required))
                stepName = SalesStep.PRODUCTS.name
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.InvalidAmount -> {
                snackbar.showSnackbar(context.getString(R.string.sales_invalid_amount))
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                viewModel.clearSaveResult()
            }
            is SalesViewModel.SaveResult.StockInsuffisant -> {
                snackbar.showSnackbar(
                    context.getString(
                        R.string.sales_stock_insufficient,
                        result.produitNom,
                        result.disponible.saleQty(),
                        result.demande.saleQty(),
                    ),
                )
                stepName = SalesStep.PRODUCTS.name
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.Cancelled -> {
                snackbar.showSnackbar(context.getString(R.string.sales_cancelled))
                activeReceipt = null
                stepName = SalesStep.LIST.name
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error))
                viewModel.clearSaveResult()
            }
            null -> Unit
        }
    }
    LaunchedEffect(currentStep, activeReceipt) {
        if (activeReceipt == null && currentStep in setOf(
                SalesStep.SUCCESS, SalesStep.INVOICE, SalesStep.OPTIONS, SalesStep.PRINT,
            )
        ) {
            stepName = SalesStep.LIST.name
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
        SalesStep.LIST -> SalesListScreen(
            records = history,
            devise = devise,
            onNewSale = {
                viewModel.clearCart()
                activeReceipt = null
                stepName = SalesStep.CLIENT.name
            },
            onOpenRecord = { record ->
                val payload = SaleRecordCodec.decode(record.notes)
                if (record.status == OperationStatus.DRAFT.name && payload != null &&
                    viewModel.loadDraft(record, clients)
                ) {
                    activeReceipt = SaleReceipt(record.id, record.reference, payload, record.createdAt)
                    stepName = SalesStep.CLIENT.name
                } else if (payload != null) {
                    activeReceipt = SaleReceipt(record.id, record.reference, payload, record.createdAt)
                    stepName = SalesStep.INVOICE.name
                }
            },
            onNavigate = onNavigate,
        )
        SalesStep.CLIENT -> FlowScaffold(
            title = stringResource(R.string.sales_new_sale),
            step = 0,
            onBack = { stepName = if (sale.editingRecordId == null) SalesStep.LIST.name else SalesStep.CART.name },
            bottomAction = FlowAction(stringResource(R.string.sales_next), sale.selectedClient != null) {
                stepName = SalesStep.PRODUCTS.name
            },
        ) { padding ->
            ClientStepContent(
                client = sale.selectedClient,
                onSelect = { customerPickerVisible = true },
                onNew = onOpenClientCreate,
                modifier = Modifier.padding(padding),
            )
        }
        SalesStep.PRODUCTS -> FlowScaffold(
            title = stringResource(R.string.sales_new_sale),
            step = 1,
            onBack = { stepName = SalesStep.CLIENT.name },
            bottomAction = FlowAction(
                stringResource(R.string.sales_view_cart, sale.lines.size),
                sale.lines.isNotEmpty(),
            ) { stepName = SalesStep.CART.name },
        ) { padding ->
            ProductsStepContent(
                client = sale.selectedClient,
                lines = sale.lines,
                products = products,
                query = productSearch,
                onQueryChange = { productSearch = it },
                onAddFree = { freeProductVisible = true },
                onScan = {
                    val intent = Intent("com.google.zxing.client.android.SCAN")
                    if (intent.resolveActivity(context.packageManager) != null) scannerLauncher.launch(intent) else scannerUnavailable = true
                },
                onAddCatalog = { viewModel.addCatalogProduct(it) },
                devise = devise,
                modifier = Modifier.padding(padding),
            )
        }
        SalesStep.CART -> FlowScaffold(
            title = stringResource(R.string.sales_cart),
            step = 1,
            onBack = { stepName = SalesStep.PRODUCTS.name },
            bottomAction = FlowAction(stringResource(R.string.sales_next), sale.lines.isNotEmpty()) {
                stepName = SalesStep.PAYMENT.name
            },
        ) { padding ->
            CartStepContent(
                lines = sale.lines,
                products = products,
                totals = totals,
                taxRate = taxRate,
                devise = devise,
                note = sale.note,
                discount = sale.discountInput,
                delivery = sale.deliveryInput,
                onNoteChange = viewModel::updateNote,
                onDiscountChange = viewModel::updateDiscount,
                onDeliveryChange = viewModel::updateDelivery,
                onQuantityChange = viewModel::changeQuantity,
                onEditLine = { editLine = it },
                onRemove = viewModel::removeLine,
                modifier = Modifier.padding(padding),
            )
        }
        SalesStep.PAYMENT -> FlowScaffold(
            title = stringResource(R.string.sales_payment),
            step = 2,
            onBack = { stepName = SalesStep.CART.name },
            bottomAction = FlowAction(stringResource(R.string.sales_next), sale.lines.isNotEmpty()) {
                stepName = SalesStep.SUMMARY.name
            },
        ) { padding ->
            PaymentStepContent(
                client = sale.selectedClient,
                totals = totals,
                paymentMethod = selectedPayment,
                paidInput = sale.paidInput,
                devise = devise,
                onChoosePayment = { paymentPickerVisible = true },
                onPaidChange = viewModel::updatePaid,
                modifier = Modifier.padding(padding),
            )
        }
        SalesStep.SUMMARY -> FlowScaffold(
            title = stringResource(R.string.sales_summary),
            step = 3,
            onBack = { stepName = SalesStep.PAYMENT.name },
            bottomAction = FlowAction(
                stringResource(R.string.sales_save_sale),
                sale.selectedClient != null && sale.lines.isNotEmpty() && !saving,
            ) {
                viewModel.save(selectedPayment, draft = false)
            },
        ) { padding ->
            SummaryStepContent(
                client = sale.selectedClient,
                lines = sale.lines,
                totals = totals,
                paymentMethod = selectedPayment,
                paidInput = sale.paidInput,
                devise = devise,
                busy = saving,
                onSaveDraft = { viewModel.save(selectedPayment, draft = true) },
                modifier = Modifier.padding(padding),
            )
        }
        SalesStep.SUCCESS -> activeReceipt?.let { receipt ->
            SuccessStepScreen(
                receipt = receipt,
                devise = devise,
                onViewInvoice = { stepName = SalesStep.INVOICE.name },
                onNewSale = {
                    viewModel.clearCart()
                    activeReceipt = null
                    stepName = SalesStep.CLIENT.name
                },
                onBackList = { stepName = SalesStep.LIST.name },
            )
        } ?: Unit
        SalesStep.INVOICE -> activeReceipt?.let { receipt ->
            InvoicePreviewScreen(
                receipt = receipt,
                devise = devise,
                onBack = { stepName = SalesStep.LIST.name },
                onShare = { context.shareInvoice(receipt, devise) },
                onOptions = { stepName = SalesStep.OPTIONS.name },
            )
        } ?: Unit
        SalesStep.OPTIONS -> activeReceipt?.let { receipt ->
            InvoiceOptionsScreen(
                receipt = receipt,
                devise = devise,
                onBack = { stepName = SalesStep.INVOICE.name },
                onPrint = { stepName = SalesStep.PRINT.name },
                onShare = { context.shareInvoice(receipt, devise) },
                onDownload = { createPdf.launch("${receipt.reference}.pdf") },
                onEmail = { context.emailInvoice(receipt, devise) },
                onView = { stepName = SalesStep.INVOICE.name },
                onDuplicate = {
                    if (viewModel.duplicate(receipt.payload, clients)) stepName = SalesStep.CLIENT.name
                },
                onReturn = { onNavigate("${Routes.SALES_RETURN}?recordId=${receipt.recordId}") },
                onCancel = { cancelVisible = true },
            )
        } ?: Unit
        SalesStep.PRINT -> activeReceipt?.let { receipt ->
            PrintScreen(
                receipt = receipt,
                devise = devise,
                onBack = { stepName = SalesStep.OPTIONS.name },
                onPrint = { context.printSaleReceipt(receipt, devise) },
            )
        } ?: Unit
    }

    if (customerPickerVisible) {
        FlowCustomerPicker(
            clients = clients,
            selectedId = sale.selectedClient?.id,
            onSelect = {
                viewModel.selectClient(it)
                customerPickerVisible = false
            },
            onNew = {
                customerPickerVisible = false
                onOpenClientCreate()
            },
            onDismiss = { customerPickerVisible = false },
        )
    }
    if (freeProductVisible) {
        FlowFreeProductDialog(
            onDismiss = { freeProductVisible = false },
            onAdd = { name, price, quantity ->
                if (viewModel.addFreeProduct(name, price, quantity)) {
                    productSearch = ""
                    freeProductVisible = false
                }
            },
        )
    }
    if (paymentPickerVisible) {
        FlowPaymentPicker(
            selected = selectedPayment,
            methods = paymentMethods.ifEmpty { listOf(fallbackPayment) },
            onSelect = {
                paymentMethod = it
                paymentPickerVisible = false
            },
            onDismiss = { paymentPickerVisible = false },
        )
    }
    if (cancelVisible && activeReceipt != null) {
        AlertDialog(
            onDismissRequest = { cancelVisible = false },
            title = { Text(stringResource(R.string.sales_cancel_sale)) },
            text = { Text(stringResource(R.string.sales_cancel_sale_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(activeReceipt!!.recordId)
                        cancelVisible = false
                    },
                    enabled = !cancelling,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                ) {
                    if (cancelling) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Text(stringResource(R.string.sales_cancel_sale))
                }
            },
            dismissButton = { TextButton(onClick = { cancelVisible = false }) { Text(stringResource(R.string.ops_cancel)) } },
        )
    }
    editLine?.let { line ->
        LineEditDialog(
            line = line,
            stock = products.firstOrNull { it.product.id == line.productId }?.stock,
            onDismiss = { editLine = null },
            onConfirm = { quantity, price ->
                viewModel.updateLine(line.id, quantity, price)
                editLine = null
            },
        )
    }
    SnackbarHost(
        hostState = snackbar,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
    )
    }
}

@Composable
private fun SalesPageTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MissaBrandMark(size = 22.dp)
        Spacer(Modifier.width(7.dp))
        Text(title, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class FlowAction(val label: String, val enabled: Boolean, val onClick: () -> Unit)

/** Action de bas de parcours : libellé, disponibilité et action (spec §3.2). */
data class FlowAction(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowScaffold(
    title: String,
    step: Int,
    onBack: () -> Unit,
    bottomAction: FlowAction,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = FlowBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { SalesPageTitle(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back), tint = FlowInk)
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.ops_status_draft), fontSize = 9.sp) },
                    )
                },
            )
        },
        bottomBar = {
            Button(
                onClick = bottomAction.onClick,
                enabled = bottomAction.enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
            ) {
                Text(bottomAction.label, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("→", fontSize = 18.sp)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SaleProgress(step)
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues())
            }
        }
    }
}

@Composable
private fun SaleProgress(current: Int) {
    val labels = listOf(
        stringResource(R.string.sales_step_client),
        stringResource(R.string.sales_step_products),
        stringResource(R.string.sales_step_payment),
        stringResource(R.string.sales_step_summary),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(18.dp),
                    shape = CircleShape,
                    color = if (index <= current) FlowBlue else FlowBorder,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (index < current) "✓" else (index + 1).toString(), color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(label, color = if (index == current) FlowBlue else FlowMuted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesListScreen(
    records: List<OperationRecordEntity>,
    devise: String,
    onNewSale: () -> Unit,
    onOpenRecord: (OperationRecordEntity) -> Unit,
    onNavigate: (String) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    val visible = records.filter { record ->
        val matchesSearch = search.isBlank() || record.reference.contains(search, true) ||
            record.title.contains(search, true) || record.counterpart.orEmpty().contains(search, true)
        val matchesFilter = when (selectedFilter) {
            "DRAFT" -> record.status == OperationStatus.DRAFT.name
            "VALIDATED" -> record.status == OperationStatus.VALIDATED.name
            "CANCELLED" -> record.status == OperationStatus.CANCELLED.name
            else -> true
        }
        matchesSearch && matchesFilter
    }
    Scaffold(
        containerColor = FlowBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { SalesPageTitle(stringResource(R.string.sales_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Routes.HOME) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back))
                    }
                },
            )
        },
        bottomBar = { FlowBottomBar(onNavigate) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Button(
                    onClick = onNewSale,
                    modifier = Modifier.fillMaxWidth().height(43.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.sales_new_sale), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Cycle commercial (spec §20) : devis → commande → facture.
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.DEVIS_COMMANDE) },
                    shape = RoundedCornerShape(7.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, FlowBorder),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = FlowBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sales_devis_entry), color = FlowInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.sales_devis_entry_desc), color = FlowMuted, fontSize = 10.sp)
                        }
                        Text("›", color = FlowMuted, fontSize = 18.sp)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text(stringResource(R.string.sales_search_sales), fontSize = 12.sp) },
                )
            }
            item { SalesFilterRow(selected = selectedFilter, records = records, onSelect = { selectedFilter = it }) }
            if (visible.isEmpty()) {
                item { EmptySalesList(onNewSale) }
            } else {
                items(visible, key = { it.id }) { record ->
                    SaleListRow(record, devise, onClick = { onOpenRecord(record) })
                }
            }
        }
    }
}

@Composable
private fun SalesFilterRow(selected: String, records: List<OperationRecordEntity>, onSelect: (String) -> Unit) {
    val filters = listOf(
        "ALL" to stringResource(R.string.sales_all),
        "DRAFT" to stringResource(R.string.sales_drafts, records.count { it.status == OperationStatus.DRAFT.name }),
        "VALIDATED" to stringResource(R.string.sales_validated),
        "CANCELLED" to stringResource(R.string.sales_cancelled),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        filters.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label, fontSize = 9.sp, maxLines = 1) })
        }
    }
}

@Composable
private fun EmptySalesList(onNewSale: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 25.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, FlowBorder),
    ) {
        Column(
            modifier = Modifier.padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = FlowBlueSoft) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = FlowBlue, modifier = Modifier.padding(11.dp))
            }
            Text(stringResource(R.string.sales_no_sales), color = FlowInk, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.sales_no_sales_description), color = FlowMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            TextButton(onClick = onNewSale) { Text(stringResource(R.string.sales_new_sale)) }
        }
    }
}

@Composable
private fun SaleListRow(record: OperationRecordEntity, devise: String, onClick: () -> Unit) {
    val status = OperationStatus.entries.firstOrNull { it.name == record.status } ?: OperationStatus.DRAFT
    val statusLabel = when (status) {
        OperationStatus.DRAFT -> stringResource(R.string.ops_status_draft)
        OperationStatus.VALIDATED -> stringResource(R.string.sales_status_paid)
        OperationStatus.CANCELLED -> stringResource(R.string.ops_status_cancelled)
    }
    val statusColor = when (status) {
        OperationStatus.DRAFT -> FlowBlue
        OperationStatus.VALIDATED -> FlowGreen
        OperationStatus.CANCELLED -> FlowRed
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FlowBorder),
    ) {
        Row(modifier = Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(35.dp), shape = CircleShape, color = FlowBlueSoft) {
                Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.reference, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(record.counterpart ?: record.title, color = FlowMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DateUtils.formatDateHeure(record.createdAt), color = FlowMuted, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                AssistChip(
                    onClick = {}, enabled = false, label = { Text(statusLabel, fontSize = 8.sp) },
                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(disabledLabelColor = statusColor, disabledContainerColor = statusColor.copy(alpha = 0.10f)),
                )
                Text(saleMoney(record.amount ?: 0.0, devise), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ClientStepContent(client: ClientEntity?, onSelect: () -> Unit, onNew: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sales_select_customer), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (client == null) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, FlowBorder),
            ) {
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = FlowBlueSoft) { Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue, modifier = Modifier.padding(10.dp)) }
                    Spacer(Modifier.width(11.dp))
                    Text(stringResource(R.string.sales_select_customer), modifier = Modifier.weight(1f), color = FlowMuted, fontSize = 13.sp)
                    Icon(Icons.Outlined.ArrowDropDown, null, tint = FlowInk)
                }
            }
        } else {
            SelectedCustomerCard(client, onClick = onSelect)
        }
        OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.sales_new_customer))
        }
        Text(stringResource(R.string.sales_customer_step_hint), color = FlowMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SelectedCustomerCard(client: ClientEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder),
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = FlowBlueSoft) { Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sales_customer), color = FlowMuted, fontSize = 10.sp)
                Text(client.nom, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(client.telephone, color = FlowMuted, fontSize = 10.sp)
            }
            Text(stringResource(R.string.sales_edit), color = FlowBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProductsStepContent(
    client: ClientEntity?, lines: List<SaleLine>, products: List<com.missa.b360.ui.stock.ProductWithStock>,
    query: String, onQueryChange: (String) -> Unit,
    onAddFree: () -> Unit, onScan: () -> Unit,
    onAddCatalog: (com.missa.b360.ui.stock.ProductWithStock) -> Unit,
    devise: String, modifier: Modifier = Modifier,
) {
    // Recherche locale sur le catalogue (nom, code, référence, code-barres — spec §9/§47).
    val visible = products.filter { product ->
        query.isBlank() ||
            product.nom.contains(query, ignoreCase = true) ||
            product.code.contains(query, ignoreCase = true) ||
            (product.reference?.contains(query, ignoreCase = true) ?: false) ||
            (product.barcode?.contains(query, ignoreCase = true) ?: false)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { client?.let { SelectedCustomerCard(it, onClick = {}) } }
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f), singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { IconButton(onClick = onScan) { Icon(Icons.Outlined.Search, stringResource(R.string.sales_scan)) } },
                placeholder = { Text(stringResource(R.string.sales_search_product), fontSize = 12.sp) },
            )
            OutlinedButton(onClick = onAddFree, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 12.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                Text(stringResource(R.string.sales_free_product), fontSize = 10.sp)
            }
        } }
        item { Text(stringResource(R.string.sales_products_title), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        if (visible.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Inventory2, null, tint = FlowBlue, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(7.dp))
                        Text(
                            stringResource(
                                if (products.isEmpty()) R.string.sales_catalog_empty else R.string.sales_no_results,
                            ),
                            color = FlowInk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                        if (products.isEmpty()) {
                            Text(
                                stringResource(R.string.sales_catalog_empty_description),
                                color = FlowMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        } else {
            items(visible, key = { it.product.id }) { product ->
                CatalogRow(
                    product = product,
                    inCart = lines.count { it.productId == product.product.id },
                    onAdd = { onAddCatalog(product) },
                    devise = devise,
                )
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = FlowBlueSoft) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ShoppingCart, null, tint = FlowBlue)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sales_cart_count, lines.size), modifier = Modifier.weight(1f), color = FlowInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(saleMoney(lines.sumOf { it.total }, devise), color = FlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Ligne du catalogue : produit, stock disponible, prix et ajout rapide (spec §9). */
@Composable
private fun CatalogRow(
    product: com.missa.b360.ui.stock.ProductWithStock,
    inCart: Int,
    onAdd: () -> Unit,
    devise: String,
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(7.dp), color = FlowBlueSoft) { Icon(Icons.Outlined.Inventory2, null, tint = FlowBlue, modifier = Modifier.padding(8.dp)) }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nom, color = FlowInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.sales_stock_available, product.stock.saleQty()),
                    color = if (product.stock <= 0.0) FlowRed else FlowMuted,
                    fontSize = 9.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                product.prixVente?.let {
                    Text(saleMoney(it, devise), color = FlowInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (inCart > 0) {
                    Text(stringResource(R.string.sales_in_cart, inCart), color = FlowBlue, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Outlined.Add, stringResource(R.string.sales_add_product), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CartStepContent(
    lines: List<SaleLine>, products: List<com.missa.b360.ui.stock.ProductWithStock>,
    totals: SaleTotals, taxRate: Double, devise: String, note: String, discount: String, delivery: String,
    onNoteChange: (String) -> Unit, onDiscountChange: (String) -> Unit, onDeliveryChange: (String) -> Unit,
    onQuantityChange: (Long, Double) -> Unit, onEditLine: (SaleLine) -> Unit,
    onRemove: (Long) -> Unit, modifier: Modifier = Modifier,
) {
    val stockOf: (SaleLine) -> Double? = { line ->
        line.productId?.let { id -> products.firstOrNull { it.product.id == id }?.stock }
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item { CartTable(lines, devise, stockOf, onQuantityChange, onEditLine, onRemove) }
        item {
            OutlinedTextField(value = note, onValueChange = onNoteChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_add_note)) }, leadingIcon = { Icon(Icons.Outlined.ReceiptLong, null) }, minLines = 1, maxLines = 2)
        }
        item { CartTotals(totals, taxRate, devise, discount, delivery, onDiscountChange, onDeliveryChange) }
    }
}

@Composable
private fun CartTable(
    lines: List<SaleLine>,
    devise: String,
    stockOf: (SaleLine) -> Double?,
    onQuantityChange: (Long, Double) -> Unit,
    onEditLine: (SaleLine) -> Unit,
    onRemove: (Long) -> Unit,
) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(FlowBlueSoft).padding(horizontal = 10.dp, vertical = 8.dp)) {
                TableTitle(R.string.sales_product, Modifier.weight(1f), TextAlign.Start)
                TableTitle(R.string.sales_qty, Modifier.width(62.dp), TextAlign.Center)
                TableTitle(R.string.sales_unit_price, Modifier.width(58.dp), TextAlign.End)
                TableTitle(R.string.sales_total, Modifier.width(60.dp), TextAlign.End)
                Spacer(Modifier.width(22.dp))
            }
            lines.forEach { line ->
                val stock = stockOf(line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditLine(line) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.name, color = FlowInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (line.productId != null) {
                            Text(
                                stringResource(R.string.sales_stock_available, stock?.saleQty() ?: "—"),
                                color = if (stock != null && line.quantity > stock) FlowRed else FlowMuted,
                                fontSize = 9.sp,
                            )
                        } else {
                            Text(stringResource(R.string.sales_free_item), color = FlowBlue, fontSize = 9.sp)
                        }
                    }
                    TinyQuantity(line.quantity, Modifier.width(62.dp), onMinus = { onQuantityChange(line.id, -1.0) }, onPlus = { onQuantityChange(line.id, 1.0) })
                    Text(saleMoney(line.unitPrice, devise), Modifier.width(58.dp), color = FlowInk, fontSize = 9.sp, textAlign = TextAlign.End, maxLines = 1)
                    Text(saleMoney(line.total, devise), Modifier.width(60.dp), color = FlowInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1)
                    IconButton(onClick = { onRemove(line.id) }, modifier = Modifier.size(22.dp)) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_remove_item), tint = FlowRed, modifier = Modifier.size(18.dp)) }
                }
                HorizontalDivider(color = FlowBorder)
            }
        }
    }
}

/** Modification d'une ligne du panier : quantité et prix unitaire (spec §9 PANIER). */
@Composable
private fun LineEditDialog(
    line: SaleLine,
    stock: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    var quantity by rememberSaveable { mutableStateOf(line.quantity.saleQty()) }
    var price by rememberSaveable { mutableStateOf(line.unitPrice.toInputAmount()) }
    val quantityValue = quantity.toSaleValue()
    val priceValue = price.toSaleValue()
    val valid = (quantityValue != null && quantityValue > 0.0) && (priceValue != null && priceValue >= 0.0)
    val exceedsStock = stock != null && quantityValue != null && quantityValue > stock
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_edit_line)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(line.name, color = FlowInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.moneyChars() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_qty)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.moneyChars() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_unit_price)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (exceedsStock) {
                    Text(
                        stringResource(R.string.sales_line_exceeds_stock, stock.saleQty()),
                        color = FlowRed,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(quantityValue ?: 0.0, priceValue ?: 0.0) }, enabled = valid) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun TableTitle(res: Int, modifier: Modifier, align: TextAlign) = Text(stringResource(res), modifier = modifier, color = FlowMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = align, maxLines = 1)

@Composable
private fun TinyQuantity(quantity: Double, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onMinus, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(19.dp)) { Text("−", color = FlowBlue, fontSize = 15.sp) }
        Text(quantity.saleQty(), modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, color = FlowInk, fontSize = 10.sp)
        TextButton(onClick = onPlus, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(19.dp)) { Text("+", color = FlowBlue, fontSize = 14.sp) }
    }
}

@Composable
private fun CartTotals(totals: SaleTotals, taxRate: Double, devise: String, discount: String, delivery: String, onDiscountChange: (String) -> Unit, onDeliveryChange: (String) -> Unit) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(stringResource(R.string.sales_summary), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            FlowAmountLine(stringResource(R.string.sales_subtotal), saleMoney(totals.subtotal, devise))
            FlowInputLine(stringResource(R.string.sales_discount), "−", discount, devise, onDiscountChange)
            FlowInputLine(stringResource(R.string.sales_delivery), "+", delivery, devise, onDeliveryChange)
            HorizontalDivider(color = FlowBorder)
            FlowAmountLine(stringResource(R.string.sales_total), saleMoney(totals.total, devise), strong = true)
            FlowAmountLine(stringResource(R.string.sales_tax_included, taxRate.saleRate()), saleMoney(totals.taxAmount, devise))
        }
    }
}

@Composable
private fun FlowInputLine(label: String, prefix: String, value: String, devise: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = FlowMuted, fontSize = 11.sp)
        Text(prefix, color = FlowMuted)
        OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.width(78.dp).height(42.dp).padding(start = 5.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Text(devise, modifier = Modifier.padding(start = 4.dp), color = FlowMuted, fontSize = 9.sp)
    }
}

@Composable
private fun FlowAmountLine(label: String, value: String, strong: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), color = if (strong) FlowInk else FlowMuted, fontSize = if (strong) 12.sp else 11.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (strong) FlowBlue else FlowInk, fontSize = if (strong) 15.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentStepContent(client: ClientEntity?, totals: SaleTotals, paymentMethod: String, paidInput: String, devise: String, onChoosePayment: () -> Unit, onPaidChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val paid = paidInput.toSaleValue() ?: totals.total
    val remaining = (totals.total - paid).coerceAtLeast(0.0)
    Column(modifier = modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        client?.let { SelectedCustomerCard(it, onClick = {}) }
        Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                FlowAmountLine(stringResource(R.string.sales_subtotal), saleMoney(totals.subtotal, devise))
                FlowAmountLine(stringResource(R.string.sales_discount), saleMoney(totals.discount, devise))
                FlowAmountLine(stringResource(R.string.sales_delivery), saleMoney(totals.delivery, devise))
                HorizontalDivider(color = FlowBorder)
                FlowAmountLine(stringResource(R.string.sales_total_due), saleMoney(totals.total, devise), strong = true)
            }
        }
        Text(stringResource(R.string.sales_payment_method), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onChoosePayment), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder)) {
            Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PointOfSale, null, tint = FlowGreen)
                Spacer(Modifier.width(10.dp))
                Text(paymentMethod, modifier = Modifier.weight(1f), color = FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Icon(Icons.Outlined.ArrowDropDown, null, tint = FlowInk)
            }
        }
        Text(stringResource(R.string.sales_paid_amount), color = FlowMuted, fontSize = 11.sp)
        OutlinedTextField(value = paidInput, onValueChange = onPaidChange, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(saleMoney(totals.total, devise)) }, suffix = { Text(devise, fontSize = 10.sp) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = FlowGreenSoft, border = BorderStroke(1.dp, FlowGreen.copy(alpha = .55f))) {
            Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = FlowGreen)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sales_remaining), modifier = Modifier.weight(1f), color = FlowGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(saleMoney(remaining, devise), color = FlowGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Text(stringResource(R.string.sales_sale_date, DateUtils.formatDateHeure(System.currentTimeMillis())), color = FlowMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SummaryStepContent(client: ClientEntity?, lines: List<SaleLine>, totals: SaleTotals, paymentMethod: String, paidInput: String, devise: String, busy: Boolean, onSaveDraft: () -> Unit, modifier: Modifier = Modifier) {
    val paid = paidInput.toSaleValue() ?: totals.total
    Column(modifier = modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        client?.let { SelectedCustomerCard(it, onClick = {}) }
        Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FlowAmountLine(stringResource(R.string.sales_articles), lines.size.toString())
                FlowAmountLine(stringResource(R.string.sales_subtotal), saleMoney(totals.subtotal, devise))
                FlowAmountLine(stringResource(R.string.sales_discount), saleMoney(totals.discount, devise))
                FlowAmountLine(stringResource(R.string.sales_delivery), saleMoney(totals.delivery, devise))
                HorizontalDivider(color = FlowBorder)
                FlowAmountLine(stringResource(R.string.sales_total), saleMoney(totals.total, devise), strong = true)
            }
        }
        Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FlowAmountLine(stringResource(R.string.sales_payment_method), paymentMethod)
                FlowAmountLine(stringResource(R.string.sales_paid_amount), saleMoney(paid, devise))
                FlowAmountLine(stringResource(R.string.sales_remaining), saleMoney((totals.total - paid).coerceAtLeast(0.0), devise))
            }
        }
        TextButton(onClick = onSaveDraft, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.sales_save_draft))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessStepScreen(receipt: SaleReceipt, devise: String, onViewInvoice: () -> Unit, onNewSale: () -> Unit, onBackList: () -> Unit) {
    Scaffold(containerColor = Color.White, topBar = { CenterAlignedTopAppBar(title = { SalesPageTitle(stringResource(R.string.sales_success_title)) }, navigationIcon = { IconButton(onClick = onBackList) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back)) } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.size(84.dp), shape = CircleShape, color = FlowGreen) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.padding(18.dp)) }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.sales_success_title), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(stringResource(R.string.sales_success_description), color = FlowMuted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            InvoiceInfoCard(receipt, devise)
            Spacer(Modifier.height(22.dp))
            Button(onClick = onViewInvoice, modifier = Modifier.fillMaxWidth().height(47.dp), colors = ButtonDefaults.buttonColors(containerColor = FlowBlue)) { Text(stringResource(R.string.sales_view_invoice)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNewSale, modifier = Modifier.fillMaxWidth().height(45.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.sales_new_sale)) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBackList) { Icon(Icons.Outlined.Home, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.sales_back_list)) }
        }
    }
}

@Composable
private fun InvoiceInfoCard(receipt: SaleReceipt, devise: String) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlowAmountLine(stringResource(R.string.sales_invoice_number), receipt.reference)
            FlowAmountLine(stringResource(R.string.sales_invoice_date), DateUtils.formatDateHeure(receipt.createdAt))
            FlowAmountLine(stringResource(R.string.sales_invoice_total), saleMoney(receipt.total, devise), strong = true)
            FlowAmountLine(stringResource(R.string.sales_payment_method), receipt.paymentMethod)
            FlowAmountLine(stringResource(R.string.sales_status), if (receipt.total <= receipt.paidAmount) stringResource(R.string.sales_status_paid) else stringResource(R.string.sales_status_pending))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoicePreviewScreen(receipt: SaleReceipt, devise: String, onBack: () -> Unit, onShare: () -> Unit, onOptions: () -> Unit) {
    Scaffold(containerColor = FlowBackground, topBar = { CenterAlignedTopAppBar(title = { SalesPageTitle(stringResource(R.string.sales_invoice_preview)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back)) } }, actions = { IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, stringResource(R.string.sales_share_invoice)) } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { InvoicePaper(receipt, devise) }
            item { Button(onClick = onOptions, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = FlowBlue)) { Text(stringResource(R.string.sales_invoice_options)) } }
        }
    }
}

@Composable
private fun InvoicePaper(receipt: SaleReceipt, devise: String) {
    val payload = receipt.payload
    Card(shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                androidx.compose.foundation.Image(painter = painterResource(R.drawable.logo_missa), contentDescription = stringResource(R.string.app_name), modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) { Text("MISSA BUSINESS", color = FlowInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp); Text("360", color = FlowGreen, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                Column(horizontalAlignment = Alignment.End) { Text(stringResource(R.string.sales_invoice_customer).uppercase(), color = FlowInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(if (payload.total <= payload.paidAmount) stringResource(R.string.sales_status_paid) else stringResource(R.string.sales_status_pending), color = FlowGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
            HorizontalDivider(color = FlowBorder)
            FlowAmountLine(stringResource(R.string.sales_invoice_number), receipt.reference)
            FlowAmountLine(stringResource(R.string.sales_invoice_date), DateUtils.formatDateHeure(receipt.createdAt))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.sales_customer), color = FlowMuted, fontSize = 9.sp)
                Text(payload.clientName, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            HorizontalDivider(color = FlowBorder)
            Row { TableTitle(R.string.sales_product, Modifier.weight(1f), TextAlign.Start); TableTitle(R.string.sales_qty, Modifier.width(36.dp), TextAlign.End); TableTitle(R.string.sales_unit_price, Modifier.width(68.dp), TextAlign.End); TableTitle(R.string.sales_total, Modifier.width(72.dp), TextAlign.End) }
            payload.lines.forEach { line ->
                Row { Text(line.name, modifier = Modifier.weight(1f), color = FlowInk, fontSize = 10.sp); Text(line.quantity.saleQty(), modifier = Modifier.width(36.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End); Text(saleMoney(line.unitPrice, devise), modifier = Modifier.width(68.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End); Text(saleMoney(line.total, devise), modifier = Modifier.width(72.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End) }
            }
            HorizontalDivider(color = FlowBorder)
            FlowAmountLine(stringResource(R.string.sales_subtotal), saleMoney(payload.subtotal, devise))
            FlowAmountLine(stringResource(R.string.sales_discount), saleMoney(payload.discount, devise))
            FlowAmountLine(stringResource(R.string.sales_delivery), saleMoney(payload.delivery, devise))
            FlowAmountLine(
                stringResource(R.string.sales_tax_included, payload.taxRate.saleRate()),
                saleMoney(payload.taxAmount, devise),
            )
            FlowAmountLine(stringResource(R.string.sales_total), saleMoney(payload.total, devise), strong = true)
            FlowAmountLine(stringResource(R.string.sales_paid_amount), saleMoney(payload.paidAmount, devise))
            payload.note?.let { Text(it, color = FlowMuted, fontSize = 10.sp) }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.sales_invoice_thanks), modifier = Modifier.fillMaxWidth(), color = FlowMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            Text("MISSA BUSINESS 360", modifier = Modifier.fillMaxWidth(), color = FlowBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceOptionsScreen(
    receipt: SaleReceipt, devise: String, onBack: () -> Unit, onPrint: () -> Unit, onShare: () -> Unit,
    onDownload: () -> Unit, onEmail: () -> Unit, onView: () -> Unit, onDuplicate: () -> Unit, onReturn: () -> Unit, onCancel: () -> Unit,
) {
    Scaffold(containerColor = FlowBackground, topBar = { CenterAlignedTopAppBar(title = { SalesPageTitle(stringResource(R.string.sales_invoice_options)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back)) } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            item { InvoiceOption(Icons.Outlined.Print, R.string.sales_print_invoice, R.string.sales_print_invoice_description, onPrint) }
            item { InvoiceOption(Icons.Outlined.Share, R.string.sales_share_invoice, R.string.sales_share_invoice_description, onShare) }
            item { InvoiceOption(Icons.Outlined.Download, R.string.sales_download_pdf, R.string.sales_download_pdf_description, onDownload) }
            item { InvoiceOption(Icons.Outlined.Email, R.string.sales_email_invoice, R.string.sales_email_invoice_description, onEmail) }
            item { InvoiceOption(Icons.Outlined.ReceiptLong, R.string.sales_view_invoice, R.string.sales_view_invoice_description, onView) }
            item { InvoiceOption(Icons.Outlined.ContentCopy, R.string.sales_duplicate_sale, R.string.sales_duplicate_sale_description, onDuplicate) }
            item { InvoiceOption(Icons.Outlined.Rollback, R.string.sales_return_sale, R.string.sales_return_sale_description, onReturn) }
            item { InvoiceOption(Icons.Outlined.Cancel, R.string.sales_cancel_sale, R.string.sales_cancel_sale_description, onCancel, destructive = true) }
            item { Text(saleMoney(receipt.total, devise), color = FlowMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun InvoiceOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, subtitle: Int, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(11.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder)) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (destructive) FlowRed else FlowBlue, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), color = if (destructive) FlowRed else FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(stringResource(subtitle), color = FlowMuted, fontSize = 10.sp)
            }
            Text("›", color = FlowMuted, fontSize = 21.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrintScreen(receipt: SaleReceipt, devise: String, onBack: () -> Unit, onPrint: () -> Unit) {
    Scaffold(containerColor = Color(0xFF1B1C20), topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.sales_print_title), color = Color.White, fontSize = 14.sp) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back), tint = Color.White) } }, colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1B1C20))) }, bottomBar = { Button(onClick = onPrint, modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = FlowBlue)) { Icon(Icons.Outlined.Print, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.sales_print)) } }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.fillMaxWidth().height(480.dp), shape = RoundedCornerShape(5.dp), color = Color.White) { Box(modifier = Modifier.padding(14.dp)) { InvoicePaper(receipt, devise) } }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.sales_print_ready), color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun FlowBottomBar(onNavigate: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            FlowNav(Icons.Outlined.Home, R.string.sales_home) { onNavigate(Routes.HOME) }
            FlowNav(Icons.Outlined.ShoppingCart, R.string.sales_nav_sales, selected = true) { }
            FlowNav(Icons.Outlined.Inventory2, R.string.module_achats) { onNavigate(AppModule.ACHATS.route) }
            FlowNav(Icons.Outlined.PersonOutline, R.string.module_clients) { onNavigate(AppModule.CLIENTS.route) }
            FlowNav(Icons.Outlined.MoreVert, R.string.more_modules) { onNavigate(AppModule.REPORTING.route) }
        }
    }
}

@Composable
private fun FlowNav(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, selected: Boolean = false, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) FlowBlue else FlowMuted, modifier = Modifier.size(21.dp))
        Text(stringResource(label), color = if (selected) FlowBlue else FlowMuted, fontSize = 9.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowCustomerPicker(clients: List<ClientEntity>, selectedId: Long?, onSelect: (ClientEntity) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        var search by rememberSaveable { mutableStateOf("") }
        val filtered = clients.filter { it.nom.contains(search, true) || it.telephone.contains(search) }
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(stringResource(R.string.sales_select_customer), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.clients_recherche)) }, singleLine = true)
            Spacer(Modifier.height(7.dp))
            if (filtered.isEmpty()) Text(stringResource(R.string.sales_no_customer), color = FlowMuted, modifier = Modifier.padding(13.dp))
            else LazyColumn(modifier = Modifier.height(290.dp)) { items(filtered, key = { it.id }) { client -> Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(client) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue); Spacer(Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text(client.nom, color = FlowInk, fontWeight = FontWeight.SemiBold); Text(client.telephone, color = FlowMuted, fontSize = 10.sp) }; if (client.id == selectedId) Icon(Icons.Outlined.Check, null, tint = FlowGreen) }; HorizontalDivider(color = FlowBorder) } }
            OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.sales_new_customer)) }
        }
    }
}

@Composable
private fun FlowFreeProductDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    val valid = name.trim().length >= 2 && (price.toSaleValue() ?: 0.0) > 0.0 && (quantity.toSaleValue() ?: 0.0) > 0.0
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.sales_free_product_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_product_name)) }, singleLine = true); OutlinedTextField(value = price, onValueChange = { price = it.moneyChars() }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_unit_price)) }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(value = quantity, onValueChange = { quantity = it.moneyChars() }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_qty)) }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }, confirmButton = { Button(onClick = { onAdd(name, price, quantity) }, enabled = valid) { Text(stringResource(R.string.sales_add_to_cart)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowPaymentPicker(selected: String, methods: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(stringResource(R.string.sales_payment_method), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            methods.forEach { method -> Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(method) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.PointOfSale, null, tint = FlowGreen); Spacer(Modifier.width(10.dp)); Text(method, modifier = Modifier.weight(1f), color = FlowInk); if (method == selected) Icon(Icons.Outlined.Check, null, tint = FlowGreen) }; HorizontalDivider(color = FlowBorder) }
        }
    }
}

private fun Context.shareInvoice(receipt: SaleReceipt, devise: String) {
    val text = "${getString(R.string.sales_invoice_customer)} ${receipt.reference}\n${receipt.clientName}\n${getString(R.string.sales_total)}: ${saleMoney(receipt.total, devise)}"
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.sales_share_subject)} ${receipt.reference}").putExtra(Intent.EXTRA_TEXT, text), getString(R.string.sales_share_invoice)))
}

private fun Context.emailInvoice(receipt: SaleReceipt, devise: String) {
    val body = "${getString(R.string.sales_invoice_customer)} ${receipt.reference}\n${getString(R.string.sales_total)}: ${saleMoney(receipt.total, devise)}"
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.sales_email_subject)} ${receipt.reference}").putExtra(Intent.EXTRA_TEXT, body), getString(R.string.sales_email_invoice)))
}

private fun Context.writeInvoicePdf(uri: Uri, receipt: SaleReceipt, devise: String): Boolean = runCatching {
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(21, 84, 232); textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(16, 28, 67); textSize = 12f }
        canvas.drawText("MISSA BUSINESS 360", 45f, 64f, title)
        canvas.drawText(receipt.reference, 45f, 105f, body)
        canvas.drawText(DateUtils.formatDateHeure(receipt.createdAt), 45f, 128f, body)
        canvas.drawText(receipt.clientName, 45f, 151f, body)
        var y = 190f
        receipt.payload.lines.take(23).forEach { line ->
            canvas.drawText("${line.name.take(32)} × ${line.quantity.saleQty()}  ${saleMoney(line.total, devise)}", 45f, y, body)
            y += 23f
        }
        y += 12f
        canvas.drawText("${getString(R.string.sales_tax_included, receipt.payload.taxRate.saleRate())} : ${saleMoney(receipt.payload.taxAmount, devise)}", 45f, y, body)
        y += 25f
        canvas.drawText("${getString(R.string.sales_total)} : ${saleMoney(receipt.total, devise)}", 45f, y, title)
        document.finishPage(page)
        contentResolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Output stream unavailable")
    } finally { document.close() }
}.isSuccess

private fun Double.saleQty(): String = if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)
private fun Double.saleRate(): String = java.text.DecimalFormat("0.##").format(this)
private fun String.toSaleValue(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun String.moneyChars(): String = filter { it.isDigit() || it == ',' || it == '.' }
