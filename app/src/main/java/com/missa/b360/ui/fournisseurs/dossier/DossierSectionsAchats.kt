package com.missa.b360.ui.fournisseurs.dossier

import com.missa.b360.ui.theme.OnbConfigCard
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.util.PieceJointeAchat
import com.missa.b360.core.util.filterMoneyInput
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

import com.missa.b360.ui.fournisseurs.*
import com.missa.b360.ui.fournisseurs.components.*
import com.missa.b360.ui.fournisseurs.form.*

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierAchats(
    fournisseur: FournisseurEntity,
    fiche: FicheFournisseur,
    devise: String,
    vm: FournisseursViewModel,
    onLier: () -> Unit,
) {
    // --- Achats & articles liés ---
    item {
        SectionRepliable(
            titre = stringResource(R.string.four_achats_section),
            icone = Iv.CartArrowDown,
            action = {
                TextButton(onClick = { onLier() }) {
                    Text(stringResource(R.string.four_lier_article), fontSize = 11.sp, color = MissaInk)
                }
            },
        ) {
            fournisseur.categoriesFournies?.let {
                LigneInfo(stringResource(R.string.four_categories), it)
            }
            if (fournisseur.delaiMoyenJours > 0) {
                LigneInfo(
                    stringResource(R.string.four_delai),
                    stringResource(R.string.four_jours, fournisseur.delaiMoyenJours),
                )
            }
            if (fournisseur.quantiteMinCommande > 0.0) {
                LigneInfo(stringResource(R.string.four_qte_min), fmtQuantite(fournisseur.quantiteMinCommande))
            }
            if (fournisseur.montantMinCommande > 0.0) {
                LigneInfo(stringResource(R.string.four_montant_min), fmtValeur(fournisseur.montantMinCommande, devise))
            }
            fournisseur.incoterm?.let { LigneInfo(stringResource(R.string.four_incoterm), it) }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MissaBorder)
            if (fiche.items.isEmpty()) {
                Text(stringResource(R.string.four_aucun_article), fontSize = 11.sp, color = MissaMuted)
            }
            fiche.items.forEach { liaison ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            buildString {
                                append(fiche.nomsProduits[liaison.productId] ?: "#${liaison.productId}")
                                liaison.reference?.let { append(" · ").append(it) }
                                if (liaison.prefere) append(" · ★")
                            },
                            fontSize = 12.sp,
                            color = MissaInk,
                        )
                        Text(
                            buildString {
                                append(fmtValeur(liaison.prixUnitaire, devise))
                                if (liaison.delaiJours > 0) {
                                    append(" · ")
                                    append(liaison.delaiJours)
                                    append(" j")
                                }
                                if (liaison.quantiteMin > 0.0) {
                                    append(" · min ")
                                    append(fmtQuantite(liaison.quantiteMin))
                                }
                            },
                            fontSize = 10.sp,
                            color = MissaMuted,
                        )
                    }
                    IconButton(
                        onClick = { vm.delierArticleFiche(liaison.id) },
                        modifier = Modifier.size(27.dp),
                    ) {
                        Icon(
                            painterResource(Iv.DeleteOutline),
                            null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierPaiement(
    fournisseur: FournisseurEntity,
    fiche: FicheFournisseur,
    vm: FournisseursViewModel,
    onAjouter: () -> Unit,
) {
    // --- Paiement & comptes ---
    item {
        SectionRepliable(
            titre = stringResource(R.string.four_paiement_section),
            icone = Iv.Payments,
            action = {
                TextButton(onClick = { onAjouter() }) {
                    Text(stringResource(R.string.four_ajouter), fontSize = 11.sp, color = MissaInk)
                }
            },
        ) {
            fournisseur.conditionsPaiement?.let {
                LigneInfo(stringResource(R.string.four_conditions), it)
            }
            if (fournisseur.joursEcheance > 0) {
                LigneInfo(
                    stringResource(R.string.four_jours_echeance),
                    stringResource(R.string.four_jours, fournisseur.joursEcheance),
                )
            }
            fournisseur.modePaiementPrefere?.let {
                LigneInfo(stringResource(R.string.four_mode_prefere), it)
            }
            if (fournisseur.paiementBloque) {
                Text(stringResource(R.string.four_paiement_bloque), fontSize = 11.sp, color = Color(0xFFB91C1C))
            }
            Text(
                stringResource(R.string.four_verification_interne_seulement),
                fontSize = 10.sp,
                color = Color(0xFF92400E),
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MissaBorder)
            if (fiche.comptes.isEmpty()) {
                Text(stringResource(R.string.four_aucun_compte), fontSize = 11.sp, color = MissaMuted)
            }
            fiche.comptes.forEach { compte ->
                LigneCompte(compte = compte, onVerifier = { vm.verifierCompte(compte.id, true) }, onRejeter = {
                    vm.verifierCompte(compte.id, false)
                })
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierDocuments(
    fiche: FicheFournisseur,
    vm: FournisseursViewModel,
    onAjouter: () -> Unit,
) {
    // --- Documents ---
    item {
        SectionRepliable(
            titre = stringResource(R.string.four_documents),
            icone = Iv.Description,
            action = {
                TextButton(onClick = { onAjouter() }) {
                    Text(stringResource(R.string.four_ajouter), fontSize = 11.sp, color = MissaInk)
                }
            },
        ) {
            if (fiche.documents.isEmpty()) {
                Text(stringResource(R.string.four_aucun_document), fontSize = 11.sp, color = MissaMuted)
            }
            fiche.documents.forEach { document ->
                LigneDocument(document = document, onSupprimer = { vm.archiverDocumentFiche(document.id) })
            }
        }
    }
}
