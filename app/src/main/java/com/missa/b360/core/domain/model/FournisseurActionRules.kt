package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurStatus

/** Une facture à payer, présentée avec le nom du fournisseur et son retard. */
data class ActionFacture(
    val facture: FournisseurFactureOuverte,
    val fournisseurNom: String,
    val joursRetard: Int,
)

/** Contenu du Tableau d'action : ce qu'il faut payer, régulariser ou débloquer. */
data class ActionBoard(
    val detteTotale: Double = 0.0,
    val enRetard: Double = 0.0,
    val aPayerSousSeptJours: Double = 0.0,
    val facturesEnRetard: List<ActionFacture> = emptyList(),
    val facturesBientot: List<ActionFacture> = emptyList(),
    val aRegulariser: List<FournisseurLigne> = emptyList(),
    val bloques: List<FournisseurLigne> = emptyList(),
) {
    val vide: Boolean
        get() = facturesEnRetard.isEmpty() && facturesBientot.isEmpty() && aRegulariser.isEmpty() && bloques.isEmpty()
}

object FournisseurActionRules {
    const val HORIZON_JOURS = 7
    private const val JOUR_MS = 86_400_000L

    /**
     * @param lignes portefeuille complet.
     * @param factures factures d'achat ouvertes de [FournisseurAchatMetrics] (même source que les soldes).
     */
    fun construire(lignes: List<FournisseurLigne>, factures: List<FournisseurFactureOuverte>, now: Long): ActionBoard {
        val noms = lignes.associate { it.fournisseur.id to it.fournisseur.nom }
        val horizon = now + HORIZON_JOURS * JOUR_MS
        val presentees = factures.map { f ->
            ActionFacture(f, noms[f.fournisseurId] ?: "#${f.fournisseurId}", AgedBalanceRules.joursDeRetard(f.dueAt, now))
        }
        val enRetard = presentees.filter { it.facture.dueAt < now }
            .sortedWith(compareByDescending<ActionFacture> { it.joursRetard }.thenByDescending { it.facture.outstanding })
        val bientot = presentees.filter { it.facture.dueAt in now..horizon }
            .sortedWith(compareBy<ActionFacture> { it.facture.dueAt }.thenByDescending { it.facture.outstanding })
        val actifs = lignes.filter { it.fournisseur.statut != FournisseurStatus.ARCHIVE }
        return ActionBoard(
            detteTotale = presentees.sumOf { it.facture.outstanding },
            enRetard = enRetard.sumOf { it.facture.outstanding },
            aPayerSousSeptJours = bientot.sumOf { it.facture.outstanding },
            facturesEnRetard = enRetard,
            facturesBientot = bientot,
            aRegulariser = actifs.filter { it.aptitude.niveau == SupplierReadinessLevel.A_REGULARISER }
                .sortedWith(compareByDescending<FournisseurLigne> { it.aptitude.motifs.size }.thenByDescending { it.dette }),
            bloques = actifs.filter { it.fournisseur.statut == FournisseurStatus.BLOQUE }
                .sortedByDescending { it.dette },
        )
    }
}
