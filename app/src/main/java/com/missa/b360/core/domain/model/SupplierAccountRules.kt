package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity

/** Règles pures des comptes de paiement fournisseur (banque et mobile money). */
object SupplierAccountRules {

    private fun norm(valeur: String?): String? = valeur?.filterNot { it.isWhitespace() }?.uppercase()?.takeIf { it.isNotEmpty() }

    /**
     * Vrai si les coordonnées qui reçoivent l'argent changent : titulaire, IBAN, numéro de compte
     * ou numéro mobile. Les espaces et la casse ne comptent pas ; banque, notes ou compte principal
     * ne sont pas des coordonnées de paiement.
     */
    fun coordonneesModifiees(ancien: FournisseurCompteBancaireEntity, nouveau: FournisseurCompteBancaireEntity): Boolean =
        norm(ancien.titulaire) != norm(nouveau.titulaire) ||
            norm(ancien.iban) != norm(nouveau.iban) ||
            norm(ancien.numeroCompte) != norm(nouveau.numeroCompte) ||
            norm(ancien.numeroMobile) != norm(nouveau.numeroMobile)

    /** Un compte doit avoir un titulaire et au moins un numéro. */
    fun valide(compte: FournisseurCompteBancaireEntity): Boolean =
        compte.titulaire.isNotBlank() &&
            (norm(compte.iban) != null || norm(compte.numeroCompte) != null || norm(compte.numeroMobile) != null)

    /** Masque un numéro : seuls les 4 derniers caractères restent visibles (« •••• 4321 »). */
    fun masquer(numero: String?): String {
        val propre = numero?.filterNot { it.isWhitespace() }.orEmpty()
        if (propre.isEmpty()) return ""
        if (propre.length <= 4) return "••••"
        return "•••• " + propre.takeLast(4)
    }
}
