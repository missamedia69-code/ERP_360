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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
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
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Badge as NotificationBadge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.PointPerformance
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.domain.model.RappelsAccueil
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.CompanyLogo
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

/* Palette du tableau de bord mobile. */
private val HomeBlue = BrandBlue
private val HomeBlueSoft = MissaSoftBlue
private val HomeGreen = Green60
private val HomeGreenSoft = Color(0xFFEAF8EF)
private val HomeOrange = Color(0xFFF28A16)
private val HomeOrangeSoft = Color(0xFFFFF1DF)
private val HomePurple = Color(0xFF7047E8)

/** Vert du « 360 » de la marque et de l'identité client. */
private val MarqueVert = Color(0xFF4BAE27)
private val HomePurpleSoft = Color(0xFFF1ECFF)
private val HomeTeal = Color(0xFF00A5A5)
private val HomeRed = Red40
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
    onOuvrirMenu: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
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
            onPersonnaliser = { showPersonnaliser = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onNavigate = { navController.navigate(it) },
        )
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.drawer_admin),
                    tint = HomeTextDark,
                    modifier = Modifier.size(24.dp),
                )
            }
            // Marque Missa - logo image rond + texte
            Image(
                painter = painterResource(R.drawable.logo_missa),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MISSA",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 12.sp,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "BUSINESS",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 12.sp,
                    )
                }
                Text(
                    text = "360",
                    color = Color(0xFF16A34A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            // Puce entité - GREEN FARM SARL style
            Surface(
                modifier = Modifier.clickable(onClick = onProfileClick),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Petit logo feuille verte
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFFE6F8EC),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Store,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(
                            text = companyName.ifBlank { "GREEN FARM SARL" },
                            color = Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = secteur.ifBlank { "Distributeur Agroalimentaire" },
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onNotificationClick, modifier = Modifier.size(40.dp)) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            NotificationBadge(containerColor = Color(0xFFEF4444), contentColor = Color.White) {
                                Text(notificationCount.coerceAtMost(99).toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        tint = HomeTextDark,
                        modifier = Modifier.size(24.dp),
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
    val greeting = state.prenomUtilisateur?.let { stringResource(R.string.home_greeting, it) }
        ?: stringResource(R.string.home_greeting_anonymous)
    // For demo values when no data: use same as maquette to show design, but real data when present
    val hasData = state.ventes > 0 || state.achats > 0 || state.nombreClients > 0
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
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.home_overview),
                    color = Color(0xFF64748B),
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
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
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
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
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
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        // KPI 4 cartes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_ventes_du_jour),
                        valeur = if (hasData) formatMontantSansDecimales(state.ventes, currency) else "1 250 000 XAF",
                        sousTitre = if (hasData) "${state.ventesCount} ventes" else "24 ventes",
                        tendance = state.tendanceVentes ?: 12.0,
                        icon = Icons.Outlined.ShoppingCart,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = Color(0xFF2563EB),
                        onClick = { onNavigate(AppModule.VENTE.route) },
                    )
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_achats_du_jour),
                        valeur = if (hasData) formatMontantSansDecimales(state.achats, currency) else "780 000 XAF",
                        sousTitre = if (hasData) "${state.achatsCount} achats" else "8 achats",
                        tendance = state.tendanceAchats ?: 8.0,
                        icon = Icons.Outlined.Inventory2,
                        iconBg = Color(0xFFECFDF5),
                        iconTint = Color(0xFF16A34A),
                        onClick = { onNavigate(AppModule.ACHATS.route) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_tresorerie_card),
                        valeur = if (hasData) formatMontantSansDecimales(state.tresorerie, currency) else "2 450 000 XAF",
                        sousTitre = stringResource(R.string.home_solde_disponible),
                        tendance = state.tendanceTresorerie ?: 5.0,
                        icon = Icons.Outlined.Payments,
                        iconBg = Color(0xFFFFF7ED),
                        iconTint = Color(0xFFF59E0B),
                        onClick = { onNavigate(AppModule.TRESORERIE.route) },
                    )
                    AccueilKpiCard(
                        modifier = Modifier.weight(1f),
                        titre = stringResource(R.string.home_clients_card),
                        valeur = if (hasData) state.nombreClients.toString() else "356",
                        sousTitre = stringResource(R.string.home_total_label),
                        tendance = state.tendanceClients ?: 4.0,
                        icon = Icons.Outlined.People,
                        iconBg = Color(0xFFF5F3FF),
                        iconTint = Color(0xFF7C3AED),
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
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onPersonnaliser),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_personalize),
                            color = Color(0xFF2563EB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                AccueilActionsGrid(modulesActifs = modulesActifs, onNavigate = onNavigate)
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
private fun tauxMarge(marge: Double, ventes: Double): String {
    if (ventes <= 0.0) return "—"
    val pct = marge / ventes * 100.0
    return String.format(java.util.Locale.ROOT, "%.1f %%", pct)
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
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.dp,
    ) {
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
            Text(text = titre, color = Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(text = valeur, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(text = sousTitre, color = Color(0xFF64748B), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            if (tendance != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.home_vs_hier), color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = (if (tendance >= 0) "+" else "") + String.format(java.util.Locale.ROOT, "%.0f%%", tendance),
                        color = Color(0xFF16A34A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text(text = stringResource(R.string.home_vs_hier) + " —", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AccueilActionsGrid(
    modulesActifs: List<ModuleCode>,
    onNavigate: (String) -> Unit,
) {
    val allActions = listOf(
        AccueilActionItem(stringResource(R.string.home_plus_vente), Icons.Outlined.ShoppingCart, Color(0xFF2563EB), Color(0xFFEFF6FF), AppModule.VENTE.createRoute(), ModuleCode.VEN),
        AccueilActionItem(stringResource(R.string.home_plus_achat), Icons.Outlined.ShoppingCart, Color(0xFF16A34A), Color(0xFFECFDF5), AppModule.ACHATS.createRoute(), ModuleCode.ACH),
        AccueilActionItem(stringResource(R.string.home_plus_client), Icons.Outlined.PersonAdd, Color(0xFF7C3AED), Color(0xFFF5F3FF), AppModule.CLIENTS.createRoute(), ModuleCode.VEN),
        AccueilActionItem(stringResource(R.string.home_plus_fournisseur), Icons.Outlined.Business, Color(0xFFF59E0B), Color(0xFFFFF7ED), AppModule.FOURNISSEURS.createRoute(), ModuleCode.ACH),
        AccueilActionItem(stringResource(R.string.home_entree_en_stock), Icons.Outlined.Inventory2, Color(0xFF0D9488), Color(0xFFECFEFF), AppModule.STOCK.createRoute(), ModuleCode.STK),
        AccueilActionItem(stringResource(R.string.home_transfert_de_stock), Icons.Outlined.LocalShipping, Color(0xFF2563EB), Color(0xFFEFF6FF), Routes.STOCK_TRANSFER_FORM, ModuleCode.STK),
        AccueilActionItem(stringResource(R.string.home_paiement_recu_label), Icons.Outlined.Payments, Color(0xFF16A34A), Color(0xFFECFDF5), AppModule.TRESORERIE.route, ModuleCode.TRE),
        AccueilActionItem(stringResource(R.string.home_depense_label), Icons.Outlined.Description, Color(0xFFF43F5E), Color(0xFFFFF1F2), AppModule.FINANCES.route, ModuleCode.CPT),
    ).filter { modulesActifs.isEmpty() || it.module in modulesActifs }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allActions.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    AccueilActionCard(
                        modifier = Modifier.weight(1f),
                        label = a.label,
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

private data class AccueilActionItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val route: String,
    val module: ModuleCode,
)

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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                color = Color(0xFF0F172A),
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
    val hasData = state.ventes > 0 || state.achats > 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_resume_activite), color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF1F5F9), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.History, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(R.string.home_aujourdhui), color = Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.BarChart,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFFEFF6FF),
                    titre = stringResource(R.string.home_ventes_label),
                    valeur = if (hasData) formatMontantSansDecimales(state.ventes, currency) else "1 250 000 XAF",
                    sousTitre = if (hasData) "${state.ventesCount} ventes" else "24 ventes",
                    tendance = state.tendanceVentes ?: 12.0,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(Color(0xFFE2E8F0)))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ShoppingCart,
                    iconTint = Color(0xFF16A34A),
                    iconBg = Color(0xFFECFDF5),
                    titre = stringResource(R.string.home_achats_label),
                    valeur = if (hasData) formatMontantSansDecimales(state.achats, currency) else "780 000 XAF",
                    sousTitre = if (hasData) "${state.achatsCount} achats" else "8 achats",
                    tendance = state.tendanceAchats ?: 8.0,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(Color(0xFFE2E8F0)))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.SwapHoriz,
                    iconTint = Color(0xFFF59E0B),
                    iconBg = Color(0xFFFFF7ED),
                    titre = stringResource(R.string.home_mouvements_stock_label),
                    valeur = if (hasData) state.mouvementsStockCount.toString() else "42",
                    sousTitre = stringResource(R.string.home_operations_label),
                    tendance = 5.0,
                )
                Box(modifier = Modifier.width(1.dp).height(90.dp).background(Color(0xFFE2E8F0)))
                AccueilResumeCell(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Payments,
                    iconTint = Color(0xFF7C3AED),
                    iconBg = Color(0xFFF5F3FF),
                    titre = stringResource(R.string.home_marge_brute_label),
                    valeur = if (hasData) formatMontantSansDecimales(state.marge, currency) else "470 000 XAF",
                    sousTitre = if (hasData) String.format(java.util.Locale.ROOT, "%.1f%%", margePct) else "37.6%",
                    tendance = state.tendanceMarge ?: 10.0,
                )
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
    tendance: Double,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = iconBg) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = titre, color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text = valeur, color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        Text(text = sousTitre, color = Color(0xFF64748B), fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = stringResource(R.string.home_vs_hier), color = Color(0xFF94A3B8), fontSize = 9.sp)
            Spacer(Modifier.width(4.dp))
            Text(text = "+" + String.format(java.util.Locale.ROOT, "%.0f%%", tendance), color = Color(0xFF16A34A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_activites_recentes), color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.home_see_all),
                    color = Color(0xFF2563EB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppModule.REPORTING.route) },
                )
            }
            Spacer(Modifier.height(10.dp))
            if (records.isEmpty()) {
                // Données de maquette quand vide
                AccueilActiviteRow(
                    icon = Icons.Outlined.ShoppingCart, iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB),
                    titre = "Vente #V-0025", sousTitre = "Client ABC", montant = "125 000 XAF", badge = stringResource(R.string.home_payee), heure = "10:42",
                    onClick = { onNavigate(AppModule.VENTE.route) },
                )
                Spacer(Modifier.height(8.dp))
                AccueilActiviteRow(
                    icon = Icons.Outlined.Inventory2, iconBg = Color(0xFFECFDF5), iconTint = Color(0xFF16A34A),
                    titre = "Achat #A-0012", sousTitre = "Fournisseur AgroMax", montant = "230 000 XAF", badge = stringResource(R.string.home_recu), heure = "09:15",
                    onClick = { onNavigate(AppModule.ACHATS.route) },
                )
                Spacer(Modifier.height(8.dp))
                AccueilActiviteRow(
                    icon = Icons.Outlined.People, iconBg = Color(0xFFF5F3FF), iconTint = Color(0xFF7C3AED),
                    titre = "Nouveau client", sousTitre = "ETS Prestige", montant = null, badge = null, heure = "Hier, 16:30",
                    onClick = { onNavigate(AppModule.CLIENTS.route) },
                )
                Spacer(Modifier.height(8.dp))
                AccueilActiviteRow(
                    icon = Icons.Outlined.LocalShipping, iconBg = Color(0xFFFFF7ED), iconTint = Color(0xFFF59E0B),
                    titre = stringResource(R.string.home_transfert_de_stock), sousTitre = "Dépôt principal → Boutique Akwa", montant = null, badge = null, heure = "Hier, 14:20",
                    onClick = { onNavigate(AppModule.STOCK.route) },
                )
            } else {
                records.forEachIndexed { idx, rec ->
                    val (icon, bg, tint) = when (rec.module) {
                        OperationModule.VENTE.name -> Triple(Icons.Outlined.ShoppingCart, Color(0xFFEFF6FF), Color(0xFF2563EB))
                        OperationModule.ACHATS.name -> Triple(Icons.Outlined.Inventory2, Color(0xFFECFDF5), Color(0xFF16A34A))
                        OperationModule.STOCK.name -> Triple(Icons.Outlined.LocalShipping, Color(0xFFFFF7ED), Color(0xFFF59E0B))
                        else -> Triple(Icons.Outlined.People, Color(0xFFF5F3FF), Color(0xFF7C3AED))
                    }
                    AccueilActiviteRow(
                        icon = icon, iconBg = bg, iconTint = tint,
                        titre = rec.title.ifBlank { rec.reference },
                        sousTitre = rec.counterpart ?: rec.reference,
                        montant = rec.amount?.let { formatMontantSansDecimales(it, currency) },
                        badge = when (rec.status) {
                            OperationStatus.VALIDATED.name -> "Payée"
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
            Text(text = titre, color = Color(0xFF0F172A), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = sousTitre, color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (montant != null) Text(text = montant, color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = heure, color = Color(0xFF94A3B8), fontSize = 10.sp)
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5)) {
                        Text(text = badge, color = Color(0xFF16A34A), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
    val hasAlert = factures > 0 || commandes > 0
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.TASKS) },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = Color(0xFFFEF3C7)) {
                Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.home_rappels_importants), color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Spacer(Modifier.width(6.dp))
                    Text(text = if (hasAlert) "$factures factures clients en retard" else "2 factures clients en retard", color = Color(0xFF334155), fontSize = 11.sp)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Spacer(Modifier.width(6.dp))
                    Text(text = if (hasAlert) "$commandes commande fournisseur attendue" else "1 commande fournisseur attendue", color = Color(0xFF334155), fontSize = 11.sp)
                }
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(26.dp), shape = RoundedCornerShape(7.dp), color = Color(0xFFEFF6FF)) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.Checklist, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.home_taches_du_jour), color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            if (taches.isEmpty()) {
                AccueilTacheRow(titre = stringResource(R.string.home_relancer_clients))
                Spacer(Modifier.height(8.dp))
                AccueilTacheRow(titre = stringResource(R.string.home_verifier_stock))
                Spacer(Modifier.height(8.dp))
                AccueilTacheRow(titre = stringResource(R.string.home_saisir_depenses))
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
                Text(text = stringResource(R.string.home_voir_toutes_taches), color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AccueilTacheRow(titre: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = titre, color = Color(0xFF334155), fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        ) {}
    }
}



