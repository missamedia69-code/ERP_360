package com.missa.b360.ui.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Reporting local, rendu dans la même grille compacte que les écrans métier. */
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
        containerColor = MissaCanvas,
        topBar = { MissaTopAppBar(title = androidx.compose.ui.res.stringResource(R.string.module_reporting), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                horizontal = MissaLayout.screenHorizontal,
                vertical = MissaLayout.screenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
        ) {
            item {
                MissaSectionTitle(
                    title = androidx.compose.ui.res.stringResource(R.string.reporting_current_totals),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.reporting_subtitle),
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
                MissaSectionTitle(title = androidx.compose.ui.res.stringResource(R.string.reporting_module_activity))
            }
            if (moduleCounts.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Icons.Outlined.Assessment,
                        title = androidx.compose.ui.res.stringResource(R.string.reporting_empty),
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
    MissaPanel(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.reporting_current_totals),
                modifier = Modifier.padding(start = 8.dp),
                color = MissaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        ReportTotalLine(R.string.module_vente, sales)
        ReportTotalLine(R.string.module_achats, purchases)
        ReportTotalLine(R.string.home_cash, cash)
    }
}

@Composable
private fun ReportTotalLine(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = androidx.compose.ui.res.stringResource(labelRes),
            modifier = Modifier.weight(1f),
            color = MissaMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(value, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ReportModuleRow(module: OperationModule, count: Int) {
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = module.appModuleForReport().icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = androidx.compose.ui.res.stringResource(module.appModuleForReport().titleRes),
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                color = MissaInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.reporting_records_count, count),
                color = MissaMuted,
                fontSize = 10.sp,
            )
        }
    }
}

private fun List<OperationRecordEntity>.amountFor(module: OperationModule): Double =
    filter { it.module == module.name }.sumOf { it.amount ?: 0.0 }

private fun OperationModule.appModuleForReport(): AppModule = when (this) {
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
