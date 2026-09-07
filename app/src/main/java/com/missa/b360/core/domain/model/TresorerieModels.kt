package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.R

/** Solde d'un compte à un instant donné, prêt pour l'affichage. */
data class SoldeCompte(
    val compte: CompteTresorerieEntity,
    val solde: Double,
    val nombreMouvements: Int,
)

/** Total encaissé, décaissé et net sur une période. */
data class FluxPeriode(
    val entrees: Double,
    val sorties: Double,
) {
    val net: Double get() = entrees - sorties
}

/**
 * Règles de trésorerie — volontairement sans dépendance Android ni Room, afin
 * d'être vérifiables par des tests unitaires ordinaires.
 *
 * Principe directeur : le montant est toujours stocké positif et c'est le sens
 * qui porte le signe. Un montant négatif en base rendrait tout total ambigu
 * (une sortie de −5 000 est-elle une sortie ou une entrée corrigée ?).
 */
object TresorerieRules {

    /** Un virement interne se saisit d'un seul geste mais crée deux mouvements. */
    const val PREFIXE_TRANSFERT = "TRF"

    /** Montant maximal accepté : au-delà, c'est une faute de frappe. */
    const val MONTANT_MAX = 1_000_000_000_000.0

    /**
     * Analyse un montant saisi : virgule ou point décimal, espaces d'unités de
     * mille (y compris l'espace insécable des claviers français) tolérés.
     * Renvoie `null` dès que la saisie n'est pas un montant strictement positif.
     */
    fun montantSaisi(texte: String): Double? {
        val nettoye = texte.trim()
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace(" ", "")
            .replace(',', '.')
        if (nettoye.isEmpty()) return null
        val valeur = nettoye.toDoubleOrNull() ?: return null
        if (!valeur.isFinite() || valeur <= 0.0 || valeur > MONTANT_MAX) return null
        // Deux décimales suffisent à toute monnaie ; le reste vient d'un calcul
        // flottant approximatif et fausserait les rapprochements au centime.
        return Math.round(valeur * 100.0) / 100.0
    }

    /** Le libellé est la seule mention obligatoire : sans lui, un relevé est illisible. */
    fun libelleValide(libelle: String): Boolean = libelle.trim().length >= 2

    /** Solde = solde d'ouverture + entrées − sorties. */
    fun solde(soldeInitial: Double, mouvements: List<MouvementTresorerieEntity>): Double =
        mouvements.fold(soldeInitial) { cumul, mouvement ->
            when (mouvement.sens) {
                SensMouvement.IN.name -> cumul + mouvement.montant
                SensMouvement.OUT.name -> cumul - mouvement.montant
                else -> cumul
            }
        }

    /** Soldes de tous les comptes, dans l'ordre d'affichage (actifs d'abord). */
    fun soldes(
        comptes: List<CompteTresorerieEntity>,
        mouvements: List<MouvementTresorerieEntity>,
    ): List<SoldeCompte> {
        val parCompte = mouvements.groupBy { it.compteId }
        return comptes.map { compte ->
            val lignes = parCompte[compte.id].orEmpty()
            SoldeCompte(
                compte = compte,
                solde = solde(compte.soldeInitial, lignes),
                nombreMouvements = lignes.size,
            )
        }
    }

    /**
     * Solde global. Les comptes fermés y sont inclus : leur argent existe
     * toujours tant qu'il n'a pas été viré ailleurs.
     */
    fun soldeGlobal(
        comptes: List<CompteTresorerieEntity>,
        mouvements: List<MouvementTresorerieEntity>,
    ): Double = soldes(comptes, mouvements).sumOf { it.solde }

    /**
     * Flux sur une période, bornes incluses.
     *
     * Les virements internes sont exclus : déplacer 100 000 de la caisse vers la
     * banque n'est ni un encaissement ni un décaissement, et les compter
     * gonflerait artificiellement les deux totaux.
     */
    fun flux(
        mouvements: List<MouvementTresorerieEntity>,
        debut: Long,
        fin: Long,
    ): FluxPeriode {
        val periode = mouvements.filter { it.date in debut..fin && it.transfertId == null }
        return FluxPeriode(
            entrees = periode.filter { it.sens == SensMouvement.IN.name }.sumOf { it.montant },
            sorties = periode.filter { it.sens == SensMouvement.OUT.name }.sumOf { it.montant },
        )
    }

