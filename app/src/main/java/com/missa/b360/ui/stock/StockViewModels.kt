package com.missa.b360.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.usecase.CategorieProduitUseCases
import com.missa.b360.core.domain.usecase.ChangerStatutProduitUseCase
import com.missa.b360.core.domain.usecase.CreateProductUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.GetProductUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.ObserveStockMovementsUseCase
import com.missa.b360.core.domain.usecase.ProductInput
import com.missa.b360.core.domain.usecase.RecordStockMovementUseCase
import com.missa.b360.core.domain.usecase.SiteUseCases
import com.missa.b360.core.domain.usecase.StockMovementResult
import com.missa.b360.core.domain.usecase.TransferStockUseCase
import com.missa.b360.core.domain.usecase.UpdateProductUseCase
import com.missa.b360.core.data.dao.StockMovementView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.missa.b360.core.util.Iso4217

/**
 * Produit joint à son stock courant — vue unique partagée par le module Stock
 * et le catalogue de la vente (spec §41 : mêmes données référentielles partout).
 */
data class ProductWithStock(
    val product: ProductEntity,
    /** Stock disponible au site principal (ou total multi-site sans site principal). */
    val stock: Double,
    /** Stock total multi-site. */
    val total: Double,
) {
    val nom: String get() = product.nom
    val code: String get() = product.code
    val reference: String? get() = product.reference
    val barcode: String? get() = product.barcode
    val unite: String? get() = product.unite
    val prixVente: Double? get() = product.prixVente
    val stockMin: Double get() = product.stockMin
    val stockSecurite: Double get() = product.stockSecurite

    /** Niveau d'alerte pour les badges : rouge = sécurité atteinte, orange = sous le minimum. */
    val level: StockLevel
        get() = when {
            stockSecurite > 0.0 && stock <= stockSecurite -> StockLevel.CRITIQUE
            stockMin > 0.0 && stock < stockMin -> StockLevel.BAS
            else -> StockLevel.OK
        }
}

enum class StockLevel { CRITIQUE, BAS, OK }

/**
 * Fusion pure produits × lignes de stock (testable, sans base).
 * La stratégie de site est **identique** à celle des écritures (use cases) :
 * site principal s'il est défini, sinon le site qui détient le plus de stock.
 */
object ProductStocks {
    fun combine(products: List<ProductEntity>, stocks: List<ProductStockEntity>): List<ProductWithStock> =
        products.map { product ->
            val lignes = stocks.filter { it.produitId == product.id }
            val total = lignes.sumOf { it.quantite }.coerceAtLeast(0.0)
            val stockPrincipal = if (product.siteId != null) {
                lignes.firstOrNull { it.siteId == product.siteId }?.quantite ?: 0.0
            } else {
                lignes.maxOfOrNull { it.quantite } ?: 0.0
            }
            ProductWithStock(product = product, stock = stockPrincipal.coerceAtLeast(0.0), total = total)
        }
}

/** Module Stock : produits (avec stock) + historique des mouvements (spec §11). */
@HiltViewModel
class StockViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    observeMovements: ObserveStockMovementsUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movements: Flow<List<StockMovementView>> = observeMovements()

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { enterprise -> enterprise?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)
}

/**
 * Formulaire produit (spec §7) — même composant pour création et modification ;
 * le mode est déterminé par la présence d'un [editingId].
 */
