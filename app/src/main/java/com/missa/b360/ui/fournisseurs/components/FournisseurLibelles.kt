package com.missa.b360.ui.fournisseurs.components

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
import com.missa.b360.ui.fournisseurs.dossier.*
import com.missa.b360.ui.fournisseurs.form.*

/** Marron caractéristique du module Fournisseurs — source unique : [AppModule.FOURNISSEURS]. */
internal val CouleurFournisseurs: Color get() = AppModule.FOURNISSEURS.couleur


internal val PAYS = listOf("CM", "SN", "CI", "GA", "GN", "MA", "FR", "BE", "AE", "NG", "KE", "GH", "CA")
internal val DEVISES = listOf("XAF", "XOF", "EUR", "USD", "MAD", "NGN", "CAD")
internal val INCOTERMS = listOf("EXW", "FOB", "CIF", "DAP", "DDP")
internal val OPERATEURS_MOBILE = listOf("MTN", "Orange", "Moov", "Wave", "M-Pesa", "Airtel")
// ======================================================================
// Libellés lisibles (valeurs codées → texte traduit)
// ======================================================================

@Composable
internal fun libelleMessageFournisseur(code: String): String = stringResource(
    when (code) {
        "err_article" -> R.string.four_err_article
        "err_article_delie" -> R.string.four_err_article_delie
        "err_compte_incomplet" -> R.string.four_err_compte_incomplet
        "err_compte_verification" -> R.string.four_err_compte_verification
        "err_document" -> R.string.four_err_document
        "err_document_retire" -> R.string.four_err_document_retire
        "err_dossier_incomplet" -> R.string.four_err_dossier_incomplet
        "err_evaluation" -> R.string.four_err_evaluation
        "err_introuvable" -> R.string.four_err_introuvable
        "err_modification" -> R.string.four_err_modification
        "err_motif_obligatoire" -> R.string.four_err_motif_obligatoire
        "err_document_verification" -> R.string.four_err_document_verification
        "err_plan_montant" -> R.string.four_err_plan_montant
        "err_plan_date" -> R.string.four_err_plan_date
        "err_plan_introuvable" -> R.string.four_err_plan_introuvable
        "err_plan_lecture_seule" -> R.string.four_err_plan_lecture_seule
        "msg_document_verifie" -> R.string.four_msg_document_verifie
        "msg_document_rejete" -> R.string.four_msg_document_rejete
        "msg_plan_ok" -> R.string.four_msg_plan_ok
        "msg_plan_annule" -> R.string.four_msg_plan_annule
        "err_transition" -> R.string.four_err_transition
        "msg_article_delie" -> R.string.four_msg_article_delie
        "msg_article_lie" -> R.string.four_msg_article_lie
        "msg_compte_ajoute" -> R.string.four_msg_compte_ajoute
        "msg_compte_rejete" -> R.string.four_msg_compte_rejete
        "msg_compte_verifie" -> R.string.four_msg_compte_verifie
        "msg_contact_ajoute" -> R.string.four_msg_contact_ajoute
        "msg_document_ajoute" -> R.string.four_msg_document_ajoute
        "msg_document_retire" -> R.string.four_msg_document_retire
        "msg_evaluation" -> R.string.four_msg_evaluation
        "msg_fournisseur_enregistre" -> R.string.four_msg_fournisseur_enregistre
        "msg_fournisseur_modifie" -> R.string.four_msg_fournisseur_modifie
        "msg_soumis" -> R.string.four_msg_soumis
        "msg_statut_mis_a_jour" -> R.string.four_msg_statut_mis_a_jour
        else -> R.string.four_err_introuvable
    },
)

@Composable
internal fun libelleStatut(statut: FournisseurStatus): String = stringResource(
    when (statut) {
        FournisseurStatus.BROUILLON -> R.string.four_statut_brouillon
        FournisseurStatus.A_VALIDER -> R.string.four_statut_a_valider
        FournisseurStatus.ACTIF -> R.string.four_statut_actif
        FournisseurStatus.SUSPENDU -> R.string.four_statut_suspendu
        FournisseurStatus.BLOQUE -> R.string.four_statut_bloque
        FournisseurStatus.ARCHIVE -> R.string.four_statut_archive
    },
)

@Composable
internal fun libelleType(type: TypeFournisseur): String = stringResource(
    when (type) {
        TypeFournisseur.ENTREPRISE -> R.string.four_type_entreprise
        TypeFournisseur.PARTICULIER -> R.string.four_type_particulier
        TypeFournisseur.PRESTATAIRE -> R.string.four_type_prestataire
        TypeFournisseur.ARTISAN -> R.string.four_type_artisan
        TypeFournisseur.TRANSPORTEUR -> R.string.four_type_transporteur
        TypeFournisseur.SOUS_TRAITANT -> R.string.four_type_sous_traitant
        TypeFournisseur.FOURNISSEUR_EQUIPEMENT -> R.string.four_type_equipement
        TypeFournisseur.FOURNISSEUR_MATIERES -> R.string.four_type_matieres
        TypeFournisseur.FOURNISSEUR_SERVICES -> R.string.four_type_services
        TypeFournisseur.COLLECTEUR_DECHETS -> R.string.four_type_dechets
        TypeFournisseur.AUTRE -> R.string.four_type_autre
    },
)

