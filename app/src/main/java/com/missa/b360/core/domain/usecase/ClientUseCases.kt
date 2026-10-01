package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.permissions.PermissionChecker
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Contrôle serveur des droits Clients à partir de la session réellement enregistrée. */
class ClientPermissionGate @Inject constructor(
    private val settingsStore: SettingsStore,
    private val userDao: UserDao,
    private val permissionChecker: PermissionChecker,
) {
    suspend fun autorise(action: PermissionChecker.Action): Boolean {
        val currentUserId = settingsStore.getLong(SettingsStore.Keys.CURRENT_USER_ID) ?: return false
        val user = userDao.getById(currentUserId) ?: return false
        if (!user.actif) return false
        return permissionChecker.hasPermission(user.roleId, "CLIENTS", action)
    }
}

/** Informations détaillées persistées avec un client, y compris ses contacts et adresses. */
data class ClientProfileInput(
    val nif: String? = null,
    val typeIdentifiantFiscal: String? = null,
    val numeroTva: String? = null,
    val assujettiTva: Boolean = true,
    val exonereTva: Boolean = false,
    val motifExoneration: String? = null,
    val tauxTva: Double? = null,
    val commercial: String? = null,
    val conditionPaiementJours: Int = 30,
    val conditionsPaiement: String? = null,
    val grilleTarifaire: String? = null,
    val remiseMaxPct: Double? = null,
    val segment: String? = null,
    val canalVente: String? = null,
    val territoire: String? = null,
    val compteComptable: String? = null,
    val contacts: List<ClientContactEntity> = emptyList(),
    val addresses: List<ClientAddressEntity> = emptyList(),
    /** L'édition simple conserve les relations jusqu'à ce que leurs écrans soient validés. */
    val replaceRelations: Boolean = true,
)

/** Règles de saisie communes à la création et à l'édition d'un client. */
object ClientValidation {
    const val LONGUEUR_NOM_MAX = 120
    const val LONGUEUR_EMAIL_MAX = 254
    const val LONGUEUR_ADRESSE_MAX = 250
    const val LONGUEUR_NOTES_MAX = 1_000
    private const val NOMBRE_CHIFFRES_TELEPHONE_MIN = 7
    private const val NOMBRE_CHIFFRES_TELEPHONE_MAX = 15

    fun normaliseNom(nom: String): String = nom.trim()

    /**
     * Conserve uniquement les caractères compatibles avec un numéro international
     * lisible : chiffres, espaces, tirets, parenthèses et un `+` initial.
     */
    fun filtrerTelephonePourSaisie(saisie: String): String = buildString {
        saisie.forEachIndexed { index, caractere ->
            when {
                caractere in '0'..'9' || caractere == ' ' || caractere == '-' ||
                    caractere == '(' || caractere == ')' -> append(caractere)
                caractere == '+' && index == 0 -> append(caractere)
            }
        }
    }

    /** Numéro sans indicatif : le sélecteur de pays gère le `+` séparément. */
    fun filtrerTelephoneLocalPourSaisie(saisie: String): String = buildString {
        saisie.forEach { caractere ->
            if (caractere in '0'..'9' || caractere == ' ' || caractere == '-' ||
                caractere == '(' || caractere == ')'
            ) {
                append(caractere)
            }
        }
    }

    /** Stockage canonique afin que 690 00-00-00 et 690000000 soient identiques. */
    fun normaliseTelephone(telephone: String): String {
        val saisie = telephone.trim()
        val chiffres = saisie.filter { it in '0'..'9' }
        return if (saisie.startsWith('+')) "+$chiffres" else chiffres
    }

    /** Assemble l'indicatif choisi et le numéro local avant validation/persistance. */
    fun telephoneAvecIndicatif(telephoneLocal: String, indicatif: String?): String {
        val chiffres = telephoneLocal.filter { it in '0'..'9' }
        val indicatifNormalise = indicatif
            ?.takeIf { it.startsWith('+') && it.drop(1).all { chiffre -> chiffre in '0'..'9' } }
        return if (indicatifNormalise == null) chiffres else "$indicatifNormalise$chiffres"
    }

