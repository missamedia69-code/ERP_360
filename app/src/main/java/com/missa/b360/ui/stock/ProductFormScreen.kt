package com.missa.b360.ui.stock

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Barcode
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.ProductInput
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue

/**
 * Formulaire produit (spec §7) — UNE PAGE : identité, type, prix, stock,
 * fournisseur et « plus de détails ». Le type détermine les sections affichées.
 * Le stock courant n'est jamais saisi ici : il provient des mouvements de stock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    onBack: () -> Unit,
    productId: Long? = null,
    viewModel: ProductFormViewModel = hiltViewModel(),
) {
    val isCreate = productId == null
    val product by viewModel.product.collectAsState(initial = null)
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val sites by viewModel.sites.collectAsState(initial = emptyList())
    val fournisseurs by viewModel.fournisseurs.collectAsState(initial = emptyList())
    val stockRows by viewModel.stockRows.collectAsState(initial = emptyList())
    val editingId by viewModel.editingId.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var nom by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(ProductType.ACHATE_REVENDU.name) }
    var reference by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var sku by rememberSaveable { mutableStateOf("") }
    var categorieId by rememberSaveable { mutableStateOf<Long?>(null) }
    var marque by rememberSaveable { mutableStateOf("") }
    var unite by rememberSaveable { mutableStateOf("") }
    var prixAchat by rememberSaveable { mutableStateOf("") }
    var prixVente by rememberSaveable { mutableStateOf("") }
    var prixRevient by rememberSaveable { mutableStateOf("") }
    var prixMinimum by rememberSaveable { mutableStateOf("") }
    var remiseMax by rememberSaveable { mutableStateOf("") }
    var stockInitial by rememberSaveable { mutableStateOf("") }
    var stockMin by rememberSaveable { mutableStateOf("") }
    var stockMax by rememberSaveable { mutableStateOf("") }
    var stockSecurite by rememberSaveable { mutableStateOf("") }
    var siteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var emplacement by rememberSaveable { mutableStateOf("") }
    var fournisseurId by rememberSaveable { mutableStateOf<Long?>(null) }
    var refFournisseur by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var poids by rememberSaveable { mutableStateOf("") }
    var volume by rememberSaveable { mutableStateOf("") }
    var origine by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var prefillDone by remember { mutableStateOf(isCreate) }
    var categorySheetVisible by remember { mutableStateOf(false) }
    var siteSheetVisible by remember { mutableStateOf(false) }
    var supplierSheetVisible by remember { mutableStateOf(false) }
    var newCategoryVisible by remember { mutableStateOf(false) }
    var scannerUnavailable by remember { mutableStateOf(false) }

    val typeEnum = runCatching { ProductType.valueOf(type) }.getOrDefault(ProductType.ACHATE_REVENDU)
    val showSupplier = typeEnum in setOf(ProductType.ACHATE_REVENDU, ProductType.MATIERE_PREMIERE)
    val showPurchasePrice = typeEnum !in setOf(ProductType.FABRIQUE, ProductType.CONNOMMABLE)
    val stockActuel = editingId?.let { id -> stockRows.filter { it.produitId == id }.sumOf { it.quantite } }

    LaunchedEffect(productId) {
        if (productId != null) viewModel.load(productId)
    }

    // Préremplissage unique en mode édition.
    LaunchedEffect(product, prefillDone) {
        val p = product
        if (p != null && !prefillDone) {
            nom = p.nom
            type = p.type.name
            reference = p.reference.orEmpty()
            barcode = p.barcode.orEmpty()
            sku = p.sku.orEmpty()
            categorieId = p.categorieId
            marque = p.marque.orEmpty()
            unite = p.unite.orEmpty()
            prixAchat = p.prixAchat?.amountToInput() ?: ""
            prixVente = p.prixVente?.amountToInput() ?: ""
            prixRevient = p.prixRevient?.amountToInput() ?: ""
            prixMinimum = p.prixMinimum?.amountToInput() ?: ""
            remiseMax = p.remiseMaxPct.amountToInput()
            stockMin = p.stockMin.amountToInput()
            stockMax = p.stockMax?.amountToInput() ?: ""
            stockSecurite = p.stockSecurite.amountToInput()
            siteId = p.siteId
            emplacement = p.emplacement.orEmpty()
            fournisseurId = p.fournisseurId
            refFournisseur = p.refFournisseur.orEmpty()
            description = p.description.orEmpty()
            poids = p.poids?.amountToInput() ?: ""
            volume = p.volume?.amountToInput() ?: ""
            origine = p.origine.orEmpty()
            notes = p.notes.orEmpty()
            prefillDone = true
        }
    }

    // Site par défaut : le site principal de l'entreprise (création).
    LaunchedEffect(sites, prefillDone) {
        if (isCreate && prefillDone && siteId == null) {
            siteId = sites.firstOrNull { it.principal }?.id ?: sites.firstOrNull()?.id
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val scanned = result.data?.getStringExtra("SCAN_RESULT").orEmpty()
        if (scanned.isNotBlank()) {
            barcode = scanned
            fieldErrors = fieldErrors - "barcode"
        }
    }
    LaunchedEffect(scannerUnavailable) {
        if (scannerUnavailable) {
            snackbar.showSnackbar(context.getString(R.string.sales_scanner_unavailable))
            scannerUnavailable = false
        }
    }
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is ProductFormViewModel.SaveResult.Saved -> {
                snackbar.showSnackbar(
                    context.getString(
                        if (result.isCreate) R.string.product_saved else R.string.product_updated,
                        result.code,
                    ),
                )
                viewModel.clearSaveResult()
                onBack()
            }
            ProductFormViewModel.SaveResult.NomObligatoire -> {
                fieldErrors = mapOf("nom" to context.getString(R.string.product_name_required))
                viewModel.clearSaveResult()
            }
            ProductFormViewModel.SaveResult.BarcodeExistant -> {
                fieldErrors = mapOf("barcode" to context.getString(R.string.product_barcode_exists))
                viewModel.clearSaveResult()
            }
            ProductFormViewModel.SaveResult.SiteRequis -> {
                fieldErrors = mapOf("stockInitial" to context.getString(R.string.product_site_required))
                viewModel.clearSaveResult()
            }
            ProductFormViewModel.SaveResult.LectureSeule -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only))
                viewModel.clearSaveResult()
            }
            ProductFormViewModel.SaveResult.DonneesInvalides -> {
                snackbar.showSnackbar(context.getString(R.string.ops_invalid))
                viewModel.clearSaveResult()
            }
            ProductFormViewModel.SaveResult.Introuvable,
            ProductFormViewModel.SaveResult.Error,
            -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error))
                viewModel.clearSaveResult()
            }
            null -> Unit
        }
    }

    fun onSave() {
        val errors = mutableMapOf<String, String>()
        val invalid = context.getString(R.string.product_amount_invalid)
        if (nom.trim().length < 2) errors["nom"] = context.getString(R.string.product_name_required)
        if (barcode.isNotBlank() && barcode.length > 60) errors["barcode"] = invalid
        listOf("prixAchat" to prixAchat, "prixVente" to prixVente, "prixRevient" to prixRevient,
            "prixMinimum" to prixMinimum, "stockMin" to stockMin, "stockMax" to stockMax,
            "stockSecurite" to stockSecurite, "poids" to poids, "volume" to volume,
        ).forEach { (key, text) ->
            text.toAmountOrNull()?.let { if (it < 0.0) errors[key] = invalid }
                ?: if (text.isNotBlank()) errors[key] = invalid
        }
        stockInitial.toAmountOrNull()?.let { if (it < 0.0) errors["stockInitial"] = invalid }
            ?: if (stockInitial.isNotBlank()) errors["stockInitial"] = invalid
        val remise = remiseMax.toAmountOrNull()
        if (remise == null && remiseMax.isNotBlank()) errors["remiseMax"] = invalid
        else if (remise != null && remise !in 0.0..100.0) errors["remiseMax"] = invalid
        if (errors.isNotEmpty()) {
            fieldErrors = errors
            return
        }
        fieldErrors = emptyMap()
        viewModel.save(
            input = ProductInput(
                nom = nom,
                type = typeEnum,
                reference = reference,
                barcode = barcode,
                sku = sku,
                categorieId = categorieId,
                marque = marque,
                unite = unite,
                prixAchat = prixAchat.toAmountOrNull(),
                prixVente = prixVente.toAmountOrNull(),
                prixRevient = prixRevient.toAmountOrNull(),
                prixMinimum = prixMinimum.toAmountOrNull(),
                remiseMaxPct = remise ?: 0.0,
                stockMin = stockMin.toAmountOrNull() ?: 0.0,
                stockMax = stockMax.toAmountOrNull(),
                stockSecurite = stockSecurite.toAmountOrNull() ?: 0.0,
                siteId = siteId,
                emplacement = emplacement,
                fournisseurId = fournisseurId,
                refFournisseur = refFournisseur,
                description = description,
                poids = poids.toAmountOrNull(),
                volume = volume.toAmountOrNull(),
                origine = origine,
                notes = notes,
            ),
            initialStock = if (isCreate) stockInitial.toAmountOrNull() else null,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(if (isCreate) R.string.product_form_new else R.string.product_form_edit),
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
                        onClick = onSave,
                        enabled = !busy,
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
            FormSection(stringResource(R.string.product_section_identity)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it; fieldErrors = fieldErrors - "nom" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_name)) },
                    isError = fieldErrors.containsKey("nom"),
                    supportingText = {
                        fieldErrors["nom"]?.let { Text(it) }
                    },
                    singleLine = true,
                )
                ProductTypePicker(selected = typeEnum, onSelect = { type = it.name })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.product_reference)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it; fieldErrors = fieldErrors - "barcode" },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.product_barcode)) },
                        isError = fieldErrors.containsKey("barcode"),
                        supportingText = { fieldErrors["barcode"]?.let { Text(it) } },
                        trailingIcon = {
                            IconButton(onClick = {
                                val intent = Intent("com.google.zxing.client.android.SCAN")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    scannerLauncher.launch(intent)
                                } else {
                                    scannerUnavailable = true
                                }
                            }) {
                                Icon(
                                    Icons.Outlined.Barcode,
                                    contentDescription = stringResource(R.string.product_scan),
                                )
                            }
                        },
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.product_sku)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = marque,
                        onValueChange = { marque = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.product_brand)) },
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == categorieId }?.nom ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1.4f),
                        label = { Text(stringResource(R.string.product_category)) },
                        trailingIcon = { Icon(Icons.Outlined.Category, null) },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = { categorySheetVisible = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.product_select))
                    }
                }
                OutlinedTextField(
                    value = unite,
                    onValueChange = { unite = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_unit)) },
                    placeholder = { Text(stringResource(R.string.product_unit_hint)) },
                    singleLine = true,
                )
            }

            // --- PRIX ---
            FormSection(stringResource(R.string.product_section_price)) {
                if (showPurchasePrice) {
                    AmountField(
                        label = stringResource(R.string.product_purchase_price),
                        value = prixAchat,
                        onValueChange = { prixAchat = it.amountChars(); fieldErrors = fieldErrors - "prixAchat" },
                        error = fieldErrors["prixAchat"],
                    )
                }
                AmountField(
                    label = stringResource(R.string.product_sale_price),
                    value = prixVente,
                    onValueChange = { prixVente = it.amountChars(); fieldErrors = fieldErrors - "prixVente" },
                    error = fieldErrors["prixVente"],
                )
                AmountField(
                    label = stringResource(R.string.product_cost_price),
                    value = prixRevient,
                    onValueChange = { prixRevient = it.amountChars(); fieldErrors = fieldErrors - "prixRevient" },
                    error = fieldErrors["prixRevient"],
                )
                AmountField(
                    label = stringResource(R.string.product_min_price),
                    value = prixMinimum,
                    onValueChange = { prixMinimum = it.amountChars(); fieldErrors = fieldErrors - "prixMinimum" },
                    error = fieldErrors["prixMinimum"],
                )
                AmountField(
                    label = stringResource(R.string.product_max_discount),
                    value = remiseMax,
                    onValueChange = { remiseMax = it.amountChars(); fieldErrors = fieldErrors - "remiseMax" },
                    error = fieldErrors["remiseMax"],
                    suffix = "%",
                )
            }

            // --- STOCK ---
            FormSection(stringResource(R.string.product_section_stock)) {
                if (!isCreate) {
                    stockActuel?.let { actuel ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.product_current_stock, actuel.displayQuantity()),
                                modifier = Modifier.weight(1f),
                                color = MissaMuted,
                            )
                            Text(
                                stringResource(R.string.product_stock_readonly_hint),
                                color = MissaMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                if (isCreate) {
                    AmountField(
                        label = stringResource(R.string.product_initial_stock),
                        value = stockInitial,
                        onValueChange = { stockInitial = it.amountChars(); fieldErrors = fieldErrors - "stockInitial" },
                        error = fieldErrors["stockInitial"],
                        hint = stringResource(R.string.product_initial_stock_hint),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountField(
                        label = stringResource(R.string.product_min_stock),
                        value = stockMin,
                        onValueChange = { stockMin = it.amountChars(); fieldErrors = fieldErrors - "stockMin" },
                        error = fieldErrors["stockMin"],
                        modifier = Modifier.weight(1f),
                    )
                    AmountField(
                        label = stringResource(R.string.product_max_stock),
                        value = stockMax,
                        onValueChange = { stockMax = it.amountChars(); fieldErrors = fieldErrors - "stockMax" },
                        error = fieldErrors["stockMax"],
                        modifier = Modifier.weight(1f),
                    )
                }
                AmountField(
                    label = stringResource(R.string.product_safety_stock),
                    value = stockSecurite,
                    onValueChange = { stockSecurite = it.amountChars(); fieldErrors = fieldErrors - "stockSecurite" },
                    error = fieldErrors["stockSecurite"],
                )
                OutlinedTextField(
                    value = sites.firstOrNull { it.id == siteId }?.nom ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_main_site)) },
                    trailingIcon = { Icon(Icons.Outlined.Storefront, null) },
                    singleLine = true,
                )
                OutlinedButton(onClick = { siteSheetVisible = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.product_select_site))
                }
                OutlinedTextField(
                    value = emplacement,
                    onValueChange = { emplacement = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_location)) },
                    placeholder = { Text(stringResource(R.string.product_location_hint)) },
                    singleLine = true,
                )
            }

            // --- FOURNISSEUR (selon le type, spec §7) ---
            if (showSupplier) {
                FormSection(stringResource(R.string.product_section_supplier)) {
                    OutlinedTextField(
                        value = fournisseurs.firstOrNull { it.id == fournisseurId }?.nom ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.product_main_supplier)) },
                        trailingIcon = { Icon(Icons.Outlined.Handshake, null) },
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { supplierSheetVisible = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.product_select_supplier))
                    }
                    OutlinedTextField(
                        value = refFournisseur,
                        onValueChange = { refFournisseur = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.product_supplier_ref)) },
                        singleLine = true,
                    )
                }
            }

            // --- PLUS DE DÉTAILS ---
            FormSection(stringResource(R.string.product_section_details)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_description)) },
                    minLines = 2,
                    maxLines = 4,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountField(
                        label = stringResource(R.string.product_weight),
                        value = poids,
                        onValueChange = { poids = it.amountChars(); fieldErrors = fieldErrors - "poids" },
                        error = fieldErrors["poids"],
                        modifier = Modifier.weight(1f),
                    )
                    AmountField(
                        label = stringResource(R.string.product_volume),
                        value = volume,
                        onValueChange = { volume = it.amountChars(); fieldErrors = fieldErrors - "volume" },
                        error = fieldErrors["volume"],
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = origine,
                    onValueChange = { origine = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_origin)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.product_notes)) },
                    minLines = 2,
                    maxLines = 4,
                )
                if (!isCreate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.product_status),
                            modifier = Modifier.weight(1f),
                            color = MissaInk,
                        )
                        Switch(
                            checked = product?.active ?: true,
                            onCheckedChange = viewModel::setActive,
                        )
                        Text(
                            stringResource(
                                if (product?.active == false) R.string.product_status_inactive else R.string.product_status_active,
                            ),
                            color = MissaMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (categorySheetVisible) {
        PickerSheet(
            title = stringResource(R.string.product_category),
            items = categories.map { PickerItem(it.id, it.nom) },
            selectedId = categorieId,
            onClear = {
                categorieId = null
                categorySheetVisible = false
            },
            clearLabel = stringResource(R.string.product_category_none),
            onNew = { newCategoryVisible = true },
            newLabel = stringResource(R.string.product_new_category),
            onSelect = {
                categorieId = it
                categorySheetVisible = false
            },
            onDismiss = { categorySheetVisible = false },
        )
    }
    if (newCategoryVisible) {
        NewCategoryDialog(
            onDismiss = { newCategoryVisible = false },
            onCreate = { name, onResult ->
                viewModel.addCategoryAsync(name) { id ->
                    if (id != null) {
                        categorieId = id
                        categorySheetVisible = false
                    }
                    newCategoryVisible = false
                    onResult(id != null)
                }
            },
        )
    }
    if (siteSheetVisible) {
        PickerSheet(
            title = stringResource(R.string.product_main_site),
            items = sites.map { PickerItem(it.id, it.nom) },
            selectedId = siteId,
            onClear = null,
            clearLabel = "",
            onNew = null,
            newLabel = "",
            onSelect = {
                siteId = it
                siteSheetVisible = false
            },
            onDismiss = { siteSheetVisible = false },
        )
    }
    if (supplierSheetVisible) {
        PickerSheet(
            title = stringResource(R.string.product_main_supplier),
            items = fournisseurs.map { PickerItem(it.id, it.nom) },
            selectedId = fournisseurId,
            onClear = {
                fournisseurId = null
                supplierSheetVisible = false
            },
            clearLabel = stringResource(R.string.product_category_none),
            onNew = null,
            newLabel = "",
            onSelect = {
                fournisseurId = it
                supplierSheetVisible = false
            },
            onDismiss = { supplierSheetVisible = false },
        )
    }
    SnackbarHost(
        hostState = snackbar,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp),
    )
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
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
private fun ProductTypePicker(selected: ProductType, onSelect: (ProductType) -> Unit) {
    val labels = mapOf(
        ProductType.ACHATE_REVENDU to stringResource(R.string.product_type_revend),
        ProductType.MATIERE_PREMIERE to stringResource(R.string.product_type_matiere),
        ProductType.FABRIQUE to stringResource(R.string.product_type_fabrique),
        ProductType.COMPOSE to stringResource(R.string.product_type_compose),
        ProductType.CONNOMMABLE to stringResource(R.string.product_type_consommable),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { (type, label) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onSelect(type) }),
                shape = RoundedCornerShape(10.dp),
                color = if (selected == type) MissaSoftBlue else Color.White,
                border = BorderStroke(1.dp, if (selected == type) BrandBlue else MissaBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = MissaInk,
                        fontSize = 13.sp,
                    )
                    if (selected == type) {
                        Icon(Icons.Outlined.Check, null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    hint: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            when {
                error != null -> Text(error)
                hint != null -> Text(hint, color = MissaMuted)
                else -> Text(" ")
            }
        },
        trailingIcon = {
            if (suffix != null) Text(suffix, color = MissaMuted)
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** Élément de sélecteur générique (id + libellé). */
private data class PickerItem(val id: Long, val label: String)

