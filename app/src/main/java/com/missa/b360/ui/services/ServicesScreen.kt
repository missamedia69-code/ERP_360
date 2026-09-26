package com.missa.b360.ui.services

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.missa.b360.ui.components.MissaMenuDeroulant
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

    var dialogueNouvellePrestation by remember { mutableStateOf(openCreate) }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Carte Synthèse Prestations ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RoseServices.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.srv_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.chiffreRealise, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
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
                        titre = stringResource(R.string.srv_nouvelle_prestation),
                        sousTitre = stringResource(R.string.st_creer),
                        onClick = { dialogueNouvellePrestation = true },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
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

            if (etat.prestations.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.RequestQuote,
                        title = stringResource(R.string.srv_aucune),
                        description = stringResource(R.string.srv_aucune_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.prestations, key = { it.record.id }) { prestation ->
                    CartePrestation(
                        prestation = prestation,
                        devise = devise,
                        onAvancer = { vm.avancer(prestation.record.id) },
                        onAjusterHeures = { prestationAjusterHeures = prestation },
                        onAnnuler = { prestationAAnnuler = prestation },
                    )
                }
            }
        }
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
            .height(82.dp)
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
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
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
                Spacer(Modifier.height(8.dp))
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
                    IconButton(onClick = onAnnuler, modifier = Modifier.size(36.dp)) {
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
    var ouvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.srv_nouvelle_prestation), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = clientChoisi?.nom ?: stringResource(R.string.sales_select_client),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.clients_flow_contact), fontSize = 11.sp) },
                        trailingIcon = { Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaInk) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.matchParentSize().clickable { ouvert = true })
                    MissaMenuDeroulant(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                        clients.forEach { cl ->
                            DropdownMenuItem(
                                text = { Text(cl.nom, color = MissaInk) },
                                onClick = {
                                    clientChoisi = cl
                                    ouvert = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = intitule,
                    onValueChange = { intitule = it },
                    label = { Text(stringResource(R.string.srv_champ_intitule), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModeFacturation.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(stringResource(m.libelleRes), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoseServices.copy(alpha = 0.2f)),
                        )
                    }
                }
                OutlinedTextField(
                    value = tarif,
                    onValueChange = { tarif = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(if (mode == ModeFacturation.FORFAIT) R.string.srv_champ_forfait else R.string.srv_champ_tarif_horaire), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (mode == ModeFacturation.HORAIRE) {
                    OutlinedTextField(
                        value = heures,
                        onValueChange = { heures = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.srv_champ_heures_prevues), fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientChoisi, intitule, mode, tarif, heures, intervenant, lieu) },
                enabled = intitule.isNotBlank() && (tarif.toDoubleOrNull() ?: 0.0) > 0.0 && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun DialogueAjusterHeures(
    prestation: Prestation,
    onFermer: () -> Unit,
    onValider: (String) -> Unit,
) {
    var heures by remember { mutableStateOf(prestation.payload.heures.toString()) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.srv_titre_ajuster_heures), fontSize = 15.sp) },
        text = {
            OutlinedTextField(
                value = heures,
                onValueChange = { heures = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.srv_champ_heures_passees), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(heures) },
                enabled = (heures.toDoubleOrNull() ?: 0.0) >= 0.0,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
