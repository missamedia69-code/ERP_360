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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.missa.b360.core.data.entity.SaleStatus
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.domain.model.SaleCalculation
import com.missa.b360.core.domain.model.SaleErrorCode
import com.missa.b360.core.domain.model.SaleFormError
import com.missa.b360.core.domain.model.SaleFormLine
import com.missa.b360.core.domain.model.SaleSaveOutcome
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

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

private enum class SalesPage { LIST, FORM, SUCCESS, INVOICE }

/**
 * Module VENTE — formulaire unique, compact et transactionnel.
 * Toutes les données proviennent de la base locale ; aucun produit/client/démo n'est inventé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onNavigate: (String) -> Unit,
    onOpenClientCreate: () -> Unit,
    openCreate: Boolean = false,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val calculation by viewModel.calculation.collectAsState()
    val validation by viewModel.validation.collectAsState()
    val actions by viewModel.actions.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val history by viewModel.history.collectAsState()
    val walkInAllowed by viewModel.walkInAllowed.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val enterpriseName by viewModel.enterpriseName.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var page by rememberSaveable(openCreate) { mutableStateOf(if (openCreate) SalesPage.FORM.name else SalesPage.LIST.name) }
    var currentPage by remember { mutableStateOf(SalesPage.valueOf(page)) }
    var activeReceipt by remember { mutableStateOf<SaleReceipt?>(null) }
    var customerPickerVisible by remember { mutableStateOf(false) }
    var freeProductVisible by remember { mutableStateOf(false) }
    var confirmExitVisible by remember { mutableStateOf(false) }
    var cancelVisible by remember { mutableStateOf(false) }
    var cancelTargetId by remember { mutableStateOf<Long?>(null) }
    var printAfterSave by remember { mutableStateOf(false) }
    var scannerUnavailable by remember { mutableStateOf(false) }

    var formInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(currentPage, openCreate) {
        if (currentPage == SalesPage.FORM && openCreate && !formInitialized) {
            formInitialized = true
            viewModel.newSale()
        }
    }

    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is SaleSaveOutcome.Success -> {
                val receipt = SaleReceipt(
                    recordId = result.recordId,
                    reference = result.reference,
                    payload = result.output,
                    completed = result.completed,
                )
                activeReceipt = receipt
                if (result.draft) {
                    snackbar.showSnackbar(context.getString(R.string.sales_draft_saved, result.reference))
                    currentPage = SalesPage.LIST
                } else {
                    if (printAfterSave) {
                        context.printSaleReceipt(receipt, devise)
                        printAfterSave = false
                    }
                    currentPage = SalesPage.SUCCESS
                }
                viewModel.clearSaveResult()
            }
            is SaleSaveOutcome.Failed -> {
                val error = result.error
                val message = if (error.code == SaleErrorCode.STOCK_INSUFFICIENT) {
                    context.getString(
                        R.string.sales_stock_insufficient,
                        error.productName.orEmpty(),
                        error.available?.let { (it / 100.0).saleQty() } ?: "0",
                    )
                } else {
                    context.getString(errorMessage(error))
                }
                printAfterSave = false
                snackbar.showSnackbar(message)
                viewModel.clearSaveResult()
            }
            SaleSaveOutcome.ReadOnly -> {
                printAfterSave = false
                snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                viewModel.clearSaveResult()
            }
            null -> Unit
        }
    }

    LaunchedEffect(actions.stockIssue) {
        actions.stockIssue?.let { issue ->
            snackbar.showSnackbar(
                context.getString(
                    R.string.sales_stock_insufficient,
                    issue.name,
                    issue.stockAvailable?.saleQty() ?: "0",
                ),
            )
        }
    }

    LaunchedEffect(scannerUnavailable) {
        if (scannerUnavailable) {
            snackbar.showSnackbar(context.getString(R.string.sales_scanner_unavailable))
            scannerUnavailable = false
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = result.data?.getStringExtra("SCAN_RESULT").orEmpty()
        if (scanned.isNotBlank()) viewModel.setQuery(scanned)
    }

    if (currentPage == SalesPage.FORM) {
        BackHandler {
            if (state.hasUnsavedInput) confirmExitVisible = true
            else {
                viewModel.clearForm()
                currentPage = SalesPage.LIST
            }
        }
    }
    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val receipt = activeReceipt
        if (uri != null && receipt != null) {
            context.writeInvoicePdf(uri, receipt, devise)
        }
    }

    Scaffold(
        containerColor = FlowBackground,
        topBar = {
            when (currentPage) {
                SalesPage.FORM -> SalesTopBar(
                    title = stringResource(R.string.sales_new_sale),
                    reference = state.lastGeneratedReference,
                    onBack = {
                        if (state.hasUnsavedInput) confirmExitVisible = true else {
                            viewModel.clearForm()
                            currentPage = SalesPage.LIST
                        }
                    },
                )
                SalesPage.LIST -> SalesTopBar(
                    title = stringResource(R.string.sales_nav_sales),
                    onBack = { onNavigate(Routes.HOME) },
                    actions = {
                        IconButton(onClick = {
                            viewModel.newSale()
                            currentPage = SalesPage.FORM
                        }) { Icon(Icons.Outlined.Add, stringResource(R.string.sales_new_sale), tint = FlowBlue) }
                    },
                )
                SalesPage.SUCCESS -> SalesTopBar(
                    title = stringResource(R.string.sales_success_title),
                    onBack = { currentPage = SalesPage.LIST },
                )
                SalesPage.INVOICE -> SalesTopBar(
                    title = stringResource(R.string.sales_invoice_preview),
                    onBack = { currentPage = if (state.hasUnsavedInput) SalesPage.FORM else SalesPage.LIST },
                )
            }
        },
        bottomBar = {
            when (currentPage) {
                SalesPage.FORM -> SalesFormBottomBar(
                    actions = actions,
                    onCancel = {
                        if (state.hasUnsavedInput) confirmExitVisible = true
                        else {
                            viewModel.clearForm()
                            currentPage = SalesPage.LIST
                        }
                    },
                    onDraft = { viewModel.save(draft = true) },
                    onSave = { viewModel.save(draft = false) },
                    onSavePrint = {
                        printAfterSave = true
                        viewModel.save(draft = false)
                    },
                )
                SalesPage.LIST -> FlowBottomBar(onNavigate)
                else -> Unit
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentPage) {
                SalesPage.LIST -> SalesListPage(
                    history = history,
                    devise = devise,
                    onOpenForm = {
                        viewModel.newSale()
                        currentPage = SalesPage.FORM
                    },
                    onOpenItem = { item ->
                        val sale = item.sale
                        val payload = item.payload
                        if (item.isDraft && sale != null) {
                            viewModel.loadDraft(sale, clients)
                            currentPage = SalesPage.FORM
                        } else if (payload != null) {
                            activeReceipt = SaleReceipt(
                                recordId = item.id,
                                reference = item.reference,
                                payload = payload,
                                createdAt = item.createdAt,
                            )
                            currentPage = SalesPage.INVOICE
                        } else if (sale != null) {
                            activeReceipt = receiptFromSale(sale, devise)
                            currentPage = SalesPage.INVOICE
                        }
                    },
                    onDuplicate = { item ->
                        if (viewModel.duplicateSale(item, clients)) currentPage = SalesPage.FORM
                    },
                    onCancel = { item ->
                        val sale = item.sale ?: return@SalesListPage
                        cancelTargetId = sale.id
                        cancelVisible = true
                    },
                    onValidateDraft = { item ->
                        viewModel.validateDraft(item.id)
                    },
                )
                SalesPage.FORM -> SaleFormPage(
                    state = state,
                    clients = clients,
                    products = products,
                    categories = categories,
                    paymentMethods = paymentMethods.ifEmpty { listOf(context.getString(R.string.sales_cash)) },
                    taxRate = taxRate,
                    devise = devise,
                    walkInAllowed = walkInAllowed,
                    calculation = calculation,
                    validation = validation,
                    onSelectCustomer = { customerPickerVisible = true },
                    onNewCustomer = {
                        customerPickerVisible = false
                        onOpenClientCreate()
                    },
                    onWalkIn = viewModel::setWalkIn,
                    onQueryChange = viewModel::setQuery,
                    onCategoryChange = viewModel::setCategory,
                    onAddProduct = viewModel::addProduct,
                    onAddFree = { freeProductVisible = true },
                    onScan = {
                        val intent = Intent("com.google.zxing.client.android.SCAN")
                        if (intent.resolveActivity(context.packageManager) != null) {
                            scannerLauncher.launch(intent)
                        } else {
                            scannerUnavailable = true
                        }
                    },
                    onQuantity = viewModel::setQuantity,
                    onIncrement = { lineId -> viewModel.changeQuantity(lineId, 1.0) },
                    onDecrement = { lineId -> viewModel.changeQuantity(lineId, -1.0) },
                    onRemoveLine = viewModel::removeLine,
                    onDiscountChange = viewModel::updateDiscount,
                    onToggleDiscountMode = viewModel::toggleDiscountPercentMode,
                    onDeliveryChange = viewModel::updateDelivery,
                    onPaymentMethod = viewModel::setPaymentMethod,
                    onToggleCredit = viewModel::toggleCredit,
                    onReceivedChange = viewModel::updateReceived,
                    onPaidChange = viewModel::updatePaid,
                    onNoteChange = viewModel::updateNote,
                    onReferenceChange = viewModel::updateInternalReference,
                    onToggleDetails = viewModel::toggleDetails,
                    enterpriseName = enterpriseName,
                )
                SalesPage.SUCCESS -> activeReceipt?.let { receipt ->
                    SaleSuccessScreen(
                        receipt = receipt,
                        devise = devise,
                        onViewInvoice = { currentPage = SalesPage.INVOICE },
                        onPrint = { context.printSaleReceipt(receipt, devise) },
                        onShare = { context.shareInvoice(receipt, devise) },
                        onNewSale = {
                            viewModel.newSale()
                            currentPage = SalesPage.FORM
                        },
                        onBackList = { currentPage = SalesPage.LIST },
                    )
                } ?: Unit
                SalesPage.INVOICE -> activeReceipt?.let { receipt ->
                    InvoicePreviewScreen(
                        receipt = receipt,
                        devise = devise,
                        onShare = { context.shareInvoice(receipt, devise) },
                        onPrint = { context.printSaleReceipt(receipt, devise) },
                        onDownload = { createPdf.launch("${receipt.reference}.pdf") },
                        onEmail = { context.emailInvoice(receipt, devise) },
                        onDuplicate = {
                            val item = history.firstOrNull { it.reference == receipt.reference }
                            if (item != null && viewModel.duplicateSale(item, clients)) currentPage = SalesPage.FORM
                        },
                        onCancel = {
                            val sale = history.firstOrNull { it.reference == receipt.reference }?.sale
                            if (sale != null) cancelTargetId = sale.id
                            cancelVisible = true
                        },
                    )
                } ?: Unit
            }
        }
    }

    if (customerPickerVisible) {
        CustomerPickerSheet(
            clients = clients,
            selectedId = state.selectedClient?.id,
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
        FreeProductDialog(
            onDismiss = { freeProductVisible = false },
            onAdd = { name, price, quantity ->
                if (viewModel.addFreeProduct(name, price, quantity)) {
                    viewModel.setQuery("")
                    freeProductVisible = false
                }
            },
        )
    }
    if (confirmExitVisible) {
        AlertDialog(
            onDismissRequest = { confirmExitVisible = false },
            title = { Text(stringResource(R.string.sales_confirm_exit_title)) },
            text = { Text(stringResource(R.string.sales_confirm_exit_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmExitVisible = false
                        viewModel.clearForm()
                        currentPage = SalesPage.LIST
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                ) { Text(stringResource(R.string.sales_confirm_exit_leave)) }
            },
            dismissButton = { TextButton(onClick = { confirmExitVisible = false }) { Text(stringResource(R.string.ops_cancel)) } },
        )
    }
    if (cancelVisible && cancelTargetId != null) {
        AlertDialog(
            onDismissRequest = { cancelVisible = false },
            title = { Text(stringResource(R.string.sales_cancel_sale)) },
            text = { Text(stringResource(R.string.sales_cancel_sale_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(cancelTargetId!!)
                        cancelVisible = false
                        currentPage = SalesPage.LIST
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowRed),
                ) { Text(stringResource(R.string.sales_cancel_sale)) }
            },
            dismissButton = { TextButton(onClick = { cancelVisible = false }) { Text(stringResource(R.string.ops_cancel)) } },
        )
    }
    SnackbarHost(
        hostState = snackbar,
        modifier = Modifier.eralign(Alignment.BottomCenter).padding(bottom = 120.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Barre supérieure et barre d'actions fixes
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesTopBar(
    title: String,
    onBack: () -> Unit,
    reference: String? = null,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MissaBrandMark(size = 22.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        title,
                        color = FlowInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!reference.isNullOrBlank()) {
                    Text(reference, color = FlowMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.sales_back), tint = FlowInk)
            }
        },
        actions = { actions() },
    )
}

@Composable
private fun SalesFormBottomBar(
    actions: SaleFormActions,
    onCancel: () -> Unit,
    onDraft: () -> Unit,
    onSave: () -> Unit,
    onSavePrint: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(R.string.ops_cancel), fontSize = 11.sp)
                }
                OutlinedButton(onClick = onDraft, enabled = actions.canDraft, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(R.string.sales_save_draft), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSave,
                    enabled = actions.canSave,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowBlue),
                ) {
                    Text(stringResource(R.string.sales_save_sale).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onSavePrint,
                    enabled = actions.canPrint,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowGreen),
                ) {
                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.sales_save_print), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Liste des ventes
// ---------------------------------------------------------------------------------------------

@Composable
private fun SalesListPage(
    history: List<SaleHistoryItem>,
    devise: String,
    onOpenForm: () -> Unit,
    onOpenItem: (SaleHistoryItem) -> Unit,
    onDuplicate: (SaleHistoryItem) -> Unit,
    onCancel: (SaleHistoryItem) -> Unit,
    onValidateDraft: (SaleHistoryItem) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ReceiptLong, null, tint = FlowBlueSoft, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.sales_no_sales), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.sales_no_sales_description), color = FlowMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onOpenForm, shape = RoundedCornerShape(9.dp)) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sales_new_sale))
                        }
                    }
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                SaleListRow(item = item, devise = devise, onOpen = { onOpenItem(item) }, onDuplicate = { onDuplicate(item) }, onCancel = { onCancel(item) }, onValidateDraft = { onValidateDraft(item) })
            }
        }
    }
}

@Composable
private fun SaleListRow(
    item: SaleHistoryItem,
    devise: String,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onCancel: () -> Unit,
    onValidateDraft: () -> Unit,
) {
    val statusColor = when (item.status) {
        SaleStatus.DRAFT.code -> FlowMuted
        SaleStatus.CANCELLED.code -> FlowRed
        else -> FlowGreen
    }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = FlowBlueSoft, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ReceiptLong, null, tint = FlowBlue, modifier = Modifier.size(20.dp)) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.reference, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(item.clientName, color = FlowMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DateUtils.formatDateHeure(item.createdAt), color = FlowMuted, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(saleMoney(item.total, devise), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                SaleStatusBadge(item.status)
            }
            if (item.isDraft && item.sale != null) {
                TextButton(onClick = onValidateDraft) { Text(stringResource(R.string.sales_validate_draft), fontSize = 10.sp, color = FlowGreen) }
            }
            IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, stringResource(R.string.sales_duplicate_sale), tint = FlowMuted, modifier = Modifier.size(17.dp)) }
            if (item.sale != null) {
                IconButton(onClick = onCancel) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_cancel_sale), tint = if (item.isCancelled) FlowMuted else FlowRed, modifier = Modifier.size(17.dp)) }
            }
        }
    }
}

@Composable
private fun SaleStatusBadge(status: String) {
    val label = when (status) {
        SaleStatus.DRAFT.code -> stringResource(R.string.ops_status_draft)
        SaleStatus.CANCELLED.code -> stringResource(R.string.sales_cancelled)
        else -> stringResource(R.string.sales_validated)
    }
    val color = when (status) {
        SaleStatus.DRAFT.code -> FlowMuted
        SaleStatus.CANCELLED.code -> FlowRed
        else -> FlowGreen
    }
    Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
}

// ---------------------------------------------------------------------------------------------
// Formulaire unique
// ---------------------------------------------------------------------------------------------

@Composable
private fun SaleFormPage(
    state: SaleFormUiState,
    clients: List<ClientEntity>,
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    paymentMethods: List<String>,
    taxRate: Double,
    devise: String,
    walkInAllowed: Boolean,
    calculation: SaleCalculation,
    validation: SaleFormError?,
    onSelectCustomer: () -> Unit,
    onNewCustomer: () -> Unit,
    onWalkIn: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAddProduct: (StockProductEntity) -> Unit,
    onAddFree: () -> Unit,
    onScan: () -> Unit,
    onQuantity: (Long, String) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit,
    onDiscountChange: (String) -> Unit,
    onToggleDiscountMode: () -> Unit,
    onDeliveryChange: (String) -> Unit,
    onPaymentMethod: (String) -> Unit,
    onToggleCredit: () -> Unit,
    onReceivedChange: (String) -> Unit,
    onPaidChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onToggleDetails: () -> Unit,
    enterpriseName: String,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item { SectionHeader(stringResource(R.string.sales_client).uppercase()) }
        item {
            ClientSection(
                state = state,
                clients = clients,
                walkInAllowed = walkInAllowed,
                onSelect = onSelectCustomer,
                onNew = onNewCustomer,
                onWalkIn = onWalkIn,
            )
        }
        item { SectionHeader(stringResource(R.string.sales_products_title).uppercase()) }
        item {
            ProductSearchSection(
                state = state,
                products = products,
                categories = categories,
                devise = devise,
                onQueryChange = onQueryChange,
                onCategoryChange = onCategoryChange,
                onAddProduct = onAddProduct,
                onAddFree = onAddFree,
                onScan = onScan,
            )
        }
        item {
            SectionHeader(stringResource(R.string.sales_cart_count, state.lines.size).uppercase())
            CartSection(
                lines = state.lines,
                devise = devise,
                onQuantity = onQuantity,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                onRemoveLine = onRemoveLine,
            )
        }
        item {
            SectionHeader(stringResource(R.string.sales_summary).uppercase())
            SummarySection(
                state = state,
                calculation = calculation,
                devise = devise,
                taxRate = taxRate,
                onDiscountChange = onDiscountChange,
                onToggleDiscountMode = onToggleDiscountMode,
                onDeliveryChange = onDeliveryChange,
            )
        }
        item {
            SectionHeader(stringResource(R.string.sales_payment).uppercase())
            PaymentSection(
                state = state,
                paymentMethods = paymentMethods,
                calculation = calculation,
                devise = devise,
                onPaymentMethod = onPaymentMethod,
                onToggleCredit = onToggleCredit,
                onReceivedChange = onReceivedChange,
                onPaidChange = onPaidChange,
            )
        }
        item {
            SectionHeader(stringResource(R.string.sales_details).uppercase())
            DetailsSection(
                state = state,
                enterpriseName = enterpriseName,
                onNoteChange = onNoteChange,
                onReferenceChange = onReferenceChange,
                onToggleDetails = onToggleDetails,
            )
        }
        item {
            if (state.lines.isNotEmpty() || state.paymentMethod.isNotBlank()) {
                validation?.let { ValidationHint(it) }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(label, color = FlowBlueDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 0.6.sp)
}

@Composable
private fun ClientSection(
    state: SaleFormUiState,
    clients: List<ClientEntity>,
    walkInAllowed: Boolean,
    onSelect: () -> Unit,
    onNew: () -> Unit,
    onWalkIn: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val client = state.selectedClient
        Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (client == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue)
                        Spacer(Modifier.width(9.dp))
                        Text(stringResource(R.string.sales_select_customer), color = FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.nom, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(listOf(client.telephone, client.code).filter { it.isNotBlank() }.joinToString(" · "), color = FlowMuted, fontSize = 10.sp)
                        }
                        Text(stringResource(R.string.sales_edit), color = FlowBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.sales_new_customer), fontSize = 10.sp)
            }
            if (walkInAllowed) {
                val selected = state.walkIn && state.selectedClient == null
                OutlinedButton(onClick = { onWalkIn(!selected) }, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(if (selected) Icons.Outlined.Check else Icons.Outlined.PersonOutline, null, modifier = Modifier.size(15.dp), tint = if (selected) FlowGreen else FlowMuted)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.sales_walk_in), fontSize = 10.sp, color = if (selected) FlowGreen else FlowMuted)
                }
            }
        }
    }
}

@Composable
private fun ProductSearchSection(
    state: SaleFormUiState,
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    devise: String,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAddProduct: (StockProductEntity) -> Unit,
    onAddFree: () -> Unit,
    onScan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = FlowMuted, modifier = Modifier.size(18.dp)) },
                placeholder = { Text(stringResource(R.string.sales_search_product), fontSize = 11.sp) },
            )
            OutlinedButton(onClick = onScan, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(9.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text(stringResource(R.string.sales_scan), fontSize = 9.sp)
            }
        }
        if (categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text(stringResource(R.string.sales_all), fontSize = 9.sp) },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = { onCategoryChange(if (state.categoryId == category.id) null else category.id) },
                        label = { Text(category.nom, fontSize = 9.sp) },
                    )
                }
            }
        }
        val filtered = products.filter { product ->
            val matchQuery = state.query.isBlank() ||
                product.nom.contains(state.query, ignoreCase = true) ||
                product.code.contains(state.query, ignoreCase = true)
            val matchCategory = state.categoryId == null || product.categorieId == state.categoryId
            matchQuery && matchCategory
        }
        if (filtered.isEmpty()) {
            EmptyCard(
                title = stringResource(R.string.sales_no_product),
                description = stringResource(R.string.sales_no_product_description),
                action = stringResource(R.string.sales_free_product),
                onAction = onAddFree,
            )
        } else {
            filtered.take(12).forEach { product ->
                ProductCompactCard(product = product, devise = devise, onAdd = { onAddProduct(product) })
            }
        }
    }
}

@Composable
private fun ProductCompactCard(product: StockProductEntity, devise: String, onAdd: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, FlowBorder)) {
        Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nom, color = FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(product.code, product.unite).filter { it.isNotBlank() }.joinToString(" · "),
                    color = FlowMuted,
                    fontSize = 9.sp,
                )
                Text(saleMoney(product.prixVente, devise), color = FlowBlueDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Text(stringResource(R.string.sales_available, product.quantite.saleQty()), color = if (product.quantite > 0) FlowGreen else FlowRed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onAdd, enabled = product.actif && product.prixVente > 0) {
                Icon(Icons.Outlined.Add, stringResource(R.string.sales_add_to_cart), tint = if (product.actif) FlowBlue else FlowMuted)
            }
        }
    }
}

@Composable
private fun CartSection(
    lines: List<SaleFormLine>,
    devise: String,
    onQuantity: (Long, String) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemoveLine: (Long) -> Unit,
) {
    if (lines.isEmpty()) {
        EmptyCard(
            title = stringResource(R.string.sales_empty_cart),
            description = stringResource(R.string.sales_empty_cart_description),
        )
        return
    }
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column {
            lines.forEachIndexed { index, line ->
                CompactCartLine(
                    line = line,
                    devise = devise,
                    onQuantity = { value -> onQuantity(line.id, value) },
                    onIncrement = { onIncrement(line.id) },
                    onDecrement = { onDecrement(line.id) },
                    onRemove = { onRemoveLine(line.id) },
                )
                if (index != lines.lastIndex) HorizontalDivider(color = FlowBorder)
            }
        }
    }
}

@Composable
private fun CompactCartLine(
    line: SaleFormLine,
    devise: String,
    onQuantity: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.name, color = FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(saleMoney(line.unitPriceCents.toDoubleMoney(), devise) + if (line.discountPct > 0) "  ·  -${line.discountPct.saleRate()}%" else "", color = FlowMuted, fontSize = 9.sp)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_remove_item), tint = FlowRed, modifier = Modifier.size(17.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sales_qty), color = FlowMuted, fontSize = 9.sp)
            IconButton(onClick = onDecrement, modifier = Modifier.size(30.dp)) { Text("−", color = FlowBlue, fontSize = 17.sp) }
            OutlinedTextField(
                value = line.quantity.saleQty(),
                onValueChange = onQuantity,
                modifier = Modifier.width(54.dp).height(42.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = FlowInk),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(30.dp)) { Text("+", color = FlowBlue, fontSize = 17.sp) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(saleMoney(line.netCents.toDoubleMoney(), devise), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (line.stockAvailable != null) {
                    Text(
                        stringResource(R.string.sales_available, line.stockAvailable.saleQty()),
                        color = if (line.quantity <= line.stockAvailable) FlowGreen else FlowRed,
                        fontSize = 8.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    state: SaleFormUiState,
    calculation: SaleCalculation,
    devise: String,
    taxRate: Double,
    onDiscountChange: (String) -> Unit,
    onToggleDiscountMode: () -> Unit,
    onDeliveryChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalLine(stringResource(R.string.sales_subtotal), saleMoney(calculation.subtotalCents.toDoubleMoney(), devise))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sales_discount), color = FlowMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleDiscountMode, contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text(if (state.discountPercentMode) "%" else stringResource(R.string.sales_amount), fontSize = 10.sp, color = FlowBlue)
                }
                OutlinedTextField(
                    value = state.discountInput,
                    onValueChange = onDiscountChange,
                    modifier = Modifier.width(100.dp).height(40.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            DeliveryInput(state.deliveryInput, onDeliveryChange, devise)
            if (taxRate > 0) {
                TotalLine(stringResource(R.string.sales_tax_included, taxRate.saleRate()), saleMoney(calculation.taxAmountCents.toDoubleMoney(), devise))
            }
            HorizontalDivider(color = FlowBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sales_total_due), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(saleMoney(calculation.totalCents.toDoubleMoney(), devise), color = FlowBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun DeliveryInput(value: String, onValueChange: (String) -> Unit, devise: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.sales_delivery), color = FlowMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(100.dp).height(40.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

@Composable
private fun PaymentSection(
    state: SaleFormUiState,
    paymentMethods: List<String>,
    calculation: SaleCalculation,
    devise: String,
    onPaymentMethod: (String) -> Unit,
    onToggleCredit: () -> Unit,
    onReceivedChange: (String) -> Unit,
    onPaidChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(stringResource(R.string.sales_total_due).uppercase(), color = FlowMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(saleMoney(calculation.totalCents.toDoubleMoney(), devise), color = FlowBlue, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(stringResource(R.string.sales_payment_method), color = FlowMuted, fontSize = 10.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(paymentMethods, key = { it }) { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { onPaymentMethod(method) },
                        label = { Text(method, fontSize = 10.sp) },
                    )
                }
            }
            if (state.isCredit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.sales_paid_amount), color = FlowMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = state.paidInput,
                        onValueChange = onPaidChange,
                        modifier = Modifier.width(110.dp).height(40.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                TotalLine(stringResource(R.string.sales_remaining), saleMoney(calculation.remainingCents.toDoubleMoney(), devise), strong = true, color = FlowRed)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.sales_received_amount), color = FlowMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = state.receivedInput,
                        onValueChange = onReceivedChange,
                        modifier = Modifier.width(110.dp).height(40.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                if (calculation.changeCents > 0) {
                    TotalLine(stringResource(R.string.sales_change), saleMoney(calculation.changeCents.toDoubleMoney(), devise), strong = true, color = FlowGreen)
                }
            }
            TextButton(onClick = onToggleCredit, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(if (state.isCredit) R.string.sales_disable_credit else R.string.sales_enable_credit), fontSize = 10.sp, color = if (state.isCredit) FlowRed else FlowBlue)
            }
        }
    }
}

@Composable
private fun DetailsSection(
    state: SaleFormUiState,
    enterpriseName: String,
    onNoteChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onToggleDetails: () -> Unit,
) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleDetails), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sales_more_details), color = FlowInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Icon(if (state.detailsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = FlowMuted)
            }
            if (state.detailsExpanded) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_add_note), fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = state.internalReference,
                    onValueChange = onReferenceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_internal_reference), fontSize = 11.sp) },
                    singleLine = true,
                )
                if (enterpriseName.isNotBlank()) {
                    Text("${stringResource(R.string.sales_seller)} : $enterpriseName", color = FlowMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ValidationHint(error: SaleFormError) {
    Text(
        stringResource(errorMessage(error)),
        color = FlowRed,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun EmptyCard(title: String, description: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ShoppingCart, null, tint = FlowBlueSoft, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(description, color = FlowMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(8.dp)) { Text(action, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, strong: Boolean = false, color: Color = FlowInk) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (strong) color else FlowMuted, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = if (strong) 12.sp else 11.sp)
    }
}

// ---------------------------------------------------------------------------------------------
// Sélecteurs et dialogues
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerPickerSheet(
    clients: List<ClientEntity>,
    selectedId: Long?,
    onSelect: (ClientEntity) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        var search by rememberSaveable { mutableStateOf("") }
        val filtered = clients.filter { it.nom.contains(search, true) || it.telephone.contains(search, true) }
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(stringResource(R.string.sales_select_customer), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.clients_recherche)) }, singleLine = true)
            Spacer(Modifier.height(7.dp))
            if (filtered.isEmpty()) {
                Text(stringResource(R.string.sales_no_customer), color = FlowMuted, modifier = Modifier.padding(13.dp))
            } else {
                LazyColumn(modifier = Modifier.height(290.dp)) {
                    items(filtered, key = { it.id }) { client ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(client) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PersonOutline, null, tint = FlowBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.nom, color = FlowInk, fontWeight = FontWeight.SemiBold)
                                Text(client.telephone, color = FlowMuted, fontSize = 10.sp)
                            }
                            if (client.id == selectedId) Icon(Icons.Outlined.Check, null, tint = FlowGreen)
                        }
                        HorizontalDivider(color = FlowBorder)
                    }
                }
            }
            OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sales_new_customer))
            }
        }
    }
}

@Composable
private fun FreeProductDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    val valid = name.trim().length >= 2 && (price.toSaleValue() ?: 0.0) > 0.0 && (quantity.toSaleValue() ?: 0.0) > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_free_product_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_product_name)) }, singleLine = true)
                OutlinedTextField(value = price, onValueChange = { price = it.moneyChars() }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_unit_price)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.moneyChars() }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_qty)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, price, quantity) }, enabled = valid) { Text(stringResource(R.string.sales_add_to_cart)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) } },
    )
}

// ---------------------------------------------------------------------------------------------
// Succès et facture
// ---------------------------------------------------------------------------------------------

@Composable
private fun SaleSuccessScreen(
    receipt: SaleReceipt,
    devise: String,
    onViewInvoice: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onNewSale: () -> Unit,
    onBackList: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CheckCircle, null, tint = FlowGreen, modifier = Modifier.size(56.dp))
                Text(stringResource(R.string.sales_success_title), color = FlowInk, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(stringResource(R.string.sales_success_description), color = FlowMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        item {
            Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FlowBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TotalLine(stringResource(R.string.sales_invoice_number), receipt.reference)
                    TotalLine(stringResource(R.string.sales_customer), receipt.clientName)
                    TotalLine(stringResource(R.string.sales_invoice_total), saleMoney(receipt.total, devise), strong = true)
                    TotalLine(stringResource(R.string.sales_payment_method), receipt.paymentMethod)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = FlowGreenSoft), border = BorderStroke(1.dp, FlowGreen.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (receipt.completed.contains("SALE")) SuccessLine(stringResource(R.string.sales_confirm_sale))
                    if (receipt.completed.contains("PAYMENT")) SuccessLine(stringResource(R.string.sales_confirm_payment))
                    if (receipt.completed.contains("STOCK")) SuccessLine(stringResource(R.string.sales_confirm_stock))
                    if (receipt.completed.contains("INVOICE")) SuccessLine(stringResource(R.string.sales_confirm_invoice))
                    if (receipt.completed.contains("FINANCE")) SuccessLine(stringResource(R.string.sales_confirm_finance))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onViewInvoice, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = FlowBlue)) {
                    Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.sales_view_invoice))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f).height(42.dp)) { Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.sales_print), fontSize = 11.sp) }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f).height(42.dp)) { Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.sales_share_invoice), fontSize = 11.sp) }
                }
                Button(onClick = onNewSale, modifier = Modifier.fillMaxWidth().height(42.dp)) { Text(stringResource(R.string.sales_new_sale)) }
                TextButton(onClick = onBackList, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sales_back_list), color = FlowMuted) }
            }
        }
    }
}

@Composable
private fun SuccessLine(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, null, tint = FlowGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = FlowInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoicePreviewScreen(
    receipt: SaleReceipt,
    devise: String,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onDownload: () -> Unit,
    onEmail: () -> Unit,
    onDuplicate: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sales_invoice_preview), color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, stringResource(R.string.sales_share_invoice), tint = FlowBlue) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            item { InvoicePaper(receipt, devise) }
            item { InvoiceOption(Icons.Outlined.Print, R.string.sales_print_invoice, R.string.sales_print_invoice_description, onPrint) }
            item { InvoiceOption(Icons.Outlined.Share, R.string.sales_share_invoice, R.string.sales_share_invoice_description, onShare) }
            item { InvoiceOption(Icons.Outlined.Download, R.string.sales_download_pdf, R.string.sales_download_pdf_description, onDownload) }
            item { InvoiceOption(Icons.Outlined.Email, R.string.sales_email_invoice, R.string.sales_email_invoice_description, onEmail) }
            item { InvoiceOption(Icons.Outlined.ContentCopy, R.string.sales_duplicate_sale, R.string.sales_duplicate_sale_description, onDuplicate) }
            item { InvoiceOption(Icons.Outlined.Cancel, R.string.sales_cancel_sale, R.string.sales_cancel_sale_description, onCancel, destructive = true) }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("MISSA BUSINESS", color = FlowInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text("360", color = FlowGreen, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.sales_invoice_customer).uppercase(), color = FlowInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (payload.paidAmount >= payload.total) stringResource(R.string.sales_status_paid) else stringResource(R.string.sales_status_pending), color = FlowGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = FlowBorder)
            TotalLine(stringResource(R.string.sales_invoice_number), receipt.reference)
            TotalLine(stringResource(R.string.sales_invoice_date), DateUtils.formatDateHeure(receipt.createdAt))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.sales_customer), color = FlowMuted, fontSize = 9.sp)
                Text(payload.clientName, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            HorizontalDivider(color = FlowBorder)
            Row { TableTitle(R.string.sales_product, Modifier.weight(1f), TextAlign.Start); TableTitle(R.string.sales_qty, Modifier.width(36.dp), TextAlign.End); TableTitle(R.string.sales_unit_price, Modifier.width(68.dp), TextAlign.End); TableTitle(R.string.sales_total, Modifier.width(72.dp), TextAlign.End) }
            payload.lines.forEach { line ->
                Row {
                    Text(line.name, modifier = Modifier.weight(1f), color = FlowInk, fontSize = 10.sp)
                    Text(line.quantity.saleQty(), modifier = Modifier.width(36.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End)
                    Text(saleMoney(line.unitPrice, devise), modifier = Modifier.width(68.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End)
                    Text(saleMoney(line.total, devise), modifier = Modifier.width(72.dp), color = FlowInk, fontSize = 10.sp, textAlign = TextAlign.End)
                }
            }
            HorizontalDivider(color = FlowBorder)
            TotalLine(stringResource(R.string.sales_subtotal), saleMoney(payload.subtotal, devise))
            TotalLine(stringResource(R.string.sales_discount), saleMoney(payload.discount, devise))
            TotalLine(stringResource(R.string.sales_delivery), saleMoney(payload.delivery, devise))
            TotalLine(stringResource(R.string.sales_tax_included, payload.taxRate.saleRate()), saleMoney(payload.taxAmount, devise))
            TotalLine(stringResource(R.string.sales_total), saleMoney(payload.total, devise), strong = true)
            TotalLine(stringResource(R.string.sales_paid_amount), saleMoney(payload.paidAmount, devise))
            payload.note?.let { Text(it, color = FlowMuted, fontSize = 10.sp) }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.sales_invoice_thanks), modifier = Modifier.fillMaxWidth(), color = FlowMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            Text("MISSA BUSINESS 360", modifier = Modifier.fillMaxWidth(), color = FlowBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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

@Composable
private fun SalesTopBarTitle(title: String) {
    Text(title, color = FlowInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun TableTitle(title: Int, modifier: Modifier, textAlign: TextAlign) {
    Text(stringResource(title), color = FlowMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = modifier, textAlign = textAlign)
}

@Composable
private fun FlowBottomBar(onNavigate: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            FlowNav(Icons.Outlined.Home, R.string.sales_home) { onNavigate(Routes.HOME) }
            FlowNav(Icons.Outlined.PointOfSale, R.string.sales_nav_sales, selected = true) { }
            FlowNav(Icons.Outlined.Inventory2, R.string.module_stock) { onNavigate(AppModule.STOCK.route) }
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

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

private fun errorMessage(error: SaleFormError): Int = when (error.code) {
    SaleErrorCode.CLIENT_REQUIRED -> R.string.sales_customer_required
    SaleErrorCode.EMPTY_CART -> R.string.sales_cart_required
    SaleErrorCode.INVALID_LINE -> R.string.sales_invalid_amount
    SaleErrorCode.STOCK_INSUFFICIENT -> R.string.sales_stock_insufficient
    SaleErrorCode.DISCOUNT_INVALID -> R.string.sales_invalid_amount
    SaleErrorCode.PAYMENT_INVALID -> R.string.sales_invalid_amount
    SaleErrorCode.PERMISSION_DENIED -> R.string.sales_permission_denied
    SaleErrorCode.READ_ONLY -> R.string.ops_read_only
    SaleErrorCode.NOT_DRAFT -> R.string.sales_invalid_amount
    SaleErrorCode.INTERNAL -> R.string.ops_error
}

private fun receiptFromSale(sale: com.missa.b360.core.data.entity.SaleEntity, devise: String): SaleReceipt {
    return SaleReceipt(
        recordId = sale.id,
        reference = sale.reference,
        payload = com.missa.b360.core.domain.model.SaleRecordPayload(
            clientId = sale.clientId,
            clientName = sale.clientName,
            lines = emptyList(),
            subtotal = sale.subtotalCents / 100.0,
            discount = sale.discountCents / 100.0,
            delivery = sale.deliveryCents / 100.0,
            taxRate = sale.taxRate,
            taxAmount = sale.taxAmountCents / 100.0,
            total = sale.totalCents / 100.0,
            paymentMethod = sale.paymentMethod,
            paidAmount = sale.paidCents / 100.0,
            totalCents = sale.totalCents,
            paidCents = sale.paidCents,
            remainingCents = sale.remainingCents,
            changeCents = sale.changeCents,
            isCredit = sale.isCredit,
        ),
        createdAt = sale.createdAt,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Context.shareInvoice(receipt: SaleReceipt, devise: String) {
    val text = "${getString(R.string.sales_invoice_customer)} ${receipt.reference}\n${receipt.clientName}\n${getString(R.string.sales_total)}: ${saleMoney(receipt.total, devise)}"
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.sales_share_subject)} ${receipt.reference}").putExtra(Intent.EXTRA_TEXT, text), getString(R.string.sales_share_invoice)))
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Context.emailInvoice(receipt: SaleReceipt, devise: String) {
    val body = "${getString(R.string.sales_invoice_customer)} ${receipt.reference}\n${getString(R.string.sales_total)}: ${saleMoney(receipt.total, devise)}"
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.sales_email_subject)} ${receipt.reference}").putExtra(Intent.EXTRA_TEXT, body), getString(R.string.sales_email_invoice)))
}

@OptIn(ExperimentalMaterial3Api::class)
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
    } finally {
        document.close()
    }
}.isSuccess

@OptIn(ExperimentalMaterial3Api::class)
private fun Context.printSaleReceipt(receipt: SaleReceipt, devise: String) {
    val manager = getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
    manager.print(
        "${getString(R.string.sales_receipt_name)} ${receipt.reference}",
        SalePdfAdapter(receipt, devise),
        null,
    )
}

/**
 * Adaptateur d'impression minimal : génère le PDF A4 de la facture la plus récente.
 * L'impression 80/58 mm reste possible via le réseau/adapter papier du service Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class SalePdfAdapter(private val receipt: SaleReceipt, private val devise: String) : android.print.PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes, cancellationSignal: android.os.CancellationSignal?, callback: android.print.LayoutResultCallback, extras: android.os.Bundle?) {
        callback.onLayoutFinished(
            android.print.PrintDocumentInfo.Builder(receipt.reference)
                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true,
        )
    }

    override fun onWrite(pages: Array<android.print.PageRange>, destination: android.os.ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal?, callback: android.print.WriteResultCallback) {
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(21, 84, 232); textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(16, 28, 67); textSize = 11f }
            canvas.drawText("MISSA BUSINESS 360", 45f, 64f, title)
            canvas.drawText(receipt.reference, 45f, 105f, body)
            canvas.drawText(DateUtils.formatDateHeure(receipt.createdAt), 45f, 126f, body)
            canvas.drawText(receipt.clientName, 45f, 147f, body)
            var y = 185f
            receipt.payload.lines.take(27).forEach { line ->
                canvas.drawText("${line.name.take(34)} × ${line.quantity.saleQty()}  ${saleMoney(line.total, devise)}", 45f, y, body)
                y += 22f
            }
            y += 10f
            canvas.drawText("Subtotal : ${saleMoney(receipt.payload.subtotal, devise)}", 45f, y, body)
            y += 20f
            canvas.drawText("Total : ${saleMoney(receipt.total, devise)}", 45f, y, title)
            y += 25f
            canvas.drawText("Payé : ${saleMoney(receipt.paidAmount, devise)}", 45f, y, body)
            y += 20f
            canvas.drawText(receipt.paymentMethod, 45f, y, body)
            document.finishPage(page)
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output -> document.writeTo(output) }
            callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message)
        } finally {
            document.close()
        }
    }
}

private fun Double.saleQty(): String = if (this % 1.0 == 0.0) toInt().toString() else DecimalFormat("0.##", DecimalFormatSymbols.getDefault()).format(this)
private fun Double.saleRate(): String = DecimalFormat("0.##", DecimalFormatSymbols.getDefault()).format(this)
private fun Long.toDoubleMoney(): Double = this / 100.0
private fun String.toSaleValue(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun String.moneyChars(): String = filter { it.isDigit() || it == ',' || it == '.' }
