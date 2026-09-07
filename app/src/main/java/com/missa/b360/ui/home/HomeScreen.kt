package com.missa.b360.ui.home

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Badge as NotificationBadge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaBarreModules
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.operations.ReportingViewModel
import com.missa.b360.ui.operations.detailAlerte
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40
import kotlinx.coroutines.launch

/* Palette du tableau de bord mobile. */
private val HomeBlue = BrandBlue
private val HomeBlueSoft = MissaSoftBlue
private val HomeGreen = Green60
private val HomeGreenSoft = Color(0xFFEAF8EF)
private val HomeOrange = Color(0xFFF28A16)
private val HomeOrangeSoft = Color(0xFFFFF1DF)
private val HomePurple = Color(0xFF7047E8)
private val HomePurpleSoft = Color(0xFFF1ECFF)
private val HomeTeal = Color(0xFF00A5A5)
private val HomeTextDark = MissaInk
private val HomeTextMuted = MissaMuted
private val HomeBackground = MissaCanvas
private val HomeBorder = MissaBorder

/**
 * Accueil mobile : tableau de bord sans données de démonstration. Les métriques sont
 * recalculées à partir des pièces opérationnelles réellement validées.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showMoreModules by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    val nonLues by viewModel.notificationsNonLues.collectAsState(initial = 0)
    val uiState by viewModel.uiState.collectAsState()
    val modulesActifs by viewModel.modulesActifs.collectAsState()
    val modulesEpingles by viewModel.modulesEpingles.collectAsState()
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
    val backupStatus = uiState.derniereSauvegarde?.let { date ->
        stringResource(R.string.home_backup_date, DateUtils.formatDateHeure(date))
    } ?: stringResource(R.string.home_backup_never)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MissaBusinessDrawer(
                companyName = companyName,
                logoUri = uiState.entrepriseLogoUri,
                backupStatus = backupStatus,
                currentRoute = currentRoute,
                onClose = { scope.launch { drawerState.close() } },
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route)
                },
                onSupport = {
                    scope.launch { drawerState.close() }
                    showSupport = true
                },
            )
        },
    ) {
        Scaffold(
            containerColor = HomeBackground,
            topBar = {
                HomeHeader(
                    companyName = companyName,
                    companyLogoUri = uiState.entrepriseLogoUri,
                    profileLabel = profileLabel,
                    sizeLabel = sizeLabel,
                    greeting = greeting,
                    notificationCount = nonLues,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    // Le bloc porte le logo et le nom de l'entreprise : il ouvre
                    // sa fiche. Le compte utilisateur a son entrée au tiroir.
                    onProfileClick = { navController.navigate(Routes.ADMIN_REGLAGES) },
                )
            },
            bottomBar = {
                MissaBarreModules(
                    modules = AppModule.barreBas(modulesActifs, modulesEpingles),
                    routeCourante = currentRoute,
                    onAccueil = { navController.naviguerOnglet(Routes.HOME) },
                    onModule = { navController.naviguerOnglet(it.route) },
                    onPlus = { showMoreModules = true },
                )
            },
        ) { padding ->
            HomeDashboard(
                state = uiState,
                modulesActifs = modulesActifs,
                onPersonnaliser = { showPersonnaliser = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onNavigate = { navController.navigate(it) },
            )
        }
    }

    if (showMoreModules) {
        ModalBottomSheet(
            onDismissRequest = { showMoreModules = false },
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.more_modules),
                    color = HomeTextDark,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_more_modules_description),
                    color = HomeTextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                AppModule.secondaires(modulesActifs).forEach { module ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(module.titleRes),
                                color = HomeTextDark,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = HomeBlueSoft,
                            ) {
                                Icon(
                                    imageVector = module.icon,
                                    contentDescription = null,
                                    tint = HomeBlue,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = HomeTextMuted,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        modifier = Modifier.clickable {
                            showMoreModules = false
                            navController.naviguerOnglet(module.route)
                        },
                    )
                }
            }
        }
    }

    if (showPersonnaliser) {
        HomePersonnaliserDialogue(
            disponibles = AppModule.epinglables(modulesActifs),
            selection = AppModule.barreBas(modulesActifs, modulesEpingles).map { it.name },
            onFermer = { showPersonnaliser = false },
            onValider = { choix ->
                viewModel.epinglerModules(choix)
                showPersonnaliser = false
            },
        )
    }

    if (showSupport) {
        HomeSupportDialogue(
            entrepriseNom = uiState.entrepriseNom,
            onFermer = { showSupport = false },
        )
    }
}

@Composable
private fun HomeHeader(
    companyName: String,
    companyLogoUri: String?,
    profileLabel: String,
    sizeLabel: String,
    greeting: String,
    notificationCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
        ) {
            // Une seule identité dans la barre : celle de l'entreprise. La marque
            // Missa Business 360 tient l'en-tête du tiroir — deux blocs
            // identitaires côte à côte se disputaient l'attention et
            // écrasaient les libellés jusqu'à 7 sp.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.drawer_admin),
                        tint = HomeTextDark,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(onClick = onProfileClick)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompanyLogo(
                        logoUri = companyLogoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Business,
                        modifier = Modifier.size(42.dp),
                        size = 42.dp,
                        shape = RoundedCornerShape(12.dp),
                        fallbackTint = HomeBlue,
                        fallbackBackground = HomeBlueSoft,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = companyName,
                            color = HomeTextDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$profileLabel · $sizeLabel",
                            color = HomeTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(42.dp)) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                NotificationBadge(containerColor = Color(0xFFEF4444)) {
                                    Text(notificationCount.coerceAtMost(99).toString(), fontSize = 8.sp)
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = HomeTextDark,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))
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
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick),
                shape = RoundedCornerShape(15.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HomeBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = RoundedCornerShape(7.dp),
                        color = HomeBlueSoft,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Business,
                            contentDescription = null,
                            tint = HomeBlue,
                            modifier = Modifier.padding(5.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_profile_format, profileLabel),
                        color = HomeTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "  •  ",
                        color = HomeTextMuted,
                        fontSize = 10.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_size_format, sizeLabel),
                        color = HomeTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = HomeTextDark,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    state: HomeUiState,
    modulesActifs: List<ModuleCode>,
    onPersonnaliser: () -> Unit,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
) {
    val currency = state.devise
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            // Quatre chiffres, quatre modules, aucun répété plus bas : les
            // ventes, les achats, ce qu'il reste entre les deux, et l'argent
            // réellement disponible.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_vente),
                    value = MoneyUtils.format(state.ventes, currency),
                    subtitle = stringResource(R.string.home_today),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconColor = HomeBlue,
                    iconBackground = HomeBlueSoft,
                    onClick = { onNavigate(AppModule.VENTE.route) },
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_achats),
                    value = MoneyUtils.format(state.achats, currency),
                    subtitle = stringResource(R.string.home_today),
                    icon = Icons.Outlined.AddShoppingCart,
                    iconColor = HomeGreen,
                    iconBackground = HomeGreenSoft,
                    onClick = { onNavigate(AppModule.ACHATS.route) },
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_gross_margin),
                    value = MoneyUtils.format(state.marge, currency),
                    subtitle = stringResource(R.string.home_today),
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    iconColor = HomePurple,
                    iconBackground = HomePurpleSoft,
                    onClick = { onNavigate(AppModule.REPORTING.route) },
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_cash),
                    value = MoneyUtils.format(state.tresorerie, currency),
                    subtitle = stringResource(R.string.home_available),
                    icon = Icons.Outlined.Payments,
                    iconColor = HomeOrange,
                    iconBackground = HomeOrangeSoft,
                    onClick = { onNavigate(AppModule.TRESORERIE.route) },
                )
            }
        }
        item {
            DashboardSectionHeader(
                title = stringResource(R.string.home_quick_actions),
                action = stringResource(R.string.home_personalize),
                onAction = onPersonnaliser,
            )
            Spacer(Modifier.height(7.dp))
            QuickActionsGrid(modulesActifs = modulesActifs, onNavigate = onNavigate)
        }
        item {
            HomeAlertesCard(onVoirTout = { onNavigate(AppModule.REPORTING.route) })
        }
        item {
            DashboardSectionHeader(
                title = stringResource(R.string.home_recent_documents),
                action = stringResource(R.string.home_see_all),
                onAction = { onNavigate(AppModule.REPORTING.route) },
            )
            Spacer(Modifier.height(7.dp))
            RecentDocuments(
                records = state.recentOperations,
                devise = currency,
                onClick = { record -> onNavigate(record.module.appModuleRoute()) },
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        // Une carte qui nomme un module doit y conduire : l'utilisateur la
        // touche de toute façon.
        modifier = modifier
            .height(112.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(11.dp),
                color = iconBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = HomeTextDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = HomeTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = HomeTextMuted,
                fontSize = 10.5.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    action: String,
    onAction: (() -> Unit)? = null,
    actionHasDropDown: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = HomeTextDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = if (onAction == null) Modifier else Modifier.clickable(onClick = onAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = action,
                color = HomeBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (actionHasDropDown) {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = HomeBlue,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private data class QuickAction(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    /** Module dont dépend le raccourci : il disparaît si le pack ne l'a pas retenu. */
    val module: ModuleCode,
)

