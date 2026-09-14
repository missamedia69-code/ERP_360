package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Étape d'une prestation de service. */
enum class EtapePrestation(val libelleRes: Int, val ordre: Int) {
    PLANIFIEE(R.string.srv_etape_planifiee, 0),
    EN_COURS(R.string.srv_etape_en_cours, 1),
    TERMINEE(R.string.srv_etape_terminee, 2),
}

/** Mode de facturation d'une prestation. */
enum class ModeFacturation(val libelleRes: Int) {
    FORFAIT(R.string.srv_mode_forfait),
    HORAIRE(R.string.srv_mode_horaire),
}

/**
 * Détail d'une prestation, conservé avec la pièce `SERVICES`.
 *
 * Comme la facture et le bon de livraison, le métier vit dans le payload : la
 * table de pièces reste générique et aucune migration n'est nécessaire.
 */
@Serializable
data class PrestationPayload(
    val schemaVersion: Int = 1,
    val clientId: Long = 0,
    val clientName: String,
    val intitule: String,
    val intervenant: String? = null,
    val lieu: String? = null,
    /** Nom stable de [ModeFacturation]. */
    val mode: String = ModeFacturation.FORFAIT.name,
    /** Prix forfaitaire, ou tarif horaire selon [mode]. */
    val tarif: Double = 0.0,
    val heures: Double = 0.0,
    /** Nom stable de [EtapePrestation]. */
    val etape: String = EtapePrestation.PLANIFIEE.name,
    val datePrevue: Long? = null,
    val dateRealisation: Long? = null,
    val note: String? = null,
)

object PrestationCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: PrestationPayload): String =
        json.encodeToString(PrestationPayload.serializer(), payload)

    fun decode(valeur: String?): PrestationPayload? = valeur?.let {
        runCatching { json.decodeFromString(PrestationPayload.serializer(), it) }.getOrNull()
    }
}

/** Une prestation prête pour l'affichage. */
data class Prestation(
    val record: OperationRecordEntity,
    val payload: PrestationPayload,
) {
    val annulee: Boolean get() = record.status == OperationStatus.CANCELLED.name
    val etape: EtapePrestation get() = PrestationRules.etape(payload.etape)
    val montant: Double get() = PrestationRules.montant(payload)
}

/** Compteur d'une étape, pour la barre de synthèse. */
data class CompteurPrestation(val etape: EtapePrestation, val nombre: Int)

/**
 * Règles des prestations de services — sans dépendance Android ni Room.
 */
object PrestationRules {

    /** Une prestation sans intitulé ne se retrouve pas dans une liste. */
    fun intituleValide(texte: String): Boolean = texte.trim().length >= 2

    fun etape(nom: String?): EtapePrestation =
        EtapePrestation.entries.firstOrNull { it.name == nom } ?: EtapePrestation.PLANIFIEE

    fun mode(nom: String?): ModeFacturation =
        ModeFacturation.entries.firstOrNull { it.name == nom } ?: ModeFacturation.FORFAIT

    /**
     * Montant dû.
     *
     * Au forfait, le prix est celui convenu, quelles que soient les heures
     * passées — c'est tout l'intérêt du forfait pour le client. À l'heure, le
     * montant suit le temps réellement consacré.
     */
    fun montant(payload: PrestationPayload): Double = when (mode(payload.mode)) {
        ModeFacturation.FORFAIT -> payload.tarif
        ModeFacturation.HORAIRE -> payload.tarif * payload.heures
    }.let { if (it.isFinite() && it > 0) Math.round(it * 100.0) / 100.0 else 0.0 }

    /** Étape suivante ; une prestation terminée ne bouge plus. */
    fun etapeSuivante(courante: EtapePrestation): EtapePrestation? = when (courante) {
        EtapePrestation.PLANIFIEE -> EtapePrestation.EN_COURS
        EtapePrestation.EN_COURS -> EtapePrestation.TERMINEE
        EtapePrestation.TERMINEE -> null
    }

    /** Avance la prestation et date la réalisation au moment de la clôture. */
    fun avancer(payload: PrestationPayload, maintenant: Long): PrestationPayload? {
        val suivante = etapeSuivante(etape(payload.etape)) ?: return null
        return payload.copy(
            etape = suivante.name,
            dateRealisation = if (suivante == EtapePrestation.TERMINEE) maintenant else null,
        )
    }

    /** À faire d'abord, terminé ensuite, annulé en dernier. */
    fun trier(prestations: List<Prestation>): List<Prestation> = prestations.sortedWith(
        compareBy<Prestation> { it.annulee }
            .thenBy { it.etape.ordre }
            .thenByDescending { it.record.createdAt },
    )

    fun compteurs(prestations: List<Prestation>): List<CompteurPrestation> = prestations
        .filterNot { it.annulee }
        .groupingBy { it.etape }
        .eachCount()
        .map { (etape, nombre) -> CompteurPrestation(etape, nombre) }
        .sortedBy { it.etape.ordre }

    /** Prestations engagées mais pas encore terminées. */
    fun enCours(prestations: List<Prestation>): List<Prestation> =
        prestations.filterNot { it.annulee || it.etape == EtapePrestation.TERMINEE }

    /**
     * Chiffre d'affaires des prestations **terminées** : une intervention
     * planifiée n'est pas un revenu acquis, la compter serait se mentir.
     */
    fun chiffreRealise(prestations: List<Prestation>): Double = prestations
        .filterNot { it.annulee }
        .filter { it.etape == EtapePrestation.TERMINEE }
        .sumOf { it.montant }

    /** Montant engagé mais non encore réalisé — le carnet de commandes. */
    fun carnet(prestations: List<Prestation>): Double = enCours(prestations).sumOf { it.montant }

    /** Heures facturables cumulées sur les prestations terminées. */
    fun heuresRealisees(prestations: List<Prestation>): Double = prestations
        .filterNot { it.annulee }
        .filter { it.etape == EtapePrestation.TERMINEE }
        .sumOf { it.payload.heures }
}
