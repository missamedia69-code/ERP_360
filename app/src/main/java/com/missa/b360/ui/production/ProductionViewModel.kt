package com.missa.b360.ui.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.GroupeArticleUseCases
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaveProductionOrderUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductionUiState(
    val selectedProduct: ProductWithStock? = null,
    val quantiteAProduire: Double = 1.0,
    val composants: List<ProductionComponent> = emptyList(),
    val editingOrderId: Long? = null,
)

@HiltViewModel
class ProductionViewModel @Inject constructor(
    private val saveProductionOrder: SaveProductionOrderUseCase,
    operations: OperationUseCases,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
    groupUseCases: GroupeArticleUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    sealed interface ActionMessage {
        data class Succes(val texte: String) : ActionMessage
        data class Erreur(val texte: String) : ActionMessage
    }

    val ordres: Flow<List<OperationRecordEntity>> = operations.observe(OperationModule.PRODUCTION)

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    /** Produits fabricables (produits finis, semi-finis). */
    val fabricables: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
        groupUseCases.observer(),
    ) { prods, stocks, groups ->
        ProductStocks.combine(ReglesGroupesArticles.produisibles(prods, groups), stocks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Composants éligibles (matières premières, emballages, semi-finis). */
    val composantsDisponibles: StateFlow<List<ProductWithStock>> = combine(
        observeProducts(),
        observeStock(),
    ) { prods, stocks -> ProductStocks.combine(ProduitRules.composants(prods), stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ProductionUiState())
    val uiState: StateFlow<ProductionUiState> = _uiState

    private val _message = MutableStateFlow<ActionMessage?>(null)
    val message: StateFlow<ActionMessage?> = _message
    private val _enregistrement = MutableStateFlow(false)
    val enregistrement: StateFlow<Boolean> = _enregistrement

    fun nouveauOrdre() {
        _uiState.value = ProductionUiState()
        _message.value = null
    }

    fun ouvrirBrouillon(order: OperationRecordEntity) {
        if (order.module != OperationModule.PRODUCTION.name || order.status != com.missa.b360.core.data.entity.OperationStatus.DRAFT.name) {
            _message.value = ActionMessage.Erreur("Seuls les brouillons de production peuvent être modifiés")
            return
        }
        val payload = ProductionCodec.decode(order.notes)
        if (payload == null || !ProductionRules.payloadIsValid(payload)) {
            _message.value = ActionMessage.Erreur("Le contenu de ce brouillon est invalide")
            return
        }
        viewModelScope.launch {
            val product = withTimeoutOrNull(5_000) {
                fabricables.first { options -> options.any { it.product.id == payload.produitId } }
                    .firstOrNull { it.product.id == payload.produitId }
            }
            if (product == null) {
                _message.value = ActionMessage.Erreur("Le produit de ce brouillon n'est plus fabricable")
                return@launch
            }
            _uiState.value = ProductionUiState(
                selectedProduct = product,
                quantiteAProduire = payload.quantite,
                composants = payload.composants,
                editingOrderId = order.id,
            )
            _message.value = null
        }
    }

    fun selectFabricable(product: ProductWithStock) {
        _uiState.value = _uiState.value.copy(selectedProduct = product)
    }

    fun setQuantiteAProduire(qte: Double) {
        if (qte > 0.0) _uiState.value = _uiState.value.copy(quantiteAProduire = qte)
    }

    fun addComposant(product: ProductWithStock, quantite: Double = 1.0) {
        val current = _uiState.value.composants
        val existing = current.firstOrNull { it.productId == product.product.id }
        val updated = if (existing != null) {
            current.map {
                if (it.productId == product.product.id) it.copy(quantite = it.quantite + quantite) else it
            }
        } else {
            current + ProductionComponent(productId = product.product.id, nom = product.nom, quantite = quantite)
        }
        _uiState.value = _uiState.value.copy(composants = updated)
    }

    fun removeComposant(productId: Long) {
        _uiState.value = _uiState.value.copy(
            composants = _uiState.value.composants.filterNot { it.productId == productId },
        )
    }

    fun lancerOrdre(draft: Boolean) {
        if (_enregistrement.value) return
        val st = _uiState.value
        val prod = st.selectedProduct ?: return
        if (st.composants.isEmpty()) {
            _message.value = ActionMessage.Erreur("Ajoutez au moins un composant à la nomenclature")
            return
        }

        viewModelScope.launch {
            _enregistrement.value = true
            try {
                val payload = ProductionRecordPayload(
                    produitId = prod.product.id,
                    produitNom = prod.nom,
                    quantite = st.quantiteAProduire,
                    composants = st.composants,
                )
                when (val res = saveProductionOrder(st.editingOrderId, payload, draft)) {
                    is SaveProductionOrderUseCase.Result.Succes -> {
                        val suffix = if (draft) "Brouillon enregistré" else "Production terminée"
                        val cost = if (draft) "" else " · coût matières valorisé"
                        _message.value = ActionMessage.Succes("$suffix : ${res.reference}$cost")
                        _uiState.value = ProductionUiState()
                    }
                    is SaveProductionOrderUseCase.Result.StockInsuffisant -> {
                        _message.value = ActionMessage.Erreur("Matière insuffisante : ${res.produitNom} (disponible ${res.disponible}, requis ${res.demande})")
                    }
                    SaveProductionOrderUseCase.Result.LectureSeule -> _message.value = ActionMessage.Erreur("Licence en lecture seule")
                    SaveProductionOrderUseCase.Result.NonAutorise -> _message.value = ActionMessage.Erreur("Action non autorisée pour ce rôle")
                    SaveProductionOrderUseCase.Result.ModuleInactif -> _message.value = ActionMessage.Erreur("Production ou Stock n'est pas activé dans le profil")
                    SaveProductionOrderUseCase.Result.MouvementDejaEnregistre -> _message.value = ActionMessage.Erreur("Cet ordre a déjà été comptabilisé dans Stock")
                    SaveProductionOrderUseCase.Result.ProduitIntrouvable -> _message.value = ActionMessage.Erreur("Produit fini absent ou non fabricable")
                    SaveProductionOrderUseCase.Result.ComposantIntrouvable -> _message.value = ActionMessage.Erreur("Composant absent ou non utilisable en production")
                    SaveProductionOrderUseCase.Result.SiteIntrouvable -> _message.value = ActionMessage.Erreur("Aucun dépôt de destination valide")
                    SaveProductionOrderUseCase.Result.BrouillonIntrouvable -> _message.value = ActionMessage.Erreur("Brouillon d'ordre introuvable ou déjà lancé")
                    SaveProductionOrderUseCase.Result.DonneesInvalides -> _message.value = ActionMessage.Erreur("Vérifiez le produit, la quantité et les composants")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                _message.value = ActionMessage.Erreur("Erreur pendant l'enregistrement; aucune validation Stock ne doit être supposée")
            } finally {
                _enregistrement.value = false
            }
        }
    }

    fun effacerMessage() {
        _message.value = null
    }
}