/**
 * Résumé d'activité de la période choisie.
 *
 * Il ne répète pas les cartes du haut : celles-ci donnent la journée en cours,
 * celui-ci se lit sur la période sélectionnée et ajoute deux mesures absentes
 * plus haut — les mouvements de stock et la marge brute.
 */
@Composable
private fun ResumeActivite(state: HomeUiState, currency: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Box {
            // Filigrane de documents, aligné à droite et très pâle.
            Image(
                painter = painterResource(R.drawable.fond_documents),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alpha = 0.26f,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.55f),
            )
            Row(modifier = Modifier.padding(vertical = 14.dp)) {
                ResumeCellule(
                    R.string.module_vente,
                    MoneyUtils.format(state.ventes, currency),
                    Icons.Outlined.BarChart,
                    HomeBlue,
                    Modifier.weight(1f),
                )
                ResumeSeparateur()
                ResumeCellule(
                    R.string.module_achats,
                    MoneyUtils.format(state.achats, currency),
                    Icons.Outlined.ShoppingCart,
                    MarqueVert,
                    Modifier.weight(1f),
                )
                ResumeSeparateur()
                ResumeCellule(
                    R.string.home_mouvements_stock,
                    state.quantiteStock.displayQuantity(),
                    Icons.Outlined.SwapHoriz,
                    HomeOrange,
                    Modifier.weight(1f),
                )
                ResumeSeparateur()
                ResumeCellule(
                    R.string.home_gross_margin_court,
                    MoneyUtils.format(state.marge, currency),
                    Icons.Outlined.Payments,
                    HomePurple,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResumeCellule(
    titreRes: Int,
    valeur: String,
    icone: ImageVector,
    couleur: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(titreRes),
            color = HomeTextDark,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = valeur,
            color = HomeTextDark,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = "—", color = HomeTextMuted, fontSize = 10.sp)
        Text(
            text = stringResource(R.string.home_vs_hier),
            color = HomeTextMuted,
            fontSize = 9.5.sp,
        )
    }
}

@Composable
private fun ResumeSeparateur() {
    Box(
        modifier = Modifier
            .height(58.dp)
            .width(1.dp)
            .background(HomeBorder),
    )
}

/**
 * Rappels de l'accueil — désormais calculés.
 *
 * Les deux lignes affichaient un texte figé qui rassurait même avec des
 * impayés : elles reflètent maintenant les tâches ouvertes et les factures
 * client dont le délai de règlement est dépassé.
 */
@Composable
private fun RappelsCard(rappels: RappelsAccueil, onClick: () -> Unit) {
    val alerte = rappels.aQuelqueChose
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (alerte) Color(0xFFFFF4E5) else Color(0xFFF6F8FC),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = if (alerte) HomeOrange else HomeBlueSoft,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = if (alerte) Color.White else HomeBlue,
                    modifier = Modifier.padding(9.dp),
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
                    text = "• " + if (rappels.tachesEnAttente > 0) {
                        stringResource(R.string.home_pending_tasks, rappels.tachesEnAttente)
                    } else {
                        stringResource(R.string.home_no_pending_tasks)
                    },
                    color = HomeTextDark,
                    fontSize = 11.sp,
                )
                Text(
                    text = "• " + if (rappels.facturesEnRetard > 0) {
                        stringResource(R.string.home_overdue_invoices, rappels.facturesEnRetard)
                    } else {
                        stringResource(R.string.home_no_overdue_invoices)
                    },
                    color = HomeTextDark,
                    fontSize = 11.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = HomeTextMuted,
                modifier = Modifier.size(15.dp),
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
    comparaison: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        // Une carte qui nomme un module doit y conduire : l'utilisateur la
        // touche de toute façon.
        modifier = modifier
            .height(150.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = iconBackground) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = HomeTextDark,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = HomeTextMuted,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                color = HomeTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            // Le tiret marque l'absence de comparaison possible : sans historique,
            // afficher « 0 % » laisserait croire à une stagnation mesurée.
            Text(
                text = comparaison ?: "—",
                color = HomeTextMuted,
                fontSize = 10.sp,
            )
            Text(
                text = stringResource(R.string.home_vs_hier),
                color = HomeTextMuted,
                fontSize = 9.5.sp,
            )
        }
    }
}

