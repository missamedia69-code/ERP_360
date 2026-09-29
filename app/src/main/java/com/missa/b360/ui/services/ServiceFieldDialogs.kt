package com.missa.b360.ui.services

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.domain.model.ServicePriority
import com.missa.b360.core.domain.model.ServiceRequestType
import com.missa.b360.ui.components.*
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaMuted
import java.util.Calendar

private val CouleurServices get() = AppModule.SERVICES.couleur

/** Tuiles de priorité, de la plus basse à la plus urgente. */
@Composable
private fun ChoixPriorite(priorite: ServicePriority, onPriorite: (ServicePriority) -> Unit) {
    MissaChoixTuiles(
        options = listOf(ServicePriority.LOW, ServicePriority.NORMAL, ServicePriority.HIGH, ServicePriority.URGENT).map {
            MissaTuile(it, stringResource(priorityLabelRes(it.name)))
        },
        selection = priorite,
        onSelection = onPriorite,
    )
}

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

    MissaFormDialogue(
        titre = stringResource(R.string.srvf_nouvelle_demande),
        icone = Iv.Build,
        couleur = CouleurServices,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = description.isNotBlank(),
        onValider = {
            onSubmit(client, description.trim(), type, priority, contact.trim().ifBlank { null }, phone.trim().ifBlank { null })
        },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_client), numero = 1) {
            MissaChampListe(
                libelle = stringResource(R.string.form_client),
                options = clients.map { it to it.nom },
                selection = client,
                onSelection = { client = it },
                icone = Iv.Person,
                placeholder = stringResource(R.string.sales_select_client),
            )
            MissaRangee {
                MissaChampTexte(contact, { contact = it }, stringResource(R.string.srvf_contact), modifier = Modifier.weight(1f), icone = Iv.PersonOutline)
                MissaChampTexte(phone, { phone = it }, stringResource(R.string.srvf_telephone), modifier = Modifier.weight(1f), icone = Iv.Call, clavier = MissaClavier.TELEPHONE)
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 2) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(ServiceRequestType.BREAKDOWN, stringResource(R.string.srvf_type_panne), Iv.Warning),
                    MissaTuile(ServiceRequestType.MAINTENANCE, stringResource(R.string.srvf_type_maintenance), Iv.HammerWrench),
                    MissaTuile(ServiceRequestType.INSTALLATION, stringResource(R.string.srvf_type_installation), Iv.Construction),
                    MissaTuile(ServiceRequestType.CUSTOMER_QUERY, stringResource(R.string.srvf_type_demande), Iv.Chat),
                    MissaTuile(ServiceRequestType.WARRANTY, stringResource(R.string.srvf_type_garantie), Iv.Security),
                    MissaTuile(ServiceRequestType.OTHER, stringResource(R.string.srvf_type_autre), Iv.MoreHoriz),
                ),
                selection = type,
                onSelection = { type = it },
                colonnes = 3,
            )
        }
        MissaFormSection(titre = stringResource(R.string.srvf_priorite), numero = 3) {
            ChoixPriorite(priority) { priority = it }
            MissaChampTexte(description, { description = it }, stringResource(R.string.srvf_motif), icone = Iv.Description, requis = true, lignes = 3)
        }
    }
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

    MissaFormDialogue(
        titre = stringResource(R.string.srvf_nouvel_ordre),
        icone = Iv.Checklist,
        couleur = CouleurServices,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.srvf_creer),
        validerActif = description.isNotBlank(),
        onValider = { onSubmit(client, description.trim(), priority) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 1) {
            MissaChampListe(
                libelle = stringResource(R.string.form_client),
                options = clients.map { it to it.nom },
                selection = client,
                onSelection = { client = it },
                icone = Iv.Person,
                placeholder = stringResource(R.string.sales_select_client),
            )
            MissaChampTexte(description, { description = it }, stringResource(R.string.srvf_intervention), icone = Iv.Build, requis = true, lignes = 3)
        }
        MissaFormSection(titre = stringResource(R.string.srvf_priorite), numero = 2) {
            ChoixPriorite(priority) { priority = it }
        }
    }
}

