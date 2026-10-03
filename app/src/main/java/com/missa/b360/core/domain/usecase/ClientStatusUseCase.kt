package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.permissions.PermissionChecker
import javax.inject.Inject

/**
 * Changement de statut d'un client (blocage, surveillance, désactivation, archivage, réactivation).
 * Ne s'appuie que sur la table de transitions de [ClientLifecycleRules] ; le blocage est toujours
 * une action manuelle (l'écran demande la confirmation), jamais une suppression.
 */
class ChangerStatutClientUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class Result {
        data object Succes : Result()
        data object Introuvable : Result()
        data object TransitionInterdite : Result()
        data object LicenceExpiree : Result()
        data object PermissionRefusee : Result()
    }

    suspend operator fun invoke(id: Long, vers: ClientStatus): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        val action = if (vers == ClientStatus.INACTIF || vers == ClientStatus.ARCHIVE) {
            PermissionChecker.Action.DELETE
        } else {
            PermissionChecker.Action.EDIT
        }
        if (!permissionGate.autorise(action)) return Result.PermissionRefusee
        val client = clientDao.getById(id) ?: return Result.Introuvable
        if (!ClientLifecycleRules.peutTransiter(client, vers)) return Result.TransitionInterdite
        clientDao.update(client.copy(statut = vers, active = estOperationnel(vers)))
        journalManager.log("CLIENTS", "STATUT_CLIENT", "Client ${client.code} : ${client.statut} → $vers")
        return Result.Succes
    }

    /** Les statuts qui laissent le client « en vie » commercialement (même bloqué) restent actifs. */
    private fun estOperationnel(statut: ClientStatus): Boolean = statut in setOf(
        ClientStatus.ACTIF, ClientStatus.SOUS_SURVEILLANCE,
        ClientStatus.BLOQUE_CREDIT, ClientStatus.BLOQUE_ADMINISTRATIF,
    )
}
