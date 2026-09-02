package com.missa.b360.ui.admin

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.UpdateEnterpriseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Réglages (9.1 — D4/RA-19) : langue, profil A–H, palier P1–P6,
 * informations entreprise (devise verrouillée, non modifiable).
 */
@HiltViewModel
class ReglagesViewModel @Inject constructor(
    private val getEnterprise: GetEnterpriseUseCase,
    private val updateEnterprise: UpdateEnterpriseUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    data class UiState(
        val charge: Boolean = false,
        val nomEntreprise: String = "",
        val devise: String = "USD",
        val pays: String? = null,
        val secteur: String = "",
        val adresse: String = "",
        val telephone: String = "",
        val email: String = "",
        val profil: ProfilActivite? = null,
        val palier: PalierTaille? = null,
        val langue: String = "fr",
        val sauvegardeMsg: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init { charger() }

    fun charger() {
        viewModelScope.launch {
            val e = getEnterprise()
            val langue = settingsStore.get(SettingsStore.Keys.LANGUE) ?: "fr"
            // Les versions déjà installées ont pu enregistrer profil/palier uniquement
            // dans DataStore pendant l'onboarding : ce repli conserve leur affichage.
            val profilEnregistre = e?.profilActivite
                ?: settingsStore.get(SettingsStore.Keys.PROFIL_ACTIVITE)
            val palierEnregistre = e?.palierTaille
                ?: settingsStore.get(SettingsStore.Keys.PALIER_TAILLE)
            _state.value = UiState(
                charge = true,
                nomEntreprise = e?.nom ?: "",
                devise = e?.devise ?: "USD",
                pays = e?.pays,
                secteur = e?.secteur ?: "",
                adresse = e?.adresse ?: "",
                telephone = e?.telephone ?: "",
                email = e?.email ?: "",
                profil = profilEnregistre?.let { runCatching { ProfilActivite.valueOf(it) }.getOrNull() },
                palier = palierEnregistre?.let { runCatching { PalierTaille.valueOf(it) }.getOrNull() },
                langue = langue,
            )
        }
    }

    fun changerLangue(code: String) {
        _state.value = _state.value.copy(langue = code)
        viewModelScope.launch {
            // Persist first so the choice survives a process restart.
            settingsStore.set(SettingsStore.Keys.LANGUE, code)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != code) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
        }
    }

    fun changerProfil(p: ProfilActivite) {
        _state.value = _state.value.copy(profil = p)
        viewModelScope.launch { updateEnterprise(profilActivite = p.name) }
    }

    fun changerPalier(p: PalierTaille) {
        _state.value = _state.value.copy(palier = p)
        viewModelScope.launch { updateEnterprise(palierTaille = p.name) }
    }

    fun changerSecteur(v: String) { _state.value = _state.value.copy(secteur = v) }
    fun changerAdresse(v: String) { _state.value = _state.value.copy(adresse = v) }
    fun changerTelephone(v: String) { _state.value = _state.value.copy(telephone = v) }
    fun changerEmail(v: String) { _state.value = _state.value.copy(email = v) }

    fun sauvegarderInfos() {
        val s = _state.value
        viewModelScope.launch {
            val ok = updateEnterprise(
                secteur = s.secteur,
                adresse = s.adresse,
                telephone = s.telephone,
                email = s.email,
            )
            _state.value = _state.value.copy(sauvegardeMsg = if (ok) "ok" else "err")
        }
    }
}