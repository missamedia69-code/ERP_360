package com.missa.b360.ui.projets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Warning
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
import com.missa.b360.core.domain.model.EtatProjet
import com.missa.b360.core.domain.model.Projet
import com.missa.b360.core.domain.model.ProjetRules
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
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
 * Module Projets — budget, avancement et dérive.
 *
 * L'écran met en avant l'écart entre le budget consommé et l'avancement
 * déclaré : c'est cet écart, et non chacune des deux valeurs prise à part, qui
 * annonce un projet en difficulté.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjetsScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    viewModel: ProjetsViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val message by viewModel.message.collectAsState()
    val occupe by viewModel.enCours.collectAsState()
    val filtre by viewModel.filtre.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var dialogue by remember { mutableStateOf(openCreate) }
    var actualiserPour by remember { mutableStateOf<Projet?>(null) }

    val texte = when (val actuel = message) {
        is ProjetsViewModel.Message.Cree -> stringResource(R.string.prj_msg_cree, actuel.reference)
        ProjetsViewModel.Message.Actualise -> stringResource(R.string.prj_msg_actualise)
        ProjetsViewModel.Message.EtatChange -> stringResource(R.string.prj_msg_etat)
        ProjetsViewModel.Message.Annule -> stringResource(R.string.prj_msg_annule)
        ProjetsViewModel.Message.LectureSeule -> stringResource(R.string.tre_msg_lecture_seule)
        ProjetsViewModel.Message.Invalide -> stringResource(R.string.tre_msg_invalide)
        ProjetsViewModel.Message.Erreur -> stringResource(R.string.tre_msg_erreur)
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
                title = { Text(stringResource(R.string.module_projets)) },
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
                Text(stringResource(R.string.prj_nouveau), fontSize = 13.sp)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { PrjSyntheseCarte(etat, devise) }

            if (etat.compteurs.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(etat.compteurs, key = { it.etat.name }) { compteur ->
                            FilterChip(
                                selected = filtre == compteur.etat,
                                onClick = { viewModel.filtrer(compteur.etat) },
                                label = {
                                    Text(
                                        stringResource(compteur.etat.libelleRes) +
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

            if (etat.projets.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.prj_aucun),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.projets, key = { it.record.id }) { projet ->
                    PrjLigne(
                        projet = projet,
                        devise = devise,
                        occupe = occupe,
                        onActualiser = { actualiserPour = projet },
                        onLivrer = { viewModel.changerEtat(projet.record.id, EtatProjet.LIVRE) },
                        onDemarrer = { viewModel.changerEtat(projet.record.id, EtatProjet.EN_COURS) },
                        onAnnuler = { viewModel.annuler(projet.record.id) },
                    )
                }
            }
        }
    }

    if (dialogue) {
        PrjDialogue(
            clients = etat.clients.map { it.id to it.nom },
            occupe = occupe,
            onFermer = { dialogue = false },
            onValider = { clientId, nom, responsable, budget ->
                viewModel.creer(
                    client = etat.clients.firstOrNull { it.id == clientId },
                    nom = nom,
                    responsable = responsable,
                    budgetTexte = budget,
                )
                dialogue = false
            },
        )
    }

    actualiserPour?.let { projet ->
        PrjActualiserDialogue(
            avancementInitial = projet.payload.avancement,
            consommeInitial = projet.payload.consomme,
            occupe = occupe,
            onFermer = { actualiserPour = null },
            onValider = { avancement, consomme ->
                viewModel.actualiser(projet.record.id, avancement, consomme)
                actualiserPour = null
            },
        )
    }
}

@Composable
private fun PrjSyntheseCarte(etat: ProjetsViewModel.EtatProjets, devise: String) {
    val alerte = etat.enDerive > 0 || etat.enRetard > 0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alerte) ProfileOrange.copy(alpha = 0.12f) else MissaSoftBlue,
        ),
        border = BorderStroke(
            1.dp,
            if (alerte) ProfileOrange else BrandBlue.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                PrjChiffre(
                    R.string.prj_budget,
                    MoneyUtils.format(etat.budgetTotal, devise),
                    Modifier.weight(1.3f),
                )
                PrjChiffre(
                    R.string.prj_consomme,
                    MoneyUtils.format(etat.consommeTotal, devise),
                    Modifier.weight(1.3f),
                )
                PrjChiffre(
                    R.string.prj_avancement_moyen,
                    Iso4217.formatPourcentage(etat.avancementMoyen),
                    Modifier.weight(0.9f),
                )
            }
            if (alerte) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = ProfileOrange,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = listOfNotNull(
                            etat.enDerive.takeIf { it > 0 }
                                ?.let { stringResource(R.string.prj_alerte_derive, it) },
                            etat.enRetard.takeIf { it > 0 }
                                ?.let { stringResource(R.string.prj_alerte_retard, it) },
                        ).joinToString(" · "),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProfileOrange,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrjChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
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

