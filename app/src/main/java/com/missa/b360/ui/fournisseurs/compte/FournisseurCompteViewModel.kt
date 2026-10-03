package com.missa.b360.ui.fournisseurs.compte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.usecase.AjouterCompteBancaireUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.domain.usecase.VerifierCompteBancaireUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.fournisseurs.FournisseurRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FournisseurCompteUiState(
    val chargement: Boolean = true,
    val ligne: FournisseurLigne? = null,
    val comptes: List<FournisseurCompteBancaireEntity> = emptyList(),
    val factures: List<FournisseurFactureOuverte> = emptyList(),
    val devise: String = Iso4217.DEVISE_REPLI,
    val message: String? = null,
)

/** Compte fournisseur : ce qu'on lui doit, les factures ouvertes et les comptes où le payer. */
@HiltViewModel
class FournisseurCompteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    compteDao: FournisseurCompteBancaireDao,
    getEnterprise: GetEnterpriseUseCase,
    private val ajouterCompte: AjouterCompteBancaireUseCase,
    private val verifierCompte: VerifierCompteBancaireUseCase,
) : ViewModel() {

    val fournisseurId: Long = savedState.get<Long>(FournisseurRoutes.ARG_ID) ?: 0L

    private val message = MutableStateFlow<String?>(null)
    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurCompteUiState> = combine(
        observePortefeuille(),
        compteDao.observeParFournisseur(fournisseurId),
        devise,
        message,
    ) { portefeuille, comptes, d, m ->
        FournisseurCompteUiState(
            chargement = false,
            ligne = portefeuille.lignes.firstOrNull { it.fournisseur.id == fournisseurId },
            comptes = comptes,
            factures = portefeuille.factures.filter { it.fournisseurId == fournisseurId },
            devise = d,
            message = m,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurCompteUiState())

    fun effacerMessage() {
        message.value = null
    }

    fun verifier(compteId: Long, approuve: Boolean) {
        viewModelScope.launch {
            message.value = when {
                !verifierCompte(compteId, fournisseurId, approuve) -> "err_compte_verification"
                approuve -> "msg_compte_verifie"
                else -> "msg_compte_rejete"
            }
        }
    }

    fun ajouter(
        titulaire: String,
        banque: String,
        numeroCompte: String,
        iban: String,
        bicSwift: String,
        operateurMobile: String,
        numeroMobile: String,
        principal: Boolean,
    ) {
        viewModelScope.launch {
            val compte = FournisseurCompteBancaireEntity(
                fournisseurId = fournisseurId,
                titulaire = titulaire.trim(),
                banque = banque.trim().ifBlank { null },
                numeroCompte = numeroCompte.trim().ifBlank { null },
                iban = iban.trim().ifBlank { null },
                bicSwift = bicSwift.trim().ifBlank { null },
                operateurMobile = operateurMobile.trim().ifBlank { null },
                numeroMobile = numeroMobile.trim().ifBlank { null },
                principal = principal,
            )
            message.value = if (ajouterCompte(compte) != null) "msg_compte_ajoute" else "err_compte_incomplet"
        }
    }
}
