package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.FournisseurBalanceDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurScoreDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.domain.model.FournisseurCacheRules
import com.missa.b360.core.journal.JournalManager
import javax.inject.Inject

/**
 * Tient `fournisseur_balances` à jour. La source de vérité est l'ensemble des pièces d'achat
 * validées : cette classe n'écrit que le résultat de [FournisseurCacheRules.balance].
 * Elle n'ouvre pas de transaction : l'appelant l'invoque dans la sienne.
 */
class FournisseurBalanceRefresher @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val fournisseurDao: FournisseurDao,
    private val balanceDao: FournisseurBalanceDao,
) {
    /** Recalcule la ligne d'un fournisseur existant ; sans effet pour une fiche absente (clé étrangère). */
    suspend fun rafraichir(fournisseurId: Long, now: Long = System.currentTimeMillis()): FournisseurBalanceEntity? {
        if (fournisseurId <= 0L) return null
        val fournisseur = fournisseurDao.getById(fournisseurId) ?: return null
        val pieces = operationDao.getByModule(OperationModule.ACHATS.name)
        val balance = FournisseurCacheRules.balance(fournisseur, pieces, now)
        balanceDao.upsert(balance)
        return balance
    }

    /** Reconstruit toutes les lignes depuis les pièces (une seule lecture des pièces). */
    suspend fun reconstruireTout(now: Long = System.currentTimeMillis()): Int {
        val pieces = operationDao.getByModule(OperationModule.ACHATS.name)
        val balances = fournisseurDao.getAll().map { FournisseurCacheRules.balance(it, pieces, now) }
        balanceDao.upsertAll(balances)
        return balances.size
    }

    /**
     * Recalcule tout, corrige les lignes dont les montants ou compteurs étaient faux et renvoie
     * l'identifiant de chacune avec sa valeur précédente (`null` si la ligne n'existait pas).
     */
    suspend fun verifierCoherence(now: Long = System.currentTimeMillis()): List<Pair<Long, FournisseurBalanceEntity?>> {
        val pieces = operationDao.getByModule(OperationModule.ACHATS.name)
        val existantes = balanceDao.getAll().associateBy { it.fournisseurId }
        val recalculees = fournisseurDao.getAll().map { FournisseurCacheRules.balance(it, pieces, now) }
        val corrigees = recalculees
            .filterNot { FournisseurCacheRules.memesMontants(existantes[it.fournisseurId], it) }
            .map { it.fournisseurId to existantes[it.fournisseurId] }
        balanceDao.upsertAll(recalculees)
        return corrigees
    }
}

/** Tient `fournisseur_scores` à jour (fiabilité mesurée sur les réceptions). Même contrat que le solde. */
class FournisseurScoreRefresher @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val fournisseurDao: FournisseurDao,
    private val scoreDao: FournisseurScoreDao,
) {
    suspend fun rafraichir(fournisseurId: Long, now: Long = System.currentTimeMillis()): FournisseurScoreEntity? {
        if (fournisseurId <= 0L) return null
        if (fournisseurDao.getById(fournisseurId) == null) return null
        val pieces = operationDao.getByModule(OperationModule.ACHATS.name)
        val score = FournisseurCacheRules.score(fournisseurId, pieces, now)
        scoreDao.upsert(score)
        return score
    }

    suspend fun reconstruireTout(now: Long = System.currentTimeMillis()): Int {
        val pieces = operationDao.getByModule(OperationModule.ACHATS.name)
        val scores = fournisseurDao.getAll().map { FournisseurCacheRules.score(it.id, pieces, now) }
        scoreDao.upsertAll(scores)
        return scores.size
    }
}

/**
 * Point d'entrée unique des caches fournisseur : [rafraichir] est appelé par les cas d'usage
 * d'achat dans leur transaction ; [assurerInitialisation] et [verifierCoherence] par le worker.
 */
class FournisseurCacheUseCase @Inject constructor(
    private val balanceRefresher: FournisseurBalanceRefresher,
    private val scoreRefresher: FournisseurScoreRefresher,
    private val fournisseurDao: FournisseurDao,
    private val balanceDao: FournisseurBalanceDao,
    private val scoreDao: FournisseurScoreDao,
    private val settingsStore: SettingsStore,
    private val database: AppDatabase,
    private val journalManager: JournalManager,
) {
    /** Recalcule solde et fiabilité d'un fournisseur ; à appeler dans la transaction de l'écriture. */
    suspend fun rafraichir(fournisseurId: Long, now: Long = System.currentTimeMillis()) {
        balanceRefresher.rafraichir(fournisseurId, now)
        scoreRefresher.rafraichir(fournisseurId, now)
    }

    /** Reconstruction complète, idempotente : une seule fois (drapeau), puis si des fiches n'ont pas de ligne. */
    suspend fun assurerInitialisation(now: Long = System.currentTimeMillis()): Boolean {
        val fait = settingsStore.get(CLE_INITIALISATION) == "true"
        val total = fournisseurDao.count()
        if (fait && balanceDao.count() >= total && scoreDao.count() >= total) return false
        database.withTransaction {
            balanceRefresher.reconstruireTout(now)
            scoreRefresher.reconstruireTout(now)
        }
        settingsStore.set(CLE_INITIALISATION, "true")
        return true
    }

    /** Recalcule tout, corrige et journalise chaque solde faux. Renvoie le nombre de soldes corrigés. */
    suspend fun verifierCoherence(now: Long = System.currentTimeMillis()): Int {
        val corrigees = database.withTransaction {
            val corrigees = balanceRefresher.verifierCoherence(now)
            scoreRefresher.reconstruireTout(now)
            corrigees
        }
        corrigees.forEach { (id, avant) ->
            journalManager.log(
                "FOURNISSEURS",
                "FOURNISSEUR_SOLDE_CORRIGE",
                "Solde du fournisseur #$id corrigé (dette précédente ${avant?.dette ?: "absente"})",
            )
        }
        return corrigees.size
    }

    companion object {
        const val CLE_INITIALISATION = "fournisseur_cache_v24"
    }
}
