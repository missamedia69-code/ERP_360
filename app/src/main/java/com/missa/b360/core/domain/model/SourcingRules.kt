package com.missa.b360.core.domain.model

import kotlin.math.round

/** Prix de liaison d'un article chez un fournisseur (extrait de la fiche catalogue). */
data class SupplierItemPrice(
    val prixUnitaire: Double,
    val debutValidite: Long? = null,
    val finValidite: Long? = null,
    val actif: Boolean = true,
)

data class SourcingCandidate(
    val fournisseurId: Long,
    val nom: String,
    val peutCommander: Boolean,
    val aptitude: SupplierReadinessLevel,
    /** Prix retenu (voir [SourcingRules.prixRetenu]) ; `null` = inconnu. */
    val prix: Double?,
    /** Délai de livraison en jours ; 0 = inconnu. */
    val delaiJours: Int,
    val quantiteMin: Double,
    /** Score de fiabilité 0–100, `null` si non significatif. */
    val fiabilite: Int?,
    val prefere: Boolean,
)

enum class SourcingExclusion { NON_COMMANDABLE, BLOQUE, QTE_MIN_NON_ATTEINTE, SANS_PRIX }

enum class SourcingReason { MOINS_CHER, PLUS_RAPIDE, PLUS_FIABLE, PREFERE }

data class SourcingRanking(
    val candidat: SourcingCandidate,
    /** `null` si le candidat n'est pas recommandable. */
    val rang: Int?,
    val score: Double?,
    val raisons: List<SourcingReason>,
    val exclusion: SourcingExclusion?,
) {
    val recommande: Boolean get() = rang != null
}

/** Choix du fournisseur pour un article : prix, délai et fiabilité mesurée. */
object SourcingRules {

    private const val POIDS_PRIX = 0.45
    private const val POIDS_DELAI = 0.25
    private const val POIDS_FIABILITE = 0.30
    private const val NEUTRE = 50.0

    /**
     * Prix de liaison valide à [now] (actif, dans sa période), sinon dernier prix payé, sinon
     * `null`. Un prix nul, négatif ou non fini n'est jamais retenu.
     */
    fun prixRetenu(liaison: SupplierItemPrice?, dernierPrixPaye: Double?, now: Long): Double? {
        val valide = liaison?.takeIf {
            it.actif && it.prixUnitaire.isFinite() && it.prixUnitaire > 0.0 &&
                (it.debutValidite == null || it.debutValidite <= now) &&
                (it.finValidite == null || now <= it.finValidite)
        }
        if (valide != null) return valide.prixUnitaire
        return dernierPrixPaye?.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun exclusion(c: SourcingCandidate, quantite: Double): SourcingExclusion? = when {
        !c.peutCommander -> SourcingExclusion.NON_COMMANDABLE
        c.aptitude == SupplierReadinessLevel.BLOQUE -> SourcingExclusion.BLOQUE
        c.quantiteMin > quantite -> SourcingExclusion.QTE_MIN_NON_ATTEINTE
        c.prix == null || !c.prix.isFinite() || c.prix <= 0.0 -> SourcingExclusion.SANS_PRIX
        else -> null
    }

    @Suppress("UNUSED_PARAMETER")
    fun classer(candidats: List<SourcingCandidate>, quantiteVoulue: Double, now: Long): List<SourcingRanking> {
        val quantite = quantiteVoulue.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val evalues = candidats.map { it to exclusion(it, quantite) }
        val eligibles = evalues.filter { it.second == null }.map { it.first }

        val meilleurPrix = eligibles.mapNotNull { it.prix }.minOrNull()
        val meilleurDelai = eligibles.map { it.delaiJours }.filter { it > 0 }.minOrNull()
        val meilleureFiabilite = eligibles.mapNotNull { it.fiabilite }.maxOrNull()

        fun score(c: SourcingCandidate): Double {
            val prix = c.prix ?: return 0.0
            val sPrix = 100.0 * (meilleurPrix ?: prix) / prix
            val sDelai = if (c.delaiJours <= 0 || meilleurDelai == null) NEUTRE else 100.0 * meilleurDelai / c.delaiJours
            val sFiabilite = c.fiabilite?.toDouble() ?: NEUTRE
            return POIDS_PRIX * sPrix + POIDS_DELAI * sDelai + POIDS_FIABILITE * sFiabilite
        }

        val classes = eligibles
            .map { it to score(it) }
            .sortedWith(
                compareByDescending<Pair<SourcingCandidate, Double>> { round(it.second * 1e6) }
                    .thenByDescending { it.first.prefere }
                    .thenBy { it.first.prix ?: Double.MAX_VALUE }
                    .thenBy { it.first.nom.lowercase() },
            )
            .mapIndexed { index, (c, s) ->
                SourcingRanking(
                    candidat = c,
                    rang = index + 1,
                    score = s,
                    raisons = buildList {
                        if (c.prix != null && c.prix == meilleurPrix) add(SourcingReason.MOINS_CHER)
                        if (c.delaiJours > 0 && c.delaiJours == meilleurDelai) add(SourcingReason.PLUS_RAPIDE)
                        if (c.fiabilite != null && c.fiabilite == meilleureFiabilite) add(SourcingReason.PLUS_FIABLE)
                        if (c.prefere) add(SourcingReason.PREFERE)
                    },
                    exclusion = null,
                )
            }

        val exclus = evalues.filter { it.second != null }
            .sortedBy { it.first.nom.lowercase() }
            .map { (c, motif) -> SourcingRanking(c, rang = null, score = null, raisons = emptyList(), exclusion = motif) }

        return classes + exclus
    }
}
