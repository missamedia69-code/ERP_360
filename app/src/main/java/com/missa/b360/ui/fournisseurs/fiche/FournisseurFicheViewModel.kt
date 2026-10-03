package com.missa.b360.ui.fournisseurs.fiche

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurContactDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurContactEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.domain.model.ConformiteResume
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.fournisseurs.FournisseurRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FournisseurFicheUiState(
    val chargement: Boolean = true,
    val ligne: FournisseurLigne? = null,
    val contact: FournisseurContactEntity? = null,
    val compte: FournisseurCompteBancaireEntity? = null,
    val conformite: ConformiteResume = ConformiteResume(),
    val factures: List<FournisseurFactureOuverte> = emptyList(),
    val devise: String = Iso4217.DEVISE_REPLI,
) {
    val introuvable: Boolean get() = !chargement && ligne == null
    val score: FournisseurScoreEntity? get() = ligne?.score
}

/** Fiche 360 : ce qu'il faut savoir avant de commander ou de payer, sur un seul écran. */
@HiltViewModel
class FournisseurFicheViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    contactDao: FournisseurContactDao,
    compteDao: FournisseurCompteBancaireDao,
    documentDao: FournisseurDocumentDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    val fournisseurId: Long = savedState.get<Long>(FournisseurRoutes.ARG_ID) ?: 0L

    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurFicheUiState> = combine(
        observePortefeuille(),
        contactDao.observeParFournisseur(fournisseurId),
        compteDao.observeParFournisseur(fournisseurId),
        documentDao.observeParFournisseur(fournisseurId),
        devise,
    ) { portefeuille, contacts, comptes, documents, d ->
        FournisseurFicheUiState(
            chargement = false,
            ligne = portefeuille.lignes.firstOrNull { it.fournisseur.id == fournisseurId },
            contact = contacts.firstOrNull { it.principal } ?: contacts.firstOrNull(),
            compte = comptes.firstOrNull { it.principal } ?: comptes.firstOrNull(),
            conformite = FournisseurConformiteRules.resumer(documents, portefeuille.now),
            factures = portefeuille.factures.filter { it.fournisseurId == fournisseurId },
            devise = d,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurFicheUiState())
}
