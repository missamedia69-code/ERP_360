package com.missa.b360.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.missa.b360.ui.components.MissaMenuDeroulant
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.missa.b360.core.util.toMoneyOrNull
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.documents.CommercialDocumentFactory
import com.missa.b360.core.documents.DocumentSharing
import com.missa.b360.core.documents.DocumentType
import com.missa.b360.core.documents.ProfessionalDocumentPdf
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.RappelsRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.missa.b360.ui.components.*

/** Bleu royal caractéristique du module Vente — source unique : [AppModule.VENTE]. */
private val BleuVente: Color get() = AppModule.VENTE.couleur

private enum class EcranVente { LISTE, FACTURE }

@Composable
fun SalesScreen(
    onNavigate: (String) -> Unit = {},
    onOpenClientCreate: () -> Unit = {},
    openCreate: Boolean = false,
    openOverdue: Boolean = false,
) {
    val vm: SalesViewModel = hiltViewModel()
    var ecran by remember { mutableStateOf(if (openCreate) EcranVente.FACTURE else EcranVente.LISTE) }
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
            openOverdue = openOverdue,
            onOuvrirClient = { id -> onNavigate(com.missa.b360.ui.clients.ClientRoutes.fiche(id)) },
        )
        EcranVente.FACTURE -> FormulaireVente(
            vm = vm,
            onBack = { ecran = EcranVente.LISTE },
            onOpenClientCreate = onOpenClientCreate,
        )
    }


}

