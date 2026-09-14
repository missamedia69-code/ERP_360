package com.missa.b360.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/**
 * Accueil Stock — hub cohérent avec l'Accueil (HomeScreen).
 * 1er module du bloc ACH-STK-VEN : tu achètes → t'as forcément un stock.
 * Respecte l'option venteSansStock (stock négatif autorisé ou non).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAccueilScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    viewModel: StockHubViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val depotChoisi by viewModel.depotChoisi.collectAsState()

    Scaffold(
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.module_stock),
                onBack = onBack,
            )
        },
        containerColor = MissaCanvas,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MissaCanvas)
                .padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Sélecteur dépôt (tous / site)
                Card(
                    shape = RoundedCornerShape(MissaLayout.cardRadius),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MissaBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.nomDepot(depots, depotChoisi) ?: stringResource(R.string.stk_tous_depots),
                                color = MissaInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = stringResource(R.string.stk_depot) + " · ${depots.size}",
                                color = MissaMuted,
                                fontSize = 11.sp,
                            )
                        }
                        if (depots.isNotEmpty()) {
                            Text(
                                text = "Changer",
                                color = BrandBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { viewModel.choisirDepot(if (depotChoisi == null) depots.firstOrNull()?.id else null) }
                                    .padding(6.dp),
                            )
                        }
                    }
                }
            }

            item {
                // Tableau de bord
                MissaPanel {
                    Text(
                        text = stringResource(R.string.stk_tableau_de_bord),
                        color = MissaInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StockKpi(
                            titre = stringResource(R.string.stk_valeur),
                            valeur = MoneyUtils.format(etat.valeurStock, devise),
                            sousTitre = stringResource(R.string.stk_valeur_detail),
                            modifier = Modifier.weight(1f),
                            onClick = { onNaviguer("module_stock") },
                        )
                        StockKpi(
                            titre = stringResource(R.string.stk_articles),
                            valeur = etat.nombreArticles.toString(),
                            sousTitre = stringResource(R.string.stk_articles_detail, etat.articlesSousSeuil.size),
                            modifier = Modifier.weight(1f),
                            onClick = { onNaviguer("module_stock") },
                        )
                    }
                    if (etat.articlesSousSeuil.isNotEmpty() || etat.ruptures > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MissaMuted.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (etat.ruptures > 0)
                                    stringResource(R.string.stk_a_traiter_rupture, etat.ruptures)
                                else
                                    stringResource(R.string.stk_a_traiter_seuil, etat.articlesSousSeuil.size),
                                color = Color(0xFFDC2626),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item {
                // Actions rapides — 4 tuiles
                Text(
                    text = stringResource(R.string.stk_actions),
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StockAction(
                        titre = "Entrée",
                        icone = Icons.Outlined.Add,
                        couleur = BrandBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("stock_movement?direction=ENTREE") },
                    )
                    StockAction(
                        titre = "Sortie",
                        icone = Icons.Outlined.SwapHoriz,
                        couleur = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("stock_movement?direction=SORTIE") },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StockAction(
                        titre = "Transfert",
                        icone = Icons.Outlined.SwapHoriz,
                        couleur = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("stock_transfert") },
                    )
                    StockAction(
                        titre = "Inventaire",
                        icone = Icons.Outlined.Assignment,
                        couleur = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("stock_inventory") },
                    )
                }
            }

            item {
                // Raccourcis
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StockRaccourci(
                        titre = stringResource(R.string.stk_articles),
                        valeur = etat.nombreArticles.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("module_stock") },
                    )
                    StockRaccourci(
                        titre = stringResource(R.string.stk_tuile_mouvements),
                        valeur = etat.derniersMouvements.size.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onNaviguer("stock_mouvements") },
                    )
                }
            }

            if (etat.derniersMouvements.isNotEmpty()) {
                item {
                    MissaPanel {
                        Text(
                            text = "Derniers mouvements",
                            color = MissaInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        etat.derniersMouvements.take(5).forEach { mv ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MissaCanvas, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MissaMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = mv.produitNom, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(text = "${mv.type} · ${mv.quantite}", color = MissaMuted, fontSize = 11.sp)
                                }
                                Text(text = mv.reference ?: "", color = MissaMuted, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.home_see_all),
                            color = BrandBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNaviguer("stock_mouvements") }.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockKpi(titre: String, valeur: String, sousTitre: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MissaCanvas),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = titre, color = MissaMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(text = valeur, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(text = sousTitre, color = MissaMuted, fontSize = 10.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StockAction(titre: String, icone: ImageVector, couleur: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = titre, color = MissaInk, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StockRaccourci(titre: String, valeur: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titre, color = MissaMuted, fontSize = 11.sp)
                Text(text = valeur, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
    }
}
