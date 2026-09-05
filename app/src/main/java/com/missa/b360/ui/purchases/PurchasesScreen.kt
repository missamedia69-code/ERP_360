package com.missa.b360.ui.purchases

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.purchases.PurchasesViewModel
import com.missa.b360.ui.sales.saleMoney
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40

/**
 * Module Achat (spec §6) — listes des factures fournisseurs + formulaire UNE PAGE :
 * fournisseur, produits (catalogue), totals, paiement (réglé / passif).
 * La validation génère les **entrées de stock** et le passif est `total − réglé`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    viewModel: PurchasesViewModel = hiltViewModel(),
) {
    val purchases by viewModel.purchases.collectAsState(initial = emptyList())
    val purchaseUi by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState(initial = emptyList())
    val suppliers by viewModel.suppliers.collectAsState(initial = emptyList())
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var formVisible by rememberSaveable(openCreate) { mutableStateOf(openCreate) }
    var supplierSearch by remember { mutableStateOf("") }
    var supplierSheetVisible by remember { mutableStateOf(false) }
    var productSearch by rememberSaveable { mutableStateOf("") }
    var paymentPickerVisible by remember { mutableStateOf(false) }
    var paymentMethod by rememberSaveable { mutableStateOf("") }
    val fallbackPayment = stringResource(R.string.sales_cash)
    val selectedPayment = paymentMethod.ifBlank { paymentMethods.firstOrNull() ?: fallbackPayment }

    LaunchedEffect(paymentMethods) {
        if (paymentMethod.isBlank() && paymentMethods.isNotEmpty()) paymentMethod = paymentMethods.first()
    }
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is PurchasesViewModel.SaveResult.Saved -> {
                snackbar.showSnackbar(
                    context.getString(
                        if (result.isDraft) R.string.purchase_draft_saved else R.string.purchase_saved,
                        result.reference,
                    ),
                )
                formVisible = false
                viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.MissingSupplier -> {
                snackbar.showSnackbar(context.getString(R.string.purchase_supplier_required)); viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.EmptyCart -> {
                snackbar.showSnackbar(context.getString(R.string.purchase_lines_required)); viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.InvalidAmount -> {
                snackbar.showSnackbar(context.getString(R.string.purchase_invalid_amount)); viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only)); viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.FournisseurIntrouvable -> {
                snackbar.showSnackbar(context.getString(R.string.purchase_supplier_missing)); viewModel.clearSaveResult()
            }
            PurchasesViewModel.SaveResult.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error)); viewModel.clearSaveResult()
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!formVisible) {
            // --- LISTE DES FACTURES FOURNISSEURS ---
            Scaffold(
                containerColor = MissaCanvas,
                topBar = { MissaTopAppBar(title = stringResource(R.string.purchase_title), onBack = onBack) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.clearCart(); formVisible = true },
                        containerColor = BrandBlue,
                        contentColor = Color.White,
                    ) { Icon(Icons.Outlined.Add, stringResource(R.string.purchase_new)) }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (purchases.isEmpty()) {
                        item {
                            MissaEmptyState(
                                icon = Icons.Outlined.Handshake,
                                title = stringResource(R.string.purchase_list_empty),
                                action = {
                                    TextButton(onClick = { viewModel.clearCart(); formVisible = true }) {
                                        Text(stringResource(R.string.purchase_new))
                                    }
                                },
                            )
                        }
                    } else {
                        items(purchases, key = { it.id }) { record ->
                            PurchaseRecordRow(
                                record = record,
                                devise = devise,
                                onOpen = {
                                    if (record.status == OperationStatus.DRAFT.name &&
                                        viewModel.loadDraft(record, suppliers)
                                    ) {
                                        formVisible = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        } else {
            // --- FORMULAIRE (UNE PAGE) ---
            val total = purchaseUi.lines.sumOf { it.total }.coerceAtLeast(0.0)
            val paid = purchaseUi.paidInput.replace(',', '.').toDoubleOrNull() ?: total
            val passif = (total - paid).coerceAtLeast(0.0)
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
                            if (purchaseUi.editingRecordId == null) R.string.purchase_new else R.string.purchase_draft_edit,
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
                                onClick = { viewModel.save(selectedPayment, draft = false) },
                                enabled = purchaseUi.supplier != null && purchaseUi.lines.isNotEmpty() && !busy,
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
                                Text(stringResource(R.string.purchase_validate), fontWeight = FontWeight.Bold)
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
                    // --- FOURNISSEUR ---
                    FormCard(stringResource(R.string.purchase_supplier)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { supplierSheetVisible = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MissaSoftBlue,
                            border = BorderStroke(1.dp, MissaBorder),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Handshake, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        purchaseUi.supplier?.nom ?: stringResource(R.string.purchase_select_supplier),
                                        color = if (purchaseUi.supplier != null) MissaInk else MissaMuted,
                                        fontWeight = if (purchaseUi.supplier != null) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    purchaseUi.supplier?.let {
                                        Text(it.code, color = MissaMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text("›", color = MissaMuted, fontSize = 20.sp)
                            }
                        }
                    }

                    // --- PRODUITS ---
                    FormCard(stringResource(R.string.purchase_products)) {
                        OutlinedTextField(
                            value = productSearch,
                            onValueChange = { productSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.purchase_search_products)) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        )
                        Spacer(Modifier.height(8.dp))
                        if (visibleProducts.isEmpty()) {
                            Text(
                                stringResource(
                                    if (products.isEmpty()) R.string.purchase_catalog_empty else R.string.purchase_no_results,
                                ),
                                color = MissaMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            )
                        } else {
                            visibleProducts.take(8).forEach { product ->
                                PurchaseCatalogRow(
                                    product = product,
                                    inCartCount = purchaseUi.lines.count { it.productId == product.product.id },
                                    onAdd = { viewModel.addCatalogProduct(product) },
                                    devise = devise,
                                )
                                HorizontalDivider(color = MissaBorder)
                            }
                        }
                        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(vertical = 6.dp))
                        if (purchaseUi.lines.isEmpty()) {
                            Text(
                                stringResource(R.string.purchase_cart_empty),
                                color = MissaMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        } else {
                            purchaseUi.lines.forEach { line ->
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
                                    Text(line.quantity.saleQty(), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, color = MissaInk, fontSize = 12.sp)
                                    TextButton(onClick = { viewModel.changeQuantity(line.id, 1.0) }, contentPadding = PaddingValues(0.dp)) { Text("+", color = BrandBlue, fontSize = 15.sp) }
                                    Text(saleMoney(line.total, devise), modifier = Modifier.width(84.dp), textAlign = TextAlign.End, color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    IconButton(onClick = { viewModel.removeLine(line.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.purchase_remove_line), tint = Red40, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- TOTALS ---
                    FormCard(stringResource(R.string.purchase_totals)) {
                        PurchaseAmountLine(stringResource(R.string.purchase_subtotal), saleMoney(total, devise))
                        if (taxRate > 0.0) {
                            val taxAmount = total * taxRate / (100.0 + taxRate)
                            PurchaseAmountLine(
                                stringResource(R.string.purchase_tax_included, taxRate.saleRateText()),
                                saleMoney(taxAmount, devise),
                            )
                        }
                        HorizontalDivider(color = MissaBorder)
                        PurchaseAmountLine(stringResource(R.string.purchase_total), saleMoney(total, devise), strong = true)
                    }

                    // --- PAIEMENT ---
                    FormCard(stringResource(R.string.purchase_payment)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { paymentPickerVisible = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MissaSoftBlue,
                            border = BorderStroke(1.dp, MissaBorder),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.purchase_payment_method), modifier = Modifier.weight(1f), color = MissaMuted, fontSize = 12.sp)
                                Text(selectedPayment, color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                        OutlinedTextField(
                            value = purchaseUi.paidInput,
                            onValueChange = viewModel::updatePaid,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.purchase_paid_amount)) },
                            placeholder = { Text(stringResource(R.string.purchase_paid_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.purchase_supplier_balance), color = MissaMuted, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                saleMoney(passif, devise),
                                color = if (passif > 0.0) Red40 else Green60,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        OutlinedTextField(
                            value = purchaseUi.note,
                            onValueChange = viewModel::updateNote,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.purchase_note)) },
                            minLines = 1,
                            maxLines = 2,
                        )
                        TextButton(onClick = { viewModel.save(selectedPayment, draft = true) }, enabled = !busy && purchaseUi.supplier != null && purchaseUi.lines.isNotEmpty()) {
                            Text(stringResource(R.string.purchase_save_draft))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // --- SÉLECTEUR FOURNISSEUR (recherche, spec §40) ---
        if (supplierSheetVisible) {
            SupplierPickerSheet(
                suppliers = suppliers,
                search = supplierSearch,
                onSearchChange = { supplierSearch = it },
                onDismiss = { supplierSheetVisible = false },
                onSelect = {
                    viewModel.selectSupplier(it)
                    supplierSheetVisible = false
                    supplierSearch = ""
                },
            )
        }

        // --- SÉLECTEUR MOYEN DE PAIEMENT ---
        if (paymentPickerVisible) {
            AlertDialog(
                onDismissRequest = { paymentPickerVisible = false },
                title = { Text(stringResource(R.string.purchase_payment_method)) },
                text = {
                    Column {
                        paymentMethods.ifEmpty { listOf(fallbackPayment) }.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    paymentMethod = method
                                    paymentPickerVisible = false
                                }.padding(vertical = 10.dp),
                            ) {
                                Text(method, color = MissaInk, modifier = Modifier.weight(1f))
                                if (method == selectedPayment) Icon(Icons.Outlined.Add, null, tint = BrandBlue)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { paymentPickerVisible = false }) { Text(stringResource(R.string.ops_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
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
private fun PurchaseAmountLine(label: String, value: String, strong: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (strong) MissaInk else MissaMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = BrandBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun PurchaseRecordRow(record: OperationRecordEntity, devise: String, onOpen: () -> Unit) {
    val payload = PurchaseRecordCodec.decode(record.notes)
    val isDraft = record.status == OperationStatus.DRAFT.name
    val isCancelled = record.status == OperationStatus.CANCELLED.name
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDraft) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.reference, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(
                        "${payload?.supplierName ?: record.counterpart.orEmpty()} · ${DateUtils.formatDateHeure(record.createdAt)}",
                        color = MissaMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (record.amount != null) Text(saleMoney(record.amount, devise), color = BrandBlue, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.padding(top = 6.dp)) {
                if (isDraft) {
                    Text(stringResource(R.string.ops_status_draft), color = MissaMuted, style = MaterialTheme.typography.bodySmall)
                } else if (isCancelled) {
                    Text(stringResource(R.string.ops_status_cancelled), color = Red40, style = MaterialTheme.typography.bodySmall)
                } else {
                    payload?.let { p ->
                        val passif = (p.total - p.paidAmount).coerceAtLeast(0.0)
                        if (passif > 0.0) {
                            Text(
                                stringResource(R.string.purchase_balance, saleMoney(passif, devise)),
                                color = Red40,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(stringResource(R.string.purchase_settled), color = Green60, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseCatalogRow(
    product: ProductWithStock,
    inCartCount: Int,
    onAdd: () -> Unit,
    devise: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(7.dp), color = MissaSoftBlue) {
            Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.product.nom, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.purchase_stock_available, product.stock.saleQty()),
                color = if (product.stock <= 0.0) Red40 else MissaMuted,
                fontSize = 9.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            product.product.prixAchat?.let {
                Text(saleMoney(it, devise), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (inCartCount > 0) {
                Text(stringResource(R.string.purchase_in_cart, inCartCount), color = BrandBlue, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            contentPadding = PaddingValues(0.dp),
        ) { Icon(Icons.Outlined.Add, stringResource(R.string.purchase_add_product), tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierPickerSheet(
    suppliers: List<FournisseurEntity>,
    search: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (FournisseurEntity) -> Unit,
) {
    val visible = suppliers.filter {
        search.isBlank() || it.nom.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(stringResource(R.string.purchase_select_supplier), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                Text(stringResource(R.string.purchase_supplier_empty), color = MissaMuted, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(visible, key = { it.id }) { supplier ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(supplier) }.padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(supplier.nom, color = MissaInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(supplier.code, color = MissaMuted, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = MissaBorder)
                    }
                }
            }
        }
    }
}

private fun Double.saleQty(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)

private fun Double.saleRateText(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)
