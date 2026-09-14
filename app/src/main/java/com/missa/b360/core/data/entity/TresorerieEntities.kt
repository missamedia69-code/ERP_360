package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Compte de trésorerie : caisse, compte bancaire ou portefeuille mobile.
 *
 * Le solde n'est pas stocké — il se recalcule par somme des mouvements sur le
 * solde initial. Un solde matérialisé se désynchronise à la première écriture
 * concurrente ou au premier mouvement corrigé ; la somme, elle, ne ment jamais.
 */
@Entity(
    tableName = "tresorerie_comptes",
    indices = [Index(value = ["nom"], unique = true)],
)
data class CompteTresorerieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** Nom stable de [TypeCompteTresorerie]. */
    val type: String = TypeCompteTresorerie.CAISSE.name,
    /** Banque ou opérateur mobile, selon le type. */
    val etablissement: String? = null,
    /** Numéro de compte ou de téléphone, affiché tronqué. */
    val numero: String? = null,
    /** Solde d'ouverture, à la mise en service du compte. */
    val soldeInitial: Double = 0.0,
    /** Un compte fermé reste consultable mais n'accepte plus de mouvement. */
    val actif: Boolean = true,
    val createdAt: Long,
)

/**
 * Mouvement de trésorerie : encaissement, décaissement, ou jambe d'un virement
 * interne. Rien n'est jamais supprimé — une erreur se corrige par un mouvement
 * inverse, ce qui préserve la piste d'audit (même convention que les pièces
 * opérationnelles).
 */
@Entity(
    tableName = "tresorerie_mouvements",
    indices = [
        Index(value = ["compteId"]),
        Index(value = ["date"]),
        Index(value = ["transfertId"]),
    ],
)
data class MouvementTresorerieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val compteId: Long,
    /** Date de valeur, distincte de [createdAt] : on saisit souvent la veille. */
    val date: Long,
    /** Nom stable de [SensMouvement] : IN ou OUT. */
    val sens: String,
    /** Toujours positif : le sens porte le signe. */
    val montant: Double,
    /** Nom stable de [CategorieTresorerie]. */
    val categorie: String = CategorieTresorerie.AUTRE.name,
    val libelle: String,
    /** Client, fournisseur, salarié ou tiers libre. */
    val tiers: String? = null,
    /** Mode de règlement (espèces, virement, mobile money…). */
    val modePaiement: String? = null,
    /** Référence de la pièce justificative : facture, reçu, bordereau. */
    val reference: String? = null,
    /** Identifiant commun aux deux jambes d'un virement interne. */
    val transfertId: String? = null,
    /** Pointé lors du rapprochement bancaire. */
    val rapproche: Boolean = false,
    val notes: String? = null,
    val createdAt: Long,
)

/** Nature du compte : détermine l'icône et les modes de règlement proposés. */
enum class TypeCompteTresorerie { CAISSE, BANQUE, MOBILE_MONEY }

/** Sens du flux, du point de vue de l'entreprise. */
enum class SensMouvement { IN, OUT }

/**
 * Poste analytique du mouvement. Volontairement court : une TPE qui doit
 * choisir parmi trente rubriques finit par tout classer en « Autre ».
 */
enum class CategorieTresorerie {
    VENTE,
    ACHAT,
    SALAIRE,
    TAXE,
    LOYER,
    TRANSPORT,
    ENERGIE,
    FINANCEMENT,
    TRANSFERT,
    AUTRE,
}
