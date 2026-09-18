package com.missa.b360.ui.stock

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import com.missa.b360.ui.theme.MissaBorder

import androidx.compose.foundation.BorderStroke

import com.missa.b360.ui.navigation.AppModule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
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
            .background(MissaCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Header du module : bandeau légèrement teinté de la couleur Stock.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppModule.STOCK.couleurPale,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(text = stringResource(R.string.module_stock), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
                Text(text = stringResource(R.string.st_sous_titre), fontSize = 11.5.sp, color = MissaMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Catégories en petits onglets matriciels : accès direct aux listes.
        StockSectionTitle(
            titre = stringResource(R.string.st_categories_titre),
            action = stringResource(R.string.st_voir_tout),
            onAction = { onNaviguer(Routes.STOCK_CATEGORIES) },
        )
        etat.categories.chunked(4).forEach { ligneCats ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ligneCats.forEach { cat ->
                    TuileCategorieMatrice(
                        icone = cat.type.icone(),
                        nom = stringResource(cat.nomRes),
                        nombre = stringResource(R.string.st_articles_count, cat.nombre),
                        modifier = Modifier.weight(1f),
                    ) {
                        onNaviguer(
                            if (TYPES_EQUIPEMENTS.contains(cat.type)) Routes.STOCK_EQUIPEMENTS
                            else Routes.stockListe(cat.type.name),
                        )
                    }
                }
                repeat(4 - ligneCats.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))

        // Valeur totale du stock.
        CarteStock(onClick = { onNaviguer(Routes.stockListe(null)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Green90) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(StockIv.Coins), null, tint = MissaInk, modifier = Modifier.size(20.dp))
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
                                painterResource(if (t >= 0) StockIv.TrendingUp else StockIv.TrendingDown),
                                null,
                                tint = MissaInk,
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
                Icon(painterResource(StockIv.ChevronRight), null, tint = MissaInk, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icone = StockIv.Inventory2,
                teinte = MissaInk,
                fond = Blue90,
                valeur = groupe(etat.nbArticles.toLong()),
                libelle = stringResource(R.string.st_articles),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.stockListe(null)) },
            )
            StatTile(
                icone = StockIv.Category,
                teinte = MissaInk,
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
                icone = StockIv.Warning,
                teinte = MissaInk,
                fond = Color(0xFFFFF4E5),
                valeur = etat.critiques.toString(),
                libelle = stringResource(R.string.st_stock_critique),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
            StatTile(
                icone = StockIv.Error,
                teinte = MissaInk,
                fond = Red80,
                valeur = etat.ruptures.toString(),
                libelle = stringResource(R.string.st_ruptures),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
        }


        // Accès inventaire physique.
        CarteStock(onClick = { onNaviguer(Routes.STOCK_INVENTORY) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Blue90) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(StockIv.ClipboardText), null, tint = MissaInk, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.st_inventaire_titre), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(stringResource(R.string.st_demarrer_inventaire), fontSize = 10.5.sp, color = MissaMuted)
                }
                Icon(painterResource(StockIv.ChevronRight), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            }
        }

        // Mouvements du jour.
        StockSectionTitle(titre = stringResource(R.string.st_mouvements_jour))
        CarteStock(onClick = { onNaviguer(Routes.STOCK_MOUVEMENTS) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MouvementMini(StockIv.TrendingUp, Green60, stringResource(R.string.st_entrees), etat.entreesJour, Modifier.weight(1f))
                MouvementMini(StockIv.TrendingDown, Red40, stringResource(R.string.st_sorties), etat.sortiesJour, Modifier.weight(1f))
                MouvementMini(StockIv.Sync, ProfilePurple, stringResource(R.string.st_transferts), etat.transfertsJour, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MouvementMini(
    icone: Int,
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
                Icon(painterResource(icone), null, tint = teinte, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(libelle, fontSize = 10.5.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(3.dp))
            Text(nombre.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = teinte)
        }
    }
}


/** Petit onglet matriciel d'une catégorie : icône, nom et nombre d'articles. */
@Composable
private fun TuileCategorieMatrice(
    icone: Int,
    nom: String,
    nombre: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(9.dp),
                color = AppModule.STOCK.couleurDouce,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = nom,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(text = nombre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}
