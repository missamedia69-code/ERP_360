package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ClientStatus

/** Niveau de risque crédit d'un client, du plus sûr au plus grave (l'ordre sert aux comparaisons). */
enum class RiskLevel { NORMAL, ATTENTION, ELEVE, BLOQUE }

/** Raisons d'un niveau de risque ou d'un avertissement ; l'interface les traduit en 5 langues. */
enum class CreditReason {
    STATUT_BLOQUE_CREDIT,
    STATUT_BLOQUE_ADMINISTRATIF,
    STATUT_INACTIF,
    FICHE_INCOMPLETE,
    SOUS_SURVEILLANCE,
    LIMITE_ATTEINTE,
    LIMITE_PROCHE,
    LIMITE_DEPASSEE_PAR_VENTE,
    RETARD_CRITIQUE,
    RETARD_MODERE,
    MONTANT_INVALIDE,
}

/** Seuils par défaut ; regroupés ici pour pouvoir devenir réglables plus tard. */
data class CreditPolicyConfig(
    /** Utilisation de la limite (en %) à partir de laquelle le risque passe à ATTENTION. */
    val attentionUtilisationPct: Double = 80.0,
    /** Utilisation de la limite (en %) à partir de laquelle le risque passe à ÉLEVÉ. */
    val eleveUtilisationPct: Double = 100.0,
    /** Retard maximal (en jours, strictement dépassé) pour passer à ATTENTION. */
    val attentionRetardJours: Int = 30,
    /** Retard maximal (en jours, strictement dépassé) pour passer à ÉLEVÉ. */
    val eleveRetardJours: Int = 60,
)

/** Situation de crédit d'un client, issue de `client_balances` et de sa fiche. */
data class CreditInput(
    val statut: ClientStatus,
    /** Limite de crédit ; `null` = illimitée. */
    val limiteCredit: Double?,
    val encours: Double,
    val enRetard: Double,
    val joursRetardMax: Int,
)

data class CreditAssessment(
    val risque: RiskLevel,
    /** Utilisation de la limite en % ; `null` si la limite est illimitée. */
    val utilisationPct: Double?,
    /** Raisons, de la plus grave à la moins grave. */
    val raisons: List<CreditReason>,
)

/** Décision de vente à crédit. */
sealed class SaleVerdict {
    data object Allow : SaleVerdict()
    data class Warn(val raison: CreditReason, val risque: RiskLevel) : SaleVerdict()
    data class Block(val raison: CreditReason) : SaleVerdict()
}

/**
 * Modèle de décision crédit (objet pur, testé en JVM).
 *
 * Le risque est calculé et affiché automatiquement, mais il ne bloque jamais une vente à lui
 * seul : seul le statut du client (bloqué, inactif, archivé), changé à la main et confirmé,
 * produit un [SaleVerdict.Block]. Un risque ATTENTION ou ÉLEVÉ produit un [SaleVerdict.Warn].
 */
object CreditPolicy {
    private const val EPSILON = 1e-9

    private val statutsBloques = setOf(ClientStatus.BLOQUE_CREDIT, ClientStatus.BLOQUE_ADMINISTRATIF)
    private val statutsHorsService = setOf(ClientStatus.INACTIF, ClientStatus.ARCHIVE, ClientStatus.DESACTIVE)

    private fun montantSain(value: Double): Double = if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0

    /** Utilisation de la limite en %, `null` si illimitée ; une limite nulle avec encours = saturée. */
    fun utilisationPct(limiteCredit: Double?, encours: Double): Double? {
        if (limiteCredit == null) return null
        val limite = montantSain(limiteCredit)
        val dette = montantSain(encours)
        return when {
            limite <= EPSILON -> if (dette > EPSILON) Double.POSITIVE_INFINITY else 0.0
            else -> dette / limite * 100.0
        }
    }

