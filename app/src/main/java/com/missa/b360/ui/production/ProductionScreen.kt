package com.missa.b360.ui.production

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.SaveProductionOrderUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.admin.AdminScaffold
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Production (spec §Production, doc OP) — création de brouillon, édition,
 * lancement transactionnel (sortie composants + entrée produit fini).
 */
@HiltViewModel
class ProductionViewModel @Inject constructor(
    private val saveProductionOrder: SaveProductionOrderUseCase,
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class UiMessage(val key: Int, val ok: Boolean = true, val args: List<Any> = emptyList())

    val products: StateFlow<List<ProductEntity>> = productDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    private val _ops = MutableStateFlow(emptyList<OperationRecordEntity>())
    val ops: StateFlow<List<OperationRecordEntity>> = _ops
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                _ops.value = operationDao.getByModule(OperationModule.PRODUCTION.name)
            } catch (_: Exception) {
            }
        }
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun nomProduit(id: Long): String = products.value.firstOrNull { it.id == id }?.nom ?: "—"

    fun charge(record: OperationRecordEntity): ProductionRecordPayload? = ProductionCodec.decode(record.notes)

    private fun execuer(recordId: Long?, payload: ProductionRecordPayload, draft: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = saveProductionOrder(recordId, payload, draft)) {
                    is SaveProductionOrderUseCase.Result.Succes -> {
                        _message.value = if (draft) {
                            UiMessage(R.string.prod_brouillon_saved, args = listOf(r.reference))
                        } else {
                            UiMessage(R.string.prod_lance_msg, args = listOf(r.reference))
                        }
                        refresh()
                    }
                    SaveProductionOrderUseCase.Result.LectureSeule ->
                        _message.value = UiMessage(R.string.prod_read_only, ok = false)
                    SaveProductionOrderUseCase.Result.DonneesInvalides ->
                        _message.value = UiMessage(R.string.prod_invalid, ok = false)
                    SaveProductionOrderUseCase.Result.ProduitIntrouvable ->
                        _message.value = UiMessage(R.string.prod_produit_introuvable, ok = false)
                    SaveProductionOrderUseCase.Result.ComposantIntrouvable ->
                        _message.value = UiMessage(R.string.prod_composant_introuvable, ok = false)
                    SaveProductionOrderUseCase.Result.SiteIntrouvable ->
                        _message.value = UiMessage(R.string.prod_site_introuvable, ok = false)
                    SaveProductionOrderUseCase.Result.BrouillonIntrouvable ->
                        _message.value = UiMessage(R.string.prod_brouillon_introuvable, ok = false)
                    is SaveProductionOrderUseCase.Result.StockInsuffisant ->
                        _message.value = UiMessage(
                            R.string.prod_stock_insuffisant,
                            ok = false,
                            args = listOf(
                                r.produitNom,
                                MoneyUtils.formatBrut(r.disponible),
                                MoneyUtils.formatBrut(r.demande),
                            ),
                        )
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun enregisterBrouillon(payload: ProductionRecordPayload, recordId: Long?) {
        execuer(recordId, payload, draft = true)
    }

    fun lancer(payload: ProductionRecordPayload, recordId: Long?) {
        execuer(recordId, payload, draft = false)
    }
}

/** Ligne de composant du formulaire (saisie en cours, pas encore payload). */
private data class ComposantLigne(
    val produitId: Long?,
    val produitNom: String,
    val quantite: String,
)

/** Construit le payload depuis la saisie — null si incohérent. */
private fun productionBuilder(
    produits: List<ProductEntity>,
    produitId: Long?,
    quantite: String,
    lignes: List<ComposantLigne>,
): ProductionRecordPayload? {
    val produit = produits.firstOrNull { it.id == produitId } ?: return null
    val q = quantite.replace(',', '.').toDoubleOrNull() ?: return null
    if (q <= 0.0) return null
    val composants = lignes.mapNotNull { ligne ->
        val id = ligne.produitId ?: return@mapNotNull null
        val lq = ligne.quantite.replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
        if (lq <= 0.0) return@mapNotNull null
        val nom = produits.firstOrNull { it.id == id }?.nom ?: ligne.produitNom
        ProductionComponent(productId = id, nom = nom, quantite = lq)
    }
    if (composants.isEmpty()) return null
    return ProductionRecordPayload(
        produitId = produit.id,
        produitNom = produit.nom,
        quantite = q,
        composants = composants,
    )
}

/**
 * Écran Production — structure admin standard : formulaire + liste des OP.
 */
