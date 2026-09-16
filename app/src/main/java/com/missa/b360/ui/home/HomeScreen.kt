package com.missa.b360.ui.home

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge as NotificationBadge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.missa.b360.BuildConfig
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40

/* Palette du tableau de bord mobile. */
private val HomeBlue = BrandBlue
private val HomeBlueSoft = MissaSoftBlue
private val HomeGreen = Green60
private val HomeGreenSoft = HomeGreenSoft
private val HomeOrange = HomeOrange
private val HomeOrangeSoft = HomeOrangeSoft
private val HomePurple = HomePurple

/** Vert du « 360 » de la marque et de l'identité client. */
private val MarqueVert = MarqueVert
private val HomePurpleSoft = HomePurpleSoft
private val HomeTeal = HomeTeal
private val HomeRed = Red40
private val HomeTextDark = MissaInk
private val HomeTextMuted = MissaMuted
private val HomeBackground = MissaCanvas
private val HomeBorder = MissaBorder

/** Identifiants stables des 8 actions rapides de l'accueil (persistés dans SettingsStore). */
object AccueilActionKeys {
    const val VENTE = "VENTE"
    const val ACHAT = "ACHAT"
    const val CLIENT = "CLIENT"
    const val FOURNISSEUR = "FOURNISSEUR"
    const val ENTREE_STOCK = "ENTREE_STOCK"
    const val TRANSFERT_STOCK = "TRANSFERT_STOCK"
    const val PAIEMENT_RECU = "PAIEMENT_RECU"
    const val DEPENSE = "DEPENSE"
    val ALL: List<String> = listOf(
        VENTE, ACHAT, CLIENT, FOURNISSEUR,
        ENTREE_STOCK, TRANSFERT_STOCK, PAIEMENT_RECU, DEPENSE,
    )
}

private data class AccueilActionDef(
    val key: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val route: String,
    val module: ModuleCode,
)

@Composable
private fun rememberAccueilActionDefs(): List<AccueilActionDef> = listOf(
    AccueilActionDef(
        key = AccueilActionKeys.VENTE,
        labelRes = R.string.home_plus_vente,
        icon = Icons.Outlined.ShoppingCart,
        tint = HomeBlue,
        bg = HomeBlueSoft,
        route = AppModule.VENTE.createRoute(),
        module = ModuleCode.VEN,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.ACHAT,
        labelRes = R.string.home_plus_achat,
        icon = Icons.Outlined.ShoppingCart,
        tint = TendrePositive,
        bg = Green90,
        route = AppModule.ACHATS.createRoute(),
        module = ModuleCode.ACH,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.CLIENT,
        labelRes = R.string.home_plus_client,
        icon = Icons.Outlined.PersonAdd,
        tint = ProfilePurple,
        bg = HomePurpleSoft,
        route = AppModule.CLIENTS.createRoute(),
        module = ModuleCode.VEN,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.FOURNISSEUR,
        labelRes = R.string.home_plus_fournisseur,
        icon = Icons.Outlined.Business,
        tint = ProfileOrange,
        bg = HomeOrangeSoft,
        route = AppModule.FOURNISSEURS.createRoute(),
        module = ModuleCode.ACH,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.ENTREE_STOCK,
        labelRes = R.string.home_entree_en_stock,
        icon = Icons.Outlined.Inventory2,
        tint = ProfileTeal,
        bg = Green90,
        route = AppModule.STOCK.createRoute(),
        module = ModuleCode.STK,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.TRANSFERT_STOCK,
        labelRes = R.string.home_transfert_de_stock,
        icon = Icons.Outlined.LocalShipping,
        tint = HomeBlue,
        bg = HomeBlueSoft,
        route = Routes.STOCK_TRANSFER_FORM,
        module = ModuleCode.STK,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.PAIEMENT_RECU,
        labelRes = R.string.home_paiement_recu_label,
        icon = Icons.Outlined.Payments,
        tint = TendrePositive,
        bg = Green90,
        route = AppModule.TRESORERIE.route,
        module = ModuleCode.TRE,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.DEPENSE,
        labelRes = R.string.home_depense_label,
        icon = Icons.Outlined.Description,
        tint = HomeRed,
        bg = Red80,
        route = AppModule.FINANCES.route,
        module = ModuleCode.CPT,
    ),
)

