package com.missa.b360.ui.stock

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40

/**
 * Module Stock (spec §50) : produits avec stock courant dérivé des mouvements,
 * historique des mouvements, et accès aux formulaires mouvement / transfert.
 */
@Composable
fun StockScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    initialMovement: StockMovementType? = null,
    viewModel: StockViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val movements by viewModel.movements.collectAsState(initial = emptyList())
    val devise by viewModel.devise.collectAsState()
    var tab by rememberSaveable { mutableStateOf("PRODUITS") }
    var search by rememberSaveable { mutableStateOf("") }

    // Actions rapides de l'accueil : ouvrir directement le formulaire demandé.
    LaunchedEffect(Unit) {
        initialMovement?.let { onNavigate("${Routes.STOCK_MOVEMENT_FORM}?type=${it.name}") }
    }

    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.module_stock),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onNavigate(
                        if (tab == "PRODUITS") {
                            "${Routes.STOCK_PRODUCT_FORM}?productId=0"
                        } else {
                            "${Routes.STOCK_MOVEMENT_FORM}?type=${StockMovementType.ENTREE.name}"
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(
                            if (tab == "PRODUITS") R.string.stock_new_product else R.string.stock_new_movement,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MissaLayout.screenHorizontal, vertical = MissaLayout.screenVertical / 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = tab == "PRODUITS",
                    onClick = { tab = "PRODUITS" },
                    label = { Text(stringResource(R.string.stock_tab_products)) },
                )
                FilterChip(
                    selected = tab == "MOUVEMENTS",
                    onClick = { tab = "MOUVEMENTS" },
                    label = { Text(stringResource(R.string.stock_tab_movements)) },
                )
                if (tab == "MOUVEMENTS") {
                    OutlinedButton(onClick = { onNavigate(Routes.STOCK_TRANSFER_FORM) }) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.stock_new_transfer))
                    }
                }
                if (tab == "PRODUITS") {
                    OutlinedButton(onClick = { onNavigate(Routes.STOCK_INVENTORY) }) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.stock_new_inventory))
                    }
                }
            }
            if (tab == "PRODUITS") {
                ProductsTabContent(
                    products = products,
                    search = search,
                    onSearchChange = { search = it },
                    devise = devise,
                    onOpenProduct = { product ->
                        onNavigate("${Routes.STOCK_PRODUCT_FORM}?productId=${product.product.id}")
                    },
                    onNewProduct = { onNavigate("${Routes.STOCK_PRODUCT_FORM}?productId=0") },
                    modifier = Modifier.weight(1f),
                )
            } else {
                MovementsTabContent(
                    movements = movements,
                    onNewMovement = { onNavigate("${Routes.STOCK_MOVEMENT_FORM}?type=${StockMovementType.ENTREE.name}") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProductsTabContent(
    products: List<ProductWithStock>,
    search: String,
    onSearchChange: (String) -> Unit,
    devise: String,
    onOpenProduct: (ProductWithStock) -> Unit,
    onNewProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = products.filter { product ->
        search.isBlank() ||
            product.nom.contains(search, ignoreCase = true) ||
            product.code.contains(search, ignoreCase = true) ||
            (product.reference?.contains(search, ignoreCase = true) ?: false) ||
            (product.barcode?.contains(search, ignoreCase = true) ?: false)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MissaLayout.screenHorizontal,
            end = MissaLayout.screenHorizontal,
            top = 0.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.stock_search_products)) },
            )
        }
        if (visible.isEmpty()) {
            item {
                MissaEmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.stock_products_empty),
                    description = stringResource(R.string.stock_products_empty_description),
                    action = { TextButton(onClick = onNewProduct) { Text(stringResource(R.string.stock_new_product)) } },
                )
            }
        } else {
            items(visible, key = { it.product.id }) { product ->
                ProductRow(product = product, devise = devise, onClick = { onOpenProduct(product) })
            }
        }
    }
}

@Composable
private fun ProductRow(product: ProductWithStock, devise: String, onClick: () -> Unit) {
    val (badgeColor, badgeLabel) = when (product.level) {
        StockLevel.CRITIQUE -> Red40 to stringResource(R.string.stock_badge_critical)
        StockLevel.BAS -> ProfileOrange to stringResource(R.string.stock_badge_low)
        StockLevel.OK -> Green60 to stringResource(R.string.stock_badge_ok)
    }
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = MissaSoftBlue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nom,
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(product.code, product.reference, product.unite)
                        .joinToString(" · ").ifBlank { product.product.type.name },
                    color = MissaMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                product.prixVente?.let { prix ->
                    Text(
                        text = MoneyUtils.format(prix, devise),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            stringResource(R.string.stock_badge_value, badgeLabel, product.stock.displayQuantity()),
                            fontSize = 10.sp,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = badgeColor,
                        disabledContainerColor = badgeColor.copy(alpha = 0.12f),
                    ),
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun MovementsTabContent(
    movements: List<StockMovementView>,
    onNewMovement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MissaLayout.screenHorizontal,
            end = MissaLayout.screenHorizontal,
            top = 0.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
    ) {
        item {
            MissaSectionTitle(
                title = stringResource(R.string.stock_tab_movements),
                subtitle = stringResource(R.string.stock_movements_subtitle),
            )
        }
        if (movements.isEmpty()) {
            item {
                MissaEmptyState(
                    icon = Icons.Outlined.TrendingUp,
                    title = stringResource(R.string.stock_movements_empty),
                    description = stringResource(R.string.stock_movements_empty_description),
                    action = { TextButton(onClick = onNewMovement) { Text(stringResource(R.string.stock_new_movement)) } },
                )
            }
        } else {
            items(movements, key = { it.id }) { movement ->
                MovementRow(movement)
            }
        }
    }
}

@Composable
private fun MovementRow(movement: StockMovementView) {
    val type = runCatching { StockMovementType.valueOf(movement.type) }.getOrDefault(StockMovementType.ENTREE)
    val (icon, sign, color) = when (type) {
        StockMovementType.ENTREE, StockMovementType.TRANSFERT_ENTREE ->
            Icons.Outlined.TrendingUp to "+" to Green60
        StockMovementType.SORTIE, StockMovementType.TRANSFERT_SORTIE ->
            Icons.Outlined.TrendingDown to "−" to Red40
        StockMovementType.AJUSTEMENT ->
            Icons.Outlined.SwapVert to if (movement.quantite >= 0) "+" else "−" to ProfileOrange
    }
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.produitNom,
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        movement.motif,
                        movement.siteNom,
                        movement.reference?.let { stringResource(R.string.stock_movement_ref, it) },
                    ).joinToString(" · "),
                    color = MissaMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateUtils.formatDateHeure(movement.horodatage),
                    color = MissaMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                movement.commentaire?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MissaMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "$sign${movement.quantite.abs().displayQuantity()}",
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Double.displayQuantity(): String =
    if (this % 1.0 == 0.0) toInt().toString() else java.text.DecimalFormat("0.##").format(this)

private fun Double.abs(): Double = if (this < 0) -this else this
