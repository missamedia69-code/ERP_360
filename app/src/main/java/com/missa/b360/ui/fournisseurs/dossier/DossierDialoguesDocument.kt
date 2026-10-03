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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogueDocument(
    onConfirmer: (type: FournisseurDocType, reference: String, chemin: String?, emission: Long?, expiration: Long?) -> Unit,
    onAnnuler: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(FournisseurDocType.ATTESTATION_FISCALE) }
    var typeOuvert by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf("") }
    var chemin by remember { mutableStateOf<String?>(null) }
    var emission by remember { mutableStateOf<Long?>(null) }
    var expiration by remember { mutableStateOf<Long?>(null) }
    var pickerDate by remember { mutableStateOf(false) }
    var cibleEmission by remember { mutableStateOf(true) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            scope.launch { PieceJointeAchat.enregistrer(context, it)?.let { cheminFichier -> chemin = cheminFichier } }
        }
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch { PieceJointeAchat.enregistrer(context, it)?.let { cheminFichier -> chemin = cheminFichier } }
        }
    }

    MissaFormDialogue(
        titre = stringResource(R.string.four_ajouter_document),
        icone = Iv.Description,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_ajouter),
        onValider = { onConfirmer(type, reference, chemin, emission, expiration) },
    ) {
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
                label = stringResource(R.string.four_date_emission),
                millis = emission,
                modifier = Modifier.weight(1f),
            ) {
                cibleEmission = true
                pickerDate = true
            }
            ChampDateFour(
                label = stringResource(R.string.four_date_expiration),
                millis = expiration,
                modifier = Modifier.weight(1f),
            ) {
                cibleEmission = false
                pickerDate = true
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, modifier = Modifier.weight(1f)) {
                Icon(painterResource(Iv.Image), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ach_photo), fontSize = 11.sp, color = MissaInk)
            }
            OutlinedButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f)) {
                Icon(painterResource(Iv.PictureAsPdf), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ach_pdf), fontSize = 11.sp, color = MissaInk)
            }
        }
        if (chemin != null) {
            Text(
                stringResource(R.string.four_fichier_pret),
                fontSize = 11.sp,
                color = Color(0xFF15803D),
            )
        }
    }

    if (pickerDate) {
        val etat = rememberDatePickerState(initialSelectedDateMillis = if (cibleEmission) emission else expiration)
        DatePickerDialog(
            onDismissRequest = { pickerDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = etat.selectedDateMillis
                    if (cibleEmission) emission = millis else expiration = millis
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

@Composable
internal fun ChampDateFour(label: String, millis: Long?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier) {
        MissaChampTexte(fmtDate(millis), { }, label, icone = Iv.Calendar, lectureSeule = true)
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
internal fun DialogueArticle(
    vm: FournisseursViewModel,
    dejaLies: Set<Long>,
    onConfirmer: (productId: Long, reference: String, prix: Double, delai: Int, quantiteMin: Double, prefere: Boolean) -> Unit,
    onAnnuler: () -> Unit,
) {
    val produits by vm.produits.collectAsStateWithLifecycle()
    var produitId by remember { mutableStateOf<Long?>(null) }
    var produitOuvert by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("") }
    var delai by remember { mutableStateOf("0") }
    var quantiteMin by remember { mutableStateOf("0") }
    var prefere by remember { mutableStateOf(false) }
    val candidats = produits.filter { it.id !in dejaLies }

    MissaFormDialogue(
        titre = stringResource(R.string.four_lier_article),
        icone = Iv.Inventory2,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_lier),
        validerActif = produitId != null,
        onValider = {
                    produitId?.let { id ->
                        onConfirmer(
                            id,
                            reference,
                            prix.toDoubleOrNull() ?: 0.0,
                            delai.toIntOrNull() ?: 0,
                            quantiteMin.toDoubleOrNull() ?: 0.0,
                            prefere,
                        )
                    }
                },
    ) {
        MissaRangee {
            MissaChampListe(
                libelle = stringResource(R.string.four_produit),
                options = candidats.map { produit -> produit.id to produit.nom },
                selection = produitId,
                onSelection = { produit -> produitId = produit },
                icone = Iv.Inventory2,
                modifier = Modifier.weight(1f),
            )
            MissaChampTexte(reference, { reference = it }, stringResource(R.string.four_reference), icone = Iv.Badge, modifier = Modifier.weight(1f))
        }
        MissaRangee {
            MissaChampTexte(prix, { prix = it.filterMoneyInput() }, stringResource(R.string.four_prix), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
            MissaChampTexte(delai, { delai = it.filter { c -> c.isDigit() } }, stringResource(R.string.four_delai), icone = Iv.Schedule, clavier = MissaClavier.ENTIER, modifier = Modifier.weight(1f))
        }
        MissaChampTexte(quantiteMin, { quantiteMin = it.filterMoneyInput() }, stringResource(R.string.four_qte_min), icone = Iv.Inventory2, clavier = MissaClavier.DECIMAL)
        MissaCaseACocher(prefere, { prefere = it }, stringResource(R.string.four_prefere))
    }
}

@Composable
internal fun DialogueEvaluation(
    noteInitiale: Double,
    commentaireInitial: String,
    onConfirmer: (note: Double, commentaire: String) -> Unit,
    onAnnuler: () -> Unit,
) {
    var note by remember { mutableStateOf(noteInitiale.toInt().coerceIn(0, 5)) }
    var commentaire by remember { mutableStateOf(commentaireInitial) }
    MissaFormDialogue(
        titre = stringResource(R.string.four_evaluer),
        icone = Iv.Star,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.st_ok),
        onValider = { onConfirmer(note.toDouble(), commentaire) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { index ->
                IconButton(onClick = { note = index }, modifier = Modifier.size(30.dp)) {
                    Text(
                        if (index <= note) "★" else "☆",
                        fontSize = 20.sp,
                        color = if (index <= note) Color(0xFFB45309) else MissaMuted,
                    )
                }
            }
        }
        MissaChampTexte(commentaire, { commentaire = it }, stringResource(R.string.four_commentaire), icone = Iv.Description)
    }
}
