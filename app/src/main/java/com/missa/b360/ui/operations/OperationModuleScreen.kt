package com.missa.b360.ui.operations

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/**
 * Ecran commun aux modules opérationnels livrés progressivement. Chaque module possède sa
 * propre route et ses propres pièces, tout en partageant les garanties de création et audit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationModuleScreen(
    module: OperationModule,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    openCreate: Boolean = false,
    initialDirection: OperationDirection = OperationDirection.NONE,
    viewModel: OperationsViewModel = hiltViewModel(),
) {
    val records by viewModel.records(module).collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val openForm = { onNavigate(operationFormRoute(module, initialDirection)) }

    LaunchedEffect(openCreate) {
        if (openCreate) openForm()
    }
    LaunchedEffect(result) {
        when (val current = result) {
            is OperationsViewModel.Result.Created -> {
                snackbar.showSnackbar(context.getString(R.string.ops_created, current.reference))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.Invalid -> {
                snackbar.showSnackbar(context.getString(R.string.ops_invalid))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error))
                viewModel.clearResult()
            }
            null -> Unit
        }
    }

    val titleRes = module.appModule().titleRes
    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(titleRes),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = openForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.ops_add),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MissaLayout.screenHorizontal,
                vertical = MissaLayout.screenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
        ) {
            item {
                MissaSectionTitle(
                    title = stringResource(titleRes),
                    subtitle = stringResource(module.hintRes()),
                )
            }
            if (records.isEmpty()) {
                item {
                    EmptyOperationState(module = module, onCreate = openForm)
                }
            } else {
                items(records, key = { it.id }) { record ->
                    OperationRecordCard(
                        record = record,
                        devise = devise,
                        onValidate = { viewModel.setStatus(record.id, OperationStatus.VALIDATED) },
                        onCancel = { viewModel.setStatus(record.id, OperationStatus.CANCELLED) },
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

/** Route du formulaire d'une opération (page dédiée, spec §3.2). */
fun operationFormRoute(module: OperationModule, direction: OperationDirection = OperationDirection.NONE): String =
    "operation_form?module=${module.name}" + direction.takeUnless { it == OperationDirection.NONE }?.let { "&direction=${it.name}" }.orEmpty()

