package com.missa.b360.ui.tresorerie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.SoldeCompte
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaFondFiligrane
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.Red40

/**
 * Module Trésorerie (TRE) — soldes, encaissements, décaissements et virements
 * internes.
 *
 * L'écran répond dans l'ordre aux trois questions du dirigeant : combien
 * ai-je, où est-ce, et qu'est-ce qui est entré ou sorti ce mois-ci.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TresorerieScreen(
    onBack: () -> Unit,
    viewModel: TresorerieViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val message by viewModel.message.collectAsState()
    val enCours by viewModel.enCours.collectAsState()
    val compteFiltre by viewModel.compteFiltre.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var dialogueCompte by remember { mutableStateOf(false) }
    var dialogueMouvement by remember { mutableStateOf<SensMouvement?>(null) }
    var dialogueVirement by remember { mutableStateOf(false) }

    val textes = mapOf(
        TresorerieViewModel.Message.CompteCree to R.string.tre_msg_compte_cree,
        TresorerieViewModel.Message.MouvementEnregistre to R.string.tre_msg_mouvement_ok,
        TresorerieViewModel.Message.VirementEnregistre to R.string.tre_msg_virement_ok,
        TresorerieViewModel.Message.LectureSeule to R.string.tre_msg_lecture_seule,
        TresorerieViewModel.Message.Invalide to R.string.tre_msg_invalide,
        TresorerieViewModel.Message.NomDejaPris to R.string.tre_msg_nom_pris,
        TresorerieViewModel.Message.ComptesIncoherents to R.string.tre_msg_comptes,
        TresorerieViewModel.Message.Erreur to R.string.tre_msg_erreur,
    )
    val texteMessage = message?.let { textes[it] }?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (texteMessage != null) {
            snackbar.showSnackbar(texteMessage)
            viewModel.effacerMessage()
        }
    }

    val comptesOuverts = viewModel.comptesOuverts(etat)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_tresorerie)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { dialogueCompte = true }) {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = stringResource(R.string.tre_nouveau_compte),
                        )
                    }
                    IconButton(
                        onClick = { dialogueVirement = true },
                        enabled = comptesOuverts.size >= 2,
                    ) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.tre_virement),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (comptesOuverts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { dialogueMouvement = SensMouvement.IN },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tre_nouveau_mouvement), fontSize = 13.sp)
                }
            }
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.TRE),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { TreSyntheseCarte(etat, devise) }

            if (etat.comptes.isEmpty()) {
                item { TreAucunCompte(onCreer = { dialogueCompte = true }) }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(etat.comptes, key = { it.compte.id }) { solde ->
                            TreCompteCarte(
                                solde = solde,
                                devise = devise,
                                selectionne = compteFiltre == solde.compte.id,
                                onClick = { viewModel.filtrerCompte(solde.compte.id) },
                                onLongClick = { viewModel.basculerActivite(solde.compte.id) },
                            )
                        }
                    }
                }
            }

            if (etat.repartition.isNotEmpty()) {
                item { TreRepartition(etat.repartition, devise) }
            }

            item {
                Text(
                    text = stringResource(
                        if (compteFiltre == null) {
                            R.string.tre_derniers_mouvements
                        } else {
                            R.string.tre_mouvements_du_compte
                        },
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (etat.mouvements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.tre_aucun_mouvement),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.mouvements, key = { it.id }) { mouvement ->
                    TreMouvementLigne(
                        mouvement = mouvement,
                        devise = devise,
                        nomCompte = etat.comptes.firstOrNull { it.compte.id == mouvement.compteId }
                            ?.compte?.nom.orEmpty(),
                        onPointer = { viewModel.basculerRapprochement(mouvement.id) },
                    )
                }
            }
        }
        }
    }

    if (dialogueCompte) {
        TreCompteDialogue(
            enCours = enCours,
            onFermer = { dialogueCompte = false },
            onValider = { nom, type, etablissement, numero, solde ->
                viewModel.creerCompte(nom, type, etablissement, numero, solde)
                dialogueCompte = false
            },
        )
    }

    dialogueMouvement?.let { sensInitial ->
        TreMouvementDialogue(
            comptes = comptesOuverts,
            sensInitial = sensInitial,
            enCours = enCours,
            onFermer = { dialogueMouvement = null },
            onValider = { compteId, sens, montant, libelle, categorie, tiers, mode, reference ->
                viewModel.enregistrerMouvement(
                    compteId = compteId,
                    sens = sens,
                    montantTexte = montant,
                    libelle = libelle,
                    categorie = categorie,
                    tiers = tiers,
                    modePaiement = mode,
                    reference = reference,
                    date = System.currentTimeMillis(),
                )
                dialogueMouvement = null
            },
        )
    }

    if (dialogueVirement) {
        TreVirementDialogue(
            comptes = comptesOuverts,
            devise = devise,
            soldes = etat.comptes,
            enCours = enCours,
            onFermer = { dialogueVirement = false },
            onValider = { source, destination, montant, libelle ->
                viewModel.virement(source, destination, montant, libelle)
                dialogueVirement = false
            },
        )
    }
}

/** Bandeau de tête : solde global et flux du mois en cours. */
@Composable
private fun TreSyntheseCarte(etat: TresorerieViewModel.EtatTresorerie, devise: String) {
    val negatif = etat.soldeGlobal < 0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (negatif) Red40 else BrandBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.tre_solde_global),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = MoneyUtils.format(etat.soldeGlobal, devise),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(11.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TreFluxColonne(
                    libelleRes = R.string.tre_encaissements_mois,
                    montant = etat.fluxDuMois.entrees,
                    devise = devise,
                    icone = Icons.AutoMirrored.Outlined.CallReceived,
                    modifier = Modifier.weight(1f),
                )
                TreFluxColonne(
                    libelleRes = R.string.tre_decaissements_mois,
                    montant = etat.fluxDuMois.sorties,
                    devise = devise,
                    icone = Icons.AutoMirrored.Outlined.CallMade,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TreFluxColonne(
    libelleRes: Int,
    montant: Double,
    devise: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = stringResource(libelleRes),
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = MoneyUtils.format(montant, devise),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Vignette d'un compte : type, nom, solde, et pastille « fermé » le cas échéant. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreCompteCarte(
    solde: SoldeCompte,
    devise: String,
    selectionne: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val type = TresorerieRules.typeCompte(solde.compte.type)
    Card(
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectionne) MissaSoftBlue else MissaSurface,
        ),
        border = BorderStroke(1.dp, if (selectionne) BrandBlue else MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(165.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (type) {
                        TypeCompteTresorerie.CAISSE -> Icons.Outlined.Savings
                        TypeCompteTresorerie.BANQUE -> Icons.Outlined.AccountBalance
                        TypeCompteTresorerie.MOBILE_MONEY -> Icons.Outlined.PhoneAndroid
                    },
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = solde.compte.nom,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = MoneyUtils.format(solde.solde, devise),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (solde.solde < 0) Red40 else MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (solde.compte.actif) {
                        TresorerieRules.libelleType(type)
                    } else {
                        R.string.tre_compte_ferme
                    },
                ),
                fontSize = 10.5.sp,
                color = if (solde.compte.actif) MissaMuted else Red40,
            )
        }
    }
}

