package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.AbsenceEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * Bulletin de paie (spec §Paie, doc P) — lignes par employé actif.
 * Le bulletin est une pièce RH (module RH) : aucun mouvement de stock,
 * aucune écriture de caisse (le règlement est une opération Finance).
 */
@Serializable
data class PayslipLine(
    val employeeId: Long,
    val nom: String,
    val code: String,
    /** Salaire de base du mois. */
    val base: Double,
    /** Primes saisies (structure anticipée — 0 par défaut). */
    val primes: Double = 0.0,
    /** Retenues saisies (structure anticipée — 0 par défaut). */
    val retenues: Double = 0.0,
    /** Jours d'absence du mois (prorata). */
    val absencesJours: Double = 0.0,
    /** Montant déduit pour les absences. */
    val absencesMontant: Double = 0.0,
    /** Avances de salaire déduites sur le mois. */
    val avancement: Double = 0.0,
    val net: Double,
)

@Serializable
data class PayslipPayload(
    val schemaVersion: Int = 1,
    val mois: Int,
    val annee: Int,
    val lignes: List<PayslipLine>,
    val totalNet: Double,
)

/** Avance de salaire (doc AV) — déduite de la paie du mois de création. */
@Serializable
data class AdvancePayload(
    val schemaVersion: Int = 1,
    val employeeId: Long,
    val employeeNom: String,
    val motif: String? = null,
)

object AdvanceCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: AdvancePayload): String =
        json.encodeToString(AdvancePayload.serializer(), payload)

    fun decode(value: String?): AdvancePayload? = value?.let {
        runCatching { json.decodeFromString(AdvancePayload.serializer(), it) }.getOrNull()
    }
}

object PayslipCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: PayslipPayload): String =
        json.encodeToString(PayslipPayload.serializer(), payload)

    fun decode(value: String?): PayslipPayload? = value?.let {
        runCatching { json.decodeFromString(PayslipPayload.serializer(), it) }.getOrNull()
    }
}

/**
 * Règles pures de la paie — testables sans base.
 * L'absence est déduite au prorata journalier :
 * `salaireBase / joursMensuels × jours absents`.
 */
object PaieRules {

    const val MS_PAR_JOUR = 86_400_000L

    fun salaireJour(base: Double, joursMensuels: Double): Double =
        if (joursMensuels <= 0.0) 0.0 else base / joursMensuels

    /**
     * Jours d'absence tombant dans le mois (mois/année 1-based). Une absence de
     * `dureeJours` jours à partir de `dateDebut` couvre les jours relatifs 0..duree-1.
     */
    fun absencesDuMois(absences: List<AbsenceEntity>, mois: Int, annee: Int): Double {
        val cal = Calendar.getInstance().apply {
            clear()
            set(annee, mois - 1, 1)
        }
        val debutMois = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val finMois = cal.timeInMillis
        return absences.sumOf { absence ->
            val jours = absence.dureeJours.toInt().coerceAtLeast(1)
            (0 until jours).count { j ->
                absence.dateDebut + j * MS_PAR_JOUR in debutMois until finMois
            }.toDouble()
        }
    }

    /** Net = base + primes − retenues − absences − avancement (peut rester négatif). */
    fun net(
        base: Double,
        primes: Double,
        retenues: Double,
        absencesMontant: Double,
        avancement: Double,
    ): Double = base + primes - retenues - absencesMontant - avancement
}
