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
    fun normaliseNom(nom: String): String = nom.trim()

    fun normaliseTelephone(telephone: String): String = telephone.trim()

    fun normaliseTexte(texte: String?): String? = texte?.trim()?.ifBlank { null }

    fun coordonneesEtConditionsSontValides(
        nom: String,
        telephone: String,
        remiseDefautPct: Double,
        limiteCredit: Double?,
    ): Boolean = normaliseNom(nom).isNotEmpty() &&
        normaliseTelephone(telephone).isNotEmpty() &&
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
        data object TelephoneObligatoire : Result()
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
        if (!ClientValidation.coordonneesEtConditionsSontValides(nom, telephone, remiseDefautPct, limiteCredit)) {
            return when {
                nomNormalise.isEmpty() -> Result.NomObligatoire
                telephoneNormalise.isEmpty() -> Result.TelephoneObligatoire
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
                email = ClientValidation.normaliseTexte(email),
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
        if (!ClientValidation.coordonneesEtConditionsSontValides(nom, telephone, remiseDefautPct, limiteCredit)) {
            return false
        }
        val existant = clientDao.getById(id) ?: return false
        clientDao.update(
            existant.copy(
                nom = nomNormalise,
                telephone = telephoneNormalise,
                type = type,
                email = ClientValidation.normaliseTexte(email),
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
        if (licenceManager.isReadOnly() || nomNormalise.isEmpty()) return null
        val id = clientDao.insertCategorie(CategoryClientEntity(nom = nomNormalise))
        journalManager.log("CLIENTS", "CATEGORIE_CREEE", "Catégorie client : $nomNormalise")
        return id
    }

    suspend fun renommer(id: Long, nom: String): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || nomNormalise.isEmpty()) return false
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
        if (licenceManager.isReadOnly() || nomNormalise.isEmpty() || remisePct !in 0.0..100.0) {
            return null
        }
        val id = clientDao.insertBadge(BadgeLoyaltyEntity(nom = nomNormalise, remisePct = remisePct))
        journalManager.log("CLIENTS", "BADGE_CREE", "Badge fidélité : $nomNormalise ($remisePct%)")
        return id
    }

    suspend fun modifier(id: Long, nom: String, remisePct: Double, actif: Boolean = true): Boolean {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || nomNormalise.isEmpty() || remisePct !in 0.0..100.0) {
            return false
        }
        val badge = clientDao.getBadgeById(id) ?: return false
        clientDao.updateBadge(badge.copy(nom = nomNormalise, remisePct = remisePct, actif = actif))
        journalManager.log("CLIENTS", "BADGE_MODIFIE", "Badge fidélité : $nomNormalise")
        return true
    }
}
