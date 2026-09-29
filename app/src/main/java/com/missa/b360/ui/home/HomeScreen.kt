package com.missa.b360.ui.home

import com.missa.b360.core.domain.model.PointJour

import com.missa.b360.core.domain.model.CockpitRules

import androidx.compose.foundation.border

import androidx.compose.material3.rememberDatePickerState

import androidx.compose.material3.DatePickerDialog

import androidx.compose.material3.DatePicker

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.Path

import androidx.compose.foundation.Canvas

import com.missa.b360.ui.icons.Iv
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.Blue80
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.ProfileTeal
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80
import com.missa.b360.ui.theme.TendrePositive

/* Palette du tableau de bord mobile — centralisée, plus d'inline Color(0x). */
private val HomeBlue = BrandBlue
private val HomeBlueSoft = MissaSoftBlue
private val HomeGreen = Green60
private val HomeGreenSoft = com.missa.b360.ui.theme.Green90
private val HomeOrange = com.missa.b360.ui.theme.ProfileOrange
private val HomeOrangeSoft = com.missa.b360.ui.theme.Blue90 // fallback clair, remplace FFF1DF/FFF7ED
private val HomePurple = com.missa.b360.ui.theme.ProfilePurple

/** Vert du « 360 » de la marque et de l'identité client. */
private val MarqueVert = com.missa.b360.ui.theme.ProfileGreen
private val HomePurpleSoft = com.missa.b360.ui.theme.Blue90
private val HomeTeal = com.missa.b360.ui.theme.ProfileTeal
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
    const val PRODUIT = "PRODUIT"
    const val LIVRAISON = "LIVRAISON"
    const val DEVIS = "DEVIS"
    const val FACTURE = "FACTURE"
    // Rétrocompatibilité
    const val ENTREE_STOCK = "ENTREE_STOCK"
    const val TRANSFERT_STOCK = "TRANSFERT_STOCK"
    const val PAIEMENT_RECU = "PAIEMENT_RECU"
    const val DEPENSE = "DEPENSE"
    val ALL: List<String> = listOf(
        VENTE, ACHAT, CLIENT, FOURNISSEUR,
        PRODUIT, LIVRAISON, DEVIS, FACTURE,
    )
}

private data class AccueilActionDef(
    val key: String,
    @StringRes val labelRes: Int,
    val icon: Int,
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
        icon = Iv.ShoppingCart,
        tint = MissaInk,
        bg = Color(0xFFEFF6FF),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.VENTE)),
        module = ModuleCode.VEN,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.ACHAT,
        labelRes = R.string.home_plus_achat,
        icon = Iv.CartArrowDown,
        tint = MissaInk,
        bg = Color(0xFFFEF3C7),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.ACHAT)),
        module = ModuleCode.ACH,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.CLIENT,
        labelRes = R.string.home_plus_client,
        icon = Iv.PersonAdd,
        tint = MissaInk,
        bg = Color(0xFFF3E8FF),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.CLIENT)),
        module = ModuleCode.VEN,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.FOURNISSEUR,
        labelRes = R.string.home_plus_fournisseur,
        icon = Iv.Handshake,
        tint = MissaInk,
        bg = Color(0xFFFCE7F3),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.FOURNISSEUR)),
        module = ModuleCode.ACH,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.PRODUIT,
        labelRes = R.string.home_plus_produit,
        icon = Iv.Inventory2,
        tint = MissaInk,
        bg = Color(0xFFCCFBF1),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.PRODUIT)),
        module = ModuleCode.STK,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.LIVRAISON,
        labelRes = R.string.home_plus_livraison,
        icon = Iv.LocalShipping,
        tint = MissaInk,
        bg = Color(0xFFE0E7FF),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.LIVRAISON)),
        module = ModuleCode.LOG,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.DEVIS,
        labelRes = R.string.home_plus_devis,
        icon = Iv.Description,
        tint = MissaInk,
        bg = Color(0xFFDCFCE7),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.DEVIS)),
        module = ModuleCode.VEN,
    ),
    AccueilActionDef(
        key = AccueilActionKeys.FACTURE,
        labelRes = R.string.home_plus_facture,
        icon = Iv.RequestQuote,
        tint = MissaInk,
        bg = Color(0xFFFFE4E6),
        route = requireNotNull(HomeNavigation.quickAction(AccueilActionKeys.FACTURE)),
        module = ModuleCode.VEN,
    ),
)