/**
 * Accueil mobile : tableau de bord sans données de démonstration. Les métriques sont
 * recalculées à partir des pièces opérationnelles réellement validées.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onOuvrirMenu: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val nonLues by viewModel.notificationsNonLues.collectAsState(initial = 0)
    val uiState by viewModel.uiState.collectAsState()
    val modulesActifs by viewModel.modulesActifs.collectAsState()
    val actionsEpingles by viewModel.actionsRapidesEpingles.collectAsState()
    var showPersonnaliser by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val companyName = uiState.entrepriseNom.ifBlank {
        stringResource(R.string.home_company_placeholder)
    }
    val profileLabel = uiState.profilActivite.profileLabel()
        ?.let { stringResource(it) }
        ?: stringResource(R.string.home_not_configured)
    val sizeLabel = uiState.palierTaille.sizeLabel()
        ?.let { stringResource(it) }
        ?: stringResource(R.string.home_not_configured)
    val greeting = uiState.prenomUtilisateur?.let {
        stringResource(R.string.home_greeting, it)
    } ?: stringResource(R.string.home_greeting_anonymous)

    // Le tiroir et la barre du bas appartiennent au graphe de navigation : les
    // redéclarer ici en donnait deux exemplaires, dont l'un se superposait à
    // l'autre sur certains écrans.
    Scaffold(
        containerColor = HomeBackground,
        topBar = {
            HomeHeader(
                companyName = companyName,
                companyLogoUri = uiState.entrepriseLogoUri,
                secteur = uiState.secteur,
                profileLabel = profileLabel,
                sizeLabel = sizeLabel,
                greeting = greeting,
                notificationCount = nonLues,
                onMenuClick = onOuvrirMenu,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                // Le bloc porte le logo et le nom de l'entreprise : il ouvre
                // sa fiche. Le compte utilisateur a son entrée au tiroir.
                onProfileClick = { navController.navigate(Routes.ADMIN_REGLAGES) },
            )
        },
    ) { padding ->
        HomeDashboard(
            state = uiState,
            modulesActifs = modulesActifs,
            actionsRapidesSelection = actionsEpingles,
            onPersonnaliser = { showPersonnaliser = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onNavigate = { navController.navigate(it) },
        )
    }

    if (showPersonnaliser) {
        HomePersonnaliserDialogue(
            actionsSelection = actionsEpingles,
            onFermer = { showPersonnaliser = false },
            onValider = { choixActions ->
                viewModel.epinglerActionsRapides(choixActions)
                showPersonnaliser = false
            },
        )
    }

}

@Composable
private fun HomeHeader(
    companyName: String,
    companyLogoUri: String?,
    secteur: String,
    profileLabel: String,
    sizeLabel: String,
    greeting: String,
    notificationCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    // Header redesigné : MISSA à gauche, entreprise à droite, sans onglet déroulant.
    // Fond basé sur le logo de l'utilisateur : watermark + dégradé + halos.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(Color.White),
        ) {
            // --- Fond basé sur le logo entreprise ---
            // Halo vert très léger derrière le logo entreprise (droite)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(160.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Green90.copy(alpha = 0.85f), Color.Transparent),
                            radius = 200f,
                        ),
                    ),
            )
            // Halo bleu très léger derrière MISSA (gauche)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(140.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Blue80.copy(alpha = 0.9f), Color.Transparent),
                            radius = 180f,
                        ),
                    ),
            )
            // Watermark du logo entreprise en fond, très faible, basé sur le logo réel
            if (companyLogoUri != null) {
                CompanyLogoWatermark(
                    logoUri = companyLogoUri,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.62f)
                        .height(64.dp)
                        .alpha(0.07f),
                )
            } else {
                // Fallback pattern quand pas de logo : damier subtil
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    MissaCanvas,
                                    Blue90,
                                    Green90,
                                ),
                            ),
                        ),
                )
            }
            // Ligne de séparation basse subtile + mini dégradé marque -> entreprise
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HomeBorder),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                HomeBlue.copy(alpha = 0.18f),
                                TendrePositive.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.drawer_admin),
                        tint = HomeTextDark,
                        modifier = Modifier.size(22.dp),
                    )
                }
                // MISSA BUSINESS à gauche
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* logo MISSA = accueil */ },
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_missa),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp)),
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MISSA",
                                color = HomeTextDark,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 12.sp,
                                letterSpacing = 0.3.sp,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "BUSINESS",
                                color = HomeTextDark,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 12.sp,
                                letterSpacing = 0.3.sp,
                            )
                        }
                        Text(
                            text = "360",
                            color = TendrePositive,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Notifications
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(38.dp)) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                NotificationBadge(containerColor = Red40, contentColor = Color.White) {
                                    Text(notificationCount.coerceAtMost(99).toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = HomeTextDark,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Logo entreprise à droite, sans onglet déroulant – 100% réel, plus de GREEN FARM hardcodé
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onProfileClick),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, HomeBorder),
                    shadowElevation = 2.dp,
                ) {
                    if (companyLogoUri != null) {
                        CompanyLogo(
                            logoUri = companyLogoUri,
                            contentDescription = stringResource(R.string.home_company_active),
                            fallbackIcon = Icons.Outlined.Store,
                            modifier = Modifier.fillMaxSize(),
                            size = 40.dp,
                            shape = CircleShape,
                            fallbackTint = TendrePositive,
                            fallbackBackground = Green90,
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Green90)) {
                            Icon(
                                imageVector = Icons.Outlined.Store,
                                contentDescription = stringResource(R.string.home_company_active),
                                tint = TendrePositive,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyLogoWatermark(
    logoUri: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        CompanyLogo(
            logoUri = logoUri,
            contentDescription = null,
            fallbackIcon = Icons.Outlined.Store,
            modifier = Modifier.fillMaxSize(),
            size = 120.dp,
            shape = RoundedCornerShape(12.dp),
            fallbackTint = TendrePositive.copy(alpha = 0.12f),
            fallbackBackground = Color.Transparent,
        )
    }
}


@Composable
private fun HomeDashboard(
    state: HomeUiState,
    modulesActifs: List<ModuleCode>,
    actionsRapidesSelection: List<String>,
    onPersonnaliser: () -> Unit,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
) {
    val currency = state.devise
    val greeting = state.prenomUtilisateur?.let { stringResource(R.string.home_greeting, it) }
        ?: stringResource(R.string.home_greeting_anonymous)
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            // Salutation
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = greeting,
                    color = HomeTextDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.home_overview),
                    color = HomeTextMuted,
                    fontSize = 13.sp,
                )
            }
        }
        item {
            // Barre profil / taille
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.ADMIN_REGLAGES) },
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HomeBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = run {
                            val profil = state.profilActivite?.let { code ->
                                when (code) {
                                    "AV" -> "D - Distribution / Grossiste"
                                    "ASV" -> "ASV"
                                    "APSV" -> "APSV"
                                    "SER" -> "Service"
                                    else -> code
                                }
                            } ?: "D - Distribution / Grossiste"
                            stringResource(R.string.home_profil_ligne, profil)
                        },
                        color = HomeTextDark,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(HomeBorder))
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = run {
                            val taille = state.palierTaille?.let { code ->
                                when (code) {
                                    "P3" -> "P3 - Petite (10-49)"
                                    "P1" -> "P1"
                                    "P2" -> "P2"
                                    "P4" -> "P4"
                                    else -> code
                                }
                            } ?: "P3 - Petite (10-49)"
                            stringResource(R.string.home_taille_ligne, taille)
                        },
                        color = HomeTextDark,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = HomeTextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        // KPI 4 cartes – 100% réel, charte 3D plein cadre avec crop
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_ventes_du_jour),
                        valeur = formatMontantSansDecimales(state.ventes, currency),
                        sousTitre = "${state.ventesCount} ventes",
                        tendance = state.tendanceVentes,
                        icon = Icons.Outlined.ShoppingCart,
                        iconBg = HomeBlueSoft,
                        iconTint = HomeBlue,
                        illustrationRes = R.drawable.illustration_ventes,
                        onClick = { onNavigate(AppModule.VENTE.route) },
                    )
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_achats_du_jour),
                        valeur = formatMontantSansDecimales(state.achats, currency),
                        sousTitre = "${state.achatsCount} achats",
                        tendance = state.tendanceAchats,
                        icon = Icons.Outlined.Inventory2,
                        iconBg = Green90,
                        iconTint = TendrePositive,
                        illustrationRes = R.drawable.illustration_stock,
                        onClick = { onNavigate(AppModule.ACHATS.route) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_tresorerie_card),
                        valeur = formatMontantSansDecimales(state.tresorerie, currency),
                        sousTitre = stringResource(R.string.home_solde_disponible),
                        tendance = state.tendanceTresorerie,
                        icon = Icons.Outlined.Payments,
                        iconBg = HomeOrangeSoft,
                        iconTint = ProfileOrange,
                        illustrationRes = R.drawable.illustration_tresorerie,
                        onClick = { onNavigate(AppModule.TRESORERIE.route) },
                    )
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_clients_card),
                        valeur = state.nombreClients.toString(),
                        sousTitre = stringResource(R.string.home_total_label),
                        tendance = state.tendanceClients,
                        icon = Icons.Outlined.People,
                        iconBg = HomePurpleSoft,
                        iconTint = ProfilePurple,
                        illustrationRes = R.drawable.illustration_clients,
                        onClick = { onNavigate(AppModule.CLIENTS.route) },
                    )
                }
            }
        }
        item {
            // Actions rapides
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_quick_actions),
                        modifier = Modifier.weight(1f),
                        color = HomeTextDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onPersonnaliser),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_personalize),
                            color = HomeBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = HomeBlue,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                AccueilActionsGrid(
                    modulesActifs = modulesActifs,
                    actionsSelection = actionsRapidesSelection,
                    onNavigate = onNavigate,
                )
            }
        }
        item {
            // Résumé de l'activité
            AccueilResumeCard(state = state, currency = currency)
        }
        item {
            // Activités récentes + Rappels + Tâches
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Sur mobile, on empile : Activités en haut, puis Rappels, puis Tâches
                AccueilActivitesRecentesCard(state = state, currency = currency, onNavigate = onNavigate)
                AccueilRappelsCard(state = state, onNavigate = onNavigate)
                AccueilTachesCard(state = state, onNavigate = onNavigate)
            }
        }
    }
}