@Composable
internal fun ServiceScheduleDialog(
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onSubmit: (Long, Long, Long) -> Unit,
) {
    var technician by remember { mutableStateOf<EmployeeEntity?>(null) }
    val startDefault = remember {
        Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.HOUR_OF_DAY, 1)
        }.timeInMillis
    }
    var start by remember { mutableStateOf<Long?>(startDefault) }
    var end by remember { mutableStateOf<Long?>(startDefault + 60 * 60 * 1000L) }
    val ordreValide = start != null && end != null && end!! > start!!

    MissaFormDialogue(
        titre = stringResource(R.string.srvf_planifier_affecter),
        icone = Iv.Calendar,
        couleur = CouleurServices,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.srvf_planifier),
        validerActif = technician != null && ordreValide,
        onValider = {
            val t = technician
            val a = start
            val b = end
            if (t != null && a != null && b != null) onSubmit(t.id, a, b)
        },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_technicien), numero = 1) {
            MissaChampListe(
                libelle = stringResource(R.string.form_technicien),
                options = employees.map { it to it.nom },
                selection = technician,
                onSelection = { technician = it },
                icone = Iv.Person,
                requis = true,
                aide = if (employees.isEmpty()) stringResource(R.string.srvf_aucun_employe) else null,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_planification), numero = 2) {
            MissaRangee {
                MissaChampDateHeure(start, { start = it }, stringResource(R.string.srvf_debut), requis = true, modifier = Modifier.weight(1f))
                MissaChampDateHeure(
                    end, { end = it }, stringResource(R.string.srvf_fin), requis = true,
                    erreur = if (start != null && end != null && !ordreValide) stringResource(R.string.srvf_erreur_fin) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
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
    var photos by remember { mutableStateOf(0) }
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
            photos++
        }
    }

    MissaFormDialogue(
        titre = stringResource(R.string.srvf_rapport_titre),
        icone = Iv.Description,
        couleur = CouleurServices,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.srvf_envoyer_rapport),
        validerActif = diagnosis.isNotBlank() && work.isNotBlank(),
        onValider = { onSubmit(diagnosis.trim(), work.trim(), resolved, signer.trim().ifBlank { null }, signatureUri) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 1) {
            MissaChampTexte(diagnosis, { diagnosis = it }, stringResource(R.string.srvf_diagnostic), icone = Iv.Search, requis = true, lignes = 2)
            MissaChampTexte(work, { work = it }, stringResource(R.string.srvf_travaux), icone = Iv.Build, requis = true, lignes = 2)
            MissaCaseACocher(resolved, { resolved = it }, stringResource(R.string.srvf_resolu))
        }
        MissaFormSection(titre = stringResource(R.string.srvf_validation_client), numero = 2) {
            MissaChampTexte(signer, { signer = it }, stringResource(R.string.srvf_representant), icone = Iv.Person)
            MissaRangee {
                MissaBoutonSecondaire(
                    texte = stringResource(if (signatureUri == null) R.string.srvf_choisir_signature else R.string.srvf_signature_jointe),
                    onClick = { signaturePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                )
                MissaBoutonSecondaire(
                    texte = if (photos == 0) stringResource(R.string.srvf_ajouter_photo) else stringResource(R.string.srvf_photos_x, photos),
                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(stringResource(R.string.srvf_signature_note), fontSize = 11.sp, color = MissaMuted)
        }
    }
}

@Composable
internal fun ServiceTimesheetDialog(onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var minutes by remember { mutableStateOf("60") }
    MissaFormDialogue(
        titre = stringResource(R.string.srvf_temps_titre),
        icone = Iv.Schedule,
        couleur = CouleurServices,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = (minutes.toIntOrNull() ?: 0) > 0,
        onValider = { minutes.toIntOrNull()?.let(onSubmit) },
    ) {
        MissaChampTexte(minutes, { minutes = it }, stringResource(R.string.srvf_minutes), icone = Iv.Schedule, clavier = MissaClavier.ENTIER, requis = true)
    }
}
