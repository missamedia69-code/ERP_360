package com.missa.b360.ui.qualite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ReportProblem
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
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.data.entity.StatutNc
import com.missa.b360.core.domain.model.QualiteMaintenanceRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
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
 * Module Qualité (QUA) — registre des non-conformités.
 *
 * Une non-conformité avance d'un cran à chaque appui : ouverte, en cours,
 * résolue. La date de clôture est posée automatiquement, ce qui rend le délai
 * de traitement mesurable sans rien demander de plus à l'utilisateur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualiteScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    viewModel: QualiteViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val message by viewModel.message.collectAsState()
    val enCours by viewModel.enCours.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var dialogue by remember { mutableStateOf(false) }

    val textes = mapOf(
        QualiteViewModel.Message.Enregistre to R.string.qua_msg_ok,
        QualiteViewModel.Message.LectureSeule to R.string.tre_msg_lecture_seule,
        QualiteViewModel.Message.Invalide to R.string.tre_msg_invalide,
        QualiteViewModel.Message.Erreur to R.string.tre_msg_erreur,
    )
    val texte = message?.let { textes[it] }?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (texte != null) {
            snackbar.showSnackbar(texte)
            viewModel.effacerMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_qualite)) },
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
                Text(stringResource(R.string.qua_declarer), fontSize = 13.sp)
            }
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.QUA),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { QuaBilanCarte(viewModel, devise) }

            item {
                Text(
                    text = stringResource(R.string.qua_registre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (etat.toutes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.qua_aucune_nc),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                // Les écarts à traiter d'abord, le reste ensuite : la file de
                // travail passe avant l'archive.
                val resolues = etat.toutes.filter { it.statut == StatutNc.RESOLUE.name }
                items(etat.aTraiter, key = { "nc-" + it.id }) { nc ->
                    QuaLigne(nc, devise, enCours) { viewModel.avancer(nc.id) }
                }
                items(resolues, key = { "ok-" + it.id }) { nc ->
                    QuaLigne(nc, devise, enCours, null)
                }
            }

            // Sommaire des fonctionnalités du module, disponibles et prévues.
            sectionFonctionsModule(ModuleCode.QUA) { route -> onNaviguer(route) }
        }
        }
    }

    if (dialogue) {
        QuaDialogue(
            enCours = enCours,
            onFermer = { dialogue = false },
            onValider = { titre, gravite, origine, description, responsable, cout ->
                viewModel.declarer(titre, gravite, origine, description, responsable, cout)
                dialogue = false
            },
        )
    }
}

@Composable
private fun QuaBilanCarte(viewModel: QualiteViewModel, devise: String) {
    val etat by viewModel.etat.collectAsState()
    val bilan = etat.bilan
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bilan.critiques > 0) Red40.copy(alpha = 0.10f) else MissaSoftBlue,
        ),
        border = BorderStroke(
            1.dp,
            if (bilan.critiques > 0) Red40 else BrandBlue.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                QuaChiffre(
                    R.string.qua_ouvertes,
                    (bilan.ouvertes + bilan.enCours).toString(),
                    Modifier.weight(1f),
                )
                QuaChiffre(
                    R.string.qua_taux_resolution,
                    Iso4217.formatPourcentage(bilan.tauxResolution),
                    Modifier.weight(1f),
                )
                QuaChiffre(
                    R.string.qua_cout,
                    MoneyUtils.format(bilan.coutTotal, devise),
                    Modifier.weight(1.3f),
                )
            }
            bilan.delaiMoyenJours?.let { delai ->
                Spacer(Modifier.size(7.dp))
                Text(
                    text = stringResource(R.string.qua_delai_moyen, String.format("%.1f", delai)),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
            }
            if (bilan.critiques > 0) {
                Spacer(Modifier.size(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ReportProblem,
                        contentDescription = null,
                        tint = Red40,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.qua_critiques_ouvertes, bilan.critiques),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Red40,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuaChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
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

/** Une non-conformité et son action d'avancement. */
@Composable
private fun QuaLigne(
    nc: NonConformiteEntity,
    devise: String,
    enCours: Boolean,
    onAvancer: (() -> Unit)?,
) {
    val gravite = QualiteMaintenanceRules.gravite(nc.gravite)
    val statut = QualiteMaintenanceRules.statutNc(nc.statut)
    val couleur = when (gravite) {
        GraviteNc.CRITIQUE -> Red40
        GraviteNc.MAJEURE -> ProfileOrange
        GraviteNc.MINEURE -> MissaMuted
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (statut == StatutNc.RESOLUE) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.ReportProblem
                },
                contentDescription = null,
                tint = if (statut == StatutNc.RESOLUE) ProfileGreen else couleur,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nc.titre,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        DateUtils.formatDate(nc.date),
                        stringResource(QualiteMaintenanceRules.libelleGravite(gravite)),
                        stringResource(
                            QualiteMaintenanceRules.libelleOrigine(
                                QualiteMaintenanceRules.origine(nc.origine),
                            ),
                        ),
                        stringResource(QualiteMaintenanceRules.libelleStatutNc(statut)),
                        nc.cout.takeIf { it > 0 }?.let { MoneyUtils.format(it, devise) },
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onAvancer != null) {
                IconButton(onClick = onAvancer, enabled = !enCours, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.qua_avancer),
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Déclaration d'une non-conformité. */
@Composable
private fun QuaDialogue(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, GraviteNc, OrigineNc, String, String, String) -> Unit,
) {
    var titre by remember { mutableStateOf("") }
    var gravite by remember { mutableStateOf(GraviteNc.MINEURE) }
    var origine by remember { mutableStateOf(OrigineNc.INTERNE) }
    var description by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var cout by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.qua_declarer), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                QuaChamp(titre, { titre = it }, R.string.qua_champ_titre)
                Text(stringResource(R.string.qua_champ_gravite), fontSize = 11.sp, color = MissaMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraviteNc.entries.forEach { candidat ->
                        FilterChip(
                            selected = gravite == candidat,
                            onClick = { gravite = candidat },
                            label = {
                                Text(
                                    stringResource(
                                        QualiteMaintenanceRules.libelleGravite(candidat),
                                    ),
                                    fontSize = 11.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                Text(stringResource(R.string.qua_champ_origine), fontSize = 11.sp, color = MissaMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrigineNc.entries.forEach { candidat ->
                        FilterChip(
                            selected = origine == candidat,
                            onClick = { origine = candidat },
                            label = {
                                Text(
                                    stringResource(
                                        QualiteMaintenanceRules.libelleOrigine(candidat),
                                    ),
                                    fontSize = 10.5.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                QuaChamp(description, { description = it }, R.string.qua_champ_description)
                QuaChamp(responsable, { responsable = it }, R.string.qua_champ_responsable)
                QuaChamp(cout, { cout = it }, R.string.qua_champ_cout, numerique = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(titre, gravite, origine, description, responsable, cout) },
                enabled = TresorerieRules.libelleValide(titre) && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun QuaChamp(
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
