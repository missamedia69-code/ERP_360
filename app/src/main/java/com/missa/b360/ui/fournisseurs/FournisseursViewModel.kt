package com.missa.b360.ui.fournisseurs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurContactDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurEvenementDao
import com.missa.b360.core.data.dao.FournisseurItemDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurContactEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.usecase.AjouterCompteBancaireUseCase
import com.missa.b360.core.domain.usecase.AjouterDocumentFournisseurUseCase
import com.missa.b360.core.domain.usecase.ChangerStatutFournisseurUseCase
import com.missa.b360.core.domain.usecase.CreateFournisseurUseCase
import com.missa.b360.core.domain.usecase.DelierArticleFournisseurUseCase
import com.missa.b360.core.domain.usecase.EvaluerFournisseurUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.LierArticleFournisseurUseCase
import com.missa.b360.core.domain.usecase.ObservePaymentMethodsUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.domain.usecase.ScannerDocumentsExpirantsUseCase
import com.missa.b360.core.domain.usecase.SoumettreFournisseurUseCase
import com.missa.b360.core.domain.usecase.SupprimerDocumentFournisseurUseCase
import com.missa.b360.core.domain.usecase.UpdateFournisseurUseCase
import com.missa.b360.core.domain.usecase.VerifierCompteBancaireUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Contact saisi dans le formulaire (persisté après création de la fiche). */
data class ContactSaisi(
    val nom: String = "",
    val prenom: String = "",
    val fonction: String = "",
    val telephone: String = "",
    val email: String = "",
    val principal: Boolean = false,
)

/** Document saisi dans le formulaire (fichier déjà copié via PieceJointeAchat). */
data class DocumentSaisi(
    val type: FournisseurDocType = FournisseurDocType.AUTRE,
    val reference: String = "",
    val cheminFichier: String? = null,
    val dateEmission: Long? = null,
    val dateExpiration: Long? = null,
)

/** Indicateurs du hub fournisseur (spec §4). */
data class HubFournisseurs(
    val actifs: Int = 0,
    val aValider: Int = 0,
    val soldeTotal: Double = 0.0,
    val commandesOuvertes: Int = 0,
    val comptesAVerifier: Int = 0,
    val documentsExpirants: Int = 0,
    val sansIdentifiantFiscal: Int = 0,
    val recents: List<FournisseurEntity> = emptyList(),
)

/** Fiche fournisseur complète : entité + enfants + KPI calculés. */
data class FicheFournisseur(
    val fournisseur: FournisseurEntity? = null,
    val contacts: List<FournisseurContactEntity> = emptyList(),
    val comptes: List<FournisseurCompteBancaireEntity> = emptyList(),
    val documents: List<FournisseurDocumentEntity> = emptyList(),
    val items: List<FournisseurItemEntity> = emptyList(),
    val nomsProduits: Map<Long, String> = emptyMap(),
    val evenements: List<FournisseurEvenementEntity> = emptyList(),
    val montantAchete: Double = 0.0,
    val nombreCommandes: Int = 0,
    val solde: Double = 0.0,
    val dernierePiece: Long? = null,
)

