package com.missa.b360.ui.onboarding

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.backup.ResultatRestauration
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ProfilConfiguration
import com.missa.b360.core.domain.usecase.BackupUseCases
import com.missa.b360.core.domain.usecase.CompleteOnboardingUseCase
import com.missa.b360.core.domain.usecase.CreateOwnerUserUseCase
import com.missa.b360.core.domain.usecase.GetOnboardingProgressUseCase
import com.missa.b360.core.domain.usecase.SetupEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ValidatePinUseCase
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.security.PinHasher
import com.missa.b360.core.util.FormatPrefs
import com.missa.b360.core.util.Fuseaux
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Étapes de l'onboarding (maquette) : bienvenue → configuration initiale → profil
 * → taille → entreprise (+logo) → PIN → récapitulatif.
 *
 * Toute la logique métier reste identique : entreprise transactionnelle (RA-19 / D4 / D5),
 * PIN (RA-01), propriétaire avec email de secours (RA-03 / D1), clôture (RA-11).
 * L'essai licence (RA-04) est créé automatiquement au premier lancement ; l'activation
 * d'un code reste possible ensuite depuis les réglages administrateur.
 * La configuration (langue, fuseau, formats, sauvegardes) est appliquée en direct
 * via FormatPrefs pour toute l'application.
 */
