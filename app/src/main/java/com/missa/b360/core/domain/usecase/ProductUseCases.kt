package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStatus
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Saisie du formulaire produit (spec §7) — même composant pour création et modification. */
data class ProductInput(
    val nom: String,
    val type: ProductType = ProductType.ACHATE_REVENDU,
    val reference: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val categorieId: Long? = null,
    val marque: String? = null,
    val unite: String? = null,
    val prixAchat: Double? = null,
    val prixVente: Double? = null,
    val prixRevient: Double? = null,
    val prixMinimum: Double? = null,
    val remiseMaxPct: Double = 0.0,
    val stockMin: Double = 0.0,
    val stockMax: Double? = null,
    val stockSecurite: Double = 0.0,
    val siteId: Long? = null,
    val emplacement: String? = null,
    val fournisseurId: Long? = null,
    val refFournisseur: String? = null,
    val description: String? = null,
    val poids: Double? = null,
    val volume: Double? = null,
    val origine: String? = null,
    val notes: String? = null,
)

/**
 * Règles de saisie du formulaire produit (validation UI puis validation métier — spec §3).
 * Logique pure, couverte par les tests unitaires.
 */
object ProductValidation {
    const val LONGUEUR_NOM_MAX = 120
    const val LONGUEUR_CODE_MAX = 60
    const val LONGUEUR_ADRESSE_MAX = 250
    const val LONGUEUR_NOTES_MAX = 1_000

    fun normaliseTexte(texte: String?): String? = texte?.trim()?.ifBlank { null }

    fun nomEstValide(nom: String): Boolean = nom.trim().length in 2..LONGUEUR_NOM_MAX

    fun codeEstValide(code: String?): Boolean =
        normaliseTexte(code)?.length?.let { it <= LONGUEUR_CODE_MAX } ?: true

    /** Montant monétaire : null (non saisi) ou finit et positif ou nul. */
    fun montantEstValide(montant: Double?): Boolean =
        montant == null || (montant.isFinite() && montant >= 0.0)

    fun remiseEstValide(remisePct: Double): Boolean = remisePct.isFinite() && remisePct in 0.0..100.0

    fun seuilEstValide(seuil: Double?): Boolean =
        seuil == null || (seuil.isFinite() && seuil >= 0.0)

    /** Stock initial : uniquement positif ou nul — il n'est jamais négatif ni flottant infini. */
    fun stockInitialEstValide(quantite: Double?): Boolean =
        quantite == null || (quantite.isFinite() && quantite >= 0.0)

    fun inputEstValide(input: ProductInput): Boolean =
        nomEstValide(input.nom) &&
            codeEstValide(input.reference) &&
            codeEstValide(input.barcode) &&
            codeEstValide(input.sku) &&
            (normaliseTexte(input.marque)?.length?.let { it <= LONGUEUR_NOM_MAX } ?: true) &&
            (normaliseTexte(input.unite)?.length?.let { it <= 30 } ?: true) &&
            montantEstValide(input.prixAchat) &&
            montantEstValide(input.prixVente) &&
            montantEstValide(input.prixRevient) &&
            montantEstValide(input.prixMinimum) &&
            remiseEstValide(input.remiseMaxPct) &&
            seuilEstValide(input.stockMin) &&
            seuilEstValide(input.stockMax) &&
            seuilEstValide(input.stockSecurite) &&
            (normaliseTexte(input.emplacement)?.length?.let { it <= LONGUEUR_ADRESSE_MAX } ?: true) &&
            codeEstValide(input.refFournisseur) &&
            (normaliseTexte(input.origine)?.length?.let { it <= LONGUEUR_NOM_MAX } ?: true) &&
            seuilEstValide(input.poids) &&
            seuilEstValide(input.volume) &&
            (normaliseTexte(input.notes)?.length?.let { it <= LONGUEUR_NOTES_MAX } ?: true)
}

/** Lecture des produits actifs (module Stock + catalogue de vente). */
class ObserveProductsUseCase @Inject constructor(
    private val productDao: ProductDao,
) {
    operator fun invoke(): Flow<List<ProductEntity>> = productDao.observeAll()
}

