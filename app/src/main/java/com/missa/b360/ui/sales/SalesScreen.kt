package com.missa.b360.ui.sales

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bleu royal caractéristique du module Vente — source unique : [AppModule.VENTE]. */
private val BleuVente: Color get() = AppModule.VENTE.couleur

private enum class EcranVente { LISTE, FACTURE }

@Composable
fun SalesScreen(
    onNavigate: (String) -> Unit = {},
    onOpenClientCreate: () -> Unit = {},
    openCreate: Boolean = false,
) {
    val vm: SalesViewModel = hiltViewModel()
    var ecran by remember { mutableStateOf(if (openCreate) EcranVente.FACTURE else EcranVente.LISTE) }
    var pieceAAnnuler by remember { mutableStateOf<OperationRecordEntity?>(null) }
    val saveResult by vm.saveResult.collectAsStateWithLifecycle()

    LaunchedEffect(saveResult) {
        if (saveResult is SalesViewModel.SaveResult.Saved) {
            ecran = EcranVente.LISTE
        }
    }

    when (ecran) {
        EcranVente.LISTE -> ListeVentes(
            vm = vm,
            onNouvelleVente = {
                vm.clearCart()
                ecran = EcranVente.FACTURE
            },
            onOuvrirFacture = { ecran = EcranVente.FACTURE },
            onAnnuler = { pieceAAnnuler = it },
        )
        EcranVente.FACTURE -> FormulaireVente(
            vm = vm,
            onBack = { ecran = EcranVente.LISTE },
            onOpenClientCreate = onOpenClientCreate,
        )
    }

    pieceAAnnuler?.let { piece ->
        AlertDialog(
            onDismissRequest = { pieceAAnnuler = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.ach_confirmer_annulation, piece.reference), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelSale(piece.id)
                    pieceAAnnuler = null
                }) { Text(stringResource(R.string.ach_annuler), color = Color(0xFFB91C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { pieceAAnnuler = null }) {
                    Text(stringResource(R.string.st_annuler), color = MissaInk)
                }
            },
        )
    }
}

