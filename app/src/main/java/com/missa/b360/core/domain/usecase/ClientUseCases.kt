package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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

    fun coordonneesEtConditionsSontValides(
        nom: String,
        telephone: String,
        remiseDefautPct: Double,
        limiteCredit: Double?,
        email: String? = null,
        adresse: String? = null,
        notes: String? = null,
    ): Boolean = nomEstValide(nom) &&
        telephoneEstValide(telephone) &&
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
    ): Verdict = when {
        limiteCredit == null -> Verdict.AUTORISE // null = illimitée
        soldeActuel + montantNouvelleVente <= limiteCredit -> Verdict.AUTORISE
        soldeActuel + montantNouvelleVente <= limiteCredit * 1.10 -> Verdict.ALERTE
        else -> Verdict.VALIDATION_REQUISE
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
) {
    sealed class Result {
        data class Succes(val clientId: Long, val code: String) : Result()
        data object LicenceExpiree : Result() // RA-05 : lecture seule
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
        remiseDefautPct: Double = 0.0,
        limiteCredit: Double? = null,
        badgeId: Long? = null,
        notes: String? = null,
        doublonConfirme: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val nomNormalise = ClientValidation.normaliseNom(nom)
        val telephoneNormalise = ClientValidation.normaliseTelephone(telephone)
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        val saisieValide = ClientValidation.coordonneesEtConditionsSontValides(
            nom = nom,
            telephone = telephone,
            remiseDefautPct = remiseDefautPct,
            limiteCredit = limiteCredit,
            email = email,
            adresse = adresse,
            notes = notes,
        )
        if (!saisieValide) {
            return when {
                nomNormalise.isEmpty() -> Result.NomObligatoire
                !ClientValidation.nomEstValide(nom) -> Result.NomInvalide
                telephoneNormalise.isEmpty() -> Result.TelephoneObligatoire
                !ClientValidation.telephoneEstValide(telephone) -> Result.TelephoneInvalide
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
        val id = clientDao.insert(
            ClientEntity(
                code = code,
                nom = nomNormalise,
                type = type,
                telephone = telephoneNormalise,
                email = ClientValidation.normaliseEmail(email),
                adresse = ClientValidation.normaliseTexte(adresse),
                categorieId = categorieId,
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                badgeId = badgeId,
                notes = ClientValidation.normaliseTexte(notes),
                statut = ClientStatus.ACTIF,
                prospect = type == ClientType.PROSPECT,
                createdAt = now,
            ),
        )
        journalManager.log("CLIENTS", "CREATION_CLIENT", "Client $code — $nomNormalise")
        return Result.Succes(id, code)
    }
}
/** Édition d'un client existant (jamais de suppression physique — C7). */
class UpdateClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(
        id: Long,
        nom: String,
        telephone: String,
        type: ClientType,
        email: String? = null,
        adresse: String? = null,
        categorieId: Long? = null,
        remiseDefautPct: Double = 0.0,
        limiteCredit: Double? = null,
        badgeId: Long? = null,
        notes: String? = null,
    ): Boolean {
        val nomNormalise = ClientValidation.normaliseNom(nom)
        val telephoneNormalise = ClientValidation.normaliseTelephone(telephone)
        if (licenceManager.isReadOnly()) return false
        val saisieValide = ClientValidation.coordonneesEtConditionsSontValides(
            nom = nom,
            telephone = telephone,
            remiseDefautPct = remiseDefautPct,
            limiteCredit = limiteCredit,
            email = email,
            adresse = adresse,
            notes = notes,
        )
        if (!saisieValide) return false
        val existant = clientDao.getById(id) ?: return false
        clientDao.update(
            existant.copy(
                nom = nomNormalise,
                telephone = telephoneNormalise,
                type = type,
                email = ClientValidation.normaliseEmail(email),
                adresse = ClientValidation.normaliseTexte(adresse),
                categorieId = categorieId,
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                badgeId = badgeId,
                notes = ClientValidation.normaliseTexte(notes),
            ),
        )
        journalManager.log("CLIENTS", "MODIFICATION_CLIENT", "Client ${existant.code} — $nomNormalise")
        return true
    }
}

/** Désactivation d'un client (RC-03 / C7 — jamais de DELETE). */
class DesactiverClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(id: Long): Boolean {
        if (licenceManager.isReadOnly()) return false
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

/** Gestion des catégories de clients (suppression verrouillée si rattachée). */
class CategorieClientUseCases @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class SuppressionResult {
        data object Supprimee : SuppressionResult()
        data object CategorieUtilisee : SuppressionResult()
        data object LectureSeule : SuppressionResult()
        data object Introuvable : SuppressionResult()
    }

    fun observer(): Flow<List<CategoryClientEntity>> = clientDao.observeCategories()

    /** @return l'identifiant créé, ou null si l'écriture est interdite/invalide. */
    suspend fun creer(nom: String): Long? {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !ClientValidation.nomEstValide(nom)) return null
        val id = clientDao.insertCategorie(CategoryClientEntity(nom = nomNormalise))
        journalManager.log("CLIENTS", "CATEGORIE_CREEE", "Catégorie client : $nomNormalise")
        return id
    }

    suspend fun renommer(id: Long, nom: String): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !ClientValidation.nomEstValide(nom)) return false
        val cat = clientDao.getCategorieById(id) ?: return false
        clientDao.updateCategorie(cat.copy(nom = nomNormalise))
        journalManager.log("CLIENTS", "CATEGORIE_MODIFIEE", "Catégorie -> $nomNormalise")
        return true
    }

    /** Renvoie précisément pourquoi une suppression ne peut pas être effectuée. */
    suspend fun supprimer(id: Long): SuppressionResult {
        if (licenceManager.isReadOnly()) return SuppressionResult.LectureSeule
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
) {
    fun observer(): Flow<List<BadgeLoyaltyEntity>> = clientDao.observeBadges()

    /** @return l'identifiant créé, ou null si l'écriture est interdite/invalide. */
    suspend fun creer(nom: String, remisePct: Double): Long? {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !ClientValidation.nomEstValide(nom) || remisePct !in 0.0..100.0) {
            return null
        }
        val id = clientDao.insertBadge(BadgeLoyaltyEntity(nom = nomNormalise, remisePct = remisePct))
        journalManager.log("CLIENTS", "BADGE_CREE", "Badge fidélité : $nomNormalise ($remisePct%)")
        return id
    }

    suspend fun modifier(id: Long, nom: String, remisePct: Double, actif: Boolean = true): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !ClientValidation.nomEstValide(nom) || remisePct !in 0.0..100.0) {
            return false
        }
        val badge = clientDao.getBadgeById(id) ?: return false
        clientDao.updateBadge(badge.copy(nom = nomNormalise, remisePct = remisePct, actif = actif))
        journalManager.log("CLIENTS", "BADGE_MODIFIE", "Badge fidélité : $nomNormalise")
        return true
    }
}
