package com.missa.b360.ui.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.EtapePrestation
import com.missa.b360.core.domain.model.ModeFacturation
import com.missa.b360.core.domain.model.Prestation
import com.missa.b360.core.domain.model.PrestationRules
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaFondFiligrane
import com.missa.b360.ui.components.sectionFonctionsModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40

/**
 * Module Services — prestations planifiées, en cours et terminées.
 *
 * Deux modes de facturation : au forfait, le prix convenu ne bouge pas ; à
 * l'heure, le montant suit le temps saisi, ajustable jusqu'à la clôture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    openCreate: Boolean = false,
    viewModel: ServicesViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val message by viewModel.message.collectAsState()
    val occupe by viewModel.enCours.collectAsState()
    val filtre by viewModel.filtre.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var dialogue by remember { mutableStateOf(openCreate) }
    var heuresPour by remember { mutableStateOf<Prestation?>(null) }

    val texte = when (val actuel = message) {
        is ServicesViewModel.Message.Creee -> stringResource(R.string.srv_msg_creee, actuel.reference)
        ServicesViewModel.Message.Avance -> stringResource(R.string.srv_msg_avance)
        ServicesViewModel.Message.Annulee -> stringResource(R.string.srv_msg_annulee)
        ServicesViewModel.Message.HeuresAjustees -> stringResource(R.string.srv_msg_heures)
        ServicesViewModel.Message.LectureSeule -> stringResource(R.string.tre_msg_lecture_seule)
        ServicesViewModel.Message.Invalide -> stringResource(R.string.tre_msg_invalide)
        ServicesViewModel.Message.EtapeFinale -> stringResource(R.string.srv_msg_etape_finale)
        ServicesViewModel.Message.Erreur -> stringResource(R.string.tre_msg_erreur)
        null -> null
    }
    LaunchedEffect(message) {
        if (texte != null) {
            snackbar.showSnackbar(texte)
            viewModel.effacerMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_services)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogue = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.srv_nouvelle), fontSize = 13.sp)
            }
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.SER),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { SrvSyntheseCarte(etat, devise) }

            if (etat.compteurs.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(etat.compteurs, key = { it.etape.name }) { compteur ->
                            FilterChip(
                                selected = filtre == compteur.etape,
                                onClick = { viewModel.filtrer(compteur.etape) },
                                label = {
                                    Text(
                                        stringResource(compteur.etape.libelleRes) +
                                            " · ${compteur.nombre}",
                                        fontSize = 11.5.sp,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                }
            }

            if (etat.prestations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.srv_aucune),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.prestations, key = { it.record.id }) { prestation ->
                    SrvLigne(
                        prestation = prestation,
                        devise = devise,
                        occupe = occupe,
                        onAvancer = { viewModel.avancer(prestation.record.id) },
                        onAnnuler = { viewModel.annuler(prestation.record.id) },
                        onHeures = { heuresPour = prestation },
                    )
                }
            }

            // Sommaire des fonctionnalités du module, disponibles et prévues.
            sectionFonctionsModule(ModuleCode.SER) { route -> onNaviguer(route) }
        }
        }
    }

    if (dialogue) {
        SrvDialogue(
            clients = etat.clients.map { it.id to it.nom },
            occupe = occupe,
            onFermer = { dialogue = false },
            onValider = { clientId, intitule, mode, tarif, heures, intervenant, lieu ->
                viewModel.creer(
                    client = etat.clients.firstOrNull { it.id == clientId },
                    intitule = intitule,
                    mode = mode,
                    tarifTexte = tarif,
                    heuresTexte = heures,
                    intervenant = intervenant,
                    lieu = lieu,
                )
                dialogue = false
            },
        )
    }

    heuresPour?.let { prestation ->
        SrvHeuresDialogue(
            initial = prestation.payload.heures,
            occupe = occupe,
            onFermer = { heuresPour = null },
            onValider = { heures ->
                viewModel.ajusterHeures(prestation.record.id, heures)
                heuresPour = null
            },
        )
    }
}

@Composable
private fun SrvSyntheseCarte(etat: ServicesViewModel.EtatServices, devise: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            SrvChiffre(
                R.string.srv_realise,
                MoneyUtils.format(etat.chiffreRealise, devise),
                Modifier.weight(1.3f),
            )
            SrvChiffre(
                R.string.srv_carnet,
                MoneyUtils.format(etat.carnet, devise),
                Modifier.weight(1.3f),
            )
            SrvChiffre(
                R.string.srv_heures,
                MoneyUtils.formatBrut(etat.heuresRealisees),
                Modifier.weight(0.8f),
            )
        }
    }
}

@Composable
private fun SrvChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valeur,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(libelleRes), fontSize = 10.sp, color = MissaMuted)
    }
}

/** Une prestation : intitulé, client, étape, montant. */
@Composable
private fun SrvLigne(
    prestation: Prestation,
    devise: String,
    occupe: Boolean,
    onAvancer: () -> Unit,
    onAnnuler: () -> Unit,
    onHeures: () -> Unit,
) {
    val couleur = when {
        prestation.annulee -> Red40
        prestation.etape == EtapePrestation.TERMINEE -> ProfileGreen
        prestation.etape == EtapePrestation.EN_COURS -> ProfileOrange
        else -> BrandBlue
    }
    val horaire = PrestationRules.mode(prestation.payload.mode) == ModeFacturation.HORAIRE
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = couleur.copy(alpha = 0.13f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (prestation.etape == EtapePrestation.TERMINEE) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.Schedule
                        },
                        contentDescription = null,
                        tint = couleur,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prestation.payload.intitule,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        prestation.payload.clientName.takeIf { it.isNotBlank() },
                        DateUtils.formatDate(prestation.record.createdAt),
                        stringResource(
                            if (prestation.annulee) {
                                R.string.ops_status_cancelled
                            } else {
                                prestation.etape.libelleRes
                            },
                        ),
                        prestation.payload.intervenant,
                        prestation.payload.heures.takeIf { it > 0 }
                            ?.let { stringResource(R.string.srv_heures_valeur, MoneyUtils.formatBrut(it)) },
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = couleur,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = MoneyUtils.format(prestation.montant, devise),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            if (!prestation.annulee && PrestationRules.etapeSuivante(prestation.etape) != null) {
                IconButton(onClick = onAvancer, enabled = !occupe, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(R.string.srv_avancer),
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (!prestation.annulee && prestation.etape != EtapePrestation.TERMINEE) {
            Row {
                // Le temps passé n'a de sens qu'en facturation horaire.
                if (horaire) {
                    TextButton(onClick = onHeures, enabled = !occupe) {
                        Text(stringResource(R.string.srv_saisir_heures), fontSize = 11.sp)
                    }
                }
                TextButton(onClick = onAnnuler, enabled = !occupe) {
                    Text(stringResource(R.string.dc_cancel_piece), fontSize = 11.sp, color = Red40)
                }
            }
        }
        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SrvDialogue(
    clients: List<Pair<Long, String>>,
    occupe: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, String, ModeFacturation, String, String, String, String) -> Unit,
) {
    var clientId by remember { mutableStateOf(clients.firstOrNull()?.first ?: 0L) }
    var intitule by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ModeFacturation.FORFAIT) }
    var tarif by remember { mutableStateOf("") }
    var heures by remember { mutableStateOf("") }
    var intervenant by remember { mutableStateOf("") }
    var lieu by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.srv_nouvelle), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SrvChamp(intitule, { intitule = it }, R.string.srv_champ_intitule)
                if (clients.isNotEmpty()) {
                    MissaSelecteurBleu(
                        label = stringResource(R.string.srv_champ_client),
                        options = clients.map { MissaOption(cle = it.first.toString(), titre = it.second) },
                        selectionCle = clientId.takeIf { it != 0L }?.toString(),
                        onSelection = { clientId = it.toLongOrNull() ?: 0L },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ModeFacturation.entries.forEach { candidat ->
                        FilterChip(
                            selected = mode == candidat,
                            onClick = { mode = candidat },
                            label = { Text(stringResource(candidat.libelleRes), fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                SrvChamp(
                    tarif,
                    { tarif = it },
                    if (mode == ModeFacturation.FORFAIT) {
                        R.string.srv_champ_forfait
                    } else {
                        R.string.srv_champ_taux_horaire
                    },
                    numerique = true,
                )
                if (mode == ModeFacturation.HORAIRE) {
                    SrvChamp(heures, { heures = it }, R.string.srv_champ_heures, numerique = true)
                }
                SrvChamp(intervenant, { intervenant = it }, R.string.srv_champ_intervenant)
                SrvChamp(lieu, { lieu = it }, R.string.srv_champ_lieu)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientId, intitule, mode, tarif, heures, intervenant, lieu) },
                enabled = PrestationRules.intituleValide(intitule) && tarif.isNotBlank() && !occupe,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

/** Saisie du temps réellement passé, en facturation horaire. */
@Composable
private fun SrvHeuresDialogue(
    initial: Double,
    occupe: Boolean,
    onFermer: () -> Unit,
    onValider: (String) -> Unit,
) {
    var heures by remember { mutableStateOf(if (initial > 0) MoneyUtils.formatBrut(initial) else "") }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.srv_saisir_heures), fontSize = 16.sp) },
        text = { SrvChamp(heures, { heures = it }, R.string.srv_champ_heures, numerique = true) },
        confirmButton = {
            TextButton(onClick = { onValider(heures) }, enabled = !occupe) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun SrvChamp(
    valeur: String,
    onValeur: (String) -> Unit,
    labelRes: Int,
    numerique: Boolean = false,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie ->
            onValeur(
                if (numerique) saisie.filter { it.isDigit() || it == ',' || it == '.' } else saisie,
            )
        },
        label = { Text(stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