    /** Retire l'indicatif du numéro stocké afin de préremplir le champ local. */
    fun telephoneSansIndicatif(telephone: String, indicatif: String?): String {
        val normalise = normaliseTelephone(telephone)
        return indicatif?.takeIf { normalise.startsWith(it) }
            ?.let { normalise.removePrefix(it) }
            ?: normalise.removePrefix("+")
    }

    fun telephoneEstValide(telephone: String): Boolean {
        val saisie = telephone.trim()
        if (saisie.isEmpty()) return false
        if (saisie != filtrerTelephonePourSaisie(saisie)) return false
        val normalise = normaliseTelephone(saisie)
        val chiffres = normalise.removePrefix("+")
        return chiffres.length in NOMBRE_CHIFFRES_TELEPHONE_MIN..NOMBRE_CHIFFRES_TELEPHONE_MAX
    }

    fun normaliseEmail(email: String?): String? = email?.trim()?.lowercase()?.ifBlank { null }

    /** Validation volontairement stricte des erreurs manifestes, sans imposer un domaine. */
    fun emailEstValide(email: String?): Boolean {
        val normalise = normaliseEmail(email) ?: return true
        if (normalise.length > LONGUEUR_EMAIL_MAX || normalise.any { it.isWhitespace() }) return false
        val arobase = normalise.indexOf('@')
        if (arobase !in 1..64 || arobase != normalise.lastIndexOf('@')) return false
        val local = normalise.substring(0, arobase)
        if (local.startsWith('.') || local.endsWith('.') || ".." in local ||
            !local.all { it.isLetterOrDigit() || it in ".!#\$%&'*+/=?^_`{|}~-" }
        ) {
            return false
        }
        val domaine = normalise.substring(arobase + 1)
        return domaine.length in 3..253 &&
            '.' in domaine &&
            !domaine.startsWith('.') &&
            !domaine.endsWith('.') &&
            domaine.split('.').all { etiquette ->
                etiquette.isNotEmpty() &&
                    etiquette.length <= 63 &&
                    !etiquette.startsWith('-') &&
                    !etiquette.endsWith('-') &&
                    etiquette.all { it.isLetterOrDigit() || it == '-' }
            }
    }

    fun normaliseTexte(texte: String?): String? = texte?.trim()?.ifBlank { null }

    fun nomEstValide(nom: String): Boolean = normaliseNom(nom).length in 2..LONGUEUR_NOM_MAX

    fun adresseEstValide(adresse: String?): Boolean =
        normaliseTexte(adresse)?.length?.let { it <= LONGUEUR_ADRESSE_MAX } ?: true

    fun notesSontValides(notes: String?): Boolean =
        normaliseTexte(notes)?.length?.let { it <= LONGUEUR_NOTES_MAX } ?: true

    fun profilEstValide(profil: ClientProfileInput): Boolean =
        (profil.nif?.trim()?.length?.let { it <= 80 } ?: true) &&
            (profil.typeIdentifiantFiscal?.trim()?.length?.let { it <= 40 } ?: true) &&
            (profil.numeroTva?.trim()?.length?.let { it <= 80 } ?: true) &&
            (profil.motifExoneration?.trim()?.length?.let { it <= 240 } ?: true) &&
            (!profil.exonereTva || !profil.motifExoneration.isNullOrBlank()) &&
            (profil.tauxTva == null || (profil.tauxTva.isFinite() && profil.tauxTva in 0.0..100.0)) &&
            (profil.remiseMaxPct == null || (profil.remiseMaxPct.isFinite() && profil.remiseMaxPct in 0.0..100.0)) &&
            (profil.commercial?.trim()?.length?.let { it <= LONGUEUR_NOM_MAX } ?: true) &&
            (profil.conditionsPaiement?.trim()?.length?.let { it <= 240 } ?: true) &&
            (profil.grilleTarifaire?.trim()?.length?.let { it <= 80 } ?: true) &&
            (profil.segment?.trim()?.length?.let { it <= 80 } ?: true) &&
            (profil.canalVente?.trim()?.length?.let { it <= 80 } ?: true) &&
            (profil.territoire?.trim()?.length?.let { it <= 120 } ?: true) &&
            (profil.compteComptable?.trim()?.length?.let { it <= 40 } ?: true) &&
            profil.conditionPaiementJours in 0..365 &&
            profil.contacts.all { contact ->
                nomEstValide(contact.nom) &&
                    (contact.telephone.isNullOrBlank() || telephoneEstValide(contact.telephone)) &&
                    emailEstValide(contact.email)
            } &&
            profil.addresses.all { address ->
                address.adresse.trim().length in 2..LONGUEUR_ADRESSE_MAX &&
                    address.libelle.trim().length <= 60 &&
                    (address.ville?.trim()?.length?.let { it <= 100 } ?: true)
            }

