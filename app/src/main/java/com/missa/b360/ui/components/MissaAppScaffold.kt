package com.missa.b360.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.widthIn

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
/**
 * En-tête mobile unifié selon la charte visuelle MISSA BUSINESS 360 :
 * - Compartiment gauche : Logo officiel circulaire MISSA + titre "MISSA BUSINESS 360" + slogan
 * - Séparateur vertical fin
 * - Compartiment droit : "Entreprise cliente" + Nom de l'entreprise + Badge activité (Commerce général...) + Médaillon logo/store
 * - Zéro chevauchement ni décalage grâce au partitionnement équilibré et aux contraintes bornées.
 */
@Composable
fun MissaAppHeader(
    companyLogoUri: String?,
    companyName: String,
    secteur: String = "",
    profilActivite: String? = null,
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
            border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Le menu possède sa propre cible tactile : le logo reste purement décoratif.
                IconButton(
                    onClick = { if (isHome) onMenuClick() else onBackClick() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = PuceHeader,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (isHome) Iv.Menu else Iv.ArrowBack),
                                contentDescription = stringResource(
                                    if (isHome) R.string.home_open_menu else R.string.ob_retour,
                                ),
                                tint = MissaInk,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(5.dp))
                MissaBrandMark(size = 40.dp)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(MissaBorder.copy(alpha = 0.55f)),
                )
                Spacer(Modifier.width(8.dp))

                // Compartiment Droit : Entreprise cliente
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onProfileClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Iv.Business),
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.header_entreprise_cliente),
                                fontSize = 9.sp,
                                color = MissaMuted,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = companyName.ifBlank { stringResource(R.string.home_company_placeholder) },
                            color = MissaInk,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val profilLibelle = when (profilActivite) {
                            "AV" -> R.string.profil_av
                            "ASV" -> R.string.profil_asv
                            "APSV" -> R.string.profil_apsv
                            "SER" -> R.string.profil_ser
                            "PRJ" -> R.string.profil_prj
                            "PERSONNEL" -> R.string.profil_personnel
                            "FULL" -> R.string.profil_full
                            "CUSTOM" -> R.string.profil_custom
                            else -> R.string.home_not_configured
                        }
                        val activiteLibelle = secteur.ifBlank { stringResource(profilLibelle) }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.padding(top = 1.dp),
                        ) {
                            Text(
                                text = activiteLibelle,
                                color = Color(0xFF0284C7),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    ) {
                        if (companyLogoUri != null) {
                            CompanyLogo(
                                logoUri = companyLogoUri,
                                contentDescription = stringResource(R.string.home_company_active),
                                fallbackIcon = Iv.Store,
                                modifier = Modifier.fillMaxSize(),
                                size = 38.dp,
                                shape = RoundedCornerShape(12.dp),
                                fallbackTint = Color(0xFF15803D),
                                fallbackBackground = Color(0xFFDCFCE7),
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    painter = painterResource(Iv.Store),
                                    contentDescription = stringResource(R.string.home_company_active),
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
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
