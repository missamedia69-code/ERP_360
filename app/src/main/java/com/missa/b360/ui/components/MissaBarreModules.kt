package com.missa.b360.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * Permet à un écran de masquer temporairement la barre de navigation.
 *
 * Certains parcours occupent tout l'écran et posent leur propre bouton d'action
 * en bas — la saisie d'une vente, par exemple. La barre y apparaîtrait
 * juste en dessous, deux barres l'une sur l'autre. Plutôt que de dresser une
 * liste d'exceptions par route, l'écran concerné annonce lui-même qu'il prend
 * le bas de l'écran.
 */
val LocalBarreNavigation = compositionLocalOf { mutableStateOf(true) }

/**
 * Barre de navigation principale, partagée par tous les écrans-liste.
 *
 * Le dessin reprend celui qui existait dans le module Vente : une simple ligne
 * d'icônes surmontant un libellé, plus fine que la `NavigationBar` de Material
 * et sans pastille de sélection — la couleur suffit à désigner l'onglet actif.
 */
@Composable
fun MissaBarreModules(
    modules: List<AppModule>,
    routeCourante: String?,
    onAccueil: () -> Unit,
    onModule: (AppModule) -> Unit,
    onPlus: () -> Unit,
) {
    // La route enregistrée porte ses arguments (« module_vente?create={create} ») :
    // comparer les chaînes entières ne désignerait jamais l'onglet courant.
    val racine = routeCourante?.substringBefore('?')
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BarreOnglet(
                icone = Icons.Outlined.Home,
                libelleRes = R.string.nav_accueil,
                actif = racine == Routes.HOME,
                onClick = onAccueil,
            )
            modules.take(AppModule.MAX_ONGLETS).forEach { module ->
                BarreOnglet(
                    icone = module.icon,
                    libelleRes = module.titleRes,
                    actif = racine == module.route,
                    onClick = { onModule(module) },
                )
            }
            BarreOnglet(
                icone = Icons.Outlined.MoreHoriz,
                libelleRes = R.string.home_more_short,
                actif = false,
                onClick = onPlus,
            )
        }
    }
}

@Composable
private fun BarreOnglet(
    icone: ImageVector,
    libelleRes: Int,
    actif: Boolean,
    onClick: () -> Unit,
) {
    val teinte = if (actif) BrandBlue else MissaMuted
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Un trait fin marque l'onglet actif : plus discret que la pastille de
        // Material, il tient dans une barre de cette hauteur.
        Spacer(
            modifier = Modifier
                .height(2.dp)
                .width(if (actif) 20.dp else 0.dp)
                .background(BrandBlue, RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.height(3.dp))
        Icon(icone, contentDescription = null, tint = teinte, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(libelleRes),
            color = teinte,
            fontSize = 9.5.sp,
            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
