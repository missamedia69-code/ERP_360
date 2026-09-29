package com.missa.b360.ui.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.components.*

@Composable
internal fun ClientPricesSection(
    prices: List<PriceClientEntity>,
    products: List<ProductEntity>,
    devise: String,
    onSave: (Long, Double) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MissaBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.clients_flow_negotiated_prices), modifier = Modifier.weight(1f), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(onClick = { editorOpen = true }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Icon(painterResource(Iv.Add), null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.clients_flow_add_price), fontSize = 9.sp)
                }
            }
            if (prices.isEmpty()) {
                Text(stringResource(R.string.clients_flow_no_prices), color = MissaMuted, fontSize = 10.sp)
            } else {
                prices.forEach { price ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(products.firstOrNull { it.id == price.produitId }?.nom ?: stringResource(R.string.clients_flow_unknown_product), color = MissaInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(fmtValeur(price.prix, devise), color = MissaMuted, fontSize = 9.sp)
                        }
                        IconButton(onClick = { onDelete(price.produitId) }, modifier = Modifier.size(26.dp)) {
                            Icon(painterResource(Iv.DeleteOutline), stringResource(R.string.clients_flow_remove_price), tint = MissaInk, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
    if (editorOpen) {
        ClientPriceDialog(products, devise, onDismiss = { editorOpen = false }, onSave = { productId, price ->
            onSave(productId, price)
            editorOpen = false
        })
    }
}

@Composable
private fun ClientPriceDialog(
    products: List<ProductEntity>,
    devise: String,
    onDismiss: () -> Unit,
    onSave: (Long, Double) -> Unit,
) {
    var productId by remember { mutableStateOf<Long?>(products.firstOrNull { it.active && it.vendable }?.id) }
    var priceInput by remember { mutableStateOf("") }
    val price = priceInput.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val sellable = products.filter { it.active && it.vendable }

    MissaFormDialogue(
        titre = stringResource(R.string.clients_flow_add_price),
        icone = Iv.Payments,
        couleur = AppModule.CLIENTS.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.clients_enregistrer),
        validerActif = productId != null && price != null,
        onValider = { productId?.let { id -> price?.let { onSave(id, it) } } },
    ) {
        MissaRangee {
            MissaChampListe(
                libelle = stringResource(R.string.clients_flow_product),
                options = sellable.map { it.id to "${it.code} · ${it.nom}" },
                selection = productId,
                onSelection = { productId = it },
                icone = Iv.Inventory2,
                requis = true,
                placeholder = stringResource(R.string.clients_flow_choose_product),
                modifier = Modifier.weight(1f),
            )
            MissaChampTexte(priceInput, { priceInput = it }, stringResource(R.string.clients_flow_negotiated_price), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, requis = true, suffixe = devise, longueurMax = 14, modifier = Modifier.weight(1f))
        }
    }
}