private fun formatMontantSansDecimales(montant: Double, devise: String): String {
    val df = java.text.DecimalFormat("#,##0", com.missa.b360.core.util.FormatPrefs.symboles())
    // Remplace l'espace insécable étroit par espace normal pour correspondre à la maquette
    val corps = df.format(montant).replace('\u00A0', ' ').replace('\u202F', ' ')
    return "$corps $devise"
}

@Composable
private fun AccueilKpiCard(
    modifier: Modifier,
    titre: String,
    valeur: String,
    sousTitre: String,
    tendance: Double?,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    illustrationRes: Int? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Fond 3D plein cadre : remplit tout l'arrière-plan et rogne les parties hors-cadre
            if (illustrationRes != null) {
                Image(
                    painter = painterResource(id = illustrationRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // remplit le cadre, rogne ce qui dépasse
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .alpha(0.14f),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(text = titre, color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(text = valeur, color = HomeTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(text = sousTitre, color = HomeTextMuted, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (tendance != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.home_vs_hier), color = HomeTextMuted, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = (if (tendance >= 0) "+" else "") + String.format(java.util.Locale.ROOT, "%.0f%%", tendance),
                            color = TendrePositive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(text = stringResource(R.string.home_vs_hier) + " —", color = HomeTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AccueilActionsGrid(
    modulesActifs: List<ModuleCode>,
    actionsSelection: List<String>,
    onNavigate: (String) -> Unit,
) {
    val defs = rememberAccueilActionDefs()
    // 1) filtre par modules actifs (comportement existant)
    // 2) filtre par sélection utilisateur : vide = tout afficher (usine)
    val visibles = defs
        .filter { modulesActifs.isEmpty() || it.module in modulesActifs }
        .filter { actionsSelection.isEmpty() || it.key in actionsSelection }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (visibles.isEmpty()) {
            Text(
                text = stringResource(R.string.home_personalize_actions_aide),
                color = HomeTextMuted,
                fontSize = 11.sp,
            )
        } else {
            visibles.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { a ->
                        AccueilActionCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(a.labelRes),
                            icon = a.icon,
                            tint = a.tint,
                            bg = a.bg,
                            onClick = { onNavigate(a.route) },
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AccueilActionCard(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(9.dp), color = bg) {
                Box(contentAlignment = Alignment.Center) {
                    // Superpose un petit + pour les 4 premiers
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    if (label.startsWith("+")) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                            Text(text = "+", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = HomeTextDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccueilResumeCard(state: HomeUiState, currency: String) {
    val margePct = if (state.ventes > 0) state.marge / state.ventes * 100.0 else 0.0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_resume_activite), color = HomeTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(20.dp), color = HomeBackground, border = BorderStroke(1.dp, HomeBorder)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(R.string.home_aujourdhui), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.BarChart,
                    iconTint = HomeBlue,
                    iconBg = HomeBlueSoft,
                    titre = stringResource(R.string.home_ventes_label),
                    valeur = formatMontantSansDecimales(state.ventes, currency),
                    sousTitre = "${state.ventesCount} ventes",
                    tendance = state.tendanceVentes,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(HomeBorder))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ShoppingCart,
                    iconTint = TendrePositive,
                    iconBg = Green90,
                    titre = stringResource(R.string.home_achats_label),
                    valeur = formatMontantSansDecimales(state.achats, currency),
                    sousTitre = "${state.achatsCount} achats",
                    tendance = state.tendanceAchats,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(HomeBorder))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.SwapHoriz,
                    iconTint = ProfileOrange,
                    iconBg = HomeOrangeSoft,
                    titre = stringResource(R.string.home_mouvements_stock_label),
                    valeur = state.mouvementsStockCount.toString(),
                    sousTitre = if (state.rupturesStock > 0) "${state.rupturesStock} ruptures" else stringResource(R.string.home_operations_label),
                    tendance = null,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(HomeBorder))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Payments,
                    iconTint = ProfilePurple,
                    iconBg = HomePurpleSoft,
                    titre = stringResource(R.string.home_marge_brute_label),
                    valeur = formatMontantSansDecimales(state.marge, currency),
                    sousTitre = String.format(java.util.Locale.ROOT, "%.1f%%", margePct),
                    tendance = state.tendanceMarge,
                )
            }
            // Ligne additionnelle réelle : stock, projets, qualité – 100% i18n
            if (state.nombreProduits > 0 || state.projetsActifs > 0 || state.nonConformitesOuvertes > 0) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.nombreProduits > 0) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MissaCanvas, border = BorderStroke(1.dp, HomeBorder)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.home_produits_count, state.nombreProduits), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = formatMontantSansDecimales(state.valeurStock, currency), color = HomeTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                    if (state.projetsActifs > 0) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = HomePurpleSoft, border = BorderStroke(1.dp, HomeBorder)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.home_projets_actifs, state.projetsActifs), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (state.projetsEnRetard > 0) stringResource(R.string.home_en_retard, state.projetsEnRetard) else stringResource(R.string.home_a_jour),
                                    color = HomeTextMuted,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    if (state.nonConformitesOuvertes > 0) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Red80, border = BorderStroke(1.dp, Red80)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.home_nc_ouvertes, state.nonConformitesOuvertes), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = stringResource(R.string.home_qualite_label), color = HomeTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccueilResumeCell(
    modifier: Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    titre: String,
    valeur: String,
    sousTitre: String,
    tendance: Double?,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = iconBg) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = titre, color = HomeTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text = valeur, color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        Text(text = sousTitre, color = HomeTextMuted, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = stringResource(R.string.home_vs_hier), color = HomeTextMuted, fontSize = 9.sp)
            Spacer(Modifier.width(4.dp))
            if (tendance != null) {
                Text(text = (if (tendance >= 0) "+" else "") + String.format(java.util.Locale.ROOT, "%.0f%%", tendance), color = if (tendance >= 0) TendrePositive else Red40, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(text = "—", color = HomeTextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AccueilActivitesRecentesCard(state: HomeUiState, currency: String, onNavigate: (String) -> Unit) {
    val records = state.recentOperations
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_activites_recentes), color = HomeTextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.home_see_all),
                    color = HomeBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppModule.REPORTING.route) },
                )
            }
            Spacer(Modifier.height(10.dp))
            if (records.isEmpty()) {
                // 100% réel : vide = message, pas de maquette
                Text(
                    text = stringResource(R.string.home_no_recent_activity),
                    color = HomeTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                records.forEachIndexed { idx, rec ->
                    val (icon, bg, tint) = when (rec.module) {
                        OperationModule.VENTE.name -> Triple(Icons.Outlined.ShoppingCart, HomeBlueSoft, HomeBlue)
                        OperationModule.ACHATS.name -> Triple(Icons.Outlined.Inventory2, Green90, TendrePositive)
                        OperationModule.STOCK.name -> Triple(Icons.Outlined.LocalShipping, HomeOrangeSoft, ProfileOrange)
                        OperationModule.PROJETS.name -> Triple(Icons.Outlined.BarChart, HomePurpleSoft, ProfilePurple)
                        OperationModule.FINANCES.name -> Triple(Icons.Outlined.Payments, HomeOrangeSoft, ProfileOrange)
                        else -> Triple(Icons.Outlined.People, HomePurpleSoft, ProfilePurple)
                    }
                    AccueilActiviteRow(
                        icon = icon, iconBg = bg, iconTint = tint,
                        titre = rec.title.ifBlank { rec.reference },
                        sousTitre = rec.counterpart ?: rec.reference,
                        montant = rec.amount?.let { formatMontantSansDecimales(it, currency) },
                        badge = when (rec.status) {
                            OperationStatus.VALIDATED.name -> stringResource(R.string.home_payee)
                            OperationStatus.DRAFT.name -> "Brouillon"
                            else -> null
                        },
                        heure = DateUtils.formatDateHeure(rec.createdAt),
                        onClick = { onNavigate(rec.module.appModuleRoute()) },
                    )
                    if (idx < records.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AccueilActiviteRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    titre: String,
    sousTitre: String,
    montant: String?,
    badge: String?,
    heure: String,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = iconBg) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titre, color = HomeTextDark, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = sousTitre, color = HomeTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (montant != null) Text(text = montant, color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = heure, color = HomeTextMuted, fontSize = 10.sp)
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = Green90) {
                        Text(text = badge, color = TendrePositive, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccueilRappelsCard(state: HomeUiState, onNavigate: (String) -> Unit) {
    val factures = state.rappels.facturesEnRetard
    val commandes = state.commandesFournisseurAttente
    val nc = state.nonConformitesOuvertes
    val ruptures = state.rupturesStock
    val hasAlert = factures > 0 || commandes > 0 || nc > 0 || ruptures > 0
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.TASKS) },
        shape = RoundedCornerShape(16.dp),
        color = if (hasAlert) HomeBackground else Color.White,
        border = BorderStroke(1.dp, if (hasAlert) HomeBorder else HomeBorder),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = if (hasAlert) HomeBackground else HomeBackground) {
                Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = if (hasAlert) ProfileOrange else HomeTextMuted, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.home_rappels_importants), color = HomeTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (!hasAlert) {
                    Text(text = stringResource(R.string.home_no_alerts), color = HomeTextMuted, fontSize = 11.sp)
                } else {
                    if (factures > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ProfileOrange))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.home_overdue_invoices, factures), color = HomeTextDark, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    if (commandes > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ProfileOrange))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.home_commande_fournisseur_attente, commandes), color = HomeTextDark, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    if (ruptures > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Red40))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.home_produits_rupture, ruptures), color = HomeTextDark, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    if (nc > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Red40))
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.home_nc_ouvertes_detail, nc), color = HomeTextDark, fontSize = 11.sp)
                        }
                    }
                }
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AccueilTachesCard(state: HomeUiState, onNavigate: (String) -> Unit) {
    val taches = state.taches.take(3)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(26.dp), shape = RoundedCornerShape(7.dp), color = HomeBlueSoft) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.Checklist, contentDescription = null, tint = HomeBlue, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.home_taches_du_jour), color = HomeTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            if (taches.isEmpty()) {
                Text(text = stringResource(R.string.home_no_tasks), color = HomeTextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                taches.forEachIndexed { idx, t ->
                    AccueilTacheRow(titre = t.titre)
                    if (idx < taches.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.TASKS) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.home_voir_toutes_taches), color = HomeBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = HomeBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AccueilTacheRow(titre: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = titre, color = HomeTextDark, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, HomeBorder),
        ) {}
    }
}



