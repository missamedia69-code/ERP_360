package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.journal.JournalManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** RA-21 — Gestion multi-site. */
class SiteUseCases @Inject constructor(
    private val siteDao: SiteDao,
    private val journalManager: JournalManager,
) {
    fun observerSites(): Flow<List<SiteEntity>> = siteDao.observeAll()

    suspend fun ajouterSite(nom: String, type: String, adresse: String?): Long {
        val id = siteDao.insert(
            SiteEntity(nom = nom.trim(), type = type, adresse = adresse, principal = false),
        )
        journalManager.log("ADMIN", "SITE_AJOUTE", "Site $nom ajouté")
        return id
    }

    suspend fun modifierSite(site: SiteEntity) {
        siteDao.update(site)
        journalManager.log("ADMIN", "SITE_MODIFIE", "Site ${site.nom} modifié")
    }

    /** @return true si le site a été supprimé (le site principal est protégé). */
    suspend fun supprimerSite(site: SiteEntity): Boolean {
        if (site.principal) return false
        siteDao.delete(site.id)
        journalManager.log("ADMIN", "SITE_SUPPRIME", "Site ${site.nom} supprimé")
        return true
    }
}