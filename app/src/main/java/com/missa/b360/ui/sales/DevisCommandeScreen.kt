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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.usecase.CommercialTarget
import com.missa.b360.core.domain.usecase.ConvertDevisToOrderUseCase
import com.missa.b360.core.domain.usecase.ConvertOrderToSaleUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ObservePaymentMethodsUseCase
import com.missa.b360.core.domain.usecase.ObserveTaxesUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaveDevisCommandeUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.core.util.toMoneyOrNull
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.TendrePositive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    observePaymentMethods: ObservePaymentMethodsUseCase,
    observeTaxes: ObserveTaxesUseCase,
    getEnterprise: GetEnterpriseUseCase,
    operations: OperationUseCases,
    private val saveDevis: SaveDevisCommandeUseCase,
    private val convertirDevis: ConvertDevisToOrderUseCase,
    private val convertirCommande: ConvertOrderToSaleUseCase,
) : ViewModel() {
    val clients: StateFlow<List<ClientEntity>> = observeClients()
        .map { list -> list.filter { it.statut == ClientStatus.ACTIF } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val moyensPaiement: StateFlow<List<String>> = observePaymentMethods()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tauxTaxe: StateFlow<Double> = observeTaxes()
        .map { taxes -> taxes.firstOrNull { it.parDefaut }?.taux ?: taxes.firstOrNull()?.taux ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)
    val devis: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.DEVIS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commandes: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.COMMANDE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val factures: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.VENTE)
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
            try {
                val ligneDevis = SaleLine(id = 1L, name = ligne, unitPrice = prix, quantity = 1.0)
                val totals = SaleCalculator.calculate(listOf(ligneDevis), 0.0, 0.0, tauxTaxe.value)
                val payload = SaleRecordPayload(
                    clientId = client.id,
                    clientName = client.nom,
                    lines = listOf(ligneDevis),
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    delivery = totals.delivery,
                    taxRate = tauxTaxe.value,
                    taxAmount = totals.taxAmount,
                    total = totals.total,
                    paymentMethod = "",
                    paidAmount = 0.0,
                )
                _feedback.value = when (saveDevis(CommercialTarget.Devis, null, payload)) {
                    is SaveDevisCommandeUseCase.Result.Succes -> R.string.devis_result_created
                    SaveDevisCommandeUseCase.Result.LectureSeule -> R.string.devis_result_readonly
                    else -> R.string.devis_error_line
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _feedback.value = R.string.devis_result_invalid
            }
        }
        return true
    }

    fun convertirEnCommande(devisId: Long) {
        viewModelScope.launch {
            try {
                _feedback.value = when (convertirDevis(devisId, CommercialTarget.Devis)) {
                    is ConvertDevisToOrderUseCase.Result.Succes -> R.string.devis_result_order
                    ConvertDevisToOrderUseCase.Result.DejaConverti -> R.string.devis_result_already_order
                    ConvertDevisToOrderUseCase.Result.LectureSeule -> R.string.devis_result_readonly
                    else -> R.string.devis_result_invalid
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _feedback.value = R.string.devis_result_invalid
            }
        }
    }

    fun facturerCommande(commandeId: Long, total: Double, moyenPaiement: String?, montantPayeText: String): Boolean {
        val methode = moyenPaiement?.trim().orEmpty()
        val montantPaye = montantPayeText.toMoneyOrNull()
        if (methode.isBlank()) {
            _feedback.value = R.string.devis_error_payment_method
            return false
        }
        if (montantPaye == null || montantPaye < 0.0 || montantPaye > total) {
            _feedback.value = R.string.devis_error_paid_amount
            return false
        }
        viewModelScope.launch {
            try {
                _feedback.value = when (val resultat = convertirCommande(commandeId, methode, montantPaye)) {
                    is ConvertOrderToSaleUseCase.Result.Succes -> R.string.devis_result_invoice
                    ConvertOrderToSaleUseCase.Result.DejaFacturee -> R.string.devis_result_already_invoice
                    ConvertOrderToSaleUseCase.Result.LectureSeule -> R.string.devis_result_readonly
                    is ConvertOrderToSaleUseCase.Result.StockInsuffisant -> R.string.devis_result_stock
                    else -> R.string.devis_result_invalid
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _feedback.value = R.string.devis_result_invalid
            }
        }
        return true
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
    val factures by viewModel.factures.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val moyensPaiement by viewModel.moyensPaiement.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    var afficherCreation by remember { mutableStateOf(openCreate) }
    var commandeAFacturer by remember { mutableStateOf<OperationRecordEntity?>(null) }
    val contexte = LocalContext.current
    val commandesDejaCreees = remember(commandes) {
        commandes.mapNotNull { SaleRecordCodec.decode(it.notes)?.sourceRecordId }.toSet()
    }
    val commandesDejaFacturees = remember(factures) {
        factures.mapNotNull { SaleRecordCodec.decode(it.notes)?.sourceRecordId }.toSet()
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            Toast.makeText(contexte, contexte.getString(it), Toast.LENGTH_SHORT).show()
            viewModel.effacerFeedback()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.devis_screen_title), onBack = onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.devis_page_desc),
                color = MissaMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
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
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (devis.isNotEmpty()) {
                    item(key = "devis-heading") {
                        Text(stringResource(R.string.devis_quotes_header), color = MissaInk, fontWeight = FontWeight.Bold)
                    }
                    items(devis, key = { "devis-${it.id}" }) { record ->
                        val payloadValide = remember(record.notes) { SaleRecordCodec.decode(record.notes) != null }
                        val annule = record.status == OperationStatus.CANCELLED.name
                        val converti = record.id in commandesDejaCreees
                        PieceCommercialeCard(
                            record = record,
                            currency = devise,
                            icon = Iv.Description,
                            actionLabel = if (annule || converti || !payloadValide) null else stringResource(R.string.devis_convert),
                            statusLabel = when {
                                annule -> stringResource(R.string.devis_cancelled_badge)
                                converti -> stringResource(R.string.devis_converted_badge)
                                !payloadValide -> stringResource(R.string.devis_unavailable_badge)
                                else -> null
                            },
                            onAction = { viewModel.convertirEnCommande(record.id) },
                        )
                    }
                }
                if (commandes.isNotEmpty()) {
                    item(key = "commandes-heading") {
                        Text(stringResource(R.string.devis_orders_header), color = MissaInk, fontWeight = FontWeight.Bold)
                    }
                    items(commandes, key = { "commande-${it.id}" }) { record ->
                        val payloadValide = remember(record.notes) { SaleRecordCodec.decode(record.notes) != null }
                        val annule = record.status == OperationStatus.CANCELLED.name
                        val facturee = record.id in commandesDejaFacturees
                        PieceCommercialeCard(
                            record = record,
                            currency = devise,
                            icon = Iv.CheckCircle,
                            actionLabel = if (annule || facturee || !payloadValide) null else stringResource(R.string.devis_create_invoice),
                            statusLabel = when {
                                annule -> stringResource(R.string.devis_cancelled_badge)
                                facturee -> stringResource(R.string.devis_invoiced_badge)
                                !payloadValide -> stringResource(R.string.devis_unavailable_badge)
                                else -> null
                            },
                            onAction = { commandeAFacturer = record },
                        )
                    }
                }
            }
        }
    }

    if (afficherCreation) {
        DevisCreationDialog(
            clients = clients,
            devise = devise,
            onDismiss = { afficherCreation = false },
            onCreate = { client, designation, prix ->
                if (viewModel.creerDevis(client, designation, prix)) afficherCreation = false
            },
        )
    }
    commandeAFacturer?.let { commande ->
        FacturerCommandeDialog(
            paymentMethods = moyensPaiement,
            currency = devise,
            total = commande.amount ?: 0.0,
            onDismiss = { commandeAFacturer = null },
            onInvoice = { method, amount ->
                if (viewModel.facturerCommande(commande.id, commande.amount ?: 0.0, method, amount)) commandeAFacturer = null
            },
        )
    }
}

