package com.missa.b360.ui.fournisseurs.conformite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.domain.model.ConformiteResume
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.usecase.AjouterDocumentFournisseurUseCase
import com.missa.b360.core.domain.usecase.ArchiverDocumentFournisseurUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.domain.usecase.VerifierDocumentFournisseurUseCase
import com.missa.b360.ui.fournisseurs.FournisseurRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FournisseurConformiteUiState(
    val chargement: Boolean = true,
    val ligne: FournisseurLigne? = null,
    val documents: List<FournisseurDocumentEntity> = emptyList(),
    val resume: ConformiteResume = ConformiteResume(),
    val now: Long = 0L,
    val message: String? = null,
)

/** Conformité : documents du fournisseur, échéances, vérification et motifs d'inaptitude. */
@HiltViewModel
class FournisseurConformiteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    documentDao: FournisseurDocumentDao,
    private val ajouterDocument: AjouterDocumentFournisseurUseCase,
    private val archiverDocument: ArchiverDocumentFournisseurUseCase,
    private val verifierDocument: VerifierDocumentFournisseurUseCase,
) : ViewModel() {

    val fournisseurId: Long = savedState.get<Long>(FournisseurRoutes.ARG_ID) ?: 0L

    private val message = MutableStateFlow<String?>(null)

    val etat: StateFlow<FournisseurConformiteUiState> = combine(
        observePortefeuille(),
        documentDao.observeParFournisseur(fournisseurId),
        message,
    ) { portefeuille, documents, m ->
        FournisseurConformiteUiState(
            chargement = false,
            ligne = portefeuille.lignes.firstOrNull { it.fournisseur.id == fournisseurId },
            documents = FournisseurConformiteRules.trier(documents),
            resume = FournisseurConformiteRules.resumer(documents, portefeuille.now),
            now = portefeuille.now,
            message = m,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurConformiteUiState())

    fun effacerMessage() {
        message.value = null
    }

    fun verifier(documentId: Long, approuve: Boolean) {
        viewModelScope.launch {
            message.value = when {
                !verifierDocument(documentId, fournisseurId, approuve) -> "err_document_verification"
                approuve -> "msg_document_verifie"
                else -> "msg_document_rejete"
            }
        }
    }

    fun retirer(documentId: Long) {
        viewModelScope.launch {
            message.value = if (archiverDocument(documentId)) "msg_document_retire" else "err_document_retire"
        }
    }

    fun ajouter(type: FournisseurDocType, reference: String, chemin: String?, emission: Long?, expiration: Long?) {
        viewModelScope.launch {
            message.value = if (ajouterDocument(fournisseurId, type, reference, chemin, emission, expiration) != null) {
                "msg_document_ajoute"
            } else {
                "err_document"
            }
        }
    }
}