/**
 * Accueil mobile : tableau de bord conforme à la nouvelle charte graphique MISSA BUSINESS 360.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onSupport: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val nonLues by viewModel.notificationsNonLues.collectAsState(initial = 0)
    val uiState by viewModel.uiState.collectAsState()
    val modulesActifs by viewModel.modulesActifs.collectAsState()
    val actionsEpingles by viewModel.actionsRapidesEpingles.collectAsState()
    var showPersonnaliser by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        HomeDashboard(
            state = uiState,
            modulesActifs = modulesActifs,
            actionsRapidesSelection = actionsEpingles,
            onPersonnaliser = { showPersonnaliser = true },
            modifier = Modifier.fillMaxSize(),
            onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
            onSupport = onSupport,
            notificationCount = nonLues,
            onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } },
            onSelectionJour = viewModel::selectionnerJour,
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
private fun HomeDashboard(
    state: HomeUiState,
    modulesActifs: List<ModuleCode>,
    actionsRapidesSelection: List<String>,
    onPersonnaliser: () -> Unit,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
    onSupport: () -> Unit = {},
    notificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onSelectionJour: (Long) -> Unit = {},
) {
    var kpiEnVue by remember { mutableStateOf(-1) }
    val currency = state.devise
    val greeting = state.prenomUtilisateur?.let { stringResource(R.string.home_greeting, it) }
        ?: stringResource(R.string.home_greeting_anonymous)
    when (kpiEnVue) {
        0 -> KpiPopup(stringResource(R.string.home_ventes_du_jour), state.serieVentes, AppModule.VENTE.couleur, stringResource(R.string.kpi_ventes_explication)) { kpiEnVue = -1 }
        1 -> KpiPopup(stringResource(R.string.home_achats_du_jour), state.serieAchats, AppModule.ACHATS.couleur, stringResource(R.string.kpi_achats_explication)) { kpiEnVue = -1 }
        2 -> KpiPopup(stringResource(R.string.home_tresorerie_card), state.serieTresorerie, AppModule.TRESORERIE.couleur, stringResource(R.string.kpi_tresorerie_explication)) { kpiEnVue = -1 }
        3 -> KpiPopup(stringResource(R.string.home_clients_card), state.serieClients, AppModule.CLIENTS.couleur, stringResource(R.string.kpi_clients_explication)) { kpiEnVue = -1 }
    }
    val profil = state.profilActivite.profileLabel()?.let { stringResource(it) }
        ?: state.profilActivite ?: "—"
    val taille = state.palierTaille.sizeLabel()?.let { stringResource(it) }
        ?: state.palierTaille ?: stringResource(R.string.home_not_configured)
    val dateDuJour = remember {
        java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    LazyColumn(
        modifier = modifier.background(HomeBackground),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            // Identité de l'entreprise : repère immédiatement l'espace de travail ouvert.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MissaInk,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF101C43), Color(0xFF183E91), Color(0xFF1554E8)),
                            ),
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompanyLogo(
                            logoUri = state.entrepriseLogoUri,
                            contentDescription = null,
                            fallbackIcon = Iv.Business,
                            modifier = Modifier.size(36.dp),
                            size = 42.dp,
                            shape = RoundedCornerShape(13.dp),
                            fallbackTint = Color.White,
                            fallbackBackground = Color.White.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.entrepriseNom.ifBlank { stringResource(R.string.home_company_placeholder) },
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.secteur.ifBlank { "MISSA BUSINESS 360" },
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box {
                            Surface(
                                modifier = Modifier.size(40.dp).clickable(onClick = onNotificationClick),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(Iv.Notifications),
                                        contentDescription = stringResource(R.string.notifications),
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                            if (notificationCount > 0) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    shape = CircleShape,
                                    color = Color(0xFFFF5C63),
                                    border = BorderStroke(1.5.dp, Color(0xFF183E91)),
                                ) {
                                    Text(
                                        text = notificationCount.coerceAtMost(99).toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = greeting,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.home_overview),
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = dateDuJour,
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onNavigate(Routes.ADMIN_REGLAGES) }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(Iv.Business), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.home_profil_ligne, profil), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(1.dp).height(17.dp).background(HomeBorder))
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(Iv.Groups), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.home_taille_ligne, taille), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(5.dp))
                Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(16.dp))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AccueilKpiCard(Modifier.weight(1f), stringResource(R.string.home_ventes_du_jour), formatMontantSansDecimales(state.ventes, currency), stringResource(R.string.home_sales_count, state.ventesCount), state.tendanceVentes, Iv.ShoppingCart, Color(0xFFE8F1FF), HomeBlue, R.drawable.home_dashboard_sales, { kpiEnVue = 0 })
                    AccueilKpiCard(Modifier.weight(1f), stringResource(R.string.home_achats_du_jour), formatMontantSansDecimales(state.achats, currency), stringResource(R.string.home_purchases_count, state.achatsCount), state.tendanceAchats, Iv.CartArrowDown, Color(0xFFFFF3DB), Color(0xFFB66A00), R.drawable.home_dashboard_purchases, { kpiEnVue = 1 })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AccueilKpiCard(Modifier.weight(1f), stringResource(R.string.home_tresorerie_card), formatMontantSansDecimales(state.tresorerie, currency), stringResource(R.string.home_solde_disponible), state.tendanceTresorerie, Iv.Bank, Color(0xFFE5F7F0), Color(0xFF16845C), R.drawable.home_dashboard_treasury, { kpiEnVue = 2 })
                    AccueilKpiCard(Modifier.weight(1f), stringResource(R.string.home_clients_card), state.nombreClients.toString(), stringResource(R.string.home_total_label), state.tendanceClients, Iv.People, Color(0xFFF1EBFF), Color(0xFF7046B8), R.drawable.home_dashboard_clients, { kpiEnVue = 3 })
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_quick_actions),
                        color = HomeTextDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onPersonnaliser, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                        Icon(painterResource(Iv.Settings), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.home_personalize), color = HomeBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                AccueilActionsGrid(modulesActifs, actionsRapidesSelection, onNavigate)
            }
        }
        item {
            AccueilResumeCard(state = state, currency = currency, onSelectionJour = onSelectionJour)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                AccueilActivitesRecentesCard(state = state, currency = currency, onNavigate = onNavigate)
                AccueilRappelsCard(state = state, onNavigate = onNavigate)
                AccueilTachesCard(state = state, onNavigate = onNavigate)
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSupport),
                shape = RoundedCornerShape(13.dp),
                color = Color(0xFFEAF2FF),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color.White, modifier = Modifier.size(30.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(Iv.Chat), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(19.dp))
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_besoin_aide), color = HomeTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.home_support_desc), color = HomeTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(painterResource(Iv.ChevronRight), contentDescription = stringResource(R.string.home_acceder_support), tint = HomeBlue, modifier = Modifier.size(20.dp))
                }
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
    icon: Int,
    iconBg: Color,
    iconTint: Color,
    illustrationRes: Int,
    onClick: () -> Unit,
) {
    val deltaColor = if ((tendance ?: 0.0) >= 0.0) Color(0xFF16845C) else Color(0xFFB42332)
    Surface(
        modifier = modifier.height(106.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder.copy(alpha = 0.72f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Illustration originale pleine carte, embarquée dans l'APK pour le mode hors ligne.
            Image(
                painter = painterResource(illustrationRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            )
            // Voile lumineux : réserve visuellement la zone texte à gauche et adoucit l'image.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.96f),
                                Color.White.copy(alpha = 0.86f),
                                Color.White.copy(alpha = 0.40f),
                            ),
                        ),
                    ),
            )
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(9.dp), color = iconBg, modifier = Modifier.size(26.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = HomeTextMuted.copy(alpha = 0.65f), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(titre, color = HomeTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(valeur, color = HomeTextDark, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sousTitre,
                        color = HomeTextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (tendance != null) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            painter = painterResource(if (tendance >= 0) Iv.TrendingUp else Iv.TrendingDown),
                            contentDescription = null,
                            tint = deltaColor,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "${if (tendance >= 0) "+" else ""}${String.format(java.util.Locale.ROOT, "%.0f%%", tendance)}",
                            color = deltaColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
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
    icon: Int,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(modifier = Modifier.size(26.dp), shape = RoundedCornerShape(9.dp), color = bg) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = HomeTextDark,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccueilResumeCard(state: HomeUiState, currency: String, onSelectionJour: (Long) -> Unit = {}) {
    val margePct = if (state.resumeVentes > 0) state.resumeMarge / state.resumeVentes * 100.0 else 0.0
    var choixDate by remember { mutableStateOf(false) }
    val estAujourdhui = state.resumeJour == CockpitRules.debutJour(System.currentTimeMillis())
    val labelJour = if (estAujourdhui) {
        stringResource(R.string.home_aujourdhui)
    } else {
        remember(state.resumeJour) {
            java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(state.resumeJour))
        }
    }
    if (choixDate) {
        val etatDate = rememberDatePickerState(initialSelectedDateMillis = state.resumeJour)
        DatePickerDialog(
            onDismissRequest = { choixDate = false },
            confirmButton = {
                TextButton(onClick = {
                    etatDate.selectedDateMillis?.let { onSelectionJour(it + 43_200_000L) }
                    choixDate = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { choixDate = false }) {
                    Text(stringResource(R.string.kpi_popup_fermer))
                }
            },
        ) {
            DatePicker(state = etatDate)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_resume_activite), color = HomeTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = HomeBackground,
                    border = BorderStroke(1.dp, HomeBorder),
                    modifier = Modifier.height(40.dp).clickable { choixDate = true },
                ) {
                    Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(Iv.Calendar), contentDescription = null, tint = MissaInk, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = labelJour, color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(painter = painterResource(Iv.ArrowDropDown), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AccueilResumeCell(
                        modifier = Modifier.weight(1f), icon = Iv.BarChart, iconTint = HomeBlue,
                        iconBg = HomeBlueSoft, titre = stringResource(R.string.home_ventes_label),
                        valeur = formatMontantSansDecimales(state.resumeVentes, currency),
                        sousTitre = stringResource(R.string.home_sales_count, state.resumeVentesCount),
                        tendance = if (estAujourdhui) state.tendanceVentes else null,
                    )
                    AccueilResumeCell(
                        modifier = Modifier.weight(1f), icon = Iv.CartArrowDown, iconTint = Color(0xFFB66A00),
                        iconBg = Color(0xFFFFF3DB), titre = stringResource(R.string.home_achats_label),
                        valeur = formatMontantSansDecimales(state.resumeAchats, currency),
                        sousTitre = stringResource(R.string.home_purchases_count, state.resumeAchatsCount),
                        tendance = if (estAujourdhui) state.tendanceAchats else null,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AccueilResumeCell(
                        modifier = Modifier.weight(1f), icon = Iv.SwapHoriz, iconTint = Color(0xFF16845C),
                        iconBg = Color(0xFFE5F7F0), titre = stringResource(R.string.home_mouvements_stock_label),
                        valeur = state.resumeMouvements.toString(),
                        sousTitre = if (state.rupturesStock > 0) stringResource(R.string.home_produits_rupture, state.rupturesStock) else stringResource(R.string.home_operations_label),
                        tendance = null,
                    )
                    AccueilResumeCell(
                        modifier = Modifier.weight(1f), icon = Iv.Percent, iconTint = Color(0xFF7046B8),
                        iconBg = Color(0xFFF1EBFF), titre = stringResource(R.string.home_marge_brute_label),
                        valeur = formatMontantSansDecimales(state.resumeMarge, currency),
                        sousTitre = String.format(java.util.Locale.ROOT, "%.1f%%", margePct),
                        tendance = if (estAujourdhui) state.tendanceMarge else null,
                    )
                }
            }
            // Ligne additionnelle réelle : stock, projets, qualité – 100% i18n
            if (state.nombreProduits > 0 || state.projetsActifs > 0 || state.nonConformitesOuvertes > 0) {
                Spacer(Modifier.height(7.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.nombreProduits > 0) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MissaCanvas, border = BorderStroke(1.dp, HomeBorder)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.home_produits_count, state.nombreProduits), color = HomeTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = formatMontantSansDecimales(state.valeurStock, currency), color = HomeTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                    if (state.projetsActifs > 0) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = HomePurpleSoft, border = BorderStroke(1.dp, HomeBorder)) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
    icon: Int,
    iconTint: Color,
    iconBg: Color,
    titre: String,
    valeur: String,
    sousTitre: String,
    tendance: Double?,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(HomeBackground)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(modifier = Modifier.size(26.dp), shape = RoundedCornerShape(9.dp), color = iconBg) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(titre, color = HomeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(valeur, color = HomeTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (tendance != null) {
                    "$sousTitre · ${if (tendance >= 0) "+" else ""}${String.format(java.util.Locale.ROOT, "%.0f%%", tendance)}"
                } else sousTitre,
                color = if (tendance == null) HomeTextMuted else if (tendance >= 0) TendrePositive else Red40,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccueilActivitesRecentesCard(state: HomeUiState, currency: String, onNavigate: (String) -> Unit) {
    val records = state.recentOperations
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.home_activites_recentes), color = HomeTextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onNavigate(AppModule.REPORTING.route) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_see_all),
                        color = HomeBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            if (records.isEmpty()) {
                // 100% réel : vide = message, pas de maquette
                Text(
                    text = stringResource(R.string.home_no_recent_activity),
                    color = HomeTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                records.forEachIndexed { idx, rec ->
                    val saleDetails = remember(rec.notes) { SaleRecordCodec.decode(rec.notes) }
                    val saleEntierementPayee = saleDetails?.let { it.paidAmount >= it.total - 0.01 } == true
                    val ventePartiellementPayee = saleDetails?.let { it.paidAmount > 0.01 && it.paidAmount < it.total - 0.01 } == true
                    val (icon, bg, tint) = when (rec.module) {
                        OperationModule.VENTE.name -> Triple(Iv.ShoppingCart, HomeBlueSoft, HomeBlue)
                        OperationModule.ACHATS.name -> Triple(Iv.Inventory2, Green90, TendrePositive)
                        OperationModule.STOCK.name -> Triple(Iv.LocalShipping, HomeOrangeSoft, ProfileOrange)
                        OperationModule.PROJETS.name -> Triple(Iv.BarChart, HomePurpleSoft, ProfilePurple)
                        OperationModule.FINANCES.name -> Triple(Iv.Payments, HomeOrangeSoft, ProfileOrange)
                        else -> Triple(Iv.People, HomePurpleSoft, ProfilePurple)
                    }
                    AccueilActiviteRow(
                        icon = icon, iconBg = bg, iconTint = tint,
                        titre = rec.title.ifBlank { rec.reference },
                        sousTitre = rec.counterpart ?: rec.reference,
                        montant = rec.amount?.let { formatMontantSansDecimales(it, currency) },
                        badge = when {
                            rec.status == OperationStatus.DRAFT.name -> stringResource(R.string.ach_brouillon)
                            rec.status == OperationStatus.VALIDATED.name && rec.module != OperationModule.VENTE.name -> stringResource(R.string.ach_valide)
                            rec.status == OperationStatus.VALIDATED.name && saleDetails == null -> stringResource(R.string.ach_valide)
                            rec.status == OperationStatus.VALIDATED.name && saleEntierementPayee -> stringResource(R.string.home_payee)
                            rec.status == OperationStatus.VALIDATED.name && ventePartiellementPayee -> stringResource(R.string.home_partially_paid)
                            rec.status == OperationStatus.VALIDATED.name -> stringResource(R.string.home_to_collect)
                            else -> null
                        },
                        badgeBackground = if (rec.status == OperationStatus.DRAFT.name) Color(0xFFFFF3DB) else Green90,
                        badgeTint = if (rec.status == OperationStatus.DRAFT.name) ProfileOrange else TendrePositive,
                        heure = DateUtils.formatDateHeure(rec.createdAt),
                        onClick = { onNavigate(HomeNavigation.operation(rec.module)) },
                    )
                    if (idx < records.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AccueilActiviteRow(
    icon: Int,
    iconBg: Color,
    iconTint: Color,
    titre: String,
    sousTitre: String,
    montant: String?,
    badge: String?,
    badgeBackground: Color,
    badgeTint: Color,
    heure: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = iconBg) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.width(7.dp))
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
                    Surface(shape = RoundedCornerShape(6.dp), color = badgeBackground) {
                        Text(text = badge, color = badgeTint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (hasAlert) HomeBackground else Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = Color.White) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Iv.Notifications),
                            contentDescription = null,
                            tint = if (hasAlert) ProfileOrange else HomeTextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(stringResource(R.string.home_rappels_importants), color = HomeTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (!hasAlert) {
                        Text(stringResource(R.string.home_no_alerts), color = HomeTextMuted, fontSize = 11.sp)
                    }
                }
            }
            if (hasAlert) {
                Spacer(Modifier.height(4.dp))
                if (factures > 0) {
                    AccueilRappelLigne(
                        texte = stringResource(R.string.home_overdue_invoices, factures),
                        couleur = ProfileOrange,
                        onClick = { onNavigate(HomeNavigation.rappel(HomeNavigation.Rappel.FACTURES_EN_RETARD)) },
                    )
                }
                if (commandes > 0) {
                    AccueilRappelLigne(
                        texte = stringResource(R.string.home_commande_fournisseur_attente, commandes),
                        couleur = ProfileOrange,
                        onClick = { onNavigate(HomeNavigation.rappel(HomeNavigation.Rappel.COMMANDES_FOURNISSEUR)) },
                    )
                }
                if (ruptures > 0) {
                    AccueilRappelLigne(
                        texte = stringResource(R.string.home_produits_rupture, ruptures),
                        couleur = Red40,
                        onClick = { onNavigate(HomeNavigation.rappel(HomeNavigation.Rappel.RUPTURES_STOCK)) },
                    )
                }
                if (nc > 0) {
                    AccueilRappelLigne(
                        texte = stringResource(R.string.home_nc_ouvertes_detail, nc),
                        couleur = Red40,
                        onClick = { onNavigate(HomeNavigation.rappel(HomeNavigation.Rappel.NON_CONFORMITES)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccueilRappelLigne(texte: String, couleur: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(couleur))
        Spacer(Modifier.width(6.dp))
        Text(texte, color = HomeTextDark, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AccueilTachesCard(state: HomeUiState, onNavigate: (String) -> Unit) {
    val taches = state.taches.take(3)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HomeBorder),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(26.dp), shape = RoundedCornerShape(7.dp), color = HomeBlueSoft) {
                    Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(Iv.Checklist), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.home_taches_du_jour), color = HomeTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(7.dp))
            if (taches.isEmpty()) {
                Text(text = stringResource(R.string.home_no_tasks), color = HomeTextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp))
            } else {
                taches.forEachIndexed { idx, t ->
                    AccueilTacheRow(titre = t.titre, statut = t.statut, onClick = { onNavigate(Routes.TASKS) })
                    if (idx < taches.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onNavigate(Routes.TASKS) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.home_voir_toutes_taches), color = HomeBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(painter = painterResource(Iv.ChevronRight), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AccueilTacheRow(titre: String, statut: String, onClick: () -> Unit) {
    val terminee = statut == com.missa.b360.core.data.entity.TaskStatus.FAITE.name
    val enCours = statut == com.missa.b360.core.data.entity.TaskStatus.EN_COURS.name
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
            .clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (terminee) Iv.CheckCircle else if (enCours) Iv.Schedule else Iv.Checklist),
            contentDescription = null,
            tint = if (terminee) TendrePositive else if (enCours) ProfileOrange else HomeTextMuted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = titre, color = HomeTextDark, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(painterResource(Iv.ChevronRight), contentDescription = null, tint = HomeTextMuted, modifier = Modifier.size(15.dp))
    }
}


@Composable
internal fun MissaBusinessDrawer(
    companyName: String,
    logoUri: String?,
    backupStatus: String,
    currentRoute: String?,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onCompanyFiche: () -> Unit,
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
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Le vrai logo de la marque, et non une icône générique : le
                // tiroir est le seul endroit où l'application se présente, la
                // barre du haut appartenant désormais à l'entreprise.
                MissaBrandMark(size = 48.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "MISSA BUSINESS",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HomeTextDark,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "360",
                            fontSize = 14.5.sp,
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
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(Iv.Close),
                        contentDescription = stringResource(R.string.home_close),
                        tint = MissaInk,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCompanyFiche() },
                shape = RoundedCornerShape(14.dp),
                color = Blue90,
                border = BorderStroke(1.dp, HomeBorder),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Le conteneur et le contenu partagent la même taille :
                    // 42 dp d'un côté et 30 de l'autre décentraient la vignette.
                    CompanyLogo(
                        logoUri = logoUri,
                        contentDescription = null,
                        fallbackIcon = Iv.Store,
                        modifier = Modifier.size(36.dp),
                        size = 42.dp,
                        shape = CircleShape,
                        fallbackTint = HomeBlue,
                        fallbackBackground = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
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
                        painter = painterResource(Iv.ChevronRight),
                        contentDescription = null,
                        tint = MissaInk,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }


            DrawerSectionTitle(stringResource(R.string.drawer_section_administration))
            DrawerMenuItem(Iv.Settings, stringResource(R.string.home_settings), currentRoute == Routes.ADMIN_REGLAGES) {
                onNavigate(Routes.ADMIN_REGLAGES)
            }
            DrawerMenuItem(Iv.UserGear, stringResource(R.string.admin_utilisateurs), currentRoute == Routes.ADMIN_UTILISATEURS) {
                onNavigate(Routes.ADMIN_UTILISATEURS)
            }
            DrawerMenuItem(Iv.Security, stringResource(R.string.home_licence_activation), currentRoute == Routes.ADMIN_LICENCE) {
                onNavigate(Routes.ADMIN_LICENCE)
            }

            DrawerSectionTitle(stringResource(R.string.home_drawer_tools))
            DrawerMenuItem(Iv.Backup, stringResource(R.string.admin_sauvegarde), currentRoute == Routes.ADMIN_SAUVEGARDE) {
                onNavigate(Routes.ADMIN_SAUVEGARDE)
            }
            DrawerMenuItem(Iv.History, stringResource(R.string.admin_journal), currentRoute == Routes.ADMIN_JOURNAL) {
                onNavigate(Routes.ADMIN_JOURNAL)
            }

            DrawerSectionTitle(stringResource(R.string.home_drawer_support))
            DrawerMenuItem(Iv.HelpOutline, stringResource(R.string.home_help_assistance)) {
                onSupport()
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = HomeBackground,
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(26.dp),
                            shape = CircleShape,
                            color = HomeGreenSoft,
                        ) {
                            Icon(
                                painter = painterResource(Iv.CloudDone),
                                contentDescription = null,
                                tint = MissaInk,
                                modifier = Modifier.padding(5.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
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
        modifier = Modifier.padding(start = 7.dp, top = 12.dp, bottom = 5.dp),
    )
}

@Composable
private fun DrawerMenuItem(
    icon: Int,
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
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (selected) Color.White else Color.Transparent,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (selected) MissaInk else MissaMuted,
                    modifier = Modifier.padding(5.dp),
                )
            }
            Spacer(Modifier.width(7.dp))
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
    "PERSONNEL" -> R.string.profil_personnel
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(
                        if (aucunCanal) R.string.obn_code_a_configurer else R.string.home_support_intro,
                    ),
                    fontSize = 12.5.sp,
                    color = HomeTextMuted,
                )
                if (whatsappOk) {
                    HomeSupportBouton(R.string.obn_code_whatsapp, Iv.Chat) {
                        if (!ContactCommercial.ouvrirWhatsApp(contexte, numero, message)) {
                            Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                        }
                        onFermer()
                    }
                }
                if (telegramOk) {
                    HomeSupportBouton(
                        R.string.obn_code_telegram,
                        Iv.Send,
                    ) {
                        if (!ContactCommercial.ouvrirTelegram(contexte, telegram, message)) {
                            Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                        }
                        onFermer()
                    }
                }
                if (emailOk) {
                    HomeSupportBouton(R.string.obn_code_email, Iv.MailOutline) {
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
    icone: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, HomeBlue),
    ) {
        Icon(painterResource(icone), contentDescription = null, tint = HomeBlue, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        Surface(modifier = Modifier.size(24.dp), shape = RoundedCornerShape(7.dp), color = def.bg) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(def.icon), contentDescription = null, tint = def.tint, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(7.dp))
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

@Composable
private fun KpiPopup(
    titre: String,
    points: List<PointJour>,
    couleur: Color,
    explication: String,
    onFermer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFermer,
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.kpi_popup_fermer)) }
        },
        title = { Text(titre, fontWeight = FontWeight.Bold, fontSize = 14.5.sp) },
        text = {
            Column {
                if (points.size >= 2) {
                    CourbeEvolution(points, couleur, modifier = Modifier.fillMaxWidth().height(118.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(points.first().label, fontSize = 10.sp, color = HomeTextMuted)
                        Text(points.last().label, fontSize = 10.sp, color = HomeTextMuted)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(explication, fontSize = 12.sp, color = HomeTextMuted)
            }
        },
    )
}

/** Courbe d'évolution simple tracée à la main (Canvas) — aucune dépendance externe. */
@Composable
private fun CourbeEvolution(
    points: List<PointJour>,
    couleur: Color,
    modifier: Modifier = Modifier,
) {
    val maxV = points.maxOf { it.valeur }
    val minV = minOf(0.0, points.minOf { it.valeur })
    Canvas(modifier = modifier) {
        val pad = 12f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val n = points.size
        val amplitude = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
        fun pt(i: Int): Offset {
            val x = pad + w * i.toFloat() / (n - 1).coerceAtLeast(1)
            val y = pad + h * (1f - ((points[i].valeur - minV) / amplitude).toFloat())
            return Offset(x, y)
        }
        val chemin = Path().apply {
            for (i in points.indices) {
                val p = pt(i)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
        }
        drawPath(chemin, couleur, style = Stroke(width = 3.5f))
        for (i in points.indices) drawCircle(couleur, radius = 4.5f, center = pt(i))
    }
}
