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

/**
 * **RC-01** — Détection de doublons client : même téléphone OU nom proche.
 * Utilisée à la saisie du formulaire client (ClientFormScreen).
 */
class DetectDuplicateClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
) {
    /** @return les clients existants pouvant être des doublons (vides si aucun). */
    suspend operator fun invoke(telephone: String, nom: String) =
        clientDao.findDoublonsPotentiels(telephone, nom)
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
        data object TelephoneObligatoire : Result()
    }

    suspend operator fun invoke(
        nom: String,
        telephone: String,
        type: ClientType = ClientType.PARTICULIER,
        remiseDefautPct: Double = 0.0,
        limiteCredit: Double? = null,
        doublonConfirme: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (telephone.isBlank()) return Result.TelephoneObligatoire

        // RC-01 — doublons : confirmation obligatoire si détectés
        if (!doublonConfirme) {
            val doublons = clientDao.findDoublonsPotentiels(telephone, nom)
            if (doublons.isNotEmpty()) return Result.DoublonPotentiel
        }

        val code = sequenceManager.next(DocType.CLIENT)
        val id = clientDao.insert(
            ClientEntity(
                code = code,
                nom = nom.trim(),
                type = type,
                telephone = telephone.trim(),
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                statut = ClientStatus.ACTIF,
                prospect = type == ClientType.PROSPECT,
                createdAt = now,
            ),
        )
        journalManager.log("CLIENTS", "CREATION_CLIENT", "Client $code — $nom")
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
        if (licenceManager.isReadOnly()) return false
        val existant = clientDao.getById(id) ?: return false
        clientDao.update(
            existant.copy(
                nom = nom.trim(),
                telephone = telephone.trim(),
                type = type,
                email = email?.trim()?.ifBlank { null },
                adresse = adresse?.trim()?.ifBlank { null },
                categorieId = categorieId,
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                badgeId = badgeId,
                notes = notes?.trim()?.ifBlank { null },
            ),
        )
        journalManager.log("CLIENTS", "MODIFICATION_CLIENT", "Client ${existant.code} — $nom")
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
    private val journalManager: JournalManager,
) {
    fun observer(): Flow<List<CategoryClientEntity>> = clientDao.observeCategories()

    suspend fun creer(nom: String): Long {
        val id = clientDao.insertCategorie(CategoryClientEntity(nom = nom.trim()))
        journalManager.log("CLIENTS", "CATEGORIE_CREEE", "Catégorie client : $nom")
        return id
    }

    suspend fun renommer(id: Long, nom: String) {
        val cat = clientDao.getCategorieById(id) ?: return
        clientDao.updateCategorie(cat.copy(nom = nom.trim()))
        journalManager.log("CLIENTS", "CATEGORIE_MODIFIEE", "Catégorie -> $nom")
    }

    /** @return false si la catégorie est rattachée à un client (suppression interdite). */
    suspend fun supprimer(id: Long): Boolean {
        if (clientDao.countClientsAvecCategorie(id) > 0) return false
        clientDao.deleteCategorie(id)
        journalManager.log("CLIENTS", "CATEGORIE_SUPPRIMEE", "Catégorie id=$id supprimée")
        return true
    }
}

/** Gestion des badges de fidélité (RC-16, remise automatique à la vente). */
class BadgeLoyaltyUseCases @Inject constructor(
    private val clientDao: ClientDao,
    private val journalManager: JournalManager,
) {
    fun observer(): Flow<List<BadgeLoyaltyEntity>> = clientDao.observeBadges()

    suspend fun creer(nom: String, remisePct: Double): Long {
        val id = clientDao.insertBadge(BadgeLoyaltyEntity(nom = nom.trim(), remisePct = remisePct))
        journalManager.log("CLIENTS", "BADGE_CREE", "Badge fidélité : $nom ($remisePct%)")
        return id
    }

    suspend fun modifier(id: Long, nom: String, remisePct: Double, actif: Boolean = true) {
        val badge = clientDao.getBadgeById(id) ?: return
        clientDao.updateBadge(badge.copy(nom = nom.trim(), remisePct = remisePct, actif = actif))
        journalManager.log("CLIENTS", "BADGE_MODIFIE", "Badge fidélité : $nom")
    }
}
