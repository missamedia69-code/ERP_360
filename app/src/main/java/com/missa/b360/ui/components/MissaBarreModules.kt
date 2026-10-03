package com.missa.b360.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import com.missa.b360.ui.theme.MissaMuted
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.MissaInk

val LocalBarreNavigation = compositionLocalOf { mutableStateOf(true) }

/**
 * Navigation « pilule » : l'onglet actif devient un cartouche sombre avec son libellé,
 * tandis que les autres destinations restent des icônes noires compactes.
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
    val moduleCourant = when {
        racine == AppModule.STOCK.route || racine?.startsWith("stock") == true -> AppModule.STOCK
        racine == AppModule.CLIENTS.route || racine?.startsWith("module_clients") == true -> AppModule.CLIENTS
        else -> AppModule.entries.firstOrNull { it.route == racine }
    }
    val visibles = modules.take(AppModule.MAX_ONGLETS)
    val elements = buildList {
        add(BarreElement(Iv.Home, R.string.nav_accueil, onAccueil))
        visibles.forEach { module ->
            add(
                BarreElement(
                    icone = module.icon,
                    libelle = if (module == AppModule.TRESORERIE) R.string.module_finances else module.titleRes,
                    onClick = { onModule(module) },
                ),
            )
        }
        add(BarreElement(Iv.MoreHoriz, R.string.home_more_short, onPlus))
    }
    val indexActif = when {
        racine == Routes.HOME -> 0
        moduleCourant in visibles -> visibles.indexOf(moduleCourant) + 1
        else -> -1
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 7.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (moduleCourant == AppModule.CLIENTS) 64.dp else 58.dp)
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                elements.forEachIndexed { index, element ->
                    BarreOnglet(
                        element = element,
                        actif = index == indexActif,
                        couleurActive = if (moduleCourant == AppModule.CLIENTS) Color(0xFF7C3AED) else null,
                        modifier = if (index == indexActif && moduleCourant != AppModule.CLIENTS) Modifier.weight(1.55f) else Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private data class BarreElement(
    val icone: Int,
    val libelle: Int,
    val onClick: () -> Unit,
)

@Composable
private fun BarreOnglet(
    element: BarreElement,
    actif: Boolean,
    modifier: Modifier = Modifier,
    couleurActive: Color? = null,
) {
    val forme = RoundedCornerShape(16.dp)
    if (couleurActive != null) {
        // Variante Clients (maquette) : icône au-dessus du libellé pour tous les onglets, pastille violette pour l'actif.
        Column(
            modifier = modifier
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (actif) couleurActive else Color.Transparent)
                .clickable(role = Role.Tab, onClick = element.onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(
                painter = painterResource(element.icone),
                contentDescription = null,
                tint = if (actif) Color.White else MissaMuted,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(element.libelle),
                color = if (actif) Color.White else MissaMuted,
                fontSize = 10.sp,
                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }
    Box(
        modifier = modifier
            .height(46.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = .8f, stiffness = 500f))
            .clip(forme)
            .clickable(onClick = element.onClick)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(if (actif) Modifier.padding(horizontal = 3.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = if (actif) {
                Modifier
                    .clip(forme)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            } else {
                Modifier.padding(10.dp)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                color = if (actif) (couleurActive ?: MissaInk) else Color.Transparent,
                shape = forme,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (actif) 10.dp else 0.dp,
                        vertical = if (actif) 7.dp else 0.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(element.icone),
                        contentDescription = stringResource(element.libelle),
                        tint = if (actif) (if (couleurActive != null) MissaInk else Color.White) else MissaInk,
                        modifier = Modifier.size(22.dp),
                    )
                    if (actif) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(element.libelle),
                            color = if (couleurActive != null) MissaInk else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