@Composable
fun ProductionScreen(
    onBack: () -> Unit,
    /** Vrai lorsqu'une action rapide demande directement la création. */
    openCreate: Boolean = false,
    viewModel: ProductionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState(initial = emptyList())
    val ops by viewModel.ops.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // --- État du formulaire
    var produitId by remember { mutableStateOf<Long?>(null) }
    var quantite by remember { mutableStateOf("") }
    var lignes by remember { mutableStateOf(listOf<ComposantLigne>()) }
    var brouillonEnEdition by remember { mutableStateOf<Long?>(null) }
    var formVisible by remember { mutableStateOf(openCreate) }

    fun viderForm() {
        produitId = null; quantite = ""; lignes = listOf(); brouillonEnEdition = null
    }

    fun chargerBrouillon(record: OperationRecordEntity) {
        val payload = viewModel.charge(record) ?: return
        brouillonEnEdition = record.id
        produitId = payload.produitId
        quantite = if (payload.quantite % 1.0 == 0.0) payload.quantite.toLong().toString() else payload.quantite.toString()
        lignes = payload.composants.map {
            ComposantLigne(
                produitId = it.productId,
                produitNom = it.nom,
                quantite = if (it.quantite % 1.0 == 0.0) it.quantite.toLong().toString() else it.quantite.toString(),
            )
        }
        formVisible = true
    }

    LaunchedEffect(viewModel.message) {
        val msg = viewModel.message.value ?: return@LaunchedEffect
        val text = if (msg.args.isEmpty()) context.getString(msg.key) else context.getString(msg.key, msg.args.toTypedArray())
        snackbar.showSnackbar(text)
        viewModel.consumeMessage()
    }

    AdminScaffold(titreRes = R.string.prod_title, onBack = onBack) {
        SnackbarHost(snackbar) { Snackbar(it) }

        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(
                title = if (formVisible && brouillonEnEdition != null)
                    stringResource(R.string.prod_brouillon_titre)
                else
                    stringResource(R.string.prod_nouvel),
            )

            if (!formVisible) {
                Button(
                    onClick = { viderForm(); formVisible = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(stringResource(R.string.prod_nouvel))
                }
                Spacer(Modifier.height(6.dp))
            }

            if (formVisible) {
                ProductSelectorField(
                    label = R.string.prod_produit,
                    choices = products.map { it.id to it.nom },
                    selectedId = produitId,
                    onSelect = { produitId = it },
                    emptyLabel = stringResource(R.string.prod_choisir_produit),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text(stringResource(R.string.prod_quantite)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                MissaSectionTitle(title = stringResource(R.string.prod_composants))

                lignes.forEachIndexed { index, ligne ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ComposantSelector(
                            keyIndex = index,
                            choices = products,
                            selectedId = ligne.produitId,
                            onSelect = { p ->
                                lignes = lignes.toMutableList().apply {
                                    set(index, ligne.copy(produitId = p.id, produitNom = p.nom))
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = ligne.quantite,
                            onValueChange = { v ->
                                lignes = lignes.toMutableList().apply {
                                    set(index, ligne.copy(quantite = v.filter { c -> c.isDigit() || c == ',' || c == '.' }))
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.7f),
                            placeholder = { Text(stringResource(R.string.prod_quantite)) },
                        )
                        IconButton(onClick = {
                            lignes = lignes.toMutableList().apply { removeAt(index) }
                        }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Red40)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                TextButton(
                    onClick = {
                        lignes = lignes + ComposantLigne(produitId = null, produitNom = "", quantite = "")
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.prod_composant_add))
                }
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = { viderForm(); formVisible = false }) {
                        Text(stringResource(R.string.prod_annuler))
                    }
                    Button(
                        onClick = {
                            val payload = productionBuilder(products, produitId, quantite, lignes) ?: return@Button
                            viewModel.enregisterBrouillon(payload, brouillonEnEdition)
                            viderForm(); formVisible = false
                        },
                        enabled = !busy,
                    ) {
                        Text(stringResource(R.string.prod_brouillon_enregistrer))
                    }
                    Button(
                        onClick = {
                            val payload = productionBuilder(products, produitId, quantite, lignes) ?: return@Button
                            viewModel.lancer(payload, brouillonEnEdition)
                            viderForm(); formVisible = false
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    ) {
                        Text(stringResource(R.string.prod_lancer))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            MissaSectionTitle(title = stringResource(R.string.prod_liste))
            if (ops.isEmpty()) {
                Text(stringResource(R.string.prod_vide), color = MissaMuted, fontSize = 12.sp)
            } else {
                ops.forEach { record ->
                    val payload = ProductionCodec.decode(record.notes)
                    val brouillon = record.status == OperationStatus.DRAFT.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = brouillon) { if (brouillon) chargerBrouillon(record) },
                        colors = CardDefaults.cardColors(containerColor = MissaCanvas),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    record.reference,
                                    color = MissaInk,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(if (brouillon) R.string.prod_brouillon else R.string.prod_lance),
                                    color = if (brouillon) MissaMuted else BrandBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                payload?.let {
                                    "${it.produitNom} × " +
                                        (if (it.quantite % 1.0 == 0.0) it.quantite.toLong() else it.quantite) +
                                        " — " + stringResource(R.string.prod_composants_count, it.composants.size)
                                } ?: record.title,
                                color = MissaMuted,
                                fontSize = 11.sp,
                            )
                            Text(
                                DateUtils.formatDateHeure(record.createdAt),
                                color = MissaMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (brouillon) {
                                Button(
                                    onClick = {
                                        val p = payload ?: return@Button
                                        viewModel.lancer(p, record.id)
                                    },
                                    enabled = !busy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                ) {
                                    Text(stringResource(R.string.prod_lancer))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/**
 * Sélecteur de produit — même composant que les sélecteurs client/employé
 * (spec §46).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectorField(
    label: Int,
    choices: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    emptyLabel: String,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.first == selectedId }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(stringResource(label)) },
            placeholder = { Text(emptyLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(emptyLabel) },
                onClick = { onSelect(null); expanded = false },
            )
            choices.forEach { (id, name) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposantSelector(
    keyIndex: Int,
    choices: List<ProductEntity>,
    selectedId: Long?,
    onSelect: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(keyIndex) { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.id == selectedId }?.nom.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            placeholder = { Text(stringResource(R.string.prod_composant_select)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { p ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(p.nom) },
                    onClick = { onSelect(p); expanded = false },
                )
            }
        }
    }
}
