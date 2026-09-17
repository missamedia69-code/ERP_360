package com.missa.b360.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/** Maquette 1 — accueil du module Stock : valorisation, tuiles, catégories, mouvements du jour. */
@Composable
fun StockAccueilScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    val vm: StockAccueilViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MissaCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = stringResource(R.string.module_stock), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
        Text(text = stringResource(R.string.st_sous_titre), fontSize = 11.5.sp, color = MissaMuted)
        Spacer(Modifier.height(12.dp))

        // Valeur totale du stock.
        CarteStock(onClick = { onNaviguer(Routes.stockListe(null)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Green90) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Inventory2, null, tint = Green60, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.st_valeur_totale), fontSize = 11.sp, color = MissaMuted)
                    Text(
                        text = fmtValeur(etat.valeur, etat.devise),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MissaInk,
                    )
                    etat.tendance?.let { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (t >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                null,
                                tint = if (t >= 0) Green60 else Red40,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.st_tendance, "%+.1f %%".format(t * 100)),
                                fontSize = 10.5.sp,
                                color = if (t >= 0) Green60 else Red40,
                            )
                        }
                    }
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MissaMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icone = Icons.Outlined.Inventory2,
                teinte = BrandBlue,
                fond = Blue90,
                valeur = groupe(etat.nbArticles.toLong()),
                libelle = stringResource(R.string.st_articles),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.stockListe(null)) },
            )
            StatTile(
                icone = Icons.Outlined.Category,
                teinte = Green60,
                fond = Green90,
                valeur = etat.nbCategories.toString(),
                libelle = stringResource(R.string.st_categories),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_CATEGORIES) },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icone = Icons.Outlined.Warning,
                teinte = ProfileOrange,
                fond = Color(0xFFFFF4E5),
                valeur = etat.critiques.toString(),
                libelle = stringResource(R.string.st_stock_critique),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
            StatTile(
                icone = Icons.Outlined.Error,
                teinte = Red40,
                fond = Red80,
                valeur = etat.ruptures.toString(),
                libelle = stringResource(R.string.st_ruptures),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
        }

        // Catégories de stock (types d'articles).
        StockSectionTitle(
            titre = stringResource(R.string.st_categories_titre),
            action = stringResource(R.string.st_voir_tout),
            onAction = { onNaviguer(Routes.STOCK_CATEGORIES) },
        )
        etat.categories.filter { it.nombre > 0 }.take(6).forEach { cat ->
            CarteStock(onClick = { onNaviguer(Routes.stockListe(cat.type.name)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = Blue90) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(cat.type.icone(), null, tint = BrandBlue, modifier = Modifier.size(17.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(cat.nomRes), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        Text(stringResource(R.string.st_articles_count, cat.nombre), fontSize = 10.5.sp, color = MissaMuted)
                    }
                    Text(fmtValeur(cat.valeur, etat.devise), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MissaMuted)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.ChevronRight, null, tint = MissaMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Mouvements du jour.
        StockSectionTitle(titre = stringResource(R.string.st_mouvements_jour))
        CarteStock(onClick = { onNaviguer(Routes.STOCK_MOUVEMENTS) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MouvementMini(Icons.Outlined.TrendingUp, Green60, stringResource(R.string.st_entrees), etat.entreesJour, Modifier.weight(1f))
                MouvementMini(Icons.Outlined.TrendingDown, Red40, stringResource(R.string.st_sorties), etat.sortiesJour, Modifier.weight(1f))
                MouvementMini(Icons.Outlined.Sync, ProfilePurple, stringResource(R.string.st_transferts), etat.transfertsJour, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MouvementMini(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    teinte: Color,
    libelle: String,
    nombre: Int,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MissaCanvas) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone, null, tint = teinte, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(libelle, fontSize = 10.5.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(3.dp))
            Text(nombre.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = teinte)
        }
    }
}

