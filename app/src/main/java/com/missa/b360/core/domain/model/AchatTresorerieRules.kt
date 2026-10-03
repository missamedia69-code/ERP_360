package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.SensMouvement

/** Mouvement de trésorerie à écrire quand une facture d'achat est validée avec un montant réglé. */
sealed interface DecaissementAchat {
    /** Rien à écrire : rien de réglé, mouvement déjà enregistré ou aucun compte de trésorerie. */
    data object Aucun : DecaissementAchat

    /** Le compte ne couvre pas la sortie : la facture ne doit pas être validée. */
    data object SoldeInsuffisant : DecaissementAchat

    /** Sortie à enregistrer : ce qui est payé à un fournisseur quitte toujours la trésorerie. */
    data class Sortie(val compteId: Long, val montant: Double) : DecaissementAchat {
        val sens: SensMouvement get() = SensMouvement.OUT
    }
}

object AchatTresorerieRules {

    /**
     * Décide du mouvement lié au montant réglé à la validation d'une facture d'achat. Le montant
     * est celui réellement payé (arrondi au centime) et doit être couvert par [soldeCourant] du
     * compte ciblé (`null` = solde inconnu, traité comme insuffisant).
     */
    fun decaissementALaValidation(
        montantPaye: Double,
        dejaEnregistre: Boolean,
        compteId: Long?,
        soldeCourant: Double?,
    ): DecaissementAchat {
        val montant = TresorerieRules.encaissementAEnregistrer(
            montantPaye = montantPaye,
            dejaEnregistre = dejaEnregistre,
            compteDisponible = compteId != null,
        ) ?: return DecaissementAchat.Aucun
        if (compteId == null) return DecaissementAchat.Aucun
        if (soldeCourant == null || !TresorerieRules.decaissementAutorise(soldeCourant, montant)) {
            return DecaissementAchat.SoldeInsuffisant
        }
        return DecaissementAchat.Sortie(compteId, montant)
    }
}
