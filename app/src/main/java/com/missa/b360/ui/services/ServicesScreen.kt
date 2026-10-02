package com.missa.b360.ui.services

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.ButtonDefaults
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
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
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.EtapePrestation
import com.missa.b360.core.domain.model.ModeFacturation
import com.missa.b360.core.domain.model.Prestation
import com.missa.b360.core.domain.model.PrestationRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

/** Rose fuchsia caractéristique du module Services — source unique : [AppModule.SERVICES]. */
private val RoseServices: Color get() = AppModule.SERVICES.couleur

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val onClick: () -> Unit,
)

@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    openCreate: Boolean = false,
    vm: ServicesViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()
    val fieldVm: ServiceFieldViewModel = hiltViewModel()
    val fieldState by fieldVm.state.collectAsStateWithLifecycle()
    val fieldMessage by fieldVm.message.collectAsStateWithLifecycle()
    val fieldBusy by fieldVm.busy.collectAsStateWithLifecycle()

    var requestDialog by remember { mutableStateOf(openCreate) }
    var orderDialog by remember { mutableStateOf(false) }
    var orderToSchedule by remember { mutableStateOf<com.missa.b360.core.data.entity.ServiceWorkOrderEntity?>(null) }
    var orderToReport by remember { mutableStateOf<com.missa.b360.core.data.entity.ServiceWorkOrderEntity?>(null) }
    var orderToTimesheet by remember { mutableStateOf<com.missa.b360.core.data.entity.ServiceWorkOrderEntity?>(null) }

    LaunchedEffect(fieldMessage) {
        if (fieldMessage != null) {
            kotlinx.coroutines.delay(4_000)
            fieldVm.clearMessage()
        }
    }

    var dialogueNouvellePrestation by remember { mutableStateOf(false) }
    var prestationAAnnuler by remember { mutableStateOf<Prestation?>(null) }
    var prestationAjusterHeures by remember { mutableStateOf<Prestation?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouvellePrestation = false
            prestationAAnnuler = null
            prestationAjusterHeures = null
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_services),
            onBack = onBack,
            couleurFond = AppModule.SERVICES.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ServiceFieldOverview(
                    requests = fieldState.requests,
                    workOrders = fieldState.workOrders,
                    onNewRequest = { requestDialog = true },
                    onNewOrder = { orderDialog = true },
                )
            }
            fieldMessage?.let { result ->
                item {
                    val (text, color) = when (result) {
                        is ServiceFieldViewModel.Message.Success -> (result.reference?.let { "${result.text} · $it" } ?: result.text) to Color(0xFF15803D)
                        is ServiceFieldViewModel.Message.Error -> result.reason.toServicesError() to Color(0xFFB91C1C)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
                        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.fillMaxWidth().padding(7.dp))
                    }
                }
            }
            if (fieldState.requests.isNotEmpty()) {
                item { Text("Demandes clients", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk) }
                items(fieldState.requests, key = { "request-${it.id}" }) { request ->
                    ServiceRequestCard(
                        request = request,
                        onQualify = { fieldVm.qualifyRequest(request.id) },
                        onConvert = { fieldVm.convertRequest(request.id) },
                    )
                }
            }
            if (fieldState.workOrders.isNotEmpty()) {
                item { Text("Ordres d'intervention", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk) }
                items(fieldState.workOrders, key = { "service-order-${it.id}" }) { order ->
                    ServiceWorkOrderCard(
                        order = order,
                        report = fieldState.reports.firstOrNull { it.workOrderId == order.id },
                        technicianName = fieldState.employees.firstOrNull { it.id == order.technicianId }?.nom,
                        onSchedule = { orderToSchedule = order },
                        onTransition = { fieldVm.transition(order.id, it) },
                        onReport = { orderToReport = order },
                        onTimesheet = { orderToTimesheet = order },
                        onApprove = { fieldVm.approveReport(order.id) },
                    )
                }
            }
            if (etat.prestations.isNotEmpty()) {
                item {
                    Text("Prestations simples · historique", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
                // --- Carte Synthèse Prestations ---
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RoseServices.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(
                            stringResource(R.string.srv_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.chiffreRealise, devise),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.srv_en_cours, etat.enCours),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseServices,
                            )
                            Text(
                                stringResource(R.string.srv_carnet, fmtValeur(etat.carnet, devise)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle (Tuiles Carrées d'accès & création) ---
            item {
                Text(
                    stringResource(R.string.srv_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.RequestQuote,
                        titre = stringResource(R.string.srv_tuile_toutes),
                        sousTitre = stringResource(R.string.srv_nb_count, etat.prestations.size),
                        onClick = { vm.filtrer(null) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Schedule,
                        titre = stringResource(R.string.srv_etape_planifiee),
                        sousTitre = stringResource(R.string.srv_nb_count, etat.prestations.count { it.etape == EtapePrestation.PLANIFIEE && !it.annulee }),
                        onClick = { vm.filtrer(EtapePrestation.PLANIFIEE) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Build,
                        titre = stringResource(R.string.srv_etape_en_cours),
                        sousTitre = stringResource(R.string.srv_nb_count, etat.prestations.count { it.etape == EtapePrestation.EN_COURS && !it.annulee }),
                        onClick = { vm.filtrer(EtapePrestation.EN_COURS) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Add,
                        titre = "Nouvelle demande",
                        sousTitre = stringResource(R.string.st_creer),
                        onClick = { requestDialog = true },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileServices(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = when (tuile.titre) {
                                stringResource(R.string.srv_tuile_toutes) -> filtre == null
                                stringResource(R.string.srv_etape_planifiee) -> filtre == EtapePrestation.PLANIFIEE
                                stringResource(R.string.srv_etape_en_cours) -> filtre == EtapePrestation.EN_COURS
                                else -> false
                            },
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Message retour ---
            message?.let { msg ->
                item {
                    val (texte, couleur) = when (msg) {
                        is ServicesViewModel.Message.Creee -> stringResource(R.string.srv_msg_creee, msg.reference) to Color(0xFF15803D)
                        ServicesViewModel.Message.Avance -> stringResource(R.string.srv_msg_avance) to Color(0xFF15803D)
                        ServicesViewModel.Message.Annulee -> stringResource(R.string.srv_msg_annulee) to Color(0xFFB91C1C)
                        ServicesViewModel.Message.HeuresAjustees -> stringResource(R.string.srv_msg_heures) to Color(0xFF15803D)
                        ServicesViewModel.Message.EtapeFinale -> stringResource(R.string.srv_msg_finale) to MissaMuted
                        else -> stringResource(R.string.ach_erreur) to Color(0xFFB91C1C)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f)) {
                        Text(
                            texte,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = couleur,
                            modifier = Modifier.fillMaxWidth().padding(7.dp),
                        )
                    }
                }
            }

            // --- Liste des Prestations ---
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.srv_titre_registre),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                        modifier = Modifier.weight(1f),
                    )
                    if (filtre != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = RoseServices.copy(alpha = 0.2f)) {
                            Text(
                                stringResource(filtre!!.libelleRes),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseServices,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            items(etat.prestations, key = { it.record.id }) { prestation ->
                CartePrestation(
                    prestation = prestation,
                    devise = devise,
                    onAvancer = { vm.avancer(prestation.record.id) },
                    onAjusterHeures = { prestationAjusterHeures = prestation },
                    onAnnuler = { prestationAAnnuler = prestation },
                )
            }
            } else if (fieldState.requests.isEmpty() && fieldState.workOrders.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.RequestQuote,
                        title = stringResource(R.string.srv_aucune),
                        description = stringResource(R.string.srv_aucune_desc),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }

    if (requestDialog) {
        ServiceRequestDialog(
            clients = fieldState.clients,
            onDismiss = { requestDialog = false },
            onSubmit = { client, description, type, priority, contact, phone ->
                fieldVm.createRequest(client, description, type, priority, contact = contact, phone = phone)
                requestDialog = false
            },
        )
    }
    if (orderDialog) {
        ServiceOrderDialog(
            clients = fieldState.clients,
            onDismiss = { orderDialog = false },
            onSubmit = { client, description, priority ->
                fieldVm.createWorkOrder(client, description, priority)
                orderDialog = false
            },
        )
    }
    orderToSchedule?.let { order ->
        ServiceScheduleDialog(
            employees = fieldState.employees,
            onDismiss = { orderToSchedule = null },
            onSubmit = { technicianId, startAt, endAt ->
                fieldVm.schedule(order.id, technicianId, startAt, endAt)
                orderToSchedule = null
            },
        )
    }
    orderToReport?.let { order ->
        ServiceReportDialog(
            onDismiss = { orderToReport = null },
            onSubmit = { diagnosis, work, resolved, signer, signatureUri ->
                fieldVm.submitReport(order.id, diagnosis, work, resolved, signer, signatureUri)
                orderToReport = null
            },
            onAttachPhoto = { uri -> fieldVm.addAttachment(order.id, uri) },
        )
    }
    orderToTimesheet?.let { order ->
        ServiceTimesheetDialog(
            onDismiss = { orderToTimesheet = null },
            onSubmit = { minutes ->
                order.technicianId?.let { fieldVm.addTimesheet(order.id, it, minutes) }
                orderToTimesheet = null
            },
        )
    }

    if (dialogueNouvellePrestation) {
        DialogueNouvellePrestation(
            clients = etat.clients,
            enCours = enCours,
            onFermer = { dialogueNouvellePrestation = false },
            onValider = { cl, intitule, mode, tarif, heures, inter, lieu ->
                vm.creer(cl, intitule, mode, tarif, heures, inter, lieu)
            },
        )
    }

    prestationAjusterHeures?.let { p ->
        DialogueAjusterHeures(
            prestation = p,
            onFermer = { prestationAjusterHeures = null },
            onValider = { h ->
                vm.ajusterHeures(p.record.id, h)
            },
        )
    }

    prestationAAnnuler?.let { p ->
        AlertDialog(
            onDismissRequest = { prestationAAnnuler = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.ach_confirmer_annulation, p.record.reference), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = { vm.annuler(p.record.id) }) {
                    Text(stringResource(R.string.ach_annuler), color = Color(0xFFB91C1C))
                }
            },
            dismissButton = {
                TextButton(onClick = { prestationAAnnuler = null }) {
                    Text(stringResource(R.string.st_annuler), color = MissaInk)
                }
            },
        )
    }
}

private fun com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.toServicesError(): String = when (this) {
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.Invalid -> "Informations invalides ou champs obligatoires manquants."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.NotFound -> "Élément ou technicien introuvable."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.Forbidden -> "Action non autorisée pour ce rôle."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.ReadOnly -> "Licence en lecture seule."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.ModuleInactive -> "Le module Services n'est pas activé dans ce profil."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.InvalidTransition -> "Cette étape ne peut pas être modifiée ainsi."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.ScheduleConflict -> "Ce technicien a déjà une intervention sur ce créneau."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.TechnicianUnavailable -> "Technicien inactif ou indisponible."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.SignatureRequired -> "Une signature client enregistrée est requise."
    com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.SelfApprovalNotAllowed -> "La validation doit être faite par une autre personne que le créateur."
    is com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases.Result.Success -> "Terminé."
}

@Composable
private fun TuileServices(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) RoseServices.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) RoseServices else MissaBorder),
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CartePrestation(
    prestation: Prestation,
    devise: String,
    onAvancer: () -> Unit,
    onAjusterHeures: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val dateStr = remember(prestation.record.createdAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(prestation.record.createdAt))
    }
    val etapeSuivante = PrestationRules.etapeSuivante(prestation.etape)
    val estHoraire = PrestationRules.mode(prestation.payload.mode) == ModeFacturation.HORAIRE

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prestation.record.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        prestation.annulee -> Color(0xFFFEE2E2)
                        prestation.etape == EtapePrestation.TERMINEE -> Color(0xFFDCFCE7)
                        prestation.etape == EtapePrestation.EN_COURS -> RoseServices.copy(alpha = 0.2f)
                        else -> Color(0xFFF3F4F6)
                    },
                ) {
                    Text(
                        stringResource(if (prestation.annulee) R.string.ach_annulee else prestation.etape.libelleRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            prestation.annulee -> Color(0xFFB91C1C)
                            prestation.etape == EtapePrestation.TERMINEE -> Color(0xFF15803D)
                            prestation.etape == EtapePrestation.EN_COURS -> RoseServices
                            else -> MissaMuted
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(prestation.payload.intitule, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            Text(prestation.payload.clientName, fontSize = 11.5.sp, color = MissaMuted)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(PrestationRules.mode(prestation.payload.mode).libelleRes) +
                        if (estHoraire) " (${prestation.payload.heures} h)" else "",
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
                Text(
                    fmtValeur(prestation.montant, devise),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            Text(dateStr, fontSize = 10.sp, color = MissaMuted)

            if (!prestation.annulee) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (etapeSuivante != null) {
                        Button(
                            onClick = onAvancer,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseServices, contentColor = Color.White),
                        ) {
                            Text(
                                stringResource(R.string.srv_passer_etape, stringResource(etapeSuivante.libelleRes)),
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                        }
                    }
                    if (estHoraire && prestation.etape != EtapePrestation.TERMINEE) {
                        OutlinedButton(onClick = onAjusterHeures) {
                            Text(stringResource(R.string.srv_action_heures), fontSize = 11.sp, color = MissaInk)
                        }
                    }
                    IconButton(onClick = onAnnuler, modifier = Modifier.size(30.dp)) {
                        Icon(painterResource(Iv.DeleteOutline), null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueNouvellePrestation(
    clients: List<ClientEntity>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (ClientEntity?, String, ModeFacturation, String, String, String, String) -> Unit,
) {
    var clientChoisi by remember { mutableStateOf<ClientEntity?>(clients.firstOrNull()) }
    var intitule by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ModeFacturation.FORFAIT) }
    var tarif by remember { mutableStateOf("") }
    var heures by remember { mutableStateOf("1") }
    var intervenant by remember { mutableStateOf("") }
    var lieu by remember { mutableStateOf("") }

    MissaFormDialogue(
        titre = stringResource(R.string.srv_nouvelle_prestation),
        icone = Iv.RequestQuote,
        couleur = AppModule.SERVICES.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = intitule.isNotBlank() && (tarif.toDoubleOrNull() ?: 0.0) > 0.0,
        enCours = enCours,
        onValider = { onValider(clientChoisi, intitule.trim(), mode, tarif, heures, intervenant.trim(), lieu.trim()) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 1) {
            MissaRangee {
                MissaChampListe(
                    libelle = stringResource(R.string.form_client),
                    options = clients.map { it to it.nom },
                    selection = clientChoisi,
                    onSelection = { clientChoisi = it },
                    icone = Iv.Person,
                    placeholder = stringResource(R.string.sales_select_client),
                    modifier = Modifier.weight(1f),
                )
                MissaChampTexte(intitule, { intitule = it }, stringResource(R.string.srv_champ_intitule), icone = Iv.RequestQuote, requis = true, modifier = Modifier.weight(1f))
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_prix), numero = 2) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(ModeFacturation.FORFAIT, stringResource(ModeFacturation.FORFAIT.libelleRes), Iv.Payments),
                    MissaTuile(ModeFacturation.HORAIRE, stringResource(ModeFacturation.HORAIRE.libelleRes), Iv.Schedule),
                ),
                selection = mode,
                onSelection = { mode = it },
                colonnes = 2,
            )
            MissaRangee {
                MissaChampTexte(
                    tarif, { tarif = it },
                    stringResource(if (mode == ModeFacturation.FORFAIT) R.string.srv_champ_forfait else R.string.srv_champ_tarif_horaire),
                    modifier = Modifier.weight(1f), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, requis = true,
                )
                if (mode == ModeFacturation.HORAIRE) {
                    MissaChampTexte(heures, { heures = it }, stringResource(R.string.srv_champ_heures_prevues), modifier = Modifier.weight(1f), icone = Iv.Schedule, clavier = MissaClavier.DECIMAL)
                }
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_planification), numero = 3) {
            MissaRangee {
                MissaChampTexte(intervenant, { intervenant = it }, stringResource(R.string.form_intervenant), icone = Iv.Person, modifier = Modifier.weight(1f))
                MissaChampTexte(lieu, { lieu = it }, stringResource(R.string.form_lieu), icone = Iv.Place, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DialogueAjusterHeures(
    prestation: Prestation,
    onFermer: () -> Unit,
    onValider: (String) -> Unit,
) {
    var heures by remember { mutableStateOf(prestation.payload.heures.toString()) }

    MissaFormDialogue(
        titre = stringResource(R.string.srv_titre_ajuster_heures),
        icone = Iv.Schedule,
        couleur = AppModule.SERVICES.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = heures.toDoubleOrNull() != null,
        onValider = { onValider(heures) },
    ) {
        MissaChampTexte(heures, { heures = it }, stringResource(R.string.srv_champ_heures_passees), icone = Iv.Schedule, clavier = MissaClavier.DECIMAL, requis = true)
    }
}
