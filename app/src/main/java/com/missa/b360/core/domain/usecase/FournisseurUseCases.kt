package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * **RF-01** — Détection de doublons fournisseur : même téléphone OU nom identique.
 */
class DetectDuplicateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
) {
    suspend operator fun invoke(telephone: String, nom: String) =
        fournisseurDao.findDoublonsPotentiels(telephone, nom)
}

/**
 * Création d'un fournisseur (module 9.3) — numérotation `FRN-2026-0001`,
 * doublons RF-01, licence (RA-05 lecture seule), journal (RA-18).
 */
class CreateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val fournisseurId: Long, val code: String) : Result()
        data object LicenceExpiree : Result()
        data object DoublonPotentiel : Result()
        data object TelephoneObligatoire : Result()
    }

    suspend operator fun invoke(
        nom: String,
        telephone: String,
        email: String? = null,
        adresse: String? = null,
        notes: String? = null,
        doublonConfirme: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (telephone.isBlank()) return Result.TelephoneObligatoire

        if (!doublonConfirme) {
            val doublons = fournisseurDao.findDoublonsPotentiels(telephone, nom)
            if (doublons.isNotEmpty()) return Result.DoublonPotentiel
        }

        val code = sequenceManager.next(DocType.FOURNISSEUR)
        val id = fournisseurDao.insert(
            FournisseurEntity(
                code = code,
                nom = nom.trim(),
                telephone = telephone.trim(),
                email = email?.trim()?.ifBlank { null },
                adresse = adresse?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                statut = FournisseurStatus.ACTIF,
                createdAt = now,
            ),
        )
        journalManager.log("FOURNISSEURS", "CREATION_FOURNISSEUR", "Fournisseur $code — $nom")
        return Result.Succes(id, code)
    }
}

/** Édition d'un fournisseur existant (jamais de suppression physique — C7). */
class UpdateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(
        id: Long,
        nom: String,
        telephone: String,
        email: String? = null,
        adresse: String? = null,
        notes: String? = null,
    ): Boolean {
        if (licenceManager.isReadOnly()) return false
        val existant = fournisseurDao.getById(id) ?: return false
        fournisseurDao.update(
            existant.copy(
                nom = nom.trim(),
                telephone = telephone.trim(),
                email = email?.trim()?.ifBlank { null },
                adresse = adresse?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
            ),
        )
        journalManager.log("FOURNISSEURS", "MODIFICATION_FOURNISSEUR", "Fournisseur ${existant.code} — $nom")
        return true
    }
}

/** Désactivation (C7 — jamais de DELETE). */
class DesactiverFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(id: Long): Boolean {
        if (licenceManager.isReadOnly()) return false
        val fournisseur = fournisseurDao.getById(id) ?: return false
        fournisseurDao.desactiver(id)
        journalManager.log("FOURNISSEURS", "DESACTIVATION_FOURNISSEUR", "Fournisseur ${fournisseur.code} désactivé")
        return true
    }
}

/** Lecture de la liste des fournisseurs actifs (module 9.3). */
class ObserveFournisseursUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
) {
    operator fun invoke(): Flow<List<FournisseurEntity>> = fournisseurDao.observeAll()
}