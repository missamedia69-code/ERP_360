package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** État d'avancement d'un projet. */
enum class EtatProjet(val libelleRes: Int, val ordre: Int) {
    EN_PREPARATION(R.string.prj_etat_preparation, 0),
    EN_COURS(R.string.prj_etat_en_cours, 1),
    LIVRE(R.string.prj_etat_livre, 2),
    SUSPENDU(R.string.prj_etat_suspendu, 3),
}

/**
 * Détail d'un projet, conservé avec la pièce `PROJETS`.
 *
 * Le budget et le consommé vivent ici : ce sont eux qui disent si un projet
 * tient sa promesse économique, information qu'aucune autre table ne porte.
 */
@Serializable
data class ProjetPayload(
    val schemaVersion: Int = 1,
    val nom: String,
    val clientId: Long = 0,
    val clientName: String? = null,
    val responsable: String? = null,
    /** Budget alloué, dans la devise de l'entreprise. */
    val budget: Double = 0.0,
    /** Dépenses déjà engagées sur le projet. */
    val consomme: Double = 0.0,
    /** Avancement déclaré, de 0 à 100. */
    val avancement: Int = 0,
    val dateDebut: Long? = null,
    val echeance: Long? = null,
    /** Nom stable de [EtatProjet]. */
    val etat: String = EtatProjet.EN_PREPARATION.name,
    val note: String? = null,
)

object ProjetCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: ProjetPayload): String =
        json.encodeToString(ProjetPayload.serializer(), payload)

    fun decode(valeur: String?): ProjetPayload? = valeur?.let {
        runCatching { json.decodeFromString(ProjetPayload.serializer(), it) }.getOrNull()
    }
}

/** Un projet prêt pour l'affichage. */
data class Projet(
    val record: OperationRecordEntity,
    val payload: ProjetPayload,
) {
    val annule: Boolean get() = record.status == OperationStatus.CANCELLED.name
    val etat: EtatProjet get() = ProjetRules.etat(payload.etat)
    val resteBudget: Double get() = payload.budget - payload.consomme
    val depasse: Boolean get() = payload.budget > 0 && payload.consomme > payload.budget
}

/** Compteur d'un état, pour la barre de synthèse. */
data class CompteurProjet(val etat: EtatProjet, val nombre: Int)

/**
 * Règles des projets — sans dépendance Android ni Room.
 */
object ProjetRules {

    /** Avancement borné : une saisie hors bornes est ramenée dans l'intervalle. */
    fun avancementValide(valeur: Int): Int = valeur.coerceIn(0, 100)

    fun nomValide(texte: String): Boolean = texte.trim().length >= 2

    fun etat(nom: String?): EtatProjet =
        EtatProjet.entries.firstOrNull { it.name == nom } ?: EtatProjet.EN_PREPARATION

    /**
     * Part du budget consommée, en pourcentage. Sans budget défini, la question
     * n'a pas de sens : on renvoie zéro plutôt qu'une division par zéro.
     */
    fun consommationBudget(payload: ProjetPayload): Double =
        if (payload.budget <= 0.0) 0.0 else payload.consomme / payload.budget * 100.0

    /**
     * Écart entre la consommation du budget et l'avancement déclaré.
     *
     * C'est l'indicateur qui compte : un projet à 30 % d'avancement ayant brûlé
     * 80 % du budget va dans le mur, même si aucune de ces deux valeurs prise
     * isolément n'alerte. Positif = dérive.
     */
    fun derive(payload: ProjetPayload): Double =
        consommationBudget(payload) - payload.avancement.toDouble()

    /** Seuil au-delà duquel la dérive mérite d'être signalée. */
    const val SEUIL_DERIVE = 15.0

    fun enDerive(projets: List<Projet>): List<Projet> = projets
        .filterNot { it.annule || it.etat == EtatProjet.LIVRE }
        .filter { it.payload.budget > 0 && derive(it.payload) > SEUIL_DERIVE }

    /** Projets en retard : échéance dépassée sans livraison. */
    fun enRetard(projets: List<Projet>, maintenant: Long): List<Projet> = projets
        .filterNot { it.annule || it.etat == EtatProjet.LIVRE }
        .filter { projet -> projet.payload.echeance?.let { it < maintenant } == true }

    /** À traiter d'abord, livré ensuite, annulé en dernier. */
    fun trier(projets: List<Projet>): List<Projet> = projets.sortedWith(
        compareBy<Projet> { it.annule }
            .thenBy { it.etat.ordre }
            .thenByDescending { derive(it.payload) },
    )

    fun compteurs(projets: List<Projet>): List<CompteurProjet> = projets
        .filterNot { it.annule }
        .groupingBy { it.etat }
        .eachCount()
        .map { (etat, nombre) -> CompteurProjet(etat, nombre) }
        .sortedBy { it.etat.ordre }

    /** Budget total engagé sur les projets vivants. */
    fun budgetTotal(projets: List<Projet>): Double = projets
        .filterNot { it.annule }
        .sumOf { it.payload.budget }

    /** Dépenses cumulées sur les projets vivants. */
    fun consommeTotal(projets: List<Projet>): Double = projets
        .filterNot { it.annule }
        .sumOf { it.payload.consomme }

    /** Avancement moyen des projets en cours, pondéré par rien : chacun compte pour un. */
    fun avancementMoyen(projets: List<Projet>): Double {
        val actifs = projets.filterNot { it.annule || it.etat == EtatProjet.LIVRE }
        if (actifs.isEmpty()) return 0.0
        return actifs.sumOf { it.payload.avancement.toDouble() } / actifs.size
    }
}