/** Ouvre le formulaire en plus de la liste depuis une action rapide de l'accueil. */
private fun AppModule.createRoute(direction: OperationDirection? = null): String =
    "$route?create=true" + direction?.let { "&direction=${it.name}" }.orEmpty()

@Composable
private fun QuickActionsGrid(
    modulesActifs: List<ModuleCode>,
    onNavigate: (String) -> Unit,
) {
    // Un raccourci vers un module que le pack n'a pas retenu mène à un écran
    // que l'utilisateur n'a pas demandé : la grille suit donc la même règle que
    // le menu. L'argent passe par la Trésorerie, seul endroit où un encaissement
    // se rattache à un compte.
    val actions = listOf(
        QuickAction(
            R.string.home_new_sale,
            Icons.AutoMirrored.Outlined.ReceiptLong,
            HomeBlue,
            AppModule.VENTE.createRoute(),
            ModuleCode.VEN,
        ),
        QuickAction(
            R.string.home_new_purchase,
            Icons.Outlined.AddShoppingCart,
            HomeGreen,
            AppModule.ACHATS.createRoute(),
            ModuleCode.ACH,
        ),
        QuickAction(
            R.string.home_new_client,
            Icons.Outlined.PersonAdd,
            HomePurple,
            AppModule.CLIENTS.createRoute(),
            ModuleCode.VEN,
        ),
        QuickAction(
            R.string.home_new_supplier,
            Icons.Outlined.AddBusiness,
            HomeOrange,
            AppModule.FOURNISSEURS.createRoute(),
            ModuleCode.ACH,
        ),
        QuickAction(
            R.string.home_stock_entry,
            Icons.Outlined.Inventory2,
            HomeTeal,
            AppModule.STOCK.createRoute(),
            ModuleCode.STK,
        ),
        QuickAction(
            R.string.home_transfer,
            Icons.Outlined.TransferWithinAStation,
            HomeBlue,
            Routes.STOCK_TRANSFER_FORM,
            ModuleCode.STK,
        ),
        QuickAction(
            R.string.home_payment_received,
            Icons.Outlined.Payments,
            HomeGreen,
            AppModule.TRESORERIE.route,
            ModuleCode.TRE,
        ),
        QuickAction(
            R.string.home_new_delivery,
            Icons.Outlined.LocalShipping,
            HomeOrange,
            AppModule.LIVRAISON.createRoute(),
            ModuleCode.LOG,
        ),
    ).filter { modulesActifs.isEmpty() || it.module in modulesActifs }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { action ->
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(action.titleRes),
                        icon = action.icon,
                        color = action.color,
                        onClick = { onNavigate(action.route) },
                    )
                }
                // Une rangée incomplète garde ses cases à la même largeur que
                // les autres plutôt que de s'étirer.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(31.dp),
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                color = HomeTextDark,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentDocuments(
    records: List<OperationRecordEntity>,
    devise: String,
    onClick: (OperationRecordEntity) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        if (records.isEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(43.dp),
                    shape = CircleShape,
                    color = HomeBlueSoft,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = HomeBlue,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_no_documents),
                        color = HomeTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_no_documents_description),
                        color = HomeTextMuted,
                        fontSize = 10.sp,
                        maxLines = 2,
                    )
                }
            }
        } else {
            Column {
                records.forEachIndexed { index, record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(record) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = HomeBlueSoft,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = HomeBlue,
                                modifier = Modifier.padding(9.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.title,
                                color = HomeTextDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_recent_record_detail,
                                    record.reference,
                                    DateUtils.formatDateHeure(record.createdAt),
                                ),
                                color = HomeTextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        record.amount?.let { amount ->
                            Text(
                                text = MoneyUtils.format(amount, devise),
                                color = HomeBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                    if (index < records.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = HomeBorder)
                    }
                }
            }
        }
    }
}

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