/** Où part l'argent : trois premiers postes de dépense du mois. */
@Composable
private fun TreRepartition(
    repartition: List<Pair<com.missa.b360.core.data.entity.CategorieTresorerie, Double>>,
    devise: String,
) {
    val total = repartition.sumOf { it.second }.takeIf { it > 0 } ?: return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = stringResource(R.string.tre_repartition),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            repartition.take(3).forEach { (categorie, montant) ->
                val part = (montant / total).toFloat().coerceIn(0f, 1f)
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(TresorerieRules.libelleCategorie(categorie)),
                            fontSize = 11.5.sp,
                            color = MissaInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = MoneyUtils.format(montant, devise),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaInk,
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(MissaBorder, RoundedCornerShape(3.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(part)
                                .height(5.dp)
                                .background(BrandBlue, RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
        }
    }
}

/** Ligne de mouvement : sens, libellé, compte, date, montant signé et pointage. */
@Composable
private fun TreMouvementLigne(
    mouvement: MouvementTresorerieEntity,
    devise: String,
    nomCompte: String,
    onPointer: () -> Unit,
) {
    val entree = mouvement.sens == SensMouvement.IN.name
    val couleur = if (entree) ProfileGreen else Red40
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = couleur.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (entree) {
                        Icons.AutoMirrored.Outlined.CallReceived
                    } else {
                        Icons.AutoMirrored.Outlined.CallMade
                    },
                    contentDescription = stringResource(
                        if (entree) R.string.tre_sens_entree else R.string.tre_sens_sortie,
                    ),
                    tint = couleur,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mouvement.libelle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    DateUtils.formatDate(mouvement.date),
                    nomCompte.takeIf { it.isNotBlank() },
                    mouvement.tiers,
                ).joinToString(" · "),
                fontSize = 10.5.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = (if (entree) "+ " else "− ") + MoneyUtils.format(mouvement.montant, devise),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = couleur,
        )
        IconButton(onClick = onPointer, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = if (mouvement.rapproche) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = stringResource(R.string.tre_rapprocher),
                tint = if (mouvement.rapproche) ProfileGreen else MissaBorder,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** Premier lancement : aucun compte, donc rien à afficher — on guide vers la création. */
@Composable
private fun TreAucunCompte(onCreer: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCreer),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tre_premier_compte_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(R.string.tre_premier_compte_sous),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
