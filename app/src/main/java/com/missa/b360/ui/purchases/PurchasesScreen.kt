package com.missa.b360.ui.purchases

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.missa.b360.core.domain.model.AchatReportRules
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import com.missa.b360.core.util.PieceJointeAchat
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Jaune caractéristique du module Achats — source unique : [AppModule.ACHATS]. */
private val JauneAchats: Color get() = AppModule.ACHATS.couleur

/** Écrans internes du module — aucune nouvelle route de navigation. */
private enum class EcranAchat { LISTE, FACTURE, COMMANDE, RECEPTION, REPORTING }

/** Typage d'une pièce ACHATS par décodage de son détail. */
private sealed interface TypePiece {
    data class Facture(val payload: PurchaseRecordPayload) : TypePiece
    data class Reception(val payload: ReceptionPayload) : TypePiece
    data class Commande(val payload: CommandeAchatPayload) : TypePiece
    data object Inconnu : TypePiece
}

private fun typeDe(piece: OperationRecordEntity): TypePiece {
    PurchaseRecordCodec.decode(piece.notes)?.let { return TypePiece.Facture(it) }
    ReceptionCodec.decode(piece.notes)?.let { return TypePiece.Reception(it) }
    CommandeAchatCodec.decode(piece.notes)?.let { return TypePiece.Commande(it) }
    return TypePiece.Inconnu
}

/**
 * Module Achats (spec §6) — la chaîne complète :
 *
 * `ACHATS décide (bon de commande) → STOCK réceptionne et valorise (bon de
 * réception) → COMPTABILITÉ enregistre (facture : dette + TVA) → TRÉSORERIE
 * règle (à la validation ou plus tard).`
 *
 * Annulation = contre-passation transactionnelle, jamais de suppression.
 */
@Composable
fun PurchasesScreen(onBack: () -> Unit, openCreate: Boolean = false) {
    val vm: PurchasesViewModel = hiltViewModel()
    var ecran by remember { mutableStateOf(if (openCreate) EcranAchat.FACTURE else EcranAchat.LISTE) }
    var pieceARegler by remember { mutableStateOf<OperationRecordEntity?>(null) }
    var pieceAAnnuler by remember { mutableStateOf<OperationRecordEntity?>(null) }
    val fournisseurs by vm.suppliers.collectAsStateWithLifecycle()
    val actionResult by vm.actionResult.collectAsStateWithLifecycle()

    when (ecran) {
        EcranAchat.LISTE -> ListeAchats(
            vm = vm,
            onBack = onBack,
            actionResult = actionResult,
            onNouvelleFacture = {
                vm.clearCart()
                ecran = EcranAchat.FACTURE
            },
            onNouvelleCommande = {
                vm.clearCommande()
                ecran = EcranAchat.COMMANDE
            },
            onReporting = { ecran = EcranAchat.REPORTING },
            onOuvrirFacture = { ecran = EcranAchat.FACTURE },
            onOuvrirCommande = { ecran = EcranAchat.COMMANDE },
            onOuvrirReception = { ecran = EcranAchat.RECEPTION },
            onRegler = { pieceARegler = it },
            onAnnuler = { pieceAAnnuler = it },
        )
        EcranAchat.FACTURE -> FormulaireAchat(
            vm = vm,
            onBack = { ecran = EcranAchat.LISTE },
            onTermine = { ecran = EcranAchat.LISTE },
        )
        EcranAchat.COMMANDE -> FormulaireCommande(
            vm = vm,
            onBack = { ecran = EcranAchat.LISTE },
            onTermine = { ecran = EcranAchat.LISTE },
        )
        EcranAchat.RECEPTION -> FormulaireReception(
            vm = vm,
            onBack = { ecran = EcranAchat.LISTE },
            onTermine = { ecran = EcranAchat.LISTE },
        )
        EcranAchat.REPORTING -> EcranReporting(vm = vm, onBack = { ecran = EcranAchat.LISTE })
    }

    // --- Règlement ultérieur ---
    pieceARegler?.let { piece ->
        val payload = remember(piece.notes) { PurchaseRecordCodec.decode(piece.notes) }
        if (payload != null) {
            DialogueReglement(
                piece = piece,
                payload = payload,
                onAnnuler = { pieceARegler = null },
                onConfirmer = { montant, mode ->
                    vm.reglerFacture(piece.id, montant, mode)
                    pieceARegler = null
                },
            )
        } else {
            pieceARegler = null
        }
    }

    // --- Annulation avec confirmation explicite ---
    pieceAAnnuler?.let { piece ->
        AlertDialog(
            onDismissRequest = { pieceAAnnuler = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.ach_confirmer_annulation, piece.reference), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = {
                    vm.annulerPiece(piece.id)
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

// ---------------------------------------------------------------------------
// Liste des pièces
// ---------------------------------------------------------------------------

@Composable
private fun ListeAchats(
    vm: PurchasesViewModel,
    onBack: () -> Unit,
    actionResult: PurchasesViewModel.ActionAchatResult?,
    onNouvelleFacture: () -> Unit,
    onNouvelleCommande: () -> Unit,
    onReporting: () -> Unit,
    onOuvrirFacture: () -> Unit,
    onOuvrirCommande: () -> Unit,
    onOuvrirReception: () -> Unit,
    onRegler: (OperationRecordEntity) -> Unit,
    onAnnuler: (OperationRecordEntity) -> Unit,
) {
    val pieces by vm.purchases.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    var pieceOuverte by remember { mutableStateOf<Long?>(null) }
    val fournisseurs by vm.suppliers.collectAsStateWithLifecycle()

    val factureValidees = pieces.filter {
        it.status == OperationStatus.VALIDATED.name && typeDe(it) is TypePiece.Facture
    }
    val depenses = factureValidees.sumOf { it.amount ?: 0.0 }
    val passif = factureValidees.sumOf { piece ->
        val payload = PurchaseRecordCodec.decode(piece.notes)
        ((piece.amount ?: 0.0) - (payload?.paidAmount ?: 0.0)).coerceAtLeast(0.0)
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
                        onClick = onNouvelleCommande,
                        colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                    ) { Text(stringResource(R.string.ach_nouvelle_commande)) }
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.module_achats),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MissaInk,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(R.string.ach_reporting),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MissaInk,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onReporting)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LigneSynthese(stringResource(R.string.ach_depenses), fmtValeur(depenses, devise))
                            LigneSynthese(stringResource(R.string.ach_passif), fmtValeur(passif, devise))
                            LigneSynthese(stringResource(R.string.ach_brouillons), brouillons.toString())
                            val resultat = actionResult
                            if (resultat != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    messageAction(resultat),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (resultat in ACTIONS_SUCCES) Color(0xFF15803D) else Color(0xFFB91C1C),
                                )
                                LaunchedEffect(resultat) {
                                    kotlinx.coroutines.delay(3_500)
                                    vm.clearActionResult()
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNouvelleCommande,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                        ) {
                            Icon(painterResource(Iv.CartArrowDown), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ach_nouvelle_commande), color = MissaInk)
                        }
                        OutlinedButton(onClick = onNouvelleFacture, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(Iv.Description), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ach_nouvelle), color = MissaInk)
                        }
                    }
                }
                items(pieces, key = { it.id }) { piece ->
                    CartePiece(
                        piece = piece,
                        devise = devise,
                        ouvert = pieceOuverte == piece.id,
                        onToggle = { pieceOuverte = if (pieceOuverte == piece.id) null else piece.id },
                        onReprendreFacture = {
                            if (vm.loadDraft(piece, fournisseurs)) onOuvrirFacture()
                        },
                        onReprendreCommande = {
                            if (vm.loadCommandeDraft(piece, fournisseurs)) onOuvrirCommande()
                        },
                        onReprendreReception = {
                            if (vm.loadReceptionDraft(piece, fournisseurs)) onOuvrirReception()
                        },
                        onRecevoir = {
                            if (vm.preparerReception(piece, fournisseurs)) onOuvrirReception()
                        },
                        onFacturerDepuisReception = {
                            if (vm.chargerFactureDepuisReception(piece, fournisseurs)) onOuvrirFacture()
                        },
                        onFacturerDepuisCommande = {
                            if (vm.chargerFactureDepuisCommande(piece, fournisseurs)) onOuvrirFacture()
                        },
                        onRegler = { onRegler(piece) },
                        onAnnuler = { onAnnuler(piece) },
                    )
                }
            }
        }
    }
}