/**
 * Formulaire d'opération — UNE PAGE (spec §3.2) : identité, montants, sens, notes,
 * résumé, barre d'actions [Annuler][Enregistrer]. Les saisies sont conservées en cas
 * d'erreur et le bouton est désactivé pendant l'enregistrement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationFormScreen(
    module: OperationModule,
    initialDirection: OperationDirection,
    onBack: () -> Unit,
    viewModel: OperationsViewModel = hiltViewModel(),
) {
    val devise by viewModel.devise.collectAsState()
    val enCours by viewModel.enCours.collectAsState()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var counterpart by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var direction by remember(initialDirection) {
        mutableStateOf(initialDirection.takeUnless { it == OperationDirection.NONE } ?: OperationDirection.IN)
    }
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    val showAmount = module != OperationModule.STOCK
    val showQuantity = module in setOf(
        OperationModule.STOCK,
        OperationModule.LIVRAISON,
        OperationModule.PRODUCTION,
        OperationModule.SERVICES,
    )
    val invalid = stringResource(R.string.product_amount_invalid)
    val titleValid = title.trim().length >= 2
    val amountValue = amount.toNumberOrNull()
    val quantityValue = quantity.toNumberOrNull()
    val canConfirm = titleValid &&
        (amount.isBlank() || amountValue != null) &&
        (quantity.isBlank() || quantityValue != null) &&
        !enCours

    LaunchedEffect(result) {
        when (val current = result) {
            is OperationsViewModel.Result.Created -> {
                snackbar.showSnackbar(context.getString(R.string.ops_created, current.reference))
                viewModel.clearResult()
                onBack()
            }
            OperationsViewModel.Result.Invalid -> {
                snackbar.showSnackbar(context.getString(R.string.ops_invalid))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.ReadOnly -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error))
                viewModel.clearResult()
            }
            null -> Unit
        }
    }

    val onConfirm: () -> Unit = onConfirm@{
        // Validation UI avant la validation métier — les saisies sont conservées (spec §3).
        val errors = mutableMapOf<String, String>()
        if (!titleValid) errors["title"] = context.getString(R.string.product_name_required)
        if (amount.isNotBlank() && amountValue == null) errors["amount"] = invalid
        if (quantity.isNotBlank() && quantityValue == null) errors["quantity"] = invalid
        if (errors.isNotEmpty()) {
            titleError = errors["title"]
            amountError = errors["amount"]
            quantityError = errors["quantity"]
            return@onConfirm
        }
        titleError = null
        amountError = null
        quantityError = null
        viewModel.create(module, title, counterpart, amount, quantity, direction, notes)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MissaCanvas,
            topBar = {
                MissaTopAppBar(
                    title = stringResource(R.string.ops_new, stringResource(module.appModule().titleRes)),
                    onBack = onBack,
                )
            },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !enCours,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Text(stringResource(R.string.ops_cancel))
                        }
                        Button(
                            onClick = onConfirm,
                            enabled = canConfirm,
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        ) {
                            if (enCours) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Outlined.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(stringResource(R.string.ops_save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // --- IDENTITÉ ---
                FormCard(stringResource(R.string.ops_title)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ops_title)) },
                        isError = titleError != null,
                        supportingText = { titleError?.let { Text(it) } ?: Text(" ") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = counterpart,
                        onValueChange = { counterpart = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ops_counterpart)) },
                        singleLine = true,
                    )
                }

                // --- MONTANTS ---
                if (showAmount || showQuantity) {
                    FormCard(stringResource(R.string.ops_amount)) {
                        if (showAmount) {
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it.numberChars(); amountError = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ops_amount)) },
                                isError = amountError != null,
                                supportingText = { amountError?.let { Text(it) } ?: Text(" ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }
                        if (showQuantity) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it.numberChars(); quantityError = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ops_quantity)) },
                                isError = quantityError != null,
                                supportingText = { quantityError?.let { Text(it) } ?: Text(" ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }
                    }
                }

                // --- SENS (caisse) ---
                if (module == OperationModule.FINANCES) {
                    FormCard(stringResource(R.string.ops_direction)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = direction == OperationDirection.IN,
                                onClick = { direction = OperationDirection.IN },
                                label = { Text(stringResource(R.string.ops_income)) },
                            )
                            FilterChip(
                                selected = direction == OperationDirection.OUT,
                                onClick = { direction = OperationDirection.OUT },
                                label = { Text(stringResource(R.string.ops_expense)) },
                            )
                        }
                    }
                }

                // --- NOTES ---
                FormCard(stringResource(R.string.ops_notes)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ops_notes)) },
                        minLines = 2,
                        maxLines = 4,
                    )
                }

                // --- RÉSUMÉ ---
                FormCard(stringResource(R.string.ops_summary)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (showAmount && amount.isNotBlank()) {
                                SummaryLine(
                                    stringResource(R.string.ops_amount),
                                    if (amountValue != null) MoneyUtils.format(amountValue, devise) else amount,
                                )
                            }
                            if (showQuantity && quantity.isNotBlank()) {
                                SummaryLine(
                                    stringResource(R.string.ops_quantity),
                                    quantity,
                                )
                            }
                            SummaryLine(
                                stringResource(R.string.ops_direction),
                                stringResource(
                                    if (direction == OperationDirection.OUT) R.string.ops_expense else R.string.ops_income,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp),
        )
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
            Text(
                text = title,
                color = BrandBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            content()
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MissaMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MissaInk, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun String.numberChars(): String = filter { it.isDigit() || it == ',' || it == '.' }

private fun String.toNumberOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() }

@Composable
private fun EmptyOperationState(module: OperationModule, onCreate: () -> Unit) {
    MissaEmptyState(
        icon = module.appModule().icon,
        title = stringResource(R.string.ops_empty, stringResource(module.appModule().titleRes)),
        action = { TextButton(onClick = onCreate) { Text(stringResource(R.string.ops_add)) } },
    )
}

@Composable
private fun OperationRecordCard(
    record: OperationRecordEntity,
    devise: String,
    onValidate: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = OperationStatus.entries.firstOrNull { it.name == record.status } ?: OperationStatus.DRAFT
    MissaPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.title, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(
                            R.string.ops_record_detail,
                            record.reference,
                            DateUtils.formatDateHeure(record.createdAt),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(status)
            }
            record.counterpart?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                record.amount?.let {
                    Text(
                        text = MoneyUtils.format(it, devise),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (record.amount != null && record.quantity != null) Spacer(Modifier.width(12.dp))
                record.quantity?.let {
                    Text(
                        text = stringResource(R.string.ops_quantity_value, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            record.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (status == OperationStatus.DRAFT) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ops_cancel))
                    }
                    TextButton(onClick = onValidate) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ops_validate))
                    }
                }
            }
    }
}

@Composable
private fun StatusChip(status: OperationStatus) {
    val (label, color) = when (status) {
        OperationStatus.DRAFT -> R.string.ops_status_draft to MaterialTheme.colorScheme.secondary
        OperationStatus.VALIDATED -> R.string.ops_status_validated to Color(0xFF16883C)
        OperationStatus.CANCELLED -> R.string.ops_status_cancelled to MaterialTheme.colorScheme.error
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(stringResource(label)) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = color,
            disabledContainerColor = color.copy(alpha = 0.10f),
        ),
        border = null,
    )
}

private fun OperationModule.appModule(): AppModule = when (this) {
    OperationModule.STOCK -> AppModule.STOCK
    OperationModule.DEVIS, OperationModule.COMMANDE -> AppModule.VENTE
    OperationModule.VENTE -> AppModule.VENTE
    OperationModule.ACHATS -> AppModule.ACHATS
    OperationModule.FINANCES -> AppModule.FINANCES
    OperationModule.LIVRAISON -> AppModule.LIVRAISON
    OperationModule.PRODUCTION -> AppModule.PRODUCTION
    OperationModule.SERVICES -> AppModule.SERVICES
    OperationModule.RH -> AppModule.RH
    OperationModule.PROJETS -> AppModule.PROJETS
}

private fun OperationModule.hintRes(): Int = when (this) {
    OperationModule.STOCK -> R.string.ops_hint_stock
    OperationModule.DEVIS, OperationModule.COMMANDE -> R.string.ops_hint_sales
    OperationModule.VENTE -> R.string.ops_hint_sales
    OperationModule.ACHATS -> R.string.ops_hint_purchases
    OperationModule.FINANCES -> R.string.ops_hint_finances
    OperationModule.LIVRAISON -> R.string.ops_hint_delivery
    OperationModule.PRODUCTION -> R.string.ops_hint_production
    OperationModule.SERVICES -> R.string.ops_hint_services
    OperationModule.RH -> R.string.ops_hint_hr
    OperationModule.PROJETS -> R.string.ops_hint_projects
}
