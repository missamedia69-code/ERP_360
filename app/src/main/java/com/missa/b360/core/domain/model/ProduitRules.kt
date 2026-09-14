package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductType

/**
 * Ce qu'un article peut faire, selon sa nature.
 *
 * Le type d'article existait dans le modèle sans qu'aucun écran ne s'en serve :
 * on pouvait vendre une matière première, acheter un produit qu'on fabrique
 * soi-même, ou consommer un produit fini comme composant. Ces règles ferment
 * ces portes, à un seul endroit.
 */
object ProduitRules {

    /**
     * Articles proposables à la vente.
     *
     * Une matière première n'est pas destinée au client : elle entre par
     * l'achat et sort par la production. Un consommable non plus — il sert au
     * fonctionnement de l'entreprise, pas à son chiffre d'affaires.
     */
    fun estVendable(type: ProductType): Boolean = when (type) {
        ProductType.ACHATE_REVENDU, ProductType.FABRIQUE, ProductType.COMPOSE -> true
        ProductType.MATIERE_PREMIERE, ProductType.CONNOMMABLE -> false
    }

    /**
     * Articles qu'on peut commander à un fournisseur.
     *
     * Un produit fabriqué en interne ne s'achète pas : s'il fallait
     * l'approvisionner, ce serait un article acheté-revendu. Un article composé
     * s'assemble à partir de ses composants.
     */
    fun estAchetable(type: ProductType): Boolean = when (type) {
        ProductType.ACHATE_REVENDU, ProductType.MATIERE_PREMIERE, ProductType.CONNOMMABLE -> true
        ProductType.FABRIQUE, ProductType.COMPOSE -> false
    }

    /** Articles utilisables comme composant d'un ordre de fabrication. */
    fun estComposant(type: ProductType): Boolean = when (type) {
        ProductType.MATIERE_PREMIERE, ProductType.ACHATE_REVENDU, ProductType.COMPOSE -> true
        ProductType.FABRIQUE, ProductType.CONNOMMABLE -> false
    }

    /** Articles qu'un ordre de fabrication peut produire. */
    fun estFabricable(type: ProductType): Boolean = when (type) {
        ProductType.FABRIQUE, ProductType.COMPOSE -> true
        else -> false
    }

    /** Filtre d'un catalogue pour la vente : actifs et vendables. */
    fun vendables(produits: List<ProductEntity>): List<ProductEntity> =
        produits.filter { it.active && estVendable(it.type) }

    /** Filtre d'un catalogue pour l'achat : actifs et achetables. */
    fun achetables(produits: List<ProductEntity>): List<ProductEntity> =
        produits.filter { it.active && estAchetable(it.type) }

    /** Filtre d'un catalogue pour les composants d'un ordre de fabrication. */
    fun composants(produits: List<ProductEntity>): List<ProductEntity> =
        produits.filter { it.active && estComposant(it.type) }

    /** Filtre d'un catalogue pour le produit fini d'un ordre de fabrication. */
    fun fabricables(produits: List<ProductEntity>): List<ProductEntity> =
        produits.filter { it.active && estFabricable(it.type) }

    /**
     * Nature d'article que l'achat doit proposer par défaut.
     *
     * Le commerçant achète pour revendre ; le fabricant achète de la matière.
     * Proposer d'emblée la bonne nature évite la faute de saisie la plus
     * fréquente — un article créé en « acheté-revendu » alors qu'il alimente
     * la production, et qui devient donc vendable par erreur.
     */
    fun natureAchatParDefaut(profil: ProfilActivite?): ProductType = when (profil) {
        ProfilActivite.APSV -> ProductType.MATIERE_PREMIERE
        else -> ProductType.ACHATE_REVENDU
    }
}
