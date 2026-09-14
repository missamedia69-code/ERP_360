package com.missa.b360.core.domain.model

import com.missa.b360.core.data.dao.GroupeArticleComplet
import com.missa.b360.core.data.entity.ProductEntity

/**
 * Règles transverses des groupes d'articles (spec §5.2 et §8).
 *
 * Un groupe du module Stock est la source de vérité des règles de gestion d'une
 * famille d'articles, **consultées par tous les modules** : ACH lit `achetable`
 * et les comptes de charge, VEN lit `vendable` et le compte de produit, PRO lit
 * `produisible`, MAI lit `equipementMaintenable`, STK lit `stocke`, CPT lit la
 * valorisation et les comptes, SER les groupes de type prestation, REP agrège
 * par groupe.
 *
 * Un article peut être rattaché à un groupe (`ProductEntity.itemGroupId`) ou non :
 * les fiches antérieures à l'arrivée des groupes retombent sur leur [ProductType]
 * via [ProduitRules]. Le groupe prime toujours quand le lien existe.
 */
object ReglesGroupesArticles {

    // --- Prédicats d'un groupe (extensions sûres : une extension absente en base
    // --- retombe sur la valeur par défaut portée par l'entité). ---

    /** Le groupe alimente-t-il le référentiel d'achat ? */
    val GroupeArticleComplet.estAchetable: Boolean
        get() = achat?.achetable ?: true

    /** Le groupe alimente-t-il le référentiel de vente ? */
    val GroupeArticleComplet.estVendable: Boolean
        get() = vente?.vendable ?: true

    /** Le groupe peut-il être produit par un ordre de fabrication ? */
    val GroupeArticleComplet.estProduisible: Boolean
        get() = production?.produisible ?: false

    /** Le groupe désigne-t-il des équipements suivis en maintenance ? */
    val GroupeArticleComplet.estMaintenable: Boolean
        get() = maintenance?.equipementMaintenable ?: false

    /** Les articles du groupe tiennent-ils un stock physique ? */
    val GroupeArticleComplet.estStocke: Boolean
        get() = groupe.stocke

    /** Le stock du groupe entre-t-il dans la valorisation comptable ? */
    val GroupeArticleComplet.estValorise: Boolean
        get() = groupe.valorise

    /** Le groupe est-il de nature prestation (aucune marchandise à livrer) ? */
    val GroupeArticleComplet.estService: Boolean
        get() = vente?.service ?: false

    /**
     * Compte du plan comptable en vigueur à utiliser par défaut pour le module.
     * `null` quand le groupe ne porte pas de compte pour ce module.
     */
    fun GroupeArticleComplet.compteParDefaut(module: ModuleCode): String? = when (module) {
        ModuleCode.ACH -> achat?.compteCharge ?: achat?.compteImmobilisation
        ModuleCode.VEN -> vente?.compteProduit
        ModuleCode.STK, ModuleCode.CPT -> stock?.compteStock ?: vente?.compteProduit ?: achat?.compteCharge
        else -> null
    }

    /**
     * Groupes exploitables par un module (spec §5.2).
     *
     * Les modules sans attribut transverse dédié (RH, TRE, CRM, QUALITE, PRJ…)
     * conservent l'ensemble du référentiel : il leur sert de vocabulaire commun.
     */
    fun utilisablesPour(module: ModuleCode, groupes: List<GroupeArticleComplet>): List<GroupeArticleComplet> {
        val actifs = groupes.filter { it.groupe.actif }
        return when (module) {
            ModuleCode.ACH -> actifs.filter { it.estAchetable }
            ModuleCode.VEN -> actifs.filter { it.estVendable }
            ModuleCode.PRO -> actifs.filter { it.estProduisible || it.estAchetable }
            ModuleCode.MAI -> actifs.filter { it.estMaintenable }
            ModuleCode.STK, ModuleCode.LOG -> actifs.filter { it.estStocke }
            ModuleCode.CPT -> actifs.filter { it.estValorise }
            ModuleCode.SER -> actifs.filter { it.estService }
            else -> actifs
        }
    }

    // --- Pont article → groupe : le lien product.itemGroupId prime, sinon repli
    // --- sur le type historique (fiches créées avant l'arrivée des groupes). ---

    /** Groupe de l'article, s'il y est rattaché. */
    fun pourArticle(article: ProductEntity, groupes: List<GroupeArticleComplet>): GroupeArticleComplet? =
        article.itemGroupId?.let { id -> groupes.firstOrNull { it.groupe.id == id } }

    fun estAchetable(article: ProductEntity, groupes: List<GroupeArticleComplet>): Boolean =
        pourArticle(article, groupes)?.estAchetable ?: ProduitRules.estAchetable(article.type)

    fun estVendable(article: ProductEntity, groupes: List<GroupeArticleComplet>): Boolean =
        pourArticle(article, groupes)?.estVendable ?: ProduitRules.estVendable(article.type)

    fun estProduisible(article: ProductEntity, groupes: List<GroupeArticleComplet>): Boolean =
        pourArticle(article, groupes)?.estProduisible ?: ProduitRules.estFabricable(article.type)

    fun estMaintenable(article: ProductEntity, groupes: List<GroupeArticleComplet>): Boolean =
        pourArticle(article, groupes)?.estMaintenable ?: false

    /** Compte par défaut d'un article pour un module (null sans groupe ni règle dédiée). */
    fun compteParDefaut(
        article: ProductEntity,
        module: ModuleCode,
        groupes: List<GroupeArticleComplet>,
    ): String? = pourArticle(article, groupes)?.compteParDefaut(module)

    // --- Catalogues filtrés pour les écrans des modules partenaires. ---

    /** Articles proposables à la vente : actifs et vendables selon leur groupe. */
    fun vendables(produits: List<ProductEntity>, groupes: List<GroupeArticleComplet>): List<ProductEntity> =
        produits.filter { it.active && estVendable(it, groupes) }

    /** Articles commandables à un fournisseur : actifs et achetables selon leur groupe. */
    fun achetables(produits: List<ProductEntity>, groupes: List<GroupeArticleComplet>): List<ProductEntity> =
        produits.filter { it.active && estAchetable(it, groupes) }

    /** Articles qu'un ordre de fabrication peut produire. */
    fun produisibles(produits: List<ProductEntity>, groupes: List<GroupeArticleComplet>): List<ProductEntity> =
        produits.filter { it.active && estProduisible(it, groupes) }

    /** Articles suivis en maintenance (équipements). */
    fun maintenables(produits: List<ProductEntity>, groupes: List<GroupeArticleComplet>): List<ProductEntity> =
        produits.filter { it.active && estMaintenable(it, groupes) }
}