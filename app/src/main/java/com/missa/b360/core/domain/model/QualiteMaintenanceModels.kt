package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.data.entity.StatutIntervention
import com.missa.b360.core.data.entity.StatutNc
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention

/** Synthèse qualité d'une période. */
data class BilanQualite(
    val ouvertes: Int = 0,
    val enCours: Int = 0,
    val resolues: Int = 0,
    val critiques: Int = 0,
    val coutTotal: Double = 0.0,
    /** Délai moyen de résolution, en jours ; null si rien n'est encore résolu. */
    val delaiMoyenJours: Double? = null,
    val parOrigine: List<Pair<OrigineNc, Int>> = emptyList(),
) {
    val total: Int get() = ouvertes + enCours + resolues

    /** Part des écarts refermés — la seule mesure de progrès qui vaille. */
    val tauxResolution: Double
        get() = if (total == 0) 0.0 else resolues.toDouble() / total * 100.0
}

/** Un équipement et son état de maintenance. */
data class EtatEquipement(
    val equipement: EquipementEntity,
    val derniereIntervention: Long?,
    val prochaineEcheance: Long?,
    val joursDeRetard: Int,
    val interventions: Int,
    val pannes: Int,
    val coutCumule: Double,
    val heuresArret: Double,
) {
    val enRetard: Boolean get() = joursDeRetard > 0
}

/**
 * Règles Qualité et Maintenance — sans dépendance Android ni Room.
 *
 * Ces deux modules partagent la même logique de fond : constater un écart,
 * le dater, mesurer le temps qu'il a fallu pour le refermer.
 */
object QualiteMaintenanceRules {

    private const val JOUR_MS = 86_400_000L

    // --- Qualité ---

    fun bilan(
        nonConformites: List<NonConformiteEntity>,
        debut: Long = Long.MIN_VALUE,
        fin: Long = Long.MAX_VALUE,
    ): BilanQualite {
        val periode = nonConformites.filter { it.date in debut..fin }
        if (periode.isEmpty()) return BilanQualite()
        val resolues = periode.filter { it.statut == StatutNc.RESOLUE.name }
        val delais = resolues.mapNotNull { nc ->
            nc.dateResolution?.let { ((it - nc.date).coerceAtLeast(0L)).toDouble() / JOUR_MS }
        }
        return BilanQualite(
            ouvertes = periode.count { it.statut == StatutNc.OUVERTE.name },
            enCours = periode.count { it.statut == StatutNc.EN_COURS.name },
            resolues = resolues.size,
            critiques = periode.count {
                it.gravite == GraviteNc.CRITIQUE.name && it.statut != StatutNc.RESOLUE.name
            },
            coutTotal = periode.sumOf { it.cout },
            delaiMoyenJours = delais.average().takeIf { delais.isNotEmpty() },
            parOrigine = periode
                .groupingBy { origine(it.origine) }
                .eachCount()
                .toList()
                .sortedByDescending { it.second },
        )
    }

    /**
     * Non-conformités à traiter en priorité : les critiques d'abord, puis les
     * majeures, et à gravité égale la plus ancienne — c'est celle qui traîne
     * depuis le plus longtemps qui coûte le plus cher.
     */
    fun aTraiter(nonConformites: List<NonConformiteEntity>): List<NonConformiteEntity> =
        nonConformites
            .filter { it.statut != StatutNc.RESOLUE.name }
            .sortedWith(
                compareByDescending<NonConformiteEntity> { poidsGravite(it.gravite) }
                    .thenBy { it.date },
            )

    private fun poidsGravite(nom: String): Int = when (gravite(nom)) {
        GraviteNc.CRITIQUE -> 3
        GraviteNc.MAJEURE -> 2
        GraviteNc.MINEURE -> 1
    }

    // --- Maintenance ---

