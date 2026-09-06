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
import com.missa.b360.core.domain.model.CleIdentifiant
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ReferentielFiscal
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

    /**
     * Modules métier actifs : le pack du profil, plus ceux que l'utilisateur a
     * ajoutés. En profil « Personnalisé », uniquement sa sélection.
     */
    var modulesPersonnalises by mutableStateOf<Set<ModuleCode>>(emptySet())
        private set

    /** Options socle actives = recommandations verrouillées + [extrasSupport]. */
    var modulesSupport by mutableStateOf<Set<ModuleCode>>(emptySet())
        private set

    /**
     * Briques transverses ajoutées à la main, en plus des recommandations.
     *
     * Elles sont conservées à part pour survivre à tout recalcul : changer
     * d'effectif ou de profil met à jour le socle recommandé sans effacer ce
     * que l'utilisateur a explicitement demandé.
     */
    var extrasSupport by mutableStateOf<Set<ModuleCode>>(emptySet())
        private set

    /** Vrai dès que l'utilisateur a ajouté une brique hors recommandations. */
    val socleAjuste: Boolean get() = extrasSupport.isNotEmpty()

    /** Effectif déclaré (P1–P6) — champ compact de l'écran « type d'activité ». */
    var palier by mutableStateOf<PalierTaille?>(null)
        private set

    // --- Étape entreprise (informations + logo) ---
    var nomEntreprise by mutableStateOf("")
    var secteur by mutableStateOf("")
    var devise by mutableStateOf(Iso4217.DEVISE_REPLI)
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

    /** Coordonnées reprises sur les devis, factures et bons de livraison. */
    var telephone by mutableStateOf("")
    var email by mutableStateOf("")
    var adresse by mutableStateOf("")

    /** Identifiants légaux exigés sur les pièces de vente (NIU / NIF, RCCM). */
    var numeroFiscal by mutableStateOf("")
    var registreCommerce by mutableStateOf("")

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
        private set

    /** Seconde saisie du code : un PIN mal tapé enfermerait dehors. */
    var pinConfirmation by mutableStateOf("")
        private set

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
                // Les recommandations sont recalculées ; on ne retient de la
                // sauvegarde que les ajouts volontaires (normalisés plus bas,
                // une fois l'effectif restauré).
                extrasSupport = ModulesPersonnalises
                    .deserialiser(socleEnregistre ?: "")
                    .let(ModulesSocle::filtrerSupport)
                    .toSet()
                modulesSupport = ModulesSocle.filtrerSupport(actifsEnregistres).toSet()
                palier = progression.palier?.let {
                    runCatching { PalierTaille.valueOf(it) }.getOrNull()
                }
                progression.entreprise?.let { entreprise ->
                    nomEntreprise = entreprise.nom
                    secteur = entreprise.secteur.orEmpty()
                    devise = entreprise.devise
                    pays = entreprise.pays.orEmpty()
                    logoUri = entreprise.logoUri
                    telephone = entreprise.telephone.orEmpty()
                    email = entreprise.email.orEmpty()
                    adresse = entreprise.adresse.orEmpty()
                    numeroFiscal = entreprise.numeroFiscal.orEmpty()
                    registreCommerce = entreprise.registreCommerce.orEmpty()
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
                // Profil, effectif et modules métier sont maintenant connus :
                // le socle recommandé peut être recalculé, et les ajouts
                // volontaires débarrassés de ce qui est devenu recommandé.
                rafraichirSocle()
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
        // Re-cliquer sur la carte déjà choisie ne fait que replier le panneau :
        // les modules ajoutés à la main ne doivent pas disparaître au passage.
        val changementDeProfil = profil != p
        profil = p
        viewModelScope.launch { settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, p.name) }
        if (p != ProfilActivite.CUSTOM) {
            val pack = ModulesSocle.metierDuPack(p)
            modulesPersonnalises = if (changementDeProfil) pack else modulesPersonnalises + pack
        }
        rafraichirSocle()
        enregistrerModules()
    }

    /**
     * Ajoute ou retire un module métier **hors pack**. Un module inclus dans le
     * profil est verrouillé : l'appel est sans effet, la cohérence du pack
     * prime sur le clic.
     */
    fun basculerModule(module: ModuleCode) {
        if (module in ModulesSocle.metierDuPack(profil)) return
        val nouvelle = modulesPersonnalises.toMutableSet()
        if (!nouvelle.add(module)) nouvelle.remove(module)
        modulesPersonnalises = nouvelle
        rafraichirSocle()
        enregistrerModules()
    }

    /**
     * Ajoute ou retire une brique transverse **hors recommandations**. Les
     * modules recommandés pour le profil sont verrouillés.
     */
    fun basculerSupport(module: ModuleCode) {
        if (module in socleRecommande()) return
        val nouvelle = extrasSupport.toMutableSet()
        if (!nouvelle.add(module)) nouvelle.remove(module)
        extrasSupport = nouvelle
        rafraichirSocle()
        enregistrerModules()
    }

    /** Retire tous les ajouts manuels : on revient au pack seul. */
    fun reinitialiserSocle() {
        extrasSupport = emptySet()
        modulesPersonnalises = ModulesSocle.metierDuPack(profil)
        rafraichirSocle()
        enregistrerModules()
    }

    /** Options socle conseillées pour la configuration courante (badges « Recommandé »). */
    fun socleRecommande(): Set<ModuleCode> =
        ModulesSocle.recommandes(profil, palier, modulesMetier())

    /** Modules métier retenus (profil ou sélection personnalisée). */
    fun modulesMetier(): List<ModuleCode> =
        ModulesSocle.metierActifs(profil, modulesPersonnalises)

    /**
     * Recalcule le socle : recommandations du moment — elles suivent le profil,
     * l'effectif et les modules métier — augmentées des ajouts volontaires.
     */
    private fun rafraichirSocle() {
        val recommandes = socleRecommande()
        extrasSupport = extrasSupport - recommandes
        modulesSupport = ModulesSocle.support
            .filter { it in recommandes || it in extrasSupport }
            .toSet()
    }

    /** La sélection est conservée immédiatement (reprise d'onboarding). */
    private fun enregistrerModules() {
        val actifs = ModulesPersonnalises.modulesActifs(profil, modulesPersonnalises, modulesSupport)
        val valeurActifs = ModulesPersonnalises.serialiser(actifs)
        val valeurExtras = ModulesPersonnalises.serialiser(ModulesSocle.filtrerSupport(extrasSupport))
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.MODULES_ACTIFS, valeurActifs)
            settingsStore.set(SettingsStore.Keys.MODULES_SUPPORT, valeurExtras)
        }
    }

    /**
     * L'écran « type d'activité » n'est valide qu'avec au moins un module métier.
     *
     * Le cas « Personnalisé » n'est plus proposé à l'installation, mais reste
     * traité : une application installée avant ce changement peut avoir ce
     * profil enregistré et doit continuer à s'ouvrir normalement.
     */
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

    /**
     * Applique le « pack pays » : devise officielle, taux de taxe suggéré et
     * indicatif téléphonique. Chaque valeur reste modifiable ensuite ; seul le
     * téléphone est préservé s'il porte déjà autre chose que l'ancien indicatif.
     *
     * Les identifiants légaux et la zone fiscale, eux, découlent du seul
     * [codePays] et n'ont donc rien à mémoriser ici.
     */
    fun choisirPays(nom: String, code: String, tauxSuggere: Double) {
        val ancienIndicatif = Iso4217.indicatifTelephone(codePays)
        pays = nom
        codePays = code
        definirTauxTaxe(tauxSuggere)
        Iso4217.deviseDuPays(code)?.let { devise = it }
        // L'indicatif suit le pays, y compris dans un numéro déjà saisi.
        telephone = Iso4217.remplacerIndicatif(
            numero = telephone,
            ancien = ancienIndicatif,
            nouveau = Iso4217.indicatifTelephone(code),
        )
        oublierIdentifiantsHorsPays()
    }

    /**
     * Un NIU camerounais n'est pas un numéro de TVA français : quand le pays
     * change, une valeur qui ne respecte plus le format attendu est effacée
     * plutôt que laissée en erreur. Ce qui reste valide est conservé.
     */
    private fun oublierIdentifiantsHorsPays() {
        ReferentielFiscal.regles(codePays, "", "").forEach { regle ->
            when (regle.cle) {
                CleIdentifiant.FISCAL ->
                    if (!regle.estValide(numeroFiscal)) numeroFiscal = ""
                CleIdentifiant.REGISTRE ->
                    if (!regle.estValide(registreCommerce)) registreCommerce = ""
            }
        }
    }

    /** L'e-mail est facultatif, mais s'il est saisi il doit rester exploitable. */
    fun emailEntrepriseEstValide(): Boolean =
        email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

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
        if (!emailEntrepriseEstValide()) {
            erreurRes = R.string.obn_erreur_email_entreprise
            return
        }
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
                        telephone = telephone.trim()
                            .takeUnless { it.isEmpty() || Iso4217.estIndicatifSeul(it) },
                        email = email.trim().ifEmpty { null },
                        adresse = adresse.trim().ifEmpty { null },
                        numeroFiscal = numeroFiscal.trim().ifEmpty { null },
                        registreCommerce = registreCommerce.trim().ifEmpty { null },
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

    /** Le code est saisi deux fois : d'abord [pin], puis [pinConfirmation]. */
    val pinConfirme: Boolean
        get() = pin.length == PIN_LONGUEUR && pin == pinConfirmation

    /** Étape courante du pavé : vrai tant que le premier code n'est pas complet. */
    val pinEnPremiereSaisie: Boolean get() = pin.length < PIN_LONGUEUR

    /**
     * Ajoute un chiffre à la saisie en cours. Quand la confirmation s'achève
     * sur un code différent, tout est remis à zéro avec un message : mieux vaut
     * recommencer deux saisies que se retrouver dehors à la première ouverture.
     */
    fun saisirChiffrePin(chiffre: String) {
        if (pinDejaConfigure) return
        erreurRes = null
        when {
            pin.length < PIN_LONGUEUR -> pin += chiffre
            pinConfirmation.length < PIN_LONGUEUR -> {
                pinConfirmation += chiffre
                if (pinConfirmation.length == PIN_LONGUEUR && pinConfirmation != pin) {
                    erreurRes = R.string.ob_pin_differents
                    pin = ""
                    pinConfirmation = ""
                }
            }
        }
    }

    /** Efface le dernier chiffre de la saisie en cours. */
    fun effacerChiffrePin() {
        if (pinDejaConfigure) return
        erreurRes = null
        if (pinConfirmation.isNotEmpty()) {
            pinConfirmation = pinConfirmation.dropLast(1)
        } else {
            pin = pin.dropLast(1)
        }
    }

    /** Repart d'un code vierge (bouton « Recommencer »). */
    fun reinitialiserPin() {
        if (pinDejaConfigure) return
        erreurRes = null
        pin = ""
        pinConfirmation = ""
    }

    /**
     * Écran : le bouton « Suivant » exige un code de quatre chiffres saisi
     * deux fois à l'identique.
     */
    fun pinEcranValide(): Boolean =
        pinDejaConfigure || (PinHasher.isValidFormat(pin) && pinConfirme)

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

    companion object {
        /** Longueur du code PIN de la maquette. */
        const val PIN_LONGUEUR = 4
    }
}
