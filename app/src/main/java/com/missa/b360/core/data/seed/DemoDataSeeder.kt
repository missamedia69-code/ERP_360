package com.missa.b360.core.data.seed

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStatus
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.data.entity.UserEntity
import com.missa.b360.core.data.repository.ProfilActivationRepository
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.core.domain.model.CockpitRules
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.security.PinHasher
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chargeur de données d'exemple complètes et réalistes pour tester et utiliser immédiatement
 * l'application MISSA BUSINESS 360 sans écran vide.
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
    private val siteDao: SiteDao,
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val clientDao: ClientDao,
    private val fournisseurDao: FournisseurDao,
    private val productDao: ProductDao,
    private val productStockDao: ProductStockDao,
    private val stockMovementDao: StockMovementDao,
    private val operationRecordDao: OperationRecordDao,
    private val compteTresorerieDao: CompteTresorerieDao,
    private val mouvementTresorerieDao: MouvementTresorerieDao,
    private val taskDao: TaskDao,
    private val notificationDao: NotificationDao,
    private val taxDao: TaxDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val settingsStore: SettingsStore,
    private val profilActivationRepository: ProfilActivationRepository,
    private val licenceManager: LicenceManager,
) {

    /**
     * Alimente la base si l'entreprise ou les tables principales sont vides.
     */
    suspend fun seedIfEmpty(force: Boolean = false) {
        val ent = enterpriseDao.get()
        val nbProduits = productDao.count()
        val nbClients = clientDao.observeAll().firstOrNull()?.size ?: 0

        if (!force && ent != null && nbProduits > 0 && nbClients > 0) {
            return
        }

        seedDemoData()
    }

    /**
     * Génère un jeu de données complet et cohérent pour tous les modules actifs.
     */
    suspend fun seedDemoData() {
        val now = System.currentTimeMillis()
        val startOfToday = CockpitRules.debutJour(now)
        val startOfYesterday = startOfToday - 86_400_000L

        // 1. Initialisation Licence & Essai
        licenceManager.ensureTrialStarted(now)

        // 2. Profil d'activité & Réglages
        profilActivationRepository.mettreAJourProfil(
            profil = ProfilActivite.ASV,
            palier = PalierTaille.P3,
            venteSansStock = false,
        )
        settingsStore.set(SettingsStore.Keys.DEVISE, "FCFA")
        settingsStore.set(SettingsStore.Keys.PAYS, "CM")
        settingsStore.set(SettingsStore.Keys.LANGUE, "fr")

        // 3. Entreprise
        val entreprise = EnterpriseEntity(
            id = 1L,
            nom = "MISSA DISTRIBUTION SARL",
            secteur = "Commerce & Distribution générale",
            adresse = "Boulevard de la Liberté, Akwa, Douala",
            telephone = "+237 690 00 00 00",
            email = "contact@missadistribution.com",
            devise = "FCFA",
            langue = "fr",
            pays = "Cameroun",
            numeroFiscal = "M051800012345A",
            registreCommerce = "RC/DLA/2020/B/145",
            logoUri = null,
            profilActivite = ProfilActivite.ASV.name,
            palierTaille = PalierTaille.P3.name,
            onboardingTermine = true,
        )
        enterpriseDao.upsert(entreprise)

        // 4. Sites & Dépôts
        val existingSiteId = siteDao.idPrincipal()
        val site1Id = existingSiteId ?: siteDao.insert(
            SiteEntity(
                nom = "Siège & Magasin Akwa",
                type = "Boutique",
                adresse = "Boulevard de la Liberté, Akwa, Douala",
                principal = true,
            ),
        )
        val site2Id = siteDao.insert(
            SiteEntity(
                nom = "Dépôt Central Bonabéri",
                type = "Entrepôt",
                adresse = "Zone Industrielle Bonabéri, Douala",
                principal = false,
            ),
        )

        // 5. Rôles & Utilisateur Propriétaire
        var roleProprietaire = roleDao.getByNom("Propriétaire")
        if (roleProprietaire == null) {
            val roleId = roleDao.insert(RoleEntity(nom = "Propriétaire", type = "SYSTEM"))
            roleProprietaire = roleDao.getById(roleId)
            val permissions = buildList {
                val actions = listOf("VIEW", "CREATE", "EDIT", "DELETE", "VALIDATE")
                val moduleNames: List<String> = AppModule.entries.map { it.name } + listOf("ADMIN")
                for (mod in moduleNames) {
                    for (act in actions) {
                        add(RolePermissionEntity(roleId, mod, act, granted = true))
                    }
                }
            }
            roleDao.insertPermissions(permissions)
        }

        if (userDao.findByEmail("alexandre@missadistribution.com") == null) {
            userDao.insert(
                UserEntity(
                    nom = "Alexandre Missa",
                    emailSecours = "alexandre@missadistribution.com",
                    pinHash = PinHasher.encode("1234"),
                    roleId = roleProprietaire?.id,
                    actif = true,
                    createdAt = now,
                ),
            )
        }

        // 6. Taxes et Modes de règlement
        if (taxDao.count() == 0) {
            taxDao.insert(TaxEntity(nom = "TVA Cameroun", taux = 19.25, parDefaut = true))
            taxDao.insert(TaxEntity(nom = "Exonéré (0%)", taux = 0.0, parDefaut = false))
        }
        if (paymentMethodDao.count() == 0) {
            paymentMethodDao.insertAll(
                listOf(
                    PaymentMethodEntity(nom = "Espèces"),
                    PaymentMethodEntity(nom = "Mobile Money (Orange/MTN)"),
                    PaymentMethodEntity(nom = "Virement bancaire"),
                    PaymentMethodEntity(nom = "Chèque"),
                ),
            )
        }

        // 7. Comptes de Trésorerie
        val comptesExistants = compteTresorerieDao.getAll()
        val caisse1Id = comptesExistants.firstOrNull { it.type == TypeCompteTresorerie.CAISSE.name }?.id
            ?: compteTresorerieDao.insert(
                CompteTresorerieEntity(
                    nom = "Caisse Principale (Showroom)",
                    type = TypeCompteTresorerie.CAISSE.name,
                    etablissement = "Magasin Akwa",
                    numero = "CAISSE-01",
                    soldeInitial = 850_000.0,
                    actif = true,
                    createdAt = now,
                ),
            )

        val banqueId = comptesExistants.firstOrNull { it.type == TypeCompteTresorerie.BANQUE.name }?.id
            ?: compteTresorerieDao.insert(
                CompteTresorerieEntity(
                    nom = "Compte Courant Société Générale",
                    type = TypeCompteTresorerie.BANQUE.name,
                    etablissement = "Société Générale Cameroun",
                    numero = "10003-0012-12345678901-45",
                    soldeInitial = 6_450_000.0,
                    actif = true,
                    createdAt = now,
                ),
            )

        val momoId = comptesExistants.firstOrNull { it.type == TypeCompteTresorerie.MOBILE_MONEY.name }?.id
            ?: compteTresorerieDao.insert(
                CompteTresorerieEntity(
                    nom = "Compte Mobile Money",
                    type = TypeCompteTresorerie.MOBILE_MONEY.name,
                    etablissement = "Orange & MTN Money",
                    numero = "+237 690 00 00 00",
                    soldeInitial = 320_000.0,
                    actif = true,
                    createdAt = now,
                ),
            )

        // 8. Clients
        val c1Id = clientDao.insert(
            ClientEntity(
                code = "CLI-2026-0001",
                nom = "Supermarché Le Progrès",
                type = ClientType.ENTREPRISE,
                telephone = "+237 677 12 34 56",
                adresse = "Akwa, Douala",
                nif = "M011500012345K",
                conditionPaiementJours = 30,
                statut = ClientStatus.ACTIF,
                createdAt = now - 14 * 86_400_000L,
            ),
        )
        val c2Id = clientDao.insert(
            ClientEntity(
                code = "CLI-2026-0002",
                nom = "ETS Kono & Frères",
                type = ClientType.ENTREPRISE,
                telephone = "+237 699 88 77 66",
                adresse = "Marché Central, Yaoundé",
                nif = "M021600098765P",
                conditionPaiementJours = 15,
                statut = ClientStatus.ACTIF,
                createdAt = now - 7 * 86_400_000L,
            ),
        )
        val c3Id = clientDao.insert(
            ClientEntity(
                code = "CLI-2026-0003",
                nom = "Boutique Étoile Moderne",
                type = ClientType.PARTICULIER,
                telephone = "+237 671 22 33 44",
                adresse = "Centre-ville, Bafoussam",
                conditionPaiementJours = 0,
                statut = ClientStatus.ACTIF,
                createdAt = now - 3 * 86_400_000L,
            ),
        )
        clientDao.insert(
            ClientEntity(
                code = "CLI-2026-0004",
                nom = "Hôtel Résidence Palace",
                type = ClientType.ENTREPRISE,
                telephone = "+237 691 55 44 33",
                adresse = "Boulevard des Plages, Kribi",
                conditionPaiementJours = 30,
                statut = ClientStatus.ACTIF,
                createdAt = now - 1 * 86_400_000L,
            ),
        )

        // 9. Fournisseurs
        val f1Id = fournisseurDao.insert(
            FournisseurEntity(
                code = "FRN-2026-0001",
                nom = "Agro-Industrie du Littoral",
                telephone = "+237 670 11 22 33",
                adresse = "Zone Industrielle Bassa, Douala",
                statut = FournisseurStatus.ACTIF,
                type = TypeFournisseur.ENTREPRISE,
                pays = "CM",
                devise = "FCFA",
                createdAt = now - 20 * 86_400_000L,
            ),
        )
        val f2Id = fournisseurDao.insert(
            FournisseurEntity(
                code = "FRN-2026-0002",
                nom = "Société Africaine de Raffinage",
                telephone = "+237 694 55 66 77",
                adresse = "Zone Industrielle Bonabéri, Douala",
                statut = FournisseurStatus.ACTIF,
                type = TypeFournisseur.ENTREPRISE,
                pays = "CM",
                devise = "FCFA",
                createdAt = now - 15 * 86_400_000L,
            ),
        )
        fournisseurDao.insert(
            FournisseurEntity(
                code = "FRN-2026-0003",
                nom = "Import & Logistique Océane",
                telephone = "+237 673 88 99 00",
                adresse = "Port Autonome de Douala",
                statut = FournisseurStatus.ACTIF,
                type = TypeFournisseur.ENTREPRISE,
                pays = "CM",
                devise = "FCFA",
                createdAt = now - 10 * 86_400_000L,
            ),
        )

        // 10. Catégories & Articles
        val catAlim = productDao.insertCategorie(ProductCategoryEntity(nom = "Alimentation & Épicerie", type = "PRODUIT"))
        val catHyg = productDao.insertCategorie(ProductCategoryEntity(nom = "Hygiène & Entretien", type = "PRODUIT"))
        val catBoiss = productDao.insertCategorie(ProductCategoryEntity(nom = "Boissons & Rafraîchissements", type = "PRODUIT"))

        data class ArticleSeed(
            val code: String,
            val nom: String,
            val pa: Double,
            val pv: Double,
            val unite: String,
            val cat: Long,
            val q1: Double,
            val q2: Double,
            val min: Double,
        )

        val catalogue = listOf(
            ArticleSeed("ART-2026-0001", "Riz Parfumé Supérieur 25kg", 16_500.0, 19_500.0, "Sac 25kg", catAlim, 45.0, 40.0, 10.0),
            ArticleSeed("ART-2026-0002", "Huile Raffinée Végétale 5L", 5_200.0, 6_500.0, "Bidon 5L", catAlim, 60.0, 80.0, 20.0),
            ArticleSeed("ART-2026-0003", "Sucre Blond en Poudre 1kg (Ctn 20)", 14_000.0, 17_000.0, "Carton 20kg", catAlim, 22.0, 20.0, 8.0),
            ArticleSeed("ART-2026-0004", "Savon de Ménage Extra (Ctn 50)", 8_500.0, 11_000.0, "Carton 50 pcs", catHyg, 35.0, 30.0, 10.0),
            ArticleSeed("ART-2026-0005", "Jus de Fruits Tropical 1L (Pack 12)", 9_000.0, 12_500.0, "Pack 12 pcs", catBoiss, 50.0, 70.0, 15.0),
        )

        for (art in catalogue) {
            val pId = productDao.insert(
                ProductEntity(
                    code = art.code,
                    nom = art.nom,
                    type = ProductType.ACHATE_REVENDU,
                    prixAchat = art.pa,
                    prixVente = art.pv,
                    prixRevient = art.pa,
                    unite = art.unite,
                    categorieId = art.cat,
                    siteId = site1Id,
                    statut = ProductStatus.ACTIF,
                    active = true,
                    stockMin = art.min,
                    stockSecurite = art.min / 2.0,
                    createdAt = now - 10 * 86_400_000L,
                ),
            )
            // Stock site 1
            productStockDao.ensureRow(pId, site1Id)
            productStockDao.remplacer(pId, site1Id, art.q1)
            productStockDao.ajouterValeur(pId, site1Id, art.q1 * art.pa)

            // Stock site 2
            productStockDao.ensureRow(pId, site2Id)
            productStockDao.remplacer(pId, site2Id, art.q2)
            productStockDao.ajouterValeur(pId, site2Id, art.q2 * art.pa)

            // Mouvements de stock
            stockMovementDao.insert(
                StockMovementEntity(
                    produitId = pId,
                    siteId = site1Id,
                    type = StockMovementType.ENTREE,
                    quantite = art.q1,
                    motif = "Stock initial magasin",
                    reference = "INI-2026",
                    horodatage = now - 5 * 86_400_000L,
                ),
            )
        }

        // 11. Opérations Métier (Ventes & Achats)
        // Ventes d'aujourd'hui
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.VENTE.name,
                reference = "VTE-2026-0042",
                title = "Vente au comptoir - Riz & Huile",
                counterpart = "Supermarché Le Progrès",
                tiersId = c1Id,
                amount = 245_000.0,
                quantity = 15.0,
                direction = OperationDirection.IN.name,
                status = OperationStatus.VALIDATED.name,
                createdAt = now - 2 * 3600_000L,
            ),
        )
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.VENTE.name,
                reference = "VTE-2026-0043",
                title = "Facture #43 - Produits Alimentaires",
                counterpart = "Boutique Étoile Moderne",
                tiersId = c3Id,
                amount = 180_000.0,
                quantity = 12.0,
                direction = OperationDirection.IN.name,
                status = OperationStatus.VALIDATED.name,
                createdAt = now - 45 * 60_000L,
            ),
        )
        // Vente d'hier (pour tendance)
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.VENTE.name,
                reference = "VTE-2026-0041",
                title = "Commande Livrée - Gros volume",
                counterpart = "ETS Kono & Frères",
                tiersId = c2Id,
                amount = 320_000.0,
                quantity = 25.0,
                direction = OperationDirection.IN.name,
                status = OperationStatus.VALIDATED.name,
                createdAt = startOfYesterday + 11 * 3600_000L,
            ),
        )
        // Achats d'aujourd'hui
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.ACHATS.name,
                reference = "ACH-2026-0018",
                title = "Réapprovisionnement Riz & Céréales",
                counterpart = "Agro-Industrie du Littoral",
                tiersId = f1Id,
                amount = 165_000.0,
                quantity = 10.0,
                direction = OperationDirection.OUT.name,
                status = OperationStatus.VALIDATED.name,
                createdAt = now - 3 * 3600_000L,
            ),
        )
        // Achat d'hier (pour tendance)
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.ACHATS.name,
                reference = "ACH-2026-0017",
                title = "Lot Huile Végétale Raffinée",
                counterpart = "Société Africaine de Raffinage",
                tiersId = f2Id,
                amount = 250_000.0,
                quantity = 50.0,
                direction = OperationDirection.OUT.name,
                status = OperationStatus.VALIDATED.name,
                createdAt = startOfYesterday + 14 * 3600_000L,
            ),
        )
        // Devis en cours
        operationRecordDao.insert(
            OperationRecordEntity(
                module = OperationModule.DEVIS.name,
                reference = "DEV-2026-0015",
                title = "Devis Fourniture Hôtel Palace",
                counterpart = "Hôtel Résidence Palace",
                amount = 450_000.0,
                quantity = 30.0,
                status = OperationStatus.DRAFT.name,
                createdAt = now - 5 * 3600_000L,
            ),
        )

        // 12. Mouvements de Trésorerie
        mouvementTresorerieDao.insert(
            com.missa.b360.core.data.entity.MouvementTresorerieEntity(
                compteId = caisse1Id,
                date = now - 2 * 3600_000L,
                sens = SensMouvement.IN.name,
                montant = 245_000.0,
                libelle = "Règlement Vente VTE-2026-0042",
                tiers = "Supermarché Le Progrès",
                modePaiement = "Espèces",
                reference = "VTE-2026-0042",
                rapproche = true,
                createdAt = now - 2 * 3600_000L,
            ),
        )
        mouvementTresorerieDao.insert(
            com.missa.b360.core.data.entity.MouvementTresorerieEntity(
                compteId = momoId,
                date = now - 45 * 60_000L,
                sens = SensMouvement.IN.name,
                montant = 180_000.0,
                libelle = "Paiement Mobile Money VTE-2026-0043",
                tiers = "Boutique Étoile Moderne",
                modePaiement = "Mobile Money (Orange/MTN)",
                reference = "VTE-2026-0043",
                rapproche = true,
                createdAt = now - 45 * 60_000L,
            ),
        )
        mouvementTresorerieDao.insert(
            com.missa.b360.core.data.entity.MouvementTresorerieEntity(
                compteId = banqueId,
                date = now - 3 * 3600_000L,
                sens = SensMouvement.OUT.name,
                montant = 165_000.0,
                libelle = "Règlement Fournisseur ACH-2026-0018",
                tiers = "Agro-Industrie du Littoral",
                modePaiement = "Virement bancaire",
                reference = "ACH-2026-0018",
                rapproche = true,
                createdAt = now - 3 * 3600_000L,
            ),
        )

        // 13. Tâches du jour
        taskDao.insert(
            TaskEntity(
                titre = "Livraison commande Supermarché Le Progrès (Facture #42)",
                notes = "Livraison au dépôt d'Akwa avant 16h.",
                echeance = now + 4 * 3600_000L,
                createdAt = now - 2 * 3600_000L,
            ),
        )
        taskDao.insert(
            TaskEntity(
                titre = "Vérification inventaire hebdomadaire Dépôt Bonabéri",
                notes = "Contrôler les écarts physiques sur le riz et le sucre.",
                echeance = now + 24 * 3600_000L,
                createdAt = now - 3600_000L,
            ),
        )
        taskDao.insert(
            TaskEntity(
                titre = "Relancer règlement facture échu ETS Kono & Frères",
                notes = "Solde restant : 320 000 FCFA.",
                echeance = now + 48 * 3600_000L,
                createdAt = now,
            ),
        )

        // 14. Notifications de bienvenue
        notificationDao.insert(
            NotificationEntity(
                type = "BIENVENUE",
                titre = "Bienvenue sur MISSA BUSINESS 360",
                message = "Votre entreprise MISSA DISTRIBUTION SARL est configurée et prête à l'emploi avec toutes les fonctionnalités actives.",
                date = now - 4 * 3600_000L,
                lue = false,
            ),
        )
        notificationDao.insert(
            NotificationEntity(
                type = "VENTE",
                titre = "Vente enregistrée avec succès",
                message = "La facture VTE-2026-0042 de 245 000 FCFA a été validée et encaissée en caisse.",
                date = now - 2 * 3600_000L,
                lue = false,
            ),
        )
    }
}
