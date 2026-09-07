package com.missa.b360.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.backup.BackupManager
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
    private val backupManager: BackupManager,
) : ViewModel() {

    /**
     * Vrai dès que l'introduction vidéo a été jouée une fois.
     *
     * L'état vit ici, et non dans l'activité : changer de langue recrée
     * l'activité, ce qui relançait la vidéo — d'où le bref écran noir le temps
     * que la surface se prépare. Un ViewModel, lui, survit à cette recréation.
     */
    var introVue: Boolean = false
        private set

    fun marquerIntroVue() {
        introVue = true
    }

    private val _state = MutableStateFlow<StartupState>(StartupState.Chargement)
    val state: StateFlow<StartupState> = _state

    /** La sauvegarde automatique ne se déclenche qu'une fois par ouverture. */
    private var sauvegardeAutoLancee = false

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
                else -> {
                    entrerApplication()
                    StartupState.Pret
                }
            }
        }
    }

    /** Appelé après saisie réussie du PIN. */
    fun deverrouiller() {
        entrerApplication()
        _state.value = StartupState.Pret
    }

    /**
     * Entrée dans l'application : sauvegarde automatique (RA-13) lorsque
     * l'onboarding a activé les sauvegardes — une fois par ouverture, via le
     * même BackupManager que l'écran d'administration (harmonie RA-13).
     */
    private fun entrerApplication() {
        if (sauvegardeAutoLancee) return
        sauvegardeAutoLancee = true
        viewModelScope.launch {
            if (settingsStore.get(SettingsStore.Keys.FREQUENCE_SAUVGARDE) == "auto") {
                backupManager.sauvegarderLocalement("AUTO")
            }
        }
    }
}
