package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Le type du produit détermine les sections affichées du formulaire (spec §7). */
enum class ProductType {
    /** Produit acheté puis revendu (commerce). */
    ACHATE_REVENDU,
    /** Matière première consommée par la production. */
    MATIERE_PREMIERE,
    /** Produit fini issu d'un ordre de production. */
    FABRIQUE,
    /** Produit assemblé à partir de composants. */
    COMPOSE,
    /** Consommable non revendable. */
    CONNOMMABLE,
    /** Pièce détachée destinée à la maintenance. */
    PIECE_MAINTENANCE,
    /** Immobilisation suivie (n° série, garantie, statut de service). */
    EQUIPEMENT,
    /** Matériel / outillage de l'entreprise. */
    MATERIEL,
    /** Tout autre bien non couvert ci-dessus. */
    AUTRE_BIEN,
    /** Service ou prestation facturée, sans stock. */
    PRESTATION,
    /** Produit semi-fini, intermédiaire de fabrication. */
    SEMI_FINI,
    /** Emballage : carton, palette, bouteille, consigne… */
    EMBALLAGE,
    /** Déchet revendu ou remis à un recycleur. */
    DECHET_VALORISABLE,
    /** Déchet à éliminer : suivi physique, aucune valeur commerciale. */
    DECHET_NON_VALORISABLE,
    /** Kit ou ensemble vendu/assemblé à partir de composants. */
    KIT,
    /** Article en consignation : propriété d'un tiers. */
    CONSIGNATION,
}

/** Statut produit — « Désactivé » unique ; jamais de suppression physique (C7). */
enum class ProductStatus { ACTIF, DESACTIVE }

/** Statut de service d'une immobilisation (maquette Équipements). */
enum class StatutEquipement { EN_SERVICE, MAINTENANCE, HORS_SERVICE }

/**
 * Extension « immobilisation » d'un produit de type équipement / matériel /
 * pièce de maintenance : n° de série, garantie, statut de service (maquette 6).
 * Table séparée : aucun produit existant ni migration de colonne nécessaire.
 */
@Entity(tableName = "product_equipements")
data class ProductEquipementEntity(
    @PrimaryKey val produitId: Long,
    val modele: String? = null,
    val numeroSerie: String? = null,
    /** Epoch millis. */
    val dateAcquisition: Long? = null,
    val prixAquisition: Double? = null,
    val fournisseurNom: String? = null,
    val responsable: String? = null,
    val garantieDebut: Long? = null,
    val garantieFin: Long? = null,
    val statut: StatutEquipement = StatutEquipement.EN_SERVICE,
)

/** Extension « déchet » d'un produit : traçabilité réglementaire et élimination. */
@Entity(tableName = "product_dechets")
data class ProductDechetEntity(
    @PrimaryKey val produitId: Long,
    val typeDechet: String? = null,
    val codeReglementaire: String? = null,
    val dangereux: Boolean = false,
    val valorisable: Boolean = true,
    val origine: String? = null,
    val modeElimination: String? = null,
    val prestataire: String? = null,
    val coutElimination: Double? = null,
    val filiereRecyclage: String? = null,
    val zoneStockage: String? = null,
)

/** Extension « emballage » d'un produit : contenant, consigne, réutilisation. */
@Entity(tableName = "product_emballages")
data class ProductEmballageEntity(
    @PrimaryKey val produitId: Long,
    val typeEmballage: String? = null,
    val matiere: String? = null,
    val dimensions: String? = null,
    val poidsKg: Double? = null,
    val capacite: Double? = null,
    val reutilisable: Boolean = false,
    val consigne: Boolean = false,
    val reutilisationsMax: Int? = null,
)

/** Extension « consignation » : le bien appartient à un tiers. */
@Entity(tableName = "product_consignations")
data class ProductConsignationEntity(
    @PrimaryKey val produitId: Long,
    val proprietaire: String? = null,
    val referenceContrat: String? = null,
    /** Epoch millis. */
    val dateDebut: Long? = null,
    val dateFin: Long? = null,
    val conditionsRetour: String? = null,
)

/** Extension « kit » : méthode de gestion du stock. */
@Entity(tableName = "product_kits")
data class ProductKitEntity(
    @PrimaryKey val produitId: Long,
    /** VIRTUEL (déstockage à la vente) ou ASSEMBLE (entrée en stock). */
    val methode: String = "VIRTUEL",
)

/** Composition d'un kit : composant et quantité (nomenclature légère). */
@Entity(tableName = "kit_composants", primaryKeys = ["kitId", "composantId"])
data class KitComposantEntity(
    val kitId: Long,
    val composantId: Long,
    val quantite: Double = 1.0,
)

/**
 * Catégorie de produit (spec §31) — suppression verrouillée si rattachée
 * à au moins un produit (même règle que les catégories clients).
 */
