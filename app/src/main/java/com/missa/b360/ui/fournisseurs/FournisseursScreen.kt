package com.missa.b360.ui.fournisseurs

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

/** Marron caractéristique du module Fournisseurs — source unique : [AppModule.FOURNISSEURS]. */
private val CouleurFournisseurs: Color get() = AppModule.FOURNISSEURS.couleur


private val PAYS = listOf("CM", "SN", "CI", "GA", "GN", "MA", "FR", "BE", "AE", "NG", "KE", "GH", "CA")
private val DEVISES = listOf("XAF", "XOF", "EUR", "USD", "MAD", "NGN", "CAD")
private val INCOTERMS = listOf("EXW", "FOB", "CIF", "DAP", "DDP")
private val OPERATEURS_MOBILE = listOf("MTN", "Orange", "Moov", "Wave", "M-Pesa", "Airtel")

/**
 * Route `fournisseur_fiche/{id}` : fiche d'un fournisseur (refonte « Fiche 360 » prévue en F5).
 * Chaque destination a son propre ViewModel : la fiche est ouverte à l'entrée de la route.
 */
@Composable
fun FournisseurFicheRoute(
    id: Long,
    onBack: () -> Unit,
    onModifier: (Long) -> Unit,
) {
    val vm: FournisseursViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.ouvrirFiche(id) }
    FicheFournisseurEcran(vm = vm, onBack = onBack, onModifier = { fournisseur -> onModifier(fournisseur.id) })
}

/**
 * Route `fournisseur_edition/{id}` : formulaire en 7 étapes ; [id] 0 = création.
 * [onTermine] reçoit l'identifiant du fournisseur enregistré.
 */
@Composable
fun FournisseurEditionRoute(
    id: Long,
    onBack: () -> Unit,
    onTermine: (Long?) -> Unit,
) {
    val vm: FournisseursViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.ouvrirEdition(id) }
    FormulaireFournisseur(vm = vm, onBack = onBack, onTermine = onTermine)
}

// ======================================================================
// Libellés lisibles (valeurs codées → texte traduit)
// ======================================================================

