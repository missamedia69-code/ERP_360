package com.missa.b360.ui.fournisseurs.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.model.FournisseurFiltre
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.FournisseurPortefeuilleRules
import com.missa.b360.core.domain.model.FournisseurTri
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FournisseurListUiState(
    val chargement: Boolean = true,
    val lignes: List<FournisseurLigne> = emptyList(),
    val aucunFournisseur: Boolean = false,
    val requete: String = "",
    val filtre: FournisseurFiltre = FournisseurFiltre.TOUS,
    val tri: FournisseurTri = FournisseurTri.NOM,
    val devise: String = Iso4217.DEVISE_REPLI,
    val compteurs: Map<FournisseurFiltre, Int> = emptyMap(),
    val detteTotale: Double = 0.0,
    val enRetard: Double = 0.0,
    val aRegulariser: Int = 0,
)

/** Liste des fournisseurs : lit les soldes et scores en cache, jamais les pièces d'achat une à une. */
@HiltViewModel
class FournisseurListViewModel @Inject constructor(
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    private val requete = MutableStateFlow("")
    private val filtre = MutableStateFlow(FournisseurFiltre.TOUS)
    private val tri = MutableStateFlow(FournisseurTri.NOM)

    fun changerRequete(valeur: String) {
        requete.value = valeur
    }

    fun changerFiltre(valeur: FournisseurFiltre) {
        filtre.value = valeur
    }

    fun changerTri(valeur: FournisseurTri) {
        tri.value = valeur
    }

    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurListUiState> = combine(
        observePortefeuille(),
        requete,
        filtre,
        tri,
        devise,
    ) { portefeuille, q, f, t, d ->
        val toutes = portefeuille.lignes
        val actives = toutes.filter { FournisseurPortefeuilleRules.correspondFiltre(it, FournisseurFiltre.TOUS) }
        FournisseurListUiState(
            chargement = false,
            lignes = FournisseurPortefeuilleRules.trier(FournisseurPortefeuilleRules.filtrer(toutes, f, q), t),
            aucunFournisseur = toutes.isEmpty(),
            requete = q,
            filtre = f,
            tri = t,
            devise = d,
            compteurs = FournisseurFiltre.entries.associateWith { FournisseurPortefeuilleRules.compter(toutes, it) },
            detteTotale = actives.sumOf { it.dette },
            enRetard = actives.sumOf { it.enRetard },
            aRegulariser = actives.count { it.aptitude.niveau == SupplierReadinessLevel.A_REGULARISER },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurListUiState())
}
