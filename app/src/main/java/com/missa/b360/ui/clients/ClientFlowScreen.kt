package com.missa.b360.ui.clients

import android.content.Context
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.usecase.ClientProfileInput
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

private enum class ClientView { LIST, DETAIL, FORM_INFO, FORM_CONTACTS, FORM_ADDRESSES, EDIT, HISTORY, ACCOUNT, SEARCH, DEACTIVATE }
private enum class ClientDetailTab { INFO, CONTACTS, ADDRESSES, NOTES }

private val ClientBlue = BrandBlue
private val ClientBlueSoft = MissaSoftBlue
private val ClientInk = MissaInk
private val ClientMuted = MissaMuted
private val ClientBorder = MissaBorder
private val ClientBackground = MissaCanvas
private val ClientGreen = Green60
private val ClientGreenSoft = Green90
private val ClientRed = Red40

private data class ClientDraft(
    val name: String = "",
    val type: ClientType = ClientType.PARTICULIER,
    val countryCode: String? = null,
    val phoneLocal: String = "",
    val email: String = "",
    val nif: String = "",
    val categoryId: Long? = null,
    val siteId: Long? = null,
    val commercial: String = "",
    val paymentDays: String = "30",
    val discount: String = "0",
    val creditLimit: String = "",
    val badgeId: Long? = null,
    val notes: String = "",
)

private data class ClientSalesMetrics(
    val salesTotal: Double = 0.0,
    val outstanding: Double = 0.0,
    val invoices: Int = 0,
)

