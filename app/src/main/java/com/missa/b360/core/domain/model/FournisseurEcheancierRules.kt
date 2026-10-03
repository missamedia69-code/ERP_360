package com.missa.b360.core.domain.model

/** Tranche d'échéance d'une facture fournisseur, de la plus urgente à la plus lointaine. */
enum class EcheanceTranche { EN_RETARD, SEMAINE, MOIS, PLUS_TARD }

/** Une facture à payer avec ses paiements planifiés (statut PLANIFIE uniquement). */
data class EcheancierLigne(
    val facture: FournisseurFactureOuverte,
    val fournisseurNom: String,
    val plans: List<PaymentPlan>,
    val resteAPlanifier: Double,
)

data class EcheancierGroupe(
    val tranche: EcheanceTranche,
    val lignes: List<EcheancierLigne>,
    val total: Double,
)

/** Échéancier : factures ouvertes groupées par urgence, avec ce qui est déjà planifié. */
object FournisseurEcheancierRules {
    private const val JOUR_MS = 86_400_000L

    fun tranche(dueAt: Long, now: Long): EcheanceTranche = when {
        dueAt < now -> EcheanceTranche.EN_RETARD
        dueAt <= now + 7 * JOUR_MS -> EcheanceTranche.SEMAINE
        dueAt <= now + 30 * JOUR_MS -> EcheanceTranche.MOIS
        else -> EcheanceTranche.PLUS_TARD
    }

    fun construire(
        factures: List<FournisseurFactureOuverte>,
        noms: Map<Long, String>,
        plans: List<PaymentPlan>,
        now: Long,
    ): List<EcheancierGroupe> {
        val plansParFacture = plans.filter { it.statut == PlanStatut.PLANIFIE }.groupBy { it.factureRecordId }
        return factures
            .sortedWith(compareBy({ it.dueAt }, { it.recordId }))
            .map { f ->
                val siens = plansParFacture[f.recordId].orEmpty().sortedWith(compareBy({ it.datePrevue }, { it.id }))
                f to EcheancierLigne(
                    facture = f,
                    fournisseurNom = noms[f.fournisseurId] ?: "#${f.fournisseurId}",
                    plans = siens,
                    resteAPlanifier = PaymentPlanRules.resteAPlanifier(f.outstanding, siens),
                )
            }
            .groupBy({ tranche(it.first.dueAt, now) }, { it.second })
            .toSortedMap()
            .map { (tranche, lignes) -> EcheancierGroupe(tranche, lignes, lignes.sumOf { it.facture.outstanding }) }
    }
}