    /**
     * Répartition des sorties par poste, du plus lourd au plus léger : c'est la
     * question que pose un dirigeant devant sa trésorerie — « où part l'argent ? ».
     */
    fun repartitionSorties(
        mouvements: List<MouvementTresorerieEntity>,
        debut: Long,
        fin: Long,
    ): List<Pair<CategorieTresorerie, Double>> = mouvements
        .filter {
            it.date in debut..fin &&
                it.sens == SensMouvement.OUT.name &&
                it.transfertId == null
        }
        .groupBy { categorie(it.categorie) }
        .map { (categorie, lignes) -> categorie to lignes.sumOf { it.montant } }
        .sortedByDescending { it.second }

    /** Écart non pointé d'un compte : ce qui reste à rapprocher du relevé. */
    fun resteARapprocher(mouvements: List<MouvementTresorerieEntity>): Double =
        mouvements.filterNot { it.rapproche }.sumOf {
            if (it.sens == SensMouvement.IN.name) it.montant else -it.montant
        }

    /** Lecture défensive : une valeur inconnue en base ne doit pas faire planter l'écran. */
    fun categorie(nom: String?): CategorieTresorerie =
        CategorieTresorerie.entries.firstOrNull { it.name == nom } ?: CategorieTresorerie.AUTRE

    fun sens(nom: String?): SensMouvement =
        SensMouvement.entries.firstOrNull { it.name == nom } ?: SensMouvement.IN

    fun typeCompte(nom: String?): TypeCompteTresorerie =
        TypeCompteTresorerie.entries.firstOrNull { it.name == nom } ?: TypeCompteTresorerie.CAISSE

    /** Libellés traduits — aucune chaîne en dur dans les écrans. */
    fun libelleType(type: TypeCompteTresorerie): Int = when (type) {
        TypeCompteTresorerie.CAISSE -> R.string.tre_type_caisse
        TypeCompteTresorerie.BANQUE -> R.string.tre_type_banque
        TypeCompteTresorerie.MOBILE_MONEY -> R.string.tre_type_mobile
    }

    fun libelleCategorie(categorie: CategorieTresorerie): Int = when (categorie) {
        CategorieTresorerie.VENTE -> R.string.tre_cat_vente
        CategorieTresorerie.ACHAT -> R.string.tre_cat_achat
        CategorieTresorerie.SALAIRE -> R.string.tre_cat_salaire
        CategorieTresorerie.TAXE -> R.string.tre_cat_taxe
        CategorieTresorerie.LOYER -> R.string.tre_cat_loyer
        CategorieTresorerie.TRANSPORT -> R.string.tre_cat_transport
        CategorieTresorerie.ENERGIE -> R.string.tre_cat_energie
        CategorieTresorerie.FINANCEMENT -> R.string.tre_cat_financement
        CategorieTresorerie.TRANSFERT -> R.string.tre_cat_transfert
        CategorieTresorerie.AUTRE -> R.string.tre_cat_autre
    }

    /**
     * Catégories proposées à la saisie selon le sens. « Transfert » n'y figure
     * pas : elle est posée par le virement interne, jamais choisie à la main.
     */
    fun categoriesPour(sens: SensMouvement): List<CategorieTresorerie> = when (sens) {
        SensMouvement.IN -> listOf(
            CategorieTresorerie.VENTE,
            CategorieTresorerie.FINANCEMENT,
            CategorieTresorerie.AUTRE,
        )
        SensMouvement.OUT -> listOf(
            CategorieTresorerie.ACHAT,
            CategorieTresorerie.SALAIRE,
            CategorieTresorerie.TAXE,
            CategorieTresorerie.LOYER,
            CategorieTresorerie.TRANSPORT,
            CategorieTresorerie.ENERGIE,
            CategorieTresorerie.FINANCEMENT,
            CategorieTresorerie.AUTRE,
        )
    }
}
