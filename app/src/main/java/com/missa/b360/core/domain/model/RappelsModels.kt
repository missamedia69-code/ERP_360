package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaskStatus

/** Ce que l'accueil doit signaler à l'utilisateur dès l'ouverture. */
data class RappelsAccueil(
    val tachesEnAttente: Int = 0,
    val facturesEnRetard: Int = 0,
    val montantEnRetard: Double = 0.0,
) {
    val aQuelqueChose: Boolean get() = tachesEnAttente > 0 || facturesEnRetard > 0
}

/**
 * Règles des rappels de l'accueil — sans dépendance Android ni Room.
 *
 * La carte « Rappels importants » affichait auparavant deux phrases figées,
 * « aucune tâche en attente » et « aucune facture en retard », qu'aucun calcul
 * n'alimentait : elle rassurait même avec dix impayés. Ces règles lui donnent
 * de vraies valeurs.
 */
object RappelsRules {

    /**
     * Délai de règlement retenu quand le client n'en fixe aucun.
     *
     * Trente jours est l'usage commercial courant ; la fiche client peut le
     * remplacer, mais la pièce ne transporte pas cette information, faute de
     * lien direct entre la vente et les conditions du client.
     */
    const val DELAI_REGLEMENT_JOURS = 30

    private const val JOUR_MS = 86_400_000L

    /** Une tâche compte tant qu'elle n'est pas faite. */
    fun tachesEnAttente(taches: List<TaskEntity>): Int =
        taches.count { it.statut != TaskStatus.FAITE.name }

    /**
     * Factures client dont le délai de règlement est dépassé et qui restent
     * partiellement ou totalement impayées.
     *
     * Le reste dû se lit dans le détail de la vente : une facture réglée au
     * centime près n'est pas en retard, même si son échéance est passée.
     */
    fun facturesEnRetard(
        pieces: List<OperationRecordEntity>,
        maintenant: Long,
        delaiJours: Int = DELAI_REGLEMENT_JOURS,
    ): List<Pair<OperationRecordEntity, Double>> = pieces
        .asSequence()
        .filter {
            it.module == OperationModule.VENTE.name &&
                it.status == OperationStatus.VALIDATED.name
        }
        .filter { it.createdAt + delaiJours * JOUR_MS < maintenant }
        .mapNotNull { piece ->
            val detail = SaleRecordCodec.decode(piece.notes) ?: return@mapNotNull null
            val reste = detail.total - detail.paidAmount
            // Le seuil du centime évite qu'un arrondi flottant fasse apparaître
            // un impayé de 0,000001.
            if (reste > 0.01) piece to reste else null
        }
        .toList()

    fun rappels(
        taches: List<TaskEntity>,
        pieces: List<OperationRecordEntity>,
        maintenant: Long,
    ): RappelsAccueil {
        val retards = facturesEnRetard(pieces, maintenant)
        return RappelsAccueil(
            tachesEnAttente = tachesEnAttente(taches),
            facturesEnRetard = retards.size,
            montantEnRetard = retards.sumOf { it.second },
        )
    }
}
