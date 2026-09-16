package com.missa.b360.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaMuted

val LocalBarreNavigation = compositionLocalOf { mutableStateOf(true) }

/**
 * Barre de navigation principale — carte blanche flottante.
 *
 * - 5 destinations : Accueil + 3 modules du pack ([AppModule.barreBas]) + Plus.
 * - Flotte au-dessus du contenu : coins 26dp, ombre légère, bordure fine, aucun fond bleu massif.
 * - Respecte la zone de navigation / geste Android ([WindowInsets.navigationBars]).
 * - Onglet actif : icône + libellé bleu MISSA sur pastille bleu extrêmement pâle,
 *   petit indicateur bleu sous l'élément ; inactif : gris/bleu gris.
 * - Zones tactiles ≥ 48dp, icônes 24dp, libellés 11sp, `weight(1f)` par onglet.
 *
 * La navigation elle-même est inchangée : mêmes callbacks, mêmes routes.
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
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

/**
 * Onglet de la barre : pastille bleu pâle + indicateur bleu sous l'élément actif,
 * gris/bleu gris sinon. La hauteur de l'indicateur est toujours réservée pour
 * que les onglets actifs et inactifs restent alignés.
 */
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
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 56.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (actif) BrandBlue.copy(alpha = 0.10f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icone, contentDescription = null, tint = teinte, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(3.dp))
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
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(if (actif) 20.dp else 0.dp)
                .background(BrandBlue, RoundedCornerShape(1.5.dp)),
        )
    }
}
