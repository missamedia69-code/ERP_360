package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FollowupStatus

/** Résultat du contrôle d'une promesse de paiement saisie. */
enum class PromiseCheck { VALIDE, MONTANT_INVALIDE, MONTANT_SUPERIEUR_ENCOURS, DATE_INVALIDE }

/** Règles pures des promesses de paiement et des relances. */
object ClientFollowupRules {
    private const val EPSILON = 1e-9
    private const val DAY_MS = ClientMetricsRules.DAY_MS

    /** Délai minimal entre deux relances d'un même client, en jours. */
    const val DELAI_ENTRE_RELANCES_JOURS: Int = 7

    /**
     * Une promesse doit porter sur un montant positif, au plus égal à l'encours, et sur une date
     * qui n'est pas antérieure au début de la journée courante ([debutJour], en millisecondes).
     */
    fun verifierPromesse(montant: Double, date: Long, encours: Double, debutJour: Long): PromiseCheck = when {
        !montant.isFinite() || montant <= 0.0 -> PromiseCheck.MONTANT_INVALIDE
        !encours.isFinite() || montant > encours + EPSILON -> PromiseCheck.MONTANT_SUPERIEUR_ENCOURS
        date < debutJour -> PromiseCheck.DATE_INVALIDE
        else -> PromiseCheck.VALIDE
    }

    /**
     * Statut à jour d'une promesse.
     *
     * - Un statut déjà définitif n'évolue plus.
     * - Tenue dès que les encaissements postérieurs à la promesse couvrent son montant, même
     *   avant la date.
     * - Non tenue quand la journée promise est écoulée sans que le montant soit couvert (un
     *   encaissement partiel ne suffit pas).
     */
    fun statutPromesse(
        statut: FollowupStatus,
        promesseDate: Long,
        promesseMontant: Double,
        encaisseDepuisPromesse: Double,
        now: Long,
    ): FollowupStatus {
        if (statut != FollowupStatus.OUVERT) return statut
        val encaisse = if (encaisseDepuisPromesse.isFinite()) encaisseDepuisPromesse else 0.0
        if (promesseMontant.isFinite() && promesseMontant > 0.0 && encaisse >= promesseMontant - EPSILON) {
            return FollowupStatus.TENU
        }
        val finDeJournee = runCatching { Math.addExact(promesseDate, DAY_MS) }.getOrDefault(Long.MAX_VALUE)
        return if (now >= finDeJournee) FollowupStatus.NON_TENU else FollowupStatus.OUVERT
    }

    /**
     * Un client est « à relancer » s'il a un montant échu, qu'aucune promesse ouverte n'est encore
     * dans les délais, et que la dernière relance remonte à au moins [delaiJours] jours.
     */
    fun aRelancer(
        enRetard: Double,
        promesseOuverteJusquA: Long?,
        derniereRelanceAt: Long?,
        now: Long,
        delaiJours: Int = DELAI_ENTRE_RELANCES_JOURS,
    ): Boolean {
        if (!enRetard.isFinite() || enRetard <= EPSILON) return false
        if (promesseOuverteJusquA != null && now < promesseOuverteJusquA + DAY_MS) return false
        if (derniereRelanceAt != null && now - derniereRelanceAt < delaiJours.coerceAtLeast(0) * DAY_MS) return false
        return true
    }
}
