package com.missa.b360.ui.livraison

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.BonLivraison
import com.missa.b360.core.domain.model.EtapeLivraison
import com.missa.b360.core.domain.model.LivraisonRules
import com.missa.b360.core.domain.model.MentionsLegales
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
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
 * Module Livraison — bons de livraison.
 *
 * Le bon suit trois étapes : à préparer, expédiée, livrée. Un appui les fait
 * avancer, jamais reculer : corriger une remise passe par une annulation, qui
 * conserve la trace de ce qui a quitté le dépôt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivraisonScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    openCreate: Boolean = false,
    viewModel: LivraisonViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val message by viewModel.message.collectAsState()
    val occupe by viewModel.enCours.collectAsState()
    val filtre by viewModel.filtre.collectAsState()
    val entreprise by viewModel.entreprise.collectAsState()
    val contexte = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var dialogue by remember { mutableStateOf(openCreate) }

    val mentions = MentionsLegales.depuis(
        entreprise = entreprise,
        libelleFiscalGenerique = stringResource(R.string.fisc_id_fiscal),
        libelleRegistreGenerique = stringResource(R.string.fisc_id_registre),
        nomParDefaut = stringResource(R.string.app_name),
    )

    val texte = when (val actuel = message) {
        is LivraisonViewModel.Message.Cree -> stringResource(R.string.liv_msg_cree, actuel.reference)
        LivraisonViewModel.Message.Avance -> stringResource(R.string.liv_msg_avance)
        LivraisonViewModel.Message.Annule -> stringResource(R.string.liv_msg_annule)
        LivraisonViewModel.Message.LectureSeule -> stringResource(R.string.tre_msg_lecture_seule)
        LivraisonViewModel.Message.Invalide -> stringResource(R.string.tre_msg_invalide)
        LivraisonViewModel.Message.EtapeFinale -> stringResource(R.string.liv_msg_etape_finale)
        LivraisonViewModel.Message.Erreur -> stringResource(R.string.tre_msg_erreur)
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
                title = { Text(stringResource(R.string.module_livraison)) },
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
                Text(stringResource(R.string.liv_nouveau), fontSize = 13.sp)
            }
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.LOG),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { LivSyntheseCarte(etat) }

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

            if (etat.bons.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.liv_aucun_bon),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.bons, key = { it.record.id }) { bon ->
                    LivBonLigne(
                        bon = bon,
                        occupe = occupe,
                        onAvancer = { viewModel.avancer(bon.record.id) },
                        onAnnuler = { viewModel.annuler(bon.record.id) },
                        onPartager = { contexte.partagerBon(bon, mentions) },
                    )
                }
            }

            // Sommaire des fonctionnalités du module, disponibles et prévues.
            sectionFonctionsModule(ModuleCode.LOG) { route -> onNaviguer(route) }
        }
        }
    }

    if (dialogue) {
        LivDialogue(
            clients = etat.clients.map { it.id to it.nom },
            occupe = occupe,
            onFermer = { dialogue = false },
            onValider = { clientId, nomLibre, adresse, transporteur, colis, origine ->
                viewModel.creer(
                    client = etat.clients.firstOrNull { it.id == clientId },
                    nomLibre = nomLibre,
                    adresse = adresse,
                    transporteur = transporteur,
                    colisTexte = colis,
                    referenceOrigine = origine,
                )
                dialogue = false
            },
        )
    }
}

@Composable
private fun LivSyntheseCarte(etat: LivraisonViewModel.EtatLivraison) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            LivChiffre(R.string.liv_en_cours, etat.enCours.toString(), Modifier.weight(1f))
            LivChiffre(
                R.string.liv_taux,
                Iso4217.formatPourcentage(etat.tauxLivraison),
                Modifier.weight(1f),
            )
            LivChiffre(
                R.string.liv_delai_moyen,
                etat.delaiMoyenJours?.let { String.format("%.1f j", it) } ?: "—",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LivChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valeur,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(libelleRes), fontSize = 10.sp, color = MissaMuted)
    }
}

