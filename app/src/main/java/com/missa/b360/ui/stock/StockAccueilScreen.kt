package com.missa.b360.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.LigneGroupeStock
import com.missa.b360.core.domain.model.LigneMouvementRecente
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.StockHub
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaBrandMark
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
import com.missa.b360.ui.theme.ProfileCommerceBlue
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.ProfileTeal
import com.missa.b360.ui.theme.ProfileViolet
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.TendrePositive
import kotlin.math.roundToInt

/** Couleurs du bandeau et des fonds de l'accueil Stock. */
private val HeaderBlueStart = Color(0xFF0B3FBF)
private val HeaderBlueEnd = Color(0xFF1554E8)

/**
 * Accueil du module Stock — carrefour du module, fidèle à la maquette :
 * bandeau marque, sélecteur de dépôt, bannière de prise en main, tableau de
 * bord, répartition par groupe, ce qu'il y a à traiter, derniers mouvements et
 * raccourcis d'action. Chaque chiffre et chaque tuile mène à un écran réel.
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

    MissaFondFiligrane(
        filigrane = Filigrane.pour(ModuleCode.STK),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 92.dp),
        ) {
            // Bandeau marque + actions, sous la barre de statut système.
            item {
                Box(Modifier.fillMaxWidth().statusBarsPadding()) {
                    BandeauStock(
                        rechargement = rechargement,
                        onRechercher = { onNaviguer(Routes.STOCK_ARTICLES) },
                        onActualiser = viewModel::recharger,
                        onParametres = { onNaviguer(Routes.ADMIN_REFERENTIELS) },
                        onDepots = { onNaviguer(Routes.ADMIN_MULTISITE) },
                        onAide = { onNaviguer(Routes.ADMIN_A_PROPOS) },
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Spacer(Modifier.height(12.dp))
                    SelecteurDepot(
                        depots = depots.map { it.id to it.nom },
                        choisi = depotChoisi,
                        minutesDepuisActivite = etat.minutesDepuisActivite,
                        onChoisir = viewModel::choisirDepot,
                    )
                    BanniereStock { onNaviguer(Routes.STOCK_ARTICLES) }
                }
            }

            // Tableau de bord.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp),
                ) {
                    EnTeteSection(
                        titre = stringResource(R.string.stk_tableau_de_bord),
                        onVoirTout = { onNaviguer(AppModule.REPORTING.route) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CarteKpi(
                                titre = stringResource(R.string.stk_valeur),
                                valeur = MoneyUtils.format(etat.valeurStock, devise),
                                detail = stringResource(R.string.stk_valeur_detail),
                                icone = Icons.Outlined.Payments,
                                couleur = ProfileCommerceBlue,
                                decor = Icons.Outlined.Payments,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(AppModule.REPORTING.route) }
                            CarteKpi(
                                titre = stringResource(R.string.stk_articles),
                                valeur = etat.nombreArticles.toString(),
                                detail = stringResource(
                                    R.string.stk_articles_detail,
                                    etat.articlesSousSeuil.size,
                                ),
                                icone = Icons.Outlined.Inventory2,
                                couleur = ProfileGreen,
                                decor = Icons.Outlined.Inventory2,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(Routes.STOCK_ARTICLES) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CarteKpi(
                                titre = stringResource(R.string.stk_alertes),
                                valeur = etat.alertes.toString(),
                                detail = stringResource(R.string.stk_alertes_detail),
                                icone = Icons.Outlined.Warning,
                                couleur = if (etat.alertes > 0) ProfileOrange else MissaMuted,
                                decor = Icons.Outlined.Notifications,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(AppModule.REPORTING.route) }
                            CarteKpi(
                                titre = stringResource(R.string.stk_aujourdhui),
                                valeur = etat.mouvementsDuJour.toString(),
                                detail = detailMouvements(etat),
                                icone = Icons.Outlined.SwapHoriz,
                                couleur = ProfilePurple,
                                decor = Icons.Outlined.SwapHoriz,
                                deltas = etat.tendanceMouvements,
                                modifier = Modifier.weight(1f),
                            ) { onNaviguer(Routes.STOCK_MOVEMENT_FORM) }
                        }
                    }
                }
            }

            // Stock par groupe.
            if (etat.groupes.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp),
                    ) {
                        EnTeteSection(
                            titre = stringResource(R.string.stk_stock_par_groupe),
                            onVoirTout = { onNaviguer(Routes.STOCK_ARTICLES) },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            etat.groupes.chunked(2).forEach { rangee ->
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    rangee.forEach { groupe ->
                                        CarteGroupe(
                                            groupe = groupe,
                                            devise = devise,
                                            modifier = Modifier.weight(1f),
                                        ) { onNaviguer(Routes.STOCK_ARTICLES) }
                                    }
                                    repeat(2 - rangee.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }

            // À traiter + Derniers mouvements (deux colonnes).
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    CarteATraiter(
                        etat = etat,
                        modifier = Modifier.weight(1f),
                        onSousSeuil = { onNaviguer(Routes.STOCK_ARTICLES) },
                        onRuptures = { onNaviguer(Routes.STOCK_ARTICLES) },
                        onVoirTout = { onNaviguer(AppModule.REPORTING.route) },
                    )
                    CarteDerniersMouvements(
                        mouvements = etat.derniersMouvements,
                        modifier = Modifier.weight(1f),
                        onVoirTout = { onNaviguer(Routes.STOCK_MOVEMENT_FORM) },
                    )
                }
            }

            // Raccourcis.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp),
                ) {
                    EnTeteSection(titre = stringResource(R.string.stk_raccourcis), onVoirTout = null)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(TUILES) { tuile ->
                            TuileRaccourci(
                                tuile = tuile,
                                onClick = { onNaviguer(tuile.route) },
                            )
                        }
                    }
                }
            }

            sectionFonctionsModule(ModuleCode.STK) { route -> onNaviguer(route) }
        }
    }
}

@Composable
private fun detailMouvements(etat: StockHub): String {
    val delta = etat.tendanceMouvements ?: return stringResource(R.string.stk_aujourdhui_detail)
    val pct = (delta * 100).roundToInt()
    val signe = if (pct >= 0) "+" else ""
    return stringResource(R.string.stk_tendance_vs_hier, "$signe$pct%")
}

/** Bandeau bleu de la marque : logo, nom, titre du module et actions. */
@Composable
private fun BandeauStock(
    rechargement: Boolean,
    onRechercher: () -> Unit,
    onActualiser: () -> Unit,
    onParametres: () -> Unit,
    onDepots: () -> Unit,
    onAide: () -> Unit,
) {
    var menuOuvert by remember { mutableStateOf(false) }
    Surface(color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HeaderBlueStart, HeaderBlueEnd))),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MissaBrandMark(size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.stk_marque),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 14.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.stk_marque_suite),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 11.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    text = stringResource(R.string.stk_titre),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRechercher) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.stk_recherche),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onActualiser, enabled = !rechargement) {
                    if (rechargement) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.stk_actualiser),
                            tint = Color.White,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOuvert = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.stk_menu),
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stk_menu_parametres)) },
                            onClick = { menuOuvert = false; onParametres() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stk_menu_depots)) },
                            onClick = { menuOuvert = false; onDepots() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stk_menu_aide)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                            onClick = { menuOuvert = false; onAide() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelecteurDepot(
    depots: List<Pair<Long, String>>,
    choisi: Long?,
    minutesDepuisActivite: Int?,
    onChoisir: (Long?) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    val libelle = depots.firstOrNull { it.first == choisi }?.second
        ?: stringResource(R.string.stk_tous_depots)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { ouvert = true },
        shape = RoundedCornerShape(12.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = MissaSoftBlue,
            ) {
                Icon(
                    Icons.Outlined.Store,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stk_depot),
                    fontSize = 10.sp,
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
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(ProfileGreen, CircleShape),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.stk_a_jour),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProfileGreen,
                    )
                }
                Text(
                    text = libelleFraicheur(minutesDepuisActivite),
                    fontSize = 9.5.sp,
                    color = MissaMuted,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(11.dp),
            )
        }
    }
    DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.stk_tous_depots)) },
            onClick = { ouvert = false; onChoisir(null) },
        )
        depots.forEach { (id, nom) ->
            DropdownMenuItem(
                text = { Text(nom) },
                onClick = { ouvert = false; onChoisir(id) },
            )
        }
    }
}