enum class OnboardingStep { BIENVENUE, CONFIGURATION, PROFIL, ENTREPRISE, PIN, TERMINE }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val setupEnterprise: SetupEnterpriseUseCase,
    private val getOnboardingProgress: GetOnboardingProgressUseCase,
    private val createOwner: CreateOwnerUserUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val validatePin: ValidatePinUseCase,
    private val backupUseCases: BackupUseCases,
) : ViewModel() {

    var step by mutableStateOf(OnboardingStep.BIENVENUE)
        private set

    // --- Étape profil ---
    var profil by mutableStateOf<ProfilActivite?>(null)
        private set

    /** Modules métier cochés quand le profil « Personnalisé » est retenu. */
    var modulesPersonnalises by mutableStateOf<Set<ModuleCode>>(emptySet())
        private set

    /** Options socle retenues (Comptabilité, Trésorerie, Logistique, Reporting…). */
    var modulesSupport by mutableStateOf<Set<ModuleCode>>(emptySet())
        private set

    /** Vrai dès que l'utilisateur touche une option socle : on cesse de la recalculer. */
    var socleAjuste by mutableStateOf(false)
        private set

    /** Effectif déclaré (P1–P6) — champ compact de l'écran « type d'activité ». */
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
    var fuseau by mutableStateOf(Fuseaux.idParDefaut())
    var formatJours by mutableStateOf("dd/MM/yyyy")
    var formatNombres by mutableStateOf("fr")
    var sauvegardesActives by mutableStateOf(true)

    /** Rétention du journal d'audit, en jours : 30, 90 ou 365 (RA-18). */
    var retentionJournal by mutableStateOf(JournalManager.RETENTION_DEFAUT_JOURS)
        private set

    // --- Restauration d'une sauvegarde existante ---
    var restaurationEnCours by mutableStateOf(false)
        private set

    /** Message de restauration (clé de chaîne) : succès ou motif d'échec. */
    var restaurationMessageRes by mutableStateOf<Int?>(null)
        private set

    /** Passe à true quand la base a été remplacée : l'écran doit redémarrer l'app. */
    var restaurationReussie by mutableStateOf(false)
        private set

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
                val actifsEnregistres = ModulesPersonnalises
                    .deserialiser(settingsStore.get(SettingsStore.Keys.MODULES_ACTIFS))
                modulesPersonnalises = ModulesSocle.filtrerMetier(actifsEnregistres).toSet()
                val socleEnregistre = settingsStore.get(SettingsStore.Keys.MODULES_SUPPORT)
                socleAjuste = socleEnregistre != null
                modulesSupport = if (socleEnregistre != null) {
                    ModulesPersonnalises.deserialiser(socleEnregistre).toSet()
                } else {
                    ModulesSocle.filtrerSupport(actifsEnregistres).toSet()
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
                fuseau = Fuseaux.resoudre(settingsStore.get(SettingsStore.Keys.FUSEAU_HORAIRE)).id
                formatJours = settingsStore.get(SettingsStore.Keys.FORMAT_DATE) ?: "dd/MM/yyyy"
                formatNombres = settingsStore.get(SettingsStore.Keys.FORMAT_NOMBRES) ?: "fr"
                sauvegardesActives = settingsStore.get(SettingsStore.Keys.FREQUENCE_SAUVGARDE) != "off"
                retentionJournal = JournalManager.retentionEnJours(
                    settingsStore.get(SettingsStore.Keys.RETENTION_JOURNAL),
                )
                pinDejaConfigure = progression.pinConfigure
                val configurationTerminee =
                    settingsStore.get(SettingsStore.Keys.FUSEAU_HORAIRE) != null
                step = when {
                    progression.entreprise != null && !progression.pinConfigure -> OnboardingStep.PIN
                    progression.entreprise != null && !progression.proprietaireCree -> OnboardingStep.PIN
                    progression.entreprise != null -> OnboardingStep.TERMINE
                    profil != null -> OnboardingStep.ENTREPRISE
                    !configurationTerminee -> OnboardingStep.CONFIGURATION
                    else -> OnboardingStep.PROFIL
                }
                // Les préférences déjà enregistrées rechargent l'affichage global.
                FormatPrefs.charger(settingsStore)
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
            OnboardingStep.BIENVENUE -> step = OnboardingStep.CONFIGURATION
            OnboardingStep.CONFIGURATION -> {
                appliquerConfiguration()
                step = OnboardingStep.PROFIL
            }
            OnboardingStep.PROFIL -> if (profilEcranValide()) step = OnboardingStep.ENTREPRISE
            OnboardingStep.ENTREPRISE -> enregistrerEntreprise()
            OnboardingStep.PIN -> validerPinEtProprietaire()
            OnboardingStep.TERMINE -> terminer()
        }
    }

    fun precedent() {
        erreurRes = null
        step = when (step) {
            OnboardingStep.CONFIGURATION -> OnboardingStep.BIENVENUE
            OnboardingStep.PROFIL -> OnboardingStep.CONFIGURATION
            OnboardingStep.ENTREPRISE -> OnboardingStep.PROFIL
            OnboardingStep.PIN -> OnboardingStep.ENTREPRISE
            else -> step
        }
    }

    // --- Profil d'activité (niveau 1 : métier / niveau 2 : socle) ---

    /**
     * Le profil ne pilote plus que les modules **métier** : les briques
     * transverses (Comptabilité, Trésorerie, Logistique, Reporting…) sont
     * recalculées comme simple proposition tant que l'utilisateur n'y a pas touché.
     */
    fun choisirProfil(p: ProfilActivite) {
        profil = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, p.name) }
        if (p != ProfilActivite.CUSTOM) {
            modulesPersonnalises = ModulesSocle
                .filtrerMetier(ProfilConfiguration.modulesPourProfil(p))
                .toSet()
        }
        rafraichirSocle()
        enregistrerModules()
    }

    /**
     * Bascule sur le profil « Personnalisé » : la sélection métier démarre de
     * celle du profil déjà choisi, pour n'avoir qu'à ajuster au lieu de tout cocher.
     */
    fun choisirPersonnalisation() {
        val depart = when {
            modulesPersonnalises.isNotEmpty() -> modulesPersonnalises
            profil != null && profil != ProfilActivite.CUSTOM ->
                ModulesSocle.filtrerMetier(ProfilConfiguration.modulesPourProfil(profil!!)).toSet()
            else -> emptySet()
        }
        profil = ProfilActivite.CUSTOM
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, ProfilActivite.CUSTOM.name)
        }
        modulesPersonnalises = depart
        rafraichirSocle()
        enregistrerModules()
    }

    /** Coche / décoche un module métier dans la sélection personnalisée. */
    fun basculerModule(module: ModuleCode) {
        val nouvelle = modulesPersonnalises.toMutableSet()
        if (!nouvelle.add(module)) nouvelle.remove(module)
        modulesPersonnalises = nouvelle
        rafraichirSocle()
        enregistrerModules()
    }

    /** Tout cocher / tout décocher depuis l'en-tête de la liste des modules métier. */
    fun basculerTousLesModules() {
        val tout = ModulesSocle.metier.toSet()
        modulesPersonnalises = if (modulesPersonnalises.size == tout.size) emptySet() else tout
        rafraichirSocle()
        enregistrerModules()
    }

    /** Active / désactive une option socle — le choix de l'utilisateur devient prioritaire. */
    fun basculerSupport(module: ModuleCode) {
        val nouvelle = modulesSupport.toMutableSet()
        if (!nouvelle.add(module)) nouvelle.remove(module)
        modulesSupport = nouvelle
        socleAjuste = true
        enregistrerModules()
    }

    /** Revient aux options socle conseillées pour le profil et l'effectif déclarés. */
    fun reinitialiserSocle() {
        socleAjuste = false
        rafraichirSocle()
        enregistrerModules()
    }

    /** Options socle conseillées pour la configuration courante (badges « Recommandé »). */
    fun socleRecommande(): Set<ModuleCode> =
        ModulesSocle.recommandes(profil, palier, modulesMetier())

    /** Modules métier retenus (profil ou sélection personnalisée). */
    fun modulesMetier(): List<ModuleCode> =
        ModulesSocle.metierActifs(profil, modulesPersonnalises)

    /** Recalcule la proposition socle tant que l'utilisateur ne l'a pas ajustée. */
    private fun rafraichirSocle() {
        if (!socleAjuste) modulesSupport = socleRecommande()
    }

    /** La sélection est conservée immédiatement (reprise d'onboarding). */
    private fun enregistrerModules() {
        val actifs = ModulesPersonnalises.modulesActifs(profil, modulesPersonnalises, modulesSupport)
        val valeurActifs = ModulesPersonnalises.serialiser(actifs)
        val valeurSocle = ModulesPersonnalises.serialiser(ModulesSocle.filtrerSupport(modulesSupport))
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.MODULES_ACTIFS, valeurActifs)
            if (socleAjuste) settingsStore.set(SettingsStore.Keys.MODULES_SUPPORT, valeurSocle)
        }
    }

    /** L'écran « type d'activité » n'est valide qu'avec au moins un module métier. */
    fun profilEcranValide(): Boolean = when (profil) {
        null -> false
        ProfilActivite.CUSTOM -> modulesPersonnalises.isNotEmpty()
        else -> true
    }

    /** Effectif choisi dans le champ bleu de l'écran « type d'activité ». */
    fun choisirPalier(p: PalierTaille) {
        palier = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, p.name) }
        rafraichirSocle()
        enregistrerModules()
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
            // L'entreprise enregistrée, l'étape suivante est le PIN (la configuration
            // initiale a déjà été franchie avant le profil).
            if (ok) step = OnboardingStep.PIN
            else erreurRes = R.string.ob_erreur_configuration_entreprise
        }
    }

    // --- Configuration initiale ---

    /** La langue est appliquée immédiatement (l'interface se recompose) et conservée. */
    fun appliquerLangue(code: String) {
        langue = code
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.LANGUE, code)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != code) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
            }
        }
    }

    /** Le fuseau pilote immédiatement l'affichage des dates dans toute l'application. */
    fun appliquerFuseau(id: String) {
        fuseau = id
        FormatPrefs.appliquer(fuseau, formatJours, formatNombres)
    }

    /** Le motif de date pilote immédiatement DateUtils (journal, licences, clients…). */
    fun appliquerFormatDate(motif: String) {
        formatJours = motif
        FormatPrefs.appliquer(fuseau, formatJours, formatNombres)
    }

    /** Le style des nombres pilote immédiatement MoneyUtils (montants partout). */
    fun appliquerFormatNombres(style: String) {
        formatNombres = style
        FormatPrefs.appliquer(fuseau, formatJours, formatNombres)
    }

    /** Les sauvegardes automatiques (démarrage) sont conservées immédiatement. */
    fun appliquerSauvegardes(actives: Boolean) {
        sauvegardesActives = actives
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.FREQUENCE_SAUVGARDE, if (actives) "auto" else "off")
        }
    }

    /** La rétention du journal pilote la purge quotidienne (RA-18). */
    fun appliquerRetentionJournal(jours: Int) {
        retentionJournal = JournalManager.retentionEnJours(jours.toString())
        val valeur = retentionJournal.toString()
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.RETENTION_JOURNAL, valeur)
        }
    }

    /**
     * Restaure une sauvegarde `.db` choisie par l'utilisateur : les données de
     * l'appareil sont remplacées après copie de sécurité, puis l'application doit
     * redémarrer pour rouvrir la base restaurée.
     */
    fun restaurerSauvegarde(source: Uri) {
        if (restaurationEnCours) return
        restaurationEnCours = true
        restaurationMessageRes = null
        viewModelScope.launch {
            val resultat = runCatching { backupUseCases.restaurer(source) }
                .getOrElse { ResultatRestauration.Echec(ResultatRestauration.Motif.ECHEC_COPIE) }
            restaurationEnCours = false
            when (resultat) {
                is ResultatRestauration.Succes -> {
                    restaurationMessageRes = R.string.obn_restaurer_ok
                    restaurationReussie = true
                }
                is ResultatRestauration.Echec -> {
                    restaurationMessageRes = when (resultat.motif) {
                        ResultatRestauration.Motif.FICHIER_ILLISIBLE ->
                            R.string.obn_restaurer_err_fichier
                        ResultatRestauration.Motif.FORMAT_INVALIDE ->
                            R.string.obn_restaurer_err_format
                        ResultatRestauration.Motif.VERSION_TROP_RECENTE ->
                            R.string.obn_restaurer_err_version
                        ResultatRestauration.Motif.ECHEC_COPIE ->
                            R.string.obn_restaurer_err_copie
                    }
                }
            }
        }
    }

    /**
     * Enregistre la configuration (déjà appliquée en direct à chaque choix) avant
     * l'étape de sécurité : passage au prochain écran.
     */
    private fun appliquerConfiguration() {
        val langueCible = langue
        val fuseauCible = fuseau
        val formatJoursCible = formatJours
        val formatNombresCible = formatNombres
        val sauvegardesCibles = if (sauvegardesActives) "auto" else "off"
        val retentionCible = retentionJournal
        // Application immédiate : la langue recompose l'interface, les formats
        // d'affichage (fuseau, date, nombres) pilotent MoneyUtils/DateUtils partout.
        FormatPrefs.appliquer(fuseauCible, formatJoursCible, formatNombresCible)
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.LANGUE, langueCible)
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != langueCible) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langueCible))
            }
            settingsStore.set(SettingsStore.Keys.FUSEAU_HORAIRE, fuseauCible)
            settingsStore.set(SettingsStore.Keys.FORMAT_DATE, formatJoursCible)
            settingsStore.set(SettingsStore.Keys.FORMAT_NOMBRES, formatNombresCible)
            settingsStore.set(SettingsStore.Keys.FREQUENCE_SAUVGARDE, sauvegardesCibles)
            settingsStore.set(SettingsStore.Keys.RETENTION_JOURNAL, retentionCible.toString())
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
