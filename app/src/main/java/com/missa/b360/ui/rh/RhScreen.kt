package com.missa.b360.ui.rh

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.AdvanceCodec
import com.missa.b360.core.domain.model.PayslipCodec
import com.missa.b360.core.domain.model.PayslipPayload
import com.missa.b360.core.domain.usecase.CreatePayslipUseCase
import com.missa.b360.core.domain.usecase.DeleteAbsenceUseCase
import com.missa.b360.core.domain.usecase.DesactivateEmployeeUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.SaveAdvanceUseCase
import com.missa.b360.core.domain.usecase.SaveAbsenceUseCase
import com.missa.b360.core.domain.usecase.SaveEmployeeUseCase
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.admin.AdminScaffold
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
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
import java.util.Calendar
import javax.inject.Inject

/**
 * RH (spec §RH / §Paie) — employés, absences, paie (P), avances (AV).
 * Un seul écran, 4 onglets ; actions via les use cases transactionnels.
 */
@HiltViewModel
class RhViewModel @Inject constructor(
    private val saveEmployee: SaveEmployeeUseCase,
    private val desactivateEmployee: DesactivateEmployeeUseCase,
    private val saveAbsence: SaveAbsenceUseCase,
    private val deleteAbsence: DeleteAbsenceUseCase,
    private val saveAdvance: SaveAdvanceUseCase,
    private val createPayslip: CreatePayslipUseCase,
    employeeDao: EmployeeDao,
    absenceDao: AbsenceDao,
    private val operationDao: OperationRecordDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class UiMessage(val key: Int, val ok: Boolean = true, val arg: String? = null)

    val employees: StateFlow<List<EmployeeEntity>> = employeeDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val absences: StateFlow<List<AbsenceEntity>> = absenceDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    /** Pièces RH (bulletins P + avances AV) — rechargé après chaque action. */
    private val _rhRecords = MutableStateFlow(emptyList<OperationRecordEntity>())
    val rhRecords: StateFlow<List<OperationRecordEntity>> = _rhRecords
    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                _rhRecords.value = operationDao.getByModule(OperationModule.RH.name)
            } catch (_: Exception) {
            }
        }
    }

    val payslips: StateFlow<List<OperationRecordEntity>> = _rhRecords
        .map { list -> list.mapNotNull { record ->
            PayslipCodec.decode(record.notes)?.let { payload -> record to payload }
        } }
        .map { pairs -> pairs.map { it.first }.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val avances: StateFlow<List<OperationRecordEntity>> = _rhRecords
        .map { list -> list.filter { AdvanceCodec.decode(it.notes) != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    private inline fun withBusy(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    fun saveEmployee(recordId: Long?, nom: String, telephone: String, poste: String?, salaireBase: Double, joursMensuels: Double, notes: String?) {
        withBusy {
            when (val r = saveEmployee(recordId, nom, telephone, poste, salaireBase, joursMensuels, notes)) {
                is SaveEmployeeUseCase.Result.Succes -> _message.value = UiMessage(R.string.rh_employe_saved, arg = r.code)
                SaveEmployeeUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
                SaveEmployeeUseCase.Result.DonneesInvalides -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
                SaveEmployeeUseCase.Result.EmployeIntrouvable -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
            }
        }
    }

    fun desactiver(employeeId: Long) {
        withBusy {
            when (desactivateEmployee(employeeId)) {
                DesactivateEmployeeUseCase.Result.Succes -> _message.value = UiMessage(R.string.rh_employe_desactive)
                DesactivateEmployeeUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
                DesactivateEmployeeUseCase.Result.Introuvable -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
            }
        }
    }

    fun saveAbsence(employeeId: Long, type: String, dateDebut: Long, dureeJours: Double, motif: String?) {
        if (employeeId <= 0L) {
            _message.value = UiMessage(R.string.rh_absence_employe_requis, ok = false)
            return
        }
        withBusy {
            when (saveAbsence(employeeId, type, dateDebut, dureeJours, motif)) {
                SaveAbsenceUseCase.Result.Succes -> _message.value = UiMessage(R.string.rh_absence_saved)
                SaveAbsenceUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
                SaveAbsenceUseCase.Result.DonneesInvalides -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
                SaveAbsenceUseCase.Result.EmployeIntrouvable -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
            }
        }
    }

    fun deleteAbsence(absenceId: Long) {
        withBusy {
            when (deleteAbsence(absenceId)) {
                DeleteAbsenceUseCase.Result.Succes -> _message.value = UiMessage(R.string.rh_absence_deleted)
                DeleteAbsenceUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
            }
        }
    }

    fun saveAdvance(employeeId: Long, montant: Double, motif: String?) {
        if (employeeId <= 0L) {
            _message.value = UiMessage(R.string.rh_avance_employe_requis, ok = false)
            return
        }
        withBusy {
            when (val r = saveAdvance(employeeId, montant, motif)) {
                is SaveAdvanceUseCase.Result.Succes -> {
                    _message.value = UiMessage(R.string.rh_avance_saved, arg = r.reference)
                    refresh()
                }
                SaveAdvanceUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
                SaveAdvanceUseCase.Result.DonneesInvalides -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
                SaveAdvanceUseCase.Result.EmployeIntrouvable -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
            }
        }
    }

    fun genererPaie(mois: Int, annee: Int) {
        withBusy {
            when (val r = createPayslip(mois, annee)) {
                is CreatePayslipUseCase.Result.Succes -> {
                    _message.value = UiMessage(R.string.rh_paie_saved, arg = "${r.reference} (${r.employes})")
                    refresh()
                }
                CreatePayslipUseCase.Result.LectureSeule -> _message.value = UiMessage(R.string.rh_read_only, ok = false)
                CreatePayslipUseCase.Result.DonneesInvalides -> _message.value = UiMessage(R.string.rh_invalid, ok = false)
                CreatePayslipUseCase.Result.AucunEmploye -> _message.value = UiMessage(R.string.rh_paie_employes_vides, ok = false)
                CreatePayslipUseCase.Result.DejaExistante -> _message.value = UiMessage(R.string.rh_paie_exists, ok = false)
            }
        }
    }

    /** Décodage du bulletin pour l'affichage détail. */
    fun payloadDe(record: OperationRecordEntity): PayslipPayload? = PayslipCodec.decode(record.notes)

    fun nomEmploye(id: Long): String = employees.value.firstOrNull { it.id == id }?.nom ?: "—"
}

private fun dateParts(jour: Int, mois: Int, annee: Int): Long {
    val cal = Calendar.getInstance().apply {
        clear()
        set(annee, mois - 1, jour, 12, 0, 0)
    }
    return cal.timeInMillis
}

/**
 * Écran RH — structure admin standard, 4 onglets.
 */
@Composable
fun RhScreen(
    onBack: () -> Unit,
    /** Vrai lorsqu'une action rapide demande directement la création. */
    openCreate: Boolean = false,
    viewModel: RhViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val absences by viewModel.absences.collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState(initial = "XAF")
    val payslips by viewModel.payslips.collectAsState(initial = emptyList())
    val avances by viewModel.avances.collectAsState(initial = emptyList())
    val busy by viewModel.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf("EMPLOYES") }

    LaunchedEffect(viewModel.message) {
        val msg = viewModel.message.value ?: return@LaunchedEffect
        val text = if (msg.arg != null) context.getString(msg.key, msg.arg) else context.getString(msg.key)
        snackbar.showSnackbar(text)
        viewModel.consumeMessage()
    }

    AdminScaffold(titreRes = R.string.rh_title, onBack = onBack) {
        SnackbarHost(snackbar) { Snackbar(it) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = tab == "EMPLOYES",
                onClick = { tab = "EMPLOYES" },
                label = { Text(stringResource(R.string.rh_tab_employes), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "ABSENCES",
                onClick = { tab = "ABSENCES" },
                label = { Text(stringResource(R.string.rh_tab_absences), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "PAIE",
                onClick = { tab = "PAIE" },
                label = { Text(stringResource(R.string.rh_tab_paie), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "AVANCES",
                onClick = { tab = "AVANCES" },
                label = { Text(stringResource(R.string.rh_tab_avances), fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            "EMPLOYES" -> EmployesTab(
                employees = employees,
                busy = busy,
                devise = devise,
                initialOpen = openCreate,
                onSave = viewModel::saveEmployee,
                onDesactiver = viewModel::desactiver,
            )
            "ABSENCES" -> AbsencesTab(
                employees = employees.filter { it.statut == "ACTIF" },
                absences = absences,
                busy = busy,
                nomEmploye = viewModel::nomEmploye,
                onSave = viewModel::saveAbsence,
                onDelete = viewModel::deleteAbsence,
            )
            "PAIE" -> PaieTab(
                payslips = payslips,
                busy = busy,
                devise = devise,
                payloadDe = viewModel::payloadDe,
                onGenerer = viewModel::genererPaie,
            )
            "AVANCES" -> AvancesTab(
                employees = employees.filter { it.statut == "ACTIF" },
                avances = avances,
                busy = busy,
                devise = devise,
                onSave = viewModel::saveAdvance,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// Onglet Employés
// ---------------------------------------------------------------------------

@Composable
private fun EmployesTab(
    employees: List<EmployeeEntity>,
    busy: Boolean,
    devise: String,
    initialOpen: Boolean,
    onSave: (Long?, String, String, String?, Double, Double, String?) -> Unit,
    onDesactiver: (Long) -> Unit,
) {
    var formOuvert by remember { mutableStateOf(initialOpen) }
    var editionId by remember { mutableStateOf<Long?>(null) }
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var poste by remember { mutableStateOf("") }
    var salaire by remember { mutableStateOf("") }
    var jours by remember { mutableStateOf("26") }
    var notes by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<EmployeeEntity?>(null) }

    fun ouvrirCreer() {
        editionId = null; nom = ""; telephone = ""; poste = ""
        salaire = ""; jours = "26"; notes = ""
        formOuvert = true
    }

    fun ouvrirEdition(e: EmployeeEntity) {
        editionId = e.id; nom = e.nom; telephone = e.telephone; poste = e.poste ?: ""
        salaire = if (e.salaireBase % 1.0 == 0.0) e.salaireBase.toLong().toString() else e.salaireBase.toString()
        jours = if (e.joursMensuels % 1.0 == 0.0) e.joursMensuels.toLong().toString() else e.joursMensuels.toString()
        notes = e.notes ?: ""
        formOuvert = true
    }

    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_tab_employes))
        if (!formOuvert) {
            Button(
                onClick = { ouvrirCreer() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.padding(start = 6.dp))
                Text(stringResource(R.string.rh_employe_new))
            }
            Spacer(Modifier.height(6.dp))
        }

        if (formOuvert) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.05f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text(stringResource(R.string.rh_field_nom)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = telephone,
                            onValueChange = { telephone = it },
                            label = { Text(stringResource(R.string.rh_field_telephone)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = poste,
                            onValueChange = { poste = it },
                            label = { Text(stringResource(R.string.rh_field_poste)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = salaire,
                            onValueChange = { salaire = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                            label = { Text(stringResource(R.string.rh_field_salaire)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = jours,
                            onValueChange = { jours = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                            label = { Text(stringResource(R.string.rh_field_jours)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.rh_field_notes)) },
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = { formOuvert = false }) {
                            Text(stringResource(R.string.rh_btn_cancel))
                        }
                        Button(
                            onClick = {
                                onSave(
                                    editionId,
                                    nom,
                                    telephone,
                                    poste.ifBlank { null },
                                    salaire.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                    jours.replace(',', '.').toDoubleOrNull() ?: 26.0,
                                    notes.ifBlank { null },
                                )
                                formOuvert = false
                            },
                            enabled = !busy,
                        ) {
                            Text(stringResource(R.string.rh_btn_save))
                        }
                    }
                }
            }
        }

        if (employees.isEmpty()) {
            Text(stringResource(R.string.rh_employe_empty), color = MissaMuted, fontSize = 12.sp)
        } else {
            employees.forEach { e ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = e.statut == "ACTIF") { ouvrirEdition(e) },
                    colors = CardDefaults.cardColors(containerColor = MissaCanvas),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(e.nom, color = MissaInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                if (e.statut != "ACTIF") {
                                    Text(
                                        stringResource(R.string.rh_desactive),
                                        color = Red40,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Text(
                                buildString {
                                    append(e.code)
                                    e.poste?.let { append(" — ").append(it) }
                                    append(" · ").append(MoneyUtils.format(e.salaireBase, devise))
                                },
                                color = MissaMuted,
                                fontSize = 11.sp,
                            )
                        }
                        if (e.statut == "ACTIF") {
                            TextButton(onClick = { confirmation = e }) {
                                Text(stringResource(R.string.rh_employe_desactiver), color = Red40, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    confirmation?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(stringResource(R.string.rh_employe_desactiver)) },
            text = { Text(stringResource(R.string.rh_employe_desactiver_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    onDesactiver(e.id)
                }) {
                    Text(stringResource(R.string.rh_btn_save), color = Red40)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text(stringResource(R.string.rh_btn_cancel))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Onglet Absences
// ---------------------------------------------------------------------------

@Composable
private fun AbsencesTab(
    employees: List<EmployeeEntity>,
    absences: List<AbsenceEntity>,
    busy: Boolean,
    nomEmploye: (Long) -> String,
    onSave: (Long, String, Long, Double, String?) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var employeId by remember { mutableStateOf<Long?>(null) }
    var type by remember { mutableStateOf("MALADIE") }
    val aujourdhui = remember { Calendar.getInstance() }
    var jour by remember { mutableStateOf(aujourdhui.get(Calendar.DAY_OF_MONTH).toString()) }
    var mois by remember { mutableStateOf((aujourdhui.get(Calendar.MONTH) + 1).toString()) }
    var annee by remember { mutableStateOf(aujourdhui.get(Calendar.YEAR).toString()) }
    var duree by remember { mutableStateOf("1") }
    var motif by remember { mutableStateOf("") }

    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_absence_nouvelle))

        EmployeeSelectorField(
            label = R.string.rh_field_employe,
            choices = employees.map { it.id to it.nom },
            selectedId = employeId,
            onSelect = { employeId = it },
            emptyLabel = stringResource(R.string.rh_employe_select),
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("MALADIE" to R.string.rh_absence_maladie, "CONGE" to R.string.rh_absence_conge, "AUTRE" to R.string.rh_absence_autre).forEach { (v, res) ->
                FilterChip(
                    selected = type == v,
                    onClick = { type = v },
                    label = { Text(stringResource(res), fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = jour,
                onValueChange = { jour = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.rh_absence_date, "jj")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = mois,
                onValueChange = { mois = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.rh_absence_date, "mm")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = annee,
                onValueChange = { annee = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.rh_absence_date, "aaaa")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
                value = duree,
                onValueChange = { duree = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                label = { Text(stringResource(R.string.rh_absence_duree)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = motif,
            onValueChange = { motif = it },
            label = { Text(stringResource(R.string.rh_absence_motif)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val d = dateParts(
                    jour.toIntOrNull() ?: 1,
                    mois.toIntOrNull() ?: 1,
                    annee.toIntOrNull() ?: aujourdhui.get(Calendar.YEAR),
                )
                onSave(
                    employeId ?: 0L,
                    type,
                    d,
                    duree.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    motif.ifBlank { null },
                )
                motif = ""
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rh_btn_save))
        }
    }

    Spacer(Modifier.height(8.dp))
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_absence_liste))
        if (absences.isEmpty()) {
            Text(stringResource(R.string.rh_absence_empty), color = MissaMuted, fontSize = 12.sp)
        } else {
            absences.forEach { a ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            buildString {
                                append(nomEmploye(a.employeeId))
                                append(" — ")
                                append(stringResource(R.string.rh_absence_type))
                                append(" · ").append(DateUtils.formatDate(a.dateDebut))
                            },
                            color = MissaInk,
                            fontSize = 12.sp,
                        )
                        Text(
                            stringResource(R.string.rh_absence_jours, a.dureeJours.toInt()),
                            color = MissaMuted,
                            fontSize = 11.sp,
                        )
                    }
                    IconButton(onClick = { onDelete(a.id) }, enabled = !busy) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.rh_absence_delete), tint = Red40)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Onglet Paie
// ---------------------------------------------------------------------------

@Composable
private fun PaieTab(
    payslips: List<OperationRecordEntity>,
    busy: Boolean,
    devise: String,
    payloadDe: (OperationRecordEntity) -> PayslipPayload?,
    onGenerer: (Int, Int) -> Unit,
) {
    val aujourdhui = remember { Calendar.getInstance() }
    var mois by remember { mutableStateOf((aujourdhui.get(Calendar.MONTH) + 1).toString()) }
    var annee by remember { mutableStateOf(aujourdhui.get(Calendar.YEAR).toString()) }
    var ouvert by remember { mutableStateOf<String?>(null) }

    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_tab_paie))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = mois,
                onValueChange = { mois = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.rh_paie_mois)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = annee,
                onValueChange = { annee = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.rh_paie_annee)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onGenerer(
                    mois.toIntOrNull() ?: 1,
                    annee.toIntOrNull() ?: aujourdhui.get(Calendar.YEAR),
                )
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rh_paie_generer))
        }
        Text(
            stringResource(R.string.rh_paie_info),
            color = MissaMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    Spacer(Modifier.height(8.dp))
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_paie_liste))
        if (payslips.isEmpty()) {
            Text(stringResource(R.string.rh_paie_empty), color = MissaMuted, fontSize = 12.sp)
        } else {
            payslips.forEach { record ->
                val payload = payloadDe(record)
                val ouvertCe = ouvert == record.reference
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ouvert = if (ouvertCe) null else record.reference },
                    colors = CardDefaults.cardColors(containerColor = MissaCanvas),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.reference, color = MissaInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                payload?.let {
                                    Text(
                                        stringResource(
                                            R.string.rh_paie_lignes,
                                            it.lignes.size,
                                        ),
                                        color = MissaMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            Text(
                                MoneyUtils.format(record.amount ?: 0.0, devise),
                                color = Green60,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                        if (ouvertCe && payload != null) {
                            Spacer(Modifier.height(8.dp))
                            payload.lignes.forEach { ligne ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(ligne.nom, color = MissaInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    if (ligne.absencesJours > 0.0) {
                                        Text(
                                            stringResource(R.string.rh_paie_abs, ligne.absencesJours.toInt()),
                                            color = Red40,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    if (ligne.avancement > 0.0) {
                                        Text(
                                            stringResource(R.string.rh_paie_av, MoneyUtils.format(ligne.avancement, devise)),
                                            color = Red40,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    Text(
                                        MoneyUtils.format(ligne.net, devise),
                                        color = MissaInk,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Onglet Avances
// ---------------------------------------------------------------------------

@Composable
private fun AvancesTab(
    employees: List<EmployeeEntity>,
    avances: List<OperationRecordEntity>,
    busy: Boolean,
    devise: String,
    onSave: (Long, Double, String?) -> Unit,
) {
    var employeId by remember { mutableStateOf<Long?>(null) }
    var montant by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }

    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_avance_nouvelle))
        EmployeeSelectorField(
            label = R.string.rh_field_employe,
            choices = employees.map { it.id to it.nom },
            selectedId = employeId,
            onSelect = { employeId = it },
            emptyLabel = stringResource(R.string.rh_employe_select),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = montant,
            onValueChange = { montant = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            label = { Text(stringResource(R.string.rh_avance_montant)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = motif,
            onValueChange = { motif = it },
            label = { Text(stringResource(R.string.rh_avance_motif)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(employeId ?: 0L, montant.replace(',', '.').toDoubleOrNull() ?: 0.0, motif.ifBlank { null })
                montant = ""; motif = ""
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rh_btn_save))
        }
    }

    Spacer(Modifier.height(8.dp))
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        MissaSectionTitle(title = stringResource(R.string.rh_avance_liste))
        if (avances.isEmpty()) {
            Text(stringResource(R.string.rh_avance_empty), color = MissaMuted, fontSize = 12.sp)
        } else {
            avances.forEach { record ->
                val payload = AdvanceCodec.decode(record.notes)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            buildString {
                                append(record.reference)
                                append(" — ")
                                append(payload?.employeeNom ?: "—")
                            },
                            color = MissaInk,
                            fontSize = 12.sp,
                        )
                        Text(DateUtils.formatDate(record.createdAt), color = MissaMuted, fontSize = 11.sp)
                    }
                    Text(
                        "-${MoneyUtils.format(record.amount ?: 0.0, devise)}",
                        color = Red40,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sélecteur d'employé — même composant que le sélecteur client (spec §46)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeSelectorField(
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
