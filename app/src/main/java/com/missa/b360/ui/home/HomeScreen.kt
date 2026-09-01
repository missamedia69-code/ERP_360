package com.missa.b360.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge as NotificationBadge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.missa.b360.BuildConfig
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.theme.Blue40
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import kotlinx.coroutines.launch

/* Palette du tableau de bord mobile. */
private val HomeBlue = BrandBlue
private val HomeBlueDark = Blue40
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
                    profileLabel = profileLabel,
                    sizeLabel = sizeLabel,
                    greeting = greeting,
                    notificationCount = nonLues,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { navController.navigate(Routes.ADMIN_REGLAGES) },
                )
            },
            bottomBar = {
                HomeBottomBar(
                    currentRoute = currentRoute,
                    onModuleClick = { navController.navigate(it.route) },
                    onMore = { showMoreModules = true },
                )
            },
        ) { padding ->
            HomeDashboard(
                state = uiState,
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
                AppModule.modulesSecondaires().forEach { module ->
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
                                imageVector = Icons.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = HomeTextMuted,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        modifier = Modifier.clickable {
                            showMoreModules = false
                            navController.navigate(module.route)
                        },
                    )
                }
            }
        }
    }

    if (showSupport) {
        AlertDialog(
            onDismissRequest = { showSupport = false },
            title = { Text(stringResource(R.string.home_support_title)) },
            text = { Text(stringResource(R.string.home_support_message)) },
            confirmButton = {
                TextButton(onClick = { showSupport = false }) {
                    Text(stringResource(R.string.home_close))
                }
            },
        )
    }
}

@Composable
private fun HomeHeader(
    companyName: String,
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
        color = HomeBlueDark,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.drawer_admin),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(7.dp))
                MissaBrandMark(size = 36.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MISSA BUSINESS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Text(
                        text = "360",
                        color = Color(0xFFB6E52B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(
                    modifier = Modifier.widthIn(max = 118.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = companyName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = profileLabel,
                        color = Color(0xFFD5DEFF),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(42.dp)) {
                    BadgedBox(
                        badge = {
                            if (notificationCount > 0) {
                                NotificationBadge(containerColor = Color(0xFF9AD72B)) {
                                    Text(notificationCount.coerceAtMost(99).toString())
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = Color.White,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = greeting,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.home_overview),
                color = Color(0xFFDCE5FF),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(15.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_profile_format, profileLabel),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.home_size_format, sizeLabel),
                            color = Color(0xFFD9E2FF),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_vente),
                    value = MoneyUtils.format(state.ventes, currency),
                    subtitle = stringResource(R.string.home_today),
                    icon = Icons.Outlined.ReceiptLong,
                    iconColor = HomeBlue,
                    iconBackground = HomeBlueSoft,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_fournisseurs),
                    value = state.nombreFournisseurs.toString(),
                    subtitle = stringResource(R.string.home_total),
                    icon = Icons.Outlined.AddBusiness,
                    iconColor = HomeGreen,
                    iconBackground = HomeGreenSoft,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_cash),
                    value = MoneyUtils.format(state.tresorerie, currency),
                    subtitle = stringResource(R.string.home_available),
                    icon = Icons.Outlined.Payments,
                    iconColor = HomeOrange,
                    iconBackground = HomeOrangeSoft,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_clients),
                    value = state.nombreClients.toString(),
                    subtitle = stringResource(R.string.home_total),
                    icon = Icons.Outlined.Groups,
                    iconColor = HomePurple,
                    iconBackground = HomePurpleSoft,
                )
            }
        }
        item {
            DashboardSectionHeader(
                title = stringResource(R.string.home_quick_actions),
                action = stringResource(R.string.home_personalize),
                onAction = { onNavigate(Routes.ADMIN_REGLAGES) },
            )
            Spacer(Modifier.height(7.dp))
            QuickActionsGrid(onNavigate = onNavigate)
        }
        item {
            DashboardSectionHeader(
                title = stringResource(R.string.home_activity_summary),
                action = stringResource(R.string.home_today),
            )
            Spacer(Modifier.height(7.dp))
            ActivitySummary(
                currency = currency,
                sales = state.ventes,
                purchases = state.achats,
                stockQuantity = state.quantiteStock,
            )
        }
        item {
            ReminderCard(onClick = { onNavigate(Routes.NOTIFICATIONS) })
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
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = iconBackground,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = HomeTextMuted,
                fontSize = 9.sp,
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
        Text(
            text = action,
            color = HomeBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = if (onAction == null) Modifier else Modifier.clickable(onClick = onAction),
        )
    }
}

private data class QuickAction(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val color: Color,
    val route: String,
)

/** Ouvre le formulaire en plus de la liste depuis une action rapide de l'accueil. */
private fun AppModule.createRoute(direction: OperationDirection? = null): String =
    "$route?create=true" + direction?.let { "&direction=${it.name}" }.orEmpty()

