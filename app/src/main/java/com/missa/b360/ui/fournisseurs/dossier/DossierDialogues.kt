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

// ======================================================================
// Dialogues de la fiche
// ======================================================================

@Composable
internal fun DialogueMotifBlocage(onConfirmer: (String) -> Unit, onAnnuler: () -> Unit) {
    var motif by remember { mutableStateOf("") }
    MissaFormDialogue(
        titre = stringResource(R.string.four_bloquer),
        icone = Iv.Warning,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_bloquer),
        validerActif = motif.isNotBlank(),
        onValider = { onConfirmer(motif) },
    ) {
        MissaChampTexte(motif, { motif = it }, stringResource(R.string.four_motif), icone = Iv.Description)

    }
}

@Composable
internal fun DialogueContact(
    onConfirmer: (nom: String, prenom: String, fonction: String, telephone: String, email: String, principal: Boolean) -> Unit,
    onAnnuler: () -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var fonction by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf(true) }
    MissaFormDialogue(
        titre = stringResource(R.string.four_ajouter_contact),
        icone = Iv.PersonAdd,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_ajouter),
        validerActif = nom.isNotBlank(),
        onValider = { onConfirmer(nom, prenom, fonction, telephone, email, principal) },
    ) {
        MissaRangee {
            MissaChampTexte(nom, { nom = it }, stringResource(R.string.four_nom), icone = Iv.Person, modifier = Modifier.weight(1f))
            MissaChampTexte(prenom, { prenom = it }, stringResource(R.string.four_prenom), icone = Iv.Person, modifier = Modifier.weight(1f))
        }
        MissaRangee {
            MissaChampTexte(fonction, { fonction = it }, stringResource(R.string.four_fonction), icone = Iv.Badge, modifier = Modifier.weight(1f))
            MissaChampTexte(telephone, { telephone = it }, stringResource(R.string.four_telephone), icone = Iv.Call, clavier = MissaClavier.TELEPHONE, modifier = Modifier.weight(1f))
        }
        MissaChampTexte(email, { email = it }, stringResource(R.string.four_email), icone = Iv.MailOutline, clavier = MissaClavier.EMAIL)
        MissaCaseACocher(principal, { principal = it }, stringResource(R.string.four_principal))
    }
}

@Composable
internal fun DialogueCompte(
    onConfirmer: (
        titulaire: String, banque: String, numero: String, iban: String,
        bic: String, operateur: String, numeroMobile: String, principal: Boolean,
    ) -> Unit,
    onAnnuler: () -> Unit,
) {
    var titulaire by remember { mutableStateOf("") }
    var banque by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bic by remember { mutableStateOf("") }
    var operateur by remember { mutableStateOf("") }
    var numeroMobile by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf(false) }
    var operateurOuvert by remember { mutableStateOf(false) }

    MissaFormDialogue(
        titre = stringResource(R.string.four_ajouter_compte),
        icone = Iv.Bank,
        couleur = CouleurFournisseurs,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_ajouter),
        validerActif = titulaire.isNotBlank() && (numero.isNotBlank() || iban.isNotBlank() || numeroMobile.isNotBlank()),
        onValider = { onConfirmer(titulaire, banque, numero, iban, bic, operateur, numeroMobile, principal) },
    ) {
        MissaRangee {
            MissaChampTexte(titulaire, { titulaire = it }, stringResource(R.string.four_titulaire), icone = Iv.Person, modifier = Modifier.weight(1f))
            MissaChampTexte(banque, { banque = it }, stringResource(R.string.four_banque), icone = Iv.Bank, modifier = Modifier.weight(1f))
        }
        MissaRangee {
            MissaChampTexte(numero, { numero = it }, stringResource(R.string.four_numero_compte), icone = Iv.Badge, modifier = Modifier.weight(1f))
            MissaChampTexte(iban, { iban = it }, stringResource(R.string.four_iban), icone = Iv.Bank, clavier = MissaClavier.MOT_CLE, modifier = Modifier.weight(1f))
        }
        MissaRangee {
            MissaChampTexte(bic, { bic = it }, stringResource(R.string.four_bic), icone = Iv.Bank, clavier = MissaClavier.MOT_CLE, modifier = Modifier.weight(1f))
            MissaChampListe(
                libelle = stringResource(R.string.four_operateur),
                options = OPERATEURS_MOBILE.map { option -> option to option },
                selection = operateur,
                onSelection = { option -> operateur = option },
                icone = Iv.Smartphone,
                modifier = Modifier.weight(1f),
            )
        }
        MissaChampTexte(numeroMobile, { numeroMobile = it }, stringResource(R.string.four_numero_mobile), icone = Iv.Call, clavier = MissaClavier.TELEPHONE)
        MissaCaseACocher(principal, { principal = it }, stringResource(R.string.four_compte_principal))
    }
}
