package com.missa.b360.ui.purchases

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.util.filterMoneyInput
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

/** Jaune caractéristique du module Achats — source unique : [AppModule.ACHATS]. */
private val JauneAchats: Color get() = AppModule.ACHATS.couleur

/** Écran interne du module : liste des pièces ou formulaire, sans nouvelle route. */
private enum class EcranAchat { LISTE, FORMULAIRE }

/**
 * Module Achats (spec §6) — factures fournisseur.
 *
 * - Liste : synthèse (dépenses validées, passif fournisseur, brouillons) + pièces ;
 *   un brouillon se reprend, une pièce validée se déplie (lignes, règlement, passif).
 * - Formulaire : fournisseur, catalogue achetable (services inclus), panier avec
 *   traçabilité lot/série/péremption, règlement — brouillon ou validation.
 *
 * La validation est transactionnelle côté domaine : stock + CUMP + mouvements,
 * décaissement trésorerie, notification « réception à contrôler ».
 */
@Composable
fun PurchasesScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    val vm: PurchasesViewModel = hiltViewModel()
    var ecran by remember { mutableStateOf(if (openCreate) EcranAchat.FORMULAIRE else EcranAchat.LISTE) }
    when (ecran) {
        EcranAchat.LISTE -> ListeAchats(
            vm = vm,
            onBack = onBack,
            onNouveau = {
                vm.clearCart()
                ecran = EcranAchat.FORMULAIRE
            },
            onFormulaire = { ecran = EcranAchat.FORMULAIRE },
        )
        EcranAchat.FORMULAIRE -> FormulaireAchat(
            vm = vm,
            onBack = { ecran = EcranAchat.LISTE },
            onTermine = { ecran = EcranAchat.LISTE },
        )
    }
}

// ---------------------------------------------------------------------------
// Liste des pièces
// ---------------------------------------------------------------------------