    fun coordonneesEtConditionsSontValides(
        nom: String,
        telephone: String,
        remiseDefautPct: Double,
        limiteCredit: Double?,
        email: String? = null,
        adresse: String? = null,
        notes: String? = null,
    ): Boolean = nomEstValide(nom) &&
        (telephone.isBlank() || telephoneEstValide(telephone)) &&
        (!telephone.isBlank() || !normaliseEmail(email).isNullOrBlank()) &&
        emailEstValide(email) &&
        adresseEstValide(adresse) &&
        notesSontValides(notes) &&
        remiseDefautPct in 0.0..100.0 &&
        (limiteCredit == null || limiteCredit >= 0.0)
}

/**
 * **RC-01** — Détection de doublons client : même téléphone OU nom proche.
 * Utilisée à la saisie du formulaire client (ClientFormScreen).
 */
class DetectDuplicateClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
) {
    /** @return les clients existants pouvant être des doublons (vides si aucun). */
    suspend operator fun invoke(telephone: String, nom: String) =
        clientDao.findDoublonsPotentiels(ClientValidation.normaliseTelephone(telephone), ClientValidation.normaliseNom(nom))
}

/**
 * **RC-05** — Limite de crédit : alerte puis validation Gérant/Propriétaire au-delà.
 * Logique pure, consommée par 9.6 Vente.
 */
class CheckCreditLimitUseCase @Inject constructor() {
    enum class Verdict { AUTORISE, ALERTE, VALIDATION_REQUISE, BLOQUE }

    operator fun invoke(
        soldeActuel: Double,
        montantNouvelleVente: Double,
        limiteCredit: Double?,
    ): Verdict {
        if (!soldeActuel.isFinite() || soldeActuel < 0.0 || !montantNouvelleVente.isFinite() ||
            montantNouvelleVente < 0.0 || (limiteCredit != null && (!limiteCredit.isFinite() || limiteCredit < 0.0))
        ) return Verdict.BLOQUE
        if (limiteCredit == null) return Verdict.AUTORISE // null = illimitée
        val futur = soldeActuel + montantNouvelleVente
        if (!futur.isFinite()) return Verdict.BLOQUE
        return when {
            futur <= limiteCredit -> Verdict.AUTORISE
            futur <= limiteCredit * 1.10 -> Verdict.ALERTE
            else -> Verdict.VALIDATION_REQUISE
        }
    }
}

/**
 * Création d'un client (module 9.9) — orchestre RC-01 (doublons), la numérotation
 * `CLI-2026-0001` (RA-09 via SequenceManager), la licence (RA-05 lecture si expirée)
 * et le journal (RA-18). Aucune donnée de démo : création uniquement sur saisie réelle.
 */
class CreateClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class Result {
        data class Succes(val clientId: Long, val code: String) : Result()
        data object LicenceExpiree : Result() // RA-05 : lecture seule
        data object PermissionRefusee : Result()
        data object DoublonPotentiel : Result() // RC-01 : confirmation requise
        data object NomObligatoire : Result()
        data object NomInvalide : Result()
        data object TelephoneObligatoire : Result()
        data object TelephoneInvalide : Result()
        data object EmailInvalide : Result()
        data object DonneesInvalides : Result()
    }

    suspend operator fun invoke(
        nom: String,
        telephone: String,
        type: ClientType = ClientType.PARTICULIER,
        email: String? = null,
        adresse: String? = null,
        categorieId: Long? = null,
        siteId: Long? = null,
        remiseDefautPct: Double = 0.0,
        limiteCredit: Double? = null,
        badgeId: Long? = null,
        notes: String? = null,
        profile: ClientProfileInput = ClientProfileInput(),
        doublonConfirme: Boolean = false,
        /** BROUILLON (fiche complète à activer) ou A_COMPLETER (création rapide nom + téléphone). */
        statutInitial: ClientStatus = ClientStatus.BROUILLON,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (statutInitial != ClientStatus.BROUILLON && statutInitial != ClientStatus.A_COMPLETER) {
            return Result.DonneesInvalides
        }
        val nomNormalise = ClientValidation.normaliseNom(nom)
        val telephoneNormalise = ClientValidation.normaliseTelephone(telephone)
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (!permissionGate.autorise(PermissionChecker.Action.CREATE)) return Result.PermissionRefusee
        val saisieValide = ClientValidation.coordonneesEtConditionsSontValides(
            nom = nom,
            telephone = telephone,
            remiseDefautPct = remiseDefautPct,
            limiteCredit = limiteCredit,
            email = email,
            adresse = adresse,
            notes = notes,
        ) && ClientValidation.profilEstValide(profile)
        if (!saisieValide) {
            return when {
                nomNormalise.isEmpty() -> Result.NomObligatoire
                !ClientValidation.nomEstValide(nom) -> Result.NomInvalide
                telephoneNormalise.isEmpty() && ClientValidation.normaliseEmail(email).isNullOrBlank() -> Result.TelephoneObligatoire
                telephoneNormalise.isNotEmpty() && !ClientValidation.telephoneEstValide(telephone) -> Result.TelephoneInvalide
                !ClientValidation.emailEstValide(email) -> Result.EmailInvalide
                else -> Result.DonneesInvalides
            }
        }

        // RC-01 — doublons : confirmation obligatoire si détectés
        if (!doublonConfirme) {
            val doublons = clientDao.findDoublonsPotentiels(telephoneNormalise, nomNormalise)
            if (doublons.isNotEmpty()) return Result.DoublonPotentiel
        }

        val code = sequenceManager.next(DocType.CLIENT)
        val id = clientDao.insertClientProfile(
            client = ClientEntity(
                code = code,
                nom = nomNormalise,
                type = type,
                telephone = telephoneNormalise,
                email = ClientValidation.normaliseEmail(email),
                adresse = ClientValidation.normaliseTexte(adresse),
                nif = ClientValidation.normaliseTexte(profile.nif),
                typeIdentifiantFiscal = ClientValidation.normaliseTexte(profile.typeIdentifiantFiscal),
                numeroTva = ClientValidation.normaliseTexte(profile.numeroTva),
                assujettiTva = profile.assujettiTva,
                exonereTva = profile.exonereTva,
                motifExoneration = ClientValidation.normaliseTexte(profile.motifExoneration),
                tauxTva = profile.tauxTva,
                commercial = ClientValidation.normaliseTexte(profile.commercial),
                conditionPaiementJours = profile.conditionPaiementJours,
                conditionsPaiement = ClientValidation.normaliseTexte(profile.conditionsPaiement),
                grilleTarifaire = ClientValidation.normaliseTexte(profile.grilleTarifaire),
                remiseMaxPct = profile.remiseMaxPct,
                segment = ClientValidation.normaliseTexte(profile.segment),
                canalVente = ClientValidation.normaliseTexte(profile.canalVente),
                territoire = ClientValidation.normaliseTexte(profile.territoire),
                compteComptable = ClientValidation.normaliseTexte(profile.compteComptable),
                categorieId = categorieId,
                siteId = siteId,
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                badgeId = badgeId,
                notes = ClientValidation.normaliseTexte(notes),
                // Création toujours non transactionnelle : l'activation est un acte séparé.
                statut = statutInitial,
                prospect = type == ClientType.PROSPECT,
                createdAt = now,
                active = false,
            ),
            contacts = profile.contacts,
            addresses = profile.addresses,
        )
        journalManager.log("CLIENTS", "CREATION_CLIENT", "Client $code — $nomNormalise")
        return Result.Succes(id, code)
    }
}
/** Détermination du taux facturé à partir du profil fiscal du client. */
object ClientTaxRules {
    fun effectiveRate(assujetti: Boolean, exonere: Boolean, tauxSpecifique: Double?, tauxStandard: Double): Double {
        if (!assujetti || exonere) return 0.0
        val taux = tauxSpecifique ?: tauxStandard
        return taux.takeIf { it.isFinite() && it in 0.0..100.0 } ?: 0.0
    }
}

