package com.missa.b360.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import kotlin.math.roundToInt

val LocalBarreNavigation = compositionLocalOf { mutableStateOf(true) }

/** Barre blanche à encoche mobile. L'onglet actif flotte dans une bulle au-dessus de la barre. */
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
        racine == AppModule.CLIENTS.route || racine?.startsWith("clients") == true -> AppModule.CLIENTS
        else -> AppModule.entries.firstOrNull { it.route == racine }
    }
    val visibles = modules.take(AppModule.MAX_ONGLETS)
    val icones = buildList {
        add(Iv.Home to R.string.nav_accueil)
        visibles.forEach { add(it.icon to if (it == AppModule.TRESORERIE) R.string.module_finances else it.titleRes) }
        add(Iv.MoreHoriz to R.string.home_more_short)
    }
    val indexActif = when {
        racine == Routes.HOME -> 0
        moduleCourant != null && visibles.contains(moduleCourant) -> visibles.indexOf(moduleCourant) + 1
        else -> -1
    }
    val positionCible = if (indexActif >= 0) indexActif + .5f else .5f
    val positionAnimee by animateFloatAsState(
        targetValue = positionCible,
        animationSpec = spring(dampingRatio = .72f, stiffness = 420f),
        label = "encocheNavigation",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp),
    ) {
        val largeurPx = with(LocalDensity.current) { maxWidth.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val top = 18.dp.toPx()
            val centre = size.width * positionAnimee / icones.size
            val demiEncoche = 38.dp.toPx()
            val profondeur = 25.dp.toPx()
            val p = Path().apply {
                moveTo(0f, top)
                lineTo(centre - demiEncoche, top)
                cubicTo(
                    centre - 23.dp.toPx(), top,
                    centre - 27.dp.toPx(), top + profondeur,
                    centre, top + profondeur,
                )
                cubicTo(
                    centre + 27.dp.toPx(), top + profondeur,
                    centre + 23.dp.toPx(), top,
                    centre + demiEncoche, top,
                )
                lineTo(size.width, top)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(p, Color.White)
            drawPath(p, MissaBorder.copy(alpha = .45f), style = Stroke(1.dp.toPx()))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            icones.forEachIndexed { index, (icone, libelle) ->
                val actif = index == indexActif
                val clic = when (index) {
                    0 -> onAccueil
                    icones.lastIndex -> onPlus
                    else -> ({ onModule(visibles[index - 1]) })
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(onClick = clic)
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!actif) {
                        Icon(
                            painter = painterResource(icone),
                            contentDescription = stringResource(libelle),
                            tint = MissaMuted.copy(alpha = .48f),
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }

        if (indexActif >= 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = (largeurPx * positionAnimee / icones.size - 28.dp.toPx()).roundToInt(),
                            y = 0,
                        )
                    }
                    .size(56.dp)
                    .clickable {
                        when (indexActif) {
                            0 -> onAccueil()
                            icones.lastIndex -> onPlus()
                            else -> onModule(visibles[indexActif - 1])
                        }
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 9.dp,
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icones[indexActif].first),
                        contentDescription = stringResource(icones[indexActif].second),
                        tint = MissaInk,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
        }
    }
}
