package com.missa.b360.ui.fournisseurs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.domain.usecase.CreateFournisseurUseCase
import com.missa.b360.core.domain.usecase.DesactiverFournisseurUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseursUseCase
import com.missa.b360.core.domain.usecase.UpdateFournisseurUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel Fournisseurs (9.3) : liste + formulaire (RF-01 doublons). */
@HiltViewModel
class FournisseursViewModel @Inject constructor(
    private val observeFournisseurs: ObserveFournisseursUseCase,
    private val createFournisseur: CreateFournisseurUseCase,
    private val updateFournisseur: UpdateFournisseurUseCase,
    private val desactiverFournisseur: DesactiverFournisseurUseCase,
) : ViewModel() {

    val fournisseurs: Flow<List<FournisseurEntity>> = observeFournisseurs()

    data class Resultat(val code: String? = null, val erreur: String? = null)
    private val _resultat = MutableStateFlow<Resultat?>(null)
    val resultat: StateFlow<Resultat?> = _resultat

    fun creer(nom: String, telephone: String, doublonConfirme: Boolean) {
        viewModelScope.launch {
            when (val r = createFournisseur(nom, telephone, doublonConfirme = doublonConfirme)) {
                is CreateFournisseurUseCase.Result.Succes -> _resultat.value = Resultat(code = r.code)
                is CreateFournisseurUseCase.Result.DoublonPotentiel -> _resultat.value = Resultat(erreur = "doublon")
                is CreateFournisseurUseCase.Result.LicenceExpiree -> _resultat.value = Resultat(erreur = "licence")
                is CreateFournisseurUseCase.Result.TelephoneObligatoire -> _resultat.value = Resultat(erreur = "telephone")
            }
        }
    }

    fun modifier(id: Long, nom: String, telephone: String) {
        viewModelScope.launch {
            val ok = updateFournisseur(id, nom, telephone)
            _resultat.value = if (ok) Resultat(code = "edit") else Resultat(erreur = "err")
        }
    }

    fun desactiver(id: Long) {
        viewModelScope.launch { desactiverFournisseur(id) }
    }
}