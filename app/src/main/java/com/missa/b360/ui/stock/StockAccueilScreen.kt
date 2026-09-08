package com.missa.b360.ui.stock

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaFondFiligrane
import com.missa.b360.ui.components.sectionFonctionsModule
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
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
 * Accueil du module Stock — carrefour du module.
 *
 * L'écran répond dans l'ordre aux questions du magasinier : sur quel dépôt
 * je travaille, où en est mon stock, que puis-je faire, et qu'ai-je à traiter.
 * Chaque chiffre et chaque tuile mène à un écran réel ; rien n'y est décoratif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAccueilScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    viewModel: StockHubViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val depotChoisi by viewModel.depotChoisi.collectAsState()
    val rechargement by viewModel.rechargement.collectAsState()
    var menuOuvert by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_stock)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNaviguer(Routes.STOCK_ARTICLES) }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.stk_recherche),
                        )
                    }
                    IconButton(onClick = viewModel::recharger, enabled = !rechargement) {
                        if (rechargement) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = BrandBlue,
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.stk_actualiser),
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOuvert = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.stk_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOuvert,
                            onDismissRequest = { menuOuvert = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stk_menu_parametres)) },
                                onClick = {
                                    menuOuvert = false
                                    onNaviguer(Routes.ADMIN_REFERENTIELS)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stk_menu_depots)) },
                                onClick = {
                                    menuOuvert = false
                                    onNaviguer(Routes.ADMIN_MULTISITE)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stk_menu_aide)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, null)
                                },
                                onClick = {
                                    menuOuvert = false
                                    onNaviguer(Routes.ADMIN_A_PROPOS)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.STK),
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                item {
                    SelecteurDepot(
                        depots = depots.map { it.id to it.nom },
                        choisi = depotChoisi,
                        onChoisir = viewModel::choisirDepot,
                    )
                }

                item { TitreSection(stringResource(R.string.stk_tableau_de_bord)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CarteIndicateur(
                                titre = stringResource(R.string.stk_valeur),
                                valeur = MoneyUtils.format(etat.valeurStock, devise),
                                detail = stringResource(R.string.stk_valeur_detail),
                                icone = Icons.Outlined.Payments,
                                couleur = BrandBlue,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(AppModule.REPORTING.route) }
                            CarteIndicateur(
                                titre = stringResource(R.string.stk_articles),
                                valeur = etat.nombreArticles.toString(),
                                detail = stringResource(
                                    R.string.stk_articles_detail,
                                    etat.articlesSousSeuil.size,
                                ),
                                icone = Icons.Outlined.Inventory2,
                                couleur = ProfileGreen,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(Routes.STOCK_ARTICLES) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CarteIndicateur(
                                titre = stringResource(R.string.stk_alertes),
                                valeur = etat.alertes.toString(),
                                detail = stringResource(R.string.stk_alertes_detail),
                                icone = Icons.Outlined.Warning,
                                couleur = if (etat.alertes > 0) ProfileOrange else MissaMuted,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(AppModule.REPORTING.route) }
                            CarteIndicateur(
                                titre = stringResource(R.string.stk_aujourdhui),
                                valeur = stringResource(
                                    R.string.stk_mouvements_valeur,
                                    etat.mouvementsDuJour,
                                ),
                                detail = stringResource(R.string.stk_aujourdhui_detail),
                                icone = Icons.Outlined.SwapHoriz,
                                couleur = ProfileOrange,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(Routes.STOCK_MOVEMENT_FORM) }
                        }
                    }
                }

                item { TitreSection(stringResource(R.string.stk_actions)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        TUILES.chunked(2).forEach { rangee ->
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                rangee.forEach { tuile ->
                                    TuileAction(
                                        tuile = tuile,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onNaviguer(tuile.route) },
                                    )
                                }
                                repeat(2 - rangee.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                if (etat.derniersMouvements.isNotEmpty()) {
                    item { TitreSection(stringResource(R.string.stk_raccourcis)) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(etat.derniersMouvements, key = { it.id }) { mouvement ->
                                CarteRaccourci(
                                    libelle = mouvement.motif,
                                    detail = DateUtils.formatDate(mouvement.horodatage),
                                    icone = iconeMouvement(mouvement.type),
                                ) { onNaviguer(Routes.STOCK_MOVEMENT_FORM) }
                            }
                        }
                    }
                }

                item { TitreSection(stringResource(R.string.stk_a_traiter)) }
                if (etat.alertes == 0) {
                    item {
                        Text(
                            text = stringResource(R.string.stk_rien_a_traiter),
                            fontSize = 12.sp,
                            color = MissaMuted,
                        )
                    }
                } else {
                    if (etat.articlesSousSeuil.isNotEmpty()) {
                        item {
                            LigneATraiter(
                                libelle = stringResource(
                                    R.string.stk_a_traiter_seuil,
                                    etat.articlesSousSeuil.size,
                                ),
                                icone = Icons.Outlined.ReportProblem,
                                couleur = ProfileOrange,
                            ) { onNaviguer(Routes.STOCK_ARTICLES) }
                        }
                    }
                    if (etat.ruptures > 0) {
                        item {
                            LigneATraiter(
                                libelle = stringResource(R.string.stk_a_traiter_rupture, etat.ruptures),
                                icone = Icons.Outlined.EventBusy,
                                couleur = Red40,
                            ) { onNaviguer(Routes.STOCK_ARTICLES) }
                        }
                    }
                }

                sectionFonctionsModule(ModuleCode.STK) { route -> onNaviguer(route) }
            }
        }
    }
}

