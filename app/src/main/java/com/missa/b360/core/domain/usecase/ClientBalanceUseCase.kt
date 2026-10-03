package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.R
import com.missa.b360.core.domain.model.ClientBalanceRules
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.domain.model.ClientBalanceSnapshot
import com.missa.b360.core.domain.model.ClientLedgerItem
import com.missa.b360.core.domain.model.ClientPaymentItem
import javax.inject.Inject

/**
 * Tient `client_balances` à jour : une seule source pour la liste, la fiche, le compte et les
 * relances. À appeler après chaque vente validée, encaissement, avoir ou annulation.
 *
 * [recalculer] n'ouvre pas de transaction : l'appelant l'invoque dans la sienne, pour que la
 * pièce et le compte soient écrits ensemble. [reconstruireTout] est autonome.
 */
class ClientBalanceUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val clientDao: ClientDao,
    private val balanceDao: ClientBalanceDao,
    private val paymentDao: ClientPaymentDao,
    private val database: AppDatabase,
    private val appNotifier: AppNotifier,
) {
    /** Recalcule le compte d'un client existant ; sans effet pour une fiche absente (clé étrangère). */
    suspend fun recalculer(clientId: Long, now: Long = System.currentTimeMillis()): ClientBalanceEntity? {
        if (clientId <= 0L) return null
        val client = clientDao.getById(clientId) ?: return null
        val ledger = ClientBalanceRules.ledgerParClient(operationDao.getVentesValideesPourClient(clientId))[clientId].orEmpty()
        val paiements = paymentDao.getByClient(clientId).map { it.enItem() }
        val avant = balanceDao.get(clientId)?.encours ?: 0.0
        val compte = construire(client, ledger, paiements, now)
        balanceDao.upsert(compte)
        if (limiteFranchie(client.limiteCredit, avant, compte.encours)) {
            appNotifier.notifier(
                type = "CLIENT_LIMITE",
                titreRes = R.string.cli_notif_limite_titre,
                message = "${client.code} — ${client.nom}",
                date = now,
            )
        }
        return compte
    }

    /** Reconstruit tous les comptes depuis les pièces (lecture unique, une seule transaction). */
    suspend fun reconstruireTout(now: Long = System.currentTimeMillis()): Int = database.withTransaction {
        val ledgers = ClientBalanceRules.ledgerParClient(operationDao.getVentesValidees())
        val paiements = paymentDao.getAll().groupBy({ it.clientId }, { it.enItem() })
        val comptes = clientDao.getAll().map { client ->
            construire(client, ledgers[client.id].orEmpty(), paiements[client.id].orEmpty(), now)
        }
        balanceDao.upsertAll(comptes)
        comptes.size
    }

    /** Recalcule les clients qui ont un encours : leurs retards avancent avec le calendrier. */
    suspend fun rafraichirEcheances(now: Long = System.currentTimeMillis()): Int {
        var traites = 0
        for (id in balanceDao.clientIdsAvecEncours()) {
            database.withTransaction { recalculer(id, now) }
            traites++
        }
        return traites
    }

    /** Premier lancement après la migration : reconstruit si des clients n'ont pas encore de compte. */
    suspend fun assurerInitialisation(now: Long = System.currentTimeMillis()): Boolean {
        if (balanceDao.count() >= clientDao.count()) return false
        reconstruireTout(now)
        return true
    }

    private fun construire(
        client: ClientEntity,
        ledger: List<ClientLedgerItem>,
        paiements: List<ClientPaymentItem>,
        now: Long,
    ): ClientBalanceEntity = ClientBalanceRules.calculer(ledger, paiements, client.conditionPaiementJours, now)
        .enEntite(client.id, now)

    /** Vrai quand l'encours passe de sous la limite à la limite atteinte ou dépassée (une seule alerte par franchissement). */
    private fun limiteFranchie(limite: Double?, avant: Double, apres: Double): Boolean {
        if (limite == null || !limite.isFinite() || limite <= 0.0) return false
        return avant < limite && apres >= limite
    }

    private fun ClientPaymentEntity.enItem() = ClientPaymentItem(montant = montant, invoiceRecordId = invoiceRecordId)

    private fun ClientBalanceSnapshot.enEntite(clientId: Long, now: Long) = ClientBalanceEntity(
        clientId = clientId,
        encours = encours,
        enRetard = enRetard,
        joursRetardMax = joursRetardMax,
        ca12Mois = ca12Mois,
        derniereVenteAt = derniereVenteAt,
        nbVentes = nbVentes,
        majAt = now,
    )
}
