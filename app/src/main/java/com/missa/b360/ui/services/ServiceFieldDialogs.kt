package com.missa.b360.ui.services

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.domain.model.ServicePriority
import com.missa.b360.core.domain.model.ServiceRequestType
import com.missa.b360.ui.components.MissaMenuDeroulant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun ServiceRequestDialog(
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onSubmit: (ClientEntity?, String, ServiceRequestType, ServicePriority, String?, String?) -> Unit,
) {
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var description by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ServiceRequestType.BREAKDOWN) }
    var priority by remember { mutableStateOf(ServicePriority.NORMAL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle demande de service") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ClientChooser(clients, client, { client = it })
                SimpleChooser("Type : ${type.label()}", ServiceRequestType.entries.map { it.label() to it }, { type = it })
                SimpleChooser("Priorité : ${priority.label()}", ServicePriority.entries.map { it.label() to it }, { priority = it })
                OutlinedTextField(description, { description = it }, label = { Text("Motif / description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(contact, { contact = it }, label = { Text("Contact (optionnel)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone (optionnel)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(client, description, type, priority, contact, phone) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
internal fun ServiceOrderDialog(
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onSubmit: (ClientEntity?, String, ServicePriority) -> Unit,
) {
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(ServicePriority.NORMAL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un ordre d'intervention") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ClientChooser(clients, client, { client = it })
                SimpleChooser("Priorité : ${priority.label()}", ServicePriority.entries.map { it.label() to it }, { priority = it })
                OutlinedTextField(description, { description = it }, label = { Text("Intervention à réaliser") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(client, description, priority) }) { Text("Créer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
internal fun ServiceScheduleDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long, Long) -> Unit,
) {
    var technician by remember { mutableStateOf<EmployeeEntity?>(null) }
    val startDefault = remember {
        Calendar.getInstance().apply { set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.HOUR_OF_DAY, 1) }.time
    }
    var start by remember { mutableStateOf(scheduleFormat.format(startDefault)) }
    var end by remember { mutableStateOf(scheduleFormat.format(Date(startDefault.time + 60 * 60 * 1000L))) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planifier et affecter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EmployeeChooser(employees, technician, { technician = it })
                OutlinedTextField(start, { start = it }, label = { Text("Début (jj/MM/aaaa HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(end, { end = it }, label = { Text("Fin (jj/MM/aaaa HH:mm)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startAt = parseSchedule(start)
                val endAt = parseSchedule(end)
                if (technician != null && startAt != null && endAt != null) onSubmit(technician!!.id, startAt, endAt)
            }) { Text("Planifier") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
internal fun ServiceReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Boolean, String?, String?) -> Unit,
    onAttachPhoto: (String) -> Unit,
) {
    val context = LocalContext.current
    var diagnosis by remember { mutableStateOf("") }
    var work by remember { mutableStateOf("") }
    var signer by remember { mutableStateOf("") }
    var signatureUri by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf(true) }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            signatureUri = it.toString()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onAttachPhoto(it.toString())
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rapport d'intervention") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(diagnosis, { diagnosis = it }, label = { Text("Diagnostic") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(work, { work = it }, label = { Text("Travaux réalisés") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(signer, { signer = it }, label = { Text("Nom du représentant client") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resolved, onCheckedChange = { resolved = it })
                    Text("Problème résolu")
                }
                OutlinedButton(onClick = { signaturePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (signatureUri == null) "Choisir la signature client" else "Signature jointe")
                }
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter une photo") }
                Text("La signature est conservée comme fichier local; aucun PDF n'est généré automatiquement.")
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(diagnosis, work, resolved, signer, signatureUri) }) { Text("Envoyer le rapport") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
internal fun ServiceTimesheetDialog(onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var minutes by remember { mutableStateOf("60") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saisir le temps de travail") },
        text = { OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Temps travaillé (minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { TextButton(onClick = { minutes.toIntOrNull()?.let(onSubmit) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun ClientChooser(clients: List<ClientEntity>, selected: ClientEntity?, onSelect: (ClientEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.nom ?: "Sélectionner un client") }
        MissaMenuDeroulant(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 260.dp)) {
            clients.forEach { client -> DropdownMenuItem(text = { Text(client.nom) }, onClick = { onSelect(client); expanded = false }) }
        }
    }
}

@Composable
private fun EmployeeChooser(employees: List<EmployeeEntity>, selected: EmployeeEntity?, onSelect: (EmployeeEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.nom ?: "Affecter un technicien") }
        MissaMenuDeroulant(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 260.dp)) {
            employees.forEach { employee -> DropdownMenuItem(text = { Text(employee.nom) }, onClick = { onSelect(employee); expanded = false }) }
            if (employees.isEmpty()) DropdownMenuItem(text = { Text("Aucun employé actif dans RH") }, onClick = { expanded = false })
        }
    }
}

@Composable
private fun <T> SimpleChooser(label: String, options: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        MissaMenuDeroulant(expanded, { expanded = false }) {
            options.forEach { (title, value) -> DropdownMenuItem(text = { Text(title) }, onClick = { onSelect(value); expanded = false }) }
        }
    }
}

private val scheduleFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }
private fun parseSchedule(value: String): Long? = runCatching { scheduleFormat.parse(value)?.time }.getOrNull()
private fun ServicePriority.label() = when (this) { ServicePriority.URGENT -> "Urgente"; ServicePriority.HIGH -> "Haute"; ServicePriority.NORMAL -> "Normale"; ServicePriority.LOW -> "Basse" }
private fun ServiceRequestType.label() = when (this) { ServiceRequestType.BREAKDOWN -> "Panne"; ServiceRequestType.MAINTENANCE -> "Maintenance"; ServiceRequestType.INSTALLATION -> "Installation"; ServiceRequestType.CUSTOMER_QUERY -> "Demande client"; ServiceRequestType.WARRANTY -> "Garantie"; ServiceRequestType.OTHER -> "Autre" }