private fun AppModule.createRoute(direction: OperationDirection? = null): String =
    "$route?create=true" + direction?.let { "&direction=${it.name}" }.orEmpty()

private fun String.appModuleRoute(): String = when (this) {
    OperationModule.STOCK.name -> AppModule.STOCK.route
    OperationModule.DEVIS.name, OperationModule.COMMANDE.name -> Routes.DEVIS_COMMANDE
    OperationModule.VENTE.name -> AppModule.VENTE.route
    OperationModule.ACHATS.name -> AppModule.ACHATS.route
    OperationModule.FINANCES.name -> AppModule.FINANCES.route
    OperationModule.LIVRAISON.name -> AppModule.LIVRAISON.route
    OperationModule.PRODUCTION.name -> AppModule.PRODUCTION.route
    OperationModule.SERVICES.name -> AppModule.SERVICES.route
    OperationModule.RH.name -> AppModule.RH.route
    OperationModule.PROJETS.name -> AppModule.PROJETS.route
    else -> Routes.HOME
}

@Composable
internal fun MissaBusinessDrawer(
    companyName: String,
    logoUri: String?,
    backupStatus: String,
    currentRoute: String?,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onSupport: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = Color.White,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Le vrai logo de la marque, et non une icône générique : le
                // tiroir est le seul endroit où l'application se présente, la
                // barre du haut appartenant désormais à l'entreprise.
                MissaBrandMark(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "MISSA BUSINESS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeTextDark,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "360",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeBlue,
                        )
                    }
                    // La version remplace le badge « Actif », déjà porté par la
                    // carte entreprise juste en dessous : c'est le premier
                    // renseignement que demande l'assistance.
                    Text(
                        text = stringResource(R.string.home_version_format, BuildConfig.VERSION_NAME),
                        fontSize = 11.sp,
                        color = HomeTextMuted,
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.home_close),
                        tint = HomeTextMuted,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Routes.ADMIN_REGLAGES) },
                shape = RoundedCornerShape(16.dp),
                color = Blue90,
                border = BorderStroke(1.dp, HomeBorder),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Le conteneur et le contenu partagent la même taille :
                    // 42 dp d'un côté et 30 de l'autre décentraient la vignette.
                    CompanyLogo(
                        logoUri = logoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Store,
                        modifier = Modifier.size(42.dp),
                        size = 42.dp,
                        shape = CircleShape,
                        fallbackTint = HomeBlue,
                        fallbackBackground = Color.White,
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = companyName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.home_company_active),
                            fontSize = 11.sp,
                            color = HomeTextMuted,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = HomeTextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }


            DrawerSectionTitle(stringResource(R.string.drawer_section_administration))
            DrawerMenuItem(Icons.Outlined.Settings, stringResource(R.string.home_settings), currentRoute == Routes.ADMIN_REGLAGES) {
                onNavigate(Routes.ADMIN_REGLAGES)
            }
            DrawerMenuItem(Icons.Outlined.Security, stringResource(R.string.home_licence_activation), currentRoute == Routes.ADMIN_LICENCE) {
                onNavigate(Routes.ADMIN_LICENCE)
            }
            DrawerMenuItem(Icons.Outlined.People, stringResource(R.string.admin_utilisateurs), currentRoute == Routes.ADMIN_UTILISATEURS) {
                onNavigate(Routes.ADMIN_UTILISATEURS)
            }
            DrawerMenuItem(Icons.Outlined.Store, stringResource(R.string.home_sites_sales), currentRoute == Routes.ADMIN_MULTISITE) {
                onNavigate(Routes.ADMIN_MULTISITE)
            }
            DrawerMenuItem(Icons.Outlined.Payments, stringResource(R.string.refer_title), currentRoute == Routes.ADMIN_REFERENTIELS) {
                onNavigate(Routes.ADMIN_REFERENTIELS)
            }

            DrawerSectionTitle(stringResource(R.string.home_drawer_tools))
            DrawerMenuItem(Icons.Outlined.Backup, stringResource(R.string.admin_sauvegarde), currentRoute == Routes.ADMIN_SAUVEGARDE) {
                onNavigate(Routes.ADMIN_SAUVEGARDE)
            }
            DrawerMenuItem(Icons.Outlined.History, stringResource(R.string.admin_journal), currentRoute == Routes.ADMIN_JOURNAL) {
                onNavigate(Routes.ADMIN_JOURNAL)
            }
            DrawerMenuItem(Icons.Outlined.Notifications, stringResource(R.string.notifications), currentRoute == Routes.NOTIFICATIONS) {
                onNavigate(Routes.NOTIFICATIONS)
            }
            DrawerMenuItem(Icons.Outlined.Checklist, stringResource(R.string.tasks_title), currentRoute == Routes.TASKS) {
                onNavigate(Routes.TASKS)
            }

            DrawerSectionTitle(stringResource(R.string.home_drawer_support))
            DrawerMenuItem(Icons.AutoMirrored.Outlined.HelpOutline, stringResource(R.string.home_help_assistance)) {
                onSupport()
            }
            DrawerMenuItem(Icons.Outlined.Info, stringResource(R.string.admin_a_propos), currentRoute == Routes.ADMIN_A_PROPOS) {
                onNavigate(Routes.ADMIN_A_PROPOS)
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = HomeBackground,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = HomeGreenSoft,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = TendrePositive,
                                modifier = Modifier.padding(7.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_data_secured),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeTextDark,
                            )
                            Text(
                                text = backupStatus,
                                fontSize = 10.5.sp,
                                color = HomeTextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        color = HomeTextMuted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 10.dp, top = 18.dp, bottom = 5.dp),
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) HomeBlueSoft else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (selected) Color.White else Color.Transparent,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) HomeBlue else HomeTextMuted,
                    modifier = Modifier.padding(7.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = if (selected) HomeBlue else HomeTextDark,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(22.dp)
                        .background(HomeBlue, RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

@StringRes
private fun String?.profileLabel(): Int? = when (this) {
    "AV" -> R.string.profil_av
    "ASV" -> R.string.profil_asv
    "APSV" -> R.string.profil_apsv
    "SER" -> R.string.profil_ser
    "PRJ" -> R.string.profil_prj
    "FULL" -> R.string.profil_full
    "CUSTOM" -> R.string.profil_custom
    else -> null
}

@StringRes
private fun String?.sizeLabel(): Int? = when (this) {
    "P1" -> R.string.palier_p1
    "P2" -> R.string.palier_p2
    "P3" -> R.string.palier_p3
    "P4" -> R.string.palier_p4
    "P5" -> R.string.palier_p5
    "P6" -> R.string.palier_p6
    else -> null
}

@Composable
internal fun HomeSupportDialogue(entrepriseNom: String, onFermer: () -> Unit) {
    val contexte = LocalContext.current
    val numero = stringResource(R.string.contact_commercial_whatsapp)
    val telegram = stringResource(R.string.contact_commercial_telegram)
    val adresse = stringResource(R.string.contact_commercial_email)
    val whatsappOk = !ContactCommercial.estTemoin(numero)
    val telegramOk = !ContactCommercial.estTemoin(telegram)
    val emailOk = !ContactCommercial.estTemoin(adresse)
    val aucunCanal = !whatsappOk && !telegramOk && !emailOk
    val objet = stringResource(R.string.home_support_objet)
    val message = stringResource(
        R.string.home_support_corps,
        entrepriseNom.ifBlank { stringResource(R.string.home_company_placeholder) },
        BuildConfig.VERSION_NAME,
    )
    val echec = stringResource(R.string.obn_code_indispo)

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.home_support_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = stringResource(
                        if (aucunCanal) R.string.obn_code_a_configurer else R.string.home_support_intro,
                    ),
                    fontSize = 12.5.sp,
                    color = HomeTextMuted,
                )
                if (whatsappOk) {
                    HomeSupportBouton(R.string.obn_code_whatsapp, Icons.AutoMirrored.Outlined.Chat) {
                        if (!ContactCommercial.ouvrirWhatsApp(contexte, numero, message)) {
                            Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                        }
                        onFermer()
                    }
                }
                if (telegramOk) {
                    HomeSupportBouton(
                        R.string.obn_code_telegram,
                        Icons.AutoMirrored.Outlined.Send,
                    ) {
                        if (!ContactCommercial.ouvrirTelegram(contexte, telegram, message)) {
                            Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                        }
                        onFermer()
                    }
                }
                if (emailOk) {
                    HomeSupportBouton(R.string.obn_code_email, Icons.Outlined.MailOutline) {
                        if (!ContactCommercial.ouvrirEmail(contexte, adresse, objet, message)) {
                            Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                        }
                        onFermer()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.home_close)) }
        },
    )
}

