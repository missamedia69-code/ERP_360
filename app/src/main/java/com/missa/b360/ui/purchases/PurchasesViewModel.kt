package com.missa.b360.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.usecase.AnnulerAchatUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ReglerAchatUseCase
import com.missa.b360.core.domain.usecase.SaveCommandeAchatUseCase
import com.missa.b360.core.domain.usecase.SaveReceptionAchatUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseursUseCase
import com.missa.b360.core.domain.usecase.ObservePaymentMethodsUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.ObserveTaxesUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SavePurchaseUseCase
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.core.util.filterMoneyInput
import com.missa.b360.core.util.toInputAmount
import com.missa.b360.core.util.toMoneyOrNull
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

/** Panier d'une facture fournisseur en cours — jamais prérempli (aucune donnée fictive). */
data class PurchaseUiState(
    val supplier: FournisseurEntity? = null,
    val lines: List<PurchaseLine> = emptyList(),
    val paidInput: String = "",
    val note: String = "",
    val editingRecordId: Long? = null,
    /** Chemins internes des pièces jointes (photos compressées, PDF). */
    val attachments: List<String> = emptyList(),
    /** Taux de TVA choisi pour la facture — null = taux par défaut du référentiel. */
    val taxTaux: Double? = null,
    /** Facture rattachée à une réception : aucun nouvel effet stock. */
    val receptionRecordId: Long? = null,
    val receptionReference: String? = null,
    val commandeRecordId: Long? = null,
)

/** Panier d'un bon de commande fournisseur en cours. */
data class CommandeUiState(
    val supplier: FournisseurEntity? = null,
    val lines: List<CommandeAchatLigne> = emptyList(),
    val note: String = "",
    val editingRecordId: Long? = null,
)

/** Lignes d'un bon de réception en cours (depuis une commande ou libres). */
data class ReceptionUiState(
    val commandeRecordId: Long? = null,
    val commandeReference: String? = null,
    val supplier: FournisseurEntity? = null,
    val lignes: List<ReceptionLigne> = emptyList(),
    val note: String = "",
    val editingRecordId: Long? = null,
)

