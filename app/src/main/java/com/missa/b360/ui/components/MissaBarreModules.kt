package com.missa.b360.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.TendrePositive

val LocalBarreNavigation = compositionLocalOf { mutableStateOf(true) }

/**
 * Barre de navigation principale — Spec UI MISSA BUSINESS 360
 *
 * - Hauteur applicative 80dp + navigationBars bottom inset dynamique
 * - 5 destinations: Accueil, Vente, Achats, Stock, Plus
 * - Chaque item: icon 24dp + label 11-12sp, zone tactile 48x48 minimum
 * - Responsive: weight(1f) par item, fillMaxWidth, pas de px fixes
 * - Limites bien marquées: bordure 1.5dp + dégradé 3dp + ombre douce 6dp + shadow 12dp
 * - Edge-to-edge: background peut aller sous zones système, content respecte Insets
 */
@Composable
fun MissaBarreModules(
    modules: List<AppModule>,
    routeCourante: String?,
    onAccueil: () -> Unit,
    onModule: (AppModule) -> Unit,
    onPlus: () -> Unit,
) {
    val racine = routeCourante?.substringBefore('?')
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Limite haute — 3 couches pour séparation nette avec zone scrollable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(com.missa.b360.ui.theme.MissaBorder),
                )
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
                // Contenu 80dp + bottom inset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(80.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarreOnglet(
                        modifier = Modifier.weight(1f),
                        icone = Icons.Outlined.Home,
                        libelleRes = R.string.nav_accueil,
                        actif = racine == Routes.HOME,
                        onClick = onAccueil,
                    )
                    modules.take(AppModule.MAX_ONGLETS).forEach { module ->
                        val libelleRes = when (module) {
                            AppModule.TRESORERIE -> R.string.module_finances
                            else -> module.titleRes
                        }
                        BarreOnglet(
                            modifier = Modifier.weight(1f),
                            icone = module.icon,
                            libelleRes = libelleRes,
                            actif = racine == module.route,
                            onClick = { onModule(module) },
                        )
                    }
                    BarreOnglet(
                        modifier = Modifier.weight(1f),
                        icone = Icons.Outlined.MoreHoriz,
                        libelleRes = R.string.home_more_short,
                        actif = false,
                        onClick = onPlus,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarreOnglet(
    modifier: Modifier = Modifier,
    icone: ImageVector,
    libelleRes: Int,
    actif: Boolean,
    onClick: () -> Unit,
) {
    val teinte = if (actif) BrandBlue else MissaMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(
            modifier = Modifier
                .height(3.dp)
                .width(if (actif) 24.dp else 0.dp)
                .background(BrandBlue, RoundedCornerShape(1.5.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Icon(icone, contentDescription = null, tint = teinte, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(libelleRes),
            color = teinte,
            fontSize = 11.sp,
            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