    fun evaluate(input: CreditInput, config: CreditPolicyConfig = CreditPolicyConfig()): CreditAssessment {
        val utilisation = utilisationPct(input.limiteCredit, input.encours)
        // Un retard n'existe que s'il reste un montant échu : on ignore des jours orphelins.
        val jours = if (montantSain(input.enRetard) > EPSILON) input.joursRetardMax.coerceAtLeast(0) else 0

        if (input.statut in statutsBloques) {
            val raison = if (input.statut == ClientStatus.BLOQUE_CREDIT) {
                CreditReason.STATUT_BLOQUE_CREDIT
            } else {
                CreditReason.STATUT_BLOQUE_ADMINISTRATIF
            }
            return CreditAssessment(RiskLevel.BLOQUE, utilisation, listOf(raison))
        }

        val eleve = mutableListOf<CreditReason>()
        val attention = mutableListOf<CreditReason>()
        if (utilisation != null) {
            when {
                utilisation >= config.eleveUtilisationPct -> eleve += CreditReason.LIMITE_ATTEINTE
                utilisation >= config.attentionUtilisationPct -> attention += CreditReason.LIMITE_PROCHE
            }
        }
        when {
            jours > config.eleveRetardJours -> eleve += CreditReason.RETARD_CRITIQUE
            jours > config.attentionRetardJours -> attention += CreditReason.RETARD_MODERE
        }
        if (input.statut == ClientStatus.SOUS_SURVEILLANCE) attention += CreditReason.SOUS_SURVEILLANCE

        val risque = when {
            eleve.isNotEmpty() -> RiskLevel.ELEVE
            attention.isNotEmpty() -> RiskLevel.ATTENTION
            else -> RiskLevel.NORMAL
        }
        return CreditAssessment(risque, utilisation, eleve + attention)
    }

    /**
     * Décision pour une vente de [montantVente] dont [montantRegle] est payé comptant ; seule la
     * part restante ajoute à l'encours. Une vente entièrement réglée n'expose à aucun risque de
     * crédit : elle reste possible même pour un client bloqué crédit.
     */
    fun canSell(
        input: CreditInput,
        montantVente: Double,
        montantRegle: Double = 0.0,
        config: CreditPolicyConfig = CreditPolicyConfig(),
    ): SaleVerdict {
        if (!montantVente.isFinite() || montantVente <= 0.0 ||
            !montantRegle.isFinite() || montantRegle < 0.0 || montantRegle > montantVente + EPSILON
        ) return SaleVerdict.Block(CreditReason.MONTANT_INVALIDE)

        if (input.statut in statutsHorsService) return SaleVerdict.Block(CreditReason.STATUT_INACTIF)
        if (input.statut == ClientStatus.BLOQUE_ADMINISTRATIF) {
            return SaleVerdict.Block(CreditReason.STATUT_BLOQUE_ADMINISTRATIF)
        }

        val partCredit = (montantVente - montantRegle).coerceAtLeast(0.0)
        if (partCredit <= EPSILON) return SaleVerdict.Allow
        if (input.statut == ClientStatus.BLOQUE_CREDIT) return SaleVerdict.Block(CreditReason.STATUT_BLOQUE_CREDIT)
        // Une fiche en brouillon ou à compléter ne peut pas porter de créance (comme `venteAutorisee`).
        if (input.statut == ClientStatus.BROUILLON || input.statut == ClientStatus.A_COMPLETER) {
            return SaleVerdict.Block(CreditReason.FICHE_INCOMPLETE)
        }

        val actuel = evaluate(input, config)
        if (actuel.risque >= RiskLevel.ELEVE) {
            return SaleVerdict.Warn(actuel.raisons.first(), actuel.risque)
        }
        val limite = input.limiteCredit
        if (limite != null && montantSain(input.encours) + partCredit > montantSain(limite) + EPSILON) {
            return SaleVerdict.Warn(CreditReason.LIMITE_DEPASSEE_PAR_VENTE, RiskLevel.ELEVE)
        }
        if (actuel.risque == RiskLevel.ATTENTION) {
            return SaleVerdict.Warn(actuel.raisons.first(), actuel.risque)
        }
        return SaleVerdict.Allow
    }
}
