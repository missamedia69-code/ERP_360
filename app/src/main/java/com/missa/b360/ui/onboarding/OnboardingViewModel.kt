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
import com.missa.b360.core.domain.usecase.GetOnboardingProgressUseCase
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
    private val getOnboardingProgress: GetOnboardingProgressUseCase,
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
    /** Code ISO conservé avec le libellé localisé du pays, notamment pour l'indicatif téléphone. */
    var codePays by mutableStateOf<String?>(null)
        private set
    var tauxTaxe by mutableStateOf(0.0)
        private set
    /** Texte conservé pendant la frappe afin de ne pas transformer « 19, » en « 19.0 ». */
    var tauxTaxeTexte by mutableStateOf("0")
        private set
    var nomSitePrincipal by mutableStateOf("")
    var logoUri by mutableStateOf<String?>(null)
        private set
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
    var initialisationTerminee by mutableStateOf(false)
        private set

    init {
        restaurerProgression()
    }

    /**
     * Reprend à l'étape réellement atteinte lorsque l'application a été fermée ou mise
     * à jour au milieu de l'onboarding. Les champs enregistrés restent disponibles.
     */
    private fun restaurerProgression() {
        viewModelScope.launch {
            runCatching { getOnboardingProgress() }
                .getOrNull()
                ?.let { progression ->
                    langue = progression.langue
                    profil = progression.profil?.let {
                        runCatching { ProfilActivite.valueOf(it) }.getOrNull()
                    }
                    palier = progression.palier?.let {
                        runCatching { PalierTaille.valueOf(it) }.getOrNull()
                    }
                    progression.entreprise?.let { entreprise ->
                        nomEntreprise = entreprise.nom
                        devise = entreprise.devise
                        pays = entreprise.pays.orEmpty()
                        logoUri = entreprise.logoUri
                        codePays = Iso4217.codePaysDepuisNom(entreprise.pays)
                    }
                    progression.tauxTaxe?.let(::definirTauxTaxe)
                    step = when {
                        progression.entreprise != null && !progression.pinConfigure -> OnboardingStep.PIN
                        progression.entreprise != null && !progression.proprietaireCree -> OnboardingStep.EMAIL
                        progression.entreprise != null -> OnboardingStep.LICENCE
                        profil != null && palier != null -> OnboardingStep.ENTREPRISE
                        profil != null || palier != null -> OnboardingStep.PROFIL
                        else -> OnboardingStep.LANGUE
                    }
                }
            initialisationTerminee = true
        }
    }

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
        if (!initialisationTerminee) return
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

    /** Applique un pays du catalogue et rend son taux immédiatement modifiable. */
    fun choisirPays(nom: String, code: String, tauxSuggere: Double) {
        pays = nom
        codePays = code
        definirTauxTaxe(tauxSuggere)
    }

    /** Une saisie libre n'a pas de code ISO ni d'indicatif téléphonique supposé. */
    fun modifierPaysManuel(nom: String) {
        pays = nom
        codePays = null
    }

    /** Le logo est facultatif, mais son URI est conservée avec l'entreprise après l'onboarding. */
    fun definirLogoUri(uri: String?) {
        logoUri = uri
    }

    /** Conserve fidèlement la saisie manuelle, y compris une virgule ou décimale en cours. */
    fun modifierTauxTaxe(valeur: String) {
        tauxTaxeTexte = valeur
        tauxTaxeValide()?.let { tauxTaxe = it }
    }

    fun tauxTaxeEstValide(): Boolean = tauxTaxeValide() != null

    private fun definirTauxTaxe(valeur: Double) {
        tauxTaxe = valeur
        tauxTaxeTexte = if (valeur % 1.0 == 0.0) valeur.toInt().toString() else valeur.toString()
    }

    /** Un taux de 0 % est admis : il couvre les pays ou activités sans TVA/GST. */
    private fun tauxTaxeValide(): Double? = tauxTaxeTexte
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it in 0.0..100.0 }

    /** RA-19 / D4 / D5 — enregistre entreprise + verrous (UseCase transactionnel). */
    private fun enregistrerEntreprise() {
        val nomEntrepriseValide = nomEntreprise.trim()
        val nomSiteValide = nomSitePrincipal.trim()
        val tauxTaxeValide = tauxTaxeValide() ?: run {
            erreurRes = R.string.ob_erreur_taux_taxe
            return
        }
        when {
            nomEntrepriseValide.isEmpty() -> {
                erreurRes = R.string.ob_erreur_nom_entreprise
                return
            }
            nomSiteValide.isEmpty() -> {
                erreurRes = R.string.ob_erreur_site_principal
                return
            }
            enregistrementEnCours -> return
        }
        val deviseSelectionnee = devise
        val paysSelectionne = pays.trim().ifEmpty { null }

        enregistrementEnCours = true
        viewModelScope.launch {
            val ok = runCatching {
                setupEnterprise(
                    SetupEnterpriseUseCase.Params(
                        nomEntreprise = nomEntrepriseValide,
                        devise = deviseSelectionnee,
                        pays = paysSelectionne,
                        codePays = codePays,
                        logoUri = logoUri,
                        tauxTaxe = tauxTaxeValide,
                        nomSitePrincipal = nomSiteValide,
                        profilActivite = profil?.name,
                        palierTaille = palier?.name,
                    ),
                )
            }.getOrDefault(false)
            enregistrementEnCours = false
            if (ok) step = OnboardingStep.PIN
            else erreurRes = R.string.ob_erreur_configuration_entreprise
        }
    }

    /** Utilisé par l'UI pour ne rendre « Suivant » disponible qu'avec un PIN complet. */
    fun pinEstValide(): Boolean = PinHasher.isValidFormat(pin)

    /** Règle partagée avec l'écriture du propriétaire. */
    fun emailEstValide(): Boolean = CreateOwnerUserUseCase.emailEstValide(emailSecours)

    /** RA-01 — PIN 4–6 chiffres, saisi deux fois. */
    private fun validerPin() {
        if (!pinEstValide()) {
            erreurRes = R.string.ob_pin_invalide
            return
        }
        if (pin != pinConfirmation) {
            erreurRes = R.string.ob_pin_differents
            return
        }
        if (enregistrementEnCours) return
        val pinValide = pin
        enregistrementEnCours = true
        viewModelScope.launch {
            val ok = runCatching { validatePin.definirPin(pinValide) }.getOrDefault(false)
            enregistrementEnCours = false
            if (ok) step = OnboardingStep.EMAIL
            else erreurRes = R.string.ob_pin_invalide
        }
    }

    /** RA-03 — email de secours obligatoire, création du Propriétaire (D1). */
    private fun enregistrerUtilisateur() {
        if (!emailEstValide()) {
            erreurRes = R.string.ob_email_invalide
            return
        }
        if (enregistrementEnCours) return
        val nomProprietaire = votreNom
        val emailProprietaire = emailSecours.trim()
        enregistrementEnCours = true
        viewModelScope.launch {
            val resultat = runCatching { createOwner(nomProprietaire, emailProprietaire) }
                .getOrElse { CreateOwnerUserUseCase.Result.EmailInvalide }
            enregistrementEnCours = false
            when (resultat) {
                is CreateOwnerUserUseCase.Result.Succes -> step = OnboardingStep.LICENCE
                else -> erreurRes = R.string.ob_email_invalide
            }
        }
    }

    /** RA-05 — activation d'un code licence (optionnel pendant l'onboarding). */
    fun activerCode() {
        if (codeLicence.isBlank() || enregistrementEnCours) return
        val codeAActiver = codeLicence.trim()
        erreurRes = null
        enregistrementEnCours = true
        viewModelScope.launch {
            val activee = runCatching { licenceManager.activer(codeAActiver) }.getOrDefault(false)
            enregistrementEnCours = false
            if (activee) licenceActive = true
            else erreurRes = R.string.ob_code_invalide
        }
    }

    /** RA-11 — clôture de l'onboarding ; l'app démarrera sur le verrou PIN. */
    fun terminer() {
        if (enregistrementEnCours) return
        enregistrementEnCours = true
        viewModelScope.launch {
            val termine = runCatching { completeOnboarding() }.getOrDefault(false)
            enregistrementEnCours = false
            if (termine) onboardingTermine = true
            else erreurRes = R.string.ob_erreur_finalisation
        }
    }
}
