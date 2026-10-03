package com.missa.b360.ui.fournisseurs.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DocumentARenouveler(val document: FournisseurDocumentEntity, val fournisseurNom: String)

data class FournisseurDocumentsUiState(
    val chargement: Boolean = true,
    val documents: List<DocumentARenouveler> = emptyList(),
    val now: Long = 0L,
)

/** Documents de conformité expirés ou proches de l'échéance, tous fournisseurs confondus. */
@HiltViewModel
class FournisseurDocumentsViewModel @Inject constructor(
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    documentDao: FournisseurDocumentDao,
) : ViewModel() {

    val etat: StateFlow<FournisseurDocumentsUiState> = combine(
        observePortefeuille(),
        documentDao.observeTousActifs(),
    ) { portefeuille, documents ->
        val noms = portefeuille.lignes
            .filter { it.fournisseur.statut != FournisseurStatus.ARCHIVE }
            .associate { it.fournisseur.id to it.fournisseur.nom }
        FournisseurDocumentsUiState(
            chargement = false,
            documents = FournisseurConformiteRules.aRenouveler(documents, portefeuille.now)
                .mapNotNull { d -> noms[d.fournisseurId]?.let { DocumentARenouveler(d, it) } },
            now = portefeuille.now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurDocumentsUiState())
}