@Composable
private fun libelleMessageFournisseur(code: String): String = stringResource(
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
private fun libelleStatut(statut: FournisseurStatus): String = stringResource(
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
private fun libelleType(type: TypeFournisseur): String = stringResource(
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
private fun libelleDocument(type: FournisseurDocType): String = stringResource(
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
private fun libelleEvenement(type: FournisseurEvenementType): String = stringResource(
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
private fun libelleManquant(cle: String): String = stringResource(
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
private fun libelleMotifDoublon(cle: String): String = stringResource(
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

private fun couleurStatut(statut: FournisseurStatus): Color = when (statut) {
    FournisseurStatus.BROUILLON -> Color(0xFF6B7280)
    FournisseurStatus.A_VALIDER -> Color(0xFFD97706)
    FournisseurStatus.ACTIF -> Color(0xFF15803D)
    FournisseurStatus.SUSPENDU -> Color(0xFFB45309)
    FournisseurStatus.BLOQUE -> Color(0xFFB91C1C)
    FournisseurStatus.ARCHIVE -> Color(0xFF9CA3AF)
}

private fun fmtDate(millis: Long?): String =
    if (millis == null) "—" else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

@Composable
private fun BadgeStatut(statut: FournisseurStatus) {
    val couleur = couleurStatut(statut)
    Box(
        Modifier.background(couleur.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(libelleStatut(statut), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}

// ======================================================================
// Fiche fournisseur (spec §6) — sections repliables
// ======================================================================

@Composable
private fun FicheFournisseurEcran(
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
                                        blocageOuvert = true
                                    }
                                }
                                FournisseurStatus.SUSPENDU -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BoutonAction(stringResource(R.string.four_reactiver), Modifier.weight(1f)) {
                                        vm.changerStatut(FournisseurStatus.ACTIF)
                                    }
                                    BoutonAction(stringResource(R.string.four_bloquer), Modifier.weight(1f)) {
                                        blocageOuvert = true
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
                            onClick = { evaluationOuverte = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(painterResource(Iv.QualityBadge), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.four_evaluer), fontSize = 12.sp, color = MissaInk)
                        }
                    }
                }
            }

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

            // --- Contacts ---
            item {
                SectionRepliable(
                    titre = stringResource(R.string.four_contacts),
                    icone = Iv.People,
                    action = {
                        TextButton(onClick = { contactOuvert = true }) {
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

            // --- Achats & articles liés ---
            item {
                SectionRepliable(
                    titre = stringResource(R.string.four_achats_section),
                    icone = Iv.CartArrowDown,
                    action = {
                        TextButton(onClick = { articleOuvert = true }) {
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

            // --- Paiement & comptes ---
            item {
                SectionRepliable(
                    titre = stringResource(R.string.four_paiement_section),
                    icone = Iv.Payments,
                    action = {
                        TextButton(onClick = { compteOuvert = true }) {
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

            // --- Documents ---
            item {
                SectionRepliable(
                    titre = stringResource(R.string.four_documents),
                    icone = Iv.Description,
                    action = {
                        TextButton(onClick = { documentOuvert = true }) {
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

@Composable
private fun BoutonAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = CouleurFournisseurs, contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun LigneInfo(label: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = MissaMuted, modifier = Modifier.weight(1f))
        Text(valeur, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MissaInk)
    }
}

@Composable
private fun SectionRepliable(
    titre: String,
    icone: Int,
    action: (@Composable () -> Unit)? = null,
    contenu: @Composable () -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { ouvert = !ouvert },
            ) {
                Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(titre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk, modifier = Modifier.weight(1f))
                action?.invoke()
                Icon(
                    painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
                    null,
                    tint = MissaInk,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (ouvert) {
                Spacer(Modifier.height(6.dp))
                contenu()
            }
        }
    }
}

private fun masquerReferenceConfidentielle(valeur: String?): String? = valeur?.let { reference ->
    val compacte = reference.filterNot { it.isWhitespace() }
    when {
        compacte.isEmpty() -> null
        compacte.length <= 4 -> "••••"
        else -> "•••• ${compacte.takeLast(4)}"
    }
}

@Composable
private fun LigneCompte(
    compte: FournisseurCompteBancaireEntity,
    onVerifier: () -> Unit,
    onRejeter: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Iv.Bank), null, tint = MissaInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(compte.titulaire)
                        if (compte.principal) append(" · ").append("★")
                    },
                    fontSize = 12.sp,
                    color = MissaInk,
                )
                Text(
                    listOfNotNull(
                        compte.banque ?: compte.operateurMobile,
                        masquerReferenceConfidentielle(compte.numeroCompte ?: compte.iban ?: compte.numeroMobile),
                    ).joinToString(" · "),
                    fontSize = 10.sp,
                    color = MissaMuted,
                )
            }
            BadgeVerification(compte.verification)
        }
        if (compte.verification == VerificationStatut.A_VERIFIER) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onVerifier) {
                    Text(stringResource(R.string.four_verifier), fontSize = 11.sp, color = Color(0xFF15803D))
                }
                TextButton(onClick = onRejeter) {
                    Text(stringResource(R.string.four_rejeter), fontSize = 11.sp, color = Color(0xFFB91C1C))
                }
            }
        }
    }
}

@Composable
private fun BadgeVerification(statut: VerificationStatut) {
    val couleur = when (statut) {
        VerificationStatut.A_VERIFIER -> Color(0xFFD97706)
        VerificationStatut.VERIFIE -> Color(0xFF15803D)
        VerificationStatut.REJETE -> Color(0xFFB91C1C)
    }
    val texte = when (statut) {
        VerificationStatut.A_VERIFIER -> stringResource(R.string.four_a_verifier)
        VerificationStatut.VERIFIE -> stringResource(R.string.four_verifie)
        VerificationStatut.REJETE -> stringResource(R.string.four_rejete)
    }
    Box(Modifier.background(couleur.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(texte, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}

@Composable
private fun LigneDocument(
    document: FournisseurDocumentEntity,
    onSupprimer: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val alerte = FournisseurRules.alerteDocument(document.dateExpiration, now)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        var vignette by remember(document.cheminFichier) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(document.cheminFichier) {
            vignette = document.cheminFichier?.let { PieceJointeAchat.charger(it) }
        }
        if (vignette != null) {
            androidx.compose.foundation.Image(
                bitmap = vignette!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(27.dp).background(MissaBorder, RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                painterResource(if (document.cheminFichier?.let(PieceJointeAchat::estPdf) == true) Iv.PictureAsPdf else Iv.Description),
                null,
                tint = MissaInk,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(libelleDocument(document.typeDocument))
                    document.reference?.let { append(" · ").append(it) }
                },
                fontSize = 12.sp,
                color = MissaInk,
            )
            Text(
                buildString {
                    append(stringResource(R.string.four_expire_le))
                    append(" ")
                    append(fmtDate(document.dateExpiration))
                },
                fontSize = 10.sp,
                color = when (alerte) {
                    FournisseurRules.AlerteDocument.EXPIRE, FournisseurRules.AlerteDocument.FORTE -> Color(0xFFB91C1C)
                    FournisseurRules.AlerteDocument.ALERTE -> Color(0xFFB45309)
                    else -> MissaMuted
                },
            )
        }
        IconButton(onClick = onSupprimer, modifier = Modifier.size(27.dp)) {
            Icon(
                painterResource(Iv.DeleteOutline),
                null,
                tint = Color(0xFFB91C1C),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}



// ======================================================================
// Dialogues de la fiche
// ======================================================================

@Composable
private fun DialogueMotifBlocage(onConfirmer: (String) -> Unit, onAnnuler: () -> Unit) {
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
private fun DialogueContact(
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
private fun DialogueCompte(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogueDocument(
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
private fun ChampDateFour(label: String, millis: Long?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier) {
        MissaChampTexte(fmtDate(millis), { }, label, icone = Iv.Calendar, lectureSeule = true)
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
private fun DialogueArticle(
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
private fun DialogueEvaluation(
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

// ======================================================================
// Formulaire 7 étapes (spec §7)
// ======================================================================

@Composable
private fun FormulaireFournisseur(
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
private fun SelecteurSimple(
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

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeIdentite(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    item {
        var typeOuvert by remember { mutableStateOf(false) }
        MissaChampListe(
            libelle = stringResource(R.string.four_type_fournisseur),
            options = TypeFournisseur.entries.map { option -> option to libelleType(option) },
            selection = form.type,
            onSelection = { option -> vm.updateForm { it.copy(type = option) } },
            icone = Iv.Category,
        )
    }
    item {
        MissaRangee {
            MissaChampTexte(form.nom, { valeur -> vm.updateForm { it.copy(nom = valeur) } }, stringResource(R.string.four_raison_sociale), icone = Iv.Business, requis = true, modifier = Modifier.weight(1f))
            MissaChampTexte(form.nomCommercial, { valeur -> vm.updateForm { it.copy(nomCommercial = valeur) } }, stringResource(R.string.four_nom_commercial), icone = Iv.Business, modifier = Modifier.weight(1f))
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelecteurSimple(
                icone = Iv.Public,
                label = stringResource(R.string.four_pays) + " *",
                valeur = form.pays,
                options = PAYS,
                modifier = Modifier.weight(1f),
            ) { choix -> vm.updateForm { it.copy(pays = choix) } }
            SelecteurSimple(
                icone = Iv.Payments,
                label = stringResource(R.string.four_devise),
                valeur = form.devise,
                options = DEVISES,
                modifier = Modifier.weight(1f),
            ) { choix -> vm.updateForm { it.copy(devise = choix) } }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MissaChampTexte(form.telephone, { valeur -> vm.updateForm { it.copy(telephone = valeur) } }, stringResource(R.string.four_telephone), modifier = Modifier.weight(1f), icone = Iv.Call, clavier = MissaClavier.TELEPHONE, requis = true)
            MissaChampTexte(form.email, { valeur -> vm.updateForm { it.copy(email = valeur) } }, stringResource(R.string.four_email), modifier = Modifier.weight(1f), icone = Iv.MailOutline, clavier = MissaClavier.EMAIL)
        }
    }
    item {
        MissaRangee {
            MissaChampTexte(form.adresse, { valeur -> vm.updateForm { it.copy(adresse = valeur) } }, stringResource(R.string.four_adresse), icone = Iv.Place, modifier = Modifier.weight(1f))
            MissaChampTexte(form.siteWeb, { valeur -> vm.updateForm { it.copy(siteWeb = valeur) } }, stringResource(R.string.four_site_web), icone = Iv.Public, modifier = Modifier.weight(1f))
        }
    }
    item {
        MissaChampTexte(form.description, { valeur -> vm.updateForm { it.copy(description = valeur) } }, stringResource(R.string.four_description), icone = Iv.Description)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeContacts(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    item {
        var nom by remember { mutableStateOf("") }
        var prenom by remember { mutableStateOf("") }
        var fonction by remember { mutableStateOf("") }
        var telephone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var principal by remember { mutableStateOf(true) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (form.contacts.isEmpty()) {
                    Text(stringResource(R.string.four_aucun_contact), fontSize = 11.sp, color = MissaMuted)
                }
                form.contacts.forEachIndexed { index, contact ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact.nom + if (contact.principal) " ★" else "",
                                fontSize = 12.sp,
                                color = MissaInk,
                            )
                            Text(
                                listOfNotNull(
                                    contact.fonction.ifBlank { null },
                                    contact.telephone.ifBlank { null },
                                    contact.email.ifBlank { null },
                                ).joinToString(" · "),
                                fontSize = 10.sp,
                                color = MissaMuted,
                            )
                        }
                        IconButton(
                            onClick = { vm.removeContactSaisi(index) },
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
                Text(stringResource(R.string.four_ajouter_contact), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
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
                Button(
                    onClick = {
                        vm.addContactSaisi(
                            ContactSaisi(
                                nom = nom,
                                prenom = prenom,
                                fonction = fonction,
                                telephone = telephone,
                                email = email,
                                principal = principal,
                            ),
                        )
                        nom = ""
                        prenom = ""
                        fonction = ""
                        telephone = ""
                        email = ""
                    },
                    enabled = nom.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurFournisseurs, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.Add), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.four_ajouter), fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeFiscalite(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    val requis = FournisseurRules.identifiantFiscalRequis(form.pays)
    item {
        if (form.type == TypeFournisseur.PARTICULIER) {
            Text(
                stringResource(R.string.four_fiscal_non_requis),
                fontSize = 11.sp,
                color = MissaMuted,
            )
        } else if (requis != null) {
            MissaChampTexte(form.identifiantFiscal, { valeur -> vm.updateForm { it.copy(identifiantFiscal = valeur) } }, requis, icone = Iv.Badge, requis = true)
        } else {
            MissaChampTexte(form.identifiantFiscal, { valeur -> vm.updateForm { it.copy(identifiantFiscal = valeur) } }, stringResource(R.string.four_identifiant), icone = Iv.Badge)
        }
    }
    item {
        MissaChampTexte(form.rccm, { valeur -> vm.updateForm { it.copy(rccm = valeur) } }, stringResource(R.string.four_rccm), icone = Iv.Badge)
    }
    item {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MissaInterrupteur(
                    actif = form.assujettiTva,
                    onChange = { valeur -> vm.updateForm { it.copy(assujettiTva = valeur) } },
                    libelle = stringResource(R.string.four_assujetti_tva),
                )
                if (form.assujettiTva) {
                    MissaChampTexte(form.numTva, { valeur -> vm.updateForm { it.copy(numTva = valeur) } }, stringResource(R.string.four_num_tva), icone = Iv.Badge)
                }
                MissaInterrupteur(
                    actif = form.exonere,
                    onChange = { valeur -> vm.updateForm { it.copy(exonere = valeur) } },
                    libelle = stringResource(R.string.four_exonere),
                )
                if (!form.exonere) {
                    MissaChampTexte(form.tauxRetenue, { valeur -> vm.updateForm { it.copy(tauxRetenue = valeur.filterMoneyInput()) } }, stringResource(R.string.four_taux_retenue), icone = Iv.Percent, clavier = MissaClavier.DECIMAL)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeAchats(
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

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapePaiement(
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
private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeDocuments(
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

private fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeValidation(
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
