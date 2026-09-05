package com.missa.b360.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Référentiels (spec §30) : moyens de paiement, taxes et unités.
 * CRUD simple — bascule actif/inactif au lieu de suppression pour les
 * moyens de paiement (historique des pièces conservé lisible).
 */
@HiltViewModel
class ReferentielsViewModel @Inject constructor(
    paymentMethodDao: PaymentMethodDao,
    taxDao: TaxDao,
    private val settingDao: SettingDao,
    private val licenceManager: LicenceManager,
) : ViewModel() {

    sealed interface Result {
        data object Saved : Result()
        data object ReadOnly : Result()
        data object Invalid : Result()
        data object Exists : Result()
    }

    val methods: StateFlow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taxes: StateFlow<List<TaxEntity>> = taxDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _unites = MutableStateFlow(emptyList<String>())
    val unites: StateFlow<List<String>> = _unites
    init {
        viewModelScope.launch {
            _unites.value = settingDao.get(UNITES_KEY)?.valeur
                ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        }
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    fun addMethod(nom: String) {
        if (licenceManager.isReadOnly()) { _result.value = Result.ReadOnly; return }
        val clean = nom.trim()
        if (clean.isEmpty()) { _result.value = Result.Invalid; return }
        if (methods.value.any { it.nom.equals(clean, ignoreCase = true) }) { _result.value = Result.Exists; return }
        viewModelScope.launch {
            _busy.value = true
            try {
                paymentMethodDao.insert(PaymentMethodEntity(nom = clean))
                _result.value = Result.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _result.value = Result.Invalid
            } finally {
                _busy.value = false
            }
        }
    }

    fun toggleMethod(method: PaymentMethodEntity) {
        if (licenceManager.isReadOnly()) { _result.value = Result.ReadOnly; return }
        viewModelScope.launch {
            _busy.value = true
            try {
                paymentMethodDao.update(method.copy(actif = !method.actif))
            } catch (_: Exception) {
            } finally {
                _busy.value = false
            }
        }
    }

    fun addTax(nom: String, taux: Double) {
        if (licenceManager.isReadOnly()) { _result.value = Result.ReadOnly; return }
        val clean = nom.trim()
        if (clean.isEmpty() || taux < 0.0 || taux > 100.0) { _result.value = Result.Invalid; return }
        viewModelScope.launch {
            _busy.value = true
            try {
                taxDao.insert(TaxEntity(nom = clean, taux = taux))
                _result.value = Result.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _result.value = Result.Invalid
            } finally {
                _busy.value = false
            }
        }
    }

    /** Une seule taxe par défaut : bascule exclusive. */
    fun setParDefaut(tax: TaxEntity) {
        if (licenceManager.isReadOnly()) { _result.value = Result.ReadOnly; return }
        if (!tax.parDefaut) {
            viewModelScope.launch {
                _busy.value = true
                try {
                    taxDao.resetParDefaut()
                    taxDao.update(tax.copy(parDefaut = true))
                    _result.value = Result.Saved
                } catch (_: Exception) {
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    fun addUnite(nom: String) {
        val clean = nom.trim()
        if (clean.isEmpty()) return
        if (_unites.value.any { it.equals(clean, ignoreCase = true) }) return
        val liste = _unites.value + clean
        _unites.value = liste
        persistUnites(liste)
    }

    fun removeUnite(nom: String) {
        val liste = _unites.value.filterNot { it == nom }
        _unites.value = liste
        persistUnites(liste)
    }

    private fun persistUnites(liste: List<String>) {
        viewModelScope.launch {
            settingDao.upsert(SettingEntity(cle = UNITES_KEY, valeur = liste.joinToString(", ")))
        }
    }

    fun clearResult() {
        _result.value = null
    }

    companion object {
        const val UNITES_KEY = "unites"
    }
}

/**
 * Écran Référentiels — 3 onglets (paiement, taxes, unités) dans la structure
 * admin standard [AdminScaffold].
 */
@Composable
fun ReferentielsScreen(
    onBack: () -> Unit,
    viewModel: ReferentielsViewModel = hiltViewModel(),
) {
    val methods by viewModel.methods.collectAsState(initial = emptyList())
    val taxes by viewModel.taxes.collectAsState(initial = emptyList())
    val unites by viewModel.unites.collectAsState()
    val busy by viewModel.busy.collectAsState()

    var tab by remember { mutableStateOf("PAIEMENT") }
    var methodNom by remember { mutableStateOf("") }
    var taxNom by remember { mutableStateOf("") }
    var taxTaux by remember { mutableStateOf("") }
    var uniteNom by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.result) {
        when (viewModel.result.value) {
            ReferentielsViewModel.Result.Saved -> {
                methodNom = ""; taxNom = ""; taxTaux = ""; uniteNom = ""
            }
            else -> Unit
        }
        viewModel.clearResult()
    }

    AdminScaffold(titreRes = R.string.refer_title, onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = tab == "PAIEMENT",
                onClick = { tab = "PAIEMENT" },
                label = { Text(stringResource(R.string.refer_tab_paiement), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "TAXES",
                onClick = { tab = "TAXES" },
                label = { Text(stringResource(R.string.refer_tab_taxes), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "UNITES",
                onClick = { tab = "UNITES" },
                label = { Text(stringResource(R.string.refer_tab_unites), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))

        when (tab) {
            "PAIEMENT" -> {
                MissaPanel(modifier = Modifier.fillMaxWidth()) {
                    MissaSectionTitle(title = stringResource(R.string.refer_tab_paiement))
                    if (methods.isEmpty()) {
                        Text(stringResource(R.string.refer_paiement_empty), color = MissaMuted, fontSize = 12.sp)
                    } else {
                        methods.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    method.nom,
                                    color = if (method.actif) MissaInk else MissaMuted,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = method.actif,
                                    onCheckedChange = { viewModel.toggleMethod(method) },
                                    enabled = !busy,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = methodNom,
                            onValueChange = { methodNom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.refer_method_name)) },
                            singleLine = true,
                        )
                        AddButton(enabled = !busy) { viewModel.addMethod(methodNom) }
                    }
                }
            }
            "TAXES" -> {
                MissaPanel(modifier = Modifier.fillMaxWidth()) {
                    MissaSectionTitle(title = stringResource(R.string.refer_tab_taxes))
                    if (taxes.isEmpty()) {
                        Text(stringResource(R.string.refer_taxes_empty), color = MissaMuted, fontSize = 12.sp)
                    } else {
                        taxes.forEach { tax ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = !busy) { viewModel.setParDefaut(tax) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tax.nom, color = MissaInk)
                                    Text(
                                        stringResource(R.string.refer_taux_pct, formatTaux(tax.taux)),
                                        color = MissaMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                                if (tax.parDefaut) {
                                    Text(
                                        stringResource(R.string.refer_default),
                                        color = BrandBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxNom,
                            onValueChange = { taxNom = it },
                            modifier = Modifier.weight(1.4f),
                            label = { Text(stringResource(R.string.refer_tax_name)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = taxTaux,
                            onValueChange = { taxTaux = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.refer_taux_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        AddButton(enabled = !busy) {
                            viewModel.addTax(taxNom, taxTaux.replace(',', '.').toDoubleOrNull() ?: -1.0)
                        }
                    }
                }
            }
            "UNITES" -> {
                MissaPanel(modifier = Modifier.fillMaxWidth()) {
                    MissaSectionTitle(title = stringResource(R.string.refer_tab_unites))
                    Text(stringResource(R.string.refer_unites_hint), color = MissaMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    if (unites.isEmpty()) {
                        Text(stringResource(R.string.refer_unites_empty), color = MissaMuted, fontSize = 12.sp)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            unites.forEach { unite ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(unite, color = MissaInk, fontSize = 12.sp)
                                    TextButton(
                                        onClick = { viewModel.removeUnite(unite) },
                                        contentPadding = PaddingValues(2.dp),
                                    ) {
                                        Text("×", color = Red40, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uniteNom,
                            onValueChange = { uniteNom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.refer_unite_name)) },
                            singleLine = true,
                        )
                        AddButton(enabled = !busy) { viewModel.addUnite(uniteNom) }
                    }
                }
            }
        }
    }
}

private fun formatTaux(taux: Double): String =
    if (taux % 1.0 == 0.0) "${taux.toLong()}" else DecimalFormat("0.##").format(taux)

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(Icons.Outlined.Add, contentDescription = null)
    }
}
