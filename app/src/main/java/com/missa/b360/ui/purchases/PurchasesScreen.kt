package com.missa.b360.ui.purchases

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
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
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
 * Hub Achats — cohérent avec Accueil et Stock.
 * 1er bloc ACH-STK-VEN : l'achat déclenche le stock, puis la vente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    viewModel: PurchasesHubViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()

    Scaffold(
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.module_achats),
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
                // KPIs
                MissaPanel {
                    Text(text = "Tableau de bord achats", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PurchaseKpi(
                            titre = stringResource(R.string.purchase_title),
                            valeur = etat.fournisseurs.toString(),
                            sousTitre = "fournisseurs",
                            modifier = Modifier.weight(1f),
                        )
                        PurchaseKpi(
                            titre = "Factures",
                            valeur = etat.factures.toString(),
                            sousTitre = MoneyUtils.format(etat.totalAchats, devise),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Text(text = "Actions rapides", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PurchaseAction(
                        titre = stringResource(R.string.purchase_new),
                        icone = Icons.Outlined.Add,
                        couleur = BrandBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.ouvrirCreation() },
                    )
                    PurchaseAction(
                        titre = "Fournisseurs",
                        icone = Icons.Outlined.Handshake,
                        couleur = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = {},
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PurchaseAction(
                        titre = "Commandes",
                        icone = Icons.Outlined.Receipt,
                        couleur = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = {},
                    )
                    PurchaseAction(
                        titre = "Stock",
                        icone = Icons.Outlined.Inventory2,
                        couleur = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = {},
                    )
                }
            }
            item {
                MissaPanel {
                    Text(text = "Réceptions récentes", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    if (etat.receptions.isEmpty()) {
                        Text(text = stringResource(R.string.purchase_list_empty), color = MissaMuted, fontSize = 12.sp)
                    } else {
                        etat.receptions.take(5).forEach { r ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MissaCanvas, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Receipt, contentDescription = null, tint = MissaMuted, modifier = Modifier.size(16.dp)) }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = r.fournisseur, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(text = r.reference, color = MissaMuted, fontSize = 11.sp)
                                }
                                Text(text = MoneyUtils.format(r.montant, devise), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseKpi(titre: String, valeur: String, sousTitre: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
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
private fun PurchaseAction(titre: String, icone: ImageVector, couleur: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = titre, color = MissaInk, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ViewModel hub minimal pour le hub Achats — agrégation simple
@dagger.hilt.android.lifecycle.HiltViewModel
class PurchasesHubViewModel @javax.inject.Inject constructor() : androidx.lifecycle.ViewModel() {
    private val _etat = kotlinx.coroutines.flow.MutableStateFlow(PurchasesHubEtat())
    val etat: kotlinx.coroutines.flow.StateFlow<PurchasesHubEtat> = _etat
    private val _devise = kotlinx.coroutines.flow.MutableStateFlow("XAF")
    val devise: kotlinx.coroutines.flow.StateFlow<String> = _devise
    fun ouvrirCreation() {}
    data class PurchasesHubEtat(
        val fournisseurs: Int = 0,
        val factures: Int = 0,
        val totalAchats: Double = 0.0,
        val receptions: List<Reception> = emptyList(),
    )
    data class Reception(val fournisseur: String, val reference: String, val montant: Double)
}
