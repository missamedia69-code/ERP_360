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
import com.missa.b360.core.security.PinHasher
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

/**
 * Étapes de l'onboarding (maquette) : bienvenue → profil → taille → entreprise (+logo)
 * → configuration → PIN → récapitulatif.
 *
 * Toute la logique métier reste identique : entreprise transactionnelle (RA-19 / D4 / D5),
 * PIN (RA-01), propriétaire avec email de secours (RA-03 / D1), clôture (RA-11).
 * L'essai licence (RA-04) est créé automatiquement au premier lancement ; l'activation
 * d'un code reste possible ensuite depuis les réglages administrateur.
 */
enum class OnboardingStep { BIENVENUE, PROFIL, TAILLE, ENTREPRISE, CONFIGURATION, PIN, TERMINE }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val setupEnterprise: SetupEnterpriseUseCase,
    private val getOnboardingProgress: GetOnboardingProgressUseCase,
    private val createOwner: CreateOwnerUserUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val validatePin: ValidatePinUseCase,
) : ViewModel() {

    var step by mutableStateOf(OnboardingStep.BIENVENUE)
        private set

    // --- Étape profil ---
    var profil by mutableStateOf<ProfilActivite?>(null)
        private set
    var palier by mutableStateOf<PalierTaille?>(null)
        private set

    // --- Étape entreprise (informations + logo) ---
    var nomEntreprise by mutableStateOf("")
    var secteur by mutableStateOf("")
    var devise by mutableStateOf("XAF")
    var pays by mutableStateOf("")
    /** Code ISO conservé avec le libellé localisé du pays, notamment pour l'indicatif téléphone. */
    var codePays by mutableStateOf<String?>(null)
        private set
    /** Taux proposé par le pays, modifiable ; texte conservé pendant la frappe. */
    var tauxTaxeTexte by mutableStateOf("0")
        private set
    var tauxTaxe by mutableStateOf(0.0)
        private set
    var nomSitePrincipal by mutableStateOf("")
    var logoUri by mutableStateOf<String?>(null)
        private set

    // --- Étape configuration initiale ---
    var langue by mutableStateOf(
        AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "fr" },
    )
    var fuseau by mutableStateOf(TimeZone.getDefault().id)
    var formatJours by mutableStateOf("dd/MM/yyyy")
    var formatNombres by mutableStateOf("fr")
    var sauvegardesActives by mutableStateOf(true)

    // --- Étape PIN + contact de récupération (propriétaire) ---
    var pin by mutableStateOf("")
    var votreNom by mutableStateOf("")
    var emailSecours by mutableStateOf("")
    var pinDejaConfigure by mutableStateOf(false)
        private set

    var erreurRes by mutableStateOf<Int?>(null)
        private set
    var enregistrementEnCours by mutableStateOf(false)
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
            runCatching { getOnboardingProgress() }.getOrNull()?.let { progression ->
                langue = progression.langue
                profil = progression.profil?.let {
                    runCatching { ProfilActivite.valueOf(it) }.getOrNull()
                }
                palier = progression.palier?.let {
                    runCatching { PalierTaille.valueOf(it) }.getOrNull()
                }
                progression.entreprise?.let { entreprise ->
                    nomEntreprise = entreprise.nom
                    secteur = entreprise.secteur.orEmpty()
                    devise = entreprise.devise
                    pays = entreprise.pays.orEmpty()
                    logoUri = entreprise.logoUri
                    codePays = Iso4217.codePaysDepuisNom(entreprise.pays)
                    if (nomSitePrincipal.isBlank()) nomSitePrincipal = entreprise.nom
                }
                progression.tauxTaxe?.let { definirTauxTaxe(it) }
                fuseau = settingsStore.get(SettingsStore.Keys.FUSEAU_HORAIRE) ?: TimeZone.getDefault().id
                formatJours = settingsStore.get(SettingsStore.Keys.FORMAT_DATE) ?: "dd/MM/yyyy"
                formatNombres = settingsStore.get(SettingsStore.Keys.FORMAT_NOMBRES) ?: "fr"
                sauvegardesActives = settingsStore.get(SettingsStore.Keys.FREQUENCE_SAUVGARDE) != "off"
                pinDejaConfigure = progression.pinConfigure
                step = when {
                    progression.entreprise != null && !progression.pinConfigure -> OnboardingStep.PIN
                    progression.entreprise != null && !progression.proprietaireCree -> OnboardingStep.PIN
                    progression.entreprise != null -> OnboardingStep.TERMINE
                    profil != null && palier != null -> OnboardingStep.ENTREPRISE
                    profil != null -> OnboardingStep.TAILLE
                    else -> OnboardingStep.BIENVENUE
                }
            }
            initialisationTerminee = true
        }
    }

    // --- Navigation ---

    /** Tente de passer à l'étape suivante avec les validations de chaque étape. */
    fun suivant() {
        if (!initialisationTerminee) return
        erreurRes = null
        when (step) {
            OnboardingStep.BIENVENUE -> step = OnboardingStep.PROFIL
            OnboardingStep.PROFIL -> if (profil != null) step = OnboardingStep.TAILLE
            OnboardingStep.TAILLE -> if (palier != null) step = OnboardingStep.ENTREPRISE
            OnboardingStep.ENTREPRISE -> enregistrerEntreprise()
            OnboardingStep.CONFIGURATION -> {
                appliquerConfiguration()
                step = OnboardingStep.PIN
            }
            OnboardingStep.PIN -> validerPinEtProprietaire()
            OnboardingStep.TERMINE -> terminer()
        }
    }

    fun precedent() {
        erreurRes = null
        step = when (step) {
            OnboardingStep.PROFIL -> OnboardingStep.BIENVENUE
            OnboardingStep.TAILLE -> OnboardingStep.PROFIL
            OnboardingStep.ENTREPRISE -> OnboardingStep.TAILLE
            OnboardingStep.CONFIGURATION -> OnboardingStep.ENTREPRISE
            OnboardingStep.PIN -> OnboardingStep.CONFIGURATION
            else -> step
        }
    }

    // --- Profil / taille ---

    fun choisirProfil(p: ProfilActivite) {
        profil = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, p.name) }
    }

    fun choisirPalier(p: PalierTaille) {
        palier = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, p.name) }
    }

    // --- Entreprise ---

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
        val secteurValide = secteur.trim().ifEmpty { null }
        val logoUriSelectionnee = logoUri

        enregistrementEnCours = true
        viewModelScope.launch {
            val ok = runCatching {
                setupEnterprise(
                    SetupEnterpriseUseCase.Params(
                        nomEntreprise = nomEntrepriseValide,
                        devise = deviseSelectionnee,
                        pays = paysSelectionne,
                        codePays = codePays,
                        tauxTaxe = tauxTaxeValide,
                        nomSitePrincipal = nomSiteValide,
                        profilActivite = profil?.name,
                        palierTaille = palier?.name,
                        secteur = secteurValide,
                        logoUri = logoUriSelectionnee,
                    ),
                )
            }.getOrDefault(false)
            enregistrementEnCours = false
            if (ok) step = OnboardingStep.CONFIGURATION
            else erreurRes = R.string.ob_erreur_configuration_entreprise
        }
    }

    // --- Configuration initiale ---

    /**
     * Enregistre la configuration (langue appliquée immédiatement, fuseau, formats et
     * sauvegardes conservés dans les réglages) avant l'étape de sécurité.
     */
    private fun appliquerConfiguration() {
        val langueCible = langue
        val fuseauCible = fuseau
        val formatJoursCible = formatJours
        val formatNombresCible = formatNombres
        val sauvegardesCibles = if (sauvegardesActives) "auto" else "off"
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.LANGUE, langueCible)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != langueCible) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langueCible))
            }
            settingsStore.set(SettingsStore.Keys.FUSEAU_HORAIRE, fuseauCible)
            settingsStore.set(SettingsStore.Keys.FORMAT_DATE, formatJoursCible)
            settingsStore.set(SettingsStore.Keys.FORMAT_NOMBRES, formatNombresCible)
            settingsStore.set(SettingsStore.Keys.FREQUENCE_SAUVGARDE, sauvegardesCibles)
        }
    }

    // --- PIN + propriétaire ---

    /** Écran : le bouton « Suivant » n'est actif qu'avec un PIN de 4 chiffres saisis. */
    fun pinEcranValide(): Boolean = pinDejaConfigure || PinHasher.isValidFormat(pin)

    /** Règle partagée avec l'écriture du propriétaire. */
    fun emailEstValide(): Boolean = CreateOwnerUserUseCase.emailEstValide(emailSecours)

    /**
     * RA-01 + RA-03 — configure le PIN (4 chiffres de la maquette) puis crée le
     * Propriétaire avec son email de secours ; le hash du PIN est copié sur la fiche.
     */
    private fun validerPinEtProprietaire() {
        if (!pinDejaConfigure && !PinHasher.isValidFormat(pin)) {
            erreurRes = R.string.obn_pin_incomplet
            return
        }
        if (!CreateOwnerUserUseCase.emailEstValide(emailSecours)) {
            erreurRes = R.string.ob_email_invalide
            return
        }
        if (enregistrementEnCours) return
        val pinACreer = pin
        val nomProprietaire = votreNom
        val emailProprietaire = emailSecours.trim()
        enregistrementEnCours = true
        viewModelScope.launch {
            val pinOk = if (pinDejaConfigure) {
                true
            } else {
                runCatching { validatePin.definirPin(pinACreer) }.getOrDefault(false)
            }
            if (!pinOk) {
                enregistrementEnCours = false
                erreurRes = R.string.ob_pin_invalide
                return@launch
            }
            val resultat = runCatching { createOwner(nomProprietaire, emailProprietaire) }
                .getOrElse { CreateOwnerUserUseCase.Result.EmailInvalide }
            enregistrementEnCours = false
            when (resultat) {
                is CreateOwnerUserUseCase.Result.Succes -> step = OnboardingStep.TERMINE
                // L'utilisateur existe déjà (reprise) : l'onboarding peut se terminer.
                is CreateOwnerUserUseCase.Result.EmailDejaUtilise -> step = OnboardingStep.TERMINE
                CreateOwnerUserUseCase.Result.EmailInvalide -> erreurRes = R.string.ob_email_invalide
            }
        }
    }

    /** RA-11 — clôture de l'onboarding ; l'app démarrera sur le verrou PIN. */
    private fun terminer() {
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
