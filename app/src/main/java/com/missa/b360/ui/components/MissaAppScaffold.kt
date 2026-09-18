package com.missa.b360.ui.components

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.widthIn

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.TendrePositive

/**
 * Architecture globale d'écran — Spec UI MISSA BUSINESS 360
 *
 * Chaque écran principal respecte :
 * SYSTEM STATUS BAR (inset)
 * APP HEADER (carte blanche flottante sous la status bar)
 * APP CONTENT (scrollable seul)
 * BOTTOM NAVIGATION (carte blanche flottante au-dessus de la zone de geste)
 * SYSTEM NAVIGATION / GESTURE INSET
 *
 * Règles :
 * - Header et barre du bas majoritairement BLANCS : pas de bandeau ni de gradient bleu
 * - Ombres très légères, bordures fines, coins 18–26dp, aucune dimension en px
 * - Zones tactiles 48dp minimum, icônes 24dp, labels 11sp, titres 15sp
 * - Responsive : dp/sp + weight/fillMaxWidth + WindowInsets (360dp → 1080dp)
 */

/** Puce circulaire neutre commune au hamburger et à la cloche : lisible sans « gros fond coloré ». */
private val PuceHeader = MissaInk.copy(alpha = 0.05f)

/**
 * Header principal — carte blanche flottante, coins arrondis, ombre discrète.
 *
 * Structure : [☰] [logo MISSA BUSINESS 360] [nom + 360 + slogan] … [🔔] [bouton entreprise rond]
 *
 * - Hamburger : zone 48dp, icône bleu foncé (MissaInk), puce circulaire très pâle.
 * - Marque : logo officiel `logo_missa`, « MISSA BUSINESS » bleu foncé, « 360 » vert,
 *   slogan discret gris/bleu clair.
 * - Cloche : pastille rouge uniquement si notifications non lues.
 * - Entreprise : bouton parfaitement rond, fond très légèrement teinté de vert.
 *
 * Tous les clics conservent leur comportement d'origine (menu/retour, notifications, profil).
 */
@Composable
fun MissaAppHeader(
    companyLogoUri: String?,
    companyName: String,
    isHome: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hamburger / retour — zone tactile 48dp, icône bleu foncé équilibrée avec le logo.
                IconButton(
                    onClick = if (isHome) onMenuClick else onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PuceHeader),
                ) {
                    Icon(
                        painter = painterResource(if (isHome) Iv.Menu else Iv.ArrowBack),
                        contentDescription = if (isHome) stringResource(R.string.drawer_admin) else "Retour",
                        tint = MissaInk,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                // Logo officiel MISSA BUSINESS 360 — conteneur légèrement arrondi.
                Image(
                    painter = painterResource(R.drawable.logo_missa),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MISSA BUSINESS",
                        color = MissaInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 17.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "360",
                        color = TendrePositive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 17.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.app_slogan),
                        color = MissaMuted,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Nom de l'entreprise à côté de son logo — la cloche vit désormais
                // au niveau du « Bonjour » de l'accueil.
                Text(
                    text = companyName,
                    color = MissaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 130.dp),
                )
                Spacer(Modifier.width(6.dp))
                // Bouton entreprise — parfaitement rond, fond très légèrement teinté de vert.
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Green90),
                ) {
                    if (companyLogoUri != null) {
                        CompanyLogo(
                            logoUri = companyLogoUri,
                            contentDescription = stringResource(R.string.home_company_active),
                            fallbackIcon = Iv.Store,
                            modifier = Modifier.fillMaxSize(),
                            size = 48.dp,
                            shape = CircleShape,
                            fallbackTint = TendrePositive,
                            fallbackBackground = Green90,
                        )
                    } else {
                        Icon(
                            painter = painterResource(Iv.Store),
                            contentDescription = stringResource(R.string.home_company_active),
                            tint = TendrePositive,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Conteneur global réutilisable — Spec section 24 & 26
 * MainActivity -> App -> Scaffold { TopBar, NavHost, NavigationBar }
 * Header et BottomNav fixes, seul le contenu défile ; les insets système sont
 * gérés par les composants eux-mêmes (statusBars / navigationBars).
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // géré manuellement via WindowInsets.statusBars / navigationBars
        content = content,
    )
}