@Composable
internal fun libelleDocument(type: FournisseurDocType): String = stringResource(
    when (type) {
        FournisseurDocType.CONTRAT -> R.string.four_doc_contrat
        FournisseurDocType.CONVENTION -> R.string.four_doc_convention
        FournisseurDocType.BON_COMMANDE_TYPE -> R.string.four_doc_bc_type
        FournisseurDocType.DEVIS -> R.string.four_doc_devis
        FournisseurDocType.ATTESTATION_FISCALE -> R.string.four_doc_attestation
        FournisseurDocType.IDENTIFIANT_FISCAL -> R.string.four_doc_identifiant
        FournisseurDocType.RCCM -> R.string.four_doc_rccm
        FournisseurDocType.RIB -> R.string.four_doc_rib
        FournisseurDocType.CERTIFICAT_QUALITE -> R.string.four_doc_qualite
        FournisseurDocType.ASSURANCE -> R.string.four_doc_assurance
        FournisseurDocType.FICHE_SECURITE -> R.string.four_doc_securite
        FournisseurDocType.CERTIFICAT_ORIGINE -> R.string.four_doc_origine
        FournisseurDocType.LICENCE -> R.string.four_doc_licence
        FournisseurDocType.AUTRE -> R.string.four_doc_autre
    },
)

@Composable
internal fun libelleEvenement(type: FournisseurEvenementType): String = stringResource(
    when (type) {
        FournisseurEvenementType.CREATION -> R.string.four_evt_creation
        FournisseurEvenementType.MISE_A_JOUR -> R.string.four_evt_maj
        FournisseurEvenementType.SOUMISSION -> R.string.four_evt_soumission
        FournisseurEvenementType.APPROBATION -> R.string.four_evt_approbation
        FournisseurEvenementType.SUSPENSION -> R.string.four_evt_suspension
        FournisseurEvenementType.BLOCAGE -> R.string.four_evt_blocage
        FournisseurEvenementType.REACTIVATION -> R.string.four_evt_reactivation
        FournisseurEvenementType.ARCHIVAGE -> R.string.four_evt_archivage
        FournisseurEvenementType.COMPTE_AJOUTE -> R.string.four_evt_compte_ajoute
        FournisseurEvenementType.COMPTE_MODIFIE -> R.string.four_evt_compte_modifie
        FournisseurEvenementType.COMPTE_VERIFIE -> R.string.four_evt_compte_verifie
        FournisseurEvenementType.COMPTE_REJETE -> R.string.four_evt_compte_rejete
        FournisseurEvenementType.DOCUMENT_AJOUTE -> R.string.four_evt_doc_ajoute
        FournisseurEvenementType.DOCUMENT_SUPPRIME -> R.string.four_evt_doc_supprime
        FournisseurEvenementType.ARTICLE_LIE -> R.string.four_evt_article_lie
        FournisseurEvenementType.ARTICLE_DELIE -> R.string.four_evt_article_delie
        FournisseurEvenementType.EVALUATION -> R.string.four_evt_evaluation
        FournisseurEvenementType.REAPPROBATION_REQUISE -> R.string.four_evt_reapprobation
    },
)

/** Champ manquant (clé stable du domaine) → libellé traduit. */
@Composable
internal fun libelleManquant(cle: String): String = stringResource(
    when (cle) {
        "raison_sociale" -> R.string.four_raison_sociale
        "adresse" -> R.string.four_adresse
        "contact_tel_ou_email" -> R.string.four_contact_obligatoire
        "contact_principal" -> R.string.four_contact_principal_manquant
        "identifiant_fiscal" -> R.string.four_identifiant
        "conditions_paiement" -> R.string.four_conditions
        "categories_fournies" -> R.string.four_categories
        else -> R.string.four_champ_manquant
    },
)

/** Motif de doublon (clé stable) → libellé traduit. */
@Composable
internal fun libelleMotifDoublon(cle: String): String = stringResource(
    when (cle) {
        "raison_sociale" -> R.string.four_doublon_raison_sociale
        "telephone" -> R.string.four_telephone
        "email" -> R.string.four_email
        "identifiant_fiscal" -> R.string.four_identifiant
        "rccm" -> R.string.four_rccm
        "pays_raison_sociale" -> R.string.four_doublon_pays_nom
        else -> R.string.four_champ_manquant
    },
)

internal fun couleurStatut(statut: FournisseurStatus): Color = when (statut) {
    FournisseurStatus.BROUILLON -> Color(0xFF6B7280)
    FournisseurStatus.A_VALIDER -> Color(0xFFD97706)
    FournisseurStatus.ACTIF -> Color(0xFF15803D)
    FournisseurStatus.SUSPENDU -> Color(0xFFB45309)
    FournisseurStatus.BLOQUE -> Color(0xFFB91C1C)
    FournisseurStatus.ARCHIVE -> Color(0xFF9CA3AF)
}

internal fun fmtDate(millis: Long?): String =
    if (millis == null) "—" else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

@Composable
internal fun BadgeStatut(statut: FournisseurStatus) {
    val couleur = couleurStatut(statut)
    Box(
        Modifier.background(couleur.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(libelleStatut(statut), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}
