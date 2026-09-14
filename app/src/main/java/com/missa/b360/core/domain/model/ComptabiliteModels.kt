package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SensMouvement

/**
 * Nature comptable d'une écriture, en rubriques compréhensibles par un
 * dirigeant de TPE.
 *
 * Ce n'est volontairement pas un plan comptable OHADA complet : une PME qui
 * découvre son résultat mensuel a besoin de six rubriques justes, pas de deux
 * cents comptes qu'elle remplira mal. Les comptes normalisés viendront avec
 * l'export vers l'expert-comptable.
 */
enum class RubriqueComptable(val produit: Boolean, val libelleRes: Int) {
    VENTES(true, R.string.cpt_rub_ventes),
    AUTRES_PRODUITS(true, R.string.cpt_rub_autres_produits),
    ACHATS(false, R.string.cpt_rub_achats),
    SALAIRES(false, R.string.cpt_rub_salaires),
    CHARGES_EXTERNES(false, R.string.cpt_rub_charges_externes),
    IMPOTS_TAXES(false, R.string.cpt_rub_impots),
}

/** Une ligne du journal, quelle que soit sa provenance. */
data class EcritureComptable(
    val date: Long,
    val libelle: String,
    val rubrique: RubriqueComptable,
    val montant: Double,
    val reference: String? = null,
    val tiers: String? = null,
    /** Vrai quand la ligne vient d'un mouvement de trésorerie encaissé/décaissé. */
    val tresorerie: Boolean = false,
)

/** Total d'une rubrique sur la période, avec sa part dans le sous-total. */
data class TotalRubrique(
    val rubrique: RubriqueComptable,
    val montant: Double,
    val nombre: Int,
)

/** Compte de résultat simplifié de la période. */
data class ResultatPeriode(
    val produits: List<TotalRubrique> = emptyList(),
    val charges: List<TotalRubrique> = emptyList(),
) {
    val totalProduits: Double get() = produits.sumOf { it.montant }
    val totalCharges: Double get() = charges.sumOf { it.montant }
    val resultat: Double get() = totalProduits - totalCharges
    val marge: Double
        get() = if (totalProduits > 0) resultat / totalProduits * 100.0 else 0.0
}

/**
 * Règles de consolidation comptable — sans dépendance Android ni Room, donc
 * vérifiables par des tests unitaires ordinaires.
 *
 * Le principe : la comptabilité **ne saisit rien**. Elle relit les ventes, les
 * achats et la trésorerie déjà enregistrés et les traduit en écritures. Une
 * double saisie serait la garantie de deux vérités contradictoires.
 */
object ComptabiliteRules {

    /**
     * Écritures issues des pièces opérationnelles validées.
     *
     * Les brouillons sont exclus : une pièce non validée n'est pas un
     * engagement, et la faire entrer au résultat gonflerait le chiffre
     * d'affaires d'un devis jamais signé.
     */
    fun ecrituresDesPieces(pieces: List<OperationRecordEntity>): List<EcritureComptable> = pieces
        .asSequence()
        .filter { it.status == OperationStatus.VALIDATED.name }
        .mapNotNull { piece ->
            val montant = piece.amount ?: return@mapNotNull null
            if (montant <= 0.0) return@mapNotNull null
            val rubrique = rubriqueDuModule(piece.module, piece.direction) ?: return@mapNotNull null
            EcritureComptable(
                date = piece.createdAt,
                libelle = piece.title,
                rubrique = rubrique,
                montant = montant,
                reference = piece.reference,
                tiers = piece.counterpart,
            )
        }
        .toList()

    /**
     * Écritures issues de la trésorerie. Les virements internes sont écartés :
     * déplacer de l'argent d'une caisse à une banque n'est ni un produit ni une
     * charge.
     */
    fun ecrituresDeTresorerie(
        mouvements: List<MouvementTresorerieEntity>,
    ): List<EcritureComptable> = mouvements
        .asSequence()
        .filter { it.transfertId == null && it.montant > 0.0 }
        .mapNotNull { mouvement ->
            val rubrique = rubriqueDeCategorie(
                TresorerieRules.categorie(mouvement.categorie),
                TresorerieRules.sens(mouvement.sens),
            ) ?: return@mapNotNull null
            EcritureComptable(
                date = mouvement.date,
                libelle = mouvement.libelle,
                rubrique = rubrique,
                montant = mouvement.montant,
                reference = mouvement.reference,
                tiers = mouvement.tiers,
                tresorerie = true,
            )
        }
        .toList()