/**
 * Parcours client relié à Room : liste, fiche, création en trois étapes, édition,
 * historique, état de compte, recherche avancée et désactivation. Aucun client,
 * contact, montant ou adresse de démonstration n'est injecté dans les vues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    onNavigate: (String) -> Unit = {},
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val clients by viewModel.clients.collectAsState(initial = emptyList())
    val categories by viewModel.categoriesFlow.collectAsState(initial = emptyList())
    val badges by viewModel.badgesFlow.collectAsState(initial = emptyList())
    val sites by viewModel.sitesFlow.collectAsState(initial = emptyList())
    val sales by viewModel.salesHistory.collectAsState(initial = emptyList())
    val devise by viewModel.deviseEntreprise.collectAsState()
    val defaultCountry by viewModel.codePaysParDefaut.collectAsState()
    val result by viewModel.resultat.collectAsState()
    val categoryError by viewModel.erreurCategorie.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var currentViewName by rememberSaveable(openCreate) {
        mutableStateOf(if (openCreate) ClientView.FORM_INFO.name else ClientView.LIST.name)
    }
    var selectedClientId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailTabName by rememberSaveable { mutableStateOf(ClientDetailTab.INFO.name) }
    var wizardClientId by rememberSaveable { mutableStateOf<Long?>(null) }
    var replaceRelations by rememberSaveable { mutableStateOf(true) }
    val selectedClient = clients.firstOrNull { it.id == selectedClientId }
    val contactsFlow = remember(selectedClientId) { viewModel.contacts(selectedClientId) }
    val addressesFlow = remember(selectedClientId) { viewModel.addresses(selectedClientId) }
    val detailContacts by contactsFlow.collectAsState(initial = emptyList())
    val detailAddresses by addressesFlow.collectAsState(initial = emptyList())
    val draftContacts = remember { mutableStateListOf<ClientContactEntity>() }
    val draftAddresses = remember { mutableStateListOf<ClientAddressEntity>() }
    var draft by remember { mutableStateOf(ClientDraft(countryCode = defaultCountry)) }
    var activeFilter by rememberSaveable { mutableStateOf("ALL") }
    val currentView = runCatching { ClientView.valueOf(currentViewName) }.getOrDefault(ClientView.LIST)
    val detailTab = runCatching { ClientDetailTab.valueOf(detailTabName) }.getOrDefault(ClientDetailTab.INFO)
    val metrics = remember(sales) { saleMetricsByClient(sales) }

    fun startNewClient() {
        selectedClientId = null
        wizardClientId = null
        replaceRelations = true
        draft = ClientDraft(countryCode = defaultCountry)
        draftContacts.clear()
        draftAddresses.clear()
        currentViewName = ClientView.FORM_INFO.name
    }

    fun startEdit(client: ClientEntity, destination: ClientView = ClientView.EDIT) {
        wizardClientId = client.id
        replaceRelations = destination != ClientView.EDIT
        draft = client.toDraft(defaultCountry)
        draftContacts.clear()
        draftContacts.addAll(detailContacts)
        draftAddresses.clear()
        draftAddresses.addAll(detailAddresses.ifEmpty { client.fallbackAddress() })
        currentViewName = destination.name
    }

    fun submitDraft() {
        val countryPrefix = Iso4217.indicatifTelephone(draft.countryCode)
        val telephone = ClientValidation.telephoneAvecIndicatif(draft.phoneLocal, countryPrefix)
        val discount = draft.discount.decimalValue() ?: -1.0
        val limit = draft.creditLimit.trim().takeIf { it.isNotEmpty() }?.decimalValue()
        val terms = draft.paymentDays.toIntOrNull() ?: -1
        val mainAddress = draftAddresses.firstOrNull { it.principale }
            ?: draftAddresses.firstOrNull()
        val profile = ClientProfileInput(
            nif = draft.nif,
            commercial = draft.commercial,
            conditionPaiementJours = terms,
            contacts = draftContacts.toList().withOnePrimaryContact(),
            addresses = draftAddresses.toList().withOnePrimaryAddress(),
            replaceRelations = replaceRelations,
        )
        if (wizardClientId == null) {
            viewModel.creer(
                nom = draft.name,
                telephone = telephone,
                type = draft.type,
                email = draft.email.ifBlank { null },
                adresse = mainAddress?.displayAddress(),
                categorieId = draft.categoryId,
                siteId = draft.siteId,
                remiseDefautPct = discount,
                limiteCredit = limit,
                badgeId = draft.badgeId,
                notes = draft.notes.ifBlank { null },
                profile = profile,
            )
        } else {
            viewModel.modifier(
                id = wizardClientId!!,
                nom = draft.name,
                telephone = telephone,
                type = draft.type,
                email = draft.email.ifBlank { null },
                adresse = mainAddress?.displayAddress(),
                categorieId = draft.categoryId,
                siteId = draft.siteId,
                remiseDefautPct = discount,
                limiteCredit = limit,
                badgeId = draft.badgeId,
                notes = draft.notes.ifBlank { null },
                profile = profile,
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importer(context.readClientCsv(uri))
    }
    val accountPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val client = selectedClient
        if (uri != null && client != null) context.writeClientAccountPdf(uri, client, sales, devise.orEmpty())
    }

    LaunchedEffect(defaultCountry) {
        if (draft.countryCode == null && defaultCountry != null) draft = draft.copy(countryCode = defaultCountry)
    }
    LaunchedEffect(result) {
        val outcome = result ?: return@LaunchedEffect
        when {
            outcome.erreur == "doublon" -> Unit
            outcome.imports != null -> {
                snackbar.showSnackbar(
                    context.getString(R.string.clients_flow_import_result, outcome.imports, outcome.ignoredImports),
                )
                viewModel.acquitterResultat()
            }
            outcome.code != null -> {
                snackbar.showSnackbar(
                    if (outcome.code == "edit") context.getString(R.string.clients_client_modifie)
                    else context.getString(R.string.clients_client_cree, outcome.code),
                )
                if (outcome.clientId != null) {
                    selectedClientId = outcome.clientId
                    currentViewName = ClientView.DETAIL.name
                } else if (selectedClientId != null) {
                    currentViewName = ClientView.DETAIL.name
                } else {
                    currentViewName = ClientView.LIST.name
                }
                viewModel.acquitterResultat()
            }
            else -> {
                val message = when (outcome.erreur) {
                    "licence" -> R.string.clients_lecture_seule
                    "nom" -> R.string.clients_nom_obligatoire
                    "nom_invalide" -> R.string.clients_nom_invalide
                    "telephone" -> R.string.clients_telephone_obligatoire
                    "telephone_invalide" -> R.string.clients_telephone_invalide
                    "email_invalide" -> R.string.clients_email_invalide
                    "import" -> R.string.clients_flow_import_error
                    else -> R.string.clients_erreur_sauvegarde
                }
                snackbar.showSnackbar(context.getString(message))
                viewModel.acquitterResultat()
            }
        }
    }
    LaunchedEffect(currentView, selectedClientId) {
        if (currentView in setOf(ClientView.DETAIL, ClientView.HISTORY, ClientView.ACCOUNT, ClientView.DEACTIVATE) && selectedClientId == null) {
            currentViewName = ClientView.LIST.name
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (currentView) {
            ClientView.LIST -> ClientListScreen(
                clients = clients,
                categories = categories,
                metrics = metrics,
                devise = devise.orEmpty(),
                activeFilter = activeFilter,
                onFilterChange = { activeFilter = it },
                onBack = onBack,
                onNew = ::startNewClient,
                onImport = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) },
                onSearch = { currentViewName = ClientView.SEARCH.name },
                onNavigate = onNavigate,
                onOpen = {
                    selectedClientId = it.id
                    detailTabName = ClientDetailTab.INFO.name
                    currentViewName = ClientView.DETAIL.name
                },
            )
            ClientView.DETAIL -> selectedClient?.let { client ->
                ClientDetailScreen(
                    client = client,
                    contacts = detailContacts,
                    addresses = detailAddresses.ifEmpty { client.fallbackAddress() },
                    category = categories.firstOrNull { it.id == client.categorieId },
                    badge = badges.firstOrNull { it.id == client.badgeId },
                    metrics = metrics[client.id] ?: ClientSalesMetrics(),
                    devise = devise.orEmpty(),
                    tab = detailTab,
                    onTabChange = { detailTabName = it.name },
                    onBack = { currentViewName = ClientView.LIST.name },
                    onEdit = { startEdit(client) },
                    onManageContacts = { startEdit(client, ClientView.FORM_CONTACTS) },
                    onManageAddresses = { startEdit(client, ClientView.FORM_ADDRESSES) },
                    onHistory = { currentViewName = ClientView.HISTORY.name },
                    onAccount = { currentViewName = ClientView.ACCOUNT.name },
                    onDeactivate = { currentViewName = ClientView.DEACTIVATE.name },
                )
            }
            ClientView.FORM_INFO -> ClientInfoFormScreen(
                draft = draft,
                onDraftChange = { draft = it },
                categories = categories,
                sites = sites,
                title = R.string.clients_nouveau,
                countryDefault = defaultCountry,
                onBack = { currentViewName = ClientView.LIST.name },
                onNext = { currentViewName = ClientView.FORM_CONTACTS.name },
            )
            ClientView.FORM_CONTACTS -> ClientContactsFormScreen(
                contacts = draftContacts,
                isEdit = wizardClientId != null,
                onBack = { currentViewName = if (wizardClientId == null) ClientView.FORM_INFO.name else ClientView.DETAIL.name },
                onNext = { currentViewName = ClientView.FORM_ADDRESSES.name },
            )
            ClientView.FORM_ADDRESSES -> ClientAddressesFormScreen(
                draft = draft,
                onDraftChange = { draft = it },
                addresses = draftAddresses,
                badges = badges,
                devise = devise.orEmpty(),
                isEdit = wizardClientId != null,
                onBack = { currentViewName = ClientView.FORM_CONTACTS.name },
                onSave = ::submitDraft,
            )
            ClientView.EDIT -> ClientInfoFormScreen(
                draft = draft,
                onDraftChange = { draft = it },
                categories = categories,
                sites = sites,
                title = R.string.clients_modifier,
                countryDefault = defaultCountry,
                onBack = { currentViewName = ClientView.DETAIL.name },
                onNext = ::submitDraft,
                saveMode = true,
            )
            ClientView.HISTORY -> selectedClient?.let { client ->
                ClientHistoryScreen(
                    client = client,
                    records = sales.salesFor(client.id),
                    devise = devise.orEmpty(),
                    onBack = { currentViewName = ClientView.DETAIL.name },
                )
            }
            ClientView.ACCOUNT -> selectedClient?.let { client ->
                ClientAccountScreen(
                    client = client,
                    records = sales.salesFor(client.id),
                    devise = devise.orEmpty(),
                    onBack = { currentViewName = ClientView.DETAIL.name },
                    onDownload = { accountPdfLauncher.launch("${client.code}-compte.pdf") },
                )
            }
            ClientView.SEARCH -> ClientSearchScreen(
                clients = clients,
                categories = categories,
                metrics = metrics,
                devise = devise.orEmpty(),
                onBack = { currentViewName = ClientView.LIST.name },
                onOpen = {
                    selectedClientId = it.id
                    currentViewName = ClientView.DETAIL.name
                },
            )
            ClientView.DEACTIVATE -> selectedClient?.let { client ->
                ClientDeactivateScreen(
                    client = client,
                    outstanding = metrics[client.id]?.outstanding ?: 0.0,
                    devise = devise.orEmpty(),
                    onBack = { currentViewName = ClientView.DETAIL.name },
                    onConfirm = {
                        viewModel.desactiver(client.id)
                        currentViewName = ClientView.LIST.name
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp),
        )
    }

    if (result?.erreur == "doublon") {
        AlertDialog(
            onDismissRequest = viewModel::annulerDoublon,
            title = { Text(stringResource(R.string.clients_doublon_titre)) },
            text = { Text(stringResource(R.string.clients_doublon_message)) },
            confirmButton = { Button(onClick = viewModel::confirmerDoublon) { Text(stringResource(R.string.clients_creer_malgre_doublon)) } },
            dismissButton = { TextButton(onClick = viewModel::annulerDoublon) { Text(stringResource(R.string.ops_cancel)) } },
        )
    }
    if (categoryError != null) {
        val message = when (categoryError) {
            "utilisee" -> R.string.clients_categorie_utilisee
            "licence" -> R.string.clients_lecture_seule
            else -> R.string.clients_erreur_sauvegarde
        }
        LaunchedEffect(categoryError) {
            snackbar.showSnackbar(context.getString(message))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientListScreen(
    clients: List<ClientEntity>,
    categories: List<CategoryClientEntity>,
    metrics: Map<Long, ClientSalesMetrics>,
    devise: String,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onSearch: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpen: (ClientEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = clients.filter { client ->
        val matchesQuery = query.isBlank() || client.nom.contains(query, true) || client.code.contains(query, true) || client.telephone.contains(query)
        val matchesStatus = when (activeFilter) {
            "ACTIVE" -> client.isActive()
            "INACTIVE" -> !client.isActive()
            "DUE" -> (metrics[client.id]?.outstanding ?: 0.0) > 0.0
            else -> true
        }
        matchesQuery && matchesStatus
    }
    val activeCount = clients.count { it.isActive() }
    val dueTotal = metrics.values.sumOf { it.outstanding }
    Scaffold(
        containerColor = ClientBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { ClientPageTitle(stringResource(R.string.clients_flow_list_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back), tint = ClientInk) } },
                actions = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.clients_flow_search), tint = ClientInk) } },
            )
        },
        bottomBar = { ClientBottomBar(onNavigate) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onNew, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(7.dp), colors = ButtonDefaults.buttonColors(containerColor = ClientBlue)) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.clients_nouveau), fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(7.dp)) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.clients_flow_import), fontSize = 11.sp)
                    }
                }
            }
            item {
                ClientStats(clients.size, activeCount, dueTotal, devise)
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.clients_recherche), fontSize = 12.sp) },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = activeFilter == "ALL", onClick = { onFilterChange("ALL") }, label = { Text(stringResource(R.string.clients_flow_all, clients.size), fontSize = 10.sp) }) }
                    item { FilterChip(selected = activeFilter == "ACTIVE", onClick = { onFilterChange("ACTIVE") }, label = { Text(stringResource(R.string.clients_flow_active, activeCount), fontSize = 10.sp) }) }
                    item { FilterChip(selected = activeFilter == "INACTIVE", onClick = { onFilterChange("INACTIVE") }, label = { Text(stringResource(R.string.clients_flow_inactive, clients.size - activeCount), fontSize = 10.sp) }) }
                    item { FilterChip(selected = activeFilter == "DUE", onClick = { onFilterChange("DUE") }, label = { Text(stringResource(R.string.clients_flow_due), fontSize = 10.sp) }) }
                }
            }
            if (visible.isEmpty()) {
                item { ClientEmptyList(onNew) }
            } else {
                items(visible, key = { it.id }) { client ->
                    ClientListRow(
                        client = client,
                        category = categories.firstOrNull { it.id == client.categorieId },
                        outstanding = metrics[client.id]?.outstanding ?: 0.0,
                        devise = devise,
                        onOpen = { onOpen(client) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientStats(total: Int, active: Int, due: Double, devise: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        ClientStatCard(stringResource(R.string.clients_flow_total_clients), total.toString(), Modifier.weight(1f))
        ClientStatCard(stringResource(R.string.clients_flow_active_short), active.toString(), Modifier.weight(1f))
        ClientStatCard(stringResource(R.string.clients_flow_outstanding), clientMoney(due, devise), Modifier.weight(1.35f))
    }
}

@Composable
private fun ClientStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = ClientMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClientEmptyList(onNew: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder)) {
        Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = ClientBlueSoft) { Icon(Icons.Outlined.PersonOutline, null, tint = ClientBlue, modifier = Modifier.padding(11.dp)) }
            Text(stringResource(R.string.clients_flow_empty_title), color = ClientInk, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.clients_flow_empty_description), color = ClientMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            TextButton(onClick = onNew) { Text(stringResource(R.string.clients_nouveau)) }
        }
    }
}

@Composable
private fun ClientListRow(client: ClientEntity, category: CategoryClientEntity?, outstanding: Double, devise: String, onOpen: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(11.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            ClientAvatar(client.nom, Modifier.size(34.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(client.nom, color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${client.code} · ${client.telephone}", color = ClientMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                category?.let { Text(it.nom, color = ClientBlue, fontSize = 9.sp) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (outstanding > 0) clientMoney(outstanding, devise) else "—", color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                ClientStatusChip(client)
            }
        }
    }
}

@Composable
private fun ClientAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = ClientBlue) {
        Box(contentAlignment = Alignment.Center) { Text(name.initials(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

@Composable
private fun ClientStatusChip(client: ClientEntity) {
    val active = client.isActive()
    AssistChip(
        onClick = {}, enabled = false,
        label = { Text(stringResource(if (active) R.string.clients_actif else R.string.clients_inactif), fontSize = 8.sp) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            disabledContainerColor = (if (active) ClientGreen else ClientRed).copy(alpha = .10f),
            disabledLabelColor = if (active) ClientGreen else ClientRed,
        ),
        border = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailScreen(
    client: ClientEntity,
    contacts: List<ClientContactEntity>,
    addresses: List<ClientAddressEntity>,
    category: CategoryClientEntity?,
    badge: BadgeLoyaltyEntity?,
    metrics: ClientSalesMetrics,
    devise: String,
    tab: ClientDetailTab,
    onTabChange: (ClientDetailTab) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onManageContacts: () -> Unit,
    onManageAddresses: () -> Unit,
    onHistory: () -> Unit,
    onAccount: () -> Unit,
    onDeactivate: () -> Unit,
) {
    Scaffold(
        containerColor = ClientBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { ClientPageTitle(stringResource(R.string.clients_flow_detail_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back), tint = ClientInk) } },
                actions = {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.clients_flow_edit), fontSize = 11.sp) }
                    IconButton(onClick = onDeactivate) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.clients_desactiver), tint = ClientInk) }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClientAvatar(client.nom, Modifier.size(48.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(client.nom, color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${client.code} · ${stringResource(client.type.labelRes())}", color = ClientMuted, fontSize = 10.sp)
                    }
                    ClientStatusChip(client)
                }
            }
            item { ClientDetailInfoCard(client, addresses, onHistory, onAccount, metrics, devise) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ClientMetricCard(stringResource(R.string.clients_flow_sales_total), clientMoney(metrics.salesTotal, devise), stringResource(R.string.clients_flow_history), onHistory, Modifier.weight(1f))
                    ClientMetricCard(stringResource(R.string.clients_flow_outstanding), clientMoney(metrics.outstanding, devise), stringResource(R.string.clients_flow_account), onAccount, Modifier.weight(1f))
                }
            }
            item { ClientTabRow(tab, contacts.size, addresses.size, onTabChange) }
            when (tab) {
                ClientDetailTab.INFO -> item { ClientDetailAttributes(client, category, badge, devise) }
                ClientDetailTab.CONTACTS -> item { ClientContactsSection(contacts, onManageContacts) }
                ClientDetailTab.ADDRESSES -> item { ClientAddressesSection(addresses, onManageAddresses) }
                ClientDetailTab.NOTES -> item { ClientNotesSection(client.notes, onEdit) }
            }
        }
    }
}

@Composable
private fun ClientDetailInfoCard(client: ClientEntity, addresses: List<ClientAddressEntity>, onHistory: () -> Unit, onAccount: () -> Unit, metrics: ClientSalesMetrics, devise: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(stringResource(R.string.clients_flow_general_information), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            ClientKeyValue(R.string.clients_telephone, client.telephone)
            ClientKeyValue(R.string.clients_email, client.email ?: "—")
            ClientKeyValue(R.string.clients_adresse, addresses.firstOrNull { it.principale }?.displayAddress() ?: client.adresse ?: "—")
            ClientKeyValue(R.string.clients_flow_nif, client.nif ?: "—")
            ClientKeyValue(R.string.clients_flow_registered, DateUtils.formatDate(client.createdAt))
            ClientKeyValue(R.string.clients_flow_sales_rep, client.commercial ?: "—")
            if (metrics.outstanding > 0.0) Text(stringResource(R.string.clients_flow_customer_due, clientMoney(metrics.outstanding, devise)), color = ClientRed, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TextButton(onClick = onHistory, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clients_flow_history), fontSize = 10.sp) }
                TextButton(onClick = onAccount, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clients_flow_account), fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun ClientKeyValue(label: Int, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(stringResource(label), color = ClientMuted, fontSize = 10.sp, modifier = Modifier.weight(0.42f))
        Text(value, color = ClientInk, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ClientMetricCard(title: String, amount: String, action: String, onAction: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = ClientMuted, fontSize = 9.sp)
            Text(amount, color = ClientBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) { Text(action, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun ClientTabRow(selected: ClientDetailTab, contacts: Int, addresses: Int, onSelect: (ClientDetailTab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        item { ClientTab(selected, ClientDetailTab.INFO, stringResource(R.string.clients_flow_information), onSelect) }
        item { ClientTab(selected, ClientDetailTab.CONTACTS, stringResource(R.string.clients_flow_contacts_count, contacts), onSelect) }
        item { ClientTab(selected, ClientDetailTab.ADDRESSES, stringResource(R.string.clients_flow_addresses_count, addresses), onSelect) }
        item { ClientTab(selected, ClientDetailTab.NOTES, stringResource(R.string.clients_notes), onSelect) }
    }
}

@Composable
private fun ClientTab(current: ClientDetailTab, value: ClientDetailTab, label: String, onSelect: (ClientDetailTab) -> Unit) {
    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun ClientDetailAttributes(client: ClientEntity, category: CategoryClientEntity?, badge: BadgeLoyaltyEntity?, devise: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ClientKeyValue(R.string.clients_type, stringResource(client.type.labelRes()))
            ClientKeyValue(R.string.clients_categories, category?.nom ?: "—")
            ClientKeyValue(R.string.clients_flow_payment_terms, stringResource(R.string.clients_flow_days_value, client.conditionPaiementJours))
            ClientKeyValue(R.string.clients_limite_credit, client.limiteCredit?.let { clientMoney(it, devise) } ?: stringResource(R.string.clients_flow_unlimited))
            ClientKeyValue(R.string.clients_remise, "${client.remiseDefautPct.decimalText()} %")
            ClientKeyValue(R.string.clients_badges, badge?.nom ?: "—")
            ClientKeyValue(R.string.clients_devise, devise.ifBlank { "—" })
        }
    }
}

@Composable
private fun ClientContactsSection(contacts: List<ClientContactEntity>, onManage: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.clients_flow_contacts), modifier = Modifier.weight(1f), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(onClick = onManage, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp)); Text(stringResource(R.string.clients_flow_manage), fontSize = 9.sp) }
            }
            if (contacts.isEmpty()) Text(stringResource(R.string.clients_flow_no_contacts), color = ClientMuted, fontSize = 11.sp)
            contacts.forEachIndexed { index, contact ->
                if (index > 0) HorizontalDivider(color = ClientBorder)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row { Text(contact.nom, modifier = Modifier.weight(1f), color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); if (contact.principal) ClientPill(R.string.clients_flow_primary) }
                    contact.fonction?.let { Text(it, color = ClientMuted, fontSize = 10.sp) }
                    contact.telephone?.let { Text(it, color = ClientMuted, fontSize = 10.sp) }
                    contact.email?.let { Text(it, color = ClientBlue, fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun ClientAddressesSection(addresses: List<ClientAddressEntity>, onManage: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.clients_flow_addresses), modifier = Modifier.weight(1f), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(onClick = onManage, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp)); Text(stringResource(R.string.clients_flow_manage), fontSize = 9.sp) }
            }
            if (addresses.isEmpty()) Text(stringResource(R.string.clients_flow_no_addresses), color = ClientMuted, fontSize = 11.sp)
            addresses.forEachIndexed { index, address ->
                if (index > 0) HorizontalDivider(color = ClientBorder)
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row { Text(address.libelle.ifBlank { stringResource(R.string.clients_adresse) }, color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); if (address.principale) { Spacer(Modifier.width(6.dp)); ClientPill(R.string.clients_flow_primary) } }
                        Text(address.displayAddress(), color = ClientMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientNotesSection(notes: String?, onEdit: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(stringResource(R.string.clients_notes), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(notes?.takeIf { it.isNotBlank() } ?: stringResource(R.string.clients_flow_no_notes), color = ClientMuted, fontSize = 11.sp)
            TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.clients_flow_edit), fontSize = 10.sp) }
        }
    }
}

@Composable
private fun ClientPill(label: Int) {
    Surface(shape = RoundedCornerShape(5.dp), color = ClientGreenSoft) { Text(stringResource(label), color = ClientGreen, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientInfoFormScreen(
    draft: ClientDraft,
    onDraftChange: (ClientDraft) -> Unit,
    categories: List<CategoryClientEntity>,
    sites: List<SiteEntity>,
    title: Int,
    countryDefault: String?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    saveMode: Boolean = false,
) {
    val fullPhone = ClientValidation.telephoneAvecIndicatif(draft.phoneLocal, Iso4217.indicatifTelephone(draft.countryCode))
    val valid = draft.countryCode != null && ClientValidation.nomEstValide(draft.name) &&
        ClientValidation.telephoneEstValide(fullPhone) && ClientValidation.emailEstValide(draft.email)
    ClientWizardScaffold(
        title = title,
        step = if (saveMode) null else 0,
        onBack = onBack,
        primaryLabel = if (saveMode) R.string.clients_enregistrer else R.string.clients_flow_next,
        enabled = valid,
        onPrimary = onNext,
    ) {
        LazyColumn(contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Text(stringResource(R.string.clients_flow_general_information), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(stringResource(R.string.clients_flow_required_hint), color = ClientMuted, fontSize = 10.sp)
            }
            item { ClientTypeChoice(draft.type, onSelect = { onDraftChange(draft.copy(type = it)) }) }
            item {
                OutlinedTextField(value = draft.name, onValueChange = { onDraftChange(draft.copy(name = it.take(ClientValidation.LONGUEUR_NOM_MAX))) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_company_name)) }, isError = draft.name.isNotEmpty() && !ClientValidation.nomEstValide(draft.name), singleLine = true)
            }
            item { ClientReadOnlyLine(R.string.clients_flow_client_code, stringResource(R.string.clients_flow_generated_on_save)) }
            item {
                OutlinedTextField(value = draft.nif, onValueChange = { onDraftChange(draft.copy(nif = it.take(80))) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_nif)) }, singleLine = true)
            }
            item {
                ClientPhoneField(
                    countryCode = draft.countryCode ?: countryDefault,
                    phoneLocal = draft.phoneLocal,
                    onCountryCode = { code -> onDraftChange(draft.copy(countryCode = code)) },
                    onPhone = { number -> onDraftChange(draft.copy(phoneLocal = ClientValidation.filtrerTelephoneLocalPourSaisie(number).take(25))) },
                    isError = draft.phoneLocal.isNotEmpty() && !ClientValidation.telephoneEstValide(fullPhone),
                )
            }
            item {
                OutlinedTextField(value = draft.email, onValueChange = { onDraftChange(draft.copy(email = it.take(ClientValidation.LONGUEUR_EMAIL_MAX))) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_email)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = draft.email.isNotEmpty() && !ClientValidation.emailEstValide(draft.email))
            }
            item { ClientSelectorField(R.string.clients_categorie_optionnelle, categories.map { it.id to it.nom }, draft.categoryId, { onDraftChange(draft.copy(categoryId = it)) }, stringResource(R.string.clients_aucune_categorie)) }
            item { ClientSelectorField(R.string.clients_site, sites.map { it.id to it.nom }, draft.siteId, { onDraftChange(draft.copy(siteId = it)) }, stringResource(R.string.clients_aucun_site)) }
        }
    }
}

@Composable
private fun ClientTypeChoice(selected: ClientType, onSelect: (ClientType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.clients_type), color = ClientMuted, fontSize = 10.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ClientType.entries) { type -> FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(stringResource(type.labelRes()), fontSize = 10.sp) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientSelectorField(label: Int, choices: List<Pair<Long, String>>, selectedId: Long?, onSelect: (Long?) -> Unit, emptyLabel: String) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.first == selectedId }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), label = { Text(stringResource(label)) }, placeholder = { Text(emptyLabel) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { onSelect(null); expanded = false })
            choices.forEach { (id, name) -> androidx.compose.material3.DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPhoneField(countryCode: String?, phoneLocal: String, onCountryCode: (String) -> Unit, onPhone: (String) -> Unit, isError: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val countries = remember(locale) { Iso4217.paysAvecIndicatif(locale) }
    val selected = countries.firstOrNull { it.code == countryCode }
    var pickerVisible by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        OutlinedButton(onClick = { pickerVisible = true }, modifier = Modifier.width(110.dp).height(56.dp)) { Text(selected?.indicatif ?: "…", fontSize = 13.sp); Icon(Icons.Outlined.ArrowDropDown, null, modifier = Modifier.size(16.dp)) }
        OutlinedTextField(value = phoneLocal, onValueChange = onPhone, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.clients_telephone)) }, singleLine = true, isError = isError || countryCode == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    }
    if (pickerVisible) {
        var query by rememberSaveable { mutableStateOf("") }
        val visible = countries.filter { query.isBlank() || it.nom.contains(query, true) || it.indicatif.contains(query) || it.code.contains(query, true) }
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { pickerVisible = false }, containerColor = Color.White) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp)) {
                Text(stringResource(R.string.clients_indicatif_pays), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.clients_rechercher_indicatif)) }, singleLine = true)
                LazyColumn(modifier = Modifier.height(330.dp)) { items(visible, key = { it.code }) { country -> Row(modifier = Modifier.fillMaxWidth().clickable { onCountryCode(country.code); pickerVisible = false }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Text(country.indicatif, color = ClientBlue, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp)); Text("${country.nom} (${country.code})", color = ClientInk, fontSize = 12.sp); if (country.code == countryCode) { Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Check, null, tint = ClientGreen) } } } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientContactsFormScreen(contacts: MutableList<ClientContactEntity>, isEdit: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    var editing by remember { mutableStateOf<ClientContactEntity?>(null) }
    ClientWizardScaffold(
        title = if (isEdit) R.string.clients_modifier else R.string.clients_nouveau,
        step = 1,
        onBack = onBack,
        primaryLabel = R.string.clients_flow_next,
        enabled = true,
        onPrimary = onNext,
    ) {
        LazyColumn(contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(stringResource(R.string.clients_flow_contacts), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(stringResource(R.string.clients_flow_contacts_hint), color = ClientMuted, fontSize = 10.sp) }
                    OutlinedButton(onClick = { editing = ClientContactEntity(clientId = 0, nom = "", principal = contacts.isEmpty()) }) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp)); Text(stringResource(R.string.clients_flow_add_contact), fontSize = 10.sp) }
                }
            }
            if (contacts.isEmpty()) item { ClientFormEmpty(R.string.clients_flow_no_contacts) }
            items(contacts, key = { it.id to it.nom }) { contact ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { editing = contact }, shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Row { Text(contact.nom, color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); if (contact.principal) { Spacer(Modifier.width(6.dp)); ClientPill(R.string.clients_flow_primary) } }; Text(listOfNotNull(contact.fonction, contact.telephone, contact.email).joinToString(" · "), color = ClientMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        IconButton(onClick = { contacts.remove(contact) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_remove_item), tint = ClientRed, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
        }
    }
    editing?.let { contact ->
        ClientContactDialog(contact = contact, onDismiss = { editing = null }, onSave = { value ->
            val index = contacts.indexOfFirst { it === contact || (it.id == contact.id && it.nom == contact.nom) }
            val normalised = if (value.principal) value else value.copy(principal = contacts.none { it !== contact && it.principal })
            if (normalised.principal) {
                contacts.indices.forEach { itemIndex ->
                    val item = contacts[itemIndex]
                    contacts[itemIndex] = if (item === contact || (item.id == contact.id && item.nom == contact.nom)) normalised else item.copy(principal = false)
                }
            } else if (index >= 0) contacts[index] = normalised else contacts.add(normalised)
            if (index < 0 && normalised !in contacts) contacts.add(normalised)
            editing = null
        })
    }
}

@Composable
private fun ClientFormEmpty(label: Int) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = ClientBlueSoft) { Text(stringResource(label), color = ClientMuted, fontSize = 11.sp, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
}

@Composable
private fun ClientContactDialog(contact: ClientContactEntity, onDismiss: () -> Unit, onSave: (ClientContactEntity) -> Unit) {
    var name by remember(contact.id, contact.nom) { mutableStateOf(contact.nom) }
    var role by remember(contact.id, contact.nom) { mutableStateOf(contact.fonction.orEmpty()) }
    var phone by remember(contact.id, contact.nom) { mutableStateOf(contact.telephone.orEmpty()) }
    var email by remember(contact.id, contact.nom) { mutableStateOf(contact.email.orEmpty()) }
    var primary by remember(contact.id, contact.nom) { mutableStateOf(contact.principal) }
    val valid = ClientValidation.nomEstValide(name) && (phone.isBlank() || ClientValidation.telephoneEstValide(phone)) && ClientValidation.emailEstValide(email)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clients_flow_contact)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_nom)) }, singleLine = true)
            OutlinedTextField(value = role, onValueChange = { role = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_role)) }, singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { phone = ClientValidation.filtrerTelephonePourSaisie(it).take(25) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_telephone)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it.take(254) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            FilterChip(selected = primary, onClick = { primary = !primary }, label = { Text(stringResource(R.string.clients_flow_primary)) })
        } },
        confirmButton = { Button(onClick = { onSave(contact.copy(nom = name, fonction = role.ifBlank { null }, telephone = phone.ifBlank { null }, email = email.ifBlank { null }, principal = primary)) }, enabled = valid) { Text(stringResource(R.string.clients_enregistrer)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientAddressesFormScreen(
    draft: ClientDraft,
    onDraftChange: (ClientDraft) -> Unit,
    addresses: MutableList<ClientAddressEntity>,
    badges: List<BadgeLoyaltyEntity>,
    devise: String,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    var editing by remember { mutableStateOf<ClientAddressEntity?>(null) }
    val terms = draft.paymentDays.toIntOrNull()
    val discount = draft.discount.decimalValue()
    val credit = draft.creditLimit.trim().takeIf { it.isNotEmpty() }?.decimalValue()
    val valid = terms != null && terms in 0..365 && discount != null && discount in 0.0..100.0 && (credit == null || credit >= 0.0)
    ClientWizardScaffold(
        title = if (isEdit) R.string.clients_modifier else R.string.clients_nouveau,
        step = 2,
        onBack = onBack,
        primaryLabel = R.string.clients_enregistrer,
        enabled = valid,
        onPrimary = onSave,
    ) {
        LazyColumn(contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.clients_flow_addresses), modifier = Modifier.weight(1f), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedButton(onClick = { editing = ClientAddressEntity(clientId = 0, adresse = "", principale = addresses.isEmpty()) }) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp)); Text(stringResource(R.string.clients_flow_add_address), fontSize = 10.sp) }
                }
            }
            if (addresses.isEmpty()) item { ClientFormEmpty(R.string.clients_flow_no_addresses) }
            items(addresses, key = { it.id to it.adresse }) { address ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { editing = address }, shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Row { Text(address.libelle.ifBlank { stringResource(R.string.clients_adresse) }, color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); if (address.principale) { Spacer(Modifier.width(6.dp)); ClientPill(R.string.clients_flow_primary) } }; Text(address.displayAddress(), color = ClientMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        IconButton(onClick = { addresses.remove(address) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.sales_remove_item), tint = ClientRed, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
            item { HorizontalDivider(color = ClientBorder) }
            item { Text(stringResource(R.string.clients_flow_other_information), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            item {
                OutlinedTextField(value = draft.commercial, onValueChange = { onDraftChange(draft.copy(commercial = it.take(120))) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_sales_rep)) }, singleLine = true)
            }
            item {
                OutlinedTextField(value = draft.paymentDays, onValueChange = { onDraftChange(draft.copy(paymentDays = it.filter(Char::isDigit).take(3))) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_payment_terms)) }, suffix = { Text(stringResource(R.string.clients_flow_days_short), fontSize = 10.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = draft.paymentDays.isNotEmpty() && (terms == null || terms !in 0..365))
            }
            item { OutlinedTextField(value = draft.discount, onValueChange = { onDraftChange(draft.copy(discount = it.decimalInput())) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_remise)) }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = discount != null && discount !in 0.0..100.0) }
            item { OutlinedTextField(value = draft.creditLimit, onValueChange = { onDraftChange(draft.copy(creditLimit = it.decimalInput())) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_limite_credit)) }, suffix = { Text(devise, fontSize = 10.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = credit != null && credit < 0) }
            item { ClientSelectorField(R.string.clients_badge_optionnel, badges.map { it.id to "${it.nom} (${it.remisePct.decimalText()}%)" }, draft.badgeId, { onDraftChange(draft.copy(badgeId = it)) }, stringResource(R.string.clients_flow_no_badge)) }
            item { ClientReadOnlyLine(R.string.clients_devise, devise.ifBlank { "—" }) }
            item { OutlinedTextField(value = draft.notes, onValueChange = { onDraftChange(draft.copy(notes = it.take(ClientValidation.LONGUEUR_NOTES_MAX))) }, modifier = Modifier.fillMaxWidth().height(125.dp), label = { Text(stringResource(R.string.clients_notes)) }, maxLines = 5) }
        }
    }
    editing?.let { address ->
        ClientAddressDialog(address, onDismiss = { editing = null }, onSave = { value ->
            val existing = addresses.indexOfFirst { it === address || (it.id == address.id && it.adresse == address.adresse) }
            val normalised = if (value.principale) value else value.copy(principale = addresses.none { it !== address && it.principale })
            if (normalised.principale) {
                addresses.indices.forEach { itemIndex ->
                    val item = addresses[itemIndex]
                    addresses[itemIndex] = if (item === address || (item.id == address.id && item.adresse == address.adresse)) normalised else item.copy(principale = false)
                }
            } else if (existing >= 0) addresses[existing] = normalised else addresses.add(normalised)
            if (existing < 0 && normalised !in addresses) addresses.add(normalised)
            editing = null
        })
    }
}

@Composable
private fun ClientAddressDialog(address: ClientAddressEntity, onDismiss: () -> Unit, onSave: (ClientAddressEntity) -> Unit) {
    var label by remember(address.id, address.adresse) { mutableStateOf(address.libelle) }
    var street by remember(address.id, address.adresse) { mutableStateOf(address.adresse) }
    var city by remember(address.id, address.adresse) { mutableStateOf(address.ville.orEmpty()) }
    var main by remember(address.id, address.adresse) { mutableStateOf(address.principale) }
    val valid = street.trim().length in 2..ClientValidation.LONGUEUR_ADRESSE_MAX && label.trim().length <= 60 && city.trim().length <= 100
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clients_flow_address)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = label, onValueChange = { label = it.take(60) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_address_label)) }, singleLine = true)
            OutlinedTextField(value = street, onValueChange = { street = it.take(ClientValidation.LONGUEUR_ADRESSE_MAX) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_adresse)) }, minLines = 2)
            OutlinedTextField(value = city, onValueChange = { city = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_city)) }, singleLine = true)
            FilterChip(selected = main, onClick = { main = !main }, label = { Text(stringResource(R.string.clients_flow_primary)) })
        } },
        confirmButton = { Button(onClick = { onSave(address.copy(libelle = label, adresse = street, ville = city.ifBlank { null }, principale = main)) }, enabled = valid) { Text(stringResource(R.string.clients_enregistrer)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientWizardScaffold(title: Int, step: Int?, onBack: () -> Unit, primaryLabel: Int, enabled: Boolean, onPrimary: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = ClientBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { ClientPageTitle(stringResource(title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back), tint = ClientInk) } },
            )
        },
        bottomBar = {
            Button(onClick = onPrimary, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = ClientBlue)) {
                Text(stringResource(primaryLabel), fontWeight = FontWeight.Bold); Spacer(Modifier.width(7.dp)); Text(if (primaryLabel == R.string.clients_enregistrer) "✓" else "→")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            step?.let { ClientProgress(it) }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun ClientProgress(current: Int) {
    val labels = listOf(R.string.clients_flow_step_information, R.string.clients_flow_step_contacts, R.string.clients_flow_step_addresses)
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 31.dp, vertical = 9.dp)) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = if (index <= current) ClientBlue else ClientBorder) { Box(contentAlignment = Alignment.Center) { Text(if (index < current) "✓" else (index + 1).toString(), color = Color.White, fontSize = 10.sp) } }
                Text(stringResource(label), color = if (index == current) ClientBlue else ClientMuted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientHistoryScreen(client: ClientEntity, records: List<OperationRecordEntity>, devise: String, onBack: () -> Unit) {
    val total = records.filter { it.status == OperationStatus.VALIDATED.name }.sumOf { SaleRecordCodec.decode(it.notes)?.total ?: 0.0 }
    val due = records.filter { it.status == OperationStatus.VALIDATED.name }.sumOf { payloadOutstanding(it) }
    Scaffold(containerColor = ClientBackground, topBar = { CenterAlignedTopAppBar(title = { ClientPageTitle(stringResource(R.string.clients_flow_sales_history)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back)) } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ClientHistoryHeader(client) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { ClientStatCard(stringResource(R.string.clients_flow_sales_total), clientMoney(total, devise), Modifier.weight(1f)); ClientStatCard(stringResource(R.string.clients_flow_outstanding), clientMoney(due, devise), Modifier.weight(1f)) } }
            if (records.isEmpty()) item { ClientFormEmpty(R.string.clients_flow_no_sales) }
            items(records, key = { it.id }) { record -> ClientSaleRow(record, devise) }
        }
    }
}

@Composable
private fun ClientHistoryHeader(client: ClientEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) { ClientAvatar(client.nom, Modifier.size(35.dp)); Spacer(Modifier.width(8.dp)); Column { Text(client.nom, color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${client.code} · ${client.telephone}", color = ClientMuted, fontSize = 9.sp) } }
}

@Composable
private fun ClientSaleRow(record: OperationRecordEntity, devise: String) {
    val payload = SaleRecordCodec.decode(record.notes)
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(DateUtils.formatDate(record.createdAt), color = ClientMuted, fontSize = 9.sp); Text(record.reference, color = ClientBlue, fontWeight = FontWeight.SemiBold, fontSize = 10.sp); Text(if (record.status == OperationStatus.CANCELLED.name) stringResource(R.string.sales_cancelled) else stringResource(R.string.sales_validated), color = if (record.status == OperationStatus.CANCELLED.name) ClientRed else ClientGreen, fontSize = 9.sp) }
            Column(horizontalAlignment = Alignment.End) { Text(clientMoney(payload?.total ?: record.amount ?: 0.0, devise), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 10.sp); Text(stringResource(R.string.clients_flow_due_short, clientMoney(payloadOutstanding(record), devise)), color = ClientMuted, fontSize = 9.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientAccountScreen(client: ClientEntity, records: List<OperationRecordEntity>, devise: String, onBack: () -> Unit, onDownload: () -> Unit) {
    val entries = records.filter { it.status == OperationStatus.VALIDATED.name }.sortedBy { it.createdAt }.toAccountEntries()
    val outstanding = entries.lastOrNull()?.balance ?: 0.0
    val creditLimit = client.limiteCredit
    Scaffold(
        containerColor = ClientBackground,
        topBar = { CenterAlignedTopAppBar(title = { ClientPageTitle(stringResource(R.string.clients_flow_account_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back)) } }) },
        bottomBar = { OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth().padding(15.dp).height(47.dp)) { Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.clients_flow_download_pdf)) } },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ClientHistoryHeader(client) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { ClientStatCard(stringResource(R.string.clients_flow_outstanding), clientMoney(outstanding, devise), Modifier.weight(1f)); ClientStatCard(stringResource(R.string.clients_limite_credit), creditLimit?.let { clientMoney(it, devise) } ?: stringResource(R.string.clients_flow_unlimited), Modifier.weight(1f)) } }
            item { ClientCreditGauge(outstanding, creditLimit, devise) }
            if (entries.isEmpty()) item { ClientFormEmpty(R.string.clients_flow_no_account_entries) }
            items(entries, key = { it.record.id }) { entry -> ClientAccountRow(entry, devise) }
        }
    }
}

@Composable
private fun ClientCreditGauge(outstanding: Double, limit: Double?, devise: String) {
    val ratio = if (limit != null && limit > 0) (outstanding / limit).coerceIn(0.0, 1.0) else 0.0
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClientKeyValue(R.string.clients_flow_credit_usage, if (limit == null) stringResource(R.string.clients_flow_unlimited) else "${(ratio * 100).toInt()} %")
            Surface(modifier = Modifier.fillMaxWidth().height(7.dp), shape = RoundedCornerShape(5.dp), color = ClientBorder) { Box(Modifier.fillMaxWidth(ratio.toFloat()).height(7.dp).background(if (ratio > .9) ClientRed else ClientGreen)) }
            Text(stringResource(R.string.clients_flow_outstanding_value, clientMoney(outstanding, devise)), color = ClientMuted, fontSize = 9.sp)
        }
    }
}

private data class AccountEntry(val record: OperationRecordEntity, val debit: Double, val credit: Double, val balance: Double)

private fun List<OperationRecordEntity>.toAccountEntries(): List<AccountEntry> {
    var balance = 0.0
    return mapNotNull { record ->
        val payload = SaleRecordCodec.decode(record.notes) ?: return@mapNotNull null
        balance += payload.total - payload.paidAmount
        AccountEntry(record, payload.total, payload.paidAmount, balance)
    }
}

@Composable
private fun ClientAccountRow(entry: AccountEntry, devise: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(DateUtils.formatDate(entry.record.createdAt), color = ClientMuted, fontSize = 9.sp); Text(entry.record.reference, color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
            Column(horizontalAlignment = Alignment.End) { Text(stringResource(R.string.clients_flow_debit_credit, clientMoney(entry.debit, devise), clientMoney(entry.credit, devise)), color = ClientMuted, fontSize = 9.sp); Text(clientMoney(entry.balance, devise), color = ClientBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientSearchScreen(clients: List<ClientEntity>, categories: List<CategoryClientEntity>, metrics: Map<Long, ClientSalesMetrics>, devise: String, onBack: () -> Unit, onOpen: (ClientEntity) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("ALL") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var commercial by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var since by rememberSaveable { mutableStateOf("") }
    var until by rememberSaveable { mutableStateOf("") }
    val results = clients.filter { client ->
        val dates = client.createdAt
        (query.isBlank() || client.nom.contains(query, true) || client.code.contains(query, true) || client.telephone.contains(query)) &&
            (status == "ALL" || (status == "ACTIVE" && client.isActive()) || (status == "INACTIVE" && !client.isActive())) &&
            (categoryId == null || client.categorieId == categoryId) &&
            (commercial.isBlank() || client.commercial.orEmpty().contains(commercial, true)) &&
            (city.isBlank() || client.adresse.orEmpty().contains(city, true)) &&
            (since.parseClientDate()?.let { dates >= it } ?: true) &&
            (until.parseClientDate()?.let { dates <= it + 86_399_999 } ?: true)
    }
    Scaffold(containerColor = ClientBackground, topBar = { CenterAlignedTopAppBar(title = { ClientPageTitle(stringResource(R.string.clients_flow_search_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back)) } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(stringResource(R.string.clients_recherche)) }, singleLine = true) }
            item { Text(stringResource(R.string.clients_flow_filters), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            item { ClientStringPicker(R.string.clients_statut, listOf("ALL" to stringResource(R.string.clients_flow_all_status), "ACTIVE" to stringResource(R.string.clients_actif), "INACTIVE" to stringResource(R.string.clients_inactif)), status) { status = it } }
            item { ClientSelectorField(R.string.clients_categories, categories.map { it.id to it.nom }, categoryId, { categoryId = it }, stringResource(R.string.clients_aucune_categorie)) }
            item { OutlinedTextField(value = commercial, onValueChange = { commercial = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_sales_rep)) }, singleLine = true) }
            item { OutlinedTextField(value = city, onValueChange = { city = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.clients_flow_city)) }, singleLine = true) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = since, onValueChange = { since = it.take(10) }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.clients_flow_from)) }, placeholder = { Text("JJ/MM/AAAA", fontSize = 9.sp) }, singleLine = true); OutlinedTextField(value = until, onValueChange = { until = it.take(10) }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.clients_flow_to)) }, placeholder = { Text("JJ/MM/AAAA", fontSize = 9.sp) }, singleLine = true) } }
            item { Text(stringResource(R.string.clients_flow_results, results.size), color = ClientMuted, fontSize = 11.sp) }
            items(results, key = { it.id }) { client -> ClientListRow(client, categories.firstOrNull { it.id == client.categorieId }, metrics[client.id]?.outstanding ?: 0.0, devise) { onOpen(client) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientStringPicker(label: Int, choices: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = choices.firstOrNull { it.first == selected }?.second.orEmpty(), onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), label = { Text(stringResource(label)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { choices.forEach { (id, text) -> androidx.compose.material3.DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDeactivateScreen(client: ClientEntity, outstanding: Double, devise: String, onBack: () -> Unit, onConfirm: () -> Unit) {
    Scaffold(containerColor = Color(0xFFFFF9F9), topBar = { CenterAlignedTopAppBar(title = { ClientPageTitle(stringResource(R.string.clients_flow_deactivate_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clients_flow_back)) } }) }, bottomBar = { Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().padding(15.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = ClientRed)) { Icon(Icons.Outlined.Cancel, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.clients_desactiver)) } }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.size(76.dp), shape = CircleShape, color = ClientRed.copy(alpha = .12f)) { Icon(Icons.Outlined.PersonOutline, null, tint = ClientRed, modifier = Modifier.padding(18.dp)) }
            Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.clients_flow_deactivate_question), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(7.dp)); Text(stringResource(R.string.clients_flow_deactivate_description, client.nom), color = ClientMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            if (outstanding > 0) { Spacer(Modifier.height(12.dp)); Surface(shape = RoundedCornerShape(8.dp), color = ClientRed.copy(alpha = .08f), border = BorderStroke(1.dp, ClientRed.copy(alpha = .4f))) { Text(stringResource(R.string.clients_flow_deactivate_due, clientMoney(outstanding, devise)), color = ClientRed, fontSize = 10.sp, modifier = Modifier.padding(11.dp), textAlign = TextAlign.Center) } }
            Spacer(Modifier.height(16.dp)); Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, ClientBorder), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(stringResource(R.string.clients_flow_consequences), color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(stringResource(R.string.clients_flow_consequence_1), color = ClientMuted, fontSize = 10.sp); Text(stringResource(R.string.clients_flow_consequence_2), color = ClientMuted, fontSize = 10.sp) } }
        }
    }
}

@Composable
private fun ClientPageTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MissaBrandMark(size = 22.dp)
        Spacer(Modifier.width(7.dp))
        Text(title, color = ClientInk, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ClientReadOnlyLine(label: Int, value: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), color = Color.White, border = BorderStroke(1.dp, ClientBorder)) { Row(Modifier.padding(11.dp)) { Text(stringResource(label), color = ClientMuted, fontSize = 10.sp, modifier = Modifier.weight(1f)); Text(value, color = ClientInk, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) } }
}

@Composable
private fun ClientBottomBar(onNavigate: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 7.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            ClientNav(Icons.Outlined.Home, R.string.sales_home) { onNavigate(Routes.HOME) }
            ClientNav(Icons.Outlined.ShoppingCart, R.string.sales_nav_sales) { onNavigate(AppModule.VENTE.route) }
            ClientNav(Icons.Outlined.PersonOutline, R.string.module_clients, selected = true) { }
            ClientNav(Icons.Outlined.Inventory2, R.string.module_achats) { onNavigate(AppModule.ACHATS.route) }
            ClientNav(Icons.Outlined.MoreVert, R.string.more_modules) { onNavigate(AppModule.REPORTING.route) }
        }
    }
}

@Composable
private fun ClientNav(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, selected: Boolean = false, click: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = click).padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = if (selected) ClientBlue else ClientMuted, modifier = Modifier.size(20.dp)); Text(stringResource(label), color = if (selected) ClientBlue else ClientMuted, fontSize = 9.sp) }
}

private fun ClientEntity.isActive(): Boolean = active && statut == ClientStatus.ACTIF
private fun String.initials(): String = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
private fun Double.decimalText(): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.getDefault())).format(this)
private fun String.decimalValue(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun String.decimalInput(): String = filter { it.isDigit() || it == ',' || it == '.' }.take(12)
private fun ClientAddressEntity.displayAddress(): String = listOf(adresse.trim(), ville?.trim().orEmpty()).filter { it.isNotEmpty() }.joinToString(", ")
private fun ClientEntity.fallbackAddress(): List<ClientAddressEntity> = adresse?.takeIf { it.isNotBlank() }?.let { listOf(ClientAddressEntity(clientId = id, libelle = "", adresse = it, principale = true)) } ?: emptyList()
private fun List<ClientContactEntity>.withOnePrimaryContact(): List<ClientContactEntity> {
    val primaryIndex = indexOfFirst { it.principal }.takeIf { it >= 0 } ?: 0
    return mapIndexed { index, contact -> contact.copy(principal = index == primaryIndex) }
}
private fun List<ClientAddressEntity>.withOnePrimaryAddress(): List<ClientAddressEntity> {
    val primaryIndex = indexOfFirst { it.principale }.takeIf { it >= 0 } ?: 0
    return mapIndexed { index, address -> address.copy(principale = index == primaryIndex) }
}

private fun saleMetricsByClient(records: List<OperationRecordEntity>): Map<Long, ClientSalesMetrics> =
    records.filter { it.status == OperationStatus.VALIDATED.name }.mapNotNull { record -> SaleRecordCodec.decode(record.notes)?.let { it.clientId to it } }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> ClientSalesMetrics(values.sumOf { it.total }, values.sumOf { (it.total - it.paidAmount).coerceAtLeast(0.0) }, values.size) }

private fun List<OperationRecordEntity>.salesFor(clientId: Long): List<OperationRecordEntity> = filter { SaleRecordCodec.decode(it.notes)?.clientId == clientId }
private fun payloadOutstanding(record: OperationRecordEntity): Double =
    if (record.status == OperationStatus.CANCELLED.name) 0.0
    else SaleRecordCodec.decode(record.notes)?.let { (it.total - it.paidAmount).coerceAtLeast(0.0) } ?: 0.0
private fun clientMoney(amount: Double, devise: String): String {
    val fraction = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fraction == 0) "#,##0" else "#,##0.${"0".repeat(fraction.coerceAtMost(2))}"
    return "${DecimalFormat(pattern, DecimalFormatSymbols(Locale.getDefault())).format(amount)} $devise"
}
private fun String.parseClientDate(): Long? = runCatching { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }.parse(this)?.time }.getOrNull()

private fun Context.readClientCsv(uri: Uri): List<ImportedClientRow> = runCatching {
    contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            emptyList()
        } else {
            val delimiter = if (lines.first().count { it == ';' } >= lines.first().count { it == ',' }) ';' else ','
            val first = lines.first().split(delimiter).map { it.trim().lowercase() }
            val header = first.any { it in setOf("nom", "name", "client", "telephone", "téléphone", "phone") }
            val nameIndex = first.indexOfFirst { it in setOf("nom", "name", "client") }.takeIf { it >= 0 } ?: 0
            val phoneIndex = first.indexOfFirst { it in setOf("telephone", "téléphone", "phone", "tel") }.takeIf { it >= 0 } ?: 1
            val emailIndex = first.indexOfFirst { it in setOf("email", "e-mail", "mail") }
            val addressIndex = first.indexOfFirst { it in setOf("adresse", "address") }
            lines.drop(if (header) 1 else 0).mapNotNull { line ->
                val columns = line.split(delimiter).map { it.trim() }
                val name = columns.getOrNull(nameIndex).orEmpty()
                val phone = ClientValidation.filtrerTelephonePourSaisie(columns.getOrNull(phoneIndex).orEmpty())
                if (name.isBlank() || phone.isBlank()) null else ImportedClientRow(name, phone, columns.getOrNull(emailIndex)?.ifBlank { null }, columns.getOrNull(addressIndex)?.ifBlank { null })
            }
        }
    } ?: emptyList()
}.getOrDefault(emptyList())

private fun Context.writeClientAccountPdf(uri: Uri, client: ClientEntity, records: List<OperationRecordEntity>, devise: String): Boolean = runCatching {
    val entries = records.salesFor(client.id).filter { it.status == OperationStatus.VALIDATED.name }.sortedBy { it.createdAt }.toAccountEntries()
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(21, 84, 232); textSize = 21f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(16, 28, 67); textSize = 11f }
        canvas.drawText("MISSA BUSINESS 360", 45f, 60f, title)
        canvas.drawText(client.nom, 45f, 95f, body)
        canvas.drawText("${client.code} · ${client.telephone}", 45f, 116f, body)
        var y = 158f
        entries.take(27).forEach { entry ->
            canvas.drawText("${DateUtils.formatDate(entry.record.createdAt)}  ${entry.record.reference.take(22)}", 45f, y, body)
            canvas.drawText(clientMoney(entry.balance, devise), 430f, y, body)
            y += 23f
        }
        y += 16f
        canvas.drawText("Solde : ${clientMoney(entries.lastOrNull()?.balance ?: 0.0, devise)}", 45f, y, title)
        document.finishPage(page)
        contentResolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Output stream unavailable")
    } finally { document.close() }
}.isSuccess


/** Convertit une fiche persistante en brouillon d'édition sans perdre les champs existants. */
private fun ClientEntity.toDraft(defaultCountry: String?): ClientDraft {
    val country = Iso4217.codePaysDepuisTelephone(telephone) ?: defaultCountry
    return ClientDraft(
        name = nom,
        type = type,
        countryCode = country,
        phoneLocal = ClientValidation.telephoneSansIndicatif(telephone, Iso4217.indicatifTelephone(country)),
        email = email.orEmpty(),
        nif = nif.orEmpty(),
        categoryId = categorieId,
        siteId = siteId,
        commercial = commercial.orEmpty(),
        paymentDays = conditionPaiementJours.toString(),
        discount = remiseDefautPct.decimalText(),
        creditLimit = limiteCredit?.decimalText().orEmpty(),
        badgeId = badgeId,
        notes = notes.orEmpty(),
    )
}