    /**
     * État du parc : dernière intervention, prochaine échéance préventive et
     * retard éventuel.
     *
     * L'échéance se calcule depuis la dernière intervention **préventive**
     * réalisée, non depuis n'importe quelle intervention : une réparation
     * d'urgence ne remet pas le compteur du plan préventif à zéro.
     */
    fun etatDuParc(
        equipements: List<EquipementEntity>,
        interventions: List<InterventionEntity>,
        maintenant: Long,
    ): List<EtatEquipement> {
        val parEquipement = interventions
            .filter { it.statut != StatutIntervention.ANNULEE.name }
            .groupBy { it.equipementId }
        return equipements.map { equipement ->
            val lignes = parEquipement[equipement.id].orEmpty()
            val realisees = lignes.filter { it.statut == StatutIntervention.REALISEE.name }
            val dernierePreventive = realisees
                .filter { it.type == TypeIntervention.PREVENTIVE.name }
                .maxOfOrNull { it.date }
            val base = dernierePreventive ?: equipement.dateMiseEnService
            val prochaine = if (equipement.periodiciteJours > 0 && base != null) {
                base + equipement.periodiciteJours * JOUR_MS
            } else {
                null
            }
            EtatEquipement(
                equipement = equipement,
                derniereIntervention = realisees.maxOfOrNull { it.date },
                prochaineEcheance = prochaine,
                joursDeRetard = prochaine
                    ?.let { ((maintenant - it) / JOUR_MS).toInt() }
                    ?.coerceAtLeast(0)
                    ?: 0,
                interventions = realisees.size,
                pannes = realisees.count { it.type == TypeIntervention.CORRECTIVE.name },
                coutCumule = realisees.sumOf { it.cout },
                heuresArret = realisees.sumOf { it.dureeHeures },
            )
        }.sortedWith(
            compareByDescending<EtatEquipement> { it.joursDeRetard }
                .thenByDescending { it.pannes },
        )
    }

    /** Équipements dont l'entretien préventif est dépassé. */
    fun enRetard(parc: List<EtatEquipement>): List<EtatEquipement> = parc.filter { it.enRetard }

    /**
     * Part des interventions préventives dans le total. Un parc sain se soigne
     * avant de tomber en panne : sous 50 %, la maintenance est subie.
     */
    fun tauxPreventif(interventions: List<InterventionEntity>): Double {
        val realisees = interventions.filter { it.statut == StatutIntervention.REALISEE.name }
        if (realisees.isEmpty()) return 0.0
        val preventives = realisees.count { it.type == TypeIntervention.PREVENTIVE.name }
        return preventives.toDouble() / realisees.size * 100.0
    }

    /** Coût total de la maintenance réalisée sur une période. */
    fun coutMaintenance(
        interventions: List<InterventionEntity>,
        debut: Long = Long.MIN_VALUE,
        fin: Long = Long.MAX_VALUE,
    ): Double = interventions
        .filter { it.statut == StatutIntervention.REALISEE.name && it.date in debut..fin }
        .sumOf { it.cout }

    // --- Lectures défensives et libellés ---

    fun gravite(nom: String?): GraviteNc =
        GraviteNc.entries.firstOrNull { it.name == nom } ?: GraviteNc.MINEURE

    fun origine(nom: String?): OrigineNc =
        OrigineNc.entries.firstOrNull { it.name == nom } ?: OrigineNc.INTERNE

    fun statutNc(nom: String?): StatutNc =
        StatutNc.entries.firstOrNull { it.name == nom } ?: StatutNc.OUVERTE

    fun typeEquipement(nom: String?): TypeEquipement =
        TypeEquipement.entries.firstOrNull { it.name == nom } ?: TypeEquipement.MACHINE

    fun typeIntervention(nom: String?): TypeIntervention =
        TypeIntervention.entries.firstOrNull { it.name == nom } ?: TypeIntervention.PREVENTIVE

    fun libelleGravite(gravite: GraviteNc): Int = when (gravite) {
        GraviteNc.MINEURE -> R.string.qua_gravite_mineure
        GraviteNc.MAJEURE -> R.string.qua_gravite_majeure
        GraviteNc.CRITIQUE -> R.string.qua_gravite_critique
    }

    fun libelleOrigine(origine: OrigineNc): Int = when (origine) {
        OrigineNc.PRODUCTION -> R.string.qua_origine_production
        OrigineNc.RECEPTION -> R.string.qua_origine_reception
        OrigineNc.CLIENT -> R.string.qua_origine_client
        OrigineNc.INTERNE -> R.string.qua_origine_interne
    }

    fun libelleStatutNc(statut: StatutNc): Int = when (statut) {
        StatutNc.OUVERTE -> R.string.qua_statut_ouverte
        StatutNc.EN_COURS -> R.string.qua_statut_en_cours
        StatutNc.RESOLUE -> R.string.qua_statut_resolue
    }

    fun libelleTypeEquipement(type: TypeEquipement): Int = when (type) {
        TypeEquipement.MACHINE -> R.string.mai_type_machine
        TypeEquipement.VEHICULE -> R.string.mai_type_vehicule
        TypeEquipement.INSTALLATION -> R.string.mai_type_installation
        TypeEquipement.OUTILLAGE -> R.string.mai_type_outillage
        TypeEquipement.INFORMATIQUE -> R.string.mai_type_informatique
    }

    fun libelleTypeIntervention(type: TypeIntervention): Int = when (type) {
        TypeIntervention.PREVENTIVE -> R.string.mai_type_preventive
        TypeIntervention.CORRECTIVE -> R.string.mai_type_corrective
    }
}