@Entity(tableName = "product_categories")
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** PRODUIT / SERVICE / DEPENSE / AUTRE. */
    val type: String = "PRODUIT",
    val parentId: Long? = null,
    val description: String? = null,
    val actif: Boolean = true,
)

/**
 * Produit — code `PRD-2026-0001` unique via SequenceManager ; code-barres indexé
 * pour la recherche à la vente ; le stock courant n'est **jamais** stocké ici :
 * il provient exclusivement des mouvements de stock (spec §43).
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["nom"]),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val type: ProductType = ProductType.ACHATE_REVENDU,
    /**
     * Groupe d'articles dont l'article hérite ses règles.
     *
     * Facultatif : les fiches créées avant l'arrivée des groupes continuent de
     * fonctionner sur leur seul [type], qui reste la valeur de repli.
     */
    val itemGroupId: Long? = null,
    /** Règles héritées du groupe, surchargeables par article (spec §14). */
    val vendable: Boolean = true,
    val achetable: Boolean = true,
    val stockable: Boolean = true,
    val reference: String? = null,
    /** Code-barres saisi ou scanné. */
    val barcode: String? = null,
    val sku: String? = null,
    val categorieId: Long? = null,
    val marque: String? = null,
    /** Unité de vente (ex. pièce, carton, litre). */
    val unite: String? = null,
    /** Photo locale (PI jamais dans le cloud). */
    val photoPath: String? = null,
    val prixAchat: Double? = null,
    val prixVente: Double? = null,
    val prixRevient: Double? = null,
    val prixMinimum: Double? = null,
    /** Remise maximale autorisée en % (spec §7). */
    val remiseMaxPct: Double = 0.0,
    /** Seuils d'alerte — le stock lui-même provient des mouvements de stock. */
    val stockMin: Double = 0.0,
    val stockMax: Double? = null,
    val stockSecurite: Double = 0.0,
    /** Entrepôt principal de stockage. */
    val siteId: Long? = null,
    val emplacement: String? = null,
    val fournisseurId: Long? = null,
    val refFournisseur: String? = null,
    val description: String? = null,
    /** Poids en kilogrammes. */
    val poids: Double? = null,
    /** Volume en litres. */
    val volume: Double? = null,
    val origine: String? = null,
    val notes: String? = null,
    val statut: ProductStatus = ProductStatus.ACTIF,
    val active: Boolean = true,
    val createdAt: Long,
)

/**
 * Stock courant d'un produit dans un site — mis à jour exclusivement par les
 * mouvements de stock (spec §43 : ne jamais modifier arbitrairement un champ stock).
 */
@Entity(tableName = "product_stock", primaryKeys = ["produitId", "siteId"])
data class ProductStockEntity(
    val produitId: Long,
    val siteId: Long,
    val quantite: Double = 0.0,
    /**
     * Valeur du stock (CUMP × quantité) pour les familles valorisées — alimentée
     * par les réceptions d'achat ; le CUMP se déduit : `valeur ÷ quantite`.
     */
    val valeur: Double = 0.0,
)

/**
 * Type de mouvement — le sens est porté par le type, jamais par un signe masqué.
 * TRANSFERT_* ne s'emploie que par paires (sortie source + entrée destination,
 * même référence TRF).
 */
enum class StockMovementType {
    ENTREE,
    SORTIE,
    /** Correction : écart signé appliqué au stock (inventaire, casse comptée…). */
    AJUSTEMENT,
    TRANSFERT_SORTIE,
    TRANSFERT_ENTREE,
}

/**
 * Mouvement de stock — piste d'audit : chaque variation de
 * [ProductStockEntity.quantite] est accompagnée de son mouvement (spec §38/§43).
 */
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["produitId"]),
        Index(value = ["siteId"]),
        Index(value = ["horodatage"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,
    val siteId: Long,
    val type: StockMovementType,
    /** Quantité positive pour ENTRÉE/SORTIE/TRANSFERT ; écart signé pour AJUSTEMENT. */
    val quantite: Double,
    val motif: String,
    /** Référence du document d'origine (vente, achat, transfert TRF…). */
    val reference: String? = null,
    val commentaire: String? = null,
    /** Traçabilité réception : lot, numéro de série, date de péremption. */
    val lot: String? = null,
    val numeroSerie: String? = null,
    val datePeremption: Long? = null,
    val horodatage: Long,
)

/** Session d'inventaire physique (maquette 8) — clôture appliquée en ajustements. */
@Entity(tableName = "inventaires")
data class InventaireEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteId: Long,
    val debut: Long,
    val fin: Long? = null,
    /** EN_COURS / CLOTURE. */
    val statut: String = "EN_COURS",
)

/** Ligne d'inventaire : attendu (stock théorique) vs compté. */
@Entity(tableName = "inventaire_lignes", primaryKeys = ["inventaireId", "produitId"])
data class InventaireLigneEntity(
    val inventaireId: Long,
    val produitId: Long,
    val attendu: Double,
    val compte: Double? = null,
)
