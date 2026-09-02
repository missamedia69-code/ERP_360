package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.domain.usecase.BackupUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sauvegarde (RA-13) : sauvegarde locale + historique. */
@HiltViewModel
class SauvegardeViewModel @Inject constructor(
    private val useCases: BackupUseCases,
) : ViewModel() {

    data class UiState(
        val enCours: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    val historique: Flow<List<BackupEntity>> = useCases.historique()

    fun sauvegarder() {
        _state.value = UiState(enCours = true)
        viewModelScope.launch {
            val fichier = useCases.sauvegarder()
            _state.value = UiState(enCours = false, message = if (fichier != null) "ok" else "err")
        }
    }
}