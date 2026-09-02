package com.missa.b360.ui.sales

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleTotals
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private val SaleBlue = Color(0xFF1554E8)
private val SaleBlueDark = Color(0xFF073DBB)
private val SaleBlueSoft = Color(0xFFF0F5FF)
private val SaleGreen = Color(0xFF16803C)
private val SaleGreenSoft = Color(0xFFEAF8EF)
private val SaleInk = Color(0xFF101C43)
private val SaleMuted = Color(0xFF65718F)
private val SaleDivider = Color(0xFFE2E7F2)
private val SaleBackground = Color(0xFFF8F9FD)
private val SaleRed = Color(0xFFEC5A67)

/**
 * Écran de caisse Vente. Il reprend la composition de la maquette (bandeau bleu, panier,
 * résumé/paiement et actions fixes), mais n'affiche jamais de produits de démonstration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacySalesScreen(
    onNavigate: (String) -> Unit,
    onOpenClientCreate: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val taxRate by viewModel.taxRate.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val saveResult by viewModel.saveResult.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var customerPickerVisible by remember { mutableStateOf(false) }
    var freeProductVisible by remember { mutableStateOf(false) }
    var paymentPickerVisible by remember { mutableStateOf(false) }
    var historyVisible by remember { mutableStateOf(false) }
    var clearConfirmationVisible by remember { mutableStateOf(false) }
    var scannerMessageRequested by remember { mutableStateOf(false) }
    var productSearch by rememberSaveable { mutableStateOf("") }
    var selectedPayment by rememberSaveable { mutableStateOf("") }

    val barcodeScanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scannedValue = result.data?.getStringExtra("SCAN_RESULT").orEmpty()
        if (scannedValue.isBlank()) scannerMessageRequested = true else productSearch = scannedValue
    }
    val fallbackPayment = stringResource(R.string.sales_cash)
    val currentPayment = selectedPayment.ifBlank { paymentMethods.firstOrNull() ?: fallbackPayment }
    val totals = state.totals(taxRate)
    val outstanding = outstandingBalance(history, state.selectedClient?.id)

    LaunchedEffect(paymentMethods) {
        if (selectedPayment.isBlank() && paymentMethods.isNotEmpty()) {
            selectedPayment = paymentMethods.first()
        }
    }
    LaunchedEffect(scannerMessageRequested) {
        if (scannerMessageRequested) {
            snackbar.showSnackbar(context.getString(R.string.sales_scanner_unavailable))
            scannerMessageRequested = false
        }
    }
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is SalesViewModel.SaveResult.Saved -> {
                snackbar.showSnackbar(
                    context.getString(
                        if (result.shouldPrint) R.string.sales_saved_and_printing else R.string.sales_draft_saved,
                        result.receipt.reference,
                    ),
                )
                if (result.shouldPrint) context.printSaleReceipt(result.receipt, devise)
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.MissingClient -> {
                snackbar.showSnackbar(context.getString(R.string.sales_customer_required))
                viewModel.clearSaveResult()
            }
            SalesViewModel.SaveResult.EmptyCart -> {
                snackbar.showSnackbar(context.getString(R.string.sales_cart_required))
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
            SalesViewModel.SaveResult.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error))
                viewModel.clearSaveResult()
            }
            null -> Unit
        }
    }

    Scaffold(
        containerColor = SaleBackground,
        topBar = {
            SalesTopBar(
                onMenu = { onNavigate(Routes.HOME) },
                onNotifications = { onNavigate(Routes.NOTIFICATIONS) },
                onMore = { historyVisible = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            SalesBottomNavigation(
                onNavigate = onNavigate,
                onMore = { historyVisible = true },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SalesHero(
                    onHistory = { historyVisible = true },
                )
            }
            item {
                CustomerCard(
                    customer = state.selectedClient,
                    onSelect = { customerPickerVisible = true },
                    onNewCustomer = onOpenClientCreate,
                )
            }
            item {
                ProductSearch(
                    query = productSearch,
                    onQueryChange = { productSearch = it },
                    onFreeProduct = { freeProductVisible = true },
                    onScanner = {
                        val intent = Intent("com.google.zxing.client.android.SCAN")
                        if (intent.resolveActivity(context.packageManager) != null) {
                            barcodeScanner.launch(intent)
                        } else {
                            scannerMessageRequested = true
                        }
                    },
                )
            }
            item {
                CartCard(
                    lines = state.lines.filter { it.name.contains(productSearch, ignoreCase = true) },
                    hasCartItems = state.lines.isNotEmpty(),
                    devise = devise,
                    note = state.note,
                    onNoteChange = viewModel::updateNote,
                    onQuantityChange = viewModel::changeQuantity,
                    onRemove = viewModel::removeLine,
                    onClear = { clearConfirmationVisible = true },
                    onAddProduct = { freeProductVisible = true },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryCard(
                        totals = totals,
                        taxRate = taxRate,
                        devise = devise,
                        discountInput = state.discountInput,
                        deliveryInput = state.deliveryInput,
                        onDiscountChange = viewModel::updateDiscount,
                        onDeliveryChange = viewModel::updateDelivery,
                        modifier = Modifier.weight(1f),
                    )
                    PaymentCard(
                        paymentMethod = currentPayment,
                        paidInput = state.paidInput,
                        total = totals.total,
                        devise = devise,
                        onPaymentClick = { paymentPickerVisible = true },
                        onPaidChange = viewModel::updatePaid,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                CustomerAccountCard(
                    customer = state.selectedClient,
                    outstanding = outstanding,
                    devise = devise,
                    modifier = Modifier.padding(horizontal = 15.dp),
                )
            }
            item {
                SaleActions(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    onSaveDraft = { viewModel.save(currentPayment, draft = true) },
                    onSaveAndPrint = { viewModel.save(currentPayment, draft = false) },
                )
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
            onNewCustomer = {
                customerPickerVisible = false
                onOpenClientCreate()
            },
            onDismiss = { customerPickerVisible = false },
        )
    }
    if (freeProductVisible) {
        FreeProductDialog(
            onDismiss = { freeProductVisible = false },
            onAdd = { name, unitPrice, quantity ->
                if (viewModel.addFreeProduct(name, unitPrice, quantity)) {
                    productSearch = ""
                    freeProductVisible = false
                }
            },
        )
    }
    if (paymentPickerVisible) {
        PaymentPickerSheet(
            selected = currentPayment,
            methods = paymentMethods.ifEmpty { listOf(fallbackPayment) },
            onSelect = {
                selectedPayment = it
                paymentPickerVisible = false
            },
            onDismiss = { paymentPickerVisible = false },
        )
    }
    if (historyVisible) {
        SalesHistorySheet(
            records = history,
            devise = devise,
            onLoadDraft = { record ->
                if (viewModel.loadDraft(record, clients)) historyVisible = false
            },
            onDismiss = { historyVisible = false },
        )
    }
    if (clearConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { clearConfirmationVisible = false },
            title = { Text(stringResource(R.string.sales_clear_title)) },
            text = { Text(stringResource(R.string.sales_clear_message)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearCart()
                    clearConfirmationVisible = false
                }) { Text(stringResource(R.string.sales_clear_cart)) }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmationVisible = false }) {
                    Text(stringResource(R.string.ops_cancel))
                }
            },
        )
    }
}

@Composable
private fun SalesTopBar(
    onMenu: () -> Unit,
    onNotifications: () -> Unit,
    onMore: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, stringResource(R.string.drawer_admin), tint = SaleInk)
            }
            BrandLockup(modifier = Modifier.weight(1f))
            IconButton(onClick = onNotifications) {
                Icon(Icons.Outlined.Notifications, stringResource(R.string.notifications), tint = SaleInk)
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreHoriz, stringResource(R.string.more_modules), tint = SaleInk)
            }
        }
    }
}

@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = SaleBlueSoft) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = SaleBlue,
                modifier = Modifier.padding(7.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
            Text("MISSA", color = SaleInk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BUSINESS", color = SaleInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.width(3.dp))
                Text("360", color = Color(0xFF55B933), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SalesHero(onHistory: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(SaleBlue, SaleBlueDark)))
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column {
            Text(
                stringResource(R.string.sales_new_sale),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.sales_create_invoice),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
            )
        }
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier.align(Alignment.CenterEnd),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Icon(Icons.Outlined.History, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.sales_history), fontSize = 13.sp)
        }
    }
}

@Composable
private fun CustomerCard(
    customer: ClientEntity?,
    onSelect: () -> Unit,
    onNewCustomer: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, SaleDivider),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = SaleBlueSoft) {
                Icon(
                    Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = SaleBlue,
                    modifier = Modifier.padding(13.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect),
            ) {
                Text(stringResource(R.string.sales_customer), color = SaleMuted, fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        customer?.nom ?: stringResource(R.string.sales_select_customer),
                        color = SaleInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Outlined.ArrowDropDown, null, tint = SaleInk)
                }
            }
            OutlinedButton(
                onClick = onNewCustomer,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                border = BorderStroke(1.dp, SaleBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaleBlue),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.sales_new_customer), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProductSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onFreeProduct: () -> Unit,
    onScanner: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = {
                Text(
                    stringResource(R.string.sales_search_product),
                    color = SaleMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = SaleMuted) },
            trailingIcon = {
                IconButton(onClick = onScanner) {
                    Icon(Icons.Outlined.Search, stringResource(R.string.sales_scan), tint = SaleMuted)
                }
            },
            shape = RoundedCornerShape(11.dp),
        )
        OutlinedButton(
            onClick = onFreeProduct,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SaleBlue),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaleBlue),
        ) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.sales_free_product), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CartCard(
    lines: List<SaleLine>,
    hasCartItems: Boolean,
    devise: String,
    note: String,
    onNoteChange: (String) -> Unit,
    onQuantityChange: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onAddProduct: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SaleDivider),
    ) {
        Column {
            if (lines.isEmpty()) {
                EmptyCart(onAddProduct = onAddProduct, isSearchEmpty = hasCartItems)
            } else {
                CartHeader()
                lines.forEach { line ->
                    CartLine(
                        line = line,
                        devise = devise,
                        onQuantityChange = { onQuantityChange(line.id, it) },
                        onRemove = { onRemove(line.id) },
                    )
                    HorizontalDivider(color = SaleDivider)
                }
            }
            if (lines.isNotEmpty()) {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    label = { Text(stringResource(R.string.sales_add_note)) },
                    leadingIcon = { Icon(Icons.Outlined.ReceiptLong, null) },
                    minLines = 1,
                    maxLines = 2,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onClear) {
                        Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.sales_clear_cart))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCart(onAddProduct: () -> Unit, isSearchEmpty: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = SaleBlueSoft) {
            Icon(Icons.Outlined.ShoppingBasket, null, tint = SaleBlue, modifier = Modifier.padding(12.dp))
        }
        Text(
            stringResource(if (isSearchEmpty) R.string.sales_no_product else R.string.sales_empty_cart),
            color = SaleInk,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(if (isSearchEmpty) R.string.sales_no_product_description else R.string.sales_empty_cart_description),
            color = SaleMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onAddProduct) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.sales_free_product))
        }
    }
}

@Composable
private fun CartHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFD))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.sales_product), modifier = Modifier.weight(1f), color = SaleMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.sales_qty), modifier = Modifier.width(82.dp), color = SaleMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(R.string.sales_unit_price), modifier = Modifier.width(70.dp), color = SaleMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        Text(stringResource(R.string.sales_total), modifier = Modifier.width(68.dp), color = SaleMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun CartLine(
    line: SaleLine,
    devise: String,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(37.dp), shape = RoundedCornerShape(8.dp), color = SaleBlueSoft) {
                Icon(Icons.Outlined.Inventory2, null, tint = SaleBlue, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(line.name, color = SaleInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.sales_free_item), color = SaleBlue, fontSize = 10.sp)
            }
        }
        QuantitySelector(quantity = line.quantity, onChange = onQuantityChange, modifier = Modifier.width(82.dp))
        Text(saleMoney(line.unitPrice, devise), modifier = Modifier.width(70.dp), color = SaleInk, fontSize = 10.sp, textAlign = TextAlign.End, maxLines = 1)
        Text(saleMoney(line.total, devise), modifier = Modifier.width(68.dp), color = SaleInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1)
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_remove_item), tint = SaleRed, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun QuantitySelector(quantity: Double, onChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        QuantityButton("−", onClick = { onChange(-1.0) })
        Text(
            quantity.saleQuantity(),
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
            color = SaleInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        QuantityButton("+", onClick = { onChange(1.0) })
    }
}

@Composable
private fun QuantityButton(value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(25.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SaleDivider),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value, color = SaleBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryCard(
    totals: SaleTotals,
    taxRate: Double,
    devise: String,
    discountInput: String,
    deliveryInput: String,
    onDiscountChange: (String) -> Unit,
    onDeliveryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SaleDivider),
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.sales_summary), color = SaleInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            SummaryLine(stringResource(R.string.sales_subtotal), saleMoney(totals.subtotal, devise))
            CompactAmountInput(
                label = stringResource(R.string.sales_discount),
                prefix = "−",
                value = discountInput,
                devise = devise,
                onValueChange = onDiscountChange,
            )
            CompactAmountInput(
                label = stringResource(R.string.sales_delivery),
                prefix = "+",
                value = deliveryInput,
                devise = devise,
                onValueChange = onDeliveryChange,
            )
            HorizontalDivider(color = SaleDivider)
            SummaryLine(
                stringResource(R.string.sales_total).uppercase(),
                saleMoney(totals.total, devise),
                emphasized = true,
            )
            SummaryLine(
                stringResource(R.string.sales_tax_included, taxRate.saleRate()),
                saleMoney(totals.taxAmount, devise),
                valueSize = 11.sp,
            )
        }
    }
}

@Composable
private fun CompactAmountInput(
    label: String,
    prefix: String,
    value: String,
    devise: String,
    onValueChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = SaleMuted, fontSize = 10.sp)
        Text(prefix, color = SaleMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(55.dp)
                .height(40.dp)
                .padding(start = 5.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Text(devise, modifier = Modifier.padding(start = 4.dp), color = SaleMuted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasized: Boolean = false, valueSize: androidx.compose.ui.unit.TextUnit = 12.sp) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = if (emphasized) SaleInk else SaleMuted, fontSize = if (emphasized) 12.sp else 10.sp, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (emphasized) SaleBlue else SaleInk, fontSize = if (emphasized) 19.sp else valueSize, fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun PaymentCard(
    paymentMethod: String,
    paidInput: String,
    total: Double,
    devise: String,
    onPaymentClick: () -> Unit,
    onPaidChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paid = paidInput.toSaleAmountOrNull() ?: total
    val remaining = (total - paid).coerceAtLeast(0.0)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SaleDivider),
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.sales_payment), color = SaleInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(stringResource(R.string.sales_payment_method), color = SaleMuted, fontSize = 10.sp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPaymentClick),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, SaleDivider),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PointOfSale, null, tint = SaleGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(paymentMethod, modifier = Modifier.weight(1f), color = SaleInk, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.ArrowDropDown, null, tint = SaleInk)
                }
            }
            Text(stringResource(R.string.sales_paid_amount), color = SaleMuted, fontSize = 10.sp)
            OutlinedTextField(
                value = paidInput,
                onValueChange = onPaidChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(saleMoney(total, devise), fontSize = 11.sp) },
                suffix = { Text(devise, color = SaleMuted, fontSize = 10.sp) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
                color = SaleGreenSoft,
                border = BorderStroke(1.dp, SaleGreen.copy(alpha = 0.55f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = SaleGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sales_remaining), color = SaleGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(saleMoney(remaining, devise), color = SaleGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CustomerAccountCard(customer: ClientEntity?, outstanding: Double, devise: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SaleBlueSoft,
        border = BorderStroke(1.dp, Color(0xFFD9E4FF)),
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = Color.White) {
                Icon(Icons.Outlined.StarOutline, null, tint = SaleBlue, modifier = Modifier.padding(13.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sales_loyalty_points), color = SaleInk, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (customer == null) stringResource(R.string.sales_select_customer) else stringResource(R.string.sales_points_pending),
                    color = SaleBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.sales_points_description), color = SaleMuted, fontSize = 9.sp)
            }
            Surface(modifier = Modifier.width(1.dp).height(54.dp), color = Color(0xFFD9E4FF)) {}
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sales_current_balance), color = SaleInk, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                Text(saleMoney(outstanding, devise), color = SaleBlue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                Text(
                    if (outstanding > 0.0) stringResource(R.string.sales_amount_due) else stringResource(R.string.sales_no_amount_due),
                    color = SaleMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SaleActions(modifier: Modifier = Modifier, onSaveDraft: () -> Unit, onSaveAndPrint: () -> Unit) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onSaveDraft,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SaleBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SaleBlue),
        ) {
            Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.sales_save_draft), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onSaveAndPrint,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaleBlue),
        ) {
            Icon(Icons.Outlined.Print, null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.sales_save_print), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SalesBottomNavigation(onNavigate: (String) -> Unit, onMore: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            SalesNavItem(Icons.Outlined.Home, R.string.sales_home, false) { onNavigate(Routes.HOME) }
            SalesNavItem(Icons.Outlined.ShoppingCart, R.string.sales_nav_sales, true) { }
            SalesNavItem(Icons.Outlined.ShoppingBasket, R.string.module_achats, false) { onNavigate(AppModule.ACHATS.route) }
            SalesNavItem(Icons.Outlined.Inventory2, R.string.module_stock, false) { onNavigate(AppModule.STOCK.route) }
            SalesNavItem(Icons.Outlined.PersonOutline, R.string.module_clients, false) { onNavigate(AppModule.CLIENTS.route) }
            SalesNavItem(Icons.Outlined.MoreHoriz, R.string.more_modules, false, onMore)
        }
    }
}

@Composable
private fun SalesNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = 45.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, stringResource(label), tint = if (selected) SaleBlue else SaleMuted, modifier = Modifier.size(23.dp))
        Text(stringResource(label), color = if (selected) SaleBlue else SaleMuted, fontSize = 9.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerPickerSheet(
    clients: List<ClientEntity>,
    selectedId: Long?,
    onSelect: (ClientEntity) -> Unit,
    onNewCustomer: () -> Unit,
    onDismiss: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = clients.filter { it.nom.contains(search, ignoreCase = true) || it.telephone.contains(search) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.sales_select_customer), color = SaleInk, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                label = { Text(stringResource(R.string.clients_recherche)) },
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text(stringResource(R.string.sales_no_customer), color = SaleMuted, modifier = Modifier.padding(vertical = 14.dp))
            } else {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(filtered, key = { it.id }) { client ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(client) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(modifier = Modifier.size(37.dp), shape = CircleShape, color = SaleBlueSoft) {
                                Icon(Icons.Outlined.PersonOutline, null, tint = SaleBlue, modifier = Modifier.padding(9.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.nom, color = SaleInk, fontWeight = FontWeight.SemiBold)
                                Text(client.telephone, color = SaleMuted, fontSize = 11.sp)
                            }
                            if (client.id == selectedId) Icon(Icons.Outlined.Check, null, tint = SaleGreen)
                        }
                        HorizontalDivider(color = SaleDivider)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNewCustomer, modifier = Modifier.fillMaxWidth()) {
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
    val isValid = name.trim().length >= 2 && (price.toSaleAmountOrNull() ?: 0.0) > 0.0 && (quantity.toSaleAmountOrNull() ?: 0.0) > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_free_product_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sales_product_name)) }, singleLine = true)
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filterMoneyInput() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_unit_price)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filterMoneyInput() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sales_qty)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, price, quantity) }, enabled = isValid) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.sales_add_to_cart))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentPickerSheet(selected: String, methods: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.sales_payment_method), color = SaleInk, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            methods.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(method) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PointOfSale, null, tint = SaleGreen)
                    Spacer(Modifier.width(12.dp))
                    Text(method, modifier = Modifier.weight(1f), color = SaleInk, fontWeight = FontWeight.SemiBold)
                    if (method == selected) Icon(Icons.Outlined.Check, null, tint = SaleGreen)
                }
                HorizontalDivider(color = SaleDivider)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesHistorySheet(
    records: List<OperationRecordEntity>,
    devise: String,
    onLoadDraft: (OperationRecordEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.sales_history), color = SaleInk, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.sales_history_description), color = SaleMuted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            if (records.isEmpty()) {
                Text(stringResource(R.string.sales_history_empty), color = SaleMuted, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(records, key = { it.id }) { record ->
                        val isDraft = record.status == "DRAFT"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isDraft) { onLoadDraft(record) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(modifier = Modifier.size(39.dp), shape = RoundedCornerShape(9.dp), color = SaleBlueSoft) {
                                Icon(Icons.Outlined.ReceiptLong, null, tint = SaleBlue, modifier = Modifier.padding(9.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.counterpart ?: record.title, color = SaleInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    stringResource(
                                        R.string.ops_record_detail,
                                        record.reference,
                                        DateUtils.formatDateHeure(record.createdAt),
                                    ),
                                    color = SaleMuted,
                                    fontSize = 10.sp,
                                )
                                if (isDraft) {
                                    Text(stringResource(R.string.ops_status_draft), color = SaleBlue, fontSize = 10.sp)
                                }
                            }
                            Text(saleMoney(record.amount ?: 0.0, devise), color = SaleBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = SaleDivider)
                    }
                }
            }
        }
    }
}

internal fun Context.printSaleReceipt(receipt: SaleReceipt, devise: String) {
    val printManager = getSystemService(PrintManager::class.java) ?: return
    printManager.print(
        "${getString(R.string.sales_receipt_name)}-${receipt.reference}",
        SalePrintAdapter(receipt, devise),
        null,
    )
}

private class SalePrintAdapter(
    private val receipt: SaleReceipt,
    private val devise: String,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("${receipt.reference}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val document = PdfDocument()
        try {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(21, 84, 232)
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(16, 28, 67)
                textSize = 13f
            }
            canvas.drawText("MISSA BUSINESS 360", 48f, 68f, titlePaint)
            canvas.drawText(receipt.reference, 48f, 108f, bodyPaint)
            canvas.drawText(DateUtils.formatDateHeure(receipt.createdAt), 48f, 132f, bodyPaint)
            canvas.drawText(receipt.clientName, 48f, 156f, bodyPaint)
            var y = 195f
            receipt.payload.lines.take(22).forEach { line ->
                canvas.drawText("${line.name.take(30)} × ${line.quantity.saleQuantity()}  ${saleMoney(line.total, devise)}", 48f, y, bodyPaint)
                y += 23f
            }
            y += 15f
            canvas.drawText("TVA ${receipt.payload.taxRate.saleRate()}% : ${saleMoney(receipt.payload.taxAmount, devise)}", 48f, y, bodyPaint)
            y += 27f
            canvas.drawText("Total : ${saleMoney(receipt.total, devise)}", 48f, y, titlePaint)
            y += 27f
            canvas.drawText("Payé : ${saleMoney(receipt.paidAmount, devise)}", 48f, y, bodyPaint)
            y += 25f
            canvas.drawText(receipt.paymentMethod, 48f, y, bodyPaint)
            document.finishPage(page)
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output -> document.writeTo(output) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message)
        } finally {
            document.close()
        }
    }
}

internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}

private fun Double.saleQuantity(): String = if (this % 1.0 == 0.0) toInt().toString() else DecimalFormat("0.##").format(this)
private fun Double.saleRate(): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.getDefault())).format(this)
private fun String.toSaleAmountOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun String.filterMoneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }
