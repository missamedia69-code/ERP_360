package com.missa.b360.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
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
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/** Maquette 3 — liste d'articles d'un type : recherche, chips catégories, badges de niveau. */
@Composable
fun StockScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    initialMovement: com.missa.b360.core.data.entity.StockMovementType? = null,
) {
    val vm: StockListeViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MissaTopAppBar(
                title = stringResource(etat.type?.libelleCatRes() ?: R.string.module_stock),
                onBack = onBack,
            )
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                StockSearchField(
                    valeur = etat.requete,
                    onValeur = vm::chercher,
                    placeholderRes = R.string.st_rechercher_article,
                )
                Spacer(Modifier.height(10.dp))
                // Chips : Tous + catégories utilisateur.
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StockChip(
                        texte = stringResource(R.string.st_tous),
                        actif = etat.categorieId == null,
                        onClick = { vm.filtrerCategorie(null) },
                    )
                    etat.categories.forEach { cat ->
                        StockChip(
                            texte = cat.nom,
                            actif = etat.categorieId == cat.id,
                            onClick = { vm.filtrerCategorie(if (etat.categorieId == cat.id) null else cat.id) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.st_articles_count, etat.articles.size),
                        fontSize = 11.sp,
                        color = MissaMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = fmtValeur(etat.valeur, etat.devise),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MissaInk,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (etat.articles.isEmpty()) {
                    MissaEmptyState(
                        icon = StockIv.Add,
                        title = stringResource(R.string.st_aucun_resultat),
                        description = stringResource(R.string.module_placeholder),
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                } else {
                    LazyColumn {
                        items(etat.articles, key = { it.product.id }) { ligne ->
                            CarteArticle(ligne = ligne, devise = etat.devise) {
                                onNavigate(Routes.stockDetail(ligne.product.id))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { onNavigate(Routes.STOCK_PRODUCT_FORM) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Green60,
            contentColor = Color.White,
        ) {
            Icon(painterResource(StockIv.Add, contentDescription = stringResource(R.string.st_nouvel_article), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CarteArticle(ligne: ProductWithStock, devise: String, onClick: () -> Unit) {
    CarteStock(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Blue90) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(ligne.product.type.icone()), null, tint = BrandBlue, modifier = Modifier.size(21.dp))
                }
            }
            ProduitImage(photoPath = ligne.product.photoPath)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ligne.nom, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
                Text(
                    text = ligne.reference?.takeIf { it.isNotBlank() } ?: ligne.code,
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
                Text(
                    text = stringResource(R.string.st_stock_ligne, fmtQuantite(ligne.stock)),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = fmtValeur(ligne.prixVente ?: 0.0, devise),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(4.dp))
                BadgeNiveau(ligne)
            }
        }
    }
}

/** Badge OK / Stock faible / Rupture / Stock critique. */
@Composable
fun BadgeNiveau(ligne: ProductWithStock) {
    val (texteRes, teinte, fond) = when {
        ligne.stock <= 0.0 -> Triple(R.string.st_rupture, Red40, Red80)
        ligne.level == StockLevel.CRITIQUE -> Triple(R.string.st_stock_critique, Red40, Red80)
        ligne.level == StockLevel.BAS -> Triple(R.string.st_stock_faible, ProfileOrange, Color(0xFFFFF4E5))
        else -> Triple(R.string.st_ok, Green60, Green90)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = fond) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (texteRes == R.string.st_stock_faible) {
                Icon(painterResource(StockIv.Star, null, tint = teinte, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(stringResource(texteRes), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = teinte)
        }
    }
}
