package com.missa.b360.core.documents

import com.missa.b360.core.data.entity.EnterpriseEntity

/** Tous les documents normalisés que les modules peuvent produire. */
enum class DocumentType(val titreDefaut: String, val prefixe: String) {
    DEVIS("DEVIS", "DEV"),
    BON_COMMANDE_CLIENT("BON DE COMMANDE", "BC"),
    BON_COMMANDE_FOURNISSEUR("BON DE COMMANDE FOURNISSEUR", "BCF"),
    FACTURE_CLIENT("FACTURE", "FAC"),
    FACTURE_FOURNISSEUR("FACTURE FOURNISSEUR", "FF"),
    BON_LIVRAISON("BON DE LIVRAISON", "BL"),
    BON_RECEPTION("BON DE RÉCEPTION", "BR"),
    RECU_PAIEMENT("REÇU DE PAIEMENT", "REC"),
    NOTE_CREDIT("NOTE DE CRÉDIT", "NC"),
    NOTE_DEBIT("NOTE DE DÉBIT", "ND"),
    FICHE_STOCK("FICHE DE STOCK", "FS"),
    INVENTAIRE("RAPPORT D’INVENTAIRE", "INV"),
    RAPPORT_COMPTABLE("RAPPORT COMPTABLE", "RC"),
    ETAT_TRESORERIE("ÉTAT DE TRÉSORERIE", "TR"),
    BULLETIN_PAIE("BULLETIN DE PAIE", "BP"),
    CONTRAT_TRAVAIL("CONTRAT DE TRAVAIL", "CT"),
    ATTESTATION_TRAVAIL("ATTESTATION DE TRAVAIL", "AT"),
    ORDRE_FABRICATION("ORDRE DE FABRICATION", "OF"),
    RAPPORT_PRODUCTION("RAPPORT DE PRODUCTION", "RP"),
    ORDRE_MAINTENANCE("ORDRE DE MAINTENANCE", "OM"),
    RAPPORT_INTERVENTION("RAPPORT D’INTERVENTION", "RI"),
    RAPPORT_SERVICE("RAPPORT DE SERVICE", "RS"),
    RAPPORT_QUALITE("RAPPORT QUALITÉ", "RQ"),
    FICHE_NON_CONFORMITE("FICHE DE NON-CONFORMITÉ", "NCF"),
    RAPPORT_PROJET("RAPPORT DE PROJET", "PRJ"),
    BORDEREAU_EXPEDITION("BORDEREAU D’EXPÉDITION", "BE"),
}

data class DocumentPartie(
    val titre: String,
    val nom: String,
    val lignes: List<String> = emptyList(),
)

data class DocumentLigne(
    val designation: String,
    val description: String? = null,
    val quantite: Double? = null,
    val unite: String? = null,
    val prixUnitaire: Double? = null,
    val taxePourcent: Double? = null,
    val montant: Double? = null,
)

data class DocumentTotal(val libelle: String, val montant: Double, val important: Boolean = false)
data class DocumentChamp(val libelle: String, val valeur: String)

data class DocumentDonnees(
    val type: DocumentType,
    val numero: String,
    val dateEmission: String,
    val entreprise: EnterpriseEntity,
    val destinataire: DocumentPartie? = null,
    val statut: String? = null,
    val meta: List<DocumentChamp> = emptyList(),
    val lignes: List<DocumentLigne> = emptyList(),
    val totaux: List<DocumentTotal> = emptyList(),
    val sections: List<Pair<String, List<String>>> = emptyList(),
    val notes: List<String> = emptyList(),
    val montantEnLettres: String? = null,
    val signataires: List<String> = emptyList(),
    val devise: String = entreprise.devise,
    val titrePersonnalise: String? = null,
)

data class DocumentOptions(
    val couleurPrincipale: Int = 0xFF073B4C.toInt(),
    val couleurAccent: Int = 0xFF0F8B78.toInt(),
    val afficherLogo: Boolean = true,
    val afficherMentionMissa: Boolean = true,
    val mentionMissa: String = "Propulsé par Missa Business 360",
    val afficherMontantEnLettres: Boolean = true,
    val localeTag: String = "fr",
)

/** Validation commune avant génération : aucun PDF légal silencieusement incomplet. */
object DocumentValidation {
    fun erreurs(document: DocumentDonnees): List<String> = buildList {
        if (document.entreprise.nom.isBlank()) add("Le nom de l’entreprise est obligatoire")
        if (document.numero.isBlank()) add("Le numéro du document est obligatoire")
        if (document.dateEmission.isBlank()) add("La date d’émission est obligatoire")
        if (document.devise.isBlank()) add("La devise est obligatoire")
        document.lignes.forEachIndexed { index, ligne ->
            if (ligne.designation.isBlank()) add("La désignation de la ligne ${index + 1} est vide")
            if (ligne.quantite != null && (!ligne.quantite.isFinite() || ligne.quantite < 0)) add("Quantité invalide à la ligne ${index + 1}")
            if (ligne.montant != null && !ligne.montant.isFinite()) add("Montant invalide à la ligne ${index + 1}")
        }
        document.totaux.forEach { if (!it.montant.isFinite()) add("Total invalide : ${it.libelle}") }
    }

    /** Découpage stable : en-tête répété et maximum maîtrisé sur chaque page. */
    fun paginer(lignes: List<DocumentLigne>, premierePage: Int = 12, suivantes: Int = 18): List<List<DocumentLigne>> {
        require(premierePage > 0 && suivantes > 0)
        if (lignes.isEmpty()) return listOf(emptyList())
        val pages = mutableListOf<List<DocumentLigne>>()
        pages += lignes.take(premierePage)
        var index = premierePage
        while (index < lignes.size) {
            pages += lignes.drop(index).take(suivantes)
            index += suivantes
        }
        return pages
    }
}