@Composable
private fun ListeAchats(
    vm: PurchasesViewModel,
    onBack: () -> Unit,
    onNouveau: () -> Unit,
    onFormulaire: () -> Unit,
) {
    val pieces by vm.purchases.collectAsStateWithLifecycle(initial = emptyList())
    val devise by vm.devise.collectAsStateWithLifecycle()
    var pieceOuverte by remember { mutableStateOf<Long?>(null) }
    val fournisseurs by vm.suppliers.collectAsStateWithLifecycle()

    val validees = pieces.filter { it.status == OperationStatus.VALIDATED.name }
    val depenses = validees.sumOf { it.amount }
    val passif = validees.sumOf { piece ->
        val payload = PurchaseRecordCodec.decode(piece.notes)
        (piece.amount - (payload?.paidAmount ?: 0.0)).coerceAtLeast(0.0)
    }
    val brouillons = pieces.count { it.status == OperationStatus.DRAFT.name }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_achats),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        if (pieces.isEmpty()) {
            MissaEmptyState(
                icon = Iv.CartArrowDown,
                title = stringResource(R.string.ach_aucune),
                description = stringResource(R.string.ach_aucune_desc),
                modifier = Modifier.padding(16.dp),
                action = {
                    Button(
                        onClick = onNouveau,
                        colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                    ) { Text(stringResource(R.string.ach_nouvelle)) }
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                stringResource(R.string.module_achats),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                            )
                            Spacer(Modifier.height(6.dp))
                            LigneSynthese(stringResource(R.string.ach_depenses), fmtValeur(depenses, devise))
                            LigneSynthese(stringResource(R.string.ach_passif), fmtValeur(passif, devise))
                            LigneSynthese(stringResource(R.string.ach_brouillons), brouillons.toString())
                        }
                    }
                }
                item {
                    Button(
                        onClick = onNouveau,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                    ) {
                        Icon(Iv.Add, null, tint = MissaInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ach_nouvelle), color = MissaInk)
                    }
                }
                items(pieces, key = { it.id }) { piece ->
                    CartePiece(
                        piece = piece,
                        devise = devise,
                        ouvert = pieceOuverte == piece.id,
                        onToggle = { pieceOuverte = if (pieceOuverte == piece.id) null else piece.id },
                        onReprendre = {
                            if (vm.loadDraft(piece, fournisseurs)) onFormulaire()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LigneSynthese(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, fontSize = 12.sp, color = MissaInk, modifier = Modifier.weight(1f))
        Text(valeur, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
    }
}

@Composable
private fun CartePiece(
    piece: OperationRecordEntity,
    devise: String,
    ouvert: Boolean,
    onToggle: () -> Unit,
    onReprendre: () -> Unit,
) {
    val payload = remember(piece.notes) { PurchaseRecordCodec.decode(piece.notes) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(piece.reference, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(
                        "${piece.counterpart} · ${fmt.format(Date(piece.createdAt))}",
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
                BadgeStatut(piece.status)
                Spacer(Modifier.width(6.dp))
                Text(fmtValeur(piece.amount, devise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Icon(
                    if (ouvert) Iv.ExpandLess else Iv.ExpandMore,
                    null,
                    tint = MissaInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (ouvert) {
                Spacer(Modifier.height(8.dp))
                if (piece.status == OperationStatus.DRAFT.name) {
                    OutlinedButton(onClick = onReprendre, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ach_reprendre), color = MissaInk)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                payload?.lines?.forEach { ligne -> LignePiece(ligne, devise) }
                if (payload != null) {
                    Spacer(Modifier.height(4.dp))
                    LigneSynthese(stringResource(R.string.ach_regle), fmtValeur(payload.paidAmount, devise))
                    LigneSynthese(
                        stringResource(R.string.ach_passif),
                        fmtValeur((payload.total - payload.paidAmount).coerceAtLeast(0.0), devise),
                    )
                    if (payload.paidAmount > 0.0) {
                        Text(
                            stringResource(R.string.ach_decaissement_enregistre),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                    payload.note?.let {
                        Text(it, fontSize = 11.sp, color = MissaMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun LignePiece(ligne: PurchaseLine, devise: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(Modifier.weight(1f)) {
            Text(ligne.name, fontSize = 12.sp, color = MissaInk)
            val trace = listOfNotNull(
                ligne.lot?.takeIf { it.isNotBlank() },
                ligne.numeroSerie?.takeIf { it.isNotBlank() },
                ligne.datePeremption?.let {
                    stringResource(R.string.ach_peremption) + " " +
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                },
            )
            if (trace.isNotEmpty()) {
                Text(trace.joinToString(" · "), fontSize = 10.sp, color = MissaMuted)
            }
        }
        Text(
            "${fmtQuantite(ligne.quantity)} × ${fmtValeur(ligne.unitPrice, devise)}",
            fontSize = 11.sp,
            color = MissaMuted,
        )
    }
}

@Composable
private fun BadgeStatut(statut: String) {
    val (texte, fond) = when (statut) {
        OperationStatus.DRAFT.name -> stringResource(R.string.ach_brouillon) to JauneAchats.copy(alpha = 0.45f)
        OperationStatus.CANCELLED.name -> stringResource(R.string.ach_annulee) to Color(0xFFE5E7EB)
        else -> stringResource(R.string.ach_valide) to Color(0xFFDCFCE7)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = fond) {
        Text(texte, fontSize = 10.sp, color = MissaInk, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

// ---------------------------------------------------------------------------
// Formulaire — facture fournisseur
// ---------------------------------------------------------------------------

@Composable
private fun FormulaireAchat(
    vm: PurchasesViewModel,
    onBack: () -> Unit,
    onTermine: () -> Unit,
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val produits by vm.products.collectAsStateWithLifecycle()
    val fournisseurs by vm.suppliers.collectAsStateWithLifecycle()
    val modes by vm.paymentMethods.collectAsStateWithLifecycle()
    val taux by vm.taxRate.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val resultat by vm.saveResult.collectAsStateWithLifecycle()

    var recherche by remember { mutableStateOf("") }
    var modePaiement by remember { mutableStateOf("") }
    var ligneOuverte by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(modes) { if (modePaiement.isBlank()) modePaiement = modes.firstOrNull().orEmpty() }

    // Seule une sauvegarde réussie quitte l'écran ; une erreur reste affichée
    // jusqu'à la prochaine tentative (le VM la réinitialise au début de save()).
    LaunchedEffect(resultat) {
        if (resultat is PurchasesViewModel.SaveResult.Saved) {
            vm.clearSaveResult()
            onTermine()
        }
    }

    val total = vm.total()
    val tva = if (taux == 0.0) 0.0 else total * taux / (100.0 + taux)
    val peutValider = ui.supplier != null && ui.lines.isNotEmpty() && total > 0.0 &&
        modePaiement.isNotBlank() && !busy

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.ach_nouvelle),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Fournisseur ---
            item {
                Selecteur(
                    libelle = stringResource(R.string.ach_fournisseur),
                    valeur = ui.supplier?.nom,
                    options = fournisseurs.map { it.nom },
                    onChoix = { index -> vm.selectSupplier(fournisseurs[index]) },
                )
            }

            // --- Catalogue ---
            item {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text(stringResource(R.string.ach_rechercher), fontSize = 11.sp, color = MissaMuted) },
                    leadingIcon = { Icon(Iv.Search, null, tint = MissaMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val filtres = produits.filter {
                recherche.isBlank() || it.product.nom.contains(recherche.trim(), ignoreCase = true)
            }
            items(filtres.take(30), key = { "cat-${it.product.id}" }) { produit ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MissaBorder),
                    modifier = Modifier.fillMaxWidth().clickable { vm.addCatalogProduct(produit) },
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(produit.product.nom, fontSize = 13.sp, color = MissaInk)
                            Text(
                                produit.product.prixAchat?.let { fmtValeur(it, devise) } ?: "—",
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                        if (produit.product.type == ProductType.PRESTATION) {
                            Surface(shape = RoundedCornerShape(8.dp), color = JauneAchats.copy(alpha = 0.45f)) {
                                Text(
                                    stringResource(R.string.ach_service),
                                    fontSize = 10.sp,
                                    color = MissaInk,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Iv.Add, null, tint = MissaInk, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // --- Panier ---
            if (ui.lines.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.ach_panier),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                    )
                }
            }
            items(ui.lines, key = { "ligne-${it.id}" }) { ligne ->
                LignePanier(
                    ligne = ligne,
                    ouvert = ligneOuverte == ligne.id,
                    onToggle = { ligneOuverte = if (ligneOuverte == ligne.id) null else ligne.id },
                    onQuantite = { delta -> vm.changeQuantity(ligne.id, delta) },
                    onPrix = { prix -> vm.updateLine(ligne.id, ligne.quantity, prix) },
                    onSupprimer = { vm.removeLine(ligne.id) },
                    onTrace = { lot, serie, peremption -> vm.updateLineTrace(ligne.id, lot, serie, peremption) },
                )
            }

            // --- Règlement ---
            if (ui.lines.isNotEmpty()) {
                item {
                    Selecteur(
                        libelle = stringResource(R.string.ach_mode_paiement),
                        valeur = modePaiement.ifBlank { null },
                        options = modes,
                        onChoix = { index -> modePaiement = modes[index] },
                    )
                }
                item {
                    OutlinedTextField(
                        value = ui.paidInput,
                        onValueChange = vm::updatePaid,
                        label = { Text(stringResource(R.string.ach_montant_regle), fontSize = 11.sp, color = MissaMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = ui.note,
                        onValueChange = vm::updateNote,
                        label = { Text(stringResource(R.string.ach_note), fontSize = 11.sp, color = MissaMuted) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            LigneSynthese(stringResource(R.string.ach_total), fmtValeur(total, devise))
                            if (taux > 0.0) {
                                LigneSynthese(
                                    stringResource(R.string.ach_tva_incluse, taux),
                                    fmtValeur(tva, devise),
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Barre d'actions ---
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                val erreur = when (resultat) {
                    PurchasesViewModel.SaveResult.MissingSupplier -> stringResource(R.string.ach_erreur_fournisseur)
                    PurchasesViewModel.SaveResult.EmptyCart -> stringResource(R.string.ach_erreur_panier)
                    PurchasesViewModel.SaveResult.InvalidAmount -> stringResource(R.string.ach_erreur_montant)
                    PurchasesViewModel.SaveResult.FournisseurIntrouvable -> stringResource(R.string.ach_erreur_fournisseur)
                    PurchasesViewModel.SaveResult.ReadOnly -> stringResource(R.string.ach_erreur_lecture_seule)
                    PurchasesViewModel.SaveResult.Error -> stringResource(R.string.ach_erreur)
                    else -> null
                }
                if (!erreur.isNullOrBlank()) {
                    Text(erreur, fontSize = 11.sp, color = Color(0xFFB91C1C), modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.save(modePaiement, draft = true) },
                        enabled = ui.supplier != null && ui.lines.isNotEmpty() && !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.ach_brouillon), color = MissaInk) }
                    Button(
                        onClick = { vm.save(modePaiement, draft = false) },
                        enabled = peutValider,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                    ) { Text(stringResource(R.string.ach_valider), color = MissaInk) }
                }
            }
        }
    }
}

@Composable
private fun LignePanier(
    ligne: PurchaseLine,
    ouvert: Boolean,
    onToggle: () -> Unit,
    onQuantite: (Double) -> Unit,
    onPrix: (Double) -> Unit,
    onSupprimer: () -> Unit,
    onTrace: (lot: String, serie: String, peremption: Long?) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.name, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer, modifier = Modifier.size(32.dp)) {
                    Icon(Iv.DeleteOutline, null, tint = MissaInk, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (ouvert) Iv.ExpandLess else Iv.ExpandMore,
                        null,
                        tint = MissaInk,
                        modifier = Modifier.size(20.dp),
                    )
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
                var prixTexte by remember(ligne.id, ligne.unitPrice) {
                    mutableStateOf(ligne.unitPrice.toString())
                }
                OutlinedTextField(
                    value = prixTexte,
                    onValueChange = { brut ->
                        prixTexte = brut.filterMoneyInput()
                        prixTexte.toDoubleOrNull()?.let(onPrix)
                    },
                    label = { Text(stringResource(R.string.ach_prix_unitaire), fontSize = 11.sp, color = MissaMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            if (ouvert) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.ach_traceabilite),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaMuted,
                )
                Spacer(Modifier.height(4.dp))
                var lot by remember(ligne.id, ligne.lot) { mutableStateOf(ligne.lot.orEmpty()) }
                var serie by remember(ligne.id, ligne.numeroSerie) { mutableStateOf(ligne.numeroSerie.orEmpty()) }
                OutlinedTextField(
                    value = lot,
                    onValueChange = { lot = it; onTrace(lot, serie, ligne.datePeremption) },
                    label = { Text(stringResource(R.string.ach_lot), fontSize = 11.sp, color = MissaMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = serie,
                    onValueChange = { serie = it; onTrace(lot, serie, ligne.datePeremption) },
                    label = { Text(stringResource(R.string.ach_numero_serie), fontSize = 11.sp, color = MissaMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                ChampDateAchat(
                    libelle = stringResource(R.string.ach_peremption),
                    valeur = ligne.datePeremption,
                    onDate = { onTrace(lot, serie, it) },
                )
            }
        }
    }
}

/** Sélecteur déroulant simple — l'option vide n'est jamais proposée. */
@Composable
private fun Selecteur(
    libelle: String,
    valeur: String?,
    options: List<String>,
    onChoix: (Int) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valeur.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(libelle, fontSize = 11.sp, color = MissaMuted) },
            trailingIcon = { Icon(Iv.ArrowDropDown, null, tint = MissaInk) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = options.isNotEmpty()) { ouvert = true },
        )
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp, color = MissaInk) },
                    onClick = {
                        ouvert = false
                        onChoix(index)
                    },
                )
            }
        }
    }
}

/** Champ de date en lecture seule ouvrant un petit calendrier (±12 h, aller/retour). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChampDateAchat(libelle: String, valeur: Long?, onDate: (Long?) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valeur?.let { fmt.format(Date(it)) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(libelle, fontSize = 11.sp, color = MissaMuted) },
            trailingIcon = { Icon(Iv.Calendar, null, tint = MissaInk, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { ouvert = true })
    }
    if (ouvert) {
        val etatDate = rememberDatePickerState(initialSelectedDateMillis = valeur?.plus(43_200_000L))
        DatePickerDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    etatDate.selectedDateMillis?.let { millis -> onDate(millis + 43_200_000L) }
                    ouvert = false
                }) { Text(stringResource(R.string.st_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { ouvert = false }) { Text(stringResource(R.string.st_annuler)) }
            },
        ) {
            DatePicker(state = etatDate)
        }
    }
}