@Composable
private fun PieceCommercialeCard(
    record: OperationRecordEntity,
    currency: String,
    icon: Int,
    actionLabel: String?,
    statusLabel: String?,
    onAction: () -> Unit,
) {
    val payload = remember(record.notes) { SaleRecordCodec.decode(record.notes) }
    val date = remember(record.createdAt) { DateUtils.formatDate(record.createdAt) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), contentDescription = null, tint = TendrePositive)
                Spacer(Modifier.width(8.dp))
                Text(record.reference, color = MissaInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    MoneyUtils.format(payload?.total ?: record.amount ?: 0.0, currency),
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Text(record.counterpart.orEmpty(), color = MissaMuted, fontSize = 12.sp)
            Text(date, color = MissaMuted, fontSize = 11.sp)
            payload?.lines?.forEach { line ->
                Text("• ${line.name}", color = MissaInk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (statusLabel != null) {
                Text(statusLabel, color = TendrePositive, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (actionLabel != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DevisCreationDialog(
    clients: List<ClientEntity>,
    devise: String,
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
                    label = { Text(stringResource(R.string.devis_total, devise)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

@Composable
private fun FacturerCommandeDialog(
    paymentMethods: List<String>,
    currency: String,
    total: Double,
    onDismiss: () -> Unit,
    onInvoice: (String?, String) -> Unit,
) {
    var methode by remember(paymentMethods) { mutableStateOf(paymentMethods.firstOrNull()) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var montantPaye by remember { mutableStateOf("0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devis_create_invoice)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.devis_invoice_total, MoneyUtils.format(total, currency)),
                    color = MissaMuted,
                    fontSize = 12.sp,
                )
                if (paymentMethods.isEmpty()) {
                    Text(stringResource(R.string.devis_no_payment_methods), color = MissaMuted, fontSize = 12.sp)
                }
                Box {
                    OutlinedButton(
                        onClick = { dropdownOpen = true },
                        enabled = paymentMethods.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            methode ?: stringResource(R.string.devis_select_payment_method),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(painterResource(Iv.ArrowDropDown), contentDescription = null)
                    }
                    DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                        paymentMethods.forEach { choix ->
                            DropdownMenuItem(
                                text = { Text(choix) },
                                onClick = { methode = choix; dropdownOpen = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = montantPaye,
                    onValueChange = { montantPaye = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.devis_paid_now, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onInvoice(methode, montantPaye) }, enabled = paymentMethods.isNotEmpty()) {
                Text(stringResource(R.string.devis_create_invoice))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devis_cancel)) }
        },
    )
}
