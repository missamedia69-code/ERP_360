package com.missa.b360.ui.stock

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockInventoryEntity
import com.missa.b360.core.data.entity.StockInventoryStatus
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockStatus
import com.missa.b360.core.data.entity.StockWarehouseEntity
import com.missa.b360.core.domain.usecase.StockUseCases
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StockTab { DASHBOARD, PRODUCTS, MOVEMENTS, REPORTS, INVENTORY }
private enum class StockStep { TAB, PRODUCT_DETAIL, PRODUCT_FORM, MOVEMENT_FORM, INVENTORY_FORM }
private enum class StockFormKind { ENTRY, EXIT, TRANSFER, ADJUSTMENT, PRODUCT, INVENTORY }

/**
 * Module Stock — dix écrans conformes à la maquette de référence :
 * tableau de bord, produits, détail produit, entrée, sortie, transfert, ajustement,
 * inventaire, mouvements et rapports. Toutes les données sont locales et persistantes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    initialAction: StockOpenAction? = null,
    viewModel: StockViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val warehouses by viewModel.warehouses.collectAsState(initial = emptyList())
    val movements by viewModel.movements.collectAsState(initial = emptyList())
    val inventories by viewModel.inventories.collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var tabName by rememberSaveable { mutableStateOf(StockTab.DASHBOARD.name) }
    var stepName by rememberSaveable { mutableStateOf(StockStep.TAB.name) }
    var selectedProductId by rememberSaveable { mutableStateOf(-1L) }
    var formKindName by rememberSaveable { mutableStateOf(StockFormKind.ENTRY.name) }
    var showAddMenu by remember { mutableStateOf(false) }
    var reportLines by remember { mutableStateOf<List<ReportLine>?>(null) }
    var reportTitle by remember { mutableStateOf("") }

    val tab = StockTab.valueOf(tabName)
    val step = StockStep.valueOf(stepName)
    val formKind = StockFormKind.valueOf(formKindName)

    fun backFromStep() {
        stepName = StockStep.TAB.name
    }

    LaunchedEffect(openCreate, initialAction) {
        val action = initialAction
        if (openCreate && action != null) {
            formKindName = when (action) {
                StockOpenAction.ENTRY -> StockFormKind.ENTRY.name
                StockOpenAction.EXIT -> StockFormKind.EXIT.name
                StockOpenAction.TRANSFER -> StockFormKind.TRANSFER.name
                StockOpenAction.ADJUSTMENT -> StockFormKind.ADJUSTMENT.name
                StockOpenAction.PRODUCT -> StockFormKind.PRODUCT.name
            }
            stepName = StockStep.MOVEMENT_FORM.name
            if (action == StockOpenAction.PRODUCT) stepName = StockStep.PRODUCT_FORM.name
        }
    }

    LaunchedEffect(result) {
        when (val r = result) {
            is StockUiResult.Success -> {
                val message = when (formKind) {
                    StockFormKind.PRODUCT -> context.getString(R.string.stk_product_created, r.reference)
                    StockFormKind.INVENTORY -> context.getString(R.string.stk_inventory_created, r.reference)
                    else -> context.getString(R.string.stk_movement_created, r.reference)
                }
                snackbar.showSnackbar(message)
                tabName = when (formKind) {
                    StockFormKind.PRODUCT -> StockTab.PRODUCTS.name
                    StockFormKind.INVENTORY -> StockTab.INVENTORY.name
                    else -> StockTab.MOVEMENTS.name
                }
                stepName = StockStep.TAB.name
                viewModel.clearResult()
            }
            StockUiResult.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.stk_read_only))
                viewModel.clearResult()
            }
            StockUiResult.Invalid -> {
                snackbar.showSnackbar(context.getString(R.string.stk_invalid))
                viewModel.clearResult()
            }
            StockUiResult.Missing -> {
                snackbar.showSnackbar(context.getString(R.string.stk_missing))
                viewModel.clearResult()
            }
            StockUiResult.Error -> {
                snackbar.showSnackbar(context.getString(R.string.stk_error))
                viewModel.clearResult()
            }
            null -> Unit
        }
    }

    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = when (step) {
                    StockStep.PRODUCT_DETAIL -> stringResource(R.string.stk_detail_product)
                    StockStep.PRODUCT_FORM -> stringResource(R.string.stk_new_product)
                    StockStep.MOVEMENT_FORM -> stringResource(formKind.titleRes())
                    StockStep.INVENTORY_FORM -> stringResource(R.string.stk_new_inventory)
                    StockStep.TAB -> stringResource(tab.titleRes())
                },
                onBack = {
                    if (step == StockStep.TAB) onBack() else backFromStep()
                },
                actions = if (step == StockStep.TAB && tab == StockTab.MOVEMENTS) {
                    {
                        IconButton(onClick = {
                            exportMovementsCsv(context, movements, products, devise)
                        }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.stk_export))
                        }
                    }
                } else {
                    {}
                },
            )
        },
        bottomBar = {
            if (step == StockStep.TAB) {
                StockBottomBar(
                    selected = tab,
                    onSelect = { tabName = it.name },
                    onAdd = { showAddMenu = true },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (step) {
                StockStep.TAB -> when (tab) {
                    StockTab.DASHBOARD -> StockDashboardContent(
                        products = products,
                        categories = categories,
                        movements = movements,
                        devise = devise,
                        onOpenProduct = { id ->
                            selectedProductId = id
                            stepName = StockStep.PRODUCT_DETAIL.name
                        },
                        onAdd = { showAddMenu = true },
                        onOpenMovements = { tabName = StockTab.MOVEMENTS.name },
                    )

                    StockTab.PRODUCTS -> StockProductsContent(
                        products = products,
                        categories = categories,
                        devise = devise,
                        onOpenProduct = { id ->
                            selectedProductId = id
                            stepName = StockStep.PRODUCT_DETAIL.name
                        },
                        onAddProduct = { formKindName = StockFormKind.PRODUCT.name; stepName = StockStep.PRODUCT_FORM.name },
                    )

                    StockTab.MOVEMENTS -> StockMovementsContent(
                        movements = movements,
                        products = products,
                        warehouses = warehouses,
                        devise = devise,
                        onAdd = { showAddMenu = true },
                    )

                    StockTab.REPORTS -> StockReportsContent(
                        products = products,
                        categories = categories,
                        movements = movements,
                        devise = devise,
                        onShowReport = { title, lines ->
                            reportTitle = title
                            reportLines = lines
                        },
                    )

                    StockTab.INVENTORY -> StockInventoryContent(
                        inventories = inventories,
                        onNewInventory = { formKindName = StockFormKind.INVENTORY.name; stepName = StockStep.INVENTORY_FORM.name },
                        onValidate = viewModel::validateInventory,
                        onComplete = viewModel::completeInventory,
                    )
                }

                StockStep.PRODUCT_DETAIL -> StockProductDetailContent(
                    productId = selectedProductId,
                    products = products,
                    categories = categories,
                    warehouses = warehouses,
                    movements = movements,
                    devise = devise,
                    onBack = { backFromStep() },
                    onAddStock = {
                        selectedProductId = selectedProductId
                        formKindName = StockFormKind.ENTRY.name
                        stepName = StockStep.MOVEMENT_FORM.name
                    },
                    onTransfer = {
                        selectedProductId = selectedProductId
                        formKindName = StockFormKind.TRANSFER.name
                        stepName = StockStep.MOVEMENT_FORM.name
                    },
                    onEdit = {
                        selectedProductId = selectedProductId
                        formKindName = StockFormKind.PRODUCT.name
                        stepName = StockStep.PRODUCT_FORM.name
                    },
                )

                StockStep.PRODUCT_FORM -> StockProductFormContent(
                    productId = selectedProductId.takeIf { it > 0 },
                    products = products,
                    categories = categories,
                    warehouses = warehouses,
                    onSave = { input ->
                        val id = selectedProductId.takeIf { it > 0 }
                        if (id == null) viewModel.createProduct(input) else viewModel.updateProduct(id, input)
                    },
                    onBack = { backFromStep() },
                )

                StockStep.MOVEMENT_FORM -> StockMovementFormContent(
                    kind = formKind,
                    products = products,
                    warehouses = warehouses,
                    selectedProductId = selectedProductId.takeIf { it > 0 },
                    onSave = viewModel,
                    onBack = { backFromStep() },
                )

                StockStep.INVENTORY_FORM -> StockInventoryFormContent(
                    products = products,
                    warehouses = warehouses,
                    onCreate = viewModel::createInventory,
                    onBack = { backFromStep() },
                )
            }
        }
    }

    if (showAddMenu) {
        StockAddMenu(onDismiss = { showAddMenu = false }) { kind ->
            showAddMenu = false
            formKindName = kind.name
            stepName = if (kind == StockFormKind.PRODUCT) StockStep.PRODUCT_FORM.name
            else if (kind == StockFormKind.INVENTORY) StockStep.INVENTORY_FORM.name
            else StockStep.MOVEMENT_FORM.name
        }
    }

    val currentReportLines = reportLines
    if (currentReportLines != null) {
        AlertDialog(
            onDismissRequest = { reportLines = null },
            title = { Text(reportTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (line in currentReportLines) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                line.label,
                                Modifier.weight(1f),
                                color = MissaMuted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(line.value, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reportLines = null }) { Text(stringResource(R.string.stk_close)) }
            },
        )
    }
}

private data class ReportLine(val label: String, val value: String)

// ---------------------------------------------------------------------------
// Barre du bas
// ---------------------------------------------------------------------------

@Composable
private fun StockBottomBar(selected: StockTab, onSelect: (StockTab) -> Unit, onAdd: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomItem(StockTab.DASHBOARD, selected, onSelect, Icons.Outlined.Dashboard, R.string.stk_dashboard)
            BottomItem(StockTab.PRODUCTS, selected, onSelect, Icons.Outlined.Inventory2, R.string.stk_products)
            Box(
                Modifier
                    .size(46.dp)
                    .background(BrandBlue, CircleShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.ops_add), tint = Color.White)
            }
            BottomItem(StockTab.MOVEMENTS, selected, onSelect, Icons.Outlined.SwapHoriz, R.string.stk_movements)
            BottomItem(StockTab.REPORTS, selected, onSelect, Icons.Outlined.BarChart, R.string.stk_reports)
        }
    }
}

@Composable
private fun BottomItem(own: StockTab, selected: StockTab, onSelect: (StockTab) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, labelRes: Int) {
    val active = own == selected
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onSelect(own) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) BrandBlue else MissaMuted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            stringResource(labelRes),
            color = if (active) BrandBlue else MissaMuted,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAddMenu(onDismiss: () -> Unit, onSelect: (StockFormKind) -> Unit) {
    val items = listOf(
        StockFormKind.ENTRY to (R.string.stk_entry_stock to Icons.Outlined.ArrowDownward),
        StockFormKind.EXIT to (R.string.stk_exit_stock to Icons.Outlined.ArrowUpward),
        StockFormKind.TRANSFER to (R.string.stk_transfer_stock_short to Icons.Outlined.SwapHoriz),
        StockFormKind.ADJUSTMENT to (R.string.stk_adjustment_stock to Icons.Outlined.Tune),
        StockFormKind.INVENTORY to (R.string.stk_new_inventory to Icons.Outlined.Assessment),
        StockFormKind.PRODUCT to (R.string.stk_new_product to Icons.Outlined.Inventory2),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.ops_add),
                Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                color = MissaInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            for ((kind, pair) in items) {
                val (labelRes, icon) = pair
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(kind) }
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(MissaSoftBlue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(19.dp))
                    }
                    Text(
                        stringResource(labelRes),
                        Modifier.padding(start = 12.dp),
                        color = MissaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Tableau de bord stock
// ---------------------------------------------------------------------------

@Composable
private fun StockDashboardContent(
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    movements: List<StockMovementEntity>,
    devise: String,
    onOpenProduct: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenMovements: () -> Unit,
) {
    val totalValue = products.sumOf { it.quantite * it.prixVente }
    val inStock = products.count { it.statut() == StockStatus.STOCK }
    val lowStock = products.count { it.statut() == StockStatus.LOW_STOCK }
    val outs = products.count { it.statut() == StockStatus.OUT }
    val entries = movements.count { it.type == StockMovementType.ENTRY.name || it.type == StockMovementType.TRANSFER_IN.name }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = MissaLayout.screenVertical),
        verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
    ) {
        item {
            StockValueCard(MoneyUtils.format(totalValue, devise))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StockStatTile(stringResource(R.string.stk_in_stock), inStock.toString(), BrandBlue, Modifier.weight(1f))
                StockStatTile(stringResource(R.string.stk_low_stock), lowStock.toString(), MissaMuted, Modifier.weight(1f))
                StockStatTile(stringResource(R.string.stk_ruptures), outs.toString(), Red40, Modifier.weight(1f))
                StockStatTile(stringResource(R.string.stk_entries), entries.toString(), Green60, Modifier.weight(1f))
            }
        }
        item {
            SectionHeader(stringResource(R.string.stk_repartition_by_category), null)
        }
        item {
            CategoryRepartition(products, categories, devise)
        }
        item {
            SectionHeader(stringResource(R.string.stk_recent_movements), stringResource(R.string.stk_all), onOpenMovements)
        }
        if (movements.isEmpty()) {
            item {
                StockEmptyPanel(
                    title = stringResource(R.string.stk_movements_empty),
                    description = stringResource(R.string.stk_products_empty_desc),
                )
            }
        } else {
            items(movements.take(5), key = { it.id }) { movement ->
                MovementRow(movement, products, devise, onClick = { onOpenProduct(movement.productId) })
            }
        }
    }
}

@Composable
private fun StockValueCard(value: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlue),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stk_stock_value), color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryRepartition(
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    devise: String,
) {
    val byCategory = products.groupBy { it.categorieId }.mapValues { (_, list) -> list.sumOf { it.quantite * it.prixVente } }
    val total = byCategory.values.sum()
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (byCategory.isEmpty()) {
                Text(stringResource(R.string.stk_no_data), color = MissaMuted, fontSize = 12.sp)
            } else {
                Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for ((catId, value) in byCategory.entries) {
                        val color = categories.firstOrNull { it.id == catId }?.let { stockColor(it.couleur) } ?: BrandBlue
                        val weight = (value / total).coerceAtLeast(0.01)
                        Box(
                            Modifier
                                .weight(weight.toFloat())
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(50)),
                        )
                    }
                }
                for ((catId, value) in byCategory.entries.sortedByDescending { it.value }) {
                    val category = categories.firstOrNull { it.id == catId }
                    val pct = if (total > 0.0) (value / total * 100.0) else 0.0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .background(category?.let { stockColor(it.couleur) } ?: BrandBlue, CircleShape),
                        )
                        Text(
                            category?.nom ?: "—",
                            Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            color = MissaInk,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("${pct.toInt()}%", color = MissaMuted, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                        Text(MoneyUtils.format(value, devise), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Liste des produits en stock
// ---------------------------------------------------------------------------

private enum class ProductFilter { ALL, LOW, OUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockProductsContent(
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    devise: String,
    onOpenProduct: (Long) -> Unit,
    onAddProduct: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(ProductFilter.ALL.name) }
    val filter = ProductFilter.valueOf(filterName)
    val low = products.count { it.statut() == StockStatus.LOW_STOCK }
    val out = products.count { it.statut() == StockStatus.OUT }
    val visible = products.filter { p ->
        search.isBlank() || p.nom.contains(search, ignoreCase = true) || p.code.contains(search, ignoreCase = true)
    }.filter { p ->
        when (filter) {
            ProductFilter.ALL -> true
            ProductFilter.LOW -> p.statut() == StockStatus.LOW_STOCK
            ProductFilter.OUT -> p.statut() == StockStatus.OUT
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(stringResource(R.string.stk_search_products)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MissaLayout.screenHorizontal, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Row(
            Modifier.padding(horizontal = MissaLayout.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = filter == ProductFilter.ALL,
                onClick = { filterName = ProductFilter.ALL.name },
                label = { Text(stringResource(R.string.stk_all_count, products.size)) },
            )
            FilterChip(
                selected = filter == ProductFilter.LOW,
                onClick = { filterName = ProductFilter.LOW.name },
                label = { Text(stringResource(R.string.stk_low_count, low)) },
            )
            FilterChip(
                selected = filter == ProductFilter.OUT,
                onClick = { filterName = ProductFilter.OUT.name },
                label = { Text(stringResource(R.string.stk_out_count, out)) },
            )
        }
        if (visible.isEmpty()) {
            StockEmptyPanel(
                title = stringResource(R.string.stk_products_empty),
                description = stringResource(R.string.stk_products_empty_desc),
                modifier = Modifier.padding(MissaLayout.screenHorizontal),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { product ->
                    ProductRow(product, categories.firstOrNull { it.id == product.categorieId }, devise, onOpenProduct)
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: StockProductEntity,
    category: StockCategoryEntity?,
    devise: String,
    onClick: (Long) -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick(product.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(MissaSoftBlue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(product.nom, color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.stk_product_code_category, product.code, category?.nom ?: "—"),
                    color = MissaMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.stk_unit_price, MoneyUtils.format(product.prixVente, devise), product.unite),
                    color = MissaMuted,
                    fontSize = 10.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.stk_quantity_display, quantityLabel(product.quantite), product.unite),
                    color = MissaInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                StockStatusChip(product.statut())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Détail d'un produit
// ---------------------------------------------------------------------------

@Composable
private fun StockProductDetailContent(
    productId: Long,
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    warehouses: List<StockWarehouseEntity>,
    movements: List<StockMovementEntity>,
    devise: String,
    onBack: () -> Unit,
    onAddStock: () -> Unit,
    onTransfer: () -> Unit,
    onEdit: () -> Unit,
) {
    val product = products.firstOrNull { it.id == productId }
    if (product == null) {
        StockEmptyPanel(stringResource(R.string.stk_missing), stringResource(R.string.stk_no_data))
        return
    }
    val category = categories.firstOrNull { it.id == product.categorieId }
    val warehouse = warehouses.firstOrNull { it.id == product.warehouseId }
    val productMovements = movements.filter { it.productId == product.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = MissaLayout.screenVertical),
        verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MissaBorder),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(MissaSoftBlue, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(product.nom, color = MissaInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${product.code} · ${category?.nom ?: "—"}", color = MissaMuted, fontSize = 11.sp)
                    }
                    StockStatusChip(product.statut())
                }
            }
        }
        item {
            DetailInfoCard(
                configuration = stringResource(
                    R.string.stk_product_code_category,
                    product.unite,
                    warehouse?.nom ?: "—",
                ),
                available = stringResource(R.string.stk_quantity_display, quantityLabel(product.quantite), product.unite),
                minimum = quantityLabel(product.seuilMin),
                maximum = quantityLabel(product.seuilMax),
                buy = MoneyUtils.format(product.prixAchat, devise),
                sell = MoneyUtils.format(product.prixVente, devise),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddStock,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandBlue),
                ) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.stk_add_to_stock), color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onTransfer,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandBlue),
                ) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.stk_transfer_stock), color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.stk_recent_movements), Modifier.weight(1f), color = MissaInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandBlue)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.stk_edit_product), color = BrandBlue, fontSize = 11.sp)
                }
            }
        }
        if (productMovements.isEmpty()) {
            item {
                StockEmptyPanel(stringResource(R.string.stk_movements_empty), stringResource(R.string.stk_no_data))
            }
        } else {
            items(productMovements, key = { it.id }) { movement ->
                MovementRow(movement, products, devise, onClick = {})
            }
        }
    }
}

@Composable
private fun DetailInfoCard(configuration: String, available: String, minimum: String, maximum: String, buy: String, sell: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stk_configuration), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            InfoLine(stringResource(R.string.stk_configuration), configuration)
            InfoLine(stringResource(R.string.stk_stock_available), available, valueColor = BrandBlue)
            InfoLine(stringResource(R.string.stk_minimum), minimum)
            InfoLine(stringResource(R.string.stk_maximum), maximum)
            InfoLine(stringResource(R.string.stk_purchase_price), buy)
            InfoLine(stringResource(R.string.stk_sale_price), sell, valueColor = Green60)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = MissaInk) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MissaMuted, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

// ---------------------------------------------------------------------------
// 4-7. Formulaires de mouvement et de produit
// ---------------------------------------------------------------------------

@Composable
private fun StockProductFormContent(
    productId: Long?,
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    warehouses: List<StockWarehouseEntity>,
    onSave: (StockUseCases.ProductInput) -> Unit,
    onBack: () -> Unit,
) {
    val editing = products.firstOrNull { it.id == productId }
    var nom by rememberSaveable(editing?.id) { mutableStateOf(editing?.nom ?: "") }
    var unite by rememberSaveable(editing?.id) { mutableStateOf(editing?.unite ?: "unité") }
    var categorieId by rememberSaveable(editing?.id) { mutableStateOf(editing?.categorieId?.toString() ?: "") }
    var warehouseId by rememberSaveable(editing?.id) { mutableStateOf(editing?.warehouseId?.toString() ?: "") }
    var prixAchat by rememberSaveable(editing?.id) { mutableStateOf(editing?.prixAchat?.cleanAmount() ?: "0") }
    var prixVente by rememberSaveable(editing?.id) { mutableStateOf(editing?.prixVente?.cleanAmount() ?: "0") }
    var seuilMin by rememberSaveable(editing?.id) { mutableStateOf(editing?.seuilMin?.cleanAmount() ?: "0") }
    var seuilMax by rememberSaveable(editing?.id) { mutableStateOf(editing?.seuilMax?.cleanAmount() ?: "0") }
    var quantiteInitiale by rememberSaveable(editing?.id) { mutableStateOf(editing?.quantiteInitiale?.cleanAmount() ?: "0") }
    var validation by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MissaLayout.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it; validation = false },
            label = { Text(stringResource(R.string.stk_product_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = validation && nom.trim().length < 2,
        )
        OutlinedTextField(
            value = unite,
            onValueChange = { unite = it },
            label = { Text(stringResource(R.string.stk_unit)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        StockPickerField(
            label = stringResource(R.string.stk_category),
            value = categories.firstOrNull { it.id == categorieId.toLongOrNull() }?.nom ?: "",
            options = categories.map { StockPickerOption(it.id.toString(), it.nom) },
            onSelect = { categorieId = it },
        )
        StockPickerField(
            label = stringResource(R.string.stk_warehouse),
            value = warehouses.firstOrNull { it.id == warehouseId.toLongOrNull() }?.nom ?: "",
            options = warehouses.map { StockPickerOption(it.id.toString(), it.nom) },
            onSelect = { warehouseId = it },
        )
        AmountField(stringResource(R.string.stk_buy_price), prixAchat) { prixAchat = it }
        AmountField(stringResource(R.string.stk_sell_price), prixVente) { prixVente = it }
        AmountField(stringResource(R.string.stk_min_seuil), seuilMin) { seuilMin = it }
        AmountField(stringResource(R.string.stk_max_seuil), seuilMax) { seuilMax = it }
        AmountField(stringResource(R.string.stk_initial_quantity), quantiteInitiale) { quantiteInitiale = it }
        Spacer(Modifier.height(12.dp))
        StockPrimaryButton(
            text = stringResource(R.string.stk_next),
            onClick = {
                val pAchat = prixAchat.toNumberOrNull() ?: 0.0
                val pVente = prixVente.toNumberOrNull() ?: 0.0
                val min = seuilMin.toNumberOrNull() ?: 0.0
                val max = seuilMax.toNumberOrNull() ?: 0.0
                val init = quantiteInitiale.toNumberOrNull() ?: 0.0
                if (nom.trim().length < 2) {
                    validation = true
                } else {
                    onSave(
                        StockUseCases.ProductInput(
                            nom = nom,
                            categorieId = categorieId.toLongOrNull(),
                            warehouseId = warehouseId.toLongOrNull(),
                            unite = unite,
                            prixAchat = pAchat,
                            prixVente = pVente,
                            seuilMin = min,
                            seuilMax = max,
                            quantiteInitiale = init,
                        ),
                    )
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StockMovementFormContent(
    kind: StockFormKind,
    products: List<StockProductEntity>,
    warehouses: List<StockWarehouseEntity>,
    selectedProductId: Long?,
    onSave: StockViewModel,
    onBack: () -> Unit,
) {
    var productId by rememberSaveable { mutableStateOf(selectedProductId?.toString() ?: "") }
    var sourceWarehouseId by rememberSaveable { mutableStateOf("") }
    var targetWarehouseId by rememberSaveable { mutableStateOf("") }
    var counterpart by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(System.currentTimeMillis())) }
    var motif by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf(false) }

    val selectedProduct = products.firstOrNull { it.id == productId.toLongOrNull() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MissaLayout.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (kind != StockFormKind.TRANSFER) {
            OutlinedTextField(
                value = counterpart,
                onValueChange = { counterpart = it },
                label = { Text(stringResource(R.string.stk_counterpart)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        InfoReadField(label = stringResource(R.string.stk_reference), value = stringResource(R.string.stk_auto_ref))
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text(stringResource(R.string.stk_date)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        StockPickerField(
            label = when (kind) {
                StockFormKind.ENTRY, StockFormKind.ADJUSTMENT -> stringResource(R.string.stk_warehouse)
                StockFormKind.EXIT -> stringResource(R.string.stk_source_warehouse)
                StockFormKind.TRANSFER -> stringResource(R.string.stk_source_warehouse)
                else -> stringResource(R.string.stk_warehouse)
            },
            value = warehouses.firstOrNull { it.id == sourceWarehouseId.toLongOrNull() }?.nom
                ?: warehouses.firstOrNull { it.id == targetWarehouseId.toLongOrNull() }?.nom
                ?: "",
            options = warehouses.map { StockPickerOption(it.id.toString(), it.nom) },
            onSelect = { sourceWarehouseId = it },
        )
        if (kind == StockFormKind.TRANSFER) {
            StockPickerField(
                label = stringResource(R.string.stk_destination_warehouse),
                value = warehouses.firstOrNull { it.id == targetWarehouseId.toLongOrNull() }?.nom ?: "",
                options = warehouses.map { StockPickerOption(it.id.toString(), it.nom) },
                onSelect = { targetWarehouseId = it },
            )
        }
        StockPickerField(
            label = stringResource(R.string.stk_product),
            value = selectedProduct?.nom ?: "",
            options = products.map { StockPickerOption(it.id.toString(), it.nom) },
            onSelect = { productId = it },
        )
        if (selectedProduct != null && kind != StockFormKind.TRANSFER) {
            Text(
                stringResource(R.string.stk_quantity_display, quantityLabel(selectedProduct.quantite), selectedProduct.unite),
                color = MissaMuted,
                fontSize = 11.sp,
            )
        }
        AmountField(stringResource(R.string.stk_quantity), quantity, isError = validation && quantity.toNumberOrNull()?.let { it <= 0.0 } != false) { quantity = it }
        if (kind == StockFormKind.ENTRY || kind == StockFormKind.EXIT) {
            AmountField(stringResource(R.string.stk_sale_price), price) { price = it }
        }
        if (kind == StockFormKind.ADJUSTMENT) {
            OutlinedTextField(
                value = motif,
                onValueChange = { motif = it },
                label = { Text(stringResource(R.string.stk_motif)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.stk_notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        StockPrimaryButton(
            text = stringResource(R.string.stk_next),
            onClick = {
                val product = productId.toLongOrNull() ?: -1L
                val qty = quantity.toNumberOrNull() ?: -1.0
                val date = parseDate(dateText)
                when (kind) {
                    StockFormKind.ENTRY -> onSave.createEntry(
                        productId = product, warehouseId = sourceWarehouseId.toLongOrNull(),
                        quantity = qty, price = price.toNumberOrNull(), counterpart = counterpart.ifBlank { null },
                        date = date, notes = notes.ifBlank { null },
                    )
                    StockFormKind.EXIT -> onSave.createExit(
                        productId = product, warehouseId = sourceWarehouseId.toLongOrNull(),
                        quantity = qty, price = price.toNumberOrNull(), counterpart = counterpart.ifBlank { null },
                        date = date, notes = notes.ifBlank { null },
                    )
                    StockFormKind.TRANSFER -> onSave.createTransfer(
                        productId = product, sourceWarehouseId = sourceWarehouseId.toLongOrNull(),
                        targetWarehouseId = targetWarehouseId.toLongOrNull(), quantity = qty,
                        date = date, notes = notes.ifBlank { null },
                    )
                    StockFormKind.ADJUSTMENT -> onSave.createAdjustment(
                        productId = product, warehouseId = sourceWarehouseId.toLongOrNull(),
                        countedQuantity = qty, date = date, notes = (motif + "\n" + notes).trim().ifBlank { null },
                    )
                    StockFormKind.PRODUCT, StockFormKind.INVENTORY -> Unit
                }
                if (product <= 0L || (kind != StockFormKind.ADJUSTMENT && qty <= 0.0) || (kind == StockFormKind.ADJUSTMENT && qty < 0.0)) {
                    validation = true
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filterAmountInput()) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun InfoReadField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// 8. Inventaire
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockInventoryContent(
    inventories: List<StockInventoryEntity>,
    onNewInventory: () -> Unit,
    onValidate: (Long) -> Unit,
    onComplete: (Long) -> Unit,
) {
    var tabName by rememberSaveable { mutableStateOf(StockInventoryStatus.DRAFT.name) }
    val tab = runCatching { StockInventoryStatus.valueOf(tabName) }.getOrDefault(StockInventoryStatus.DRAFT)
    val filtered = inventories.filter { it.status == tab.name }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = MissaLayout.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(tab == StockInventoryStatus.DRAFT, { tabName = StockInventoryStatus.DRAFT.name }, { Text(stringResource(R.string.stk_inventory_draft)) })
            FilterChip(tab == StockInventoryStatus.VALIDATED, { tabName = StockInventoryStatus.VALIDATED.name }, { Text(stringResource(R.string.stk_inventory_validated)) })
            FilterChip(tab == StockInventoryStatus.COMPLETED, { tabName = StockInventoryStatus.COMPLETED.name }, { Text(stringResource(R.string.stk_inventory_completed)) })
        }
        if (filtered.isEmpty()) {
            StockEmptyPanel(
                title = stringResource(R.string.stk_no_data),
                description = stringResource(R.string.stk_recent_movements),
                modifier = Modifier.padding(MissaLayout.screenHorizontal).padding(top = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { inventory ->
                    InventoryRow(inventory, onValidate, onComplete)
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
        StockPrimaryButton(
            text = stringResource(R.string.stk_new_inventory),
            onClick = onNewInventory,
            modifier = Modifier.padding(horizontal = MissaLayout.screenHorizontal, vertical = 10.dp),
        )
    }
}

@Composable
private fun InventoryRow(
    inventory: StockInventoryEntity,
    onValidate: (Long) -> Unit,
    onComplete: (Long) -> Unit,
) {
    val status = runCatching { StockInventoryStatus.valueOf(inventory.status) }.getOrDefault(StockInventoryStatus.DRAFT)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(inventory.reference, Modifier.weight(1f), color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                SurfacePill(
                    label = when (status) {
                        StockInventoryStatus.DRAFT -> stringResource(R.string.stk_inventory_draft)
                        StockInventoryStatus.VALIDATED -> stringResource(R.string.stk_inventory_validated)
                        StockInventoryStatus.COMPLETED -> stringResource(R.string.stk_inventory_completed)
                    },
                    color = when (status) {
                        StockInventoryStatus.DRAFT -> BrandBlue
                        StockInventoryStatus.VALIDATED -> Green60
                        StockInventoryStatus.COMPLETED -> MissaMuted
                    },
                )
            }
            Text(DateUtils.formatDateHeure(inventory.date), color = MissaMuted, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (status == StockInventoryStatus.DRAFT) {
                    TextButton(onClick = { onValidate(inventory.id) }) { Text(stringResource(R.string.stk_validate), color = Green60) }
                } else if (status == StockInventoryStatus.VALIDATED) {
                    TextButton(onClick = { onComplete(inventory.id) }) { Text(stringResource(R.string.stk_complete), color = BrandBlue) }
                }
                TextButton(onClick = {}) { Text(stringResource(R.string.stk_view_all_inventories), color = MissaMuted) }
            }
        }
    }
}

@Composable
private fun StockInventoryFormContent(
    products: List<StockProductEntity>,
    warehouses: List<StockWarehouseEntity>,
    onCreate: (Long?, Long, String?, List<StockUseCases.InventoryLineInput>) -> Unit,
    onBack: () -> Unit,
) {
    var warehouseId by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(System.currentTimeMillis())) }
    var notes by rememberSaveable { mutableStateOf("") }
    var counts by remember(products.map { it.id }.joinToString()) {
        mutableStateOf(products.associate { it.id to quantityLabel(it.quantite) })
    }
    var validation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = MissaLayout.screenVertical),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            StockPickerField(
                label = stringResource(R.string.stk_warehouse),
                value = warehouses.firstOrNull { it.id == warehouseId.toLongOrNull() }?.nom ?: "",
                options = warehouses.map { StockPickerOption(it.id.toString(), it.nom) },
                onSelect = { warehouseId = it },
            )
        }
        item {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text(stringResource(R.string.stk_date)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SectionHeader(stringResource(R.string.stk_new_inventory), null)
        }
        items(products, key = { it.id }) { product ->
            val current = counts[product.id] ?: quantityLabel(product.quantite)
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MissaBorder),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                stringResource(R.string.stk_expected) + " : " + stringResource(R.string.stk_quantity_display, quantityLabel(product.quantite), product.unite),
                                color = MissaMuted,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = current,
                        onValueChange = { counts = counts + (product.id to it.filterAmountInput()) },
                        label = { Text(stringResource(R.string.stk_counted)) },
                        singleLine = true,
                        isError = validation && (current.toNumberOrNull() ?: -1.0) < 0.0,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            StockPrimaryButton(
                text = stringResource(R.string.stk_save),
                onClick = {
                    val lines = products.map { product ->
                        StockUseCases.InventoryLineInput(
                            productId = product.id,
                            expected = product.quantite,
                            counted = (counts[product.id] ?: quantityLabel(product.quantite)).toNumberOrNull() ?: -1.0,
                        )
                    }
                    if (lines.any { it.counted < 0.0 }) {
                        validation = true
                    } else {
                        onCreate(warehouseId.toLongOrNull(), parseDate(dateText), notes.ifBlank { null }, lines)
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 9. Mouvements de stock
// ---------------------------------------------------------------------------

private enum class MovementFilter { ALL, IN, OUT, ADJ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockMovementsContent(
    movements: List<StockMovementEntity>,
    products: List<StockProductEntity>,
    warehouses: List<StockWarehouseEntity>,
    devise: String,
    onAdd: () -> Unit,
) {
    var filterName by rememberSaveable { mutableStateOf(MovementFilter.ALL.name) }
    val filter = MovementFilter.valueOf(filterName)
    val visible = movements.filter { m ->
        when (filter) {
            MovementFilter.ALL -> true
            MovementFilter.IN -> m.type == StockMovementType.ENTRY.name || m.type == StockMovementType.TRANSFER_IN.name
            MovementFilter.OUT -> m.type == StockMovementType.EXIT.name || m.type == StockMovementType.TRANSFER_OUT.name
            MovementFilter.ADJ -> m.type == StockMovementType.ADJUSTMENT.name
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = MissaLayout.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(filter == MovementFilter.ALL, { filterName = MovementFilter.ALL.name }, { Text(stringResource(R.string.stk_all)) })
            FilterChip(filter == MovementFilter.IN, { filterName = MovementFilter.IN.name }, { Text(stringResource(R.string.stk_entries_tab)) })
            FilterChip(filter == MovementFilter.OUT, { filterName = MovementFilter.OUT.name }, { Text(stringResource(R.string.stk_exits_tab)) })
            FilterChip(filter == MovementFilter.ADJ, { filterName = MovementFilter.ADJ.name }, { Text(stringResource(R.string.stk_adjustments_tab)) })
        }
        if (visible.isEmpty()) {
            StockEmptyPanel(
                title = stringResource(R.string.stk_movements_empty),
                description = stringResource(R.string.stk_no_data),
                modifier = Modifier.padding(MissaLayout.screenHorizontal).padding(top = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visible.groupBy { DateUtils.formatDate(it.date) }.forEach { (date, list) ->
                    item {
                        Text(date, color = MissaMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(list, key = { it.id }) { movement ->
                        MovementRow(movement, products, devise, onClick = {})
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun MovementRow(
    movement: StockMovementEntity,
    products: List<StockProductEntity>,
    devise: String,
    onClick: () -> Unit,
) {
    val type = runCatching { StockMovementType.valueOf(movement.type) }.getOrDefault(StockMovementType.ADJUSTMENT)
    val positive = movement.delta >= 0.0
    val color = when (type) {
        StockMovementType.ENTRY, StockMovementType.TRANSFER_IN -> Green60
        StockMovementType.EXIT, StockMovementType.TRANSFER_OUT -> Red40
        StockMovementType.ADJUSTMENT -> BrandBlue
    }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (positive) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stockMovementLabel(type), color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(movement.reference, color = MissaMuted, fontSize = 10.sp)
                }
                Text(
                    movementProductLabel(movement, products),
                    color = MissaMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(DateUtils.formatDateHeure(movement.date), color = MissaMuted, fontSize = 9.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    signedQuantityLabel(movement.delta),
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                movement.price?.takeIf { it > 0.0 }?.let {
                    Text(MoneyUtils.format(it, devise), color = MissaMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 10. Rapports de stock
// ---------------------------------------------------------------------------

@Composable
private fun StockReportsContent(
    products: List<StockProductEntity>,
    categories: List<StockCategoryEntity>,
    movements: List<StockMovementEntity>,
    devise: String,
    onShowReport: (String, List<ReportLine>) -> Unit,
) {
    val reportCards = listOf(
        Triple(R.string.stk_report_value, R.string.stk_report_value_desc, Icons.Outlined.BarChart),
        Triple(R.string.stk_report_low, R.string.stk_report_low_desc, Icons.Outlined.WarningAmber),
        Triple(R.string.stk_report_movement, R.string.stk_report_movement_desc, Icons.Outlined.SwapHoriz),
        Triple(R.string.stk_report_repartition, R.string.stk_report_repartition_desc, Icons.Outlined.Category),
        Triple(R.string.stk_report_monthly, R.string.stk_report_monthly_desc, Icons.Outlined.LocalShipping),
        Triple(R.string.stk_report_history, R.string.stk_report_history_desc, Icons.Outlined.History),
    )
    fun reportLines(titleRes: Int): List<ReportLine> = when (titleRes) {
        R.string.stk_report_value -> products.sortedByDescending { it.quantite * it.prixVente }
            .map { ReportLine(it.nom, MoneyUtils.format(it.quantite * it.prixVente, devise)) }
        R.string.stk_report_low -> products.filter { it.statut() != StockStatus.STOCK }
            .map { ReportLine(it.nom, quantityLabel(it.quantite)) }
        R.string.stk_report_movement -> movements.groupingBy { runCatching { StockMovementType.valueOf(it.type) }.getOrDefault(StockMovementType.ADJUSTMENT) }
            .eachCount()
            .map { (type, count) -> ReportLine(stockMovementLabel(type), count.toString()) }
        R.string.stk_report_repartition -> categories.map { category ->
            val count = products.count { it.categorieId == category.id }
            ReportLine(category.nom, count.toString())
        }
        R.string.stk_report_monthly -> monthlyReportLines(movements, devise)
        R.string.stk_report_history -> monthlyValueLines(products, movements, devise)
        else -> emptyList()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MissaLayout.screenHorizontal, vertical = MissaLayout.screenVertical),
        verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
    ) {
        item {
            SectionHeader(stringResource(R.string.stk_report_title), stringResource(R.string.stk_report_intro))
        }
        reportCards.chunked(2).forEach { rowItems ->
            item {
                val noDataLabel = stringResource(R.string.stk_no_data)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((title, desc, icon) in rowItems) {
                        val reportTitle = stringResource(title)
                        ReportCard(
                            title = reportTitle,
                            description = stringResource(desc),
                            icon = icon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val lines = reportLines(title)
                                onShowReport(
                                    reportTitle,
                                    lines.ifEmpty { listOf(ReportLine(noDataLabel, "")) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(MissaSoftBlue, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(19.dp))
            }
            Text(title, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(description, color = MissaMuted, fontSize = 10.sp, maxLines = 3)
        }
    }
}

private fun StockStatus.statusRes(): Int = when (this) {
    StockStatus.STOCK -> R.string.stk_status_stock
    StockStatus.LOW_STOCK -> R.string.stk_status_low
    StockStatus.OUT -> R.string.stk_status_out
}

private fun monthlyReportLines(movements: List<StockMovementEntity>, devise: String): List<ReportLine> =
    movements.groupBy { monthKey(it.date) }
        .toSortedMap(compareBy { it })
        .map { (month, list) ->
            val incoming = list.filter { it.delta > 0.0 }.sumOf { it.delta }
            val outgoing = list.filter { it.delta < 0.0 }.sumOf { it.delta }
            ReportLine(month, "+${quantityLabel(incoming)} / ${quantityLabel(outgoing)}")
        }

private fun monthlyValueLines(products: List<StockProductEntity>, movements: List<StockMovementEntity>, devise: String): List<ReportLine> =
    movements.groupBy { monthKey(it.date) }
        .toSortedMap(compareBy { it })
        .map { (month, list) ->
            val value = list.sumOf { it.delta * (it.price?.takeIf { p -> p > 0.0 } ?: products.firstOrNull { p -> p.id == it.productId }?.prixVente ?: 0.0) }
            ReportLine(month, MoneyUtils.format(value, devise))
        }

private fun monthKey(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = MissaInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, color = BrandBlue, fontSize = 11.sp) }
        }
    }
}

private fun parseDate(text: String): Long {
    val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
    formats.forEach { pattern ->
        val parsed = runCatching { SimpleDateFormat(pattern, Locale.getDefault()).parse(text.trim()) }.getOrNull()
        if (parsed != null) return parsed.time
    }
    return System.currentTimeMillis()
}

private fun Double.cleanAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString().replace('.', ',')

private fun String.filterAmountInput(): String =
    filter { it.isDigit() || it == ',' || it == '.' }.take(12)

private fun String.toNumberOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

private fun exportMovementsCsv(context: Context, movements: List<StockMovementEntity>, products: List<StockProductEntity>, devise: String) {
    val csv = buildString {
        appendLine("Référence;Type;Produit;Quantité;Delta;Date")
        movements.forEach { m ->
            appendLine(
                listOf(
                    m.reference,
                    runCatching { StockMovementType.valueOf(m.type).name }.getOrDefault(m.type),
                    movementProductLabel(m, products).replace(";", ","),
                    m.quantity.toString(),
                    m.delta.toString(),
                    DateUtils.formatDateHeure(m.date),
                ).joinToString(";"),
            )
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Movements stock ${DateUtils.formatDate(System.currentTimeMillis())}")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV"))
}

@Composable
private fun StockFormKind.titleRes(): Int = when (this) {
    StockFormKind.ENTRY -> R.string.stk_entry_stock
    StockFormKind.EXIT -> R.string.stk_exit_stock
    StockFormKind.TRANSFER -> R.string.stk_transfer_stock_short
    StockFormKind.ADJUSTMENT -> R.string.stk_adjustment_stock
    StockFormKind.PRODUCT -> R.string.stk_new_product
    StockFormKind.INVENTORY -> R.string.stk_new_inventory
}

@Composable
private fun StockTab.titleRes(): Int = when (this) {
    StockTab.DASHBOARD -> R.string.stk_dashboard
    StockTab.PRODUCTS -> R.string.stk_products
    StockTab.MOVEMENTS -> R.string.stk_movements
    StockTab.REPORTS -> R.string.stk_reports
    StockTab.INVENTORY -> R.string.stk_inventory
}