@Composable
private fun ListeVentes(
    vm: SalesViewModel,
    onNouvelleVente: () -> Unit,
    onOuvrirFacture: () -> Unit,
    openOverdue: Boolean,
    onOuvrirClient: (Long) -> Unit,
) {
    val pieces by vm.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val clients by vm.clients.collectAsStateWithLifecycle(initialValue = emptyList())
    val devise by vm.devise.collectAsStateWithLifecycle()
    val entreprise by vm.entreprise.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val erreurPdf = stringResource(R.string.doc_generation_erreur)

    val validees = pieces.filter { it.status == OperationStatus.VALIDATED.name }
    val caTotal = validees.sumOf { it.amount ?: 0.0 }
    val brouillons = pieces.count { it.status == OperationStatus.DRAFT.name }
    var filtreStatut by remember { mutableStateOf<String?>(null) }
    var afficherRetards by remember(openOverdue) { mutableStateOf(openOverdue) }
    val idsFacturesEnRetard = remember(pieces) {
        RappelsRules.facturesEnRetard(pieces, System.currentTimeMillis()).map { it.first.id }.toSet()
    }

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
                modifier = Modifier.padding(12.dp),
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
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = BleuVente.copy(alpha = 0.16f)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
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
                if (afficherRetards) {
                    item(key = "factures-en-retard-banner") {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF1E8)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.sales_overdue_title, idsFacturesEnRetard.size),
                                    color = Color(0xFF9A3412),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { afficherRetards = false; filtreStatut = null }) {
                                    Text(stringResource(R.string.sales_show_all))
                                }
                            }
                        }
                    }
                }
                // --- Structure Matricielle 4 Tuiles ---
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TuileVente(
                            icone = Iv.ShoppingCart,
                            titre = stringResource(R.string.crm_tuile_tous),
                            sousTitre = pieces.size.toString(),
                            estActif = filtreStatut == null && !afficherRetards,
                            modifier = Modifier.weight(1f),
                            onClick = { afficherRetards = false; filtreStatut = null },
                        )
                        TuileVente(
                            icone = Iv.CheckCircle,
                            titre = stringResource(R.string.sales_tab_history),
                            sousTitre = validees.size.toString(),
                            estActif = filtreStatut == OperationStatus.VALIDATED.name,
                            modifier = Modifier.weight(1f),
                            onClick = { afficherRetards = false; filtreStatut = OperationStatus.VALIDATED.name },
                        )
                        TuileVente(
                            icone = Iv.Edit,
                            titre = stringResource(R.string.ach_brouillons),
                            sousTitre = brouillons.toString(),
                            estActif = filtreStatut == OperationStatus.DRAFT.name,
                            modifier = Modifier.weight(1f),
                            onClick = { afficherRetards = false; filtreStatut = OperationStatus.DRAFT.name },
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
                val piecesAffichees = when {
                    afficherRetards -> pieces.filter { it.id in idsFacturesEnRetard }
                    filtreStatut != null -> pieces.filter { it.status == filtreStatut }
                    else -> pieces
                }
                if (piecesAffichees.isEmpty()) {
                    item(key = "factures-en-retard-empty") {
                        MissaEmptyState(
                            icon = Iv.CheckCircle,
                            title = stringResource(if (afficherRetards) R.string.sales_overdue_empty else R.string.sales_filter_empty),
                            description = stringResource(if (afficherRetards) R.string.sales_overdue_empty_desc else R.string.sales_empty_desc),
                            action = {
                                TextButton(onClick = { afficherRetards = false; filtreStatut = null }) {
                                    Text(stringResource(R.string.sales_show_all))
                                }
                            },
                        )
                    }
                }
                items(piecesAffichees, key = { it.id }) { piece ->
                    CartePieceVente(
                        piece = piece,
                        devise = devise,
                        onOuvrirClient = onOuvrirClient,
                        onReprendre = {
                            if (vm.loadDraft(piece, clients)) onOuvrirFacture()
                        },
                        onPartagerPdf = entreprise?.let { societe ->
                            {
                                scope.launch {
                                    val resultat = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val donnees = CommercialDocumentFactory.depuisVente(
                                                piece = piece,
                                                entreprise = societe,
                                                type = DocumentType.FACTURE_CLIENT,
                                            ) ?: error("Payload de vente invalide")
                                            ProfessionalDocumentPdf.generer(context, donnees)
                                        }
                                    }
                                    resultat.onSuccess { DocumentSharing.partager(context, it.fichier, piece.reference) }
                                        .onFailure { Toast.makeText(context, erreurPdf, Toast.LENGTH_LONG).show() }
                                }
                            }
                        },
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
    onOuvrirClient: (Long) -> Unit,
    onReprendre: () -> Unit,
    onPartagerPdf: (() -> Unit)?,
) {
    val dateStr = remember(piece.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(piece.createdAt))
    }
    val payload = remember(piece.notes) { SaleRecordCodec.decode(piece.notes) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(piece.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                BadgeStatut(piece.status)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val clientId = payload?.clientId ?: 0L
                Text(
                    payload?.clientName ?: piece.counterpart.orEmpty(),
                    fontSize = 12.sp,
                    color = if (clientId > 0L) BleuVente else MissaInk,
                    modifier = if (clientId > 0L) Modifier.heightIn(min = 48.dp).clickable { onOuvrirClient(clientId) }.wrapContentHeight(Alignment.CenterVertically) else Modifier,
                )
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
            } else if (onPartagerPdf != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onPartagerPdf, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.doc_partager_pdf), fontSize = 11.sp, color = BleuVente)
                }
            }
            // Une facture validée est immuable ; sa correction passe par un avoir, jamais par une annulation directe.
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
    val context = LocalContext.current
    var dialogueNouveauClient by remember { mutableStateOf(false) }
    var quickClientError by remember { mutableStateOf<String?>(null) }

    var modePaiement by remember { mutableStateOf("") }
    LaunchedEffect(modes) { if (modePaiement.isBlank()) modePaiement = modes.firstOrNull().orEmpty() }

    val totals = ui.totals(taxRate)
    val soldeClient by vm.clientBalance.collectAsStateWithLifecycle()

    MissaFormulaireTheme(AppModule.VENTE.couleur) {
    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.sales_new_sale),
            onBack = onBack,
            couleurFond = AppModule.VENTE.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { MissaFormSectionTitre(stringResource(R.string.form_client), numero = 1) }
            item {
                SelecteurClient(
                    selectedClient = ui.selectedClient,
                    clients = clients,
                    onSelect = vm::selectClient,
                    onOpenClientCreate = { dialogueNouveauClient = true },
                    onSelectCashCustomer = { vm.selectCashClient(context.getString(R.string.sales_cash_customer)) },
                )
            }
            val clientChoisi = ui.selectedClient
            if (clientChoisi != null && clientChoisi.id > 0L) {
                item {
                    com.missa.b360.ui.clients.components.ClientCreditBanner(
                        client = clientChoisi,
                        balance = soldeClient,
                        montantVente = totals.total,
                        montantRegle = (ui.paidInput.toMoneyOrNull() ?: totals.total).coerceIn(0.0, totals.total),
                        devise = devise.orEmpty(),
                    )
                }
            }
            if (clients.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BleuVente.copy(alpha = 0.26f),
                        modifier = Modifier.fillMaxWidth().clickable { dialogueNouveauClient = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(Iv.PersonAdd), null, tint = MissaInk, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.sales_aucun_client),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MissaInk,
                                )
                                Text(
                                    stringResource(R.string.sales_creer_client_invite),
                                    fontSize = 10.sp,
                                    color = MissaInk.copy(alpha = 0.8f),
                                )
                            }
                            Icon(painterResource(Iv.Add), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            item { MissaFormSectionTitre(stringResource(R.string.form_section_article), numero = 2) }
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
                    Surface(shape = RoundedCornerShape(12.dp), color = BleuVente.copy(alpha = 0.12f)) {
                        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.sales_subtotal), fontSize = 11.sp, color = MissaMuted)
                                Text(fmtValeur(totals.subtotal, devise), fontSize = 12.sp, color = MissaInk)
                            }
                            if (totals.discount > 0.0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.sales_client_discount), fontSize = 12.sp, color = MissaMuted)
                                    Text("−${fmtValeur(totals.discount, devise)}", fontSize = 12.sp, color = Color(0xFF15803D))
                                }
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
                item { MissaFormSectionTitre(stringResource(R.string.form_section_paiement), numero = 3) }
                item {
                    MissaChoixPaiement(
                        moyens = modes,
                        selection = modePaiement.ifBlank { null },
                        onSelection = { modePaiement = it },
                        libelle = stringResource(R.string.ach_mode_paiement),
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
                SalesViewModel.SaveResult.ClientNonEligible -> stringResource(R.string.sales_err_client_inactive)
                SalesViewModel.SaveResult.ValidationCreditRequise -> stringResource(R.string.sales_err_credit_limit)
                SalesViewModel.SaveResult.CompteEncaissementRequis -> stringResource(R.string.sales_err_cash_account)
                SalesViewModel.SaveResult.ModuleStockInactif -> stringResource(R.string.sales_err_stock_module_inactive)
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

    if (dialogueNouveauClient) {
        DialogueCreationClientRapide(
            error = quickClientError,
            onDismiss = { dialogueNouveauClient = false; quickClientError = null },
            onValider = { nom, telephone, email, adresse ->
                vm.creerClientRapide(
                    nom, telephone, email, adresse,
                    onSuccess = { dialogueNouveauClient = false; quickClientError = null },
                    onFailure = { cause ->
                        quickClientError = if (cause == "doublon")
                            context.getString(R.string.sales_quick_client_duplicate)
                        else context.getString(R.string.sales_quick_client_invalid)
                    },
                )
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
    onSelectCashCustomer: () -> Unit,
) {
    // Clé -1 = « client comptant » (pas de fiche) ; le client choisi hors liste
    // (ex. client comptant) s'affiche en indication.
    val comptant = stringResource(R.string.sales_cash_customer)
    val options = listOf(-1L to comptant) + clients.map { it.id to it.nom }
    MissaChampListe(
        libelle = stringResource(R.string.sales_select_client),
        options = options,
        selection = selectedClient?.id?.takeIf { id -> clients.any { it.id == id } },
        onSelection = { id ->
            if (id == -1L) onSelectCashCustomer() else clients.firstOrNull { it.id == id }?.let(onSelect)
        },
        icone = Iv.Person,
        requis = true,
        placeholder = selectedClient?.nom ?: stringResource(R.string.sales_select_client),
        actionNouveau = stringResource(R.string.clients_nouveau_client) to onOpenClientCreate,
    )
}

/** Boîte de dialogue de création rapide d'un client in-situ sans abandonner le panier vente. */
@Composable
private fun DialogueCreationClientRapide(
    error: String?,
    onDismiss: () -> Unit,
    onValider: (nom: String, telephone: String, email: String?, adresse: String?) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }

    MissaFormDialogue(
        titre = stringResource(R.string.sales_nouveau_client_rapide),
        icone = Iv.PersonAdd,
        couleur = AppModule.VENTE.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = nom.isNotBlank() && telephone.isNotBlank(),
        erreur = error,
        onValider = { onValider(nom.trim(), telephone.trim(), email.trim().ifBlank { null }, adresse.trim().ifBlank { null }) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 1) {
            MissaRangee {
                MissaChampTexte(nom, { nom = it }, stringResource(R.string.clients_nom), icone = Iv.Person, requis = true, longueurMax = 120, modifier = Modifier.weight(1f))
                MissaChampTexte(telephone, { telephone = it }, stringResource(R.string.clients_telephone), icone = Iv.Call, clavier = MissaClavier.TELEPHONE, requis = true, longueurMax = 25, modifier = Modifier.weight(1f))
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_contact), numero = 2) {
            MissaRangee {
                MissaChampTexte(email, { email = it }, stringResource(R.string.sales_email_optionnel), icone = Iv.MailOutline, clavier = MissaClavier.EMAIL, longueurMax = 100, modifier = Modifier.weight(1f))
                MissaChampTexte(adresse, { adresse = it }, stringResource(R.string.sales_adresse_optionnelle), icone = Iv.Place, longueurMax = 150, modifier = Modifier.weight(1f))
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
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MissaBorder),
            modifier = Modifier.fillMaxWidth().clickable { onAjouter(produit) },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
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
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.name, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer, modifier = Modifier.size(27.dp)) {
                    Icon(painterResource(Iv.DeleteOutline), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantite(-1.0) }, modifier = Modifier.size(30.dp)) {
                    Text("−", fontSize = 16.sp, color = MissaInk)
                }
                Text(fmtQuantite(ligne.quantity), fontSize = 13.sp, color = MissaInk)
                IconButton(onClick = { onQuantite(1.0) }, modifier = Modifier.size(30.dp)) {
                    Text("+", fontSize = 16.sp, color = MissaInk)
                }
                Spacer(Modifier.width(6.dp))
                Text(fmtValeur(ligne.unitPrice * ligne.quantity, devise), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MissaInk)
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
    MissaFormPied(
        texte = stringResource(R.string.ach_valider),
        onValider = onValider,
        actif = valideActive,
        enCours = busy,
        secondaire = stringResource(R.string.ach_brouillon) to onBrouillon,
        secondaireActif = valideActive,
        erreur = erreur,
    )
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
            .height(70.dp)
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