@HiltViewModel
class PurchasesViewModel @Inject constructor(
    operations: OperationUseCases,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    observeFournisseurs: ObserveFournisseursUseCase,
    observeTaxes: ObserveTaxesUseCase,
    observePaymentMethods: ObservePaymentMethodsUseCase,
    getEnterprise: GetEnterpriseUseCase,
    private val savePurchase: SavePurchaseUseCase,
    private val saveCommandeAchat: SaveCommandeAchatUseCase,
    private val saveReceptionAchat: SaveReceptionAchatUseCase,
    private val reglerAchat: ReglerAchatUseCase,
    private val annulerAchat: AnnulerAchatUseCase,
    private val fournisseurItemDao: com.missa.b360.core.data.dao.FournisseurItemDao,
) : ViewModel() {

    sealed interface SaveResult {
        data class Saved(val reference: String, val isDraft: Boolean) : SaveResult
        data object MissingSupplier : SaveResult
        data object EmptyCart : SaveResult
        data object InvalidAmount : SaveResult
        data object ReadOnly : SaveResult
        data object FournisseurIntrouvable : SaveResult
        data object Error : SaveResult
    }

    val purchases: StateFlow<List<OperationRecordEntity>> = operations.observe(OperationModule.ACHATS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Catalogue produits avec stock courant — les prix affichés sont les prix d'achat. */
    /**
     * Catalogue commandable auprès d'un fournisseur.
     *
     * Un produit fabriqué en interne en est exclu : s'il fallait
     * l'approvisionner, ce serait un article acheté-revendu.
     */
    val products: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { produits, stocks -> ProductStocks.combine(ProduitRules.achetables(produits), stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suppliers: StateFlow<List<FournisseurEntity>> = observeFournisseurs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taxRate: StateFlow<Double> = observeTaxes()
        .map { taxes -> taxes.firstOrNull { it.parDefaut }?.taux ?: taxes.firstOrNull()?.taux ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** Référentiel des taxes — le taux est choisi par facture. */
    val taxes: StateFlow<List<TaxEntity>> = observeTaxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val paymentMethods: StateFlow<List<String>> = observePaymentMethods()
        .map { methods -> methods.filter { it.actif }.map { it.nom } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult

    private var nextLineId = 1L

    /**
     * Liaisons article ↔ fournisseur du fournisseur choisi (spec Fournisseurs §6.4) :
     * référence, dernier prix validé, délai et quantité minimum affichés au panier.
     */
    private val _itemsFournisseur =
        MutableStateFlow<Map<Long, com.missa.b360.core.data.entity.FournisseurItemEntity>>(emptyMap())
    val itemsFournisseur: StateFlow<Map<Long, com.missa.b360.core.data.entity.FournisseurItemEntity>> =
        _itemsFournisseur

    fun selectSupplier(supplier: FournisseurEntity) {
        _uiState.value = _uiState.value.copy(supplier = supplier)
        viewModelScope.launch {
            _itemsFournisseur.value =
                fournisseurItemDao.listeParFournisseur(supplier.id).associateBy { it.productId }
        }
    }

    /** Reprend un brouillon fournisseur sans créer de deuxième pièce. */
    fun loadDraft(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = PurchaseRecordCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        nextLineId = (payload.lines.maxOfOrNull { it.id } ?: 0L) + 1L
        _uiState.value = PurchaseUiState(
            supplier = supplier,
            lines = payload.lines,
            paidInput = payload.paidAmount.toInputAmount(),
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    fun clearCart() {
        _uiState.value = PurchaseUiState()
    }

    fun addCatalogProduct(product: ProductWithStock) {
        updateKeepingFullPayment { current ->
            val existing = current.lines.firstOrNull { it.productId == product.product.id }
            if (existing != null) {
                current.copy(
                    lines = current.lines.map {
                        if (it.id == existing.id) it.copy(quantity = it.quantity + 1.0) else it
                    },
                )
            } else {
                current.copy(
                    lines = current.lines + PurchaseLine(
                        id = nextLineId++,
                        name = product.product.nom,
                        unitPrice = product.product.prixAchat ?: 0.0,
                        quantity = 1.0,
                        productId = product.product.id,
                    ),
                )
            }
        }
    }

    fun changeQuantity(lineId: Long, delta: Double) {
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines.mapNotNull { line ->
                    if (line.id != lineId) line
                    else line.copy(quantity = line.quantity + delta).takeIf { it.quantity > 0.0 }
                },
            )
        }
    }

    fun updateLine(lineId: Long, quantity: Double, unitPrice: Double) {
        if (!quantity.isFinite() || quantity <= 0.0 || !unitPrice.isFinite() || unitPrice < 0.0) return
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines.map {
                    if (it.id == lineId) it.copy(quantity = quantity, unitPrice = unitPrice) else it
                },
            )
        }
    }

    fun removeLine(lineId: Long) {
        updateKeepingFullPayment { current ->
            current.copy(lines = current.lines.filterNot { it.id == lineId })
        }
    }

    /** Traçabilité réception d'une ligne : lot, numéro de série, péremption. */
    fun updateLineTrace(lineId: Long, lot: String, numeroSerie: String, datePeremption: Long?) {
        updateKeepingFullPayment { current ->
            current.copy(
                lines = current.lines.map {
                    if (it.id == lineId) {
                        it.copy(lot = lot, numeroSerie = numeroSerie, datePeremption = datePeremption)
                    } else it
                },
            )
        }
    }

    fun updatePaid(value: String) {
        _uiState.value = _uiState.value.copy(paidInput = value.filterMoneyInput())
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value.take(500))
    }

    private fun updateKeepingFullPayment(transform: (PurchaseUiState) -> PurchaseUiState) {
        val current = _uiState.value
        val totalBefore = current.lines.sumOf { it.total }
        val paidBefore = current.paidInput.toMoneyOrNull()
        val paymentWasFull = current.paidInput.isBlank() ||
            (paidBefore != null && kotlin.math.abs(paidBefore - totalBefore) < 0.001)
        val updated = transform(current)
        _uiState.value = if (paymentWasFull) {
            updated.copy(paidInput = updated.lines.sumOf { it.total }.toInputAmount())
        } else {
            updated
        }
    }

    fun total(): Double = _uiState.value.lines.sumOf { it.total }.coerceAtLeast(0.0)

    /**
     * Enregistre la facture fournisseur (brouillon ou validée) — la persistance est
     * **transactionnelle** (spec §6) : pièce + entrées de stock + journal.
     * Le passif fournisseur = total − réglé.
     */
    fun save(paymentMethod: String, draft: Boolean) {
        if (_busy.value) return
        _saveResult.value = null
        val state = _uiState.value
        val supplier = state.supplier ?: run {
            _saveResult.value = SaveResult.MissingSupplier
            return
        }
        if (state.lines.isEmpty()) {
            _saveResult.value = SaveResult.EmptyCart
            return
        }
        val total = state.lines.sumOf { it.total }.coerceAtLeast(0.0)
        // Le taux est choisi par facture ; à défaut, taux par défaut du référentiel.
        val taxRate = state.taxTaux ?: this.taxRate.value
        val paidAmount = state.paidInput.toMoneyOrNull() ?: total
        if (paidAmount < 0.0 || paidAmount > total || paymentMethod.isBlank() || total <= 0.0) {
            _saveResult.value = SaveResult.InvalidAmount
            return
        }

        viewModelScope.launch {
            _busy.value = true
            try {
                val payload = PurchaseRecordPayload(
                    supplierId = supplier.id,
                    supplierName = supplier.nom,
                    lines = state.lines,
                    subtotal = total,
                    taxRate = taxRate,
                    taxAmount = if (taxRate == 0.0) 0.0 else total * taxRate / (100.0 + taxRate),
                    total = total,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    note = state.note.trim().ifBlank { null },
                    receptionRecordId = state.receptionRecordId,
                    commandeRecordId = state.commandeRecordId,
                    attachments = state.attachments,
                )
                when (val result = savePurchase(recordId = state.editingRecordId, payload = payload, draft = draft)) {
                    is SavePurchaseUseCase.Result.Succes -> {
                        _saveResult.value = SaveResult.Saved(result.reference, draft)
                        clearCart()
                    }
                    SavePurchaseUseCase.Result.LectureSeule -> _saveResult.value = SaveResult.ReadOnly
                    SavePurchaseUseCase.Result.DonneesInvalides -> _saveResult.value = SaveResult.InvalidAmount
                    SavePurchaseUseCase.Result.FournisseurIntrouvable -> _saveResult.value = SaveResult.FournisseurIntrouvable
                    SavePurchaseUseCase.Result.BrouillonIntrouvable -> _saveResult.value = SaveResult.Error
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _saveResult.value = SaveResult.Error
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    // ------------------------------------------------------------------
    // Chaîne commande → réception → facture → règlement → annulation
    // ------------------------------------------------------------------

    /** Résultat des actions chaînées — un seul message à la fois. */
    sealed interface ActionAchatResult {
        data object CommandeEnregistree : ActionAchatResult
        data object ReceptionEnregistree : ActionAchatResult
        data object ReglementEnregistre : ActionAchatResult
        data object PieceAnnulee : ActionAchatResult
        data object FournisseurManquant : ActionAchatResult
        data object PanierVide : ActionAchatResult
        data object DonneesInvalides : ActionAchatResult
        data object DepasseCommande : ActionAchatResult
        data object CommandeIntrouvable : ActionAchatResult
        data object StockInsuffisant : ActionAchatResult
        data object FactureLiee : ActionAchatResult
        data object ReceptionLiee : ActionAchatResult
        data object CompteIntrouvable : ActionAchatResult
        data object FournisseurNonActif : ActionAchatResult
        data object PaiementBloque : ActionAchatResult
        data object LectureSeule : ActionAchatResult
        data object Erreur : ActionAchatResult
    }

    private val _commandeState = MutableStateFlow(CommandeUiState())
    val commandeState: StateFlow<CommandeUiState> = _commandeState

    private val _receptionState = MutableStateFlow(ReceptionUiState())
    val receptionState: StateFlow<ReceptionUiState> = _receptionState

    private val _actionResult = MutableStateFlow<ActionAchatResult?>(null)
    val actionResult: StateFlow<ActionAchatResult?> = _actionResult
    fun clearActionResult() {
        _actionResult.value = null
    }

    // --- TVA par facture ---
    fun setTaxTaux(taux: Double?) {
        _uiState.value = _uiState.value.copy(taxTaux = taux)
    }

    // --- Pièces jointes (chemins gérés par PieceJointeAchat côté UI) ---
    fun addAttachment(path: String) {
        _uiState.value = _uiState.value.copy(attachments = _uiState.value.attachments + path)
    }

    fun removeAttachment(path: String) {
        com.missa.b360.core.util.PieceJointeAchat.supprimer(path)
        _uiState.value = _uiState.value.copy(attachments = _uiState.value.attachments - path)
    }

    // --- Bon de commande ---
    fun clearCommande() {
        _commandeState.value = CommandeUiState()
    }

    fun selectSupplierCommande(supplier: FournisseurEntity) {
        _commandeState.value = _commandeState.value.copy(supplier = supplier)
    }

    fun addCatalogProductCommande(product: ProductWithStock) {
        val current = _commandeState.value
        val existing = current.lines.firstOrNull { it.productId == product.product.id }
        _commandeState.value = if (existing != null) {
            current.copy(
                lines = current.lines.map {
                    if (it.id == existing.id) it.copy(quantity = it.quantity + 1.0) else it
                },
            )
        } else {
            current.copy(
                lines = current.lines + CommandeAchatLigne(
                    id = (current.lines.maxOfOrNull { it.id } ?: 0L) + 1L,
                    name = product.product.nom,
                    quantity = 1.0,
                    unitPrice = product.product.prixAchat ?: 0.0,
                    productId = product.product.id,
                ),
            )
        }
    }

    fun changeQuantityCommande(lineId: Long, delta: Double) {
        _commandeState.value = _commandeState.value.copy(
            lines = _commandeState.value.lines.mapNotNull { ligne ->
                if (ligne.id != lineId) ligne
                else ligne.copy(quantity = ligne.quantity + delta).takeIf { it.quantity > 0.0 }
            },
        )
    }

    fun updateLineCommande(lineId: Long, quantity: Double, unitPrice: Double) {
        if (!quantity.isFinite() || quantity <= 0.0 || !unitPrice.isFinite() || unitPrice < 0.0) return
        _commandeState.value = _commandeState.value.copy(
            lines = _commandeState.value.lines.map {
                if (it.id == lineId) it.copy(quantity = quantity, unitPrice = unitPrice) else it
            },
        )
    }

    fun removeLineCommande(lineId: Long) {
        _commandeState.value = _commandeState.value.copy(
            lines = _commandeState.value.lines.filterNot { it.id == lineId },
        )
    }

    fun updateNoteCommande(value: String) {
        _commandeState.value = _commandeState.value.copy(note = value.take(500))
    }

    fun loadCommandeDraft(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = CommandeAchatCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        _commandeState.value = CommandeUiState(
            supplier = supplier,
            lines = payload.lines,
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    fun enregistrerCommande(draft: Boolean) {
        if (_busy.value) return
        val state = _commandeState.value
        val supplier = state.supplier ?: run {
            _actionResult.value = ActionAchatResult.FournisseurManquant
            return
        }
        if (state.lines.isEmpty()) {
            _actionResult.value = ActionAchatResult.PanierVide
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val payload = CommandeAchatPayload(
                    supplierId = supplier.id,
                    supplierName = supplier.nom,
                    lines = state.lines,
                    note = state.note.trim().ifBlank { null },
                )
                when (saveCommandeAchat(recordId = state.editingRecordId, payload = payload, draft = draft)) {
                    is SaveCommandeAchatUseCase.Result.Succes -> {
                        _actionResult.value = ActionAchatResult.CommandeEnregistree
                        clearCommande()
                    }
                    SaveCommandeAchatUseCase.Result.LectureSeule -> _actionResult.value = ActionAchatResult.LectureSeule
                    SaveCommandeAchatUseCase.Result.DonneesInvalides -> _actionResult.value = ActionAchatResult.DonneesInvalides
                    SaveCommandeAchatUseCase.Result.FournisseurIntrouvable -> _actionResult.value = ActionAchatResult.FournisseurManquant
                    SaveCommandeAchatUseCase.Result.FournisseurNonActif -> _actionResult.value = ActionAchatResult.FournisseurNonActif
                    SaveCommandeAchatUseCase.Result.BrouillonIntrouvable -> _actionResult.value = ActionAchatResult.Erreur
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _actionResult.value = ActionAchatResult.Erreur
            } finally {
                _busy.value = false
            }
        }
    }

    // --- Bon de réception ---
    fun clearReception() {
        _receptionState.value = ReceptionUiState()
    }

    /** Prépare la réception d'une commande validée : quantités reçues = commandées. */
    fun preparerReception(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = CommandeAchatCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        _receptionState.value = ReceptionUiState(
            commandeRecordId = record.id,
            commandeReference = record.reference,
            supplier = supplier,
            lignes = payload.lines
                .filter { it.quantity > 0.0 }
                .mapNotNull { ligne ->
                    val produitId = ligne.productId ?: return@mapNotNull null
                    ReceptionLigne(
                        productId = produitId,
                        name = ligne.name,
                        quantiteCommandee = ligne.quantity,
                        quantiteRecue = ligne.quantity,
                    )
                },
        )
        return true
    }

    fun loadReceptionDraft(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = ReceptionCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        _receptionState.value = ReceptionUiState(
            commandeRecordId = payload.commandeRecordId,
            commandeReference = payload.commandeReference,
            supplier = supplier,
            lignes = payload.lignes,
            note = payload.note.orEmpty(),
            editingRecordId = record.id,
        )
        return true
    }

    fun updateLigneReception(
        productId: Long,
        quantiteRecue: Double,
        lot: String,
        numeroSerie: String,
        datePeremption: Long?,
    ) {
        if (!quantiteRecue.isFinite() || quantiteRecue < 0.0) return
        _receptionState.value = _receptionState.value.copy(
            lignes = _receptionState.value.lignes.map {
                if (it.productId == productId) {
                    it.copy(
                        quantiteRecue = quantiteRecue,
                        lot = lot,
                        numeroSerie = numeroSerie,
                        datePeremption = datePeremption,
                    )
                } else it
            },
        )
    }

    fun updateNoteReception(value: String) {
        _receptionState.value = _receptionState.value.copy(note = value.take(500))
    }

    fun enregistrerReception(draft: Boolean) {
        if (_busy.value) return
        val state = _receptionState.value
        val supplier = state.supplier ?: run {
            _actionResult.value = ActionAchatResult.FournisseurManquant
            return
        }
        if (state.lignes.none { it.quantiteRecue > 0.0 }) {
            _actionResult.value = ActionAchatResult.PanierVide
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val payload = ReceptionPayload(
                    commandeRecordId = state.commandeRecordId,
                    commandeReference = state.commandeReference,
                    supplierId = supplier.id,
                    supplierName = supplier.nom,
                    lignes = state.lignes,
                    note = state.note.trim().ifBlank { null },
                )
                when (saveReceptionAchat(recordId = state.editingRecordId, payload = payload, draft = draft)) {
                    is SaveReceptionAchatUseCase.Result.Succes -> {
                        _actionResult.value = ActionAchatResult.ReceptionEnregistree
                        clearReception()
                    }
                    SaveReceptionAchatUseCase.Result.LectureSeule -> _actionResult.value = ActionAchatResult.LectureSeule
                    SaveReceptionAchatUseCase.Result.DonneesInvalides -> _actionResult.value = ActionAchatResult.DonneesInvalides
                    SaveReceptionAchatUseCase.Result.FournisseurIntrouvable -> _actionResult.value = ActionAchatResult.FournisseurManquant
                    SaveReceptionAchatUseCase.Result.CommandeIntrouvable -> _actionResult.value = ActionAchatResult.CommandeIntrouvable
                    SaveReceptionAchatUseCase.Result.DepasseCommande -> _actionResult.value = ActionAchatResult.DepasseCommande
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _actionResult.value = ActionAchatResult.Erreur
            } finally {
                _busy.value = false
            }
        }
    }

    // --- Pré-remplissage de facture depuis réception / commande ---

    /** Facture sur réception validée : quantités reçues, prix du catalogue. */
    fun chargerFactureDepuisReception(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = ReceptionCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        val prixParProduit = products.value.associate { it.product.id to (it.product.prixAchat ?: 0.0) }
        var nextId = 1L
        _uiState.value = PurchaseUiState(
            supplier = supplier,
            lines = payload.lignes.filter { it.quantiteRecue > 0.0 }.map { ligne ->
                PurchaseLine(
                    id = nextId++,
                    name = ligne.name,
                    unitPrice = prixParProduit[ligne.productId] ?: 0.0,
                    quantity = ligne.quantiteRecue,
                    productId = ligne.productId,
                )
            },
            receptionRecordId = record.id,
            receptionReference = record.reference,
            commandeRecordId = payload.commandeRecordId,
        )
        return true
    }

    /** Facture directe sur commande validée (services, achats sans réception physique). */
    fun chargerFactureDepuisCommande(record: OperationRecordEntity, availableSuppliers: List<FournisseurEntity>): Boolean {
        val payload = CommandeAchatCodec.decode(record.notes) ?: return false
        val supplier = availableSuppliers.firstOrNull { it.id == payload.supplierId } ?: return false
        var nextId = 1L
        _uiState.value = PurchaseUiState(
            supplier = supplier,
            lines = payload.lines.map { ligne ->
                PurchaseLine(
                    id = nextId++,
                    name = ligne.name,
                    unitPrice = ligne.unitPrice,
                    quantity = ligne.quantity,
                    productId = ligne.productId,
                )
            },
            commandeRecordId = record.id,
        )
        return true
    }

    // --- Règlement ultérieur et annulation ---

    fun reglerFacture(recordId: Long, montantInput: String, modePaiement: String) {
        if (_busy.value) return
        val montant = montantInput.toMoneyOrNull()
        if (montant == null || montant <= 0.0 || modePaiement.isBlank()) {
            _actionResult.value = ActionAchatResult.DonneesInvalides
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                when (reglerAchat(recordId, montant, modePaiement)) {
                    is ReglerAchatUseCase.Result.Succes -> _actionResult.value = ActionAchatResult.ReglementEnregistre
                    ReglerAchatUseCase.Result.LectureSeule -> _actionResult.value = ActionAchatResult.LectureSeule
                    ReglerAchatUseCase.Result.Introuvable -> _actionResult.value = ActionAchatResult.Erreur
                    ReglerAchatUseCase.Result.MontantInvalide -> _actionResult.value = ActionAchatResult.DonneesInvalides
                    ReglerAchatUseCase.Result.CompteIntrouvable -> _actionResult.value = ActionAchatResult.CompteIntrouvable
                    ReglerAchatUseCase.Result.DejaEnregistre -> _actionResult.value = ActionAchatResult.Erreur
                    ReglerAchatUseCase.Result.PaiementBloque -> _actionResult.value = ActionAchatResult.PaiementBloque
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _actionResult.value = ActionAchatResult.Erreur
            } finally {
                _busy.value = false
            }
        }
    }

    fun annulerPiece(recordId: Long) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                when (annulerAchat(recordId)) {
                    AnnulerAchatUseCase.Result.Succes -> _actionResult.value = ActionAchatResult.PieceAnnulee
                    AnnulerAchatUseCase.Result.LectureSeule -> _actionResult.value = ActionAchatResult.LectureSeule
                    AnnulerAchatUseCase.Result.Introuvable -> _actionResult.value = ActionAchatResult.Erreur
                    AnnulerAchatUseCase.Result.DejaAnnulee -> _actionResult.value = ActionAchatResult.Erreur
                    AnnulerAchatUseCase.Result.StockInsuffisant -> _actionResult.value = ActionAchatResult.StockInsuffisant
                    AnnulerAchatUseCase.Result.FactureLiee -> _actionResult.value = ActionAchatResult.FactureLiee
                    AnnulerAchatUseCase.Result.ReceptionLiee -> _actionResult.value = ActionAchatResult.ReceptionLiee
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _actionResult.value = ActionAchatResult.Erreur
            } finally {
                _busy.value = false
            }
        }
    }
}
