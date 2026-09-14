package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.util.Iso4217

/**
 * Bloc « émetteur » repris sur les pièces commerciales : identité, coordonnées et
 * identifiants légaux de l'entreprise, libellés selon la zone fiscale du pays.
 *
 * La plupart des législations imposent ces mentions sur une facture ; l'objet les
 * assemble une fois pour l'écran, le PDF, l'impression, le partage et l'e-mail.
 *
 * @property nom raison sociale, jamais vide côté appelant.
 * @property coordonnees adresse, téléphone et e-mail effectivement renseignés.
 * @property identifiants identifiants légaux déjà préfixés de leur libellé local
 *   (« NIU : P0123456789X », « ICE : 001234567000089 »…).
 */
data class MentionsLegales(
    val nom: String,
    val coordonnees: List<String> = emptyList(),
    val identifiants: List<String> = emptyList(),
) {
    /** Lignes affichables sous la raison sociale, dans l'ordre de lecture. */
    val lignes: List<String> get() = coordonnees + identifiants

    /** Vrai dès qu'une information autre que le nom est disponible. */
    val complet: Boolean get() = lignes.isNotEmpty()

    /** Rendu texte pour un partage, un e-mail ou un PDF. */
    fun texte(separateur: String = "\n"): String =
        (listOf(nom) + lignes).filter(String::isNotBlank).joinToString(separateur)

    companion object {
        /** Aucune entreprise chargée : le nom de repli évite une pièce anonyme. */
        fun vide(nomParDefaut: String) = MentionsLegales(nom = nomParDefaut)

        /**
         * Construit le bloc à partir de la fiche entreprise. Les libellés génériques
         * (chaînes traduites) ne servent que pour les pays sans désignation officielle
         * connue dans [ReferentielFiscal].
         */
        fun depuis(
            entreprise: EnterpriseEntity?,
            libelleFiscalGenerique: String,
            libelleRegistreGenerique: String,
            nomParDefaut: String = "",
        ): MentionsLegales {
            if (entreprise == null) return vide(nomParDefaut)
            val codePays = Iso4217.codePaysDepuisNom(entreprise.pays)
            val regles = ReferentielFiscal.regles(
                codePays = codePays,
                libelleFiscalGenerique = libelleFiscalGenerique,
                libelleRegistreGenerique = libelleRegistreGenerique,
            )
            val valeurs = mapOf(
                CleIdentifiant.FISCAL to entreprise.numeroFiscal,
                CleIdentifiant.REGISTRE to entreprise.registreCommerce,
            )
            return MentionsLegales(
                nom = entreprise.nom.trim().ifEmpty { nomParDefaut },
                coordonnees = listOfNotNull(
                    entreprise.adresse?.trim()?.ifEmpty { null },
                    entreprise.telephone?.trim()?.ifEmpty { null },
                    entreprise.email?.trim()?.ifEmpty { null },
                ),
                identifiants = regles.mapNotNull { regle ->
                    valeurs[regle.cle]?.trim()?.ifEmpty { null }?.let { valeur ->
                        "${regle.libelle} : $valeur"
                    }
                },
            )
        }
    }
}
