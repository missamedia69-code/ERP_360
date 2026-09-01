package com.missa.b360.ui.operations

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
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
import com.missa.b360.ui.navigation.AppModule

/**
 * Ecran commun aux modules opérationnels livrés progressivement. Chaque module possède sa
 * propre route et ses propres pièces, tout en partageant les garanties de création et audit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationModuleScreen(
    module: OperationModule,
    onBack: () -> Unit,
    openCreate: Boolean = false,
    initialDirection: OperationDirection = OperationDirection.NONE,
    viewModel: OperationsViewModel = hiltViewModel(),
) {
    val records by viewModel.records(module).collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    val result by viewModel.result.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var formVisible by remember { mutableStateOf(false) }

    LaunchedEffect(openCreate) {
        if (openCreate) formVisible = true
    }
    LaunchedEffect(result) {
        when (val current = result) {
            is OperationsViewModel.Result.Created -> {
                formVisible = false
                snackbar.showSnackbar(stringResource(R.string.ops_created, current.reference))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.Invalid -> {
                snackbar.showSnackbar(stringResource(R.string.ops_invalid))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.ReadOnly -> {
                snackbar.showSnackbar(stringResource(R.string.ops_read_only))
                viewModel.clearResult()
            }
            OperationsViewModel.Result.Error -> {
                snackbar.showSnackbar(stringResource(R.string.ops_error))
                viewModel.clearResult()
            }
            null -> Unit
        }
    }

    val titleRes = module.appModule().titleRes
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(titleRes), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { formVisible = true }) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = stringResource(module.hintRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (records.isEmpty()) {
                item {
                    EmptyOperationState(module = module, onCreate = { formVisible = true })
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

    if (formVisible) {
        OperationFormDialog(
            module = module,
            initialDirection = initialDirection,
            onDismiss = { formVisible = false },
            onConfirm = { title, counterpart, amount, quantity, direction, notes ->
                viewModel.create(module, title, counterpart, amount, quantity, direction, notes)
            },
        )
    }
}

@Composable
private fun EmptyOperationState(module: OperationModule, onCreate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = module.appModule().icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = stringResource(R.string.ops_empty, stringResource(module.appModule().titleRes)),
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onCreate) {
                Text(stringResource(R.string.ops_add))
            }
        }
    }
}

@Composable
private fun OperationRecordCard(
    record: OperationRecordEntity,
    devise: String,
    onValidate: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = OperationStatus.entries.firstOrNull { it.name == record.status } ?: OperationStatus.DRAFT
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
}

@Composable
private fun StatusChip(status: OperationStatus) {
    val (label, color) = when (status) {
        OperationStatus.DRAFT -> R.string.ops_status_draft to MaterialTheme.colorScheme.secondary
        OperationStatus.VALIDATED -> R.string.ops_status_validated to Color(0xFF16803C)
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

@Composable
private fun OperationFormDialog(
    module: OperationModule,
    initialDirection: OperationDirection,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, OperationDirection, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var counterpart by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var direction by remember(initialDirection) {
        mutableStateOf(initialDirection.takeUnless { it == OperationDirection.NONE } ?: OperationDirection.IN)
    }
    val showAmount = module != OperationModule.STOCK
    val showQuantity = module in setOf(
        OperationModule.STOCK,
        OperationModule.LIVRAISON,
        OperationModule.PRODUCTION,
        OperationModule.SERVICES,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ops_new, stringResource(module.appModule().titleRes))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ops_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = counterpart,
                    onValueChange = { counterpart = it },
                    label = { Text(stringResource(R.string.ops_counterpart)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showAmount) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text(stringResource(R.string.ops_amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showQuantity) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(stringResource(R.string.ops_quantity)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (module == OperationModule.FINANCES) {
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
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.ops_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, counterpart, amount, quantity, direction, notes) }) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.ops_cancel))
            }
        },
    )
}

private fun OperationModule.appModule(): AppModule = when (this) {
    OperationModule.STOCK -> AppModule.STOCK
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
    OperationModule.VENTE -> R.string.ops_hint_sales
    OperationModule.ACHATS -> R.string.ops_hint_purchases
    OperationModule.FINANCES -> R.string.ops_hint_finances
    OperationModule.LIVRAISON -> R.string.ops_hint_delivery
    OperationModule.PRODUCTION -> R.string.ops_hint_production
    OperationModule.SERVICES -> R.string.ops_hint_services
    OperationModule.RH -> R.string.ops_hint_hr
    OperationModule.PROJETS -> R.string.ops_hint_projects
}
