package com.missa.b360.ui.home

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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.missa.b360.R
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import kotlinx.coroutines.launch

/* Palette du tableau de bord mobile. */
private val HomeBlue = Color(0xFF1247E8)
private val HomeBlueDark = Color(0xFF0738B8)
private val HomeBlueSoft = Color(0xFFEFF4FF)
private val HomeGreen = Color(0xFF20A83E)
private val HomeGreenSoft = Color(0xFFEAF8EE)
private val HomeOrange = Color(0xFFFF9418)
private val HomeOrangeSoft = Color(0xFFFFF1DF)
private val HomePurple = Color(0xFF7047E8)
private val HomePurpleSoft = Color(0xFFF1ECFF)
private val HomeTeal = Color(0xFF00A5A5)
private val HomeTextDark = Color(0xFF111B3D)
private val HomeTextMuted = Color(0xFF68738F)
private val HomeBackground = Color(0xFFF7F9FD)
private val HomeBorder = Color(0xFFE4E9F3)

/**
 * Accueil mobile : un tableau de bord utile dès les premiers modules réels, sans données
 * de démonstration. Les métriques de ventes/achats restent à 0 tant que ces modules ne
 * disposent pas encore de transactions à agréger.
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                companyName = companyName,
                logoUri = uiState.entrepriseLogoUri,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route)
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
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 20.dp),
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
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MISSA",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "BUSINESS",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        text = "360",
                        color = Color(0xFFB6E52B),
                        fontSize = 14.sp,
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

            Spacer(Modifier.height(20.dp))
            Text(
                text = greeting,
                color = Color.White,
                fontSize = 25.sp,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.module_vente),
                    value = "0 $currency",
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
                    value = "0 $currency",
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
            ActivitySummary(currency = currency)
        }
        item {
            ReminderCard(onClick = { onNavigate(Routes.NOTIFICATIONS) })
        }
        item {
            DashboardSectionHeader(
                title = stringResource(R.string.home_recent_documents),
                action = stringResource(R.string.home_see_all),
                onAction = { onNavigate(AppModule.VENTE.route) },
            )
            Spacer(Modifier.height(7.dp))
            RecentDocuments(onClick = { onNavigate(AppModule.VENTE.route) })
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

@Composable
private fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    val rows = listOf(
        listOf(
            QuickAction(R.string.home_new_sale, Icons.Outlined.ReceiptLong, HomeBlue, AppModule.VENTE.route),
            QuickAction(R.string.home_new_purchase, Icons.Outlined.AddShoppingCart, HomeGreen, AppModule.ACHATS.route),
            QuickAction(R.string.home_new_client, Icons.Outlined.PersonAdd, HomePurple, AppModule.CLIENTS.route),
            QuickAction(R.string.home_new_supplier, Icons.Outlined.AddBusiness, HomeOrange, AppModule.FOURNISSEURS.route),
        ),
        listOf(
            QuickAction(R.string.home_stock_entry, Icons.Outlined.Inventory2, HomeTeal, AppModule.STOCK.route),
            QuickAction(R.string.home_transfer, Icons.Outlined.TransferWithinAStation, HomeBlue, AppModule.STOCK.route),
            QuickAction(R.string.home_payment_received, Icons.Outlined.Payments, HomeGreen, AppModule.FINANCES.route),
            QuickAction(R.string.home_expense, Icons.Outlined.Description, HomeOrange, AppModule.FINANCES.route),
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
private fun ActivitySummary(currency: String) {
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
                value = "0 $currency",
                icon = Icons.Outlined.BarChart,
                color = HomeBlue,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.module_achats),
                value = "0 $currency",
                icon = Icons.Outlined.ShoppingCart,
                color = HomeGreen,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.module_stock),
                value = "0",
                icon = Icons.Outlined.Inventory2,
                color = HomeOrange,
            )
            SummaryDivider()
            SummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_gross_margin),
                value = "0 $currency",
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
private fun RecentDocuments(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
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
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = HomeTextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

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
private fun AdminDrawerContent(
    companyName: String,
    logoUri: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = Color.White,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 25.dp, bottom = 25.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = HomeBlue,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(
                            text = "MISSA BUSINESS",
                            color = HomeTextDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "360",
                            color = HomeBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Routes.ADMIN_REGLAGES) },
                    shape = RoundedCornerShape(15.dp),
                    color = HomeBlueSoft,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompanyLogo(
                            logoUri = logoUri,
                            contentDescription = null,
                            fallbackIcon = Icons.Outlined.Store,
                            modifier = Modifier.size(40.dp),
                            size = 30.dp,
                            shape = CircleShape,
                            fallbackTint = HomeBlue,
                            fallbackBackground = Color.White,
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = companyName,
                                color = HomeTextDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.home_company_active),
                                color = HomeTextMuted,
                                fontSize = 10.sp,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = HomeTextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                DrawerSection(stringResource(R.string.home_drawer_primary))
                DrawerItem(Icons.Outlined.Store, R.string.home_title, selected = true) {
                    onNavigate(Routes.HOME)
                }
                DrawerSection(stringResource(R.string.drawer_section_administration))
                DrawerItem(Icons.Outlined.Settings, R.string.admin_reglages) {
                    onNavigate(Routes.ADMIN_REGLAGES)
                }
                DrawerItem(Icons.Outlined.CloudSync, R.string.admin_licence) {
                    onNavigate(Routes.ADMIN_LICENCE)
                }
                DrawerItem(Icons.Outlined.Backup, R.string.admin_sauvegarde) {
                    onNavigate(Routes.ADMIN_SAUVEGARDE)
                }
                DrawerItem(Icons.Outlined.History, R.string.admin_journal) {
                    onNavigate(Routes.ADMIN_JOURNAL)
                }
                DrawerItem(Icons.Outlined.Badge, R.string.admin_utilisateurs) {
                    onNavigate(Routes.ADMIN_UTILISATEURS)
                }
                DrawerItem(Icons.Outlined.Store, R.string.admin_multisite) {
                    onNavigate(Routes.ADMIN_MULTISITE)
                }
                DrawerSection(stringResource(R.string.home_drawer_other))
                DrawerItem(Icons.Outlined.Info, R.string.admin_a_propos) {
                    onNavigate(Routes.ADMIN_A_PROPOS)
                }
            }
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        color = HomeTextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 10.dp, top = 13.dp, bottom = 5.dp),
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    @StringRes titleRes: Int,
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
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) HomeBlue else HomeTextMuted,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.weight(1f),
                color = if (selected) HomeBlue else HomeTextDark,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = HomeTextMuted,
                modifier = Modifier.size(12.dp),
            )
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
