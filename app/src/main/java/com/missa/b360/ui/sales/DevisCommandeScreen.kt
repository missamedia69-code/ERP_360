package com.missa.b360.ui.sales

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.usecase.CommercialTarget
import com.missa.b360.core.domain.usecase.ConvertDevisToOrderUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaveDevisCommandeUseCase
import com.missa.b360.core.util.toMoneyOrNull
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.TendrePositive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevisCommandeViewModel @Inject constructor(
    observeClients: ObserveClientsUseCase,
    operations: OperationUseCases,
    private val saveDevis: SaveDevisCommandeUseCase,
    private val convertirDevis: ConvertDevisToOrderUseCase,
) : ViewModel() {
    val clients: StateFlow<List<ClientEntity>> = observeClients()
        .map { list -> list.filter { it.statut == ClientStatus.ACTIF } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devis: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.DEVIS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commandes: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.COMMANDE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _feedback = MutableStateFlow<Int?>(null)
    val feedback: StateFlow<Int?> = _feedback
    fun effacerFeedback() { _feedback.value = null }

    fun creerDevis(client: ClientEntity?, designation: String, prixText: String): Boolean {
        val ligne = designation.trim()
        val prix = prixText.toMoneyOrNull()
        if (client == null) {
            _feedback.value = R.string.devis_error_select_client
            return false
        }
        if (ligne.length !in 2..120 || prix == null || prix <= 0.0) {
            _feedback.value = R.string.devis_error_line
            return false
        }
        viewModelScope.launch {
            val payload = SaleRecordPayload(
                clientId = client.id,
                clientName = client.nom,
                lines = listOf(SaleLine(id = 1L, name = ligne, unitPrice = prix, quantity = 1.0)),
                subtotal = prix,
                discount = 0.0,
                delivery = 0.0,
                taxRate = 0.0,
                taxAmount = 0.0,
                total = prix,
                paymentMethod = "",
                paidAmount = 0.0,
            )
            _feedback.value = when (saveDevis(CommercialTarget.Devis, null, payload)) {
                is SaveDevisCommandeUseCase.Result.Succes -> R.string.devis_result_created
                SaveDevisCommandeUseCase.Result.LectureSeule -> R.string.devis_result_readonly
                else -> R.string.devis_error_line
            }
        }
        return true
    }

    fun convertirEnCommande(devisId: Long) {
        viewModelScope.launch {
            _feedback.value = when (convertirDevis(devisId, CommercialTarget.Devis)) {
                is ConvertDevisToOrderUseCase.Result.Succes -> R.string.devis_result_order
                ConvertDevisToOrderUseCase.Result.DejaConverti -> R.string.devis_result_already_order
                ConvertDevisToOrderUseCase.Result.LectureSeule -> R.string.devis_result_readonly
                else -> R.string.devis_result_invalid
            }
        }
    }
}

@Composable
fun DevisCommandeScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    viewModel: DevisCommandeViewModel = hiltViewModel(),
) {
    val devis by viewModel.devis.collectAsState()
    val commandes by viewModel.commandes.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    var afficherCreation by remember { mutableStateOf(openCreate) }
    val contexte = LocalContext.current

    LaunchedEffect(feedback) {
        feedback?.let {
            Toast.makeText(contexte, contexte.getString(it), Toast.LENGTH_SHORT).show()
            viewModel.effacerFeedback()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.devis_screen_title), onBack = onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.devis_screen_title), color = MissaInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.devis_page_desc), color = MissaMuted, fontSize = 12.sp)
            }
            Button(onClick = { afficherCreation = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(painterResource(Iv.Add), contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.devis_new))
            }
        }
        if (devis.isEmpty() && commandes.isEmpty()) {
            MissaEmptyState(
                icon = Iv.Description,
                title = stringResource(R.string.devis_screen_title),
                description = stringResource(R.string.devis_empty),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (devis.isNotEmpty()) {
                    item(key = "devis-heading") {
                        Text(stringResource(R.string.devis_quotes_header), color = MissaInk, fontWeight = FontWeight.Bold)
                    }
                    items(devis, key = { "devis-${it.id}" }) { record ->
                        DevisRow(record = record, onConvert = { viewModel.convertirEnCommande(record.id) })
                    }
                }
                if (commandes.isNotEmpty()) {
                    item(key = "commandes-heading") {
                        Text(stringResource(R.string.devis_orders_header), color = MissaInk, fontWeight = FontWeight.Bold)
                    }
                    items(commandes, key = { "commande-${it.id}" }) { record ->
                        CommandeRow(record)
                    }
                }
            }
        }
    }

    if (afficherCreation) {
        DevisCreationDialog(
            clients = clients,
            onDismiss = { afficherCreation = false },
            onCreate = { client, designation, prix ->
                if (viewModel.creerDevis(client, designation, prix)) afficherCreation = false
            },
        )
    }
}

@Composable
private fun CommandeRow(record: OperationRecordEntity) {
    val payload = remember(record.notes) { com.missa.b360.core.domain.model.SaleRecordCodec.decode(record.notes) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Iv.CheckCircle), contentDescription = null, tint = TendrePositive)
                Spacer(Modifier.width(8.dp))
                Text(record.reference, color = MissaInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(payload?.total?.let { java.text.NumberFormat.getNumberInstance().format(it) }.orEmpty(), color = MissaInk, fontWeight = FontWeight.Bold)
            }
            Text(record.counterpart.orEmpty(), color = MissaMuted, fontSize = 12.sp)
            payload?.lines?.forEach { line ->
                Text("• ${line.name}", color = MissaInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DevisRow(record: OperationRecordEntity, onConvert: () -> Unit) {
    val payload = remember(record.notes) { com.missa.b360.core.domain.model.SaleRecordCodec.decode(record.notes) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Iv.Description), contentDescription = null, tint = TendrePositive)
                Spacer(Modifier.width(8.dp))
                Text(record.reference, color = MissaInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(payload?.total?.let { java.text.NumberFormat.getNumberInstance().format(it) }.orEmpty(), color = MissaInk, fontWeight = FontWeight.Bold)
            }
            Text(record.counterpart.orEmpty(), color = MissaMuted, fontSize = 12.sp)
            payload?.lines?.forEach { line ->
                Text("• ${line.name}", color = MissaInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onConvert, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.devis_convert))
            }
        }
    }
}

@Composable
private fun DevisCreationDialog(
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onCreate: (ClientEntity?, String, String) -> Unit,
) {
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var selectionOuverte by remember { mutableStateOf(false) }
    var designation by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devis_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (clients.isEmpty()) {
                    Text(stringResource(R.string.devis_no_clients), color = MissaMuted, fontSize = 12.sp)
                }
                Box {
                    OutlinedButton(
                        onClick = { selectionOuverte = true },
                        enabled = clients.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = client?.nom ?: stringResource(R.string.devis_select_client),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(painterResource(Iv.ArrowDropDown), contentDescription = null)
                    }
                    DropdownMenu(expanded = selectionOuverte, onDismissRequest = { selectionOuverte = false }) {
                        clients.forEach { choix ->
                            DropdownMenuItem(
                                text = { Text(choix.nom) },
                                onClick = { client = choix; selectionOuverte = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.devis_line_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = prix,
                    onValueChange = { prix = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.devis_total)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(client, designation, prix) }) { Text(stringResource(R.string.devis_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devis_cancel)) }
        },
    )
}