/** Résultat d'action affiché en vert (sinon rouge). */
private val ACTIONS_SUCCES = setOf(
    PurchasesViewModel.ActionAchatResult.CommandeEnregistree,
    PurchasesViewModel.ActionAchatResult.ReceptionEnregistree,
    PurchasesViewModel.ActionAchatResult.ReglementEnregistre,
    PurchasesViewModel.ActionAchatResult.PieceAnnulee,
)

@Composable
private fun messageAction(resultat: PurchasesViewModel.ActionAchatResult): String = when (resultat) {
    PurchasesViewModel.ActionAchatResult.CommandeEnregistree -> stringResource(R.string.ach_msg_commande)
    PurchasesViewModel.ActionAchatResult.ReceptionEnregistree -> stringResource(R.string.ach_msg_reception)
    PurchasesViewModel.ActionAchatResult.ReglementEnregistre -> stringResource(R.string.ach_msg_reglement)
    PurchasesViewModel.ActionAchatResult.PieceAnnulee -> stringResource(R.string.ach_msg_annulation)
    PurchasesViewModel.ActionAchatResult.FournisseurManquant -> stringResource(R.string.ach_erreur_fournisseur)
    PurchasesViewModel.ActionAchatResult.PanierVide -> stringResource(R.string.ach_erreur_panier)
    PurchasesViewModel.ActionAchatResult.DonneesInvalides -> stringResource(R.string.ach_erreur_montant)
    PurchasesViewModel.ActionAchatResult.DepasseCommande -> stringResource(R.string.ach_erreur_depasse_commande)
    PurchasesViewModel.ActionAchatResult.CommandeIntrouvable -> stringResource(R.string.ach_erreur_commande)
    PurchasesViewModel.ActionAchatResult.StockInsuffisant -> stringResource(R.string.ach_erreur_stock_insuffisant)
    PurchasesViewModel.ActionAchatResult.FactureLiee -> stringResource(R.string.ach_erreur_facture_liee)
    PurchasesViewModel.ActionAchatResult.ReceptionLiee -> stringResource(R.string.ach_erreur_reception_liee)
    PurchasesViewModel.ActionAchatResult.CompteIntrouvable -> stringResource(R.string.ach_erreur_compte)
    PurchasesViewModel.ActionAchatResult.FournisseurNonActif -> stringResource(R.string.ach_erreur_fournisseur_non_actif)
    PurchasesViewModel.ActionAchatResult.PaiementBloque -> stringResource(R.string.ach_erreur_paiement_bloque)
    PurchasesViewModel.ActionAchatResult.LectureSeule -> stringResource(R.string.ach_erreur_lecture_seule)
    PurchasesViewModel.ActionAchatResult.Erreur -> stringResource(R.string.ach_erreur)
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
    onReprendreFacture: () -> Unit,
    onReprendreCommande: () -> Unit,
    onReprendreReception: () -> Unit,
    onRecevoir: () -> Unit,
    onFacturerDepuisReception: () -> Unit,
    onFacturerDepuisCommande: () -> Unit,
    onRegler: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val type = remember(piece.notes) { typeDe(piece) }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val validee = piece.status == OperationStatus.VALIDATED.name
    val brouillon = piece.status == OperationStatus.DRAFT.name
    val libelleType = when (type) {
        is TypePiece.Facture -> stringResource(R.string.ach_piece_facture)
        is TypePiece.Reception -> stringResource(R.string.ach_piece_reception)
        is TypePiece.Commande -> stringResource(R.string.ach_piece_commande)
        TypePiece.Inconnu -> ""
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(piece.reference, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        if (libelleType.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = JauneAchats.copy(alpha = 0.45f)) {
                                Text(
                                    libelleType,
                                    fontSize = 9.sp,
                                    color = MissaInk,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Text(
                        "${piece.counterpart} · ${fmt.format(Date(piece.createdAt))}",
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
                BadgeStatut(piece.status)
                Spacer(Modifier.width(6.dp))
                if ((piece.amount ?: 0.0) > 0.0) {
                    Text(fmtValeur(piece.amount ?: 0.0, devise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
                Icon(
                    if (ouvert) painterResource(Iv.ExpandLess) else painterResource(Iv.ExpandMore),
                    null,
                    tint = MissaInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (ouvert) {
                Spacer(Modifier.height(8.dp))
                // Détail par type.
                when (type) {
                    is TypePiece.Facture -> DetailFacture(type.payload, devise)
                    is TypePiece.Reception -> DetailReception(type.payload)
                    is TypePiece.Commande -> DetailCommande(type.payload, devise)
                    TypePiece.Inconnu -> Unit
                }
                // Actions par type et statut.
                Spacer(Modifier.height(8.dp))
                when {
                    brouillon -> when (type) {
                        is TypePiece.Facture -> BoutonAction(R.string.ach_reprendre, onReprendreFacture)
                        is TypePiece.Commande -> BoutonAction(R.string.ach_reprendre, onReprendreCommande)
                        is TypePiece.Reception -> BoutonAction(R.string.ach_reprendre, onReprendreReception)
                        TypePiece.Inconnu -> Unit
                    }
                    validee -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (type) {
                            is TypePiece.Commande -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BoutonAction(R.string.ach_recevoir, onRecevoir, Modifier.weight(1f))
                                BoutonAction(R.string.ach_facturer, onFacturerDepuisCommande, Modifier.weight(1f))
                            }
                            is TypePiece.Reception -> BoutonAction(R.string.ach_facturer, onFacturerDepuisReception)
                            is TypePiece.Facture -> {
                                val reste = com.missa.b360.core.domain.model.AchatCommandeRules
                                    .restantARegler(type.payload.total, type.payload.paidAmount)
                                if (reste > 0.005) {
                                    BoutonAction(R.string.ach_regler, onRegler)
                                }
                            }
                            TypePiece.Inconnu -> Unit
                        }
                        BoutonAction(R.string.ach_annuler, onAnnuler, teinte = Color(0xFFB91C1C))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoutonAction(libelleRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, teinte: Color = MissaInk) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(stringResource(libelleRes), color = teinte, fontSize = 12.sp)
    }
}

@Composable
private fun DetailFacture(payload: PurchaseRecordPayload, devise: String) {
    payload.lines.forEach { ligne -> LignePiece(ligne, devise) }
    Spacer(Modifier.height(4.dp))
    LigneSynthese(stringResource(R.string.ach_regle), fmtValeur(payload.paidAmount, devise))
    LigneSynthese(
        stringResource(R.string.ach_passif),
        fmtValeur((payload.total - payload.paidAmount).coerceAtLeast(0.0), devise),
    )
    if (payload.taxAmount > 0.0) {
        LigneSynthese(stringResource(R.string.ach_tva), fmtValeur(payload.taxAmount, devise))
    }
    if (payload.receptionRecordId != null) {
        Text(stringResource(R.string.ach_facture_sur_reception), fontSize = 11.sp, color = MissaMuted)
    }
    if (payload.attachments.isNotEmpty()) {
        Text(
            stringResource(R.string.ach_pieces_jointes) + " : " + payload.attachments.size,
            fontSize = 11.sp,
            color = MissaMuted,
        )
    }
    payload.note?.let { Text(it, fontSize = 11.sp, color = MissaMuted) }
}

@Composable
private fun DetailReception(payload: ReceptionPayload) {
    payload.lignes.filter { it.quantiteRecue > 0.0 }.forEach { ligne ->
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
            Text(fmtQuantite(ligne.quantiteRecue), fontSize = 11.sp, color = MissaMuted)
        }
    }
    payload.commandeReference?.let {
        Text(stringResource(R.string.ach_piece_commande) + " " + it, fontSize = 11.sp, color = MissaMuted)
    }
    payload.note?.let { Text(it, fontSize = 11.sp, color = MissaMuted) }
}

@Composable
private fun DetailCommande(payload: CommandeAchatPayload, devise: String) {
    payload.lines.forEach { ligne ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(ligne.name, fontSize = 12.sp, color = MissaInk, modifier = Modifier.weight(1f))
            Text(
                "${fmtQuantite(ligne.quantity)} × ${fmtValeur(ligne.unitPrice, devise)}",
                fontSize = 11.sp,
                color = MissaMuted,
            )
        }
    }
    payload.note?.let { Text(it, fontSize = 11.sp, color = MissaMuted) }
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
// Catalogue + sélecteurs partagés
// ---------------------------------------------------------------------------

@Composable
private fun BlocCatalogue(
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
    filtres.take(30).forEach { produit ->
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
                Icon(painterResource(Iv.Add), null, tint = MissaInk, modifier = Modifier.size(20.dp))
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
            trailingIcon = { Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaInk) },
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
            trailingIcon = { Icon(painterResource(Iv.Calendar), null, tint = MissaInk, modifier = Modifier.size(18.dp)) },
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
    val taxes by vm.taxes.collectAsStateWithLifecycle()
    val tauxDefaut by vm.taxRate.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val resultat by vm.saveResult.collectAsStateWithLifecycle()
    val itemsFournisseur by vm.itemsFournisseur.collectAsStateWithLifecycle()

    var modePaiement by remember { mutableStateOf("") }
    var ligneOuverte by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(modes) { if (modePaiement.isBlank()) modePaiement = modes.firstOrNull().orEmpty() }
    LaunchedEffect(resultat) {
        if (resultat is PurchasesViewModel.SaveResult.Saved) {
            vm.clearSaveResult()
            onTermine()
        }
    }

    val total = vm.total()
    val tauxApplique = ui.taxTaux ?: tauxDefaut
    val tva = if (tauxApplique == 0.0) 0.0 else total * tauxApplique / (100.0 + tauxApplique)
    val peutValider = ui.supplier != null && ui.lines.isNotEmpty() && total > 0.0 &&
        modePaiement.isNotBlank() && !busy

    // Pièces jointes : photo compressée ou PDF copié, jamais en base.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { PieceJointeAchat.enregistrer(context, uri)?.let(vm::addAttachment) }
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { PieceJointeAchat.enregistrer(context, uri)?.let(vm::addAttachment) }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.ach_nouvelle),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Rattachement : la facture sur réception ne regénère aucun stock.
            if (ui.receptionReference != null) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDCFCE7)) {
                        Text(
                            stringResource(R.string.ach_facture_sur_reception) + " — " + ui.receptionReference,
                            fontSize = 11.sp,
                            color = MissaInk,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            item {
                Selecteur(
                    libelle = stringResource(R.string.ach_fournisseur),
                    valeur = ui.supplier?.nom,
                    options = fournisseurs.map { it.nom },
                    onChoix = { index -> vm.selectSupplier(fournisseurs[index]) },
                )
            }

            item { BlocCatalogue(produits = produits, devise = devise, onAjouter = vm::addCatalogProduct) }

            if (ui.lines.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.ach_panier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
            }
            items(ui.lines, key = { "ligne-${it.id}" }) { ligne ->
                LignePanier(
                    ligne = ligne,
                    liaison = ligne.productId?.let { itemsFournisseur[it] },
                    devise = devise,
                    ouvert = ligneOuverte == ligne.id,
                    onToggle = { ligneOuverte = if (ligneOuverte == ligne.id) null else ligne.id },
                    onQuantite = { delta -> vm.changeQuantity(ligne.id, delta) },
                    onPrix = { prix -> vm.updateLine(ligne.id, ligne.quantity, prix) },
                    onSupprimer = { vm.removeLine(ligne.id) },
                    onTrace = { lot, serie, peremption -> vm.updateLineTrace(ligne.id, lot, serie, peremption) },
                )
            }

            if (ui.lines.isNotEmpty()) {
                // --- TVA : taux choisi par facture ---
                item {
                    val optionsTaxe = taxes.map { "${it.nom} (${it.taux} %)" }
                    val libelleActuel = ui.taxTaux
                        ?.let { taux -> "${stringResource(R.string.ach_tva)} $taux %" }
                        ?: "${stringResource(R.string.ach_taux_defaut)} ($tauxDefaut %)"
                    Selecteur(
                        libelle = stringResource(R.string.ach_tva),
                        valeur = libelleActuel,
                        options = optionsTaxe,
                        onChoix = { index -> vm.setTaxTaux(taxes[index].taux) },
                    )
                }
                // --- Pièces jointes ---
                item {
                    Text(
                        stringResource(R.string.ach_pieces_jointes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Icon(painterResource(Iv.Image), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.ach_photo), color = MissaInk, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Icon(painterResource(Iv.PictureAsPdf), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.ach_pdf), color = MissaInk, fontSize = 12.sp)
                        }
                    }
                }
                items(ui.attachments, key = { it }) { chemin ->
                    VignettePieceJointe(path = chemin, onSupprimer = { vm.removeAttachment(chemin) })
                }
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
                            if (tauxApplique > 0.0) {
                                LigneSynthese(
                                    stringResource(R.string.ach_tva_incluse, tauxApplique),
                                    fmtValeur(tva, devise),
                                )
                            }
                        }
                    }
                }
            }
        }

        BarreActions(
            busy = busy,
            libelleGauche = stringResource(R.string.ach_brouillon),
            gaucheActive = ui.supplier != null && ui.lines.isNotEmpty(),
            onGauche = { vm.save(modePaiement, draft = true) },
            libelleDroite = stringResource(R.string.ach_valider),
            droiteActive = peutValider,
            onDroite = { vm.save(modePaiement, draft = false) },
            erreur = when (resultat) {
                PurchasesViewModel.SaveResult.MissingSupplier -> stringResource(R.string.ach_erreur_fournisseur)
                PurchasesViewModel.SaveResult.EmptyCart -> stringResource(R.string.ach_erreur_panier)
                PurchasesViewModel.SaveResult.InvalidAmount -> stringResource(R.string.ach_erreur_montant)
                PurchasesViewModel.SaveResult.FournisseurIntrouvable -> stringResource(R.string.ach_erreur_fournisseur)
                PurchasesViewModel.SaveResult.ReadOnly -> stringResource(R.string.ach_erreur_lecture_seule)
                PurchasesViewModel.SaveResult.Error -> stringResource(R.string.ach_erreur)
                else -> null
            },
        )
    }
}

/** Vignette d'une pièce jointe : aperçu photo ou icône PDF, suppression à droite. */
@Composable
private fun VignettePieceJointe(path: String, onSupprimer: () -> Unit) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(path) {
        if (!PieceJointeAchat.estPdf(path)) bitmap = PieceJointeAchat.charger(path)
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (PieceJointeAchat.estPdf(path)) {
                Icon(painterResource(Iv.PictureAsPdf), null, tint = MissaInk, modifier = Modifier.size(28.dp))
            } else {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                path.substringAfterLast('/'),
                fontSize = 11.sp,
                color = MissaMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSupprimer, modifier = Modifier.size(32.dp)) {
                Icon(painterResource(Iv.Close), null, tint = MissaInk, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun LignePanier(
    ligne: PurchaseLine,
    liaison: com.missa.b360.core.data.entity.FournisseurItemEntity?,
    devise: String,
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
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.name, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(Iv.DeleteOutline), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (ouvert) painterResource(Iv.ExpandLess) else painterResource(Iv.ExpandMore),
                        null,
                        tint = MissaInk,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (liaison != null) {
                BadgeLiaisonFournisseur(liaison, ligne.unitPrice, devise)
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
                ChampsTracabilite(
                    lotInitial = ligne.lot.orEmpty(),
                    serieInitiale = ligne.numeroSerie.orEmpty(),
                    peremption = ligne.datePeremption,
                    onTrace = onTrace,
                )
            }
        }
    }
}

/**
 * Référence fournisseur (spec Fournisseurs §6.4) : dernier prix validé, délai,
 * quantité minimum ; en rouge si le prix saisi diffère du prix fournisseur.
 */
@Composable
private fun BadgeLiaisonFournisseur(
    liaison: com.missa.b360.core.data.entity.FournisseurItemEntity,
    prixActuel: Double,
    devise: String,
) {
    val ecart = liaison.prixUnitaire > 0.0 && kotlin.math.abs(prixActuel - liaison.prixUnitaire) > 1.0
    val labelEcart = if (ecart) stringResource(R.string.four_prix_differents) else ""
    Text(
        buildString {
            liaison.reference?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
            append(fmtValeur(liaison.prixUnitaire, devise))
            if (liaison.delaiJours > 0) append(" · ").append(liaison.delaiJours).append(" j")
            if (liaison.quantiteMin > 0.0) append(" · min ").append(fmtQuantite(liaison.quantiteMin))
            if (ecart) append("  ⚠ ").append(labelEcart)
        },
        fontSize = 10.sp,
        color = if (ecart) Color(0xFFB91C1C) else MissaMuted,
    )
}

/** Lot / numéro de série / péremption — communs au panier facture et à la réception. */
@Composable
private fun ChampsTracabilite(
    lotInitial: String,
    serieInitiale: String,
    peremption: Long?,
    onTrace: (lot: String, serie: String, peremption: Long?) -> Unit,
) {
    Text(stringResource(R.string.ach_traceabilite), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MissaMuted)
    Spacer(Modifier.height(4.dp))
    var lot by remember(lotInitial) { mutableStateOf(lotInitial) }
    var serie by remember(serieInitiale) { mutableStateOf(serieInitiale) }
    var date by remember(peremption) { mutableStateOf(peremption) }
    OutlinedTextField(
        value = lot,
        onValueChange = {
            lot = it
            onTrace(lot, serie, date)
        },
        label = { Text(stringResource(R.string.ach_lot), fontSize = 11.sp, color = MissaMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = serie,
        onValueChange = {
            serie = it
            onTrace(lot, serie, date)
        },
        label = { Text(stringResource(R.string.ach_numero_serie), fontSize = 11.sp, color = MissaMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    ChampDateAchat(
        libelle = stringResource(R.string.ach_peremption),
        valeur = date,
        onDate = {
            date = it
            onTrace(lot, serie, date)
        },
    )
}

// ---------------------------------------------------------------------------
// Formulaire — bon de commande
// ---------------------------------------------------------------------------

@Composable
private fun FormulaireCommande(
    vm: PurchasesViewModel,
    onBack: () -> Unit,
    onTermine: () -> Unit,
) {
    val ui by vm.commandeState.collectAsStateWithLifecycle()
    val produits by vm.products.collectAsStateWithLifecycle()
    val fournisseurs by vm.suppliers.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionResult by vm.actionResult.collectAsStateWithLifecycle()
    val itemsFournisseur by vm.itemsFournisseur.collectAsStateWithLifecycle()

    LaunchedEffect(actionResult) {
        if (actionResult == PurchasesViewModel.ActionAchatResult.CommandeEnregistree) {
            vm.clearActionResult()
            onTermine()
        }
    }

    val total = ui.lines.sumOf { it.total }.coerceAtLeast(0.0)

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.ach_nouvelle_commande),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Selecteur(
                    libelle = stringResource(R.string.ach_fournisseur),
                    valeur = ui.supplier?.nom,
                    options = fournisseurs.map { it.nom },
                    onChoix = { index -> vm.selectSupplierCommande(fournisseurs[index]) },
                )
            }
            item { BlocCatalogue(produits = produits, devise = devise, onAjouter = vm::addCatalogProductCommande) }
            if (ui.lines.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.ach_panier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
            }
            items(ui.lines, key = { "cmd-${it.id}" }) { ligne ->
                LigneCommande(
                    ligne = ligne,
                    liaison = ligne.productId?.let { itemsFournisseur[it] },
                    devise = devise,
                    onQuantite = { delta -> vm.changeQuantityCommande(ligne.id, delta) },
                    onPrix = { prix -> vm.updateLineCommande(ligne.id, ligne.quantity, prix) },
                    onSupprimer = { vm.removeLineCommande(ligne.id) },
                )
            }
            item {
                OutlinedTextField(
                    value = ui.note,
                    onValueChange = vm::updateNoteCommande,
                    label = { Text(stringResource(R.string.ach_note), fontSize = 11.sp, color = MissaMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (ui.lines.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            LigneSynthese(stringResource(R.string.ach_total), fmtValeur(total, devise))
                        }
                    }
                }
            }
        }
        BarreActions(
            busy = busy,
            libelleGauche = stringResource(R.string.ach_brouillon),
            gaucheActive = ui.supplier != null && ui.lines.isNotEmpty(),
            onGauche = { vm.enregistrerCommande(draft = true) },
            libelleDroite = stringResource(R.string.ach_valider),
            droiteActive = ui.supplier != null && ui.lines.isNotEmpty() && !busy,
            onDroite = { vm.enregistrerCommande(draft = false) },
            erreur = when (actionResult) {
                PurchasesViewModel.ActionAchatResult.FournisseurManquant -> stringResource(R.string.ach_erreur_fournisseur)
                PurchasesViewModel.ActionAchatResult.PanierVide -> stringResource(R.string.ach_erreur_panier)
                PurchasesViewModel.ActionAchatResult.LectureSeule -> stringResource(R.string.ach_erreur_lecture_seule)
                PurchasesViewModel.ActionAchatResult.DonneesInvalides,
                PurchasesViewModel.ActionAchatResult.Erreur,
                -> stringResource(R.string.ach_erreur)
                else -> null
            },
        )
    }
}

@Composable
private fun LigneCommande(
    ligne: CommandeAchatLigne,
    liaison: com.missa.b360.core.data.entity.FournisseurItemEntity?,
    devise: String,
    onQuantite: (Double) -> Unit,
    onPrix: (Double) -> Unit,
    onSupprimer: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.name, fontSize = 13.sp, color = MissaInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(Iv.DeleteOutline), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                }
            }
            if (liaison != null) {
                BadgeLiaisonFournisseur(liaison, ligne.unitPrice, devise)
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
        }
    }
}

// ---------------------------------------------------------------------------
// Formulaire — bon de réception
// ---------------------------------------------------------------------------

@Composable
private fun FormulaireReception(
    vm: PurchasesViewModel,
    onBack: () -> Unit,
    onTermine: () -> Unit,
) {
    val ui by vm.receptionState.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionResult by vm.actionResult.collectAsStateWithLifecycle()
    var ligneOuverte by remember { mutableStateOf<Long?>(null) }
    var note by remember(ui.editingRecordId, ui.commandeRecordId) { mutableStateOf(ui.note) }

    LaunchedEffect(actionResult) {
        if (actionResult == PurchasesViewModel.ActionAchatResult.ReceptionEnregistree) {
            vm.clearActionResult()
            onTermine()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.ach_reception),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(ui.supplier?.nom.orEmpty(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        ui.commandeReference?.let {
                            Text(
                                stringResource(R.string.ach_piece_commande) + " " + it,
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }
            items(ui.lignes, key = { "rec-${it.productId}" }) { ligne ->
                LigneReception(
                    ligne = ligne,
                    ouvert = ligneOuverte == ligne.productId,
                    onToggle = { ligneOuverte = if (ligneOuverte == ligne.productId) null else ligne.productId },
                    onMaj = { quantite, lot, serie, peremption ->
                        vm.updateLigneReception(ligne.productId, quantite, lot, serie, peremption)
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        vm.updateNoteReception(it)
                    },
                    label = { Text(stringResource(R.string.ach_note), fontSize = 11.sp, color = MissaMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        BarreActions(
            busy = busy,
            libelleGauche = stringResource(R.string.ach_brouillon),
            gaucheActive = ui.lignes.isNotEmpty(),
            onGauche = { vm.enregistrerReception(draft = true) },
            libelleDroite = stringResource(R.string.ach_valider),
            droiteActive = ui.lignes.any { it.quantiteRecue > 0.0 } && !busy,
            onDroite = { vm.enregistrerReception(draft = false) },
            erreur = when (actionResult) {
                PurchasesViewModel.ActionAchatResult.DepasseCommande -> stringResource(R.string.ach_erreur_depasse_commande)
                PurchasesViewModel.ActionAchatResult.CommandeIntrouvable -> stringResource(R.string.ach_erreur_commande)
                PurchasesViewModel.ActionAchatResult.PanierVide -> stringResource(R.string.ach_erreur_panier)
                PurchasesViewModel.ActionAchatResult.LectureSeule -> stringResource(R.string.ach_erreur_lecture_seule)
                PurchasesViewModel.ActionAchatResult.DonneesInvalides,
                PurchasesViewModel.ActionAchatResult.Erreur,
                -> stringResource(R.string.ach_erreur)
                else -> null
            },
        )
    }
}

@Composable
private fun LigneReception(
    ligne: ReceptionLigne,
    ouvert: Boolean,
    onToggle: () -> Unit,
    onMaj: (quantite: Double, lot: String, serie: String, peremption: Long?) -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ligne.name, fontSize = 13.sp, color = MissaInk)
                    if (ligne.quantiteCommandee > 0.0) {
                        Text(
                            stringResource(R.string.ach_commandee) + " : " + fmtQuantite(ligne.quantiteCommandee),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (ouvert) painterResource(Iv.ExpandLess) else painterResource(Iv.ExpandMore),
                        null,
                        tint = MissaInk,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            var quantiteTexte by remember(ligne.productId, ligne.quantiteRecue) {
                mutableStateOf(ligne.quantiteRecue.let { if (it == 0.0) "" else it.toString() })
            }
            OutlinedTextField(
                value = quantiteTexte,
                onValueChange = { brut ->
                    quantiteTexte = brut.filterMoneyInput()
                    val valeur = quantiteTexte.toDoubleOrNull() ?: 0.0
                    if (valeur >= 0.0) onMaj(valeur, ligne.lot.orEmpty(), ligne.numeroSerie.orEmpty(), ligne.datePeremption)
                },
                label = { Text(stringResource(R.string.ach_recue), fontSize = 11.sp, color = MissaMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (ouvert) {
                Spacer(Modifier.height(8.dp))
                ChampsTracabilite(
                    lotInitial = ligne.lot.orEmpty(),
                    serieInitiale = ligne.numeroSerie.orEmpty(),
                    peremption = ligne.datePeremption,
                    onTrace = { lot, serie, peremption -> onMaj(ligne.quantiteRecue, lot, serie, peremption) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Règlement ultérieur
// ---------------------------------------------------------------------------

@Composable
private fun DialogueReglement(
    piece: OperationRecordEntity,
    payload: PurchaseRecordPayload,
    onAnnuler: () -> Unit,
    onConfirmer: (montant: String, mode: String) -> Unit,
) {
    val vm: PurchasesViewModel = hiltViewModel()
    val modes by vm.paymentMethods.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val reste = com.missa.b360.core.domain.model.AchatCommandeRules.restantARegler(payload.total, payload.paidAmount)
    var montant by remember { mutableStateOf(reste.toString()) }
    var mode by remember { mutableStateOf(modes.firstOrNull().orEmpty()) }
    LaunchedEffect(modes) { if (mode.isBlank()) mode = modes.firstOrNull().orEmpty() }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.ach_regler) + " — " + piece.reference, color = MissaInk, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.ach_reste_a_regler) + " : " + fmtValeur(reste, devise),
                    fontSize = 12.sp,
                    color = MissaInk,
                )
                OutlinedTextField(
                    value = montant,
                    onValueChange = { montant = it.filterMoneyInput() },
                    label = { Text(stringResource(R.string.ach_montant), fontSize = 11.sp, color = MissaMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Selecteur(
                    libelle = stringResource(R.string.ach_mode_paiement),
                    valeur = mode.ifBlank { null },
                    options = modes,
                    onChoix = { index -> mode = modes[index] },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmer(montant, mode) },
                enabled = (montant.toDoubleOrNull() ?: 0.0) in 0.01..reste + 0.005 && mode.isNotBlank(),
            ) { Text(stringResource(R.string.ach_confirmer), color = MissaInk) }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text(stringResource(R.string.st_annuler), color = MissaInk) }
        },
    )
}

// ---------------------------------------------------------------------------
// Reporting
// ---------------------------------------------------------------------------

@Composable
private fun EcranReporting(vm: PurchasesViewModel, onBack: () -> Unit) {
    val pieces by vm.purchases.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()

    val calendriers = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val debutMoisCourant = cal.timeInMillis
        val debutMoisPrecedent = cal.apply { add(Calendar.MONTH, -1) }.timeInMillis
        val fenetres = (5 downTo 0).map { decalage ->
            val c = Calendar.getInstance().apply {
                timeInMillis = debutMoisCourant
                add(Calendar.MONTH, -decalage)
            }
            val debut = c.timeInMillis
            val fin = c.apply { add(Calendar.MONTH, 1) }.timeInMillis
            debut to fin
        }
        Triple(debutMoisCourant, debutMoisPrecedent, fenetres)
    }
    val (debutCourant, debutPrecedent, fenetres) = calendriers
    val finCourant = fenetres.last().second

    val factures = remember(pieces) {
        pieces.filter { it.status == OperationStatus.VALIDATED.name }
            .mapNotNull { piece -> PurchaseRecordCodec.decode(piece.notes)?.let { piece to it } }
    }
    val montantsDates = remember(factures) {
        factures.map { (piece, _) -> piece.createdAt to (piece.amount ?: 0.0) }
    }
    val depensesCourant = AchatReportRules.depenses(montantsDates, debutCourant, finCourant)
    val depensesPrecedent = AchatReportRules.depenses(montantsDates, debutPrecedent, debutCourant)
    val evolution = AchatReportRules.evolution(depensesCourant, depensesPrecedent)
    val passif = factures.sumOf { (piece, payload) ->
        ((piece.amount ?: 0.0) - payload.paidAmount).coerceAtLeast(0.0)
    }
    val top = remember(factures) {
        AchatReportRules.topFournisseurs(
            factures
                .filter { (piece, _) -> piece.createdAt in debutCourant until finCourant }
                .map { (piece, _) -> piece.counterpart.orEmpty().ifBlank { "—" } to (piece.amount ?: 0.0) },
        )
    }
    val mois = remember(factures) { AchatReportRules.parMois(montantsDates, fenetres) }
    val maxMois = (mois.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0)
    val fmtMois = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.ach_reporting),
            onBack = onBack,
            couleurFond = AppModule.ACHATS.couleurPale,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = JauneAchats.copy(alpha = 0.26f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        LigneSynthese(stringResource(R.string.ach_mois_courant), fmtValeur(depensesCourant, devise))
                        LigneSynthese(stringResource(R.string.ach_mois_precedent), fmtValeur(depensesPrecedent, devise))
                        LigneSynthese(
                            stringResource(R.string.ach_evolution),
                            evolution?.let { String.format(Locale.getDefault(), "%+.1f %%", it) } ?: "—",
                        )
                        LigneSynthese(stringResource(R.string.ach_passif), fmtValeur(passif, devise))
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.ach_top_fournisseurs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            if (top.isEmpty()) {
                item {
                    Text(stringResource(R.string.ach_aucune_depense_mois), fontSize = 12.sp, color = MissaMuted)
                }
            }
            items(top, key = { it.nom }) { ligne ->
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ligne.nom, fontSize = 12.sp, color = MissaInk, modifier = Modifier.weight(1f))
                        Text(
                            "${ligne.nombre} ×",
                            fontSize = 11.sp,
                            color = MissaMuted,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(fmtValeur(ligne.total, devise), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.ach_six_mois),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mois.forEach { point ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fmtMois.format(Date(point.debut)),
                                    fontSize = 11.sp,
                                    color = MissaMuted,
                                    modifier = Modifier.width(36.dp),
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(12.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = JauneAchats.copy(alpha = 0.26f),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {}
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = JauneAchats,
                                        modifier = Modifier
                                            .fillMaxWidth((point.total / maxMois).toFloat().coerceIn(0f, 1f))
                                            .height(12.dp),
                                    ) {}
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    fmtValeur(point.total, devise),
                                    fontSize = 10.sp,
                                    color = MissaInk,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Barre d'actions commune (brouillon / valider)
// ---------------------------------------------------------------------------

@Composable
private fun BarreActions(
    busy: Boolean,
    libelleGauche: String,
    gaucheActive: Boolean,
    onGauche: () -> Unit,
    libelleDroite: String,
    droiteActive: Boolean,
    onDroite: () -> Unit,
    erreur: String?,
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            if (!erreur.isNullOrBlank()) {
                Text(erreur, fontSize = 11.sp, color = Color(0xFFB91C1C), modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onGauche,
                    enabled = gaucheActive && !busy,
                    modifier = Modifier.weight(1f),
                ) { Text(libelleGauche, color = MissaInk) }
                Button(
                    onClick = onDroite,
                    enabled = droiteActive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = JauneAchats, contentColor = MissaInk),
                ) { Text(libelleDroite, color = MissaInk) }
            }
        }
    }
}
