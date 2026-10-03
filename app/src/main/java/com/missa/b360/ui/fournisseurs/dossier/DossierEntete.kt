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

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierEntete(
    fournisseur: FournisseurEntity,
    message: String?,
    vm: FournisseursViewModel,
    onModifier: (FournisseurEntity) -> Unit,
    onBloquer: () -> Unit,
) {
    // --- En-tête ---
    item {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fournisseur.nom, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        Text(
                            "${fournisseur.code} · ${libelleType(fournisseur.type)} · ${fournisseur.pays}",
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                    BadgeStatut(fournisseur.statut)
                }
                fournisseur.noteEvaluation?.let { note ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            append("★ ")
                            append(String.format(Locale.getDefault(), "%.1f", note))
                            append(" / 5")
                            fournisseur.dateEvaluation?.let { append(" · ").append(fmtDate(it)) }
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (fournisseur.statut == FournisseurStatus.BLOQUE && !fournisseur.motifBlocage.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.four_motif_blocage_affiche, fournisseur.motifBlocage),
                        fontSize = 11.sp,
                        color = Color(0xFFB91C1C),
                    )
                }
                if (message != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        message?.let { libelleMessageFournisseur(it) }.orEmpty(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message?.startsWith("err_") == true) Color(0xFFB91C1C) else Color(0xFF15803D),
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Actions selon le statut — cycle de vie (spec §3).
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (fournisseur.statut) {
                        FournisseurStatus.BROUILLON -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoutonAction(stringResource(R.string.four_soumettre), Modifier.weight(1f)) { vm.soumettre() }
                            BoutonAction(stringResource(R.string.four_modifier), Modifier.weight(1f)) { onModifier(fournisseur) }
                        }
                        FournisseurStatus.A_VALIDER -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoutonAction(stringResource(R.string.four_approuver), Modifier.weight(1f)) {
                                vm.changerStatut(FournisseurStatus.ACTIF)
                            }
                            BoutonAction(stringResource(R.string.four_renvoyer_brouillon), Modifier.weight(1f)) {
                                vm.changerStatut(FournisseurStatus.BROUILLON)
                            }
                        }
                        FournisseurStatus.ACTIF -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoutonAction(stringResource(R.string.four_suspendre), Modifier.weight(1f)) {
                                vm.changerStatut(FournisseurStatus.SUSPENDU)
                            }
                            BoutonAction(stringResource(R.string.four_bloquer), Modifier.weight(1f)) {
                                onBloquer()
                            }
                        }
                        FournisseurStatus.SUSPENDU -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BoutonAction(stringResource(R.string.four_reactiver), Modifier.weight(1f)) {
                                vm.changerStatut(FournisseurStatus.ACTIF)
                            }
                            BoutonAction(stringResource(R.string.four_bloquer), Modifier.weight(1f)) {
                                onBloquer()
                            }
                        }
                        FournisseurStatus.BLOQUE -> BoutonAction(
                            stringResource(R.string.four_passer_suspendu),
                            Modifier.fillMaxWidth(),
                        ) { vm.changerStatut(FournisseurStatus.SUSPENDU) }
                        FournisseurStatus.ARCHIVE -> Unit
                    }
                    if (fournisseur.statut != FournisseurStatus.ARCHIVE &&
                        fournisseur.statut != FournisseurStatus.BROUILLON &&
                        fournisseur.statut != FournisseurStatus.A_VALIDER
                    ) {
                        BoutonAction(stringResource(R.string.four_modifier), Modifier.fillMaxWidth()) {
                            onModifier(fournisseur)
                        }
                    }
                    if (fournisseur.statut != FournisseurStatus.ARCHIVE) {
                        TextButton(onClick = { vm.changerStatut(FournisseurStatus.ARCHIVE) }) {
                            Text(stringResource(R.string.four_archiver), fontSize = 11.sp, color = Color(0xFFB91C1C))
                        }
                    }
                }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierKpi(
    fiche: FicheFournisseur,
    devise: String,
    onEvaluer: () -> Unit,
) {
    // --- KPI (spec §6.10) ---
    item {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                LigneInfo(stringResource(R.string.four_kpi_achats), fmtValeur(fiche.montantAchete, devise))
                LigneInfo(stringResource(R.string.four_kpi_commandes), fiche.nombreCommandes.toString())
                LigneInfo(stringResource(R.string.four_kpi_solde), fmtValeur(fiche.solde, devise))
                LigneInfo(stringResource(R.string.four_kpi_derniere), fmtDate(fiche.dernierePiece))
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { onEvaluer() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(Iv.QualityBadge), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.four_evaluer), fontSize = 12.sp, color = MissaInk)
                }
            }
        }
    }
}
