package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import kotlinx.coroutines.flow.Flow

/** Situation de compte dérivée (convention C7 : jamais de DELETE, on remplace la ligne). */
@Dao
interface ClientBalanceDao {
    @Query("SELECT * FROM client_balances")
    fun observeAll(): Flow<List<ClientBalanceEntity>>

    @Query("SELECT * FROM client_balances WHERE clientId = :clientId")
    fun observe(clientId: Long): Flow<ClientBalanceEntity?>

    @Query("SELECT * FROM client_balances WHERE clientId = :clientId")
    suspend fun get(clientId: Long): ClientBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: ClientBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(balances: List<ClientBalanceEntity>)

    @Query("SELECT COUNT(*) FROM client_balances")
    suspend fun count(): Int

    /** Clients dont l'échéance peut évoluer avec le temps : à recalculer chaque jour. */
    @Query("SELECT clientId FROM client_balances WHERE encours > 0")
    suspend fun clientIdsAvecEncours(): List<Long>
}

/** Journal de suivi : insertion et changement de statut uniquement. */
@Dao
interface ClientFollowupDao {
    @Insert
    suspend fun insert(followup: ClientFollowupEntity): Long

    @Update
    suspend fun update(followup: ClientFollowupEntity)

    @Query("SELECT * FROM client_followups WHERE id = :id")
    suspend fun getById(id: Long): ClientFollowupEntity?

    @Query("SELECT * FROM client_followups WHERE clientId = :clientId ORDER BY createdAt DESC, id DESC")
    fun observeByClient(clientId: Long): Flow<List<ClientFollowupEntity>>

    @Query("SELECT * FROM client_followups ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ClientFollowupEntity>>

    /** Promesses encore ouvertes, à réévaluer (tenue / non tenue). */
    @Query("SELECT * FROM client_followups WHERE type = 'PROMESSE' AND statut = 'OUVERT'")
    suspend fun getPromessesOuvertes(): List<ClientFollowupEntity>

    @Query("SELECT MAX(createdAt) FROM client_followups WHERE clientId = :clientId AND type = 'RELANCE'")
    suspend fun derniereRelanceAt(clientId: Long): Long?

    @Query(
        "SELECT MAX(promesseDate) FROM client_followups " +
            "WHERE clientId = :clientId AND type = 'PROMESSE' AND statut = 'OUVERT'",
    )
    suspend fun promesseOuverteJusquA(clientId: Long): Long?
}

/** Encaissements postérieurs à la facture ; aucune suppression, annulation par ligne négative. */
@Dao
interface ClientPaymentDao {
    @Insert
    suspend fun insert(payment: ClientPaymentEntity): Long

    @Query("SELECT * FROM client_payments WHERE id = :id")
    suspend fun getById(id: Long): ClientPaymentEntity?

    @Query("SELECT * FROM client_payments WHERE reference = :reference LIMIT 1")
    suspend fun getByReference(reference: String): ClientPaymentEntity?

    @Query("SELECT * FROM client_payments WHERE clientId = :clientId ORDER BY paiementAt DESC, id DESC")
    suspend fun getByClient(clientId: Long): List<ClientPaymentEntity>

    @Query("SELECT * FROM client_payments WHERE clientId = :clientId ORDER BY paiementAt DESC, id DESC")
    fun observeByClient(clientId: Long): Flow<List<ClientPaymentEntity>>

    @Query("SELECT * FROM client_payments")
    suspend fun getAll(): List<ClientPaymentEntity>

    /** Somme nette encaissée depuis [depuis] (contre-passations comprises). */
    @Query("SELECT COALESCE(SUM(montant), 0) FROM client_payments WHERE clientId = :clientId AND paiementAt >= :depuis")
    suspend fun encaisseDepuis(clientId: Long, depuis: Long): Double
}