    /**
     * Journal consolidé sur une période, du plus récent au plus ancien.
     *
     * Les ventes et achats déjà saisis comme pièces ne sont pas repris depuis la
     * trésorerie : les catégories « Ventes » et « Achats » d'un encaissement
     * correspondent au règlement d'une pièce, pas à une opération nouvelle.
     * Sans cette règle, une facture réglée compterait deux fois.
     */
    fun journal(
        pieces: List<OperationRecordEntity>,
        mouvements: List<MouvementTresorerieEntity>,
        debut: Long,
        fin: Long,
    ): List<EcritureComptable> {
        val desPieces = ecrituresDesPieces(pieces)
        val piecesCouvrentVentes = desPieces.any { it.rubrique == RubriqueComptable.VENTES }
        val piecesCouvrentAchats = desPieces.any { it.rubrique == RubriqueComptable.ACHATS }
        val deTresorerie = ecrituresDeTresorerie(mouvements).filterNot {
            (it.rubrique == RubriqueComptable.VENTES && piecesCouvrentVentes) ||
                (it.rubrique == RubriqueComptable.ACHATS && piecesCouvrentAchats)
        }
        return (desPieces + deTresorerie)
            .filter { it.date in debut..fin }
            .sortedByDescending { it.date }
    }

    /** Totaux par rubrique, du plus lourd au plus léger. */
    fun totaux(ecritures: List<EcritureComptable>, produits: Boolean): List<TotalRubrique> =
        ecritures
            .filter { it.rubrique.produit == produits }
            .groupBy { it.rubrique }
            .map { (rubrique, lignes) ->
                TotalRubrique(rubrique, lignes.sumOf { it.montant }, lignes.size)
            }
            .sortedByDescending { it.montant }

    /** Compte de résultat simplifié : produits, charges, résultat net. */
    fun resultat(ecritures: List<EcritureComptable>): ResultatPeriode = ResultatPeriode(
        produits = totaux(ecritures, produits = true),
        charges = totaux(ecritures, produits = false),
    )

    /**
     * Base de TVA collectée sur les ventes de la période, et son montant au taux
     * de l'entreprise. Les montants saisis sont toutes taxes comprises : la base
     * hors taxes s'en déduit, elle ne s'additionne pas.
     */
    fun tvaCollectee(ecritures: List<EcritureComptable>, taux: Double): Double {
        if (taux <= 0.0) return 0.0
        val ttc = ecritures.filter { it.rubrique == RubriqueComptable.VENTES }.sumOf { it.montant }
        return arrondi(ttc - ttc / (1.0 + taux / 100.0))
    }

    /** TVA déductible sur les achats de la période, même raisonnement. */
    fun tvaDeductible(ecritures: List<EcritureComptable>, taux: Double): Double {
        if (taux <= 0.0) return 0.0
        val ttc = ecritures.filter { it.rubrique == RubriqueComptable.ACHATS }.sumOf { it.montant }
        return arrondi(ttc - ttc / (1.0 + taux / 100.0))
    }

    /** Solde de TVA : positif à payer, négatif à reporter en crédit. */
    fun tvaAPayer(ecritures: List<EcritureComptable>, taux: Double): Double =
        arrondi(tvaCollectee(ecritures, taux) - tvaDeductible(ecritures, taux))

    private fun rubriqueDuModule(module: String, direction: String): RubriqueComptable? =
        when (module) {
            OperationModule.VENTE.name -> RubriqueComptable.VENTES
            OperationModule.SERVICES.name -> RubriqueComptable.VENTES
            OperationModule.ACHATS.name -> RubriqueComptable.ACHATS
            OperationModule.PRODUCTION.name -> RubriqueComptable.ACHATS
            OperationModule.RH.name -> RubriqueComptable.SALAIRES
            OperationModule.FINANCES.name -> when (direction) {
                OperationDirection.IN.name -> RubriqueComptable.AUTRES_PRODUITS
                OperationDirection.OUT.name -> RubriqueComptable.CHARGES_EXTERNES
                else -> null
            }
            // Devis, commandes, livraisons et stock ne créent aucun produit ni
            // charge : ils déplacent des engagements ou des quantités.
            else -> null
        }

    private fun rubriqueDeCategorie(
        categorie: CategorieTresorerie,
        sens: SensMouvement,
    ): RubriqueComptable? = when (categorie) {
        CategorieTresorerie.VENTE -> RubriqueComptable.VENTES
        CategorieTresorerie.ACHAT -> RubriqueComptable.ACHATS
        CategorieTresorerie.SALAIRE -> RubriqueComptable.SALAIRES
        CategorieTresorerie.TAXE -> RubriqueComptable.IMPOTS_TAXES
        CategorieTresorerie.LOYER,
        CategorieTresorerie.TRANSPORT,
        CategorieTresorerie.ENERGIE,
        -> RubriqueComptable.CHARGES_EXTERNES
        // Un apport ou un remboursement d'emprunt n'est ni un produit ni une
        // charge : c'est un mouvement de financement, hors compte de résultat.
        CategorieTresorerie.FINANCEMENT -> null
        CategorieTresorerie.TRANSFERT -> null
        CategorieTresorerie.AUTRE -> when (sens) {
            SensMouvement.IN -> RubriqueComptable.AUTRES_PRODUITS
            SensMouvement.OUT -> RubriqueComptable.CHARGES_EXTERNES
        }
    }

    private fun arrondi(valeur: Double): Double = Math.round(valeur * 100.0) / 100.0
}
