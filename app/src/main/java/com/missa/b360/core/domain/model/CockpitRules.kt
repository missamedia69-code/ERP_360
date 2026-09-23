package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Un point de courbe quotidienne : libellé du jour et valeur. */
data class PointJour(val label: String, val valeur: Double)

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

    /** Nombre de jours affichés dans les courbes des indicateurs. */
    const val JOURS_SERIE = 14

    /** Achats validés d'une journée donnée (module ACHATS). */
    fun achatsJour(records: List<OperationRecordEntity>, jour: Long): Double =
        records
            .filter { it.status == OperationStatus.VALIDATED.name }
            .filter { it.module == OperationModule.ACHATS.name }
            .filter { it.createdAt in jour until jour + JOUR_MS }
            .sumOf { it.amount ?: 0.0 }

    /** Pièces validées d'un module pour une journée donnée. */
    fun piecesJour(records: List<OperationRecordEntity>, jour: Long, module: OperationModule): Int =
        records.count {
            it.status == OperationStatus.VALIDATED.name &&
                it.module == module.name &&
                it.createdAt in jour until jour + JOUR_MS
        }

    /** Mouvements de stock validés d'une journée donnée. */
    fun mouvementsJour(records: List<OperationRecordEntity>, jour: Long): Int =
        piecesJour(records, jour, OperationModule.STOCK)

    /** Flux net cumulé de toutes les pièces validées avant [limite]. */
    fun fluxAvant(records: List<OperationRecordEntity>, limite: Long): Double =
        records
            .filter { it.status == OperationStatus.VALIDATED.name }
            .filter { it.createdAt < limite }
            .sumOf { record ->
                when (record.direction) {
                    OperationDirection.IN.name -> record.amount ?: 0.0
                    OperationDirection.OUT.name -> -(record.amount ?: 0.0)
                    else -> 0.0
                }
            }

    /** Débuts de journée des [jours] derniers jours, du plus ancien au plus récent. */
    fun debutsJours(maintenant: Long, jours: Int = JOURS_SERIE): List<Long> {
        val aujourdhui = debutJour(maintenant)
        return (jours - 1 downTo 0).map { aujourdhui - it * JOUR_MS }
    }

    /** Libellé court d'un jour (« 5/3 ») pour les courbes. */
    fun libelleJour(jour: Long): String =
        SimpleDateFormat("d/M", Locale.getDefault()).format(Date(jour))

    /** Courbe quotidienne des ventes validées. */
    fun serieVentes(records: List<OperationRecordEntity>, maintenant: Long, jours: Int = JOURS_SERIE): List<PointJour> =
        debutsJours(maintenant, jours).map { j -> PointJour(libelleJour(j), encaissements(records, j)) }

    /** Courbe quotidienne des achats validés. */
    fun serieAchats(records: List<OperationRecordEntity>, maintenant: Long, jours: Int = JOURS_SERIE): List<PointJour> =
        debutsJours(maintenant, jours).map { j -> PointJour(libelleJour(j), achatsJour(records, j)) }

    /** Courbe du solde de trésorerie : flux cumulé (ouvertures + flux daily). */
    fun serieTresorerie(records: List<OperationRecordEntity>, maintenant: Long, jours: Int = JOURS_SERIE): List<PointJour> {
        val debuts = debutsJours(maintenant, jours)
        var cumul = fluxAvant(records, debuts.first())
        return debuts.map { j ->
            cumul += fluxJour(records, j)
            PointJour(libelleJour(j), cumul)
        }
    }

    /** Courbe du nombre total de clients au fil des [jours] derniers jours. */
    fun serieClients(creations: List<Long>, maintenant: Long, jours: Int = JOURS_SERIE): List<PointJour> =
        debutsJours(maintenant, jours).map { j ->
            PointJour(libelleJour(j), creations.count { it < j + JOUR_MS }.toDouble())
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
