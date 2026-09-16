package com.missa.b360.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.Blue80
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.TendrePositive

/**
 * Architecture globale d'écran — Spec UI MISSA BUSINESS 360
 *
 * Chaque écran principal respecte:
 * SYSTEM STATUS BAR (inset)
 * APP HEADER (64dp + statusBars)
 * APP CONTENT (scrollable seul)
 * BOTTOM NAVIGATION (80dp + navigationBars)
 * SYSTEM NAVIGATION / GESTURE INSET
 *
 * Règles:
 * - Background peut aller sous zones système, content respecte Insets (edge-to-edge)
 * - Header et BottomNav fixes, seul contenu défile
 * - dp/sp, WindowInsets, weight/fillMaxWidth, pas de px fixes
 * - Zones tactiles 48dp minimum, icônes 24dp, labels 11-12sp, titres 15-16sp
 * - Responsive: 360x800, 360x780, 390x844, 412x915, 720x1280, 1080x1920
 */

/**
 * Header principal fixe — 64dp contenu + statusBars inset
 * Structure: ☰ LOGO MISSA BUSINESS 🔔 👤
 * Padding horizontal 16dp, zones tactiles 48x48, icônes 24dp, logo 40x40, avatar 40x40, titre 15-16sp
 */
@Composable
fun MissaAppHeader(
    companyLogoUri: String?,
    notificationCount: Int,
    isHome: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
        ) {
            // Watermark logo entreprise — remplit arrière-plan avec Crop, rogne hors-cadre
            if (companyLogoUri != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.09f),
                ) {
                    CompanyLogo(
                        logoUri = companyLogoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Store,
                        modifier = Modifier.matchParentSize(),
                        size = 200.dp,
                        shape = RoundedCornerShape(0.dp),
                        fallbackTint = TendrePositive.copy(alpha = 0.12f),
                        fallbackBackground = Color.Transparent,
                    )
                }
            }
            // Halos décoratifs
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Green90.copy(alpha = 0.55f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(900f, 100f),
                            radius = 400f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Blue80.copy(alpha = 0.55f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(100f, 100f),
                            radius = 350f,
                        ),
                    ),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = if (isHome) onMenuClick else onBackClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isHome) Icons.Outlined.Menu else Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = if (isHome) stringResource(R.string.drawer_admin) else "Retour",
                            tint = MissaInk,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.logo_missa),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MISSA",
                                color = MissaInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 15.sp,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "BUSINESS",
                                color = MissaInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 15.sp,
                            )
                        }
                        Text(
                            text = "360",
                            color = TendrePositive,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 15.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onNotificationClick, modifier = Modifier.size(48.dp)) {
                        BadgedBox(
                            badge = {
                                if (notificationCount > 0) {
                                    Badge(containerColor = Red40, contentColor = Color.White) {
                                        Text(notificationCount.coerceAtMost(99).toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications),
                                tint = MissaInk,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        color = Color.White,
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
                                    contentDescription = null,
                                    tint = TendrePositive,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                // Limites bien marquées
                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(MissaBorder))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    BrandBlue.copy(alpha = 0.22f),
                                    TendrePositive.copy(alpha = 0.22f),
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.06f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

/**
 * Conteneur global réutilisable — Spec section 24 & 26
 * MainActivity -> App -> Scaffold { TopAppBar, NavHost, NavigationBar }
 * Header fixe 64dp + statusBars, Content scrollable, BottomNav fixe 80dp + navigationBars
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissaAppScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MissaCanvas,
        topBar = topBar,
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // On gère manuellement via WindowInsets.statusBars / navigationBars
        content = content,
    )
}
