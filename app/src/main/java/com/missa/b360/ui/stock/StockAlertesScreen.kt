package com.missa.b360.ui.stock

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/** Maquette 10 — alertes stock : ruptures et articles sous seuil, filtrables. */
@Composable
fun StockAlertesScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    val vm: StockAlertesViewModel = hiltViewModel()
    val alertes by vm.etat.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_alertes_titre), onBack = onBack, couleurFond = AppModule.STOCK.couleurPale)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StockChip(stringResource(R.string.st_toutes), filtre == 0) { vm.setFiltre(0) }
            StockChip(stringResource(R.string.st_stock_critique), filtre == 1) { vm.setFiltre(1) }
            StockChip(stringResource(R.string.st_ruptures), filtre == 2) { vm.setFiltre(2) }
        }
        if (alertes.isEmpty()) {
            MissaEmptyState(
                icon = StockIv.Notifications,
                title = stringResource(R.string.st_ok),
                description = stringResource(R.string.st_aucun_resultat),
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item { Spacer(Modifier.height(10.dp)) }
                items(alertes, key = { it.product.id }) { ligne ->
                    CarteStock(onClick = { onNaviguer(Routes.stockDetail(ligne.product.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (ligne.stock <= 0) Red80 else Color(0xFFFFF4E5),
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painterResource(ligne.product.type.icone()),
                                        null,
                                        tint = if (ligne.stock <= 0) Red40 else ProfileOrange,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ligne.nom, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
                                Text(
                                    text = stringResource(
                                        R.string.st_stock_min_format,
                                        fmtQuantite(ligne.stock),
                                        fmtQuantite(ligne.stockMin),
                                    ),
                                    fontSize = 10.5.sp,
                                    color = MissaMuted,
                                )
                            }
                            BadgeNiveau(ligne)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
