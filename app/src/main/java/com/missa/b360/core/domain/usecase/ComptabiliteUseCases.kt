package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.domain.model.ComptabiliteRules
import com.missa.b360.core.domain.model.EcritureComptable
import com.missa.b360.core.domain.model.ResultatPeriode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Photographie comptable d'une période : journal, résultat et TVA. */
data class SyntheseComptable(
    val journal: List<EcritureComptable> = emptyList(),
    val resultat: ResultatPeriode = ResultatPeriode(),
    val tauxTva: Double = 0.0,
    val tvaCollectee: Double = 0.0,
    val tvaDeductible: Double = 0.0,
    val tvaAPayer: Double = 0.0,
    val vide: Boolean = true,
)

/**
 * Module Comptabilité (CPT) — **lecture seule par construction**.
 *
 * Il ne crée aucune donnée : il relit les pièces validées et les mouvements de
 * trésorerie, et les présente en journal, compte de résultat et position de
 * TVA. Toute saisie propre à la comptabilité ouvrirait une seconde vérité à
 * côté des ventes et des achats.
 */
class ObserverComptabiliteUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val mouvementDao: MouvementTresorerieDao,
    private val taxDao: TaxDao,
) {
    operator fun invoke(debut: () -> Long, fin: () -> Long): Flow<SyntheseComptable> = combine(
        operationDao.observeAll(),
        mouvementDao.observeAll(),
        taxDao.observeAll(),
    ) { pieces, mouvements, taxes ->
        // Le taux retenu est celui de la taxe par défaut posée à l'onboarding ;
        // les multi-taux viendront avec la phase 4.
        val taux = taxes.firstOrNull { it.parDefaut }?.taux
            ?: taxes.firstOrNull()?.taux
            ?: 0.0
        val journal = ComptabiliteRules.journal(pieces, mouvements, debut(), fin())
        SyntheseComptable(
            journal = journal,
            resultat = ComptabiliteRules.resultat(journal),
            tauxTva = taux,
            tvaCollectee = ComptabiliteRules.tvaCollectee(journal, taux),
            tvaDeductible = ComptabiliteRules.tvaDeductible(journal, taux),
            tvaAPayer = ComptabiliteRules.tvaAPayer(journal, taux),
            vide = journal.isEmpty(),
        )
    }
}
