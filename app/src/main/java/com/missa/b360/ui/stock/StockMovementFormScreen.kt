package com.missa.b360.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.usecase.StockMovementResult
import com.missa.b360.core.domain.usecase.TransferStockUseCase
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40

/**
 * Formulaire mouvement de stock (spec §11) — UNE PAGE : type, produit, quantité,
 * motif, justification et résumé « stock avant → mouvement → stock après ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementFormScreen(
    onBack: () -> Unit,
    initialDirection: StockMovementType = StockMovementType.ENTREE,
    onOpenTransfer: () -> Unit,
    viewModel: StockOpsViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val outcome by viewModel.outcome.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var type by rememberSaveable { mutableStateOf(initialDirection.name) }
    var produitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var quantite by rememberSaveable { mutableStateOf("") }
    var motif by rememberSaveable { mutableStateOf("RECEPTION") }
    var motifAutre by rememberSaveable { mutableStateOf("") }
    var docReference by rememberSaveable { mutableStateOf("") }
    var commentaire by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerVisible by remember { mutableStateOf(false) }

    val typeEnum = runCatching { StockMovementType.valueOf(type) }
        .getOrDefault(StockMovementType.ENTREE)
    val produit = products.firstOrNull { it.product.id == produitId }
    val isAdjustment = typeEnum == StockMovementType.AJUSTEMENT
    val quantiteValue = quantite.toQuantityOrNull()
    val delta = when {
        quantiteValue == null -> 0.0
        isAdjustment -> quantiteValue
        typeEnum == StockMovementType.SORTIE -> -quantiteValue
        else -> quantiteValue
    }
    val avant = produit?.stock ?: 0.0
    val apres = (avant + delta).coerceAtLeast(0.0)
    val canConfirm = produit != null && quantiteValue != null && !isInvalidQuantity() && !busy

    fun isInvalidQuantity(): Boolean {
        if (quantiteValue == null) return false
        return if (isAdjustment) quantiteValue == 0.0 else quantiteValue <= 0.0
    }

    LaunchedEffect(outcome) {
        when (val current = outcome) {
            is StockOpsViewModel.MovementOutcome.Result -> {
                when (val result = current.result) {
                    is StockMovementResult.Succes -> {
                        snackbar.showSnackbar(context.getString(R.string.stock_move_success))
                        viewModel.clearOutcome()
                        onBack()
                    }
                    is StockMovementResult.StockInsuffisant -> {
                        error = context.getString(
                            R.string.stock_move_insufficient,
                            result.disponible.displayQuantity(),
                            result.demande.displayQuantity(),
                        )
                        viewModel.clearOutcome()
                    }
                    StockMovementResult.ProduitIntrouvable -> {
                        error = context.getString(R.string.stock_move_product_missing)
                        viewModel.clearOutcome()
                    }
                    StockMovementResult.SiteIntrouvable -> {
                        error = context.getString(R.string.stock_move_no_site)
                        viewModel.clearOutcome()
                    }
                    StockMovementResult.LectureSeule -> {
                        snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                        viewModel.clearOutcome()
                    }
                    StockMovementResult.Invalid -> {
                        error = context.getString(R.string.stock_move_invalid_quantity)
                        viewModel.clearOutcome()
                    }
                }
            }
            is StockOpsViewModel.MovementOutcome.Transfer -> Unit
            null -> Unit
        }
    }

    fun onConfirm() {
        if (!canConfirm || produitId == null) return
        val motifFinal = if (motif == "AUTRE") motifAutre.trim() else motif
        error = null
        viewModel.record(
            produitId = produitId,
            type = typeEnum,
            quantite = quantiteValue!!,
            motif = motifFinal,
            reference = docReference,
            commentaire = commentaire,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.stock_move_title),
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
                        enabled = !busy,
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
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.stock_move_confirm), fontWeight = FontWeight.Bold)
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
            // --- TYPE ---
            MovementCard(stringResource(R.string.stock_move_type)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MovementTypeChip(
                            label = stringResource(R.string.stock_move_entry),
                            selected = typeEnum == StockMovementType.ENTREE,
                            onClick = { type = StockMovementType.ENTREE.name },
                            modifier = Modifier.weight(1f),
                        )
                        MovementTypeChip(
                            label = stringResource(R.string.stock_move_exit),
                            selected = typeEnum == StockMovementType.SORTIE,
                            onClick = { type = StockMovementType.SORTIE.name },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MovementTypeChip(
                            label = stringResource(R.string.stock_move_adjust),
                            selected = typeEnum == StockMovementType.AJUSTEMENT,
                            onClick = { type = StockMovementType.AJUSTEMENT.name },
                            modifier = Modifier.weight(1f),
                        )
                        MovementTypeChip(
                            label = stringResource(R.string.stock_move_transfer),
                            selected = false,
                            onClick = onOpenTransfer,
                            modifier = Modifier.weight(1f),
                            accent = true,
                        )
                    }
                }
            }

            // --- PRODUIT ---
            MovementCard(stringResource(R.string.stock_move_product)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickerVisible = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MissaSoftBlue,
                    border = BorderStroke(1.dp, MissaBorder),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                produit?.nom ?: stringResource(R.string.stock_move_select_product),
                                color = if (produit != null) MissaInk else MissaMuted,
                                fontWeight = if (produit != null) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            produit?.let {
                                Text(
                                    stringResource(
                                        R.string.stock_move_product_subtitle,
                                        it.code,
                                        it.stock.displayQuantity(),
                                    ),
                                    color = MissaMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Text("›", color = MissaMuted, fontSize = 20.sp)
                    }
                }
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it.quantityChars(isAdjustment) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (isAdjustment) R.string.stock_move_ecart else R.string.stock_move_quantity,
                            ),
                        )
                    },
                    placeholder = { Text("0") },
                    isError = isInvalidQuantity() && quantite.isNotBlank(),
                    supportingText = {
                        if (isInvalidQuantity() && quantite.isNotBlank()) {
                            Text(stringResource(R.string.stock_move_invalid_quantity))
                        } else {
                            Text(" ")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            // --- MOTIF ---
            MovementCard(stringResource(R.string.stock_move_motif)) {
                val motifs = listOf(
                    "RECEPTION" to stringResource(R.string.stock_motif_reception),
                    "PERTE" to stringResource(R.string.stock_motif_perte),
                    "CASSE" to stringResource(R.string.stock_motif_casse),
                    "CORRECTION" to stringResource(R.string.stock_motif_correction),
                    "INVENTAIRE" to stringResource(R.string.stock_motif_inventaire),
                    "RETOUR" to stringResource(R.string.stock_motif_retour),
                    "AUTRE" to stringResource(R.string.stock_motif_autre),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    motifs.chunked(3).forEach { rowMotifs ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowMotifs.forEach { (key, label) ->
                                FilterChip(
                                    selected = motif == key,
                                    onClick = { motif = key },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - rowMotifs.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (motif == "AUTRE") {
                    OutlinedTextField(
                        value = motifAutre,
                        onValueChange = { motifAutre = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.stock_move_motif_autre_label)) },
                        singleLine = true,
                    )
                }
            }

            // --- JUSTIFICATION ---
            MovementCard(stringResource(R.string.stock_move_justification)) {
                OutlinedTextField(
                    value = docReference,
                    onValueChange = { docReference = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stock_move_doc_reference)) },
                    placeholder = { Text(stringResource(R.string.stock_move_doc_reference_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = commentaire,
                    onValueChange = { commentaire = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stock_move_comment)) },
                    minLines = 1,
                    maxLines = 3,
                )
            }

            // --- RÉSUMÉ ---
            MovementCard(stringResource(R.string.stock_move_summary)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryValue(
                        label = stringResource(R.string.stock_move_stock_before),
                        value = avant.displayQuantity(),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stock_move_delta_label, delta.signedDisplayQuantity()),
                        color = if (delta < 0) Red40 else Green60,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    SummaryValue(
                        label = stringResource(R.string.stock_move_stock_after),
                        value = apres.displayQuantity(),
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = Red40,
                        fontSize = 12.sp,
                    )
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

    if (pickerVisible) {
        ProductPickerSheet(
            products = products,
            searchPlaceholder = stringResource(R.string.stock_move_select_product),
            onDismiss = { pickerVisible = false },
            onSelect = { product ->
                produitId = product.product.id
                quantite = "1"
                pickerVisible = false
            },
        )
    }
}

/**
 * Formulaire transfert de stock (spec §13) — source ≠ destination,
 * quantité ≤ stock disponible, résumé avant/après.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferFormScreen(
    onBack: () -> Unit,
    viewModel: StockOpsViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val stockRows by viewModel.stockRows.collectAsState(initial = emptyList())
    val sites by viewModel.sites.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val outcome by viewModel.outcome.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var siteSourceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var siteDestId by rememberSaveable { mutableStateOf<Long?>(null) }
    var produitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var quantite by rememberSaveable { mutableStateOf("") }
    var motif by rememberSaveable { mutableStateOf("TRANSFERT") }
    var commentaire by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var productPickerVisible by remember { mutableStateOf(false) }
    var sitePickerVisible by remember { mutableStateOf(false) }
    var sitePickerRole by remember { mutableStateOf("SOURCE") }

    val produit = products.firstOrNull { it.product.id == produitId }
    val sourceSite = sites.firstOrNull { it.id == siteSourceId }
    val destSite = sites.firstOrNull { it.id == siteDestId }
    val sameSite = siteSourceId != null && siteSourceId == siteDestId
    val quantiteValue = quantite.toQuantityOrNull()
    // Stock disponible à la source : la ligne exacte (produit, site source).
    val stockSource = if (produitId != null && siteSourceId != null) {
        stockRows.firstOrNull { it.produitId == produitId && it.siteId == siteSourceId }?.quantite ?: 0.0
    } else {
        0.0
    }
    val insufficient = quantiteValue != null && quantiteValue > stockSource
    val canConfirm = produit != null && siteSourceId != null && siteDestId != null &&
        !sameSite && quantiteValue != null && quantiteValue > 0.0 && !insufficient && !busy

    LaunchedEffect(sites) {
        if (siteSourceId == null && sites.isNotEmpty()) {
            siteSourceId = sites.firstOrNull { it.principal }?.id ?: sites.firstOrNull()?.id
        }
        if (siteDestId == null && sites.size > 1 && siteSourceId != null) {
            siteDestId = sites.firstOrNull { it.id != siteSourceId }?.id
        }
    }

    LaunchedEffect(outcome) {
        when (val current = outcome) {
            is StockOpsViewModel.MovementOutcome.Transfer -> {
                when (val result = current.result) {
                    is TransferStockUseCase.Result.Succes -> {
                        snackbar.showSnackbar(context.getString(R.string.stock_transfer_success, result.reference))
                        viewModel.clearOutcome()
                        onBack()
                    }
                    is TransferStockUseCase.Result.StockInsuffisant -> {
                        error = context.getString(
                            R.string.stock_move_insufficient,
                            result.disponible.displayQuantity(),
                            result.demande.displayQuantity(),
                        )
                        viewModel.clearOutcome()
                    }
                    TransferStockUseCase.Result.ProduitIntrouvable -> {
                        error = context.getString(R.string.stock_move_product_missing)
                        viewModel.clearOutcome()
                    }
                    TransferStockUseCase.Result.Invalid -> {
                        error = context.getString(R.string.stock_transfer_same_site)
                        viewModel.clearOutcome()
                    }
                    TransferStockUseCase.Result.LectureSeule -> {
                        snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                        viewModel.clearOutcome()
                    }
                }
            }
            is StockOpsViewModel.MovementOutcome.Result -> Unit
            null -> Unit
        }
    }

    fun onConfirm() {
        if (!canConfirm || produitId == null || siteSourceId == null || siteDestId == null) return
        error = null
        viewModel.transfer(
            produitId = produitId,
            siteSourceId = siteSourceId,
            siteDestId = siteDestId,
            quantite = quantiteValue!!,
            motif = motif,
            commentaire = commentaire,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.stock_transfer_title),
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
                        enabled = !busy,
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
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.stock_transfer_confirm), fontWeight = FontWeight.Bold)
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
            // --- ENTREPÔTS ---
            MovementCard(stringResource(R.string.stock_transfer_sites)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SiteSelect(
                        label = stringResource(R.string.stock_transfer_source),
                        site = sourceSite,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            sitePickerRole = "SOURCE"
                            sitePickerVisible = true
                        },
                    )
                    Icon(Icons.Outlined.SwapVert, null, tint = MissaMuted, modifier = Modifier.size(20.dp))
                    SiteSelect(
                        label = stringResource(R.string.stock_transfer_destination),
                        site = destSite,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            sitePickerRole = "DEST"
                            sitePickerVisible = true
                        },
                    )
                }
                if (sameSite) {
                    Text(
                        text = stringResource(R.string.stock_transfer_same_site),
                        color = Red40,
                        fontSize = 12.sp,
                    )
                }
            }

            // --- PRODUIT + QUANTITÉ ---
            MovementCard(stringResource(R.string.stock_move_product)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { productPickerVisible = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MissaSoftBlue,
                    border = BorderStroke(1.dp, MissaBorder),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                produit?.nom ?: stringResource(R.string.stock_move_select_product),
                                color = if (produit != null) MissaInk else MissaMuted,
                                fontWeight = if (produit != null) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            produit?.let {
                                Text(
                                    stringResource(R.string.stock_transfer_available, stockSource.displayQuantity()),
                                    color = MissaMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Text("›", color = MissaMuted, fontSize = 20.sp)
                    }
                }
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it.quantityChars(allowNegative = false) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stock_move_quantity)) },
                    placeholder = { Text("0") },
                    isError = insufficient,
                    supportingText = {
                        if (insufficient) Text(stringResource(R.string.stock_transfer_available, stockSource.displayQuantity()))
                        else Text(" ")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = commentaire,
                    onValueChange = { commentaire = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stock_move_comment)) },
                    singleLine = true,
                )
            }

            // --- RÉSUMÉ ---
            MovementCard(stringResource(R.string.stock_transfer_summary)) {
                Text(
                    text = stringResource(
                        R.string.stock_transfer_route,
                        sourceSite?.nom ?: "—",
                        destSite?.nom ?: "—",
                    ),
                    color = MissaInk,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryValue(
                        label = stringResource(R.string.stock_move_stock_before),
                        value = stockSource.displayQuantity(),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.stock_move_delta_label,
                            -(quantiteValue ?: 0.0).signedDisplayQuantity(),
                        ),
                        color = Red40,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    SummaryValue(
                        label = stringResource(R.string.stock_move_stock_after),
                        value = (stockSource - (quantiteValue ?: 0.0)).coerceAtLeast(0.0).displayQuantity(),
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let {
                    Text(text = it, color = Red40, fontSize = 12.sp)
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

    if (productPickerVisible) {
        ProductPickerSheet(
            products = products,
            searchPlaceholder = stringResource(R.string.stock_move_select_product),
            onDismiss = { productPickerVisible = false },
            onSelect = { product ->
                produitId = product.product.id
                quantite = "1"
                productPickerVisible = false
            },
        )
    }
    if (sitePickerVisible) {
        SitePickerSheet(
            sites = sites,
            selectedId = if (sitePickerRole == "SOURCE") siteSourceId else siteDestId,
            onDismiss = { sitePickerVisible = false },
            onSelect = { id ->
                if (sitePickerRole == "SOURCE") siteSourceId = id else siteDestId = id
                sitePickerVisible = false
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Composants partagés des deux formulaires
// ---------------------------------------------------------------------------

@Composable
private fun MovementCard(title: String, content: @Composable () -> Unit) {
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
private fun MovementTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MissaSoftBlue else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandBlue else MissaBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (accent && !selected) ProfileOrange else MissaInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MissaMuted, fontSize = 11.sp)
        Text(value, color = MissaInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SiteSelect(
    label: String,
    site: SiteEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(54.dp),
        shape = RoundedCornerShape(10.dp),
        color = MissaSoftBlue,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(
                label,
                color = MissaMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                site?.nom ?: stringResource(R.string.stock_transfer_select_site),
                color = MissaInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Sélecteur produit avec recherche (recherche locale, spec §47). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerSheet(
    products: List<ProductWithStock>,
    searchPlaceholder: String,
    onDismiss: () -> Unit,
    onSelect: (ProductWithStock) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    val visible = products.filter { product ->
        search.isBlank() ||
            product.nom.contains(search, ignoreCase = true) ||
            product.code.contains(search, ignoreCase = true) ||
            (product.reference?.contains(search, ignoreCase = true) ?: false) ||
            (product.barcode?.contains(search, ignoreCase = true) ?: false)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(
                searchPlaceholder,
                color = MissaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text(
                    stringResource(R.string.stock_picker_empty),
                    color = MissaMuted,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(visible, key = { it.product.id }) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(product) }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.nom, color = MissaInk, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${product.code} · ${stringResource(R.string.sales_stock_available, product.stock.displayQuantity())}",
                                    color = MissaMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            product.prixVente?.let {
                                Text(
                                    it.displayMoney(),
                                    color = BrandBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = MissaBorder)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SitePickerSheet(
    sites: List<SiteEntity>,
    selectedId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(
                stringResource(R.string.stock_transfer_select_site),
                color = MissaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            sites.forEach { site ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(site.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(site.nom, modifier = Modifier.weight(1f), color = MissaInk)
                    if (site.id == selectedId) Icon(Icons.Outlined.Check, null, tint = BrandBlue)
                }
                androidx.compose.material3.HorizontalDivider(color = MissaBorder)
            }
        }
    }
}

private fun String.quantityChars(allowNegative: Boolean): String =
    buildString {
        this@quantityChars.forEachIndexed { index, char ->
            when {
                char in '0'..'9' || char == ',' || char == '.' -> append(char)
                char == '-' && index == 0 && allowNegative -> append(char)
            }
        }
    }

private fun String.toQuantityOrNull(): Double? =
    trim().takeIf { it.isNotEmpty() }
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() }

private fun Double.signedDisplayQuantity(): String =
    if (this < 0) "-${(-this).displayQuantity()}" else "+${displayQuantity()}"

private fun Double.displayQuantity(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)

private fun Double.displayMoney(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("#,##0.00").format(this)
