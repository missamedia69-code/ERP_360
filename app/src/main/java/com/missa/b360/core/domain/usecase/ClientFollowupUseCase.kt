package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.FollowupChannel
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.domain.model.ClientFollowupRules
import com.missa.b360.core.domain.model.PromiseCheck
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.permissions.PermissionChecker
import java.util.Calendar
import javax.inject.Inject

/**
 * Journal de suivi d'un client : relances, appels, promesses de paiement et notes.
 * Une entrée n'est jamais supprimée ; seule la promesse change de statut (tenue / non tenue / close).
 */
class ClientFollowupUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val followupDao: ClientFollowupDao,
    private val paymentDao: ClientPaymentDao,
    private val balanceDao: ClientBalanceDao,
    private val licenceManager: LicenceManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class Result {
        data class Succes(val id: Long) : Result()
        data object ClientIntrouvable : Result()
        data object LicenceExpiree : Result()
        data object PermissionRefusee : Result()
        data object DonneesInvalides : Result()
        data class PromesseInvalide(val controle: PromiseCheck) : Result()
    }

    suspend fun enregistrer(
        clientId: Long,
        type: FollowupType,
        canal: FollowupChannel? = null,
        message: String? = null,
        promesseDate: Long? = null,
        promesseMontant: Double? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (!permissionGate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionRefusee
        if (clientDao.getById(clientId) == null) return Result.ClientIntrouvable
        val texte = ClientValidation.normaliseTexte(message)
        if (texte != null && texte.length > LONGUEUR_MESSAGE_MAX) return Result.DonneesInvalides
        if (type == FollowupType.NOTE && texte == null) return Result.DonneesInvalides
        if (type != FollowupType.PROMESSE && (promesseDate != null || promesseMontant != null)) return Result.DonneesInvalides
        if (type == FollowupType.PROMESSE) {
            if (promesseDate == null || promesseMontant == null) return Result.DonneesInvalides
            val encours = balanceDao.get(clientId)?.encours ?: 0.0
            val controle = ClientFollowupRules.verifierPromesse(promesseMontant, promesseDate, encours, debutDeJournee(now))
            if (controle != PromiseCheck.VALIDE) return Result.PromesseInvalide(controle)
            // Une seule promesse ouverte par client : la précédente est close, jamais supprimée.
            followupDao.getPromessesOuvertes().filter { it.clientId == clientId }.forEach {
                followupDao.update(it.copy(statut = FollowupStatus.CLOS))
            }
        }
        val id = followupDao.insert(
            ClientFollowupEntity(
                clientId = clientId,
                type = type,
                canal = canal,
                message = texte,
                promesseDate = promesseDate,
                promesseMontant = promesseMontant,
                createdAt = now,
            ),
        )
        return Result.Succes(id)
    }

    /** Clôture manuelle d'une promesse encore ouverte. */
    suspend fun cloturer(id: Long): Boolean {
        if (licenceManager.isReadOnly() || !permissionGate.autorise(PermissionChecker.Action.EDIT)) return false
        val suivi = followupDao.getById(id) ?: return false
        if (suivi.type != FollowupType.PROMESSE || suivi.statut != FollowupStatus.OUVERT) return false
        followupDao.update(suivi.copy(statut = FollowupStatus.CLOS))
        return true
    }

    /**
     * Réévalue les promesses ouvertes (tenue dès que le montant est encaissé, non tenue le
     * lendemain de la date promise). Sans droit d'édition : c'est un calcul automatique.
     * Retourne le nombre de promesses dont le statut a changé.
     */
    suspend fun reevaluerPromesses(now: Long = System.currentTimeMillis()): Int {
        var modifiees = 0
        for (promesse in followupDao.getPromessesOuvertes()) {
            val date = promesse.promesseDate ?: continue
            val montant = promesse.promesseMontant ?: continue
            val encaisse = paymentDao.encaisseDepuis(promesse.clientId, promesse.createdAt)
            val statut = ClientFollowupRules.statutPromesse(promesse.statut, date, montant, encaisse, now)
            if (statut != promesse.statut) {
                followupDao.update(promesse.copy(statut = statut))
                modifiees++
            }
        }
        return modifiees
    }

    private fun debutDeJournee(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        const val LONGUEUR_MESSAGE_MAX = 500
    }
}
