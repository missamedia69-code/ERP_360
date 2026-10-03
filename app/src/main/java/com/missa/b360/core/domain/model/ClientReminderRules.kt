package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType

/**
 * Écran des relances : les clients en retard groupés par tranche d'ancienneté, du plus ancien au
 * plus récent, et les promesses non tenues. Règles pures, sans accès aux données.
 */
object ClientReminderRules {
    /** Tranche d'un retard exprimé en jours ; 0 ou moins = non échu. */
    fun tranche(joursRetard: Int): AgingBucket = when {
        joursRetard <= 0 -> AgingBucket.NON_ECHU
        joursRetard <= 30 -> AgingBucket.JOURS_1_30
        joursRetard <= 60 -> AgingBucket.JOURS_31_60
        joursRetard <= 90 -> AgingBucket.JOURS_61_90
        else -> AgingBucket.PLUS_90
    }

    /**
     * Clients ayant un montant échu, groupés par tranche de leur plus ancien retard. Les plus
     * grosses sommes passent d'abord dans chaque tranche ; les tranches les plus anciennes d'abord.
     */
    fun groupes(items: List<ClientListItem>): List<Pair<AgingBucket, List<ClientListItem>>> =
        items.filter { it.enRetard > 0.0 }
            .groupBy { tranche(it.balance?.joursRetardMax ?: 0) }
            .filterKeys { it != AgingBucket.NON_ECHU }
            .toList()
            .sortedByDescending { it.first.ordinal }
            .map { (tranche, liste) -> tranche to liste.sortedByDescending { it.enRetard } }

    /** Total échu de l'ensemble des clients. */
    fun totalEnRetard(items: List<ClientListItem>): Double = items.sumOf { it.enRetard }

    /** Promesses qui n'ont pas été honorées, la plus récente d'abord. */
    fun promessesNonTenues(suivis: List<ClientFollowupEntity>): List<ClientFollowupEntity> =
        suivis.filter { it.type == FollowupType.PROMESSE && it.statut == FollowupStatus.NON_TENU }
            .sortedByDescending { it.promesseDate ?: it.createdAt }
}