/** Un bon : destinataire, étape, transporteur, et les actions disponibles. */
@Composable
private fun LivBonLigne(
    bon: BonLivraison,
    occupe: Boolean,
    onAvancer: () -> Unit,
    onAnnuler: () -> Unit,
    onPartager: () -> Unit,
) {
    val couleur = when {
        bon.annule -> Red40
        bon.etape == EtapeLivraison.LIVREE -> ProfileGreen
        bon.etape == EtapeLivraison.EXPEDIEE -> ProfileOrange
        else -> BrandBlue
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = couleur.copy(alpha = 0.13f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (bon.etape == EtapeLivraison.LIVREE) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.LocalShipping
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
                    text = bon.payload.clientName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        bon.record.reference,
                        DateUtils.formatDate(bon.record.createdAt),
                        stringResource(
                            if (bon.annule) R.string.ops_status_cancelled else bon.etape.libelleRes,
                        ),
                        bon.payload.transporteur,
                        bon.payload.nombreColis.takeIf { it > 0 }
                            ?.let { stringResource(R.string.liv_colis, it) },
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = couleur,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                bon.payload.adresseLivraison?.let { adresse ->
                    Text(
                        text = adresse,
                        fontSize = 10.5.sp,
                        color = MissaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onPartager, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.dc_partager),
                    tint = MissaMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (!bon.annule && LivraisonRules.etapeSuivante(bon.etape) != null) {
                IconButton(onClick = onAvancer, enabled = !occupe, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(R.string.liv_avancer),
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (!bon.annule && bon.etape != EtapeLivraison.LIVREE) {
            TextButton(onClick = onAnnuler, enabled = !occupe) {
                Text(stringResource(R.string.dc_cancel_piece), fontSize = 11.sp, color = Red40)
            }
        }
        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Création d'un bon de livraison. */
@Composable
private fun LivDialogue(
    clients: List<Pair<Long, String>>,
    occupe: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, String, String, String, String, String) -> Unit,
) {
    var clientId by remember { mutableStateOf(clients.firstOrNull()?.first ?: 0L) }
    var nomLibre by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var transporteur by remember { mutableStateOf("") }
    var colis by remember { mutableStateOf("") }
    var origine by remember { mutableStateOf("") }

    val destinataire = clients.firstOrNull { it.first == clientId }?.second ?: nomLibre
    val valide = LivraisonRules.destinataireValide(destinataire)

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.liv_nouveau), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (clients.isNotEmpty()) {
                    MissaSelecteurBleu(
                        label = stringResource(R.string.liv_champ_client),
                        options = clients.map { MissaOption(cle = it.first.toString(), titre = it.second) },
                        selectionCle = clientId.takeIf { it != 0L }?.toString(),
                        onSelection = { clientId = it.toLongOrNull() ?: 0L },
                    )
                } else {
                    // Aucun client enregistré : la saisie libre évite de bloquer
                    // une livraison pour un destinataire ponctuel.
                    LivChamp(nomLibre, { nomLibre = it }, R.string.liv_champ_destinataire)
                }
                LivChamp(adresse, { adresse = it }, R.string.liv_champ_adresse)
                LivChamp(transporteur, { transporteur = it }, R.string.liv_champ_transporteur)
                LivChamp(colis, { colis = it }, R.string.liv_champ_colis, numerique = true)
                LivChamp(origine, { origine = it }, R.string.liv_champ_origine)
                Text(
                    text = stringResource(R.string.liv_origine_aide),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientId, nomLibre, adresse, transporteur, colis, origine) },
                enabled = valide && !occupe,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

@Composable
private fun LivChamp(
    valeur: String,
    onValeur: (String) -> Unit,
    labelRes: Int,
    numerique: Boolean = false,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie -> onValeur(if (numerique) saisie.filter(Char::isDigit) else saisie) },
        label = { Text(stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Partage du bon avec les mentions légales de l'émetteur : le bon de livraison
 * circule avec la marchandise et engage l'entreprise autant qu'une facture.
 */
private fun Context.partagerBon(bon: BonLivraison, mentions: MentionsLegales) {
    val lignes = buildList {
        add(mentions.texte())
        add("")
        add("${getString(R.string.module_livraison)} ${bon.record.reference}")
        add(bon.payload.clientName)
        bon.payload.adresseLivraison?.let { add(it) }
        bon.payload.transporteur?.let { add(it) }
        if (bon.payload.nombreColis > 0) {
            add(getString(R.string.liv_colis, bon.payload.nombreColis))
        }
        bon.payload.referenceOrigine?.let { add(it) }
    }
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(
                    Intent.EXTRA_SUBJECT,
                    "${getString(R.string.module_livraison)} ${bon.record.reference}",
                )
                .putExtra(Intent.EXTRA_TEXT, lignes.joinToString("\n")),
            getString(R.string.dc_partager),
        ),
    )
}
