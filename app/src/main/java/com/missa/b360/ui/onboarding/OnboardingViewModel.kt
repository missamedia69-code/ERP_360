package com.missa.b360.ui.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.usecase.CompleteOnboardingUseCase
import com.missa.b360.core.domain.usecase.CreateOwnerUserUseCase
import com.missa.b360.core.domain.usecase.SetupEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ValidatePinUseCase
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.security.PinHasher
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Étapes de l'onboarding (Phase B) : langue → profil → entreprise → PIN → email → licence → checklist. */
enum class OnboardingStep { LANGUE, PROFIL, ENTREPRISE, PIN, EMAIL, LICENCE, CHECKLIST }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val setupEnterprise: SetupEnterpriseUseCase,
    private val createOwner: CreateOwnerUserUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val licenceManager: LicenceManager,
    private val validatePin: ValidatePinUseCase,
) : ViewModel() {

    var step by mutableStateOf(OnboardingStep.LANGUE)
        private set

    // Étape langue — conserve le bouton coché en cas de recréation exceptionnelle.
    var langue by mutableStateOf(
        AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "fr" },
    )
        private set

    // Étape profil
    var profil by mutableStateOf<ProfilActivite?>(null)
        private set
    var palier by mutableStateOf<PalierTaille?>(null)
        private set

    // Étape entreprise
    var nomEntreprise by mutableStateOf("")
    var votreNom by mutableStateOf("")
    var devise by mutableStateOf(Iso4217.DEFAUT)
    var pays by mutableStateOf("")
    var tauxTaxe by mutableStateOf(0.0)
    var nomSitePrincipal by mutableStateOf("")
    var enregistrementEnCours by mutableStateOf(false)
        private set

    // Étapes PIN / email / licence
    var pin by mutableStateOf("")
    var pinConfirmation by mutableStateOf("")
    var emailSecours by mutableStateOf("")
    var codeLicence by mutableStateOf("")
    var erreurRes by mutableStateOf<Int?>(null)
        private set
    var licenceActive by mutableStateOf(false)
        private set
    var onboardingTermine by mutableStateOf(false)
        private set

    /**
     * Enregistre d'abord le choix, puis applique la locale.
     *
     * L'activité gère les changements de locale elle-même : Compose se recompose sans
     * être détruit, ce qui évite le flash noir tout en conservant la langue choisie.
     */
    fun choisirLangue(code: String) {
        if (langue == code && AppCompatDelegate.getApplicationLocales().toLanguageTags() == code) return
        langue = code
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.LANGUE, code)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != code) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
        }
    }

    fun choisirProfil(p: ProfilActivite) {
        profil = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, p.name) }
    }

    fun choisirPalier(p: PalierTaille) {
        palier = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, p.name) }
    }

    /** Tente de passer à l'étape suivante avec les validations de chaque étape. */
    fun suivant() {
        erreurRes = null
        when (step) {
            OnboardingStep.LANGUE -> step = OnboardingStep.PROFIL
            OnboardingStep.PROFIL -> if (profil != null && palier != null) step = OnboardingStep.ENTREPRISE
            OnboardingStep.ENTREPRISE -> enregistrerEntreprise()
            OnboardingStep.PIN -> validerPin()
            OnboardingStep.EMAIL -> enregistrerUtilisateur()
            OnboardingStep.LICENCE -> step = OnboardingStep.CHECKLIST
            OnboardingStep.CHECKLIST -> terminer()
        }
    }

    fun precedent() {
        erreurRes = null
        step = when (step) {
            OnboardingStep.PROFIL -> OnboardingStep.LANGUE
            OnboardingStep.ENTREPRISE -> OnboardingStep.PROFIL
            OnboardingStep.PIN -> OnboardingStep.ENTREPRISE
            OnboardingStep.EMAIL -> OnboardingStep.PIN
            else -> step
        }
    }

    /** RA-19 / D4 / D5 — enregistre entreprise + verrous (UseCase transactionnel). */
    private fun enregistrerEntreprise() {
        if (nomEntreprise.isBlank() || nomSitePrincipal.isBlank()) {
            erreurRes = R.string.ob_erreur_nom
            return
        }
        enregistrementEnCours = true
        viewModelScope.launch {
            val ok = setupEnterprise(
                SetupEnterpriseUseCase.Params(
                    nomEntreprise = nomEntreprise,
                    devise = devise,
                    pays = pays.ifBlank { null },
                    tauxTaxe = tauxTaxe,
                    nomSitePrincipal = nomSitePrincipal,
                ),
            )
            enregistrementEnCours = false
            if (ok) step = OnboardingStep.PIN else erreurRes = R.string.ob_erreur_nom
        }
    }

    /** RA-01 — PIN 4–6 chiffres, saisi deux fois. */
    private fun validerPin() {
        if (!PinHasher.isValidFormat(pin)) {
            erreurRes = R.string.ob_pin_invalide
            return
        }
        if (pin != pinConfirmation) {
            erreurRes = R.string.ob_pin_differents
            return
        }
        viewModelScope.launch {
            if (validatePin.definirPin(pin)) step = OnboardingStep.EMAIL
            else erreurRes = R.string.ob_pin_invalide
        }
    }

    /** RA-03 — email de secours obligatoire, création du Propriétaire (D1). */
    private fun enregistrerUtilisateur() {
        viewModelScope.launch {
            when (createOwner(votreNom, emailSecours)) {
                is CreateOwnerUserUseCase.Result.Succes -> step = OnboardingStep.LICENCE
                else -> erreurRes = R.string.ob_email_invalide
            }
        }
    }

    /** RA-05 — activation d'un code licence (optionnel pendant l'onboarding). */
    fun activerCode() {
        viewModelScope.launch {
            licenceActive = licenceManager.activer(codeLicence)
            if (!licenceActive) erreurRes = R.string.ob_code_invalide
        }
    }

    /** RA-11 — clôture de l'onboarding ; l'app démarrera sur le verrou PIN. */
    fun terminer() {
        viewModelScope.launch {
            if (completeOnboarding()) onboardingTermine = true
        }
    }
}
