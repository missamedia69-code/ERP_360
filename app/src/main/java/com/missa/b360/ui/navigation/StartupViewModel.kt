package com.missa.b360.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de démarrage : onboarding (1re ouverture), verrou PIN (RA-01) ou accueil. */
sealed class StartupState {
    data object Chargement : StartupState()
    data object Onboarding : StartupState()
    data object VerrouPin : StartupState()
    data object Pret : StartupState()
}

/**
 * Décide de l'écran de démarrage (Phase B) :
 * - onboarding non terminé → parcours d'onboarding ;
 * - PIN configuré → **verrou PIN à chaque ouverture** (RA-01) ;
 * - sinon → accueil.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val pinManager: PinManager,
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Chargement)
    val state: StateFlow<StartupState> = _state

    init {
        evaluer()
    }

    fun evaluer() {
        viewModelScope.launch {
            val termine = settingsStore.get(SettingsStore.Keys.ONBOARDING_TERMINE) == "true"
            val pinConfigure = pinManager.isConfigured()
            _state.value = when {
                !termine -> StartupState.Onboarding
                pinConfigure -> StartupState.VerrouPin
                else -> StartupState.Pret
            }
        }
    }

    /** Appelé après saisie réussie du PIN. */
    fun deverrouiller() {
        _state.value = StartupState.Pret
    }
}
