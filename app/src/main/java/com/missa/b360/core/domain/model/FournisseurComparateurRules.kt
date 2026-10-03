package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurItemEntity

/** Comparateur : transforme les liaisons article ↔ fournisseur en candidats pour [SourcingRules]. */
object FournisseurComparateurRules {

    /**
     * Un candidat par liaison active dont le fournisseur est connu. Le prix est celui de la liaison
     * s'il est valide à [now] ; sinon `null` (le dernier prix payé n'est pas encore tracé : le
     * candidat est alors écarté « sans prix », jamais deviné).
     */
    fun candidats(
        lignes: List<FournisseurLigne>,
        liaisons: List<FournisseurItemEntity>,
        now: Long,
    ): List<SourcingCandidate> {
        val parId = lignes.associateBy { it.fournisseur.id }
        return liaisons.filter { it.actif }.mapNotNull { liaison ->
            val ligne = parId[liaison.fournisseurId] ?: return@mapNotNull null
            SourcingCandidate(
                fournisseurId = ligne.fournisseur.id,
                nom = ligne.fournisseur.nom,
                peutCommander = FournisseurRules.peutCommander(ligne.fournisseur.statut),
                aptitude = ligne.aptitude.niveau,
                prix = SourcingRules.prixRetenu(
                    SupplierItemPrice(liaison.prixUnitaire, liaison.debutValidite, liaison.finValidite, liaison.actif),
                    dernierPrixPaye = null,
                    now = now,
                ),
                delaiJours = liaison.delaiJours,
                quantiteMin = liaison.quantiteMin,
                fiabilite = ligne.note,
                prefere = liaison.prefere,
            )
        }
    }

    /** Nombre de fournisseurs actifs liés à chaque article (pour choisir l'article à comparer). */
    fun fournisseursParArticle(liaisons: List<FournisseurItemEntity>): Map<Long, Int> =
        liaisons.filter { it.actif }.groupBy { it.productId }.mapValues { (_, l) -> l.map { it.fournisseurId }.distinct().size }
}
