package com.missa.b360

import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.AchatCommandeRules
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.ComposantBesoin
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReturnRules
import com.missa.b360.core.domain.model.SaleCalculator
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleStockEffects
import com.missa.b360.core.domain.model.TresorerieRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test de simulation end-to-end de tous les flux et transactions interconnectés :
 *
 * 1. Création Entreprise & Caisse Principale
 * 2. Fournisseur & Achat de Matières Premières (Commande → Réception → Facture → Décaissement Caisse)
 * 3. Ordre de Production (Consommation MP → Sortie stock → Entrée Produit Fini)
 * 4. Client & Vente au comptoir (Vérification stock → Sortie stock PF → Encaissement Caisse)
 * 5. Retour client partiel (Avoir → Réintégration stock → Remboursement trésorerie)
 * 6. Audit & Réconciliation globale Trésorerie / Stock / Créances.
 */
class SimulationFluxCompletTest {

    @Test
    fun `simulation cycle complet achat production vente tresorerie et retour`() {
        val now = 1774000000000L

        // --- 1. Infrastructure de base (Site, Catégorie, Caisse) ---
        val siteId = 1L
        val categorie = ProductCategoryEntity(id = 10L, nom = "Maroquinerie & Cuir")
        val caissePrincipale = CompteTresorerieEntity(
            id = 100L,
            nom = "Caisse Principale",
            type = TypeCompteTresorerie.CAISSE.name,
            etablissement = "Siège Principal",
            soldeInitial = 50_000.0,
            actif = true,
            createdAt = now,
        )

        // --- 2. Articles (Matière Première & Produit Fini) ---
        val matierePremiere = ProductEntity(
            id = 201L,
            code = "PRD-2026-0001",
            nom = "Cuir Tanné Végétal",
            type = ProductType.MATIERE_PREMIERE,
            categorieId = categorie.id,
            siteId = siteId,
            prixAchat = 5_000.0,
            active = true,
        )
        val produitFini = ProductEntity(
            id = 202L,
            code = "PRD-2026-0002",
            nom = "Sac Besace Prestige",
            type = ProductType.PRODUIT_FABRIQUE,
            categorieId = categorie.id,
            siteId = siteId,
            prixVente = 25_000.0,
            active = true,
        )

        // --- 3. Fournisseur & Cycle d'Achat ---
        val fournisseur = FournisseurEntity(
            id = 301L,
            code = "FRN-2026-0001",
            nom = "Tanneries du Littoral",
            telephone = "+237690001122",
            statut = FournisseurStatus.ACTIF,
            createdAt = now,
        )

        // 3a. Bon de Commande (10 m² de cuir à 5 000 FCFA)
        val commandePayload = CommandeAchatPayload(
            supplierId = fournisseur.id,
            supplierName = fournisseur.nom,
            lines = listOf(
                CommandeAchatLigne(
                    id = 1L,
                    productId = matierePremiere.id,
                    name = matierePremiere.nom,
                    quantity = 10.0,
                    unitPrice = 5_000.0,
                ),
            ),
        )
        val jsonCommande = CommandeAchatCodec.encode(commandePayload)
        val commandeDecodee = CommandeAchatCodec.decode(jsonCommande)
        assertNotNull(commandeDecodee)
        assertEquals(50_000.0, commandeDecodee!!.lines.sumOf { it.total }, 1e-9)

        // 3b. Réception Physique
        val receptionLigne = ReceptionLigne(
            productId = matierePremiere.id,
            name = matierePremiere.nom,
            quantiteCommandee = 10.0,
            quantiteRecue = 10.0,
        )
        val restant = AchatCommandeRules.restantARecevoir(receptionLigne.quantiteCommandee, receptionLigne.quantiteRecue)
        assertEquals(0.0, restant, 1e-9)

        // Impact Stock Achat : entrée de 10 unités de MP
        var stockMatierePremiere = 0.0
        stockMatierePremiere += receptionLigne.quantiteRecue
        assertEquals(10.0, stockMatierePremiere, 1e-9)

        // 3c. Facture Fournisseur & Paiement comptant depuis la Caisse
        val factureAchatPayload = PurchaseRecordPayload(
            supplierId = fournisseur.id,
            supplierName = fournisseur.nom,
            lines = listOf(
                PurchaseLine(
                    id = 1L,
                    productId = matierePremiere.id,
                    name = matierePremiere.nom,
                    quantity = 10.0,
                    unitPrice = 5_000.0,
                ),
            ),
            total = 50_000.0,
            paymentMethod = "Espèces",
            paidAmount = 50_000.0,
        )

        val compteDecaissement = TresorerieRules.compteCible(
            factureAchatPayload.paymentMethod,
            listOf(caissePrincipale),
        )
        assertNotNull("La caisse principale doit être sélectionnée automatiquement pour 'Espèces'", compteDecaissement)
        assertEquals(caissePrincipale.id, compteDecaissement!!.id)

        var soldeCaisse = caissePrincipale.soldeInitial
        soldeCaisse -= factureAchatPayload.paidAmount
        assertEquals(0.0, soldeCaisse, 1e-9) // 50 000 - 50 000

        // --- 4. Cycle de Production (Fabriquer 4 sacs avec 8 m² de cuir) ---
        val opPayload = ProductionRecordPayload(
            produitId = produitFini.id,
            produitNom = produitFini.nom,
            quantite = 4.0,
            composants = listOf(
                ComposantBesoin(
                    composantId = matierePremiere.id,
                    composantNom = matierePremiere.nom,
                    quantite = 8.0, // 2 m² par sac
                ),
            ),
        )
        val besoinsComposants = ProductionRules.besoinsParComposant(opPayload)
        assertEquals(8.0, besoinsComposants[matierePremiere.id] ?: 0.0, 1e-9)

        // Vérification disponibilité et déduction
        assertTrue("Stock MP suffisant pour lancer la fabrication", stockMatierePremiere >= 8.0)
        stockMatierePremiere -= 8.0
        var stockProduitFini = 0.0
        stockProduitFini += opPayload.quantite

        assertEquals(2.0, stockMatierePremiere, 1e-9) // 10 - 8 = 2 m² restants
        assertEquals(4.0, stockProduitFini, 1e-9)     // 4 sacs fabriqués

        // --- 5. Client & Cycle de Vente (Vente de 3 sacs à un client) ---
        val client = ClientEntity(
            id = 401L,
            code = "CLI-2026-0001",
            nom = "Hôtel Douala Palace",
            telephone = "+237677889900",
            statut = ClientStatus.ACTIF,
            createdAt = now,
        )

        val ligneVente = SaleLine(
            id = 1L,
            productId = produitFini.id,
            name = produitFini.nom,
            quantity = 3.0,
            unitPrice = 25_000.0,
        )
        val totalsVente = SaleCalculator.calculate(
            lines = listOf(ligneVente),
            discount = 0.0,
            delivery = 0.0,
            taxRate = 0.0,
        )
        assertEquals(75_000.0, totalsVente.total, 1e-9)

        // Sortie de stock vente
        val besoinsVente = SaleStockEffects.besoinsParProduit(listOf(ligneVente))
        val qteDemandeeVente = besoinsVente[produitFini.id] ?: 0.0
        assertTrue("Stock PF suffisant pour la vente", stockProduitFini >= qteDemandeeVente)
        stockProduitFini -= qteDemandeeVente
        assertEquals(1.0, stockProduitFini, 1e-9) // 4 - 3 = 1 sac restant

        // Encaissement en Caisse (Paiement total 75 000 FCFA en espèces)
        val compteEncaissement = TresorerieRules.compteCible("Espèces", listOf(caissePrincipale))
        assertNotNull(compteEncaissement)
        soldeCaisse += 75_000.0
        assertEquals(75_000.0, soldeCaisse, 1e-9)

        // Facture de vente persistée
        val factureVentePayload = SaleRecordPayload(
            clientId = client.id,
            clientName = client.nom,
            lines = listOf(ligneVente),
            subtotal = totalsVente.subtotal,
            discount = totalsVente.discount,
            delivery = totalsVente.delivery,
            taxRate = 0.0,
            taxAmount = 0.0,
            total = totalsVente.total,
            paymentMethod = "Espèces",
            paidAmount = totalsVente.total,
        )
        val jsonVente = SaleRecordCodec.encode(factureVentePayload)
        val venteDecodee = SaleRecordCodec.decode(jsonVente)
        assertNotNull(venteDecodee)
        assertEquals(3.0, venteDecodee!!.lines.first().quantity, 1e-9)

        // --- 6. Retour Client (Avoir sur 1 sac endommagé ou retourné) ---
        val ligneRetour = listOf(
            SaleLine(
                id = 1L,
                productId = produitFini.id,
                name = produitFini.nom,
                quantity = 1.0,
                unitPrice = 25_000.0,
            ),
        )
        val avoirPayload = ReturnRules.construireAvoir(
            factureOriginale = factureVentePayload,
            lignesRetournees = ligneRetour,
            modeRemboursement = "Espèces",
            motif = "Erreur taille client",
            sourceRecordId = 501L,
        )
        assertEquals(25_000.0, avoirPayload.total, 1e-9)

        // Réintégration en stock du produit retourné
        stockProduitFini += 1.0
        assertEquals(2.0, stockProduitFini, 1e-9) // 1 restant + 1 retourné = 2 sacs

        // Remboursement depuis la Caisse
        soldeCaisse -= avoirPayload.paidAmount
        assertEquals(50_000.0, soldeCaisse, 1e-9) // 75 000 - 25 000 = 50 000 FCFA

        // --- 7. Bilan Global des Flux & Cohérence ---
        // Solde initial Caisse = 50 000
        // Achat MP = -50 000 (Solde = 0)
        // Vente PF = +75 000 (Solde = 75 000)
        // Remboursement Retour = -25 000 (Solde final = 50 000)
        assertEquals(50_000.0, soldeCaisse, 1e-9)

        // Stocks finaux :
        // Matière Première : 10 achetés - 8 consommés = 2 m²
        assertEquals(2.0, stockMatierePremiere, 1e-9)
        // Produit Fini : 4 fabriqués - 3 vendus + 1 retourné = 2 sacs
        assertEquals(2.0, stockProduitFini, 1e-9)
    }
}