/** Un projet : avancement, budget consommé, et l'écart entre les deux. */
@Composable
private fun PrjLigne(
    projet: Projet,
    devise: String,
    occupe: Boolean,
    onActualiser: () -> Unit,
    onLivrer: () -> Unit,
    onDemarrer: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val derive = ProjetRules.derive(projet.payload)
    val enDerive = projet.payload.budget > 0 && derive > ProjetRules.SEUIL_DERIVE
    val couleur = when {
        projet.annule -> Red40
        projet.etat == EtatProjet.LIVRE -> ProfileGreen
        enDerive || projet.depasse -> Red40
        projet.etat == EtatProjet.SUSPENDU -> ProfileOrange
        else -> BrandBlue
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projet.payload.nom,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(
                            if (projet.annule) R.string.ops_status_cancelled else projet.etat.libelleRes,
                        ),
                        projet.payload.clientName,
                        projet.payload.responsable,
                        stringResource(R.string.prj_avancement, projet.payload.avancement),
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = couleur,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!projet.annule) {
                IconButton(
                    onClick = onActualiser,
                    enabled = !occupe,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.prj_actualiser),
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        // Deux barres superposées : l'avancement déclaré et le budget consommé.
        // Voir la seconde dépasser la première, c'est voir la dérive.
        if (projet.payload.budget > 0) {
            Spacer(Modifier.height(4.dp))
            PrjBarre(projet.payload.avancement / 100f, BrandBlue)
            Spacer(Modifier.height(2.dp))
            PrjBarre(
                (ProjetRules.consommationBudget(projet.payload) / 100.0).toFloat(),
                if (enDerive || projet.depasse) Red40 else ProfileGreen,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(
                    R.string.prj_budget_ligne,
                    MoneyUtils.format(projet.payload.consomme, devise),
                    MoneyUtils.format(projet.payload.budget, devise),
                ),
                fontSize = 10.sp,
                color = MissaMuted,
            )
        }
        if (!projet.annule && projet.etat != EtatProjet.LIVRE) {
            Row {
                if (projet.etat == EtatProjet.EN_PREPARATION) {
                    TextButton(onClick = onDemarrer, enabled = !occupe) {
                        Text(stringResource(R.string.prj_demarrer), fontSize = 11.sp)
                    }
                }
                TextButton(onClick = onLivrer, enabled = !occupe) {
                    Text(stringResource(R.string.prj_livrer), fontSize = 11.sp)
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
private fun PrjBarre(part: Float, couleur: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(MissaBorder, RoundedCornerShape(3.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(part.coerceIn(0f, 1f))
                .height(5.dp)
                .background(couleur, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun PrjDialogue(
    clients: List<Pair<Long, String>>,
    occupe: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, String, String, String) -> Unit,
) {
    var clientId by remember { mutableStateOf(0L) }
    var nom by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.prj_nouveau), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PrjChamp(nom, { nom = it }, R.string.prj_champ_nom)
                if (clients.isNotEmpty()) {
                    MissaSelecteurBleu(
                        label = stringResource(R.string.prj_champ_client),
                        options = clients.map { MissaOption(cle = it.first.toString(), titre = it.second) },
                        selectionCle = clientId.takeIf { it != 0L }?.toString(),
                        onSelection = { clientId = it.toLongOrNull() ?: 0L },
                    )
                }
                PrjChamp(responsable, { responsable = it }, R.string.prj_champ_responsable)
                PrjChamp(budget, { budget = it }, R.string.prj_champ_budget, numerique = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientId, nom, responsable, budget) },
                enabled = ProjetRules.nomValide(nom) && !occupe,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

/** Saisie conjointe de l'avancement et du consommé — l'un sans l'autre n'informe pas. */
@Composable
private fun PrjActualiserDialogue(
    avancementInitial: Int,
    consommeInitial: Double,
    occupe: Boolean,
    onFermer: () -> Unit,
    onValider: (String, String) -> Unit,
) {
    var avancement by remember { mutableStateOf(avancementInitial.toString()) }
    var consomme by remember {
        mutableStateOf(if (consommeInitial > 0) MoneyUtils.formatBrut(consommeInitial) else "")
    }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.prj_actualiser), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PrjChamp(
                    avancement,
                    { avancement = it },
                    R.string.prj_champ_avancement,
                    numerique = true,
                )
                PrjChamp(consomme, { consomme = it }, R.string.prj_champ_consomme, numerique = true)
                Text(
                    text = stringResource(R.string.prj_actualiser_aide),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(avancement, consomme) }, enabled = !occupe) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun PrjChamp(
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