/** Ligne de stock lisible pour les listes (spec §47 : pas de rechargement inutile). */
class ObserveProductStockUseCase @Inject constructor(
    private val stockDao: ProductStockDao,
) {
    operator fun invoke(): Flow<List<ProductStockEntity>> = stockDao.observeToutes()
}

/** Fiche produit pour le mode édition. */
class GetProductUseCase @Inject constructor(
    private val productDao: ProductDao,
) {
    operator fun invoke(id: Long): Flow<ProductEntity?> = productDao.observeById(id)
}

/**
 * Création d'un produit (spec §7) — numérotation `PRD-2026-0001` (RA-09),
 * licence (RA-05), journal (RA-18). Le stock initial (si autorisé) génère un
 * **mouvement** d'entrée : le stock courant ne naît jamais sans mouvement (§43).
 */
class CreateProductUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val productId: Long, val code: String, val stockInitial: Double) : Result()
        data object LectureSeule : Result()
        data object NomObligatoire : Result()
        data object DonneesInvalides : Result()
        data object BarcodeExistant : Result()
        /** Stock initial saisi sans entrepôt principal — un mouvement exige un site. */
        data object SiteRequis : Result()
    }

    suspend operator fun invoke(
        input: ProductInput,
        initialStock: Double? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val nomNormalise = input.nom.trim()
        if (nomNormalise.isEmpty()) return Result.NomObligatoire
        if (!ProductValidation.inputEstValide(input)) return Result.DonneesInvalides
        val stock = initialStock?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        if (stock > 0.0 && input.siteId == null) return Result.SiteRequis
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val barcode = ProductValidation.normaliseTexte(input.barcode)
        if (barcode != null && productDao.getByBarcode(barcode) != null) return Result.BarcodeExistant

        val code = sequenceManager.next(DocType.PRODUIT)
        val id = database.withTransaction {
            val produitId = productDao.insert(
                ProductEntity(
                    code = code,
                    nom = nomNormalise,
                    type = input.type,
                    reference = ProductValidation.normaliseTexte(input.reference),
                    barcode = barcode,
                    sku = ProductValidation.normaliseTexte(input.sku),
                    categorieId = input.categorieId,
                    marque = ProductValidation.normaliseTexte(input.marque),
                    unite = ProductValidation.normaliseTexte(input.unite),
                    prixAchat = input.prixAchat,
                    prixVente = input.prixVente,
                    prixRevient = input.prixRevient,
                    prixMinimum = input.prixMinimum,
                    remiseMaxPct = input.remiseMaxPct,
                    stockMin = input.stockMin,
                    stockMax = input.stockMax,
                    stockSecurite = input.stockSecurite,
                    siteId = input.siteId,
                    emplacement = ProductValidation.normaliseTexte(input.emplacement),
                    fournisseurId = input.fournisseurId,
                    refFournisseur = ProductValidation.normaliseTexte(input.refFournisseur),
                    description = ProductValidation.normaliseTexte(input.description),
                    poids = input.poids,
                    volume = input.volume,
                    origine = ProductValidation.normaliseTexte(input.origine),
                    notes = ProductValidation.normaliseTexte(input.notes),
                    statut = ProductStatus.ACTIF,
                    createdAt = now,
                ),
            )
            if (stock > 0.0) {
                val siteId = input.siteId!!
                stockDao.ensureRow(produitId, siteId)
                stockDao.remplacer(ProductStockEntity(produitId, siteId, stock))
                movementDao.insert(
                    StockMovementEntity(
                        produitId = produitId,
                        siteId = siteId,
                        type = StockMovementType.ENTREE,
                        quantite = stock,
                        motif = "STOCK_INITIAL",
                        reference = code,
                        horodatage = now,
                    ),
                )
            }
            produitId
        }
        journalManager.log("STOCK", "CREATION_PRODUIT", "Produit $code — $nomNormalise")
        return Result.Succes(id, code, stock)
    }
}

/**
 * Édition d'un produit (jamais de suppression physique — C7).
 * Le stock courant **n'est pas modifiable** ici : toute variation passe par
 * un mouvement de stock (spec §7 IMPORTANT / §43).
 */
class UpdateProductUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(id: Long, input: ProductInput): Boolean {
        val nomNormalise = input.nom.trim()
        if (nomNormalise.isEmpty()) return false
        if (!ProductValidation.inputEstValide(input)) return false
        if (licenceManager.isReadOnly()) return false
        val existant = productDao.getById(id) ?: return false
        val barcode = ProductValidation.normaliseTexte(input.barcode)
        if (barcode != null && barcode != existant.barcode) {
            val porteur = productDao.getByBarcode(barcode)
            if (porteur != null && porteur.id != id) return false
        }
        productDao.update(
            existant.copy(
                nom = nomNormalise,
                type = input.type,
                reference = ProductValidation.normaliseTexte(input.reference),
                barcode = barcode,
                sku = ProductValidation.normaliseTexte(input.sku),
                categorieId = input.categorieId,
                marque = ProductValidation.normaliseTexte(input.marque),
                unite = ProductValidation.normaliseTexte(input.unite),
                prixAchat = input.prixAchat,
                prixVente = input.prixVente,
                prixRevient = input.prixRevient,
                prixMinimum = input.prixMinimum,
                remiseMaxPct = input.remiseMaxPct,
                stockMin = input.stockMin,
                stockMax = input.stockMax,
                stockSecurite = input.stockSecurite,
                siteId = input.siteId,
                emplacement = ProductValidation.normaliseTexte(input.emplacement),
                fournisseurId = input.fournisseurId,
                refFournisseur = ProductValidation.normaliseTexte(input.refFournisseur),
                description = ProductValidation.normaliseTexte(input.description),
                poids = input.poids,
                volume = input.volume,
                origine = ProductValidation.normaliseTexte(input.origine),
                notes = ProductValidation.normaliseTexte(input.notes),
            ),
        )
        journalManager.log("STOCK", "MODIFICATION_PRODUIT", "Produit ${existant.code} — $nomNormalise")
        return true
    }
}

/**
 * Changement de statut (Actif ↔ Inactif) — jamais de suppression physique (C7).
 * Un produit désactivé ne peut plus être ajouté à un panier, mais son historique
 * (ventes, mouvements) reste intact.
 */
class ChangerStatutProduitUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(id: Long, actif: Boolean): Boolean {
        if (licenceManager.isReadOnly()) return false
        val existant = productDao.getById(id) ?: return false
        if (actif == existant.active) return true
        if (actif) {
            productDao.update(existant.copy(statut = ProductStatus.ACTIF, active = true))
        } else {
            productDao.desactiver(id)
        }
        journalManager.log(
            "STOCK",
            if (actif) "ACTIVATION_PRODUIT" else "DESACTIVATION_PRODUIT",
            "Produit ${existant.code} — ${if (actif) "activé" else "désactivé"}",
        )
        return true
    }
}

/**
 * Gestion des catégories de produit (spec §31) — même convention que les
 * catégories clients : suppression verrouillée si rattachée.
 */
class CategorieProduitUseCases @Inject constructor(
    private val productDao: ProductDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class SuppressionResult {
        data object Supprimee : SuppressionResult()
        data object CategorieUtilisee : SuppressionResult()
        data object LectureSeule : SuppressionResult()
        data object Introuvable : SuppressionResult()
    }

    fun observer(): Flow<List<ProductCategoryEntity>> = productDao.observeCategories()

    /** @return l'identifiant créé, ou null si l'écriture est interdite/invalide. */
    suspend fun creer(nom: String): Long? {
        val nomNormalise = nom.trim()
        if (licenceManager.isReadOnly() || !ProductValidation.nomEstValide(nom)) return null
        val id = productDao.insertCategorie(ProductCategoryEntity(nom = nomNormalise))
        journalManager.log("STOCK", "CATEGORIE_CREEE", "Catégorie produit : $nomNormalise")
        return id
    }

    suspend fun supprimer(id: Long): SuppressionResult {
        if (licenceManager.isReadOnly()) return SuppressionResult.LectureSeule
        if (productDao.getCategorieById(id) == null) return SuppressionResult.Introuvable
        if (productDao.countProductsAvecCategorie(id) > 0) return SuppressionResult.CategorieUtilisee
        productDao.deleteCategorie(id)
        journalManager.log("STOCK", "CATEGORIE_SUPPRIMEE", "Catégorie produit id=$id supprimée")
        return SuppressionResult.Supprimee
    }
}
