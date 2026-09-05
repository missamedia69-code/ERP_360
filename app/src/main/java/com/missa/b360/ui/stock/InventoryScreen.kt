package com.missa.b360.ui.stock

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.InventoryRules
import com.missa.b360.core.domain.usecase.SaveInventoryUseCase
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    observeProducts: com.missa.b360.core.domain.usecase.ObserveProductsUseCase,
    observeStock: com.missa.b360.core.domain.usecase.ObserveProductStockUseCase,
    private val saveInventory: SaveInventoryUseCase,
) : ViewModel() {
    sealed interface Result {
        data class Succes(val reference: String, val ajustements: Int) : Result
        data object LectureSeule : Result
        data object AucuneLecture : Result
        data object ProduitIntrouvable : Result
        data object SiteIntrouvable : Result
        data object Error : Result
    }

    val products: StateFlow<List<ProductWithStock>> =
        kotlinx.coroutines.flow.combine(
            observeProducts(),
            observeStock(),
        ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Quantité comptée saisie par produit — vide = non compté (pas d'ajustement). */
    val counts = mutableStateMapOf<Long, String>()
    fun countText(produitId: Long): String = counts[produitId].orEmpty()
    fun setCount(produitId: Long, text: String) {
        counts[produitId] = text.filter { it.isDigit() || it == ',' || it == '.' }
    }

    fun lectures(): List<SaveInventoryUseCase.Lecture> =
        products.value.mapNotNull { product ->
            val text = counts[product.product.id].orEmpty()
            if (text.isBlank()) return@mapNotNull null
            val value = text.replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
            SaveInventoryUseCase.Lecture(product.product.id, value)
        }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    fun save() {
        if (_busy.value) return
        val lectures = lectures()
        if (lectures.isEmpty()) {
            _result.value = Result.AucuneLecture
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val r = saveInventory(lectures)) {
                    is SaveInventoryUseCase.Result.Succes -> {
                        _result.value = Result.Succes(r.reference, r.ajustements)
                        counts.clear()
                    }
                    SaveInventoryUseCase.Result.LectureSeule -> _result.value = Result.LectureSeule
                    SaveInventoryUseCase.Result.AucuneLecture -> _result.value = Result.AucuneLecture
                    SaveInventoryUseCase.Result.ProduitIntrouvable -> _result.value = Result.ProduitIntrouvable
                    SaveInventoryUseCase.Result.SiteIntrouvable -> _result.value = Result.SiteIntrouvable
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}

/**
 * Inventaire (spec §12) — comptage produit par produit : stock théorique (lu de la
 * base), quantité comptée, écart signé ; validation → AJUSTEMENT par écart non nul,
 * même référence INV, stock jamais négatif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val result by viewModel.result.collectAsState()
    var search by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val visible = products.filter {
        search.isBlank() ||
            it.product.nom.contains(search, ignoreCase = true) ||
            it.product.code.contains(search, ignoreCase = true) ||
            (it.product.reference?.contains(search, ignoreCase = true) ?: false)
    }
    val comptees = products.count { viewModel.countText(it.product.id).isNotBlank() }
    val ajustementsAttendus = products.count { product ->
        val text = viewModel.countText(product.product.id)
        if (text.isBlank()) return@count false
        val value = text.replace(',', '.').toDoubleOrNull() ?: return@count false
        InventoryRules.ecartRequiertAjustement(InventoryRules.ecart(product.stock, value))
    }

    LaunchedEffect(result) {
        when (val current = result) {
            is InventoryViewModel.Result.Succes -> {
                snackbar.showSnackbar(context.getString(R.string.inventory_success, current.reference, current.ajustements))
                viewModel.clearResult()
                onBack()
            }
            InventoryViewModel.Result.LectureSeule -> {
                snackbar.showSnackbar(context.getString(R.string.ops_read_only)); viewModel.clearResult()
            }
            InventoryViewModel.Result.AucuneLecture -> {
                snackbar.showSnackbar(context.getString(R.string.inventory_no_count)); viewModel.clearResult()
            }
            InventoryViewModel.Result.ProduitIntrouvable -> {
                snackbar.showSnackbar(context.getString(R.string.inventory_product_missing)); viewModel.clearResult()
            }
            InventoryViewModel.Result.SiteIntrouvable -> {
                snackbar.showSnackbar(context.getString(R.string.inventory_no_site)); viewModel.clearResult()
            }
            InventoryViewModel.Result.Error -> {
                snackbar.showSnackbar(context.getString(R.string.ops_error)); viewModel.clearResult()
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MissaCanvas,
            topBar = { MissaTopAppBar(title = stringResource(R.string.inventory_title), onBack = onBack) },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(stringResource(R.string.ops_cancel)) }
                        Button(
                            onClick = viewModel::save,
                            enabled = comptees > 0 && !busy,
                            modifier = Modifier.weight(2f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Outlined.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(stringResource(R.string.inventory_confirm), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.inventory_search)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    )
                }
                if (visible.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.inventory_empty),
                            color = MissaMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(visible, key = { it.product.id }) { product ->
                        InventoryRow(
                            product = product,
                            countText = viewModel.countText(product.product.id),
                            onCountChange = { viewModel.setCount(product.product.id, it) },
                        )
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MissaSoftBlue)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.inventory_counted, comptees), color = MissaInk, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.inventory_expected_adjustments, ajustementsAttendus),
                                    color = if (ajustementsAttendus > 0) BrandBlue else MissaMuted,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(stringResource(R.string.inventory_hint), color = MissaMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InventoryRow(
    product: ProductWithStock,
    countText: String,
    onCountChange: (String) -> Unit,
) {
    val value = countText.replace(',', '.').toDoubleOrNull()
    val ecart = value?.let { InventoryRules.ecart(product.stock, it) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp), color = MissaSoftBlue) {
                Icon(Icons.Outlined.Inventory2, null, tint = BrandBlue, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.product.nom, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.inventory_theoretical, product.stock.saleQtyText()),
                    color = MissaMuted,
                    fontSize = 10.sp,
                )
            }
            OutlinedTextField(
                value = countText,
                onValueChange = onCountChange,
                modifier = Modifier.width(88.dp),
                label = { Text(stringResource(R.string.inventory_counted_label), textAlign = TextAlign.Center) },
                placeholder = { Text("0", textAlign = TextAlign.Center) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (ecart != null && InventoryRules.ecartRequiertAjustement(ecart)) {
                Spacer(Modifier.width(8.dp))
                Text(
                    (if (ecart > 0) "+" else "") + ecart.saleQtyText(),
                    color = if (ecart > 0) Green60 else Red40,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private fun Double.saleQtyText(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)
