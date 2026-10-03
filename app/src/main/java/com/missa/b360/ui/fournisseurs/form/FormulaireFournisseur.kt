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

// ======================================================================
// Formulaire 7 étapes (spec §7)
// ======================================================================

@Composable
internal fun FormulaireFournisseur(
    vm: FournisseursViewModel,
    onBack: () -> Unit,
    onTermine: (Long?) -> Unit,
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val modes by vm.modesPaiement.collectAsStateWithLifecycle()
    val deviseEntreprise by vm.devise.collectAsStateWithLifecycle()

    LaunchedEffect(form.enregistre) {
        if (form.enregistre) {
            val idEnregistre = form.idEnregistre
            vm.ouvrirFormulaire(null)
            onTermine(idEnregistre)
        }
    }

    val titresEtapes = listOf(
        stringResource(R.string.four_etape_identite),
        stringResource(R.string.four_contacts),
        stringResource(R.string.four_fiscalite),
        stringResource(R.string.four_achats_section),
        stringResource(R.string.four_paiement_section),
        stringResource(R.string.four_documents),
        stringResource(R.string.four_etape_validation),
    )

    MissaFormulaireTheme(CouleurFournisseurs) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = if (form.enEditionId == null) {
                stringResource(R.string.four_nouveau)
            } else {
                stringResource(R.string.four_modifier)
            },
            onBack = onBack,
            couleurFond = CouleurFournisseurs,
        )

        // Progression
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.four_etape_n, form.etape, titresEtapes.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Spacer(Modifier.width(6.dp))
            Text("— ${titresEtapes[form.etape - 1]}", fontSize = 12.sp, color = MissaMuted)
        }

        Surface(
            color = com.missa.b360.ui.theme.OnbConfigCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            form.erreur?.let { cle ->
                item {
                    Text(
                        libelleManquant(cle),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C),
                    )
                }
            }
            form.manquants?.let { manquants ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFB91C1C).copy(alpha = 0.08f),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(7.dp)) {
                            Text(
                                stringResource(R.string.four_dossier_incomplet),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                            )
                            manquants.forEach { cle ->
                                Text("• ${libelleManquant(cle)}", fontSize = 11.sp, color = Color(0xFFB91C1C))
                            }
                        }
                    }
                }
            }

            item { MissaFormSectionTitre(titresEtapes[form.etape - 1], numero = form.etape) }
            when (form.etape) {
                1 -> itemsEtapeIdentite(vm, form)
                2 -> itemsEtapeContacts(vm, form)
                3 -> itemsEtapeFiscalite(vm, form)
                4 -> itemsEtapeAchats(vm, form)
                5 -> itemsEtapePaiement(vm, form, modes)
                6 -> itemsEtapeDocuments(vm, form)
                7 -> itemsEtapeValidation(vm, form, deviseEntreprise)
            }
        }
        }

        // Barre de navigation du formulaire (pied du kit : bouton principal pleine largeur)
        val precedent: Pair<String, () -> Unit> = stringResource(R.string.four_precedent) to { vm.etapePrecedente(); Unit }
        when {
            form.etape < 7 -> MissaFormPied(
                texte = stringResource(R.string.four_continuer),
                onValider = vm::etapeSuivante,
                secondaire = if (form.etape > 1) precedent else null,
            )
            form.enEditionId == null -> {
                TextButton(onClick = vm::etapePrecedente, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text("← ${stringResource(R.string.four_precedent)}", fontSize = 12.sp, color = MissaInk)
                }
                MissaFormPied(
                    texte = stringResource(R.string.four_soumettre_validation),
                    onValider = { vm.enregistrer(soumettre = true) },
                    actif = !form.busy,
                    enCours = form.busy,
                    secondaire = Pair<String, () -> Unit>(stringResource(R.string.four_enregistrer_brouillon), { vm.enregistrer(soumettre = false) }),
                    secondaireActif = !form.busy,
                )
            }
            else -> MissaFormPied(
                texte = stringResource(R.string.four_enregistrer),
                onValider = vm::modifierFiche,
                actif = !form.busy,
                enCours = form.busy,
                secondaire = precedent,
            )
        }
    }
    }

    // Dialogue anti-doublon (spec §1)
    form.doublons?.let { doublons ->
        MissaFormDialogue(
            titre = stringResource(R.string.four_doublons_titre),
            couleur = CouleurFournisseurs,
            onFermer = vm::annulerDoublon,
            libelleValider = stringResource(R.string.four_enregistrer_quand_meme),
            onValider = vm::confirmerDoublon,
        ) {
            Text(stringResource(R.string.four_doublons_desc), fontSize = 12.sp, color = MissaInk)
            doublons.forEach { (fiche, motifs) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.08f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(6.dp)) {
                        Text("${fiche.nom} (${fiche.code})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        motifs.forEach { motif ->
                            Text("• ${libelleMotifDoublon(motif)}", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SelecteurSimple(
    label: String,
    valeur: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    icone: Int? = Iv.Category,
    onChoix: (String) -> Unit,
) {
    val requis = label.trimEnd().endsWith("*")
    MissaChampListe(
        libelle = if (requis) label.trimEnd().removeSuffix("*").trimEnd() else label,
        requis = requis,
        options = options.map { it to it },
        selection = valeur.takeIf { it.isNotEmpty() },
        onSelection = onChoix,
        modifier = modifier,
        icone = icone,
    )
}
