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

// Fiche fournisseur (spec §6) — sections repliables
// ======================================================================

@Composable
internal fun DossierFournisseurEcran(
    vm: FournisseursViewModel,
    onBack: () -> Unit,
    onModifier: (FournisseurEntity) -> Unit,
) {
    val fiche by vm.fiche.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val fournisseur = fiche.fournisseur

    var blocageOuvert by remember { mutableStateOf(false) }
    var contactOuvert by remember { mutableStateOf(false) }
    var compteOuvert by remember { mutableStateOf(false) }
    var documentOuvert by remember { mutableStateOf(false) }
    var articleOuvert by remember { mutableStateOf(false) }
    var evaluationOuverte by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3_000)
            vm.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = fournisseur?.nom ?: stringResource(R.string.module_fournisseurs),
            onBack = onBack,
            couleurFond = CouleurFournisseurs,
        )
        if (fournisseur == null) {
            MissaEmptyState(
                icon = Iv.Handshake,
                title = stringResource(R.string.four_introuvable),
                modifier = Modifier.padding(12.dp),
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            itemsDossierEntete(fournisseur, message, vm, onModifier, onBloquer = { blocageOuvert = true })
            itemsDossierKpi(fiche, devise, onEvaluer = { evaluationOuverte = true })
            itemsDossierGeneral(fournisseur)
            itemsDossierContacts(fiche, onAjouter = { contactOuvert = true })
            itemsDossierFiscalite(fournisseur)
            itemsDossierAchats(fournisseur, fiche, devise, vm, onLier = { articleOuvert = true })
            itemsDossierPaiement(fournisseur, fiche, vm, onAjouter = { compteOuvert = true })
            itemsDossierDocuments(fiche, vm, onAjouter = { documentOuvert = true })
            itemsDossierHistorique(fiche)
        }
    }

    if (blocageOuvert) {
        DialogueMotifBlocage(
            onConfirmer = { motif ->
                vm.changerStatut(FournisseurStatus.BLOQUE, motif)
                blocageOuvert = false
            },
            onAnnuler = { blocageOuvert = false },
        )
    }
    if (contactOuvert) {
        DialogueContact(
            onConfirmer = { nom, prenom, fonction, telephone, email, principal ->
                vm.ajouterContact(nom, prenom, fonction, telephone, email, principal)
                contactOuvert = false
            },
            onAnnuler = { contactOuvert = false },
        )
    }
    if (compteOuvert) {
        DialogueCompte(
            onConfirmer = { titulaire, banque, numero, iban, bic, operateur, numeroMobile, principal ->
                vm.ajouterCompte(titulaire, banque, numero, iban, bic, operateur, numeroMobile, principal)
                compteOuvert = false
            },
            onAnnuler = { compteOuvert = false },
        )
    }
    if (documentOuvert) {
        DialogueDocument(
            onConfirmer = { type, reference, chemin, emission, expiration ->
                vm.ajouterDocumentFiche(type, reference, chemin, emission, expiration)
                documentOuvert = false
            },
            onAnnuler = { documentOuvert = false },
        )
    }
    if (articleOuvert) {
        DialogueArticle(
            vm = vm,
            dejaLies = fiche.items.map { it.productId }.toSet(),
            onConfirmer = { productId, reference, prix, delai, quantiteMin, prefere ->
                vm.lierArticleFiche(productId, reference, prix, delai, quantiteMin, prefere)
                articleOuvert = false
            },
            onAnnuler = { articleOuvert = false },
        )
    }
    if (evaluationOuverte) {
        DialogueEvaluation(
            noteInitiale = fournisseur?.noteEvaluation ?: 0.0,
            commentaireInitial = fournisseur?.commentaireEvaluation.orEmpty(),
            onConfirmer = { note, commentaire ->
                vm.evaluer(note, commentaire)
                evaluationOuverte = false
            },
            onAnnuler = { evaluationOuverte = false },
        )
    }
}
