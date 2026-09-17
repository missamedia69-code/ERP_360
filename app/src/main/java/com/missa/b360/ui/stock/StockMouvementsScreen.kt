package com.missa.b360.ui.stock

import com.missa.b360.ui.navigation.AppModule

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
import androidx.compose.ui.text.style.TextAlign
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.R
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Maquette 7 — historique des mouvements groupés par jour, filtré par sens. */
@Composable
fun StockMouvementsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val vm: StockMouvementsViewModel = hiltViewModel()
    val groupes by vm.etat.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_mouvements_titre), onBack = onBack, couleurFond = AppModule.STOCK.couleurPale)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = { onNavigate(Routes.STOCK_MOVEMENT_FORM) },
                shape = RoundedCornerShape(10.dp),
                color = BrandBlue,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.st_nouveau_mouvement),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp),
                )
            }
            Surface(
                onClick = { onNavigate(Routes.STOCK_TRANSFER_FORM) },
                shape = RoundedCornerShape(10.dp),
                color = Blue90,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.st_transferts_stock),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StockChip(stringResource(R.string.st_tous), filtre == 0) { vm.setFiltre(0) }
            StockChip(stringResource(R.string.st_entrees), filtre == 1) { vm.setFiltre(1) }
            StockChip(stringResource(R.string.st_sorties), filtre == 2) { vm.setFiltre(2) }
            StockChip(stringResource(R.string.st_transferts), filtre == 3) { vm.setFiltre(3) }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item { Spacer(Modifier.height(10.dp)) }
            groupes.forEach { groupe ->
                item {
                    Text(
                        text = groupe.titreRes?.let { stringResource(it) } ?: groupe.titreTexte.orEmpty(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaMuted,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                items(groupe.lignes, key = { it.id }) { mv ->
                    LigneMouvement(mv)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun LigneMouvement(mv: StockMovementView) {
    val estEntree = mv.type == "ENTREE" || mv.type == "TRANSFERT_ENTREE"
    val estTransfert = mv.type == "TRANSFERT_SORTIE" || mv.type == "TRANSFERT_ENTREE"
    val (icone, teinte, fond) = when {
        estTransfert -> Triple(StockIv.Sync, MissaInk, AppModule.STOCK.couleurDouce)
        estEntree -> Triple(StockIv.TrendingUp, MissaInk, AppModule.STOCK.couleurDouce)
        else -> Triple(StockIv.TrendingDown, MissaInk, AppModule.STOCK.couleurDouce)
    }
    val titreType = when {
        estTransfert -> stringResource(R.string.st_mv_transfert)
        estEntree -> stringResource(R.string.st_mv_entree)
        else -> stringResource(R.string.st_mv_sortie)
    }
    val signe = if (mv.type == "SORTIE" || mv.type == "TRANSFERT_SORTIE") "-" else "+"
    CarteStock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = fond) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icone), null, tint = teinte, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$titreType · ${mv.motif}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                    maxLines = 1,
                )
                Text(text = mv.produitNom, fontSize = 10.5.sp, color = MissaMuted, maxLines = 1)
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(mv.horodatage)),
                    fontSize = 9.5.sp,
                    color = MissaMuted,
                )
            }
            Text(
                text = "$signe${fmtQuantite(kotlin.math.abs(mv.quantite))}",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = teinte,
            )
        }
    }
}