/** Sélecteur générique en bottom sheet : liste + option « aucune » + création rapide. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    items: List<PickerItem>,
    selectedId: Long?,
    onClear: (() -> Unit)?,
    clearLabel: String,
    onNew: (() -> Unit)?,
    newLabel: String,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 25.dp)) {
            Text(title, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            onClear?.let { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(action)
                        .padding(vertical = 12.dp),
                ) {
                    Text(clearLabel, color = MissaMuted)
                }
                androidx.compose.material3.HorizontalDivider(color = MissaBorder)
            }
            onNew?.let { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(action)
                        .padding(vertical = 12.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(newLabel, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                }
                androidx.compose.material3.HorizontalDivider(color = MissaBorder)
            }
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.stock_picker_empty),
                    color = MissaMuted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.label,
                            modifier = Modifier.weight(1f),
                            color = MissaInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.id == selectedId) Icon(Icons.Outlined.Check, null, tint = BrandBlue)
                    }
                    if (index < items.lastIndex) androidx.compose.material3.HorizontalDivider(color = MissaBorder)
                }
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String, (Boolean) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.product_new_category)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.product_category)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) { } }, enabled = name.trim().length >= 2) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

private fun String.toAmountOrNull(): Double? =
    trim().takeIf { it.isNotEmpty() }
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() }

private fun String.amountChars(): String = filter { it.isDigit() || it == ',' || it == '.' }

private fun Double?.amountToInput(): String {
    val value = this ?: return ""
    return if (value % 1.0 == 0.0) toInt().toString() else toString()
}

private fun Double.displayQuantity(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)
