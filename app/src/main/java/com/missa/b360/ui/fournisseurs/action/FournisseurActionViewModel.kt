package com.missa.b360.ui.fournisseurs.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.model.ActionBoard
import com.missa.b360.core.domain.model.FournisseurActionRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FournisseurActionUiState(
    val chargement: Boolean = true,
    val tableau: ActionBoard = ActionBoard(),
    val devise: String = Iso4217.DEVISE_REPLI,
    val now: Long = 0L,
)

/** Tableau d'action : qui payer, quand, et quels dossiers régulariser. */
@HiltViewModel
class FournisseurActionViewModel @Inject constructor(
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurActionUiState> = combine(observePortefeuille(), devise) { portefeuille, d ->
        FournisseurActionUiState(
            chargement = false,
            tableau = FournisseurActionRules.construire(portefeuille.lignes, portefeuille.factures, portefeuille.now),
            devise = d,
            now = portefeuille.now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurActionUiState())
}
