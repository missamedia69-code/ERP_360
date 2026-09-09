package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import java.util.Calendar

/** Un point du graphique de performance : un mois et le montant encaissé. */
data class PointPerformance(
    /** Index du mois, de 1 (janvier) à 12 (décembre). */
    val moisIndex: Int,
    val montant: Double,
)

/**
 * Données du « cockpit compact » de l'accueil — calculées sans interface ni base.
 *
 * Tout part des pièces réellement validées : aucune valeur n'est inventée. Les
 * tendances comparent la journée en cours à la veille ; la performance affiche
 * les encaissements des six derniers mois civils.
 */
object CockpitRules {

    /** Nombre de mois affichés dans le graphique de performance. */
    const val MOIS_PERFORMANCE = 6

    /** Seuil (en %) sous lequel une marge est signalée comme faible. */
    const val MARGE_FAIBLE_PCT = 10.0

    /**
     * Variation en pourcentage entre la valeur courante et la veille.
     *
     * Renvoie `null` quand la comparaison n'a pas de sens — valeur de la veille
     * nulle, ou aucune donnée — plutôt qu'un « 0 % » trompeur.
     */
    fun variationPct(courant: Double, precedent: Double): Double? {
        if (precedent == 0.0) return null
        return (courant - precedent) / precedent * 100.0
    }

    /** Encaissements d'une journée donnée (module VENTE, pièces validées). */
    fun encaissements(records: List<OperationRecordEntity>, jour: Long): Double =
        records
            .filter { it.status == OperationStatus.VALIDATED.name }
            .filter { it.module == OperationModule.VENTE.name }
            .filter { it.createdAt in jour until jour + JOUR_MS }
            .sumOf { it.amount ?: 0.0 }

    /** Flux net (au sens trésorerie) d'une journée : entrées moins sorties. */
    fun fluxJour(records: List<OperationRecordEntity>, jour: Long): Double =
        records
            .filter { it.status == OperationStatus.VALIDATED.name }
            .filter { it.createdAt in jour until jour + JOUR_MS }
            .sumOf { record ->
                when (record.direction) {
                    OperationDirection.IN.name -> record.amount ?: 0.0
                    OperationDirection.OUT.name -> -(record.amount ?: 0.0)
                    else -> 0.0
                }
            }

    /**
     * Marge brute du jour, calculée une seule fois comme les ventes moins les
     * achats des pièces validées du même jour.
     */
    fun margeJour(ventes: Double, achats: Double): Double = ventes - achats

    /**
     * Série des [MOIS_PERFORMANCE] derniers mois civils, du plus ancien au plus
     * récent, avec le montant total des ventes validées de chaque mois.
     *
     * Le mois courant est toujours présent, même s'il ne contient encore rien :
     * le graphique garde sa forme et l'utilisateur voit l'état le plus récent.
     */
    fun performanceMensuelle(
        records: List<OperationRecordEntity>,
        maintenant: Long,
    ): List<PointPerformance> {
        val ventesParMois = records
            .filter { it.status == OperationStatus.VALIDATED.name }
            .filter { it.module == OperationModule.VENTE.name }
            .groupBy { record -> cleMois(record.createdAt) }
            .mapValues { (_, lignes) -> lignes.sumOf { it.amount ?: 0.0 } }
        val maintenantCal = Calendar.getInstance().apply { timeInMillis = maintenant }
        return (MOIS_PERFORMANCE - 1 downTo 0).map { decalage ->
            val cal = (Calendar.getInstance().apply { timeInMillis = maintenant }).apply {
                add(Calendar.MONTH, -decalage)
            }
            PointPerformance(
                moisIndex = cal.get(Calendar.MONTH) + 1,
                montant = ventesParMois[cleMois(cal.timeInMillis)] ?: 0.0,
            )
        }
    }

    private fun cleMois(horodatage: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = horodatage }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Début de la journée civile courante, en epoch millis. */
    fun debutJour(maintenant: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = maintenant }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private const val JOUR_MS = 86_400_000L
}
