package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.data.entity.VerificationStatut

/** Nature d'un règlement fournisseur, déduite du moyen de paiement (libre) choisi. */
enum class PaymentMode {
    ESPECES,
    VIREMENT,
    MOBILE_MONEY,
    AUTRE,
    ;

    /** Ce mode envoie l'argent vers des coordonnées bancaires ou mobiles du fournisseur. */
    val exigeCompte: Boolean get() = this == VIREMENT || this == MOBILE_MONEY

    companion object {
        private val MOTS_VIREMENT = listOf("virement", "transfer", "banque", "bank")

        /**
         * Déduit le mode du nom du moyen de paiement, avec les mêmes mots-clés que
         * [TresorerieRules.compteCible] (caisse → espèces, mobile → mobile money). Chèque et carte
         * ne désignent aucun compte du fournisseur : ils restent [AUTRE].
         */
        fun depuis(moyenPaiement: String?): PaymentMode {
            val texte = moyenPaiement.orEmpty().lowercase(java.util.Locale.ROOT)
            return when (TresorerieRules.typeCompteVise(moyenPaiement)) {
                TypeCompteTresorerie.CAISSE -> ESPECES
                TypeCompteTresorerie.MOBILE_MONEY -> MOBILE_MONEY
                TypeCompteTresorerie.BANQUE -> if (MOTS_VIREMENT.any { texte.contains(it) }) VIREMENT else AUTRE
                else -> AUTRE
            }
        }
    }
}

enum class PaymentGuardReason {
    STATUT_INTERDIT,
    PAIEMENT_BLOQUE,
    MONTANT_INVALIDE,
    AU_DESSUS_PLAFOND,
    COMPTE_REQUIS,
    COMPTE_NON_VERIFIE,
    COMPTE_REJETE,
    COMPTE_RECENT_NON_VERIFIE,
}

sealed interface PaymentVerdict {
    data object Autorise : PaymentVerdict
    data class Confirmer(val motif: PaymentGuardReason) : PaymentVerdict
    data class Refuser(val motif: PaymentGuardReason) : PaymentVerdict
}

/**
 * Garde de paiement : l'argent qui sort demande plus de contrôle. La trésorerie suffisante
 * reste vérifiée par le cas d'usage de règlement, pas ici.
 */
object PaymentGuard {

    const val DELAI_COMPTE_RECENT_JOURS = SupplierReadinessRules.DELAI_COMPTE_RECENT_JOURS

    fun evaluer(
        f: FournisseurEntity,
        montant: Double,
        mode: PaymentMode,
        compteBeneficiaire: SupplierAccountInfo?,
        now: Long,
    ): PaymentVerdict {
        if (!montant.isFinite() || montant <= 0.0) return PaymentVerdict.Refuser(PaymentGuardReason.MONTANT_INVALIDE)
        if (!FournisseurRules.peutEtrePaye(f.statut)) return PaymentVerdict.Refuser(PaymentGuardReason.STATUT_INTERDIT)
        if (f.paiementBloque) return PaymentVerdict.Refuser(PaymentGuardReason.PAIEMENT_BLOQUE)

        if (mode.exigeCompte) {
            val compte = compteBeneficiaire ?: return PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_REQUIS)
            when (compte.verification) {
                VerificationStatut.REJETE -> return PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_REJETE)
                VerificationStatut.A_VERIFIER ->
                    return if (SupplierReadinessRules.compteRecent(compte, now)) {
                        PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_RECENT_NON_VERIFIE)
                    } else {
                        PaymentVerdict.Confirmer(PaymentGuardReason.COMPTE_NON_VERIFIE)
                    }
                VerificationStatut.VERIFIE -> Unit
            }
        }

        if (f.plafondPaiement > 0.0 && montant > f.plafondPaiement) {
            return PaymentVerdict.Confirmer(PaymentGuardReason.AU_DESSUS_PLAFOND)
        }
        return PaymentVerdict.Autorise
    }
}
