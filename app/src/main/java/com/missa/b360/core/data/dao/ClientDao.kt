package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.PriceClientEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Clients (module 9.9).
 * Convention C7 : aucune suppression physique — statut `active`/`DESACTIVE` uniquement.
 */
@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE active = 1 ORDER BY nom")
    fun observeAll(): Flow<List<ClientEntity>>

    /** Liste complète destinée au module Client, y compris les comptes désactivés. */
    @Query("SELECT * FROM clients ORDER BY nom")
    fun observeAllIncludingInactive(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Query("SELECT * FROM clients WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ClientEntity?

    /** Détection de doublons RC-01 : même téléphone OU nom proche. */
    @Query(
        "SELECT * FROM clients WHERE active = 1 AND (telephone = :telephone " +
            "OR LOWER(TRIM(nom)) = LOWER(TRIM(:nom)))",
    )
    suspend fun findDoublonsPotentiels(telephone: String, nom: String): List<ClientEntity>

    /** Solde dérivé (RC-08) — Phase D : requête agrégée factures − paiements. */
    @Insert
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Query("SELECT COUNT(*) FROM clients WHERE active = 1 AND (telephone = :telephone)")
    suspend fun countTelephone(telephone: String): Int

    /** Désactivation (jamais de DELETE — C7). */
    @Query("UPDATE clients SET statut = 'DESACTIVE', active = 0 WHERE id = :id")
    suspend fun desactiver(id: Long)

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Int

    // --- Catégories ---
    @Query("SELECT * FROM client_categories ORDER BY nom")
    fun observeCategories(): Flow<List<CategoryClientEntity>>

    @Query("SELECT * FROM client_categories WHERE id = :id")
    suspend fun getCategorieById(id: Long): CategoryClientEntity?

    @Insert
    suspend fun insertCategorie(categorie: CategoryClientEntity): Long

    @Update
    suspend fun updateCategorie(categorie: CategoryClientEntity)

    /** Suppression interdite si rattachée : le UseCase vérifie ce compteur. */
    @Query("SELECT COUNT(*) FROM clients WHERE categorieId = :categorieId")
    suspend fun countClientsAvecCategorie(categorieId: Long): Int

    @Query("DELETE FROM client_categories WHERE id = :categorieId")
    suspend fun deleteCategorie(categorieId: Long)

    // --- Badges de fidélité ---
    @Query("SELECT * FROM loyalty_badges ORDER BY nom")
    fun observeBadges(): Flow<List<BadgeLoyaltyEntity>>

    @Query("SELECT * FROM loyalty_badges WHERE id = :id")
    suspend fun getBadgeById(id: Long): BadgeLoyaltyEntity?

    @Insert
    suspend fun insertBadge(badge: BadgeLoyaltyEntity): Long

    @Update
    suspend fun updateBadge(badge: BadgeLoyaltyEntity)

    // --- Prix spécifiques (RC-07) ---
    @Query("SELECT * FROM client_prices WHERE clientId = :clientId")
    fun observePrix(clientId: Long): Flow<List<PriceClientEntity>>

    @Insert
    suspend fun insertPrix(prix: PriceClientEntity): Long


    // --- Contacts et adresses du profil client ---
    @Query("SELECT * FROM client_contacts WHERE clientId = :clientId ORDER BY principal DESC, nom")
    fun observeContacts(clientId: Long): Flow<List<ClientContactEntity>>

    @Query("SELECT * FROM client_addresses WHERE clientId = :clientId ORDER BY principale DESC, libelle")
    fun observeAddresses(clientId: Long): Flow<List<ClientAddressEntity>>

    @Insert
    suspend fun insertContacts(contacts: List<ClientContactEntity>)

    @Insert
    suspend fun insertAddresses(addresses: List<ClientAddressEntity>)

    @Query("DELETE FROM client_contacts WHERE clientId = :clientId")
    suspend fun deleteContacts(clientId: Long)

    @Query("DELETE FROM client_addresses WHERE clientId = :clientId")
    suspend fun deleteAddresses(clientId: Long)

    /** Remplace le sous-profil de façon atomique en conservant le client lui-même. */
    @Transaction
    suspend fun replaceProfileRelations(
        clientId: Long,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>,
    ) {
        deleteContacts(clientId)
        deleteAddresses(clientId)
        if (contacts.isNotEmpty()) insertContacts(contacts.map { it.copy(id = 0, clientId = clientId) })
        if (addresses.isNotEmpty()) insertAddresses(addresses.map { it.copy(id = 0, clientId = clientId) })
    }

    /** Crée le client et ses données liées dans la même transaction Room. */
    @Transaction
    suspend fun insertClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>,
    ): Long {
        val id = insert(client)
        replaceProfileRelations(id, contacts, addresses)
        return id
    }

    /** Met à jour le client et remplace ses relations sans laisser de données orphelines. */
    @Transaction
    suspend fun updateClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>,
    ) {
        update(client)
        replaceProfileRelations(client.id, contacts, addresses)
    }
}