@Composable
private fun ListeVentes(
    vm: SalesViewModel,
    onNouvelleVente: () -> Unit,
    onOuvrirFacture: () -> Unit,
    onAnnuler: (OperationRecordEntity) -> Unit,
) {
    val pieces by vm.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val clients by vm.clients.collectAsStateWithLifecycle(initialValue = emptyList())
    val devise by vm.devise.collectAsStateWithLifecycle()

    val validees = pieces.filter { it.status == OperationStatus.VALIDATED.name }
    val caTotal = validees.sumOf { it.amount ?: 0.0 }
    val brouillons = pieces.count { it.status == OperationStatus.DRAFT.name }
    var filtreStatut by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_vente),
            onBack = null,
            couleurFond = AppModule.VENTE.couleurPale,
        )
        if (pieces.isEmpty()) {
            MissaEmptyState(
                icon = Iv.ShoppingCart,
                title = stringResource(R.string.module_vente),
                description = stringResource(R.string.sales_empty_desc),
                modifier = Modifier.padding(16.dp),
                action = {
                    Button(
                        onClick = onNouvelleVente,
                        colors = ButtonDefaults.buttonColors(containerColor = BleuVente, contentColor = Color.White),
                    ) { Text(stringResource(R.string.sales_new_sale)) }
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = BleuVente.copy(alpha = 0.16f)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                stringResource(R.string.module_vente),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.sales_total_sales), fontSize = 11.sp, color = MissaMuted)
                                Text(fmtValeur(caTotal, devise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.ach_brouillons), fontSize = 11.sp, color = MissaMuted)
                                Text(brouillons.toString(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MissaInk)
                            }
                        }
                    }
                }
                // --- Structure Matricielle 4 Tuiles ---
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TuileVente(
                            icone = Iv.ShoppingCart,
                            titre = stringResource(R.string.crm_tuile_tous),
                            sousTitre = pieces.size.toString(),
                            estActif = filtreStatut == null,
                            modifier = Modifier.weight(1f),
                            onClick = { filtreStatut = null },
                        )
                        TuileVente(
                            icone = Iv.CheckCircle,
                            titre = stringResource(R.string.sales_tab_history),
                            sousTitre = validees.size.toString(),
                            estActif = filtreStatut == OperationStatus.VALIDATED.name,
                            modifier = Modifier.weight(1f),
                            onClick = { filtreStatut = OperationStatus.VALIDATED.name },
                        )
                        TuileVente(
                            icone = Iv.Edit,
                            titre = stringResource(R.string.ach_brouillons),
                            sousTitre = brouillons.toString(),
                            estActif = filtreStatut == OperationStatus.DRAFT.name,
                            modifier = Modifier.weight(1f),
                            onClick = { filtreStatut = OperationStatus.DRAFT.name },
                        )
                        TuileVente(
                            icone = Iv.Add,
                            titre = stringResource(R.string.sales_new_sale),
                            sousTitre = stringResource(R.string.st_creer),
                            estActif = false,
                            modifier = Modifier.weight(1f),
                            onClick = onNouvelleVente,
                        )
                    }
                }
                val piecesAffichees = pieces.filter { filtreStatut == null || it.status == filtreStatut }
                items(piecesAffichees, key = { it.id }) { piece ->
                    CartePieceVente(
                        piece = piece,
                        devise = devise,
                        onReprendre = {
                            if (vm.loadDraft(piece, clients)) onOuvrirFacture()
                        },
                        onAnnuler = { onAnnuler(piece) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartePieceVente(
    piece: OperationRecordEntity,
    devise: String,
    onReprendre: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val dateStr = remember(piece.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(piece.createdAt))
    }
    val payload = remember(piece.notes) { SaleRecordCodec.decode(piece.notes) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(piece.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                BadgeStatut(piece.status)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(payload?.clientName ?: piece.counterpart.orEmpty(), fontSize = 12.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Text(fmtValeur(piece.amount ?: 0.0, devise), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
            }
            Text(dateStr, fontSize = 10.sp, color = MissaMuted)
            if (piece.status == OperationStatus.DRAFT.name) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onReprendre,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BleuVente, contentColor = Color.White),
                ) { Text(stringResource(R.string.ach_reprendre), fontSize = 11.sp) }
            } else if (piece.status == OperationStatus.VALIDATED.name) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onAnnuler,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.ach_annuler), color = Color(0xFFB91C1C), fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun BadgeStatut(statut: String) {
    val (fond, texte, libelleRes) = when (statut) {
        OperationStatus.VALIDATED.name -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), R.string.ach_valide)
        OperationStatus.DRAFT.name -> Triple(Color(0xFFF3F4F6), MissaMuted, R.string.ach_brouillon)
        OperationStatus.CANCELLED.name -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), R.string.ach_annulee)
        else -> Triple(Color(0xFFF3F4F6), MissaMuted, R.string.ach_brouillon)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = fond) {
        Text(stringResource(libelleRes), color = texte, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun FormulaireVente(
    vm: SalesViewModel,
    onBack: () -> Unit,
    onOpenClientCreate: () -> Unit,
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val produits by vm.products.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle(initialValue = emptyList())
    val modes by vm.paymentMethods.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val taxRate by vm.taxRate.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val saveResult by vm.saveResult.collectAsStateWithLifecycle()

    var modePaiement by remember { mutableStateOf("") }
    LaunchedEffect(modes) { if (modePaiement.isBlank()) modePaiement = modes.firstOrNull().orEmpty() }

    val totals = ui.totals(taxRate)

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.sales_new_sale),
            onBack = onBack,
            couleurFond = AppModule.VENTE.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SelecteurClient(
                    selectedClient = ui.selectedClient,
                    clients = clients,
                    onSelect = vm::selectClient,
                    onOpenClientCreate = onOpenClientCreate,
                )
            }
            item {
                BlocCatalogueVente(
                    produits = produits,
                    devise = devise,
                    onAjouter = vm::addCatalogProduct,
                )
            }
            if (ui.lines.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.ach_panier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
            }
            items(ui.lines, key = { "ligne-${it.id}" }) { ligne ->
                LignePanierVente(
                    ligne = ligne,
                    devise = devise,
                    onQuantite = { delta -> vm.changeQuantity(ligne.id, delta) },
                    onPrix = { p -> vm.updateLine(ligne.id, ligne.quantity, p) },
                    onSupprimer = { vm.removeLine(ligne.id) },
                )
            }
            if (ui.lines.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = BleuVente.copy(alpha = 0.12f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.sales_subtotal), fontSize = 11.sp, color = MissaMuted)
                                Text(fmtValeur(totals.subtotal, devise), fontSize = 12.sp, color = MissaInk)
                            }
                            if (totals.taxAmount > 0.0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.ach_tva_incluse, taxRate), fontSize = 11.sp, color = MissaMuted)
                                    Text(fmtValeur(totals.taxAmount, devise), fontSize = 12.sp, color = MissaInk)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.ach_total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                                Text(fmtValeur(totals.total, devise), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                        }
                    }
                }
                item {
                    SelecteurModePaiement(
                        modes = modes,
                        selectionne = modePaiement,
                        onChoix = { modePaiement = it },
                    )
                }
            }
        }
        BarreActionsVente(
            busy = saving,
            valideActive = ui.selectedClient != null && ui.lines.isNotEmpty() && !saving,
            onBrouillon = { vm.save(modePaiement, draft = true) },
            onValider = { vm.save(modePaiement, draft = false) },
            erreur = when (saveResult) {
                SalesViewModel.SaveResult.MissingClient -> stringResource(R.string.sales_err_client_required)
                SalesViewModel.SaveResult.EmptyCart -> stringResource(R.string.ach_erreur_panier)
                SalesViewModel.SaveResult.InvalidAmount -> stringResource(R.string.ach_erreur_montant)
                SalesViewModel.SaveResult.ReadOnly -> stringResource(R.string.ach_erreur_lecture_seule)
                is SalesViewModel.SaveResult.StockInsuffisant -> {
                    val res = saveResult as SalesViewModel.SaveResult.StockInsuffisant
                    stringResource(R.string.sales_err_stock_insufficient, res.produitNom, fmtQuantite(res.disponible))
                }
                SalesViewModel.SaveResult.Error -> stringResource(R.string.ach_erreur)
                else -> null
            },
        )
    }
}