@Composable
private fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    val rows = listOf(
        listOf(
            QuickAction(R.string.home_new_sale, Icons.Outlined.ReceiptLong, HomeBlue, AppModule.VENTE.createRoute()),
            QuickAction(R.string.home_new_purchase, Icons.Outlined.AddShoppingCart, HomeGreen, AppModule.ACHATS.createRoute()),
            QuickAction(R.string.home_new_client, Icons.Outlined.PersonAdd, HomePurple, AppModule.CLIENTS.createRoute()),
            QuickAction(R.string.home_new_supplier, Icons.Outlined.AddBusiness, HomeOrange, AppModule.FOURNISSEURS.createRoute()),
        ),
        listOf(
            QuickAction(R.string.home_stock_entry, Icons.Outlined.Inventory2, HomeTeal, AppModule.STOCK.createRoute()),
            QuickAction(R.string.home_transfer, Icons.Outlined.TransferWithinAStation, HomeBlue, AppModule.STOCK.createRoute()),
            QuickAction(
                R.string.home_payment_received,
                Icons.Outlined.Payments,
                HomeGreen,
                AppModule.FINANCES.createRoute(OperationDirection.IN),
            ),
            QuickAction(
                R.string.home_expense,
                Icons.Outlined.Description,
                HomeOrange,
                AppModule.FINANCES.createRoute(OperationDirection.OUT),
            ),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
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
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                color = HomeTextDark,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivitySummary(
    currency: String,
    sales: Double,
    purchases: Double,
    stockQuantity: Double,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
        ) {
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.module_vente),
                value = MoneyUtils.format(sales, currency),
                icon = Icons.Outlined.BarChart,
                color = HomeBlue,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.module_achats),
                value = MoneyUtils.format(purchases, currency),
                icon = Icons.Outlined.ShoppingCart,
                color = HomeGreen,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.module_stock),
                value = stockQuantity.displayQuantity(),
                icon = Icons.Outlined.Inventory2,
                color = HomeOrange,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_gross_margin),
                value = MoneyUtils.format(sales - purchases, currency),
                icon = Icons.Outlined.Payments,
                color = HomePurple,
            )
        }
    }
}

@Composable
private fun SummaryItem(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(text = title, color = HomeTextMuted, fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = HomeTextDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(60.dp)
            .background(HomeBorder),
    )
}

@Composable
private fun ReminderCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBF3),
        border = BorderStroke(1.dp, Color(0xFFF5DDA8)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(0xFFFFB52E),
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_reminders),
                    color = HomeTextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_no_pending_tasks),
                    color = HomeTextDark,
                    fontSize = 10.sp,
                )
                Text(
                    text = stringResource(R.string.home_no_overdue_invoices),
                    color = HomeTextDark,
                    fontSize = 10.sp,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = HomeTextDark,
                modifier = Modifier.size(15.dp),
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
private fun HomeBottomBar(
    currentRoute: String?,
    onModuleClick: (AppModule) -> Unit,
    onMore: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            AppModule.modulesBarreBas().take(4).forEach { module ->
                NavigationBarItem(
                    selected = currentRoute == module.route,
                    onClick = { onModuleClick(module) },
                    icon = { Icon(module.icon, contentDescription = null) },
                    label = { Text(stringResource(module.titleRes), fontSize = 10.sp) },
                )
            }
            NavigationBarItem(
                selected = false,
                onClick = onMore,
                icon = {
                    Surface(
                        modifier = Modifier.size(31.dp),
                        shape = CircleShape,
                        color = HomeBlueSoft,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = stringResource(R.string.more_modules),
                            tint = HomeBlue,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(R.string.home_more_short),
                        color = HomeBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

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
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = HomeBlue,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MISSA BUSINESS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HomeTextDark,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "360",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeBlue,
                        )
                        Spacer(Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = HomeGreenSoft,
                        ) {
                            Text(
                                text = stringResource(R.string.home_active),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
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
                    CompanyLogo(
                        logoUri = logoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Store,
                        modifier = Modifier.size(42.dp),
                        size = 30.dp,
                        shape = CircleShape,
                        fallbackTint = HomeBlue,
                        fallbackBackground = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = companyName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.home_company_active),
                            fontSize = 10.sp,
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

            DrawerSectionTitle(stringResource(R.string.home_drawer_primary))
            DrawerMenuItem(
                icon = Icons.Outlined.Home,
                title = stringResource(R.string.home_title),
                selected = currentRoute == Routes.HOME,
            ) {
                onNavigate(Routes.HOME)
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

            DrawerSectionTitle(stringResource(R.string.home_drawer_support))
            DrawerMenuItem(Icons.Outlined.HelpOutline, stringResource(R.string.home_help_assistance)) {
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeTextDark,
                            )
                            Text(
                                text = backupStatus,
                                fontSize = 9.sp,
                                color = HomeTextMuted,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.home_version_format, BuildConfig.VERSION_NAME),
                        fontSize = 9.sp,
                        color = Color(0xFF9AA3B8),
                    )
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
        fontSize = 9.sp,
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
    "A" -> R.string.profil_a
    "B" -> R.string.profil_b
    "C" -> R.string.profil_c
    "D" -> R.string.profil_d
    "E" -> R.string.profil_e
    "F" -> R.string.profil_f
    "G" -> R.string.profil_g
    "H" -> R.string.profil_h
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
