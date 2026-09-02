package com.missa.b360.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockStatus
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.BrandBlue

/** Couleur hexadécimale stockée en base. */
fun stockColor(hex: String?, fallback: Color = BrandBlue): Color {
    val value = hex?.trim().orEmpty()
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

/** Option d'un sélecteur (produit, entrepôt, catégorie…). */
data class StockPickerOption(val value: String, val label: String)

/** Sélecteur matériel léger : une carte cliquable qui ouvre un menu d'options. */
@Composable
fun StockPickerField(
    label: String,
    value: String,
    options: List<StockPickerOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MissaBorder),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                val display = if (value.isNotBlank()) value
                else if (placeholder.isNotBlank()) placeholder
                else stringResource(R.string.stk_picker_select)
                Text(label, fontSize = 10.sp, color = MissaMuted)
                Text(
                    display,
                    fontSize = 13.sp,
                    color = if (value.isBlank()) MissaMuted else MissaInk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MissaMuted,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(.85f),
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.value)
                    },
                )
            }
        }
    }
}

/** Pastille de statut produit (En stock / Stock faible / Rupture). */
@Composable
fun StockStatusChip(status: StockStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        StockStatus.STOCK -> stringResource(R.string.stk_status_stock) to Green60
        StockStatus.LOW_STOCK -> stringResource(R.string.stk_status_low) to BrandBlue
        StockStatus.OUT -> stringResource(R.string.stk_status_out) to Red40
    }
    SurfacePill(
        label = label,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun SurfacePill(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Tuile d'indicateur compacte (tableau de bord). */
@Composable
fun StockStatTile(title: String, value: String, accent: Color = BrandBlue, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, color = MissaInk, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(title, color = MissaMuted, fontSize = 9.sp, maxLines = 1)
            Spacer(Modifier.height(1.dp))
            Box(
                Modifier
                    .width(22.dp)
                    .height(4.dp)
                    .background(accent, RoundedCornerShape(50)),
            )
        }
    }
}

/** État vide standard du module Stock. */
@Composable
fun StockEmptyPanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MissaSoftBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Assessment, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, color = MissaMuted, fontSize = 11.sp, maxLines = 2)
            }
        }
    }
}

/** Bouton primaire plein largeur, cohérent avec les autres modules. */
@Composable
fun StockPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Libellé d'un type de mouvement (fragment de langue stable, non composable). */
fun stockMovementLabel(type: StockMovementType): String = when (type) {
    StockMovementType.ENTRY -> "Entrée"
    StockMovementType.EXIT -> "Sortie"
    StockMovementType.TRANSFER_IN, StockMovementType.TRANSFER_OUT -> "Transfert"
    StockMovementType.ADJUSTMENT -> "Ajustement"
}

/** Convertit un mouvement en libellé de produit (durable). */
fun movementProductLabel(movement: StockMovementEntity, products: List<StockProductEntity>): String =
    products.firstOrNull { it.id == movement.productId }?.nom ?: "Produit #${movement.productId}"

fun quantityLabel(q: Double): String =
    if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()

fun signedQuantityLabel(delta: Double): String {
    val value = quantityLabel(kotlin.math.abs(delta))
    return if (delta >= 0.0) "+$value" else "−$value"
}

/** Modèle d'option d'entrepôt pour les sélecteurs. */
fun warehouseOption(id: Long, nom: String): StockPickerOption =
    StockPickerOption(id.toString(), nom)

fun categoryOption(id: Long, nom: String): StockPickerOption =
    StockPickerOption(id.toString(), nom)
