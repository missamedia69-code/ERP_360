package com.missa.b360.core.domain.model

enum class PlanStatut { PLANIFIE, PAYE, REPORTE, ANNULE }

/** Paiement prévu d'une facture fournisseur. Une ligne n'est jamais supprimée. */
data class PaymentPlan(
    val id: Long,
    val factureRecordId: Long,
    val datePrevue: Long,
    val montant: Double,
    val statut: PlanStatut = PlanStatut.PLANIFIE,
)

/** Échéancier : plans de paiement bornés par le reste dû de leur facture. */
object PaymentPlanRules {

    private const val TOLERANCE = 0.005

    /** Montant encore planifiable : reste dû moins les plans encore `PLANIFIE` de la facture. */
    fun resteAPlanifier(resteDu: Double, plans: List<PaymentPlan>): Double {
        if (!resteDu.isFinite()) return 0.0
        val engage = plans.filter { it.statut == PlanStatut.PLANIFIE && it.montant.isFinite() }.sumOf { it.montant }
        return (resteDu - engage).coerceAtLeast(0.0)
    }

    /** Un plan est valide s'il est positif et ne fait pas dépasser le reste dû de sa facture. */
    fun planifiable(resteDu: Double, plans: List<PaymentPlan>, montant: Double): Boolean =
        montant.isFinite() && montant > 0.0 && montant <= resteAPlanifier(resteDu, plans) + TOLERANCE

    /**
     * Report : l'ancienne ligne passe `REPORTE` (elle garde sa date dans l'historique) et une
     * nouvelle ligne `PLANIFIE` (id 0, à créer) porte la nouvelle date. `null` si le plan n'est
     * plus `PLANIFIE` ou si la date ne change pas.
     */
    fun reporter(plan: PaymentPlan, nouvelleDate: Long): Pair<PaymentPlan, PaymentPlan>? {
        if (plan.statut != PlanStatut.PLANIFIE || plan.datePrevue == nouvelleDate) return null
        return plan.copy(statut = PlanStatut.REPORTE) to plan.copy(id = 0, datePrevue = nouvelleDate, statut = PlanStatut.PLANIFIE)
    }

    /** Annulation : seul un plan `PLANIFIE` peut être annulé. */
    fun annuler(plan: PaymentPlan): PaymentPlan? =
        if (plan.statut == PlanStatut.PLANIFIE) plan.copy(statut = PlanStatut.ANNULE) else null

    /**
     * Identifiants des plans `PLANIFIE` de la facture soldés par un règlement de [montantRegle] :
     * dans l'ordre des dates, tant que le règlement couvre entièrement le montant du plan.
     */
    fun plansPayes(plans: List<PaymentPlan>, factureRecordId: Long, montantRegle: Double): List<Long> {
        if (!montantRegle.isFinite() || montantRegle <= 0.0) return emptyList()
        var reste = montantRegle
        val payes = mutableListOf<Long>()
        plans.filter { it.factureRecordId == factureRecordId && it.statut == PlanStatut.PLANIFIE }
            .sortedWith(compareBy({ it.datePrevue }, { it.id }))
            .forEach { plan ->
                if (plan.montant <= reste + TOLERANCE) {
                    payes += plan.id
                    reste -= plan.montant
                } else {
                    return payes
                }
            }
        return payes
    }
}
