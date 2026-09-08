package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.GroupeArticleComplet
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.domain.model.GroupesStandards
import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.util.Iso4217
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Référentiel des groupes d'articles.
 *
 * Les groupes portent les règles transverses du catalogue : ce qui se stocke,
 * se vend, s'achète, se fabrique ou s'entretient. Ils sont créés à
 * l'installation avec les comptes du référentiel comptable du pays, puis
 * ajustables par l'utilisateur.
 */
class GroupeArticleUseCases @Inject constructor(
    private val database: AppDatabase,
    private val dao: GroupeArticleDao,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "REFERENTIEL" }

    fun observer(): Flow<List<GroupeArticleComplet>> = dao.observerComplets()

    suspend fun complet(id: Long): GroupeArticleComplet? = dao.complet(id)

    /**
     * Installe les groupes standards s'ils n'existent pas encore.
     *
     * Idempotent : rejouable à chaque démarrage sans créer de doublon, ce qui
     * permet aussi de doter une installation antérieure aux groupes.
     */
    suspend fun installerGroupesStandards(
        pays: String?,
        maintenant: Long = System.currentTimeMillis(),
    ): Int {
        val referentiel = ReferentielFiscal
            .zone(Iso4217.codePaysDepuisNom(pays))
            .referentielComptable
        var crees = 0
        database.withTransaction {
            for (modele in GroupesStandards.modeles(referentiel, maintenant)) {
                if (dao.parCode(modele.groupe.code) != null) continue
                val id = dao.inserer(modele.groupe)
                dao.insererStock(modele.stock.copy(itemGroupId = id))
                dao.insererAchat(modele.achat.copy(itemGroupId = id))
                dao.insererVente(modele.vente.copy(itemGroupId = id))
                dao.insererProduction(modele.production.copy(itemGroupId = id))
                dao.insererMaintenance(modele.maintenance.copy(itemGroupId = id))
                dao.insererComptabilite(modele.comptabilite.copy(itemGroupId = id))
                crees++
            }
        }
        if (crees > 0) {
            journalManager.log(MODULE, "GROUPES_INSTALLES", "$crees groupes d'articles")
        }
        return crees
    }
}