@Composable
private fun libelleFraicheur(minutes: Int?): String {
    if (minutes == null) return stringResource(R.string.stk_a_jour_court)
    return when {
        minutes < 1 -> stringResource(R.string.stk_a_jour_instant)
        minutes < 60 ->
            stringResource(R.string.stk_a_jour_relatif, stringResource(R.string.stk_minutes, minutes))
        else ->
            stringResource(R.string.stk_a_jour_relatif, stringResource(R.string.stk_heures, minutes / 60))
    }
}

/** Bannière bleue de prise en main, avec l'illustration d'entrepôt en fond. */
@Composable
private fun BanniereStock(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(HeaderBlueStart, HeaderBlueEnd)))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.fond_entrepot),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.28f,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.52f),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stk_hero_titre),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.stk_hero_texte),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun EnTeteSection(titre: String, onVoirTout: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titre,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            modifier = Modifier.weight(1f),
        )
        if (onVoirTout != null) {
            Row(
                modifier = Modifier.clickable(onClick = onVoirTout),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    fontSize = 11.sp,
                    color = BrandBlue,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

/** En-tête de section portant une icône ; utilisé par les deux colonnes du bas. */
@Composable
private fun EnTeteColonne(titre: String, onVoirTout: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titre,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            modifier = Modifier.weight(1f),
        )
        if (onVoirTout != null) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = stringResource(R.string.home_see_all),
                tint = BrandBlue,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Carte de tableau de bord : icône, valeur, libellé et détail, fond décoratif. */
@Composable
private fun CarteKpi(
    titre: String,
    valeur: String,
    detail: String,
    icone: ImageVector,
    couleur: Color,
    decor: ImageVector,
    modifier: Modifier = Modifier,
    deltas: Double? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.height(118.dp)) {
            // Décor discret dans le coin — un rappel du domaine (pièces, articles…).
            Icon(
                decor,
                contentDescription = null,
                tint = couleur.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(52.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = couleur.copy(alpha = 0.13f),
                ) {
                    Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.padding(7.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = valeur,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = titre,
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                    maxLines = 1,
                )
                Text(
                    text = detail,
                    fontSize = 9.5.sp,
                    color = if (deltas != null) TendrePositive else MissaMuted,
                    fontWeight = if (deltas != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CarteGroupe(
    groupe: LigneGroupeStock,
    devise: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val style = styleGroupe(groupe.code)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.height(104.dp)) {
            Icon(
                style.icone,
                contentDescription = null,
                tint = style.couleur.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 16.dp)
                    .size(50.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = style.couleur.copy(alpha = 0.13f),
                ) {
                    Icon(
                        style.icone,
                        contentDescription = null,
                        tint = style.couleur,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupe.nom,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.stk_groupe_articles, groupe.nombreArticles),
                        fontSize = 9.5.sp,
                        color = MissaMuted,
                        maxLines = 1,
                    )
                    Text(
                        text = MoneyUtils.format(groupe.valeur, devise),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class StyleGroupe(val icone: ImageVector, val couleur: Color)

private fun styleGroupe(code: String): StyleGroupe = when (code) {
    "MARCH" -> StyleGroupe(Icons.Outlined.ShoppingCart, ProfileCommerceBlue)
    "EQUIP" -> StyleGroupe(Icons.Outlined.Settings, ProfileViolet)
    "MP" -> StyleGroupe(Icons.Outlined.Build, ProfileGreen)
    "CONSO" -> StyleGroupe(Icons.Outlined.AddBox, ProfileOrange)
    "PF" -> StyleGroupe(Icons.Outlined.Inventory2, ProfileTeal)
    "SE" -> StyleGroupe(Icons.Outlined.Category, ProfilePurple)
    "SERV" -> StyleGroupe(Icons.Outlined.Assignment, ProfileCommerceBlue)
    "CHIM" -> StyleGroupe(Icons.Outlined.Settings, ProfileViolet)
    else -> StyleGroupe(Icons.Outlined.Category, MissaMuted)
}

/** Carte « À traiter » : file d'alertes du magasinier. */
@Composable
private fun CarteATraiter(
    etat: StockHub,
    modifier: Modifier = Modifier,
    onSousSeuil: () -> Unit,
    onRuptures: () -> Unit,
    onVoirTout: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column {
            EnTeteColonne(stringResource(R.string.stk_a_traiter), onVoirTout)
            if (etat.articlesSousSeuil.isNotEmpty()) {
                LigneATraiter(
                    libelle = stringResource(
                        R.string.stk_a_traiter_seuil,
                        etat.articlesSousSeuil.size,
                    ),
                    badge = stringResource(R.string.stk_urgence_urgent),
                    couleur = ProfileOrange,
                    badgeCouleur = ProfileOrange,
                    exemple = exempleSousSeuil(etat),
                    onClick = onSousSeuil,
                )
            }
            if (etat.ruptures > 0) {
                LigneATraiter(
                    libelle = stringResource(R.string.stk_a_traiter_rupture, etat.ruptures),
                    badge = stringResource(R.string.stk_urgence_important),
                    couleur = BrandBlue,
                    badgeCouleur = ProfileGreen,
                    exemple = null,
                    onClick = onRuptures,
                )
            }
            // Fonctionnalités non encore livrées : affichées comme prévues, jamais cliquables.
            LigneATraiter(
                libelle = stringResource(R.string.stk_a_traiter_inventaire),
                badge = stringResource(R.string.stk_urgence_avenir),
                couleur = MissaMuted,
                badgeCouleur = MissaMuted,
                exemple = null,
                onClick = null,
            )
            LigneATraiter(
                libelle = stringResource(R.string.stk_a_traiter_peremption),
                badge = stringResource(R.string.stk_urgence_avenir),
                couleur = MissaMuted,
                badgeCouleur = MissaMuted,
                exemple = null,
                onClick = null,
            )
        }
    }
}

@Composable
private fun exempleSousSeuil(etat: StockHub): String {
    val noms = etat.articlesSousSeuil.take(3).joinToString(", ") { it.produit.nom }
    return stringResource(R.string.stk_a_traiter_exemples, noms)
}

@Composable
private fun LigneATraiter(
    libelle: String,
    badge: String,
    couleur: Color,
    badgeCouleur: Color,
    exemple: String?,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ReportProblem,
                contentDescription = null,
                tint = couleur,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = libelle,
                fontSize = 10.5.sp,
                color = MissaInk,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(Modifier.width(4.dp))
            BadgeUrgence(badge, badgeCouleur)
        }
        if (exemple != null) {
            Text(
                text = exemple,
                fontSize = 9.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun BadgeUrgence(titre: String, couleur: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = couleur.copy(alpha = 0.13f),
    ) {
        Text(
            text = titre,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = couleur,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

/** Carte « Derniers mouvements » : les variations de stock les plus récentes. */
@Composable
private fun CarteDerniersMouvements(
    mouvements: List<LigneMouvementRecente>,
    modifier: Modifier = Modifier,
    onVoirTout: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column {
            EnTeteColonne(stringResource(R.string.stk_derniers_mouvements), onVoirTout)
            if (mouvements.isEmpty()) {
                Text(
                    text = stringResource(R.string.stk_aucun_mouvement),
                    fontSize = 10.sp,
                    color = MissaMuted,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                )
            } else {
                mouvements.forEach { mouvement ->
                    LigneMouvement(mouvement)
                }
            }
        }
    }
}

@Composable
private fun LigneMouvement(mouvement: LigneMouvementRecente) {
    val (sens, couleur, signe) = when {
        mouvement.estSortie -> Triple(Icons.AutoMirrored.Outlined.TrendingDown, Red40, "-")
        mouvement.estEntree -> Triple(Icons.AutoMirrored.Outlined.TrendingUp, ProfileGreen, "+")
        else -> Triple(
            Icons.Outlined.SwapVert,
            ProfileOrange,
            if (mouvement.quantite >= 0) "+" else "-",
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = couleur.copy(alpha = 0.12f),
        ) {
            Icon(sens, contentDescription = null, tint = couleur, modifier = Modifier.padding(6.dp))
        }
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mouvement.produitNom,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = libelleMouvement(mouvement),
                fontSize = 8.5.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$signe${formatQuantite(mouvement.quantite)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = couleur,
            )
            Text(
                text = DateUtils.formatHeure(mouvement.horodatage),
                fontSize = 8.5.sp,
                color = MissaMuted,
            )
        }
    }
}

@Composable
private fun libelleMouvement(mouvement: LigneMouvementRecente): String {
    val type = when (mouvement.type) {
        StockMovementType.ENTREE -> stringResource(R.string.stk_type_entree)
        StockMovementType.SORTIE -> stringResource(R.string.stk_type_sortie)
        StockMovementType.AJUSTEMENT -> stringResource(R.string.stk_type_ajustement)
        else -> stringResource(R.string.stk_type_transfert)
    }
    val reference = mouvement.reference?.takeIf { it.isNotBlank() }
    return if (reference != null) "$type · $reference" else type
}

private fun formatQuantite(quantite: Double): String {
    val simple = if (quantite % 1.0 == 0.0) quantite.toLong().toString()
        else String.format(java.util.Locale.ROOT, "%.1f", quantite)
    return simple
}

/** Tuile d'action d'un raccourci de la rangée du bas. */
@Composable
private fun TuileRaccourci(tuile: TuileStock, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MissaSoftBlue,
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(9.dp),
                color = tuile.couleur.copy(alpha = 0.14f),
            ) {
                Icon(
                    tuile.icone,
                    contentDescription = null,
                    tint = tuile.couleur,
                    modifier = Modifier.padding(7.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(tuile.titreRes),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(tuile.sousTitreRes),
                    fontSize = 9.sp,
                    color = MissaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
