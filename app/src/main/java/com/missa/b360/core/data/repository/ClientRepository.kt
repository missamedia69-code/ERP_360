package com.missa.b360.core.data.repository

import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.SuppressionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for client-related data operations.
 * Encapsulates DAO access and provides a cleaner API for use cases.
 */
interface ClientRepository {

    /** Observe all active clients. */
    fun observeAllClients(): Flow<List<ClientEntity>>

    /** Observe all clients including inactive ones. */
    fun observeAllIncludingInactive(): Flow<List<ClientEntity>>

    /** Find potential duplicate clients by phone and name. */
    fun findDoublonsPotentiels(telephoneNormalise: String, nomNormalise: String): Flow<List<ClientEntity>>

    /** Observe contacts for a given client. */
    fun observeContacts(clientId: Long): Flow<List<ClientContactEntity>>

    /** Observe addresses for a given client. */
    fun observeAddresses(clientId: Long): Flow<List<ClientAddressEntity>>

    /** Get a client by ID. */
    fun getById(id: Long): ClientEntity?

    /** Insert a client profile with contacts and addresses. */
    suspend fun insertClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>
    ): Long

    /** Update a client entity. */
    suspend fun update(client: ClientEntity)

    /** Update a client profile with contacts and addresses. */
    suspend fun updateClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>
    )

    /** Deactivate a client (soft delete). */
    suspend fun desactiver(id: Long)

    /** Observe all client categories. */
    fun observeCategories(): Flow<List<CategoryClientEntity>>

    /** Get a category by ID. */
    fun getCategoryById(id: Long): CategoryClientEntity?

    /** Create a new client category. */
    suspend fun creerCategorie(nom: String): Long?

    /** Rename an existing client category. */
    suspend fun renommerCategorie(id: Long, nom: String): Boolean

    /** Delete a client category if not used. */
    suspend fun supprimerCategorie(id: Long): SuppressionResult

    /** Create a new loyalty badge. */
    suspend fun creerBadge(nom: String, remisePct: Double): Long?

    /** Get a loyalty badge by ID. */
    fun getBadgeById(id: Long): BadgeLoyaltyEntity?

    /** Update an existing loyalty badge. */
    suspend fun modifierBadge(id: Long, nom: String, remisePct: Double, actif: Boolean): Boolean

    /** Observe all loyalty badges. */
    fun observeBadges(): Flow<List<BadgeLoyaltyEntity>>
}