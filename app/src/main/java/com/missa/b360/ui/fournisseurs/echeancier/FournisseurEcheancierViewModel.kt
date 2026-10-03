package com.missa.b360.ui.fournisseurs.echeancier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurPaiementPlanifieDao
import com.missa.b360.core.domain.model.EcheancierGroupe
import com.missa.b360.core.domain.model.FournisseurEcheancierRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.domain.usecase.PlanResultat
import com.missa.b360.core.domain.usecase.PlanifierPaiementUseCase
import com.missa.b360.core.domain.usecase.versPlan
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FournisseurEcheancierUiState(
    val chargement: Boolean = true,
    val groupes: List<EcheancierGroupe> = emptyList(),
    val detteTotale: Double = 0.0,
    val planifie: Double = 0.0,
    val devise: String = Iso4217.DEVISE_REPLI,
    val message: String? = null,
)

internal fun PlanResultat.versCode(succes: String): String = when (this) {
    PlanResultat.OK -> succes
    PlanResultat.MONTANT_INVALIDE -> "err_plan_montant"
    PlanResultat.DATE_INVALIDE -> "err_plan_date"
    PlanResultat.LECTURE_SEULE -> "err_plan_lecture_seule"
    PlanResultat.FACTURE_INTROUVABLE, PlanResultat.PLAN_INTROUVABLE, PlanResultat.DEJA_TRAITE -> "err_plan_introuvable"
}

/** Échéancier : toutes les factures à payer par urgence, avec les paiements déjà planifiés. */
@HiltViewModel
class FournisseurEcheancierViewModel @Inject constructor(
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    planDao: FournisseurPaiementPlanifieDao,
    getEnterprise: GetEnterpriseUseCase,
    private val planifierPaiement: PlanifierPaiementUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurEcheancierUiState> = combine(
        observePortefeuille(),
        planDao.observePlanifies(),
        devise,
        message,
    ) { portefeuille, plans, d, m ->
        val noms = portefeuille.lignes.associate { it.fournisseur.id to it.fournisseur.nom }
        val plansDomaine = plans.map { it.versPlan() }
        FournisseurEcheancierUiState(
            chargement = false,
            groupes = FournisseurEcheancierRules.construire(portefeuille.factures, noms, plansDomaine, portefeuille.now),
            detteTotale = portefeuille.factures.sumOf { it.outstanding },
            planifie = plansDomaine.sumOf { it.montant },
            devise = d,
            message = m,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurEcheancierUiState())

    fun effacerMessage() {
        message.value = null
    }

    fun planifier(factureRecordId: Long, montant: Double, date: Long) {
        viewModelScope.launch {
            message.value = planifierPaiement.planifier(factureRecordId, montant, date).versCode("msg_plan_ok")
        }
    }

    fun reporter(planId: Long, date: Long) {
        viewModelScope.launch {
            message.value = planifierPaiement.reporter(planId, date).versCode("msg_plan_ok")
        }
    }

    fun annuler(planId: Long) {
        viewModelScope.launch {
            message.value = planifierPaiement.annuler(planId).versCode("msg_plan_annule")
        }
    }
}