private fun Double.displayQuantity(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

@Composable
private fun MissaBusinessDrawer(
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
                color = Color(0xFFF0F4FF),
                border = BorderStroke(1.dp, Color(0xFFE0E7FA)),
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
                color = Color(0xFFF7F8FC),
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
                                tint = Color(0xFF16A34A),
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
        color = Color(0xFF8A94AA),
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
                color = if (selected) HomeBlue else Color(0xFF17213F),
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

/**
 * Alertes du tableau de bord ramenées sur l'accueil : le reporting vient à
 * l'utilisateur au lieu d'attendre qu'il ouvre un écran. Seules les alertes des
 * modules actifs sont retenues ; la carte disparaît quand tout est à jour.
 */
@Composable
private fun HomeAlertesCard(
    onVoirTout: () -> Unit,
    viewModel: ReportingViewModel = hiltViewModel(),
) {
    val tableau by viewModel.tableau.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val modules by viewModel.modulesActifs.collectAsState()
    val alertes = tableau.alertes.filter { it.code.module in modules }.take(3)
    if (alertes.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, Red40.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onVoirTout),
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Red40,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.kpi_titre_alertes),
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.home_see_all),
                    color = BrandBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
            for (alerte in alertes) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Red40),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(alerte.code.libelleRes),
                            color = MissaInk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                        )
                        Text(
                            text = detailAlerte(alerte, devise),
                            color = MissaMuted,
                            fontSize = 10.5.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigation depuis la barre du bas.
 *
 * Sans ces deux garde-fous, chaque appui empile un écran de plus : cinq appuis
 * sur « Stock » obligeaient à revenir cinq fois en arrière pour retrouver
 * l'accueil. `launchSingleTop` évite le doublon en sommet de pile, et le
 * `popUpTo` ramène l'accueil comme unique racine.
 */
private fun NavController.naviguerOnglet(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Assistance : trois canaux réels plutôt qu'un message d'attente.
 *
 * Les coordonnées viennent de `res/values/contact.xml`, comme pour l'achat du
 * code d'activation — un seul endroit à tenir à jour. Un canal non configuré
 * disparaît au lieu d'ouvrir une conversation vide.
 */
@Composable
private fun HomeSupportDialogue(entrepriseNom: String, onFermer: () -> Unit) {
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
                    HomeSupportBouton(R.string.obn_code_whatsapp, Icons.Outlined.Chat) {
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
 * Choix des modules épinglés dans la barre du bas (RA-22).
 *
 * C'est ce que promet le lien « Personnaliser » posé au-dessus des actions
 * rapides : il n'a rien à voir avec les réglages de l'entreprise, où il menait
 * jusqu'ici. Trois onglets au maximum, l'accueil et « Plus » occupant déjà deux
 * places sur cinq.
 */
@Composable
private fun HomePersonnaliserDialogue(
    disponibles: List<AppModule>,
    selection: List<String>,
    onFermer: () -> Unit,
    onValider: (List<String>) -> Unit,
) {
    val choix = remember { mutableStateListOf<String>().apply { addAll(selection) } }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.home_personalize)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_personalize_aide, AppModule.MAX_ONGLETS),
                    fontSize = 12.sp,
                    color = HomeTextMuted,
                )
                Spacer(Modifier.height(6.dp))
                disponibles.forEach { module ->
                    val coche = module.name in choix
                    // Au-delà de la limite, les cases non cochées se figent :
                    // mieux vaut un choix impossible visible qu'un enregistrement
                    // silencieusement tronqué.
                    val autorise = coche || choix.size < AppModule.MAX_ONGLETS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = autorise) {
                                if (coche) choix.remove(module.name) else choix.add(module.name)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = coche,
                            onCheckedChange = {
                                if (coche) choix.remove(module.name) else choix.add(module.name)
                            },
                            enabled = autorise,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = module.icon,
                            contentDescription = null,
                            tint = if (autorise) HomeBlue else HomeTextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(module.titleRes),
                            fontSize = 13.sp,
                            color = if (autorise) HomeTextDark else HomeTextMuted,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(choix.toList()) }) {
                Text(stringResource(R.string.ops_save))
            }
        },
        dismissButton = {
            // Vider la sélection rétablit la disposition d'usine.
            TextButton(onClick = { onValider(emptyList()) }) {
                Text(stringResource(R.string.home_personalize_defaut))
            }
        },
    )
}