/** Barre de commande du cockpit : entité active d'un côté, « Nouvelle vente » de l'autre. */
@Composable
private fun CockpitBarreEntite(
    nomEntreprise: String,
    secteur: String,
    logoUri: String?,
    onNouvelleVente: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompanyLogo(
                logoUri = logoUri,
                contentDescription = null,
                fallbackIcon = Icons.Outlined.Business,
                modifier = Modifier.size(30.dp),
                size = 30.dp,
                shape = RoundedCornerShape(9.dp),
                fallbackTint = MarqueVert,
                fallbackBackground = HomeGreenSoft,
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_entite),
                    color = HomeTextMuted,
                    fontSize = 10.sp,
                )
                Text(
                    text = nomEntreprise,
                    color = HomeTextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (secteur.isNotBlank()) {
                    Text(
                        text = secteur,
                        color = HomeTextMuted,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = HomeTextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.clickable(onClick = onNouvelleVente),
                shape = RoundedCornerShape(10.dp),
                color = MarqueVert,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_new_sale),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Carte KPI compacte du cockpit : valeur, libellé et tendance en un coup d'œil. */
@Composable
private fun CockpitKpiCard(
    modifier: Modifier,
    titre: String,
    valeur: String,
    tendance: Double?,
    icone: ImageVector,
    couleur: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = titre,
                    modifier = Modifier.weight(1f),
                    color = HomeTextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = couleur.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = couleur,
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = valeur,
                color = HomeTextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            TendanceLine(tendance)
        }
    }
}

