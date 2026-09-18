package com.missa.b360.ui.stock

import com.missa.b360.core.domain.usecase.CategorieProduitUseCases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.entity.InventaireEntity
import com.missa.b360.core.data.entity.InventaireLigneEntity
import com.missa.b360.core.data.entity.ProductStatus
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.usecase.RecordStockMovementUseCase
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.ObserveStockMovementsUseCase
import com.missa.b360.core.domain.usecase.SiteUseCases
import com.missa.b360.core.domain.usecase.SupprimerProduitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** Ligne « catégorie de stock » (type d'article) de l'accueil et de l'écran catégories. */
data class CatStockRow(
    val type: ProductType,
    val nomRes: Int,
    val nombre: Int,
    val valeur: Double,
)

data class StockAccueilState(
    val devise: String = "",
    val valeur: Double = 0.0,
    val tendance: Double? = null,
    val nbArticles: Int = 0,
    val nbCategories: Int = 0,
    val critiques: Int = 0,
    val ruptures: Int = 0,
    val categories: List<CatStockRow> = emptyList(),
    /** Catégories librement créées par l'utilisateur (table product_categories). */
    val categoriesLibres: List<CatLibreRow> = emptyList(),
    val entreesJour: Int = 0,
    val sortiesJour: Int = 0,
    val transfertsJour: Int = 0,
    /** Quantités entrées / sorties sur 30 jours glissants (tableau de bord). */
    val entrees30j: Double = 0.0,
    val sorties30j: Double = 0.0,
    /** Trois catégories les plus valorisées (tableau de bord). */
    val topCategories: List<CatStockRow> = emptyList(),
    /** Activité mensuelle entrées/sorties, 6 derniers mois (tableau de bord). */
    val activite: List<MoisActivite> = emptyList(),
    /** Une session d'inventaire physique est en cours. */
    val inventaireEnCours: Boolean = false,
    /** Valorisation et nombre des immobilisations (équipements, matériel, pièces). */
    val valeurImmo: Double = 0.0,
    val nbImmo: Int = 0,
)

/** Barre mensuelle du graphique d'activité du tableau de bord. */
data class MoisActivite(val label: String, val entrees: Double, val sorties: Double)

/** Ligne d'une catégorie utilisateur dans la matrice : id, nom et nombre d'articles. */
data class CatLibreRow(
    val id: Long,
    val nom: String,
    val nombre: Int,
)

/** Accueil du module Stock : valorisation, compteurs, catégories et mouvements du jour. */
@HiltViewModel
class StockAccueilViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    observeMovements: ObserveStockMovementsUseCase,
    getEnterprise: GetEnterpriseUseCase,
    private val categoriesUseCases: CategorieProduitUseCases,
    inventaireDao: com.missa.b360.core.data.dao.InventaireDao,
) : ViewModel() {

    /** Crée une catégorie utilisateur depuis la matrice (matrice rafraîchie par le flux). */
    fun creerCategorie(nom: String) {
        viewModelScope.launch { categoriesUseCases.creer(nom) }
    }

    private val _suppressionCategorie = kotlinx.coroutines.flow.MutableStateFlow<CategorieProduitUseCases.SuppressionResult?>(null)
    val suppressionCategorie: StateFlow<CategorieProduitUseCases.SuppressionResult?> = _suppressionCategorie

    /** Supprime une catégorie utilisateur (refusée si des articles y sont rattachés). */
    fun supprimerCategorie(id: Long) {
        viewModelScope.launch { _suppressionCategorie.value = categoriesUseCases.supprimer(id) }
    }

    fun clearSuppressionCategorie() {
        _suppressionCategorie.value = null
    }

    val etat: StateFlow<StockAccueilState> = combine(
        combine(observeProducts(), observeStock()) { produits, stocks ->
            ProductStocks.combine(produits, stocks)
        },
        observeMovements(1_500),
        getEnterprise.observer(),
        categoriesUseCases.observer(),
        inventaireDao.observeEnCours(),
    ) { lignes, mouvements, entreprise, catsLibres, sessionInventaire ->
        val devise = entreprise?.devise.orEmpty()
        // Valorisation au coût de revient, à défaut au prix d'achat (même règle que StockHubRules).
        val valeur = lignes.sumOf { l ->
            l.total * (l.product.prixRevient ?: l.product.prixAchat ?: 0.0)
        }
        val parType = ProductType.entries.map { type ->
            val duType = lignes.filter { it.product.type == type }
            CatStockRow(
                type = type,
                nomRes = type.libelleCatRes(),
                nombre = duType.size,
                valeur = duType.sumOf { l -> l.total * (l.product.prixRevient ?: l.product.prixAchat ?: 0.0) },
            )
        }
        val debutJour = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val hier = debutJour - 86_400_000L
        val duJour = mouvements.filter { it.horodatage >= debutJour }
        val dHier = mouvements.filter { it.horodatage in hier until debutJour }
        val entrees = duJour.count { it.type == "ENTREE" }
        val sorties = duJour.count { it.type == "SORTIE" }
        val transferts = duJour.count { it.type == "TRANSFERT_SORTIE" }
        val totalJour = duJour.size
        val totalHier = dHier.size
        val cutoff30 = System.currentTimeMillis() - 30L * 86_400_000L
        val entrees30j = mouvements.filter { it.type == "ENTREE" && it.horodatage >= cutoff30 }.sumOf { abs(it.quantite) }
        val sorties30j = mouvements.filter { it.type == "SORTIE" && it.horodatage >= cutoff30 }.sumOf { abs(it.quantite) }
        val activite = (5 downTo 0).map { back ->
            val debut = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -back)
            }.timeInMillis
            val fin = Calendar.getInstance().apply {
                timeInMillis = debut
                add(Calendar.MONTH, 1)
            }.timeInMillis
            val duMois = mouvements.filter { it.horodatage in debut until fin }
            MoisActivite(
                label = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(java.util.Date(debut)),
                entrees = duMois.filter { it.type == "ENTREE" }.sumOf { abs(it.quantite) },
                sorties = duMois.filter { it.type == "SORTIE" }.sumOf { abs(it.quantite) },
            )
        }
        StockAccueilState(
            devise = devise,
            valeur = valeur,
            tendance = if (totalHier > 0) (totalJour - totalHier).toDouble() / totalHier else null,
            nbArticles = lignes.size,
            nbCategories = parType.count { it.nombre > 0 } + catsLibres.count { it.actif },
            critiques = lignes.count { it.level == StockLevel.CRITIQUE || it.level == StockLevel.BAS },
            ruptures = lignes.count { it.stock <= 0.0 },
            categories = parType,
            categoriesLibres = catsLibres.filter { it.actif }.map { c ->
                CatLibreRow(
                    id = c.id,
                    nom = c.nom,
                    nombre = lignes.count { it.product.categorieId == c.id },
                )
            },
            entreesJour = entrees,
            sortiesJour = sorties,
            transfertsJour = transferts,
            entrees30j = entrees30j,
            sorties30j = sorties30j,
            topCategories = parType.filter { it.nombre > 0 }.sortedByDescending { it.valeur }.take(3),
            activite = activite,
            inventaireEnCours = sessionInventaire != null,
            valeurImmo = parType.filter { TYPES_EQUIPEMENTS.contains(it.type) }.sumOf { it.valeur },
            nbImmo = parType.filter { TYPES_EQUIPEMENTS.contains(it.type) }.sumOf { it.nombre },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockAccueilState())
}

