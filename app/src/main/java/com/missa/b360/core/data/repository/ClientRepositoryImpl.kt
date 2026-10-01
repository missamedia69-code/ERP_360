package com.missa.b360.core.data.repository

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.SuppressionResult
import com.missa.b360.core.data.entity.ClientStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Implementation of [ClientRepository] using Room DAOs. */
@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao
) : ClientRepository {

    override fun observeAllClients(): Flow<List<ClientEntity>> =
        clientDao.observeAll()

    override fun observeAllIncludingInactive(): Flow<List<ClientEntity>> =
        clientDao.observeAllIncludingInactive()

    override fun findDoublonsPotentiels(
        telephoneNormalise: String,
        nomNormalise: String
    ): Flow<List<ClientEntity>> =
        clientDao.findDoublonsPotentiels(telephoneNormalise, nomNormalise)

    override fun observeContacts(clientId: Long): Flow<List<ClientContactEntity>> =
        clientDao.observeContacts(clientId)

    override fun observeAddresses(clientId: Long): Flow<List<ClientAddressEntity>> =
        clientDao.observeAddresses(clientId)

    override fun getById(id: Long): ClientEntity? =
        clientDao.getById(id)

    override suspend fun insertClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>
    ): Long =
        clientDao.insertClientProfile(client, contacts, addresses)

    override suspend fun update(client: ClientEntity) {
        clientDao.update(client)
    }

    override suspend fun updateClientProfile(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        addresses: List<ClientAddressEntity>
    ) {
        clientDao.updateClientProfile(client, contacts, addresses)
    }

    override suspend fun desactiver(id: Long) {
        clientDao.desactiver(id)
    }

    override fun observeCategories(): Flow<List<CategoryClientEntity>> =
        clientDao.observeCategories()

    override fun getCategoryById(id: Long): CategoryClientEntity? =
        clientDao.getCategorieById(id)

    override suspend fun creerCategorie(nom: String): Long? =
        clientDao.insertCategorie(CategoryClientEntity(nom = nom))

    override suspend fun renommerCategorie(id: Long, nom: String): Boolean {
        val cat = clientDao.getCategorieById(id) ?: return false
        clientDao.updateCategorie(cat.copy(nom = nom))
        return true
    }

    override suspend fun supprimerCategorie(id: Long): SuppressionResult {
        if (clientDao.getCategorieById(id) == null) return SuppressionResult.Introuvable
        if (clientDao.countClientsAvecCategorie(id) > 0) return SuppressionResult.CategorieUtilisee
        clientDao.deleteCategorie(id)
        return SuppressionResult.Supprimee
    }

    override suspend fun creerBadge(nom: String, remisePct: Double): Long? {
        val entity = BadgeLoyaltyEntity(nom = nom, remisePct = remisePct)
        return clientDao.insertBadge(entity)
    }

    override fun getBadgeById(id: Long): BadgeLoyaltyEntity? =
        clientDao.getBadgeById(id)

    override suspend fun modifierBadge(
        id: Long,
        nom: String,
        remisePct: Double,
        actif: Boolean
    ): Boolean {
        val badge = clientDao.getBadgeById(id) ?: return false
        clientDao.updateBadge(badge.copy(nom = nom, remisePct = remisePct, actif = actif))
        return true
    }

    override fun observeBadges(): Flow<List<BadgeLoyaltyEntity>> =
        clientDao.observeBadges()
}