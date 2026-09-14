package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.GroupeAchatEntity
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.GroupeComptabiliteEntity
import com.missa.b360.core.data.entity.GroupeMaintenanceEntity
import com.missa.b360.core.data.entity.GroupeProductionEntity
import com.missa.b360.core.data.entity.GroupeStockEntity
import com.missa.b360.core.data.entity.GroupeVenteEntity
import com.missa.b360.core.data.entity.MethodeValorisation
import com.missa.b360.core.data.entity.ProductType

/** Un groupe et ses six extensions, avant écriture en base. */
data class ModeleGroupe(
    val groupe: GroupeArticleEntity,
    val stock: GroupeStockEntity,
    val achat: GroupeAchatEntity,
    val vente: GroupeVenteEntity,
    val production: GroupeProductionEntity,
    val maintenance: GroupeMaintenanceEntity,
    val comptabilite: GroupeComptabiliteEntity,
)

/**
 * Groupes d'articles livrés d'office à l'installation.
 *
 * Ils couvrent les familles que toute entreprise manipule, et reprennent les
 * natures que `ProductType` codait en dur — un utilisateur retrouve donc son
 * vocabulaire sans avoir à paramétrer quoi que ce soit avant de saisir son
 * premier article. Les comptes suivent le référentiel du pays : le plan
 * SYSCOHADA et le plan français ne numérotent pas les stocks de la même façon.
 */
object GroupesStandards {

    /** Comptes par défaut, selon le référentiel comptable de l'entité. */
    private data class Comptes(
        val stockMarchandises: String,
        val stockMatieres: String,
        val stockProduitsFinis: String,
        val achatMarchandises: String,
        val achatMatieres: String,
        val venteMarchandises: String,
        val venteServices: String,
        val ecartInventaire: String,
        val immobilisations: String,
    )

    /**
     * SYSCOHADA (Afrique de l'Ouest et centrale) et PCG (France) partagent la
     * logique par classes, mais pas les numéros. Tout autre référentiel retombe
     * sur SYSCOHADA, majoritaire sur le marché visé.
     */
    private fun comptes(referentiel: String?): Comptes = when (referentiel?.uppercase()) {
        "PCG" -> Comptes(
            stockMarchandises = "370",
            stockMatieres = "310",
            stockProduitsFinis = "355",
            achatMarchandises = "607",
            achatMatieres = "601",
            venteMarchandises = "707",
            venteServices = "706",
            ecartInventaire = "6037",
            immobilisations = "215",
        )
        else -> Comptes(
            stockMarchandises = "311",
            stockMatieres = "321",
            stockProduitsFinis = "361",
            achatMarchandises = "601",
            achatMatieres = "602",
            venteMarchandises = "701",
            venteServices = "706",
            ecartInventaire = "6031",
            immobilisations = "241",
        )
    }

    /**
     * Nature historique correspondant au groupe, pour que les articles déjà
     * saisis retrouvent leur famille sans intervention.
     */
    val natureParCode: Map<String, ProductType> = mapOf(
        "MARCH" to ProductType.ACHATE_REVENDU,
        "MP" to ProductType.MATIERE_PREMIERE,
        "PF" to ProductType.FABRIQUE,
        "SE" to ProductType.COMPOSE,
        "CONSO" to ProductType.CONNOMMABLE,
    )

