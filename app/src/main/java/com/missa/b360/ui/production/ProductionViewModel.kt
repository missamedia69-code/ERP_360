package com.missa.b360.ui.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveProductStockUseCase
import com.missa.b360.core.domain.usecase.ObserveProductsUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.SaveProductionOrderUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class ProductionViewModel @Inject constructor(
    private val saveProductionOrder: SaveProductionOrderUseCase,
    operations: OperationUseCases,
    observeProducts: ObserveProductsUseCase,
    observeStock: ObserveProductStockUseCase,
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
    ) { prods, stocks -> ProductStocks.combine(ProduitRules.fabricables(prods), stocks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        val st = _uiState.value
        val prod = st.selectedProduct ?: return
        if (st.composants.isEmpty()) {
            _message.value = ActionMessage.Erreur("Ajoutez au moins un composant à la nomenclature")
            return
        }

        viewModelScope.launch {
            val payload = ProductionRecordPayload(
                produitId = prod.product.id,
                produitNom = prod.nom,
                quantite = st.quantiteAProduire,
                composants = st.composants,
            )
            when (val res = saveProductionOrder(null, payload, draft)) {
                is SaveProductionOrderUseCase.Result.Succes -> {
                    _message.value = ActionMessage.Succes("Ordre enregistré : ${res.reference}")
                    _uiState.value = ProductionUiState()
                }
                is SaveProductionOrderUseCase.Result.StockInsuffisant -> {
                    _message.value = ActionMessage.Erreur("Composant insuffisant : ${res.produitNom} (dispo: ${res.disponible})")
                }
                SaveProductionOrderUseCase.Result.LectureSeule -> _message.value = ActionMessage.Erreur("Licence en lecture seule")
                else -> _message.value = ActionMessage.Erreur("Erreur lors de la validation")
            }
        }
    }

    fun effacerMessage() {
        _message.value = null
    }
}