data class StockListeState(
    val devise: String = "",
    val type: ProductType? = null,
    val requete: String = "",
    val categorieId: Long? = null,
    val categories: List<ProductCategoryEntity> = emptyList(),
    val articles: List<ProductWithStock> = emptyList(),
    val valeur: Double = 0.0,
)

/** Liste d'articles filtrée par type d'article, catégorie utilisateur et recherche. */
@HiltViewModel
class StockListeViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    productDao: ProductDao,
    getEnterprise: GetEnterpriseUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val typeInitial: ProductType? =
        savedStateHandle.get<String>("type")?.let { nom ->
            runCatching { ProductType.valueOf(nom) }.getOrNull()
        }

    /** Catégorie utilisateur demandée par la route (tuile de la matrice). */
    private val categorieInitiale: Long? =
        savedStateHandle.get<String>("cat")?.toLongOrNull()

    private val _requete = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _categorieId = kotlinx.coroutines.flow.MutableStateFlow(categorieInitiale)

    fun chercher(texte: String) { _requete.value = texte }
    fun filtrerCategorie(id: Long?) { _categorieId.value = id }

    val etat: StateFlow<StockListeState> = combine(
        combine(observeProducts(), observeStock()) { produits, stocks ->
            ProductStocks.combine(produits, stocks)
        },
        productDao.observeCategories(),
        getEnterprise.observer(),
        _requete,
        _categorieId,
    ) { lignes, categories, entreprise, requete, categorieId ->
        val filtrees = lignes
            .filter { typeInitial == null || it.product.type == typeInitial }
            .filter { categorieId == null || it.product.categorieId == categorieId }
            .filter { requete.isBlank() || it.nom.contains(requete, ignoreCase = true) || (it.product.reference ?: "").contains(requete, ignoreCase = true) || (it.barcode ?: "").contains(requete, ignoreCase = true) }
            .sortedBy { it.nom }
        StockListeState(
            devise = entreprise?.devise.orEmpty(),
            type = typeInitial,
            requete = requete,
            categorieId = categorieId,
            categories = categories.filter { it.actif },
            articles = filtrees,
            valeur = filtrees.sumOf { l -> l.total * (l.product.prixRevient ?: l.product.prixAchat ?: 0.0) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockListeState(type = typeInitial))
}

data class StockDetailState(
    val devise: String = "",
    val product: ProductEntity? = null,
    val stocks: List<ProductStockEntity> = emptyList(),
    val sites: List<SiteEntity> = emptyList(),
    val categorie: String? = null,
    val fournisseur: FournisseurEntity? = null,
    val mouvements: List<StockMovementView> = emptyList(),
)

/** Détail d'un article : infos, stock par site, prix, fournisseur, historique. */
@HiltViewModel
class StockDetailViewModel @Inject constructor(
    productDao: ProductDao,
    observeStock: ObserveProductStockUseCase,
    sites: SiteUseCases,
    observeMovements: ObserveStockMovementsUseCase,
    fournisseurDao: FournisseurDao,
    getEnterprise: GetEnterpriseUseCase,
    equipementDao: com.missa.b360.core.data.dao.ProductEquipementDao,
    extrasDao: com.missa.b360.core.data.dao.ProductExtrasDao,
    savedStateHandle: SavedStateHandle,
    private val supprimerProduit: SupprimerProduitUseCase,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _suppressionResult = kotlinx.coroutines.flow.MutableStateFlow<SupprimerProduitUseCase.Result?>(null)
    val suppressionResult: StateFlow<SupprimerProduitUseCase.Result?> = _suppressionResult

    /** Demande la suppression (désactivation) de l'article affiché. */
    fun supprimer() {
        viewModelScope.launch { _suppressionResult.value = supprimerProduit(id) }
    }

    fun clearSuppressionResult() {
        _suppressionResult.value = null
    }

    val etat: StateFlow<StockDetailState> = combine(
        combine(
            productDao.observeById(id),
            observeStock(),
            sites.observerSites(),
        ) { product, stocks, listeSites -> Triple(product, stocks, listeSites) },
        combine(
            observeMovements(),
            fournisseurDao.observeAll(),
            getEnterprise.observer(),
        ) { mouvements, fournisseurs, entreprise -> Triple(mouvements, fournisseurs, entreprise) },
    ) { a, b ->
        val product = a.first
        val stocks = a.second
        val sites = a.third
        val mouvements = b.first
        val fournisseurs = b.second
        val entreprise = b.third
        StockDetailState(
            devise = entreprise?.devise.orEmpty(),
            product = product,
            stocks = stocks.filter { it.produitId == id },
            sites = sites,
            categorie = null,
            fournisseur = fournisseurs.firstOrNull { it.id == product?.fournisseurId },
            mouvements = mouvements.filter { m ->
                // StockMovementView ne porte pas produitId : filtre par code produit.
                product != null && m.produitCode == product.code
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockDetailState())

    /** Nom de la catégorie de l'article (résolu séparément). */
    val categorieNom: StateFlow<String?> = productDao.observeById(id)
        .map { p -> p?.categorieId?.let { productDao.getCategorieById(it)?.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Extension immobilisation (équipements) de l'article, si applicable. */
    val equipement: StateFlow<com.missa.b360.core.data.entity.ProductEquipementEntity?> =
        equipementDao.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dechet: StateFlow<com.missa.b360.core.data.entity.ProductDechetEntity?> =
        extrasDao.observeDechet(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val emballage: StateFlow<com.missa.b360.core.data.entity.ProductEmballageEntity?> =
        extrasDao.observeEmballage(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val consignation: StateFlow<com.missa.b360.core.data.entity.ProductConsignationEntity?> =
        extrasDao.observeConsignation(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val kit: StateFlow<com.missa.b360.core.data.entity.ProductKitEntity?> =
        extrasDao.observeKit(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val composantsKit: StateFlow<List<com.missa.b360.core.data.entity.KitComposantEntity>> =
        extrasDao.observeComposants(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val equipementDao2 = equipementDao

    /** Change le statut de service d'un équipement (maquette 6). */
    fun setStatutEquipement(statut: com.missa.b360.core.data.entity.StatutEquipement) {
        viewModelScope.launch { equipementDao2.setStatut(id, statut.name) }
    }
}

/** Groupe de mouvements d'une même journée (maquette 7). */
data class MvJour(val titreRes: Int? = null, val titreTexte: String? = null, val lignes: List<StockMovementView>)

@HiltViewModel
class StockMouvementsViewModel @Inject constructor(
    observeMovements: ObserveStockMovementsUseCase,
) : ViewModel() {

    private val _filtre = kotlinx.coroutines.flow.MutableStateFlow(0)
    val filtre: StateFlow<Int> = _filtre
    fun setFiltre(f: Int) { _filtre.value = f }

    val etat: StateFlow<List<MvJour>> = combine(observeMovements(), _filtre) { mouvements, f ->
        val filtrees = mouvements.filter { mv ->
            when (f) {
                1 -> mv.type == "ENTREE"
                2 -> mv.type == "SORTIE"
                3 -> mv.type == "TRANSFERT_SORTIE" || mv.type == "TRANSFERT_ENTREE"
                else -> true
            }
        }
        val cal = Calendar.getInstance()
        val debutJour = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        filtrees
            .groupBy { mv ->
                cal.timeInMillis = mv.horodatage
                val d = Calendar.getInstance().apply {
                    timeInMillis = mv.horodatage
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                d
            }
            .toSortedMap(compareByDescending { it })
            .map { (jour, lignes) ->
                if (jour == debutJour) MvJour(titreRes = R.string.st_aujourdhui, lignes = lignes)
                else MvJour(
                    titreTexte = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(jour)),
                    lignes = lignes,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class StockAlertesViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
) : ViewModel() {

    private val _filtre = kotlinx.coroutines.flow.MutableStateFlow(0)
    val filtre: StateFlow<Int> = _filtre
    fun setFiltre(f: Int) { _filtre.value = f }

    val etat: StateFlow<List<ProductWithStock>> = combine(
        combine(observeProducts(), observeStock()) { produits, stocks ->
            ProductStocks.combine(produits, stocks)
        },
        _filtre,
    ) { lignes, f ->
        val alertes = lignes.filter { it.stock <= 0.0 || it.level != StockLevel.OK }
        when (f) {
            1 -> alertes.filter { it.stock > 0.0 } // stock critique / faible
            2 -> alertes.filter { it.stock <= 0.0 } // ruptures
            else -> alertes
        }.sortedBy { it.stock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Équipements (immobilisations) : produits de type équipement/matériel/pièce + statut. */
@HiltViewModel
class StockEquipementsViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    equipementDao: com.missa.b360.core.data.dao.ProductEquipementDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    private val _requete = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _statut = kotlinx.coroutines.flow.MutableStateFlow(0)
    val requete: StateFlow<String> = _requete
    val filtreStatut: StateFlow<Int> = _statut
    fun chercher(t: String) { _requete.value = t }
    fun setStatut(s: Int) { _statut.value = s }

    data class LigneEquipement(
        val product: ProductEntity,
        val equipement: com.missa.b360.core.data.entity.ProductEquipementEntity?,
    )

    data class Etat(
        val lignes: List<LigneEquipement> = emptyList(),
        val enService: Int = 0,
        val maintenance: Int = 0,
        val horsService: Int = 0,
        val devise: String = "",
        val valeur: Double = 0.0,
    )

    val etat: StateFlow<Etat> = combine(
        combine(observeProducts(), equipementDao.observeAll()) { produits, eqs ->
            produits.filter { TYPES_EQUIPEMENTS.contains(it.type) }
                .map { p -> LigneEquipement(p, eqs.firstOrNull { it.produitId == p.id }) }
        },
        _requete,
        _statut,
        getEnterprise.observer(),
    ) { lignes, requete, statut, entreprise ->
        val filtrees = lignes
            .filter { requete.isBlank() || it.product.nom.contains(requete, ignoreCase = true) }
            .filter {
                when (statut) {
                    1 -> it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.EN_SERVICE
                    2 -> it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.MAINTENANCE
                    3 -> it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.HORS_SERVICE
                    else -> true
                }
            }
        Etat(
            lignes = filtrees.sortedBy { it.product.nom },
            enService = lignes.count { it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.EN_SERVICE },
            maintenance = lignes.count { it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.MAINTENANCE },
            horsService = lignes.count { it.equipement?.statut == com.missa.b360.core.data.entity.StatutEquipement.HORS_SERVICE },
            devise = entreprise?.devise.orEmpty(),
            valeur = lignes.sumOf { it.product.prixRevient ?: it.product.prixAchat ?: 0.0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Etat())
}

/** Inventaire physique (maquette 8) : session, comptage, écarts, clôture. */
@HiltViewModel
class InventaireViewModel @Inject constructor(
    private val inventaireDao: com.missa.b360.core.data.dao.InventaireDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val siteDao: SiteDao,
    private val recordStockMovement: RecordStockMovementUseCase,
) : ViewModel() {

    data class LigneInventaire(
        val produitId: Long,
        val nom: String,
        val reference: String,
        val attendu: Double,
        val compte: Double?,
    ) {
        val ecart: Double? get() = compte?.let { it - attendu }
    }

    data class Etat(
        val session: InventaireEntity? = null,
        val lignes: List<LigneInventaire> = emptyList(),
        val siteNom: String? = null,
    ) {
        val total get() = lignes.size
        val comptes get() = lignes.count { it.compte != null }
        val ecarts get() = lignes.filter { it.ecart != null && it.ecart != 0.0 }
    }

    val etat: StateFlow<Etat> = inventaireDao.observeEnCours()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(Etat())
            } else {
                combine(
                    inventaireDao.observeLignes(session.id),
                    productDao.observeAll(),
                    siteDao.observeAll(),
                ) { lignesRaw, produits, sites ->
                    Etat(
                        session = session,
                        siteNom = sites.firstOrNull { it.id == session.siteId }?.nom,
                        lignes = lignesRaw.mapNotNull { l ->
                            produits.firstOrNull { it.id == l.produitId }?.let { p ->
                                LigneInventaire(
                                    produitId = p.id,
                                    nom = p.nom,
                                    reference = p.reference?.takeIf { it.isNotBlank() } ?: p.code,
                                    attendu = l.attendu,
                                    compte = l.compte,
                                )
                            }
                        }.sortedBy { it.nom },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Etat())

    /** Ouvre une session sur le site principal : attendu = stock actuel de chaque produit actif. */
    fun demarrer() {
        viewModelScope.launch {
            if (inventaireDao.observeEnCours().first() != null) return@launch
            val siteId = siteDao.idPrincipal()
                ?: siteDao.observeAll().first().firstOrNull()?.id
                ?: return@launch
            val id = inventaireDao.insert(
                InventaireEntity(siteId = siteId, debut = System.currentTimeMillis()),
            )
            productDao.observeAll().first()
                .filter { it.active }
                .forEach { p ->
                    val stock = stockDao.quantite(p.id, siteId) ?: 0.0
                    inventaireDao.upsertLigne(
                        InventaireLigneEntity(inventaireId = id, produitId = p.id, attendu = stock),
                    )
                }
        }
    }

    fun enregistrerCompte(produitId: Long, texte: String, attendu: Double) {
        val session = etat.value.session ?: return
        viewModelScope.launch {
            inventaireDao.upsertLigne(
                InventaireLigneEntity(
                    inventaireId = session.id,
                    produitId = produitId,
                    attendu = attendu,
                    compte = texte.toDoubleOrNull(),
                ),
            )
        }
    }

    /** Clôture : applique chaque écart signé en AJUSTEMENT, puis ferme la session. */
    fun cloturer(onDone: () -> Unit) {
        val session = etat.value.session ?: return
        viewModelScope.launch {
            val lignes = inventaireDao.observeLignes(session.id).first()
            lignes.forEach { l ->
                val compte = l.compte ?: return@forEach
                val ecart = compte - l.attendu
                if (abs(ecart) >= 0.0001) {
                    recordStockMovement(
                        produitId = l.produitId,
                        type = StockMovementType.AJUSTEMENT,
                        quantite = ecart,
                        motif = "Inventaire #${session.id} — ajustement",
                    )
                }
            }
            inventaireDao.cloturer(session.id, System.currentTimeMillis())
            onDone()
        }
    }
}
