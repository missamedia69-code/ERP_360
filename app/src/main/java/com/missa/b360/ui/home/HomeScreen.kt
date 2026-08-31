package com.missa.b360.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * HomeScreen (RA-22) — 3 zones + cloche :
 * - ☰ haut gauche → tiroir Administration & Paramétrage ;
 * - 🔔 haut droite → notifications (badge non-lues) ;
 * - barre du bas → 4 modules par défaut + ➕ « Plus de modules ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showMoreModules by remember { mutableStateOf(false) }
    val nonLues by viewModel.notificationsNonLues.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Le sheet applique largeur, surface et zones sûres Material 3 au menu ☰.
            ModalDrawerSheet {
                AdminDrawerContent(onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(route)
                })
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_title), fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Outlined.AdminPanelSettings,
                                contentDescription = stringResource(R.string.drawer_admin),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                            BadgedBox(
                                badge = {
                                    if (nonLues > 0) {
                                        Badge { Text(nonLues.toString()) }
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.notifications),
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                HomeBottomBar(
                    onModuleClick = { navController.navigate(it.route) },
                    onMore = { showMoreModules = true },
                )
            },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                PlaceholderScreen(titleRes = R.string.home_title, subtitleRes = R.string.home_subtitle)
            }
        }
    }

    if (showMoreModules) {
        ModalBottomSheet(onDismissRequest = { showMoreModules = false }) {
            Text(
                text = stringResource(R.string.more_modules),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AppModule.modulesSecondaires().forEach { module ->
                ListItem(
                    headlineContent = { Text(stringResource(module.titleRes)) },
                    leadingContent = { Icon(module.icon, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreModules = false
                        navController.navigate(module.route)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeBottomBar(
    onModuleClick: (AppModule) -> Unit,
    onMore: () -> Unit,
) {
    NavigationBar {
        AppModule.modulesBarreBas().forEach { module ->
            NavigationBarItem(
                selected = false,
                onClick = { onModuleClick(module) },
                icon = { Icon(module.icon, contentDescription = null) },
                label = { Text(stringResource(module.titleRes)) },
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = onMore,
            icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.more_modules)) },
            label = { Text(stringResource(R.string.more_modules)) },
        )
    }
}

/** Tiroir ☰ — Administration & Paramétrage uniquement (RA-22). */
@Composable
private fun AdminDrawerContent(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Store, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.drawer_admin),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        DrawerItem(Icons.Outlined.Language, R.string.admin_reglages) { onNavigate(Routes.ADMIN_REGLAGES) }
        DrawerItem(Icons.Outlined.CloudSync, R.string.admin_licence) { onNavigate(Routes.ADMIN_LICENCE) }
        DrawerItem(Icons.Outlined.Backup, R.string.admin_sauvegarde) { onNavigate(Routes.ADMIN_SAUVEGARDE) }
        DrawerItem(Icons.Outlined.History, R.string.admin_journal) { onNavigate(Routes.ADMIN_JOURNAL) }
        DrawerItem(Icons.Outlined.Badge, R.string.admin_utilisateurs) { onNavigate(Routes.ADMIN_UTILISATEURS) }
        DrawerItem(Icons.Outlined.Store, R.string.admin_multisite) { onNavigate(Routes.ADMIN_MULTISITE) }
        DrawerItem(Icons.Outlined.Info, R.string.admin_a_propos) { onNavigate(Routes.ADMIN_A_PROPOS) }
    }
}

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, titleRes: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    )
}