/** Ligne de tendance : flèche + pourcentage coloré, ou tiret si non comparable. */
@Composable
private fun TendanceLine(tendance: Double?) {
    if (tendance == null) {
        Text(
            text = stringResource(R.string.home_vs_hier),
            color = HomeTextMuted,
            fontSize = 9.sp,
        )
        return
    }
    val positive = tendance >= 0.0
    val couleur = if (positive) HomeGreen else HomeRed
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (positive) Icons.AutoMirrored.Outlined.TrendingUp
            else Icons.AutoMirrored.Outlined.TrendingDown,
            contentDescription = null,
            tint = couleur,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = String.format(java.util.Locale.ROOT, "%+.1f %%", tendance),
            color = couleur,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.home_vs_hier),
            color = HomeTextMuted,
            fontSize = 9.sp,
        )
    }
}

/** Graphique de performance : encaissements des mois récents, en clair. */
@Composable
private fun CockpitPerformanceCard(
    points: List<PointPerformance>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_performance),
                    modifier = Modifier.weight(1f),
                    color = HomeTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_encaissements),
                    color = HomeBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (points.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_documents),
                    color = HomeTextMuted,
                    fontSize = 11.sp,
                )
            } else {
                Column {
                    val max = points.maxOf { it.montant }.coerceAtLeast(1.0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        points.forEach { point ->
                            val hauteur = (point.montant / max).coerceIn(0.06, 1.0)
                            BarrePerformance(
                                fraction = hauteur.toFloat(),
                                isCurrent = point == points.last(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        points.forEach { point ->
                            Text(
                                text = stringResource(moisCourtRes(point.moisIndex)),
                                modifier = Modifier.weight(1f),
                                color = HomeTextMuted,
                                fontSize = 8.5.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarrePerformance(fraction: Float, isCurrent: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(if (isCurrent) HomeGreen else HomeGreenSoft),
        )
    }
}

/** Résumé du « centre d'activité » : factures, alertes et santé de l'entreprise. */
@Composable
private fun CockpitCentreActivite(
    rappels: RappelsAccueil,
    derniereSauvegarde: Long?,
    onVoirFactures: () -> Unit,
    onVoirAlertes: () -> Unit,
    onVoirSante: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(vertical = 7.dp)) {
            Text(
                text = stringResource(R.string.home_centre_activite),
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                color = HomeTextDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            CockpitActiviteLigne(
                titre = stringResource(R.string.home_factures),
                detail = stringResource(
                    R.string.home_factures_detail,
                    rappels.facturesEnRetard,
                ),
                statutRes = statutFactures(rappels),
                icone = Icons.AutoMirrored.Outlined.ReceiptLong,
                onClick = onVoirFactures,
            )
            CockpitActiviteLigne(
                titre = stringResource(R.string.home_alertes),
                detail = stringResource(
                    R.string.home_alertes_detail,
                    rappels.tachesEnAttente,
                ),
                statutRes = statutAlertes(rappels),
                icone = Icons.Outlined.Notifications,
                onClick = onVoirAlertes,
            )
            CockpitActiviteLigne(
                titre = stringResource(R.string.home_sante),
                detail = derniereSauvegarde?.let {
                    stringResource(R.string.home_backup_date, DateUtils.formatDate(it))
                } ?: stringResource(R.string.home_backup_never),
                statutRes = R.string.home_statut_ok,
                icone = Icons.Outlined.Security,
                onClick = onVoirSante,
            )
        }
    }
}

@Composable
private fun CockpitActiviteLigne(
    titre: String,
    detail: String,
    statutRes: Int,
    icone: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(9.dp),
            color = HomeBlueSoft,
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = HomeBlue,
                modifier = Modifier.padding(7.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titre,
                color = HomeTextDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = HomeTextMuted,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatutBadge(statutRes)
    }
}

@Composable
private fun StatutBadge(statutRes: Int) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = HomeGreenSoft,
    ) {
        Text(
            text = stringResource(statutRes),
            color = HomeGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun statutFactures(rappels: RappelsAccueil): Int =
    if (rappels.facturesEnRetard > 0) R.string.home_statut_a_suivre else R.string.home_statut_ok

@Composable
private fun statutAlertes(rappels: RappelsAccueil): Int =
    if (rappels.tachesEnAttente > 0) R.string.home_statut_a_suivre else R.string.home_statut_ok

/** Ressource du libellé court d'un mois (1 = janvier … 12 = décembre). */
private fun moisCourtRes(mois: Int): Int = when (mois) {
    1 -> R.string.mois_jan
    2 -> R.string.mois_fev
    3 -> R.string.mois_mar
    4 -> R.string.mois_avr
    5 -> R.string.mois_mai
    6 -> R.string.mois_juin
    7 -> R.string.mois_juil
    8 -> R.string.mois_aout
    9 -> R.string.mois_sep
    10 -> R.string.mois_oct
    11 -> R.string.mois_nov
    else -> R.string.mois_dec
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
            Box {
                // Le même filigrane que le résumé : l'état vide reste habité.
                Image(
                    painter = painterResource(R.drawable.fond_documents),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alpha = 0.26f,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.45f),
                )
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
                        text = stringResource(R.string.home_documents_hint),
                        color = HomeTextMuted,
                        fontSize = 10.sp,
                        maxLines = 2,
                    )
                }
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
