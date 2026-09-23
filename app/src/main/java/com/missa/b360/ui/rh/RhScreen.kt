package com.missa.b360.ui.rh

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.EmployeeStatus
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Rouge rubis caractéristique du module RH — source unique : [AppModule.RH]. */
private val RougeRh: Color get() = AppModule.RH.couleur

private enum class OngletRh { EMPLOYES, PAIE }

@Composable
fun RhScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    vm: RhViewModel = hiltViewModel(),
) {
    val employees by vm.employees.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var onglet by remember { mutableStateOf(OngletRh.EMPLOYES) }
    var dialogueNouvelEmploye by remember { mutableStateOf(openCreate) }
    var employePourAbsence by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employePourAvance by remember { mutableStateOf<EmployeeEntity?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouvelEmploye = false
            employePourAbsence = null
            employePourAvance = null
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    val actifs = employees.filter { it.statut == EmployeeStatus.ACTIF.name }
    val masseSalariale = actifs.sumOf { it.salaireBase }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_rh),
            onBack = onBack,
            couleurFond = AppModule.RH.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Carte Synthèse RH ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RougeRh.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.rh_masse_salariale),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(masseSalariale, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.rh_effectif_actif, actifs.size),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MissaInk,
                            )
                            Text(
                                stringResource(R.string.rh_total_inscrits, employees.size),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Onglets / Actions Rapides ---
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { dialogueNouvelEmploye = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RougeRh, contentColor = Color.White),
                    ) {
                        Icon(painterResource(Iv.PersonAdd), null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.rh_nouvel_employe), fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { vm.genererBulletinPaie() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                    ) {
                        Icon(painterResource(Iv.Payments), null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.rh_generer_paie), fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            // --- Message retour ---
            message?.let { msg ->
                item {
                    val (texte, couleur) = when (msg) {
                        is RhViewModel.ActionMessage.Succes -> msg.texte to Color(0xFF15803D)
                        is RhViewModel.ActionMessage.Erreur -> msg.texte to Color(0xFFB91C1C)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f)) {
                        Text(
                            texte,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = couleur,
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                        )
                    }
                }
            }

            // --- Titre Liste Employés ---
            item {
                Text(
                    stringResource(R.string.rh_titre_personnel),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (employees.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Person,
                        title = stringResource(R.string.rh_aucun_employe),
                        description = stringResource(R.string.rh_aucun_employe_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(employees, key = { it.id }) { emp ->
                    CarteEmploye(
                        employe = emp,
                        devise = devise,
                        onAbsence = { employePourAbsence = emp },
                        onAvance = { employePourAvance = emp },
                        onDesactiver = { vm.desactiverEmploye(emp.id) },
                    )
                }
            }
        }
    }

    // --- Dialogues ---
    if (dialogueNouvelEmploye) {
        DialogueNouvelEmploye(
            onFermer = { dialogueNouvelEmploye = false },
            onValider = { nom, tel, poste, salaire, jours ->
                vm.creerEmploye(nom, tel, poste, salaire, jours)
            },
        )
    }

    employePourAbsence?.let { emp ->
        DialogueAbsence(
            employe = emp,
            onFermer = { employePourAbsence = null },
            onValider = { type, duree, motif ->
                vm.declarerAbsence(emp.id, type, duree, motif)
            },
        )
    }

    employePourAvance?.let { emp ->
        DialogueAvance(
            employe = emp,
            devise = devise,
            onFermer = { employePourAvance = null },
            onValider = { montant, motif ->
                vm.verserAvance(emp.id, montant, motif)
            },
        )
    }
}

@Composable
private fun CarteEmploye(
    employe: EmployeeEntity,
    devise: String,
    onAbsence: () -> Unit,
    onAvance: () -> Unit,
    onDesactiver: () -> Unit,
) {
    val estActif = employe.statut == EmployeeStatus.ACTIF.name

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(employe.nom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        Spacer(Modifier.width(6.dp))
                        Text(employe.code, fontSize = 10.sp, color = MissaMuted)
                    }
                    employe.poste?.let {
                        Text(it, fontSize = 11.sp, color = RougeRh)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (estActif) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                ) {
                    Text(
                        stringResource(if (estActif) R.string.rh_statut_actif else R.string.rh_statut_inactif),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (estActif) Color(0xFF15803D) else MissaMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(employe.telephone, fontSize = 11.sp, color = MissaMuted)
                Text(
                    stringResource(R.string.rh_salaire_mensuel, fmtValeur(employe.salaireBase, devise)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            if (estActif) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAbsence,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) { Text(stringResource(R.string.rh_action_absence), fontSize = 10.5.sp, color = MissaInk) }
                    OutlinedButton(
                        onClick = onAvance,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) { Text(stringResource(R.string.rh_action_avance), fontSize = 10.5.sp, color = MissaInk) }
                    IconButton(onClick = onDesactiver, modifier = Modifier.size(32.dp)) {
                        Icon(painterResource(Iv.Prohibit), null, tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueNouvelEmploye(
    onFermer: () -> Unit,
    onValider: (String, String, String?, Double, Double) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var poste by remember { mutableStateOf("") }
    var salaire by remember { mutableStateOf("") }
    var jours by remember { mutableStateOf("26") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.rh_nouveau_titre), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.rh_champ_nom), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text(stringResource(R.string.rh_champ_tel), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = poste,
                    onValueChange = { poste = it },
                    label = { Text(stringResource(R.string.rh_champ_poste), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = salaire,
                    onValueChange = { salaire = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.rh_champ_salaire), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = salaire.toDoubleOrNull() ?: 0.0
                    val j = jours.toDoubleOrNull() ?: 26.0
                    onValider(nom, telephone, poste.ifBlank { null }, s, j)
                },
                enabled = nom.isNotBlank() && telephone.isNotBlank(),
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun DialogueAbsence(
    employe: EmployeeEntity,
    onFermer: () -> Unit,
    onValider: (String, Double, String?) -> Unit,
) {
    var type by remember { mutableStateOf("CONGE") }
    var duree by remember { mutableStateOf("1") }
    var motif by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.rh_declarer_absence, employe.nom), fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CONGE", "MALADIE", "AUTRE").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RougeRh.copy(alpha = 0.2f)),
                        )
                    }
                }
                OutlinedTextField(
                    value = duree,
                    onValueChange = { duree = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.rh_champ_duree_jours), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text(stringResource(R.string.rh_champ_motif), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(type, duree.toDoubleOrNull() ?: 1.0, motif.ifBlank { null }) },
                enabled = (duree.toDoubleOrNull() ?: 0.0) > 0.0,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun DialogueAvance(
    employe: EmployeeEntity,
    devise: String,
    onFermer: () -> Unit,
    onValider: (Double, String?) -> Unit,
) {
    var montant by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.rh_verser_avance, employe.nom), fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = montant,
                    onValueChange = { montant = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.rh_champ_montant_avance), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text(stringResource(R.string.rh_champ_motif), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(montant.toDoubleOrNull() ?: 0.0, motif.ifBlank { null }) },
                enabled = (montant.toDoubleOrNull() ?: 0.0) > 0.0,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