/** État du formulaire de création/édition en 7 étapes (spec §7). */
data class FournisseurFormState(
    val etape: Int = 1,
    val enEditionId: Long? = null,
    val statutCourant: FournisseurStatus = FournisseurStatus.BROUILLON,
    // Étape 1 — identité
    val type: TypeFournisseur = TypeFournisseur.ENTREPRISE,
    val nom: String = "",
    val nomCommercial: String = "",
    val pays: String = "CM",
    val devise: String = "XAF",
    val telephone: String = "",
    val email: String = "",
    val adresse: String = "",
    val siteWeb: String = "",
    val description: String = "",
    // Étape 3 — fiscalité
    val typeIdentifiant: String = "",
    val identifiantFiscal: String = "",
    val rccm: String = "",
    val assujettiTva: Boolean = true,
    val numTva: String = "",
    val tauxRetenue: String = "0",
    val exonere: Boolean = false,
    // Étape 4 — achats
    val categoriesFournies: String = "",
    val delaiMoyen: String = "0",
    val quantiteMin: String = "0",
    val montantMin: String = "0",
    val incoterm: String = "",
    // Étape 5 — paiement
    val conditionsPaiement: String = "",
    val joursEcheance: String = "0",
    val modePaiementPrefere: String = "",
    // Étapes 2 & 6 — enfants saisis avant création
    val contacts: List<ContactSaisi> = emptyList(),
    val documents: List<DocumentSaisi> = emptyList(),
    // Flux de sauvegarde
    val doublons: List<Pair<FournisseurEntity, List<String>>>? = null,
    val manquants: List<String>? = null,
    val erreur: String? = null,
    val enregistre: Boolean = false,
    val busy: Boolean = false,
) {
    val typeIdentifiantAttendu: String? get() = FournisseurRules.identifiantFiscalRequis(pays)
}

