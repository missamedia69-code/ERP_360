package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.FournisseurBalanceDao
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurContactDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurScoreDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.domain.model.FournisseurAchatMetrics
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.FournisseurPortefeuilleRules
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Portefeuille fournisseurs : lignes de décision et factures ouvertes, à l'instant [now]. */
data class FournisseurPortefeuille(
    val lignes: List<FournisseurLigne> = emptyList(),
    val factures: List<FournisseurFactureOuverte> = emptyList(),
    val now: Long = 0L,
)

/**
 * Observe tout ce que la liste et le Tableau d'action affichent : fournisseurs, soldes et scores
 * (caches de F2), aptitude (comptes, documents, contact) et factures ouvertes issues des pièces.
 */
class ObserveFournisseurPortefeuilleUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val contactDao: FournisseurContactDao,
    private val compteDao: FournisseurCompteBancaireDao,
    private val documentDao: FournisseurDocumentDao,
    private val balanceDao: FournisseurBalanceDao,
    private val scoreDao: FournisseurScoreDao,
    private val operationDao: OperationRecordDao,
) {
    private class Cache(
        val balances: List<com.missa.b360.core.data.entity.FournisseurBalanceEntity>,
        val scores: List<com.missa.b360.core.data.entity.FournisseurScoreEntity>,
        val pieces: List<com.missa.b360.core.data.entity.OperationRecordEntity>,
    )

    private class Sante(
        val contacts: Set<Long>,
        val comptes: List<com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity>,
        val documents: List<com.missa.b360.core.data.entity.FournisseurDocumentEntity>,
    )

    operator fun invoke(horloge: () -> Long = { System.currentTimeMillis() }): Flow<FournisseurPortefeuille> {
        val caches = combine(
            balanceDao.observeAll(),
            scoreDao.observeAll(),
            operationDao.observeByModule(OperationModule.ACHATS.name),
        ) { balances, scores, pieces -> Cache(balances, scores, pieces) }
        val sante = combine(
            contactDao.observeFournisseursAvecContactPrincipal(),
            compteDao.observeTous(),
            documentDao.observeTousActifs(),
        ) { contacts, comptes, documents -> Sante(contacts.toSet(), comptes, documents) }
        return combine(fournisseurDao.observeTous(), caches, sante) { fournisseurs, cache, s ->
            val now = horloge()
            val echeances = fournisseurs.associate { it.id to it.joursEcheance }
            FournisseurPortefeuille(
                lignes = FournisseurPortefeuilleRules.lignes(
                    fournisseurs, s.contacts, s.comptes, s.documents, cache.balances, cache.scores, now,
                ),
                factures = FournisseurAchatMetrics.facturesOuvertes(cache.pieces, null) { id -> echeances[id] ?: 0 },
                now = now,
            )
        }
    }
}
