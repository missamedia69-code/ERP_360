package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.LicenceStatus
import com.missa.b360.core.domain.usecase.DissociateDeviceUseCase
import com.missa.b360.core.domain.usecase.GetLicenceInfoUseCase
import com.missa.b360.core.licensing.LicenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Licence (9.1 — RA-04..06) : statut, activation, désassociation (3/an). */
@HiltViewModel
class LicenceViewModel @Inject constructor(
    private val getLicence: GetLicenceInfoUseCase,
    private val licenceManager: LicenceManager,
    private val desassocier: DissociateDeviceUseCase,
) : ViewModel() {

    data class UiState(
        val charge: Boolean = false,
        val statut: LicenceStatus? = null,
        val dateDebutEssai: Long? = null,
        val dateExpiration: Long? = null,
        val code: String? = null,
        val appareilId: String? = null,
        val desassociationsUtilisees: Int = 0,
        val desassociationsMax: Int = LicenceManager.DESASSOCIATIONS_MAX_PAR_AN,
        val codeSaisi: String = "",
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            val info = getLicence()
            _state.value = UiState(
                charge = true,
                statut = info.statut,
                dateDebutEssai = info.dateDebutEssai,
                dateExpiration = info.dateExpiration,
                code = info.code,
                appareilId = info.appareilId,
                desassociationsUtilisees = info.desassociationsUtilisees,
                desassociationsMax = info.desassociationsMax,
            )
        }
    }

    fun changerCode(v: String) { _state.value = _state.value.copy(codeSaisi = v.uppercase()) }

    fun activerCode() {
        val code = _state.value.codeSaisi
        viewModelScope.launch {
            val ok = licenceManager.activer(code)
            _state.value = _state.value.copy(message = if (ok) "ok" else "err")
            charger()
        }
    }

    fun desassocierAppareil() {
        viewModelScope.launch {
            val message = when (val r = desassocier.invoke()) {
                is DissociateDeviceUseCase.Result.Success -> "ok"
                is DissociateDeviceUseCase.Result.MaxAtteint -> "max"
                is DissociateDeviceUseCase.Result.PasDeLicence -> "none"
            }
            _state.value = _state.value.copy(message = message)
            charger()
        }
    }
}