/** Règles pures du cycle de vie client, utilisées aussi par les tests. */
object ClientLifecycleRules {
    private val typesAvecIdentifiantFiscal = setOf(
        ClientType.ENTREPRISE,
        ClientType.ADMINISTRATION,
        ClientType.REVENDEUR,
        ClientType.GROSSISTE,
        ClientType.DISTRIBUTEUR,
        ClientType.CLIENT_EXPORT,
        ClientType.CLIENT_PROJET,
    )

    fun informationsFiscalesRequises(type: ClientType): Boolean = type in typesAvecIdentifiantFiscal

    fun peutActiver(client: ClientEntity): Boolean =
        client.statut in setOf(ClientStatus.BROUILLON, ClientStatus.A_COMPLETER, ClientStatus.INACTIF, ClientStatus.DESACTIVE) &&
            ClientValidation.nomEstValide(client.nom) &&
            (ClientValidation.telephoneEstValide(client.telephone) ||
                (!client.email.isNullOrBlank() && ClientValidation.emailEstValide(client.email))) &&
            (client.email.isNullOrBlank() || ClientValidation.emailEstValide(client.email)) &&
            (!informationsFiscalesRequises(client.type) ||
                (!client.nif.isNullOrBlank() && !client.adresse.isNullOrBlank()))

    /**
     * Table explicite des changements de statut autorisés (source unique, testée).
     * `DESACTIVE` n'existe que pour lire d'anciens enregistrements : on peut en sortir, pas y entrer.
     * `ARCHIVE` n'est jamais une suppression : on peut restaurer un client archivé en `INACTIF`.
     */
    val transitions: Map<ClientStatus, Set<ClientStatus>> = mapOf(
        ClientStatus.BROUILLON to setOf(ClientStatus.A_COMPLETER, ClientStatus.ACTIF, ClientStatus.ARCHIVE),
        ClientStatus.A_COMPLETER to setOf(ClientStatus.ACTIF, ClientStatus.INACTIF, ClientStatus.ARCHIVE),
        ClientStatus.ACTIF to setOf(
            ClientStatus.SOUS_SURVEILLANCE, ClientStatus.BLOQUE_CREDIT,
            ClientStatus.BLOQUE_ADMINISTRATIF, ClientStatus.INACTIF,
        ),
        ClientStatus.SOUS_SURVEILLANCE to setOf(
            ClientStatus.ACTIF, ClientStatus.BLOQUE_CREDIT,
            ClientStatus.BLOQUE_ADMINISTRATIF, ClientStatus.INACTIF,
        ),
        ClientStatus.BLOQUE_CREDIT to setOf(
            ClientStatus.ACTIF, ClientStatus.SOUS_SURVEILLANCE,
            ClientStatus.BLOQUE_ADMINISTRATIF, ClientStatus.INACTIF,
        ),
        ClientStatus.BLOQUE_ADMINISTRATIF to setOf(
            ClientStatus.ACTIF, ClientStatus.SOUS_SURVEILLANCE,
            ClientStatus.BLOQUE_CREDIT, ClientStatus.INACTIF,
        ),
        ClientStatus.INACTIF to setOf(ClientStatus.ACTIF, ClientStatus.ARCHIVE),
        ClientStatus.DESACTIVE to setOf(ClientStatus.ACTIF, ClientStatus.INACTIF, ClientStatus.ARCHIVE),
        ClientStatus.ARCHIVE to setOf(ClientStatus.INACTIF),
    )

    fun transitionsDepuis(statut: ClientStatus): Set<ClientStatus> = transitions[statut].orEmpty()

    /** Les passages vers un blocage, l'inactivité ou l'archive exigent une confirmation explicite. */
    fun confirmationRequise(vers: ClientStatus): Boolean = vers in setOf(
        ClientStatus.BLOQUE_CREDIT, ClientStatus.BLOQUE_ADMINISTRATIF,
        ClientStatus.INACTIF, ClientStatus.ARCHIVE,
    )

    /**
     * Le changement est-il permis pour ce client ? Table de transitions, puis conditions de fond :
     * une réactivation depuis un état non opérationnel passe par [peutActiver].
     */
    fun peutTransiter(client: ClientEntity, vers: ClientStatus): Boolean {
        if (vers !in transitionsDepuis(client.statut)) return false
        val reactivation = vers == ClientStatus.ACTIF && client.statut in setOf(
            ClientStatus.BROUILLON, ClientStatus.A_COMPLETER, ClientStatus.INACTIF, ClientStatus.DESACTIVE,
        )
        return !reactivation || peutActiver(client)
    }