@Composable
private fun HomeSupportBouton(
    texteRes: Int,
    icone: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, HomeBlue),
    ) {
        Icon(icone, contentDescription = null, tint = HomeBlue, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(texteRes), fontSize = 13.sp, color = HomeBlue)
    }
}

/**
 * Personnalisation des actions rapides uniquement.
 *
 * Le menu « Personnaliser » au-dessus des actions rapides ne doit contenir
 * que les 8 actions rapides (VENTE, ACHAT, CLIENT, FOURNISSEUR, ENTREE_STOCK,
 * TRANSFERT_STOCK, PAIEMENT_RECU, DEPENSE). Cocher/décocher => apparition/disparition immédiate.
 * La barre du bas n'est pas configurable ici.
 */
@Composable
private fun HomePersonnaliserDialogue(
    actionsSelection: List<String>,
    onFermer: () -> Unit,
    onValider: (List<String>) -> Unit,
) {
    // Si actionsSelection vide (usine = tout afficher), on pré-coche tout pour que l'utilisateur voie l'état effectif.
    val defs = rememberAccueilActionDefs()
    val choixActions = remember {
        mutableStateListOf<String>().apply {
            if (actionsSelection.isEmpty()) addAll(AccueilActionKeys.ALL) else addAll(actionsSelection)
        }
    }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.home_quick_actions)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_personalize_actions_aide),
                    fontSize = 11.sp,
                    color = HomeTextMuted,
                )
                defs.forEach { def ->
                    val coche = def.key in choixActions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (coche) choixActions.remove(def.key) else choixActions.add(def.key)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = coche,
                            onCheckedChange = {
                                if (coche) choixActions.remove(def.key) else choixActions.add(def.key)
                            },
                        )
                        Spacer(Modifier.width(4.dp))
                        Surface(modifier = Modifier.size(28.dp), shape = RoundedCornerShape(7.dp), color = def.bg) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(def.icon, contentDescription = null, tint = def.tint, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(def.labelRes),
                            fontSize = 13.sp,
                            color = HomeTextDark,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Si toutes les actions sont cochées, on enregistre vide = usine (tout afficher).
                val actionsAEnregistrer = if (choixActions.size == AccueilActionKeys.ALL.size) emptyList() else choixActions.toList()
                onValider(actionsAEnregistrer)
            }) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onValider(emptyList()) }) {
                Text(stringResource(R.string.home_personalize_defaut))
            }
        },
    )
}
