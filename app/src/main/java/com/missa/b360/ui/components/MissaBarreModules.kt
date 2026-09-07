package com.missa.b360.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaSoftBlue

/**
 * Barre de navigation principale, partagée par l'accueil et les écrans-liste
 * des modules.
 *
 * Elle était jusqu'ici définie dans l'accueil seul : elle disparaissait donc
 * dès qu'on ouvrait un module, obligeant à revenir en arrière pour changer de
 * destination. Un composant unique garantit qu'elle est identique partout où
 * elle apparaît.
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
    Surface(color = Color.White, shadowElevation = 10.dp) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            // Sans cette contrainte, Material réserve deux fois la place de la
            // barre gestuelle : la barre paraît alors surélevée, décollée du
            // bord de l'écran.
            windowInsets = WindowInsets.navigationBars,
        ) {
            NavigationBarItem(
                selected = racine == com.missa.b360.ui.navigation.Routes.HOME,
                onClick = onAccueil,
                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_accueil), fontSize = 10.sp) },
            )
            modules.take(AppModule.MAX_ONGLETS).forEach { module ->
                NavigationBarItem(
                    selected = racine == module.route,
                    onClick = { onModule(module) },
                    icon = { Icon(module.icon, contentDescription = null) },
                    label = { Text(stringResource(module.titleRes), fontSize = 10.sp) },
                )
            }
            NavigationBarItem(
                selected = false,
                onClick = onPlus,
                icon = {
                    Surface(
                        modifier = Modifier.size(31.dp),
                        shape = CircleShape,
                        color = MissaSoftBlue,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = stringResource(R.string.more_modules),
                            tint = BrandBlue,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(R.string.home_more_short),
                        color = BrandBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}