    /** Les brouillons et comptes bloqués crédit ne peuvent porter une créance. */
    fun venteAutorisee(client: ClientEntity, montant: Double, regle: Double): Boolean {
        if (!montant.isFinite() || montant <= 0.0 || !regle.isFinite() || regle < 0.0 || regle > montant + 1e-9) return false
        return when (client.statut) {
            ClientStatus.ACTIF -> client.active
            ClientStatus.BROUILLON, ClientStatus.A_COMPLETER -> !client.active && regle >= montant - 1e-9
            ClientStatus.BLOQUE_CREDIT, ClientStatus.SOUS_SURVEILLANCE -> client.active && regle >= montant - 1e-9
            else -> false
        }
    }
}

/** Activation explicite après vérification des coordonnées et éléments requis. */
class ActiverClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class Result {
        data object Succes : Result()
        data object Introuvable : Result()
        data object CoordonneesManquantes : Result()
        data object InformationsFiscalesManquantes : Result()
        data object LicenceExpiree : Result()
        data object PermissionRefusee : Result()
    }

    suspend operator fun invoke(id: Long): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (!permissionGate.autorise(PermissionChecker.Action.VALIDATE)) return Result.PermissionRefusee
        val client = clientDao.getById(id) ?: return Result.Introuvable
        if (!ClientLifecycleRules.peutActiver(client)) {
            val infosFiscalesManquantes = ClientLifecycleRules.informationsFiscalesRequises(client.type) &&
                (client.nif.isNullOrBlank() || client.adresse.isNullOrBlank())
            return if (infosFiscalesManquantes) Result.InformationsFiscalesManquantes
            else Result.CoordonneesManquantes
        }
        clientDao.update(client.copy(statut = ClientStatus.ACTIF, active = true))
        journalManager.log("CLIENTS", "ACTIVATION_CLIENT", "Client ${client.code} activé")
        return Result.Succes
    }
}

/** Édition d'un client existant (jamais de suppression physique — C7). */
class UpdateClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    suspend operator fun invoke(
        id: Long,
        nom: String,
        telephone: String,
        type: ClientType,
        email: String? = null,
        adresse: String? = null,
        categorieId: Long? = null,
        siteId: Long? = null,
        remiseDefautPct: Double = 0.0,
        limiteCredit: Double? = null,
        badgeId: Long? = null,
        notes: String? = null,
        /** Null préserve les relations existantes, pour compatibilité avec les anciens formulaires. */
        profile: ClientProfileInput? = null,
    ): Boolean {
        val nomNormalise = ClientValidation.normaliseNom(nom)
        val telephoneNormalise = ClientValidation.normaliseTelephone(telephone)
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT)) return false
        val saisieValide = ClientValidation.coordonneesEtConditionsSontValides(
            nom = nom,
            telephone = telephone,
            remiseDefautPct = remiseDefautPct,
            limiteCredit = limiteCredit,
            email = email,
            adresse = adresse,
            notes = notes,
        ) && (profile?.let(ClientValidation::profilEstValide) ?: true)
        if (!saisieValide) return false
        val existant = clientDao.getById(id) ?: return false
        val clientMisAJour = existant.copy(
            nom = nomNormalise,
            telephone = telephoneNormalise,
            type = type,
            email = ClientValidation.normaliseEmail(email),
            adresse = ClientValidation.normaliseTexte(adresse),
            nif = if (profile == null) existant.nif else ClientValidation.normaliseTexte(profile.nif),
            typeIdentifiantFiscal = if (profile == null) existant.typeIdentifiantFiscal else ClientValidation.normaliseTexte(profile.typeIdentifiantFiscal),
            numeroTva = if (profile == null) existant.numeroTva else ClientValidation.normaliseTexte(profile.numeroTva),
            assujettiTva = profile?.assujettiTva ?: existant.assujettiTva,
            exonereTva = profile?.exonereTva ?: existant.exonereTva,
            motifExoneration = if (profile == null) existant.motifExoneration else ClientValidation.normaliseTexte(profile.motifExoneration),
            tauxTva = if (profile == null) existant.tauxTva else profile.tauxTva,
            commercial = if (profile == null) existant.commercial else ClientValidation.normaliseTexte(profile.commercial),
            conditionPaiementJours = profile?.conditionPaiementJours ?: existant.conditionPaiementJours,
            conditionsPaiement = if (profile == null) existant.conditionsPaiement else ClientValidation.normaliseTexte(profile.conditionsPaiement),
            grilleTarifaire = if (profile == null) existant.grilleTarifaire else ClientValidation.normaliseTexte(profile.grilleTarifaire),
            remiseMaxPct = if (profile == null) existant.remiseMaxPct else profile.remiseMaxPct,
            segment = if (profile == null) existant.segment else ClientValidation.normaliseTexte(profile.segment),
            canalVente = if (profile == null) existant.canalVente else ClientValidation.normaliseTexte(profile.canalVente),
            territoire = if (profile == null) existant.territoire else ClientValidation.normaliseTexte(profile.territoire),
            compteComptable = if (profile == null) existant.compteComptable else ClientValidation.normaliseTexte(profile.compteComptable),
            categorieId = categorieId,
            siteId = siteId,
            remiseDefautPct = remiseDefautPct,
            limiteCredit = limiteCredit,
            badgeId = badgeId,
            notes = ClientValidation.normaliseTexte(notes),
        )
        if (profile == null || !profile.replaceRelations) {
            clientDao.update(clientMisAJour)
        } else {
            clientDao.updateClientProfile(
                client = clientMisAJour,
                contacts = profile.contacts,
                addresses = profile.addresses,
            )
        }
        journalManager.log("CLIENTS", "MODIFICATION_CLIENT", "Client ${existant.code} — $nomNormalise")
        return true
    }
}

