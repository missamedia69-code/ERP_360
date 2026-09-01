package com.missa.b360.ui.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.navigation.AppModule

/** Reporting local : agrégats déterministes à partir des pièces réellement enregistrées. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(
    onBack: () -> Unit,
    viewModel: OperationsViewModel = hiltViewModel(),
) {
    val records by viewModel.allRecords().collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    val validated = records.filter { it.status == OperationStatus.VALIDATED.name }
    val sales = validated.amountFor(OperationModule.VENTE)
    val purchases = validated.amountFor(OperationModule.ACHATS)
    val cash = validated
        .filter { it.module == OperationModule.FINANCES.name }
        .sumOf { record ->
            when (record.direction) {
                OperationDirection.IN.name -> record.amount ?: 0.0
                OperationDirection.OUT.name -> -(record.amount ?: 0.0)
                else -> 0.0
            }
        }
    val moduleCounts = OperationModule.entries.mapNotNull { module ->
        val count = records.count { it.module == module.name }
        count.takeIf { it > 0 }?.let { module to it }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.module_reporting), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.ob_retour))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.reporting_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ReportTotalsCard(
                    sales = MoneyUtils.format(sales, devise),
                    purchases = MoneyUtils.format(purchases, devise),
                    cash = MoneyUtils.format(cash, devise),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.reporting_module_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (moduleCounts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.reporting_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(moduleCounts, key = { it.first.name }) { (module, count) ->
                    ReportModuleRow(module, count)
                }
            }
        }
    }
}

@Composable
private fun ReportTotalsCard(sales: String, purchases: String, cash: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Assessment, contentDescription = null)
                Text(
                    text = stringResource(R.string.reporting_current_totals),
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
            ReportTotalLine(R.string.module_vente, sales)
            ReportTotalLine(R.string.module_achats, purchases)
            ReportTotalLine(R.string.home_cash, cash)
        }
    }
}

@Composable
private fun ReportTotalLine(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportModuleRow(module: OperationModule, count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(module.appModuleForReport().icon, contentDescription = null)
            Text(
                text = stringResource(module.appModuleForReport().titleRes),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.reporting_records_count, count))
        }
    }
}

private fun List<OperationRecordEntity>.amountFor(module: OperationModule): Double =
    filter { it.module == module.name }.sumOf { it.amount ?: 0.0 }

private fun OperationModule.appModuleForReport(): AppModule = when (this) {
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