    fun modeles(referentiel: String?, maintenant: Long): List<ModeleGroupe> {
        val c = comptes(referentiel)
        return listOf(
            modele(
                code = "MARCH",
                nom = "Marchandises",
                description = "Articles achetés puis revendus en l'état.",
                maintenant = maintenant,
                compteStock = c.stockMarchandises,
                compteCharge = c.achatMarchandises,
                compteProduit = c.venteMarchandises,
                ecart = c.ecartInventaire,
                achetable = true,
                vendable = true,
            ),
            modele(
                code = "MP",
                nom = "Matières premières",
                description = "Consommées par la production, jamais vendues en l'état.",
                maintenant = maintenant,
                compteStock = c.stockMatieres,
                compteCharge = c.achatMatieres,
                compteProduit = null,
                ecart = c.ecartInventaire,
                achetable = true,
                vendable = false,
            ),
            modele(
                code = "PF",
                nom = "Produits finis",
                description = "Issus d'un ordre de fabrication.",
                maintenant = maintenant,
                compteStock = c.stockProduitsFinis,
                compteCharge = null,
                compteProduit = c.venteMarchandises,
                ecart = c.ecartInventaire,
                achetable = false,
                vendable = true,
                produisible = true,
                nomenclatureRequise = true,
            ),
            modele(
                code = "SE",
                nom = "Sous-ensembles",
                description = "Assemblés puis réutilisés dans un autre article.",
                maintenant = maintenant,
                compteStock = c.stockProduitsFinis,
                compteCharge = null,
                compteProduit = c.venteMarchandises,
                ecart = c.ecartInventaire,
                achetable = false,
                vendable = true,
                produisible = true,
                nomenclatureRequise = true,
            ),
            modele(
                code = "CONSO",
                nom = "Consommables",
                description = "Fournitures d'exploitation, non revendables.",
                maintenant = maintenant,
                compteStock = c.stockMarchandises,
                compteCharge = c.achatMarchandises,
                compteProduit = null,
                ecart = c.ecartInventaire,
                achetable = true,
                vendable = false,
                consommable = true,
                valorise = false,
            ),
            modele(
                code = "SERV",
                nom = "Prestations",
                description = "Services facturés, sans stock ni valorisation.",
                maintenant = maintenant,
                compteStock = null,
                compteCharge = null,
                compteProduit = c.venteServices,
                ecart = null,
                achetable = false,
                vendable = true,
                service = true,
                stocke = false,
                valorise = false,
            ),
            modele(
                code = "EQUIP",
                nom = "Équipements",
                description = "Immobilisations suivies en maintenance.",
                maintenant = maintenant,
                compteStock = null,
                compteCharge = null,
                compteProduit = null,
                ecart = null,
                achetable = true,
                vendable = false,
                stocke = false,
                valorise = false,
                immobilisation = true,
                maintenable = true,
                compteImmobilisation = c.immobilisations,
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun modele(
        code: String,
        nom: String,
        description: String,
        maintenant: Long,
        compteStock: String?,
        compteCharge: String?,
        compteProduit: String?,
        ecart: String?,
        achetable: Boolean,
        vendable: Boolean,
        produisible: Boolean = false,
        nomenclatureRequise: Boolean = false,
        consommable: Boolean = false,
        service: Boolean = false,
        stocke: Boolean = true,
        valorise: Boolean = true,
        immobilisation: Boolean = false,
        maintenable: Boolean = false,
        compteImmobilisation: String? = null,
    ) = ModeleGroupe(
        groupe = GroupeArticleEntity(
            code = code,
            nom = nom,
            description = description,
            stocke = stocke,
            valorise = valorise,
            immobilisation = immobilisation,
            methodeValorisation = if (valorise) MethodeValorisation.CUMP.name else null,
            createdAt = maintenant,
            updatedAt = maintenant,
        ),
        stock = GroupeStockEntity(
            itemGroupId = 0,
            compteStock = compteStock,
            compteEcartInventaire = ecart,
        ),
        achat = GroupeAchatEntity(
            itemGroupId = 0,
            compteCharge = compteCharge,
            compteImmobilisation = compteImmobilisation,
            achetable = achetable,
            consommable = consommable,
            immobilisable = immobilisation,
        ),
        vente = GroupeVenteEntity(
            itemGroupId = 0,
            compteProduit = compteProduit,
            vendable = vendable,
            service = service,
            livraisonRequise = !service,
        ),
        production = GroupeProductionEntity(
            itemGroupId = 0,
            produisible = produisible,
            nomenclatureRequise = nomenclatureRequise,
        ),
        maintenance = GroupeMaintenanceEntity(
            itemGroupId = 0,
            equipementMaintenable = maintenable,
            planRequis = maintenable,
        ),
        comptabilite = GroupeComptabiliteEntity(
            itemGroupId = 0,
            amortissable = immobilisation,
            dureeAmortissementAnnees = if (immobilisation) DUREE_AMORTISSEMENT_DEFAUT else 0,
            stockValorise = valorise,
        ),
    )

    /** Cinq ans : durée usuelle pour le matériel d'exploitation courant. */
    private const val DUREE_AMORTISSEMENT_DEFAUT = 5
}