/**
 * ViewModel du module Fournisseurs : hub, liste filtrable, fiche complète,
 * formulaire en 7 étapes, workflow de statuts, comptes bancaires, documents
 * et liaisons articles — tout passe par les use cases (licence, audit).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FournisseursViewModel @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val contactDao: FournisseurContactDao,
    private val compteDao: FournisseurCompteBancaireDao,
    private val documentDao: FournisseurDocumentDao,
    private val itemDao: FournisseurItemDao,
    private val evenementDao: FournisseurEvenementDao,
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val createFournisseur: CreateFournisseurUseCase,
    private val updateFournisseur: UpdateFournisseurUseCase,
    private val soumettreFournisseur: SoumettreFournisseurUseCase,
    private val changerStatutUseCase: ChangerStatutFournisseurUseCase,
    private val ajouterCompte: AjouterCompteBancaireUseCase,
    private val verifierCompteUseCase: VerifierCompteBancaireUseCase,
    private val ajouterDocument: AjouterDocumentFournisseurUseCase,
    private val supprimerDocument: SupprimerDocumentFournisseurUseCase,
    private val lierArticle: LierArticleFournisseurUseCase,
    private val delierArticle: DelierArticleFournisseurUseCase,
    private val evaluerFournisseur: EvaluerFournisseurUseCase,
    private val scannerDocuments: ScannerDocumentsExpirantsUseCase,
    observeProducts: ObserveProductsUseCase,
    observePaymentMethods: ObservePaymentMethodsUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    val modesPaiement: StateFlow<List<String>> = observePaymentMethods()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Catalogue pour la liaison fournisseur ↔ article (spec §6.4). */
    val produits: StateFlow<List<ProductEntity>> = observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    // ------------------------------------------------------------------
    // Liste filtrable + hub
    // ------------------------------------------------------------------

    private val _filtreStatut = MutableStateFlow<FournisseurStatus?>(null)
    val filtreStatut: StateFlow<FournisseurStatus?> = _filtreStatut
    fun setFiltreStatut(statut: FournisseurStatus?) {
        _filtreStatut.value = statut
    }

    private val _recherche = MutableStateFlow("")
    val recherche: StateFlow<String> = _recherche
    fun setRecherche(value: String) {
        _recherche.value = value
    }

    private val tousFournisseurs: StateFlow<List<FournisseurEntity>> = fournisseurDao.observeTous()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listeFiltree: StateFlow<List<FournisseurEntity>> =
        combine(tousFournisseurs, _filtreStatut, _recherche) { tous, statut, query ->
            tous.filter { f ->
                (statut == null || f.statut == statut) &&
                    (query.isBlank() ||
                        f.nom.contains(query, ignoreCase = true) ||
                        f.code.contains(query, ignoreCase = true) ||
                        f.telephone.contains(query))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pièces d'achat (BC/RE/FA) — source des KPI et des soldes fournisseurs. */
    private val piecesAchat: StateFlow<List<com.missa.b360.core.data.entity.OperationRecordEntity>> =
        operationDao.observeByModule(OperationModule.ACHATS.name)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hub: StateFlow<HubFournisseurs> = combine(
        tousFournisseurs,
        compteDao.observeComptesAVerifier(),
        piecesAchat,
    ) { tous, comptesAVerifier, pieces ->
        val facturesValidees = pieces.filter {
            it.reference.startsWith("FA") && it.status == OperationStatus.VALIDATED.name
        }
        val soldeTotal = facturesValidees.sumOf { piece ->
            val payload = PurchaseRecordCodec.decode(piece.notes)
            if (payload == null) 0.0 else (payload.total - payload.paidAmount).coerceAtLeast(0.0)
        }
        val receptionsValidees = pieces.filter {
            it.reference.startsWith("RE") && it.status == OperationStatus.VALIDATED.name
        }.mapNotNull { ReceptionCodec.decode(it.notes)?.commandeRecordId }.toSet()
        val commandesOuvertes = pieces.count {
            it.reference.startsWith("BC") && it.status == OperationStatus.VALIDATED.name &&
                it.id !in receptionsValidees
        }
        HubFournisseurs(
            actifs = tous.count { it.statut == FournisseurStatus.ACTIF },
            aValider = tous.count { it.statut == FournisseurStatus.A_VALIDER },
            soldeTotal = soldeTotal,
            commandesOuvertes = commandesOuvertes,
            comptesAVerifier = comptesAVerifier,
            documentsExpirants = scannerDocuments.documentsExpirants().size,
            sansIdentifiantFiscal = tous.count {
                it.statut == FournisseurStatus.ACTIF &&
                    FournisseurRules.identifiantFiscalObligatoire(it.pays, it.type) &&
                    it.identifiantFiscal.isNullOrBlank()
            },
            recents = tous.sortedByDescending { it.createdAt }.take(5),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubFournisseurs())

    // ------------------------------------------------------------------
    // Fiche fournisseur
    // ------------------------------------------------------------------

    private val _ficheId = MutableStateFlow<Long?>(null)
    val ficheId: StateFlow<Long?> = _ficheId

    val fiche: StateFlow<FicheFournisseur> = combine(
        _ficheId,
        tousFournisseurs,
        piecesAchat,
    ) { id, tous, pieces ->
        Triple(id, tous.firstOrNull { it.id == id }, pieces)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(null, null, emptyList()))
        .let { base ->
            // Les enfants (contacts, comptes…) sont observés par fournisseur : un
            // second flux les combine avec la fiche courante.
            combine(base, enfantsFlow()) { (id, fournisseur, pieces), enfants ->
                val factures = pieces.filter {
                    it.reference.startsWith("FA") && it.status == OperationStatus.VALIDATED.name
                }.mapNotNull { piece -> piece to (PurchaseRecordCodec.decode(piece.notes) ?: return@mapNotNull null) }
                    .filter { (_, payload) -> id != null && payload.supplierId == id }
                val commandes = pieces.count { piece ->
                    piece.reference.startsWith("BC") &&
                        com.missa.b360.core.domain.model.CommandeAchatCodec.decode(piece.notes)
                            ?.let { it.supplierId == id } == true
                }
                FicheFournisseur(
                    fournisseur = fournisseur,
                    contacts = enfants.first,
                    comptes = enfants.second,
                    documents = enfants.third,
                    items = enfants.fourth.first,
                    nomsProduits = enfants.fourth.second,
                    evenements = enfants.fifth,
                    montantAchete = factures.sumOf { (_, payload) -> payload.total },
                    nombreCommandes = commandes,
                    solde = factures.sumOf { (_, payload) ->
                        (payload.total - payload.paidAmount).coerceAtLeast(0.0)
                    },
                    dernierePiece = factures.maxOfOrNull { (piece, _) -> piece.createdAt },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FicheFournisseur())
        }

    /**
     * Flux combiné des enfants du fournisseur courant. `flatMapLatest` garde un
     * seul abonnement actif ; les noms de produits sont résolus à chaque emission.
     */
    private fun enfantsFlow(): kotlinx.coroutines.flow.Flow<Quintuple> = _ficheId.flatMapLatest { id ->
        if (id == null) {
            kotlinx.coroutines.flow.flowOf(
                Quintuple(
                    first = emptyList(),
                    second = emptyList(),
                    third = emptyList(),
                    fourth = emptyList<FournisseurItemEntity>() to emptyMap<Long, String>(),
                    fifth = emptyList(),
                ),
            )
        } else {
            combine(
                contactDao.observeParFournisseur(id),
                compteDao.observeParFournisseur(id),
                documentDao.observeParFournisseur(id),
                itemDao.observeParFournisseur(id),
                evenementDao.observeParFournisseur(id),
            ) { contacts, comptes, documents, items, evenements ->
                val noms = items.map { it.productId }.distinct().associateWith { produitId ->
                    productDao.getById(produitId)?.nom ?: "#$produitId"
                }
                Quintuple(contacts, comptes, documents, items to noms, evenements)
            }
        }
    }

    private data class Quintuple(
        val first: List<FournisseurContactEntity>,
        val second: List<FournisseurCompteBancaireEntity>,
        val third: List<FournisseurDocumentEntity>,
        val fourth: Pair<List<FournisseurItemEntity>, Map<Long, String>>,
        val fifth: List<FournisseurEvenementEntity>,
    )

    fun ouvrirFiche(id: Long?) {
        _ficheId.value = id
    }

    // ------------------------------------------------------------------
    // Formulaire 7 étapes
    // ------------------------------------------------------------------

    private val _form = MutableStateFlow(FournisseurFormState())
    val form: StateFlow<FournisseurFormState> = _form
    fun updateForm(transform: (FournisseurFormState) -> FournisseurFormState) {
        _form.value = transform(_form.value)
    }

    fun ouvrirFormulaire(fournisseur: FournisseurEntity?) {
        _form.value = if (fournisseur == null) {
            FournisseurFormState(devise = devise.value)
        } else {
            FournisseurFormState(
                enEditionId = fournisseur.id,
                statutCourant = fournisseur.statut,
                type = fournisseur.type,
                nom = fournisseur.nom,
                nomCommercial = fournisseur.nomCommercial.orEmpty(),
                pays = fournisseur.pays,
                devise = fournisseur.devise,
                telephone = fournisseur.telephone,
                email = fournisseur.email.orEmpty(),
                adresse = fournisseur.adresse.orEmpty(),
                siteWeb = fournisseur.siteWeb.orEmpty(),
                description = fournisseur.description.orEmpty(),
                typeIdentifiant = fournisseur.typeIdentifiantFiscal.orEmpty(),
                identifiantFiscal = fournisseur.identifiantFiscal.orEmpty(),
                rccm = fournisseur.rccm.orEmpty(),
                assujettiTva = fournisseur.assujettiTva,
                numTva = fournisseur.numTva.orEmpty(),
                tauxRetenue = fournisseur.tauxRetenue.toString(),
                exonere = fournisseur.exonere,
                categoriesFournies = fournisseur.categoriesFournies.orEmpty(),
                delaiMoyen = fournisseur.delaiMoyenJours.toString(),
                quantiteMin = fournisseur.quantiteMinCommande.toString(),
                montantMin = fournisseur.montantMinCommande.toString(),
                incoterm = fournisseur.incoterm.orEmpty(),
                conditionsPaiement = fournisseur.conditionsPaiement.orEmpty(),
                joursEcheance = fournisseur.joursEcheance.toString(),
                modePaiementPrefere = fournisseur.modePaiementPrefere.orEmpty(),
            )
        }
    }

    fun etapePrecedente() {
        _form.value = _form.value.copy(
            etape = (_form.value.etape - 1).coerceAtLeast(1),
            erreur = null,
        )
    }

    fun etapeSuivante() {
        val etat = _form.value
        val erreur = when (etat.etape) {
            1 -> when {
                etat.nom.isBlank() -> "raison_sociale"
                etat.telephone.isBlank() && etat.email.isBlank() -> "contact_tel_ou_email"
                else -> null
            }
            3 -> if (FournisseurRules.identifiantFiscalObligatoire(etat.pays, etat.type) &&
                etat.identifiantFiscal.isBlank()
            ) {
                "identifiant_fiscal"
            } else {
                null
            }
            5 -> if (etat.conditionsPaiement.isBlank()) "conditions_paiement" else null
            else -> null
        }
        if (erreur != null) {
            _form.value = etat.copy(erreur = erreur)
            return
        }
        _form.value = etat.copy(etape = (etat.etape + 1).coerceAtMost(7), erreur = null)
    }

    fun addContactSaisi(contact: ContactSaisi) {
        if (contact.nom.isBlank()) return
        _form.value = _form.value.copy(
            contacts = _form.value.contacts.let { liste ->
                val sansAncienPrincipal = if (contact.principal) {
                    liste.map { it.copy(principal = false) }
                } else {
                    liste
                }
                sansAncienPrincipal + contact
            },
        )
    }

    fun removeContactSaisi(index: Int) {
        _form.value = _form.value.copy(
            contacts = _form.value.contacts.filterIndexed { i, _ -> i != index },
        )
    }

    fun addDocumentSaisi(document: DocumentSaisi) {
        _form.value = _form.value.copy(documents = _form.value.documents + document)
    }

    fun removeDocumentSaisi(index: Int) {
        _form.value = _form.value.copy(
            documents = _form.value.documents.filterIndexed { i, _ -> i != index },
        )
    }

    private fun entiteDepuisForm(etat: FournisseurFormState): FournisseurEntity = FournisseurEntity(
        id = etat.enEditionId ?: 0,
        code = "", // généré à la création
        nom = etat.nom.trim(),
        telephone = etat.telephone.trim(),
        email = etat.email.trim().ifBlank { null },
        adresse = etat.adresse.trim().ifBlank { null },
        statut = etat.statutCourant,
        createdAt = 0,
        type = etat.type,
        nomCommercial = etat.nomCommercial.trim().ifBlank { null },
        pays = etat.pays,
        devise = etat.devise,
        siteWeb = etat.siteWeb.trim().ifBlank { null },
        description = etat.description.trim().ifBlank { null },
        typeIdentifiantFiscal = etat.typeIdentifiant.trim()
            .ifBlank { etat.typeIdentifiantAttendu }
            ?.ifBlank { null },
        identifiantFiscal = etat.identifiantFiscal.trim().ifBlank { null },
        rccm = etat.rccm.trim().ifBlank { null },
        assujettiTva = etat.assujettiTva,
        numTva = etat.numTva.trim().ifBlank { null },
        tauxRetenue = etat.tauxRetenue.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0,
        exonere = etat.exonere,
        conditionsPaiement = etat.conditionsPaiement.trim().ifBlank { null },
        joursEcheance = etat.joursEcheance.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        modePaiementPrefere = etat.modePaiementPrefere.ifBlank { null },
        categoriesFournies = etat.categoriesFournies.trim().ifBlank { null },
        delaiMoyenJours = etat.delaiMoyen.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        quantiteMinCommande = etat.quantiteMin.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
        montantMinCommande = etat.montantMin.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
        incoterm = etat.incoterm.trim().ifBlank { null },
    )

    /** Enregistrer en brouillon (toujours possible) ou soumettre à validation. */
    fun enregistrer(soumettre: Boolean) {
        val etat = _form.value
        if (etat.busy) return
        _form.value = etat.copy(busy = true, erreur = null, manquants = null)
        viewModelScope.launch {
            val entite = entiteDepuisForm(etat)
            val doublonConfirme = etat.doublons != null
            when (val resultat = createFournisseur(entite, doublonConfirme = doublonConfirme)) {
                is CreateFournisseurUseCase.Result.LicenceExpiree ->
                    _form.value = _form.value.copy(busy = false, erreur = "licence")
                is CreateFournisseurUseCase.Result.ChampsManquants ->
                    _form.value = _form.value.copy(busy = false, manquants = resultat.champs)
                is CreateFournisseurUseCase.Result.DoublonPotentiel -> {
                    val fiches = resultat.fiches.mapNotNull { (id, motifs) ->
                        fournisseurDao.getById(id)?.let { it to motifs }
                    }
                    _form.value = _form.value.copy(busy = false, doublons = fiches)
                }
                is CreateFournisseurUseCase.Result.Succes -> {
                    persisterEnfants(resultat.fournisseurId, etat)
                    if (soumettre) {
                        when (val soumis = soumettreFournisseur(resultat.fournisseurId)) {
                            is SoumettreFournisseurUseCase.Result.ChampsManquants ->
                                _form.value = _form.value.copy(busy = false, manquants = soumis.champs)
                            else -> Unit
                        }
                    }
                    _form.value = _form.value.copy(busy = false, enregistre = true)
                    _message.value = "msg_fournisseur_enregistre"
                    ouvrirFiche(resultat.fournisseurId)
                }
            }
        }
    }

    fun confirmerDoublon() {
        enregistrer(soumettre = false)
    }

    fun annulerDoublon() {
        _form.value = _form.value.copy(doublons = null)
    }

    fun modifierFiche() {
        val etat = _form.value
        val id = etat.enEditionId ?: return
        if (etat.busy) return
        _form.value = etat.copy(busy = true, erreur = null)
        viewModelScope.launch {
            val existant = fournisseurDao.getById(id)
            if (existant == null) {
                _form.value = _form.value.copy(busy = false, erreur = "introuvable")
                return@launch
            }
            val modifie = entiteDepuisForm(etat).copy(
                code = existant.code,
                createdAt = existant.createdAt,
                statut = existant.statut,
                soumisLe = existant.soumisLe,
                approuveLe = existant.approuveLe,
                approuve = existant.approuve,
                plafondPaiement = existant.plafondPaiement,
                paiementBloque = existant.paiementBloque,
                noteEvaluation = existant.noteEvaluation,
                commentaireEvaluation = existant.commentaireEvaluation,
                dateEvaluation = existant.dateEvaluation,
            )
            val ok = updateFournisseur(modifie)
            _form.value = _form.value.copy(busy = false, enregistre = ok)
            _message.value = if (ok) "msg_fournisseur_modifie" else "err_modification"
        }
    }

    private suspend fun persisterEnfants(fournisseurId: Long, etat: FournisseurFormState) {
        etat.contacts.forEach { contact ->
            contactDao.insert(
                FournisseurContactEntity(
                    fournisseurId = fournisseurId,
                    nom = contact.nom.trim(),
                    prenom = contact.prenom.trim().ifBlank { null },
                    fonction = contact.fonction.trim().ifBlank { null },
                    telephone = contact.telephone.trim().ifBlank { null },
                    email = contact.email.trim().ifBlank { null },
                    principal = contact.principal,
                ),
            )
        }
        etat.documents.forEach { document ->
            ajouterDocument(
                fournisseurId = fournisseurId,
                typeDocument = document.type,
                reference = document.reference,
                cheminFichier = document.cheminFichier,
                dateEmission = document.dateEmission,
                dateExpiration = document.dateExpiration,
            )
        }
    }

    // ------------------------------------------------------------------
    // Actions de la fiche
    // ------------------------------------------------------------------

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun clearMessage() {
        _message.value = null
    }

    fun changerStatut(vers: FournisseurStatus, motif: String? = null) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            _message.value = when (changerStatutUseCase(id, vers, motif)) {
                ChangerStatutFournisseurUseCase.Result.Succes -> "msg_statut_mis_a_jour"
                ChangerStatutFournisseurUseCase.Result.MotifObligatoire -> "err_motif_obligatoire"
                ChangerStatutFournisseurUseCase.Result.TransitionRefusee -> "err_transition"
                ChangerStatutFournisseurUseCase.Result.Introuvable -> "err_introuvable"
            }
        }
    }

    fun soumettre() {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            _message.value = when (val resultat = soumettreFournisseur(id)) {
                SoumettreFournisseurUseCase.Result.Succes -> "msg_soumis"
                is SoumettreFournisseurUseCase.Result.ChampsManquants -> "err_dossier_incomplet"
                SoumettreFournisseurUseCase.Result.TransitionRefusee -> "err_transition"
            }
        }
    }

    fun ajouterContact(nom: String, prenom: String, fonction: String, telephone: String, email: String, principal: Boolean) {
        val id = _ficheId.value ?: return
        if (nom.isBlank()) return
        viewModelScope.launch {
            if (principal) contactDao.retirerRôlePrincipal(id)
            contactDao.insert(
                FournisseurContactEntity(
                    fournisseurId = id,
                    nom = nom.trim(),
                    prenom = prenom.trim().ifBlank { null },
                    fonction = fonction.trim().ifBlank { null },
                    telephone = telephone.trim().ifBlank { null },
                    email = email.trim().ifBlank { null },
                    principal = principal,
                ),
            )
            _message.value = "msg_contact_ajoute"
        }
    }

    fun ajouterCompte(
        titulaire: String,
        banque: String,
        numeroCompte: String,
        iban: String,
        bicSwift: String,
        operateurMobile: String,
        numeroMobile: String,
        principal: Boolean,
    ) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            val compte = FournisseurCompteBancaireEntity(
                fournisseurId = id,
                titulaire = titulaire.trim(),
                banque = banque.trim().ifBlank { null },
                numeroCompte = numeroCompte.trim().ifBlank { null },
                iban = iban.trim().ifBlank { null },
                bicSwift = bicSwift.trim().ifBlank { null },
                operateurMobile = operateurMobile.trim().ifBlank { null },
                numeroMobile = numeroMobile.trim().ifBlank { null },
                principal = principal,
            )
            _message.value = if (ajouterCompte(compte) != null) "msg_compte_ajoute" else "err_compte_incomplet"
        }
    }

    fun verifierCompte(compteId: Long, approuve: Boolean) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            verifierCompteUseCase(compteId, id, approuve)
            _message.value = if (approuve) "msg_compte_verifie" else "msg_compte_rejete"
        }
    }

    fun ajouterDocumentFiche(
        type: FournisseurDocType,
        reference: String,
        cheminFichier: String?,
        dateEmission: Long?,
        dateExpiration: Long?,
    ) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            val resultat = ajouterDocument(id, type, reference, cheminFichier, dateEmission, dateExpiration)
            _message.value = if (resultat != null) "msg_document_ajoute" else "err_document"
        }
    }

    fun supprimerDocumentFiche(documentId: Long) {
        viewModelScope.launch {
            supprimerDocument(documentId)
            _message.value = "msg_document_retire"
        }
    }

    fun lierArticleFiche(
        productId: Long,
        reference: String,
        prix: Double,
        delaiJours: Int,
        quantiteMin: Double,
        prefere: Boolean,
    ) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            val resultat = lierArticle(
                FournisseurItemEntity(
                    fournisseurId = id,
                    productId = productId,
                    reference = reference.trim().ifBlank { null },
                    prixUnitaire = prix,
                    delaiJours = delaiJours,
                    quantiteMin = quantiteMin,
                    prefere = prefere,
                ),
            )
            _message.value = if (resultat != null) "msg_article_lie" else "err_article"
        }
    }

    fun delierArticleFiche(liaisonId: Long) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            delierArticle(liaisonId, id)
            _message.value = "msg_article_delie"
        }
    }

    fun evaluer(note: Double, commentaire: String) {
        val id = _ficheId.value ?: return
        viewModelScope.launch {
            val ok = evaluerFournisseur(id, note, commentaire)
            _message.value = if (ok) "msg_evaluation" else "err_evaluation"
        }
    }
}
