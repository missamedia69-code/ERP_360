package com.missa.b360.ui.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Maquette 2 — toutes les catégories de stock (types d'article) avec compteurs et valeurs. */
@Composable
fun StockCategoriesScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    val vm: StockAccueilViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_categories_titre), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(ProductType.entries.toList(), key = { it.name }) { type ->
                val ligne = etat.categories.firstOrNull { it.type == type }
                CarteStock(onClick = { onNaviguer(if (TYPES_EQUIPEMENTS.contains(type)) Routes.STOCK_EQUIPEMENTS else Routes.stockListe(type.name)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = Blue90) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(type.icone(), null, tint = BrandBlue, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(type.libelleCatRes()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                            )
                            Text(
                                text = stringResource(R.string.st_articles_count, ligne?.nombre ?: 0),
                                fontSize = 10.5.sp,
                                color = MissaMuted,
                            )
                        }
                        Text(
                            text = fmtValeur(ligne?.valeur ?: 0.0, etat.devise),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.ChevronRight,
                            null,
                            tint = MissaMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