/** Désactivation d'un client (RC-03 / C7 — jamais de DELETE). */
class DesactiverClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    suspend operator fun invoke(id: Long): Boolean {
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.DELETE)) return false
        val client = clientDao.getById(id) ?: return false
        clientDao.desactiver(id)
        journalManager.log("CLIENTS", "DESACTIVATION_CLIENT", "Client ${client.code} désactivé")
        return true
    }
}

/** Lecture de la liste des clients actifs + observables (module 9.2). */
class ObserveClientsUseCase @Inject constructor(
    private val clientDao: ClientDao,
) {
    operator fun invoke(): Flow<List<ClientEntity>> = clientDao.observeAll()
}

/** Liste pour l'administration client, incluant les comptes désactivés conservés en historique. */
class ObserveAllClientsUseCase @Inject constructor(
    private val clientDao: ClientDao,
) {
    operator fun invoke(): Flow<List<ClientEntity>> = clientDao.observeAllIncludingInactive()
}

/** Gestion des catégories de clients (suppression verrouillée si rattachée). */
class CategorieClientUseCases @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class SuppressionResult {
        data object Supprimee : SuppressionResult()
        data object CategorieUtilisee : SuppressionResult()
        data object LectureSeule : SuppressionResult()
        data object PermissionRefusee : SuppressionResult()
        data object Introuvable : SuppressionResult()
    }

    fun observer(): Flow<List<CategoryClientEntity>> = clientDao.observeCategories()

    /** @return l'identifiant créé, ou null si l'écriture est interdite/invalide. */
    suspend fun creer(nom: String): Long? {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.CREATE) || !ClientValidation.nomEstValide(nom)) return null
        val id = clientDao.insertCategorie(CategoryClientEntity(nom = nomNormalise))
        journalManager.log("CLIENTS", "CATEGORIE_CREEE", "Catégorie client : $nomNormalise")
        return id
    }

    suspend fun renommer(id: Long, nom: String): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT) || !ClientValidation.nomEstValide(nom)) return false
        val cat = clientDao.getCategorieById(id) ?: return false
        clientDao.updateCategorie(cat.copy(nom = nomNormalise))
        journalManager.log("CLIENTS", "CATEGORIE_MODIFIEE", "Catégorie -> $nomNormalise")
        return true
    }

    /** Renvoie précisément pourquoi une suppression ne peut pas être effectuée. */
    suspend fun supprimer(id: Long): SuppressionResult {
        if (licenceManager.isReadOnly()) return SuppressionResult.LectureSeule
        if (!permissionGate.autorise(PermissionChecker.Action.DELETE)) return SuppressionResult.PermissionRefusee
        if (clientDao.getCategorieById(id) == null) return SuppressionResult.Introuvable
        if (clientDao.countClientsAvecCategorie(id) > 0) return SuppressionResult.CategorieUtilisee
        clientDao.deleteCategorie(id)
        journalManager.log("CLIENTS", "CATEGORIE_SUPPRIMEE", "Catégorie id=$id supprimée")
        return SuppressionResult.Supprimee
    }
}

