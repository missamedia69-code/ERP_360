package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.core.domain.model.AgedBalance
import com.missa.b360.core.domain.model.AgedBalanceRules
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.ClientBalanceRules
import com.missa.b360.core.domain.model.ClientPaymentItem
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.permissions.PermissionChecker
import javax.inject.Inject

/** Facture encore ouverte d'un client, prête à afficher (numéro, échéance, retard, tranche). */
data class ClientOpenInvoiceLine(
    val recordId: Long?,
    val reference: String,
    val issuedAt: Long,
    val dueAt: Long,
    val total: Double,
    val outstanding: Double,
    val joursRetard: Int,
    val tranche: AgingBucket,
)

/** Compte détaillé d'un client : factures ouvertes, balance âgée et encaissements. */
data class ClientAccount(
    val factures: List<ClientOpenInvoiceLine>,
    val balanceAgee: AgedBalance,
    val paiements: List<ClientPaymentEntity>,
)

/**
 * Détail du compte client. Les totaux viennent du même calcul que `client_balances`
 * ([ClientBalanceRules]) : la fiche, la liste et le compte ne peuvent pas diverger.
 */
class ClientAccountUseCase @Inject constructor(
    private val clientDao: ClientDao,
    private val operationDao: OperationRecordDao,
    private val paymentDao: ClientPaymentDao,
) {
    suspend operator fun invoke(clientId: Long, now: Long = System.currentTimeMillis()): ClientAccount? {
        val client = clientDao.getById(clientId) ?: return null
        val ventes = operationDao.getVentesValideesPourClient(clientId)
        val ledger = ClientBalanceRules.ledgerParClient(ventes)[clientId].orEmpty()
        val paiements = paymentDao.getByClient(clientId)
        val items = paiements.map { ClientPaymentItem(montant = it.montant, invoiceRecordId = it.invoiceRecordId) }
        val ouvertes = ClientBalanceRules.facturesOuvertes(ledger, items, client.conditionPaiementJours)
        val references = ventes.associate { it.id to it.reference }
        val lignes = ouvertes.map {
            ClientOpenInvoiceLine(
                recordId = it.recordId,
                reference = it.recordId?.let { id -> references[id] }.orEmpty(),
                issuedAt = it.issuedAt,
                dueAt = it.dueAt,
                total = it.total,
                outstanding = it.outstanding,
                joursRetard = AgedBalanceRules.joursDeRetard(it.dueAt, now),
                tranche = AgedBalanceRules.tranche(it.dueAt, now),
            )
        }.sortedBy { it.dueAt }
        return ClientAccount(lignes, AgedBalanceRules.calculer(ouvertes, now), paiements)
    }
}

/** Modes d'encaissement proposés (codes stockés tels quels). */
object ClientPaymentModes {
    const val ESPECES = "ESPECES"
    const val MOBILE_MONEY = "MOBILE_MONEY"
    const val VIREMENT = "VIREMENT"
    const val CHEQUE = "CHEQUE"
    const val AUTRE = "AUTRE"
    val TOUS = listOf(ESPECES, MOBILE_MONEY, VIREMENT, CHEQUE, AUTRE)
}

/**
 * Encaissement postérieur à la vente : lié à une facture ou imputé aux plus anciennes échéances.
 * Jamais supprimé (voir [ClientPaymentEntity]) ; le compte est recalculé dans la même transaction.
 */
class ClientPaymentUseCase @Inject constructor(
    private val database: AppDatabase,
    private val clientDao: ClientDao,
    private val operationDao: OperationRecordDao,
    private val paymentDao: ClientPaymentDao,
    private val balance: ClientBalanceUseCase,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val permissionGate: ClientPermissionGate,
) {
    sealed class Result {
        data class Succes(val paiementId: Long) : Result()
        data object ClientIntrouvable : Result()
        data object MontantInvalide : Result()
        data object MontantSuperieurEncours : Result()
        data object ModeInvalide : Result()
        data object LicenceExpiree : Result()
        data object PermissionRefusee : Result()
    }

    suspend fun encaisser(
        clientId: Long,
        montant: Double,
        modePaiement: String,
        invoiceRecordId: Long? = null,
        note: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        if (!permissionGate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionRefusee
        if (modePaiement !in ClientPaymentModes.TOUS) return Result.ModeInvalide
        if (!montant.isFinite() || montant <= 0.0) return Result.MontantInvalide
        val client = clientDao.getById(clientId) ?: return Result.ClientIntrouvable
        val texte = ClientValidation.normaliseTexte(note)?.take(240)
        return database.withTransaction {
            val ventes = operationDao.getVentesValideesPourClient(clientId)
            val ledger = ClientBalanceRules.ledgerParClient(ventes)[clientId].orEmpty()
            val items = paymentDao.getByClient(clientId).map { ClientPaymentItem(it.montant, it.invoiceRecordId) }
            val ouvertes = ClientBalanceRules.facturesOuvertes(ledger, items, client.conditionPaiementJours)
            val plafond = if (invoiceRecordId == null) {
                ouvertes.sumOf { it.outstanding }
            } else {
                ouvertes.firstOrNull { it.recordId == invoiceRecordId }?.outstanding ?: 0.0
            }
            if (montant > plafond + EPSILON) return@withTransaction Result.MontantSuperieurEncours
            val id = paymentDao.insert(
                ClientPaymentEntity(
                    clientId = clientId,
                    invoiceRecordId = invoiceRecordId,
                    montant = montant,
                    modePaiement = modePaiement,
                    reference = "ENC-$clientId-$now",
                    paiementAt = now,
                    note = texte,
                    createdAt = now,
                ),
            )
            balance.recalculer(clientId, now)
            journalManager.log("CLIENTS", "ENCAISSEMENT_CLIENT", "Client ${client.code} : encaissement $montant")
            Result.Succes(id)
        }
    }

    private companion object {
        const val EPSILON = 1e-6
    }
}
