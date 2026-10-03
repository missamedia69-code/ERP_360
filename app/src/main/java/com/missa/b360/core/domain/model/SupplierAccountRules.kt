package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.VerificationStatut

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

    /**
     * Compte qui recevra un règlement fait par [mode]. Seuls les comptes utilisables avec ce mode
     * sont candidats (IBAN ou numéro de compte pour un virement, numéro mobile pour le mobile money).
     * Si [compteId] est donné, ce compte seul est retenu (`null` s'il n'est pas candidat). Sinon :
     * le compte principal d'abord, puis le plus fiable (vérifié, à vérifier, rejeté), puis le plus
     * récemment modifié. Aucun compte pour les modes qui n'en exigent pas.
     */
    fun compteBeneficiaire(
        comptes: List<FournisseurCompteBancaireEntity>,
        mode: PaymentMode,
        compteId: Long? = null,
    ): FournisseurCompteBancaireEntity? {
        if (!mode.exigeCompte) return null
        val candidats = comptes.filter {
            if (mode == PaymentMode.MOBILE_MONEY) norm(it.numeroMobile) != null
            else norm(it.iban) != null || norm(it.numeroCompte) != null
        }
        if (compteId != null) return candidats.firstOrNull { it.id == compteId }
        val rang = { c: FournisseurCompteBancaireEntity ->
            when (c.verification) {
                VerificationStatut.VERIFIE -> 0
                VerificationStatut.A_VERIFIER -> 1
                VerificationStatut.REJETE -> 2
            }
        }
        return candidats.sortedWith(
            compareByDescending<FournisseurCompteBancaireEntity> { it.principal }
                .thenBy(rang)
                .thenByDescending { it.modifieLe }
                .thenBy { it.id },
        ).firstOrNull()
    }

    /** Masque un numéro : seuls les 4 derniers caractères restent visibles (« •••• 4321 »). */
    fun masquer(numero: String?): String {
        val propre = numero?.filterNot { it.isWhitespace() }.orEmpty()
        if (propre.isEmpty()) return ""
        if (propre.length <= 4) return "••••"
        return "•••• " + propre.takeLast(4)
    }
}

/** Vue minimale d'un compte pour la garde de paiement. */
fun FournisseurCompteBancaireEntity.versInfo(): SupplierAccountInfo =
    SupplierAccountInfo(verification = verification, modifieLe = modifieLe)
