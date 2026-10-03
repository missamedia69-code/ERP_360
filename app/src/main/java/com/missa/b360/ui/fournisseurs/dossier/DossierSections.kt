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

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierGeneral(
    fournisseur: FournisseurEntity,
) {
    // --- Général ---
    item {
        SectionRepliable(titre = stringResource(R.string.four_general), icone = Iv.Business) {
            LigneInfo(stringResource(R.string.four_type_fournisseur), libelleType(fournisseur.type))
            fournisseur.nomCommercial?.let { LigneInfo(stringResource(R.string.four_nom_commercial), it) }
            LigneInfo(stringResource(R.string.four_pays), fournisseur.pays)
            LigneInfo(stringResource(R.string.four_devise), fournisseur.devise)
            LigneInfo(stringResource(R.string.four_telephone), fournisseur.telephone)
            fournisseur.email?.let { LigneInfo(stringResource(R.string.four_email), it) }
            fournisseur.adresse?.let { LigneInfo(stringResource(R.string.four_adresse), it) }
            fournisseur.siteWeb?.let { LigneInfo(stringResource(R.string.four_site_web), it) }
            fournisseur.description?.let { LigneInfo(stringResource(R.string.four_description), it) }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierContacts(
    fiche: FicheFournisseur,
    onAjouter: () -> Unit,
) {
    // --- Contacts ---
    item {
        SectionRepliable(
            titre = stringResource(R.string.four_contacts),
            icone = Iv.People,
            action = {
                TextButton(onClick = { onAjouter() }) {
                    Text(stringResource(R.string.four_ajouter), fontSize = 11.sp, color = MissaInk)
                }
            },
        ) {
            if (fiche.contacts.isEmpty()) {
                Text(stringResource(R.string.four_aucun_contact), fontSize = 11.sp, color = MissaMuted)
            }
            fiche.contacts.forEach { contact ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(painterResource(Iv.Person), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            buildString {
                                append(contact.nom)
                                contact.prenom?.let { append(" ").append(it) }
                                if (contact.principal) append(" · ")
                            } + if (contact.principal) {
                                stringResource(R.string.four_principal)
                            } else {
                                ""
                            },
                            fontSize = 12.sp,
                            color = MissaInk,
                        )
                        Text(
                            listOfNotNull(contact.fonction, contact.telephone, contact.email)
                                .joinToString(" · "),
                            fontSize = 10.sp,
                            color = MissaMuted,
                        )
                    }
                }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierFiscalite(
    fournisseur: FournisseurEntity,
) {
    // --- Fiscalité ---
    item {
        SectionRepliable(titre = stringResource(R.string.four_fiscalite), icone = Iv.Percent) {
            fournisseur.identifiantFiscal?.let { valeur ->
                LigneInfo(
                    fournisseur.typeIdentifiantFiscal ?: stringResource(R.string.four_type_identifiant),
                    valeur,
                )
            } ?: run {
                if (FournisseurRules.identifiantFiscalObligatoire(fournisseur.pays, fournisseur.type)) {
                    Text(
                        stringResource(R.string.four_sans_identifiant),
                        fontSize = 11.sp,
                        color = Color(0xFFB91C1C),
                    )
                }
            }
            fournisseur.rccm?.let { LigneInfo(stringResource(R.string.four_rccm), it) }
            fournisseur.numTva?.let { LigneInfo(stringResource(R.string.four_num_tva), it) }
            LigneInfo(
                stringResource(R.string.four_assujetti_tva),
                if (fournisseur.assujettiTva) {
                    stringResource(R.string.st_oui)
                } else {
                    stringResource(R.string.st_non)
                },
            )
            if (fournisseur.tauxRetenue > 0.0) {
                LigneInfo(
                    stringResource(R.string.four_taux_retenue),
                    String.format(Locale.getDefault(), "%.2f %%", fournisseur.tauxRetenue),
                )
            }
            if (fournisseur.exonere) {
                Text(stringResource(R.string.four_exonere), fontSize = 11.sp, color = Color(0xFF15803D))
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsDossierHistorique(
    fiche: FicheFournisseur,
) {
    // --- Historique / audit ---
    item {
        SectionRepliable(titre = stringResource(R.string.four_historique), icone = Iv.History) {
            if (fiche.evenements.isEmpty()) {
                Text(stringResource(R.string.four_aucun_evenement), fontSize = 11.sp, color = MissaMuted)
            }
            fiche.evenements.forEach { evenement ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        fmtDate(evenement.date),
                        fontSize = 10.sp,
                        color = MissaMuted,
                        modifier = Modifier.width(76.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            libelleEvenement(evenement.type),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        evenement.details?.let {
                            Text(it, fontSize = 10.sp, color = MissaMuted)
                        }
                    }
                }
            }
        }
    }
}