@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val getProduct: GetProductUseCase,
    private val createProduct: CreateProductUseCase,
    private val updateProduct: UpdateProductUseCase,
    private val changeStatus: ChangerStatutProduitUseCase,
    private val categoriesUseCases: CategorieProduitUseCases,
    sites: SiteUseCases,
    fournisseurDao: FournisseurDao,
    observeStock: ObserveProductStockUseCase,
    private val equipementDao: com.missa.b360.core.data.dao.ProductEquipementDao,
    private val extrasDao: com.missa.b360.core.data.dao.ProductExtrasDao,
    observeProducts: ObserveProductsUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val contexte: android.content.Context,
) : ViewModel() {

    /** Articles actifs (choix des composants d'un kit). */
    val produits: StateFlow<List<ProductEntity>> = observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface SaveResult {
        data class Saved(val code: String, val isCreate: Boolean, val id: Long = 0L) : SaveResult
        data object NomObligatoire : SaveResult
        data object DonneesInvalides : SaveResult
        data object BarcodeExistant : SaveResult
        data object SiteRequis : SaveResult
        data object LectureSeule : SaveResult
        data object Introuvable : SaveResult
        data object Error : SaveResult
    }

    /** Id du produit en cours d'édition — null tant qu'aucun chargement n'est demandé. */
    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId

    private val _product = MutableStateFlow<ProductEntity?>(null)
    val product: StateFlow<ProductEntity?> = _product

    private var observing: kotlinx.coroutines.Job? = null

    val categories: StateFlow<List<com.missa.b360.core.data.entity.ProductCategoryEntity>> =
        categoriesUseCases.observer()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sites: StateFlow<List<SiteEntity>> = sites.observerSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val fournisseurs: StateFlow<List<FournisseurEntity>> = fournisseurDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lignes de stock (lecture seule) pour afficher le stock actuel en mode édition. */
    val stockRows: StateFlow<List<ProductStockEntity>> = observeStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    /** Anti double-soumission : true pendant la sauvegarde (spec §3 SAUVEGARDE). */
    val busy: StateFlow<Boolean> = _busy

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult

    fun load(productId: Long) {
        if (_editingId.value == productId) return
        _editingId.value = productId
        observing?.cancel()
        observing = viewModelScope.launch {
            getProduct(productId).collect { _product.value = it }
        }
        viewModelScope.launch { extrasDao.observeDechet(productId).collect { _dechet.value = it } }
        viewModelScope.launch { extrasDao.observeEmballage(productId).collect { _emballage.value = it } }
        viewModelScope.launch { extrasDao.observeConsignation(productId).collect { _consignation.value = it } }
        viewModelScope.launch { extrasDao.observeKit(productId).collect { _kit.value = it } }
        viewModelScope.launch { extrasDao.observeComposants(productId).collect { _composants.value = it } }
    }

    private val _dechet = kotlinx.coroutines.flow.MutableStateFlow<com.missa.b360.core.data.entity.ProductDechetEntity?>(null)
    val dechet: StateFlow<com.missa.b360.core.data.entity.ProductDechetEntity?> = _dechet
    private val _emballage = kotlinx.coroutines.flow.MutableStateFlow<com.missa.b360.core.data.entity.ProductEmballageEntity?>(null)
    val emballage: StateFlow<com.missa.b360.core.data.entity.ProductEmballageEntity?> = _emballage
    private val _consignation = kotlinx.coroutines.flow.MutableStateFlow<com.missa.b360.core.data.entity.ProductConsignationEntity?>(null)
    val consignation: StateFlow<com.missa.b360.core.data.entity.ProductConsignationEntity?> = _consignation
    private val _kit = kotlinx.coroutines.flow.MutableStateFlow<com.missa.b360.core.data.entity.ProductKitEntity?>(null)
    val kit: StateFlow<com.missa.b360.core.data.entity.ProductKitEntity?> = _kit
    private val _composants = kotlinx.coroutines.flow.MutableStateFlow<List<com.missa.b360.core.data.entity.KitComposantEntity>>(emptyList())
    val composants: StateFlow<List<com.missa.b360.core.data.entity.KitComposantEntity>> = _composants

    fun save(
        input: ProductInput,
        initialStock: Double?,
        equipement: com.missa.b360.core.data.entity.ProductEquipementEntity? = null,
        imageTemp: String? = null,
        supprimerImage: Boolean = false,
        dechet: com.missa.b360.core.data.entity.ProductDechetEntity? = null,
        emballage: com.missa.b360.core.data.entity.ProductEmballageEntity? = null,
        consignation: com.missa.b360.core.data.entity.ProductConsignationEntity? = null,
        kit: com.missa.b360.core.data.entity.ProductKitEntity? = null,
        composants: List<com.missa.b360.core.data.entity.KitComposantEntity> = emptyList(),
    ) {
        if (_busy.value) return
        val id = _editingId.value
        viewModelScope.launch {
            _busy.value = true
            val result = try {
                if (id == null) {
                    when (val r = createProduct(input, initialStock)) {
                        is CreateProductUseCase.Result.Succes -> {
                            equipement?.let { e -> equipementDao.upsert(e.copy(produitId = r.productId)) }
                            persisterExtensions(r.productId, dechet, emballage, consignation, kit, composants)
                            appliquerImage(r.productId, imageTemp, supprimerImage)
                            SaveResult.Saved(r.code, true, r.productId)
                        }
                        CreateProductUseCase.Result.NomObligatoire -> SaveResult.NomObligatoire
                        CreateProductUseCase.Result.DonneesInvalides -> SaveResult.DonneesInvalides
                        CreateProductUseCase.Result.BarcodeExistant -> SaveResult.BarcodeExistant
                        CreateProductUseCase.Result.SiteRequis -> SaveResult.SiteRequis
                        CreateProductUseCase.Result.LectureSeule -> SaveResult.LectureSeule
                    }
                } else {
                    val success = updateProduct(id, input)
                    when {
                        success -> {
                            equipement?.let { e -> equipementDao.upsert(e.copy(produitId = id)) }
                            persisterExtensions(id, dechet, emballage, consignation, kit, composants)
                            appliquerImage(id, imageTemp, supprimerImage)
                            SaveResult.Saved(_product.value?.code ?: "", false, id)
                        }
                        _product.value == null -> SaveResult.Introuvable
                        else -> SaveResult.Error
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                SaveResult.Error
            }
            _busy.value = false
            _saveResult.value = result
        }
    }

    /**
     * Écrit le JPEG compact dans le stockage interne et persiste son chemin
     * dans `photoPath` (colonne existante, lue par les écrans de détail/liste).
     */
    private suspend fun appliquerImage(produitId: Long, imageTemp: String?, supprimerImage: Boolean) {
        val chemin = if (imageTemp != null) {
            com.missa.b360.core.util.ImageProduit.promouvoirTemp(contexte, produitId, imageTemp)
        } else if (supprimerImage) {
            com.missa.b360.core.util.ImageProduit.supprimer(contexte, produitId)
            null
        } else {
            return
        }
        productDao.getById(produitId)?.let { p ->
            productDao.update(p.copy(photoPath = chemin))
        }
    }

    /** True si l'article en cours d'édition a déjà une image sur le disque. */
    fun imageExiste(produitId: Long): Boolean =
        com.missa.b360.core.util.ImageProduit.existe(contexte, produitId)

    private suspend fun persisterExtensions(
        produitId: Long,
        dechet: com.missa.b360.core.data.entity.ProductDechetEntity?,
        emballage: com.missa.b360.core.data.entity.ProductEmballageEntity?,
        consignation: com.missa.b360.core.data.entity.ProductConsignationEntity?,
        kit: com.missa.b360.core.data.entity.ProductKitEntity?,
        composants: List<com.missa.b360.core.data.entity.KitComposantEntity>,
    ) {
        dechet?.let { extrasDao.upsertDechet(it.copy(produitId = produitId)) }
        emballage?.let { extrasDao.upsertEmballage(it.copy(produitId = produitId)) }
        consignation?.let { extrasDao.upsertConsignation(it.copy(produitId = produitId)) }
        kit?.let { extrasDao.upsertKit(it.copy(produitId = produitId)) }
        if (kit != null) {
            composants.forEach { c -> extrasDao.upsertComposant(c.copy(kitId = produitId)) }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * Création rapide d'une catégorie depuis le sélecteur du formulaire.
     * @param onDone le nouvel identifiant, ou null si la saisie est invalide / lecture seule.
     */
    fun addCategoryAsync(nom: String, onDone: (Long?) -> Unit) {
        val nomNormalise = nom.trim()
        if (nomNormalise.isEmpty()) {
            onDone(null)
            return
        }
        viewModelScope.launch {
            onDone(
                runCatching { categoriesUseCases.creer(nomNormalise) }.getOrNull(),
            )
        }
    }

    /** Bascule Actif/Inactif en mode édition (jamais de suppression — C7). */
    fun setActive(actif: Boolean) {
        val id = _editingId.value ?: return
        viewModelScope.launch {
            runCatching { changeStatus(id, actif) }
        }
    }
}

/**
 * Formulaires mouvement de stock (spec §11) et transfert (spec §13) —
 * données de référence partagées (produits + sites) et écritures transactionnelles.
 */
@HiltViewModel
class StockOpsViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    private val recordMovement: RecordStockMovementUseCase,
    private val transferStock: TransferStockUseCase,
    sites: SiteUseCases,
) : ViewModel() {
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(produits, stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lignes de stock par site — permet au transfert d'afficher le stock exact du site source. */
    val stockRows: StateFlow<List<ProductStockEntity>> = observeStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sites: StateFlow<List<SiteEntity>> = sites.observerSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface MovementOutcome {
        data class Result(val result: StockMovementResult) : MovementOutcome
        data class Transfer(val result: TransferStockUseCase.Result) : MovementOutcome
    }

    private val _busy = MutableStateFlow(false)
    /** Anti double-soumission pendant la confirmation (spec §3 SAUVEGARDE). */
    val busy: StateFlow<Boolean> = _busy

    private val _outcome = MutableStateFlow<MovementOutcome?>(null)
    val outcome: StateFlow<MovementOutcome?> = _outcome

    fun record(
        produitId: Long,
        type: StockMovementType,
        quantite: Double,
        motif: String,
        reference: String,
        commentaire: String,
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val result = try {
                recordMovement(
                    produitId = produitId,
                    type = type,
                    quantite = quantite,
                    motif = motif,
                    reference = reference,
                    commentaire = commentaire,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                StockMovementResult.Invalid
            }
            _busy.value = false
            _outcome.value = MovementOutcome.Result(result)
        }
    }

    fun transfer(
        produitId: Long,
        siteSourceId: Long,
        siteDestId: Long,
        quantite: Double,
        motif: String,
        commentaire: String,
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val result = try {
                transferStock(
                    produitId = produitId,
                    siteSourceId = siteSourceId,
                    siteDestId = siteDestId,
                    quantite = quantite,
                    motif = motif,
                    commentaire = commentaire,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                TransferStockUseCase.Result.Invalid
            }
            _busy.value = false
            _outcome.value = MovementOutcome.Transfer(result)
        }
    }

    fun clearOutcome() {
        _outcome.value = null
    }
}
