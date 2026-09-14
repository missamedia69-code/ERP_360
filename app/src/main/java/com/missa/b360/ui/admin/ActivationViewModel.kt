package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.UpdateEnterpriseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel central pour la gestion de l'activation des profils.
 * Chaque profil actif active réellement les modules et éléments qui lui ont été
 * affectés par défaut OU par l'utilisateur, dans toute l'application.
 */
@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val activationRepository: ProfilActivationRepository,
    private val getEnterprise: GetEnterpriseUseCase,
    private val updateEnterprise: UpdateEnterpriseUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val activation: StateFlow<ActivationProfil> =
        activationRepository.observeActivation()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivationProfil.VIDE)

    data class EntrepriseUi(
        val nom: String = "",
        val devise: String = "XAF",
        val pays: String? = null,
        val secteur: String = "",
        val adresse: String = "",
        val telephone: String = "",
        val email: String = "",
        val numeroFiscal: String = "",
        val registreCommerce: String = "",
        val charge: Boolean = false,
    )

    private val _entreprise = MutableStateFlow(EntrepriseUi())
    val entreprise: StateFlow<EntrepriseUi> = _entreprise

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        chargerEntreprise()
    }

    private fun chargerEntreprise() {
        viewModelScope.launch {
            val e = getEnterprise()
            _entreprise.value = EntrepriseUi(
                nom = e?.nom ?: "",
                devise = e?.devise ?: "XAF",
                pays = e?.pays,
                secteur = e?.secteur ?: "",
                adresse = e?.adresse ?: "",
                telephone = e?.telephone ?: "",
                email = e?.email ?: "",
                numeroFiscal = e?.numeroFiscal ?: "",
                registreCommerce = e?.registreCommerce ?: "",
                charge = true,
            )
        }
    }

    fun changerProfil(profil: ProfilActivite) {
        viewModelScope.launch {
            activationRepository.mettreAJourProfil(
                profil = profil,
                palier = activation.value.palier,
                venteSansStock = activation.value.venteSansStock,
                modulesPersonnalises = activation.value.modulesPersonnalises,
                extrasSupport = activation.value.extrasSupport,
                elementsPersonnalises = activation.value.elementsPersonnalises,
            )
            _message.value = "ok"
        }
    }

    fun changerPalier(palier: PalierTaille) {
        viewModelScope.launch {
            val act = activation.value
            if (act.profil == null) return@launch
            activationRepository.mettreAJourProfil(
                profil = act.profil,
                palier = palier,
                venteSansStock = act.venteSansStock,
                modulesPersonnalises = act.modulesPersonnalises,
                extrasSupport = act.extrasSupport,
                elementsPersonnalises = act.elementsPersonnalises,
            )
            _message.value = "ok"
        }
    }

    fun basculerVenteSansStock() {
        viewModelScope.launch {
            activationRepository.basculerVenteSansStock()
            _message.value = "ok"
        }
    }

    fun basculerModuleMetier(module: ModuleCode) {
        viewModelScope.launch {
            val act = activation.value
            if (act.profil == null) return@launch
            if (act.isModuleActif(module)) {
                // Si module verrouillé par le pack, ne rien faire
                if (module in ModulesSocle.metierDuPack(act.profil)) return@launch
                activationRepository.retirerModuleMetier(module)
            } else {
                activationRepository.ajouterModuleMetier(module)
            }
            _message.value = "ok"
        }
    }

    fun basculerModuleSupport(module: ModuleCode) {
        viewModelScope.launch {
            val act = activation.value
            if (act.profil == null) return@launch
            if (act.isModuleActif(module)) {
                val recommandes = ModulesSocle.recommandes(act.profil, act.palier, act.modulesMetierEffectifs)
                if (module in recommandes) return@launch
                activationRepository.retirerModuleSupport(module)
            } else {
                activationRepository.ajouterModuleSupport(module)
            }
            _message.value = "ok"
        }
    }

    fun basculerElement(module: ModuleCode, element: String) {
        viewModelScope.launch {
            val act = activation.value
            if (act.profil == null) return@launch
            val actuels = act.elementsPersonnalises[module].orEmpty().toMutableSet()
            if (element in actuels) actuels.remove(element) else actuels.add(element)
            // Si on retire tout et que le module est du pack, on garde au moins les défauts ?
            // On persiste tel quel, le calcul fera union avec défauts
            activationRepository.mettreAJourElements(module, actuels)
            _message.value = "ok"
        }
    }

    fun reinitialiser() {
        viewModelScope.launch {
            val act = activation.value
            if (act.profil == null) return@launch
            activationRepository.mettreAJourProfil(
                profil = act.profil,
                palier = act.palier,
                venteSansStock = act.venteSansStock,
                modulesPersonnalises = emptySet(),
                extrasSupport = emptySet(),
                elementsPersonnalises = emptyMap(),
            )
            _message.value = "ok"
        }
    }

    // --- Infos entreprise (ancien ReglagesViewModel) ---
    fun changerSecteur(v: String) { _entreprise.value = _entreprise.value.copy(secteur = v) }
    fun changerAdresse(v: String) { _entreprise.value = _entreprise.value.copy(adresse = v) }
    fun changerTelephone(v: String) { _entreprise.value = _entreprise.value.copy(telephone = v) }
    fun changerEmail(v: String) { _entreprise.value = _entreprise.value.copy(email = v) }
    fun changerNumeroFiscal(v: String) { _entreprise.value = _entreprise.value.copy(numeroFiscal = v) }
    fun changerRegistreCommerce(v: String) { _entreprise.value = _entreprise.value.copy(registreCommerce = v) }

    fun sauvegarderInfos() {
        val e = _entreprise.value
        viewModelScope.launch {
            val ok = updateEnterprise(
                secteur = e.secteur,
                adresse = e.adresse,
                telephone = e.telephone,
                email = e.email,
                numeroFiscal = e.numeroFiscal,
                registreCommerce = e.registreCommerce,
            )
            _message.value = if (ok) "ok" else "err"
        }
    }

    fun changerLangue(code: String) {
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.LANGUE, code)
            val locales = androidx.core.os.LocaleListCompat.forLanguageTags(code)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
            _message.value = "ok"
        }
    }

    fun clearMessage() { _message.value = null }
}
