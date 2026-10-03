package com.missa.b360.ui.fournisseurs.form

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
import com.missa.b360.ui.fournisseurs.dossier.*

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeAchats(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    // Adaptation au type (spec §8) : prestataires et services sans dépôt / lot / stock.
    val montreStock = form.type in setOf(
        TypeFournisseur.ENTREPRISE,
        TypeFournisseur.FOURNISSEUR_MATIERES,
        TypeFournisseur.FOURNISSEUR_EQUIPEMENT,
        TypeFournisseur.ARTISAN,
        TypeFournisseur.SOUS_TRAITANT,
        TypeFournisseur.TRANSPORTEUR,
        TypeFournisseur.COLLECTEUR_DECHETS,
    )
    item {
        MissaChampTexte(form.categoriesFournies, { valeur -> vm.updateForm { it.copy(categoriesFournies = valeur) } }, if (form.type == TypeFournisseur.COLLECTEUR_DECHETS) {
                        stringResource(R.string.four_types_dechets)
                    } else {
                        stringResource(R.string.four_categories)
                    }, icone = Iv.Category)
    }
    if (montreStock) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MissaChampTexte(form.delaiMoyen, { valeur -> vm.updateForm { it.copy(delaiMoyen = valeur.filter { c -> c.isDigit() }) } }, stringResource(R.string.four_delai), modifier = Modifier.weight(1f), icone = Iv.Schedule, clavier = MissaClavier.ENTIER)
                MissaChampTexte(form.quantiteMin, { valeur -> vm.updateForm { it.copy(quantiteMin = valeur.filterMoneyInput()) } }, stringResource(R.string.four_qte_min), modifier = Modifier.weight(1f), icone = Iv.Inventory2, clavier = MissaClavier.DECIMAL)
            }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MissaChampTexte(form.montantMin, { valeur -> vm.updateForm { it.copy(montantMin = valeur.filterMoneyInput()) } }, stringResource(R.string.four_montant_min), modifier = Modifier.weight(1f), icone = Iv.Payments, clavier = MissaClavier.DECIMAL)
            if (montreStock) {
                SelecteurSimple(
                    icone = Iv.LocalShipping,
                    label = stringResource(R.string.four_incoterm),
                    valeur = form.incoterm.ifBlank { "—" },
                    options = INCOTERMS,
                    modifier = Modifier.weight(1f),
                ) { choix -> vm.updateForm { it.copy(incoterm = choix) } }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapePaiement(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
    modes: List<String>,
) {
    item {
        MissaChampTexte(form.conditionsPaiement, { valeur -> vm.updateForm { it.copy(conditionsPaiement = valeur) } }, stringResource(R.string.four_conditions), icone = Iv.Schedule, requis = true)
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MissaChampTexte(form.joursEcheance, { valeur -> vm.updateForm { it.copy(joursEcheance = valeur.filter { c -> c.isDigit() }) } }, stringResource(R.string.four_jours_echeance), modifier = Modifier.weight(1f), icone = Iv.Schedule, clavier = MissaClavier.ENTIER)
            SelecteurSimple(
                icone = Iv.Payments,
                label = stringResource(R.string.four_mode_prefere),
                valeur = form.modePaiementPrefere.ifBlank { "—" },
                options = modes,
                modifier = Modifier.weight(1f),
            ) { choix -> vm.updateForm { it.copy(modePaiementPrefere = choix) } }
        }
    }
    item {
        Text(stringResource(R.string.four_banque_dans_fiche), fontSize = 11.sp, color = MissaMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeDocuments(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    item {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var type by remember { mutableStateOf(FournisseurDocType.ATTESTATION_FISCALE) }
        var typeOuvert by remember { mutableStateOf(false) }
        var reference by remember { mutableStateOf("") }
        var chemin by remember { mutableStateOf<String?>(null) }
        var expiration by remember { mutableStateOf<Long?>(null) }
        var pickerDate by remember { mutableStateOf(false) }

        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                scope.launch {
                    PieceJointeAchat.enregistrer(context, it)?.let { cheminFichier -> chemin = cheminFichier }
                }
            }
        }
        val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                scope.launch {
                    PieceJointeAchat.enregistrer(context, it)?.let { cheminFichier -> chemin = cheminFichier }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (form.documents.isEmpty()) {
                    Text(stringResource(R.string.four_aucun_document), fontSize = 11.sp, color = MissaMuted)
                }
                form.documents.forEachIndexed { index, document ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(
                                if (document.cheminFichier?.let(PieceJointeAchat::estPdf) == true) {
                                    Iv.PictureAsPdf
                                } else {
                                    Iv.Description
                                },
                            ),
                            null,
                            tint = MissaInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                libelleDocument(document.type) +
                                    (document.reference.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                fontSize = 12.sp,
                                color = MissaInk,
                            )
                            Text(fmtDate(document.dateExpiration), fontSize = 10.sp, color = MissaMuted)
                        }
                        IconButton(
                            onClick = { vm.removeDocumentSaisi(index) },
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
                HorizontalDivider(color = MissaBorder)
                MissaRangee {
                    MissaChampListe(
                        libelle = stringResource(R.string.four_type_document),
                        options = FournisseurDocType.entries.map { option -> option to libelleDocument(option) },
                        selection = type,
                        onSelection = { option -> type = option },
                        icone = Iv.Description,
                        modifier = Modifier.weight(1f),
                    )
                    MissaChampTexte(reference, { reference = it }, stringResource(R.string.four_reference), icone = Iv.Badge, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChampDateFour(
                        label = stringResource(R.string.four_date_expiration),
                        millis = expiration,
                        modifier = Modifier.weight(1f),
                    ) { pickerDate = true }
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(painterResource(Iv.Image), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ach_photo), fontSize = 11.sp, color = MissaInk)
                    }
                    OutlinedButton(
                        onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(painterResource(Iv.PictureAsPdf), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ach_pdf), fontSize = 11.sp, color = MissaInk)
                    }
                }
                Button(
                    onClick = {
                        vm.addDocumentSaisi(
                            DocumentSaisi(
                                type = type,
                                reference = reference,
                                cheminFichier = chemin,
                                dateExpiration = expiration,
                            ),
                        )
                        reference = ""
                        chemin = null
                        expiration = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurFournisseurs, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.Add), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.four_ajouter), fontSize = 12.sp, color = Color.White)
                }
            }
        }

        if (pickerDate) {
            val etat = rememberDatePickerState(initialSelectedDateMillis = expiration)
            DatePickerDialog(
                onDismissRequest = { pickerDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        expiration = etat.selectedDateMillis
                        pickerDate = false
                    }) { Text(stringResource(R.string.st_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pickerDate = false }) { Text(stringResource(R.string.st_annuler)) }
                },
            ) {
                DatePicker(state = etat)
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeValidation(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
    devise: String,
) {
    item {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    stringResource(R.string.four_resume),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(6.dp))
                LigneInfo(stringResource(R.string.four_raison_sociale), form.nom)
                LigneInfo(stringResource(R.string.four_type_fournisseur), libelleType(form.type))
                LigneInfo(stringResource(R.string.four_pays), "${form.pays} · ${form.devise}")
                LigneInfo(stringResource(R.string.four_telephone), form.telephone.ifBlank { "—" })
                if (form.identifiantFiscal.isNotBlank()) {
                    LigneInfo(
                        form.typeIdentifiantAttendu ?: stringResource(R.string.four_identifiant),
                        form.identifiantFiscal,
                    )
                }
                LigneInfo(stringResource(R.string.four_conditions), form.conditionsPaiement.ifBlank { "—" })
                if (form.montantMin.toDoubleOrNull()?.takeIf { it > 0.0 } != null) {
                    LigneInfo(
                        stringResource(R.string.four_montant_min),
                        fmtValeur(form.montantMin.toDoubleOrNull() ?: 0.0, devise),
                    )
                }
                LigneInfo(stringResource(R.string.four_contacts), form.contacts.size.toString())
                LigneInfo(stringResource(R.string.four_documents), form.documents.size.toString())
            }
        }
    }
    item {
        Text(
            stringResource(R.string.four_etape_validation_desc),
            fontSize = 11.sp,
            color = MissaMuted,
        )
    }
}