/** Gestion des badges de fidélité (RC-16, remise automatique à la vente). */
class BadgeLoyaltyUseCases @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    fun observer(): Flow<List<BadgeLoyaltyEntity>> = clientDao.observeBadges()

    /** @return l'identifiant créé, ou null si l'écriture est interdite/invalide. */
    suspend fun creer(nom: String, remisePct: Double): Long? {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.CREATE) ||
            !ClientValidation.nomEstValide(nom) || !remisePct.isFinite() || remisePct !in 0.0..100.0
        ) {
            return null
        }
        val id = clientDao.insertBadge(BadgeLoyaltyEntity(nom = nomNormalise, remisePct = remisePct))
        journalManager.log("CLIENTS", "BADGE_CREE", "Badge fidélité : $nomNormalise ($remisePct%)")
        return id
    }

    suspend fun modifier(id: Long, nom: String, remisePct: Double, actif: Boolean = true): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT) ||
            !ClientValidation.nomEstValide(nom) || !remisePct.isFinite() || remisePct !in 0.0..100.0
        ) {
            return false
        }
        val badge = clientDao.getBadgeById(id) ?: return false
        clientDao.updateBadge(badge.copy(nom = nomNormalise, remisePct = remisePct, actif = actif))
        journalManager.log("CLIENTS", "BADGE_MODIFIE", "Badge fidélité : $nomNormalise")
        return true
    }
}


/** Grille de prix négociés client × produit : seule Clients possède ces règles. */
class ClientPriceUseCases @Inject constructor(
    private val clientDao: ClientDao,
    private val productDao: ProductDao,
    private val licenceManager: LicenceManager,
    private val permissionGate: ClientPermissionGate,
    private val journalManager: JournalManager,
) {
    fun observer(clientId: Long): Flow<List<com.missa.b360.core.data.entity.PriceClientEntity>> =
        clientDao.observePrix(clientId)

    suspend fun definir(clientId: Long, productId: Long, prix: Double): Boolean {
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT) ||
            clientId <= 0 || productId <= 0 || !prix.isFinite() || prix <= 0.0
        ) return false
        val client = clientDao.getById(clientId) ?: return false
        val product = productDao.getById(productId) ?: return false
        if (!product.active || !product.vendable || !ProduitRules.estVendable(product.type)) return false
        clientDao.upsertPrix(com.missa.b360.core.data.entity.PriceClientEntity(
            clientId = clientId,
            produitId = productId,
            prix = prix,
        ))
        journalManager.log("CLIENTS", "PRIX_NEGOCIE_MODIFIE", "Client ${client.code} — produit ${product.code}")
        return true
    }

    suspend fun supprimer(clientId: Long, productId: Long): Boolean {
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT) ||
            clientDao.getById(clientId) == null
        ) return false
        clientDao.deletePrix(clientId, productId)
        journalManager.log("CLIENTS", "PRIX_NEGOCIE_SUPPRIME", "Client id=$clientId — produit id=$productId")
        return true
    }
}

/** Accès au profil détaillé client sans exposer le DAO à l'interface Compose. */
class ClientProfileUseCase @Inject constructor(
    private val clientDao: ClientDao,
) {
    fun observeContacts(clientId: Long): Flow<List<ClientContactEntity>> = clientDao.observeContacts(clientId)

    fun observeAddresses(clientId: Long): Flow<List<ClientAddressEntity>> = clientDao.observeAddresses(clientId)

    fun observePrices(clientId: Long): Flow<List<com.missa.b360.core.data.entity.PriceClientEntity>> = clientDao.observePrix(clientId)
}
