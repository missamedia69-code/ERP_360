package com.missa.b360.ui.projets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.missa.b360.core.domain.model.EtatProjet
import com.missa.b360.core.domain.model.Projet
import com.missa.b360.core.domain.model.ProjetRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.util.Locale

/** Indigo caractéristique du module Projets — source unique : [AppModule.PROJETS]. */
private val IndigoProjets: Color get() = AppModule.PROJETS.couleur

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun ProjetsScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    openCreate: Boolean = false,
    vm: ProjetsViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()

    var dialogueNouveauProjet by remember { mutableStateOf(openCreate) }
    var projetAActualiser by remember { mutableStateOf<Projet?>(null) }
    var projetAAnnuler by remember { mutableStateOf<Projet?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouveauProjet = false
            projetAActualiser = null
            projetAAnnuler = null
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_projets),
            onBack = onBack,
            couleurFond = AppModule.PROJETS.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Carte Synthèse Portefeuille Projets ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = IndigoProjets.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.prj_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.budgetTotal, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.prj_avancement_moyen, String.format(Locale.ROOT, "%.0f%%", etat.avancementMoyen)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoProjets,
                            )
                            Text(
                                stringResource(R.string.prj_consomme, fmtValeur(etat.consommeTotal, devise)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.prj_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.Workspaces,
                        titre = stringResource(R.string.prj_tuile_tous),
                        sousTitre = stringResource(R.string.prj_nb_count, etat.projets.size),
                        estActif = filtre == null,
                        onClick = { vm.filtrer(null) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Schedule,
                        titre = stringResource(R.string.prj_etat_en_cours),
                        sousTitre = stringResource(R.string.prj_nb_count, etat.projets.count { it.etat == EtatProjet.EN_COURS && !it.annule }),
                        estActif = filtre == EtatProjet.EN_COURS,
                        onClick = { vm.filtrer(EtatProjet.EN_COURS) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Warning,
                        titre = stringResource(R.string.prj_tuile_derives),
                        sousTitre = stringResource(R.string.prj_nb_count, etat.enDerive + etat.enRetard),
                        estActif = false,
                        onClick = { vm.filtrer(EtatProjet.EN_COURS) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Add,
                        titre = stringResource(R.string.prj_nouveau_projet),
                        sousTitre = stringResource(R.string.st_creer),
                        estActif = false,
                        onClick = { dialogueNouveauProjet = true },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileProjets(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
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
                        is ProjetsViewModel.Message.Cree -> stringResource(R.string.prj_msg_cree, msg.reference) to Color(0xFF15803D)
                        ProjetsViewModel.Message.Actualise -> stringResource(R.string.prj_msg_actualise) to Color(0xFF15803D)
                        ProjetsViewModel.Message.EtatChange -> stringResource(R.string.prj_msg_etat_change) to Color(0xFF15803D)
                        ProjetsViewModel.Message.Annule -> stringResource(R.string.prj_msg_annule) to Color(0xFFB91C1C)
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

            // --- Registre des Projets ---
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.prj_titre_registre),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                        modifier = Modifier.weight(1f),
                    )
                    if (filtre != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = IndigoProjets.copy(alpha = 0.2f)) {
                            Text(
                                stringResource(filtre!!.libelleRes),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoProjets,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (etat.projets.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Workspaces,
                        title = stringResource(R.string.prj_aucun),
                        description = stringResource(R.string.prj_aucun_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.projets, key = { it.record.id }) { projet ->
                    CarteProjet(
                        projet = projet,
                        devise = devise,
                        onChangerEtat = { e -> vm.changerEtat(projet.record.id, e) },
                        onActualiser = { projetAActualiser = projet },
                        onAnnuler = { projetAAnnuler = projet },
                    )
                }
            }
        }
    }

    if (dialogueNouveauProjet) {
        DialogueNouveauProjet(
            clients = etat.clients,
            enCours = enCours,
            onFermer = { dialogueNouveauProjet = false },
            onValider = { cl, nom, resp, budget ->
                vm.creer(cl, nom, resp, budget)
            },
        )
    }

    projetAActualiser?.let { p ->
        DialogueActualiserProjet(
            projet = p,
            onFermer = { projetAActualiser = null },
            onValider = { av, co ->
                vm.actualiser(p.record.id, av, co)
            },
        )
    }

    projetAAnnuler?.let { p ->
        AlertDialog(
            onDismissRequest = { projetAAnnuler = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.ach_confirmer_annulation, p.record.reference), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = { vm.annuler(p.record.id) }) {
                    Text(stringResource(R.string.ach_annuler), color = Color(0xFFB91C1C))
                }
            },
            dismissButton = {
                TextButton(onClick = { projetAAnnuler = null }) {
                    Text(stringResource(R.string.st_annuler), color = MissaInk)
                }
            },
        )
    }
}

@Composable
private fun TuileProjets(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) IndigoProjets.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) IndigoProjets else MissaBorder),
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
private fun CarteProjet(
    projet: Projet,
    devise: String,
    onChangerEtat: (EtatProjet) -> Unit,
    onActualiser: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val tauxConsomme = ProjetRules.consommationBudget(projet.payload)
    val estEnDerive = ProjetRules.derive(projet.payload) > 0.0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(projet.record.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        projet.annule -> Color(0xFFFEE2E2)
                        projet.etat == EtatProjet.LIVRE -> Color(0xFFDCFCE7)
                        projet.etat == EtatProjet.EN_COURS -> IndigoProjets.copy(alpha = 0.2f)
                        else -> Color(0xFFF3F4F6)
                    },
                ) {
                    Text(
                        stringResource(if (projet.annule) R.string.ach_annulee else projet.etat.libelleRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            projet.annule -> Color(0xFFB91C1C)
                            projet.etat == EtatProjet.LIVRE -> Color(0xFF15803D)
                            projet.etat == EtatProjet.EN_COURS -> IndigoProjets
                            else -> MissaMuted
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(projet.payload.nom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            projet.payload.clientName?.let {
                Text(it, fontSize = 11.5.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(6.dp))

            // Progression
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.prj_avancement), fontSize = 11.sp, color = MissaMuted)
                Text("${projet.payload.avancement}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            }
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { (projet.payload.avancement / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = IndigoProjets,
                trackColor = Color(0xFFE5E7EB),
            )

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.prj_budget_consomme, fmtValeur(projet.payload.consomme, devise)),
                    fontSize = 11.sp,
                    color = if (estEnDerive) Color(0xFFDC2626) else MissaMuted,
                    fontWeight = if (estEnDerive) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    stringResource(R.string.prj_budget_total, fmtValeur(projet.payload.budget, devise)),
                    fontSize = 11.sp,
                    color = MissaInk,
                )
            }

            if (!projet.annule && projet.etat != EtatProjet.LIVRE) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (projet.etat == EtatProjet.EN_PREPARATION) {
                        Button(
                            onClick = { onChangerEtat(EtatProjet.EN_COURS) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoProjets, contentColor = Color.White),
                        ) {
                            Text(stringResource(R.string.prj_demarrer), fontSize = 11.sp, color = Color.White)
                        }
                    } else if (projet.etat == EtatProjet.EN_COURS) {
                        Button(
                            onClick = { onChangerEtat(EtatProjet.LIVRE) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D), contentColor = Color.White),
                        ) {
                            Text(stringResource(R.string.prj_livrer), fontSize = 11.sp, color = Color.White)
                        }
                    }
                    OutlinedButton(onClick = onActualiser) {
                        Text(stringResource(R.string.prj_avancement), fontSize = 11.sp, color = MissaInk)
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
private fun DialogueNouveauProjet(
    clients: List<ClientEntity>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (ClientEntity?, String, String, String) -> Unit,
) {
    var clientChoisi by remember { mutableStateOf<ClientEntity?>(clients.firstOrNull()) }
    var nom by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var ouvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.prj_nouveau_projet), fontSize = 16.sp) },
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
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.prj_champ_nom), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = responsable,
                    onValueChange = { responsable = it },
                    label = { Text(stringResource(R.string.prj_champ_responsable), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.prj_champ_budget), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientChoisi, nom, responsable, budget) },
                enabled = nom.isNotBlank() && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun DialogueActualiserProjet(
    projet: Projet,
    onFermer: () -> Unit,
    onValider: (String, String) -> Unit,
) {
    var avancement by remember { mutableStateOf(projet.payload.avancement.toString()) }
    var consomme by remember { mutableStateOf(projet.payload.consomme.toString()) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.prj_titre_actualiser), fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = avancement,
                    onValueChange = { avancement = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.prj_champ_avancement_pourcent), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = consomme,
                    onValueChange = { consomme = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.prj_champ_consomme_reel), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(avancement, consomme) },
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