/** Une tuile du carrefour : icône, titre, sous-titre, destination. */
private data class TuileStock(
    val titreRes: Int,
    val sousTitreRes: Int,
    val icone: ImageVector,
    val couleur: Color,
    val route: String,
)

private val TUILES = listOf(
    TuileStock(
        R.string.stk_tuile_ajouter,
        R.string.stk_tuile_ajouter_sous,
        Icons.Outlined.AddBox,
        ProfileGreen,
        "${Routes.STOCK_MOVEMENT_FORM}?type=ENTREE",
    ),
    TuileStock(
        R.string.stk_tuile_modifier,
        R.string.stk_tuile_modifier_sous,
        Icons.Outlined.EditNote,
        ProfileOrange,
        "${Routes.STOCK_MOVEMENT_FORM}?type=AJUSTEMENT",
    ),
    TuileStock(
        R.string.stk_tuile_mouvements,
        R.string.stk_tuile_mouvements_sous,
        Icons.Outlined.SwapHoriz,
        BrandBlue,
        Routes.STOCK_MOVEMENT_FORM,
    ),
    TuileStock(
        R.string.stk_tuile_inventaires,
        R.string.stk_tuile_inventaires_sous,
        Icons.Outlined.Assignment,
        BrandBlue,
        Routes.STOCK_INVENTORY,
    ),
    TuileStock(
        R.string.stk_tuile_transferts,
        R.string.stk_tuile_transferts_sous,
        Icons.AutoMirrored.Outlined.CompareArrows,
        ProfileOrange,
        Routes.STOCK_TRANSFER_FORM,
    ),
    TuileStock(
        R.string.stk_tuile_dashboard,
        R.string.stk_tuile_dashboard_sous,
        Icons.Outlined.BarChart,
        BrandBlue,
        AppModule.REPORTING.route,
    ),
)

private fun iconeMouvement(type: StockMovementType): ImageVector = when (type) {
    StockMovementType.ENTREE, StockMovementType.TRANSFERT_ENTREE -> Icons.Outlined.AddBox
    StockMovementType.SORTIE, StockMovementType.TRANSFERT_SORTIE -> Icons.Outlined.SwapHoriz
    StockMovementType.AJUSTEMENT -> Icons.Outlined.EditNote
}

@Composable
private fun SelecteurDepot(
    depots: List<Pair<Long, String>>,
    choisi: Long?,
    onChoisir: (Long?) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    val libelle = depots.firstOrNull { it.first == choisi }?.second
        ?: stringResource(R.string.stk_tous_depots)
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { ouvert = true },
            shape = RoundedCornerShape(12.dp),
            color = MissaSurface,
            border = BorderStroke(1.dp, MissaBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Store,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stk_depot),
                        fontSize = 10.5.sp,
                        color = MissaMuted,
                    )
                    Text(
                        text = libelle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stk_tous_depots)) },
                onClick = {
                    ouvert = false
                    onChoisir(null)
                },
            )
            depots.forEach { (id, nom) ->
                DropdownMenuItem(
                    text = { Text(nom) },
                    onClick = {
                        ouvert = false
                        onChoisir(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun CarteIndicateur(
    titre: String,
    valeur: String,
    detail: String,
    icone: ImageVector,
    couleur: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = couleur.copy(alpha = 0.12f),
            ) {
                Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.padding(7.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = titre, fontSize = 11.sp, color = MissaMuted, maxLines = 1)
            Text(
                text = valeur,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                fontSize = 10.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TuileAction(
    tuile: TuileStock,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = tuile.couleur.copy(alpha = 0.12f),
            ) {
                Icon(
                    tuile.icone,
                    contentDescription = null,
                    tint = tuile.couleur,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tuile.titreRes),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(tuile.sousTitreRes),
                    fontSize = 10.sp,
                    color = MissaMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CarteRaccourci(
    libelle: String,
    detail: String,
    icone: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(168.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MissaSoftBlue,
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icone, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = libelle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = detail, fontSize = 9.5.sp, color = MissaMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LigneATraiter(
    libelle: String,
    icone: ImageVector,
    couleur: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(11.dp))
            Text(
                text = libelle,
                fontSize = 12.5.sp,
                color = MissaInk,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun TitreSection(titre: String) {
    Text(
        text = titre,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MissaInk,
        modifier = Modifier.padding(top = 3.dp),
    )
}