@Composable
private fun SelecteurClient(
    selectedClient: ClientEntity?,
    clients: List<ClientEntity>,
    onSelect: (ClientEntity) -> Unit,
    onOpenClientCreate: () -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedClient?.nom ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.sales_select_client), fontSize = 11.sp, color = MissaMuted) },
            trailingIcon = { Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaInk) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { ouvert = true })
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            DropdownMenuItem(
                text = { Text("+ " + stringResource(R.string.clients_nouveau_client), fontWeight = FontWeight.Bold, color = BleuVente) },
                onClick = {
                    ouvert = false
                    onOpenClientCreate()
                },
            )
            clients.forEach { client ->
                DropdownMenuItem(
                    text = { Text(client.nom, color = MissaInk) },
                    onClick = {
                        onSelect(client)
                        ouvert = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BlocCatalogueVente(
    produits: List<ProductWithStock>,
    devise: String,
    onAjouter: (ProductWithStock) -> Unit,
) {
    var recherche by remember { mutableStateOf("") }
    OutlinedTextField(
        value = recherche,
        onValueChange = { recherche = it },
        label = { Text(stringResource(R.string.ach_rechercher), fontSize = 11.sp, color = MissaMuted) },
        leadingIcon = { Icon(painterResource(Iv.Search), null, tint = MissaMuted, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    val filtres = produits.filter {
        recherche.isBlank() || it.product.nom.contains(recherche.trim(), ignoreCase = true)
    }
    filtres.take(20).forEach { produit ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MissaBorder),
            modifier = Modifier.fillMaxWidth().clickable { onAjouter(produit) },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(produit.product.nom, fontSize = 13.sp, color = MissaInk)
                    Text(
                        fmtValeur(produit.product.prixVente ?: 0.0, devise) + " · Stock: " + fmtQuantite(produit.stock),
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
                Icon(painterResource(Iv.Add), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LignePanierVente(
    ligne: SaleLine,
    devise: String,
    onQuantite: (Double) -> Unit,
    onPrix: (Double) -> Unit,
    onSupprimer: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.name, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(Iv.DeleteOutline), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantite(-1.0) }, modifier = Modifier.size(36.dp)) {
                    Text("−", fontSize = 18.sp, color = MissaInk)
                }
                Text(fmtQuantite(ligne.quantity), fontSize = 13.sp, color = MissaInk)
                IconButton(onClick = { onQuantite(1.0) }, modifier = Modifier.size(36.dp)) {
                    Text("+", fontSize = 18.sp, color = MissaInk)
                }
                Spacer(Modifier.width(8.dp))
                Text(fmtValeur(ligne.unitPrice * ligne.quantity, devise), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MissaInk)
            }
        }
    }
}

@Composable
private fun SelecteurModePaiement(
    modes: List<String>,
    selectionne: String,
    onChoix: (String) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectionne,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ach_mode_paiement), fontSize = 11.sp, color = MissaMuted) },
            trailingIcon = { Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaInk) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { ouvert = true })
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode, color = MissaInk) },
                    onClick = {
                        onChoix(mode)
                        ouvert = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BarreActionsVente(
    busy: Boolean,
    valideActive: Boolean,
    onBrouillon: () -> Unit,
    onValider: () -> Unit,
    erreur: String?,
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            if (erreur != null) {
                Text(
                    text = erreur,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB91C1C),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBrouillon,
                    enabled = valideActive && !busy,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.ach_brouillon), color = MissaInk) }
                Button(
                    onClick = onValider,
                    enabled = valideActive && !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BleuVente, contentColor = Color.White),
                ) { Text(stringResource(R.string.ach_valider), color = Color.White) }
            }
        }
    }
}

internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { java.util.Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(java.util.Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}

@Composable
private fun TuileVente(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) BleuVente.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) BleuVente else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}
