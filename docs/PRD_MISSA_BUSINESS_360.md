# MISSA BUSINESS 360 (ERP 360)
## Document des Exigences Produit (Product Requirements Document - PRD)
**Version :** 2.0  
**Date :** 20 Septembre 2026  
**Statut :** Spécification de Référence  
**Plateforme cible :** Android (Smartphone & Tablette), 100% Hors-ligne (Offline-First)  
**Langues supportées :** Français (par défaut), Anglais, Espagnol, Chinois simplifié, Arabe (RTL)  

---

## 1. VISION DU PRODUIT & OBJECTIFS

### 1.1 Contexte & Problématique
Les micro-entreprises, artisans, commerçants, industriels et PME en Afrique subsaharienne et sur les marchés émergents opèrent dans des conditions d'infrastructure réseau instables ou inexistantes, avec des besoins de gestion complets mais sans budget pour de lourds ERP cloud (SAP, Odoo, Sage). 

### 1.2 Vision du Produit
**ERP 360** est un Progiciel de Gestion Intégré (PGI/ERP) complet, autonome, embarqué sur appareil mobile Android. Il offre une traçabilité totale des flux physiques (stocks), financiers (achats, ventes, trésorerie, comptabilité) et organisationnels (RH, CRM, qualité, projets, logistique), **sans nécessiter aucune connexion internet pour son fonctionnement quotidien**.

### 1.3 Objectifs Clés
1. **Zéro dépendance réseau** : L'intégralité des fonctionnalités (ventes, achats, inventaires, rapports, bilans) s'exécute en local sur base de données SQLite/Room embarquée.
2. **Conformité multi-zones & OHADA** : Support natif du plan comptable SYSCOHADA, de la zone CEMAC/UEMOA, des taxes locales (TVA, Centimes additionnels, Acomptes) et adaptation dynamique des identifiants fiscaux (NIU, RCCM, NIF, SIRET, ICE, etc.).
3. **Ergonomie industrielle & réactivité** : Interface Jetpack Compose responsive, zones de clic conformes à l'accessibilité (≥ 48dp), fluidité 60fps, couleurs sémantiques par module et icônes unifiées.
4. **Intégrité comptable stricte** : Aucune suppression physique d'écriture ou de document validé ; traçabilité absolue par contre-passation et journal d'audit scellé.

---

## 2. ARCHITECTURE TECHNIQUE & PRINCIPES FONDAMENTAUX

### 2.1 Stack Technologique
* **Langage :** Kotlin 100% (Coroutines, StateFlow, Flow réactifs).
* **UI / Rendu :** Jetpack Compose, Material 3 adapté, Charte propriétaire MISSA UI (Flat monochrome + contrastes vifs).
* **Persistance locale :** Room Database (SQLite), version courante : 17.
* **Injection de dépendances :** Dagger Hilt.
* **Architecture applicative :** Clean Architecture (Data → Domain / Use Cases → Presentation / ViewModel / Compose).
* **Internationalisation :** 5 locales complètes (`values`, `values-en`, `values-es`, `values-zh`, `values-ar`).

### 2.2 Principes de Conception Non Négociables
1. **Couleurs Sémantiques Fixes :**
   * Vente : Bleu Royal (`0xFF2563EB`)
   * Stock : Gris Neutre (`0xFF6B7280`)
   * Achats : Jaune Ambré (`0xFFFACC15`)
   * Fournisseurs : Marron Terre / Cuir (`0xFF92400E`)
   * Finances / Trésorerie / Comptabilité : Vert Monétaire (`0xFF16A34A`)
   * Production : Orange Industriel (`0xFFF97316`)
   * Clients : Violet Doux (`0xFF8B5CF6`)
   * RH : Rouge Rubis (`0xFFE11D48`)
   * Livraison : Bleu Ciel (`0xFF38BDF8`)
   * Maintenance : Rouge Brique (`0xFFB91C1C`)
   * Qualité : Pourpre (`0xFF7C3AED`)
   * Logistique : Vert Olive (`0xFF65A30D`)
2. **Iconographie stricte :** Utilisation exclusive des sets de vecteurs internes `Iv.*` et `StockIv.*`. **Toutes les icônes sont rendues en noir (`Color.Black` ou `MissaInk`)**. Interdiction totale des icônes génériques Android Material Icons.
3. **Immutabilité des pièces d'opération :** Une pièce validée (Bon de commande, Bon de réception, Facture de vente, Écriture comptable) ne peut plus être modifiée ni supprimée. Seule une pièce d'annulation ou un avoir comptable contre-passe le flux.

---

## 3. SPÉCIFICATIONS FONCTIONNELLES PAR MODULE

---

### MODULE 1 : GESTION DES STOCKS & ARTICLES (STK)
Le cœur logistique et d'évaluation patrimoniale de l'entreprise.

#### 1. Typologie des Produits & Articles
Le système classe chaque référence selon 5 catégories fondamentales :
* **Marchandise (Vente directe)** : Achetée pour être revendue en l'état. Possède un prix d'achat HT et un prix de vente HT.
* **Matière première / Ingrédient** : Entrée pour consommation en production. Pas de prix de vente par défaut (sauf si explicitement activé en vente d'appoint).
* **Produit fini / Semi-fini** : Issu des ordres de fabrication (Production). Possède un coût de revient calculé et un prix de vente.
* **Immobilisation** : Bien d'équipement de l'entreprise (Matériel, Machine, Véhicule). Suivi de valeur amortissable, possibilité de cession/vente exceptionnelle.
* **Prestation de service / Consommable** : Non stockable physiquement, pas de gestion de quantité minimale/maximale.

#### 2. Matrice d'accueil & Navigation
* **Hero Dashboard** : Affiche la **Valeur Globale du Stock** valorisée au CMUP (Coût Moyen Unitaire Pondéré).
* Indicateurs synthétiques : Nombre d'entrées/sorties du jour, références sous seuil critique, ruptures de stock avérées.
* **Matrice dynamique des catégories** : Tuiles carrées représentant chaque famille d'article. Clic sur une tuile → filtrage instantané de la liste.
* Bouton d'ajout contextuel unique : Type et catégorie verrouillés par défaut selon la tuile cliquée.

#### 3. Formulaires & Fiches Articles
* **Champs obligatoires** : Code SKU (génération auto ou manuel), Désignation, Famille, Unité de mesure (Kg, Litre, Pièce, Carton, Mètre, Heure).
* **Champs conditionnels** :
  * Si type Marchandise / Produit fini : Prix d'achat, Prix de vente, Taux de marge/markup auto-calculé.
  * Si traçabilité activée : Gestion obligatoire des numéros de Lots et dates de péremption.
  * Machine de production : Pas de prix de vente proposé par défaut.
* **Seuils d'alerte** : Stock d'alerte (seuil de réapprovisionnement), Stock minimum, Stock maximum de sécurité.
* **Compression d'images** : Photos produits automatiquement redimensionnées et compressées en WebP/JPEG local pour préserver le stockage de l'appareil.

#### 4. Mouvements & Opérations de Stock
* **Entrée de stock** : Réception fournisseur, production interne, régularisation positive.
* **Sortie de stock** : Vente client, consommation en fabrication, mise au rebut (déchet/perte).
* **Transfert d'entrepôt** : Déplacement d'un dépôt source vers un dépôt cible.
* **Inventaire tournant & de clôture** : Écran de pointage par scan ou saisie manuelle. Calcul immédiat des écarts d'inventaire et génération d'écritures de régularisation.

---

### MODULE 2 : RÉFÉRENTIEL FOURNISSEURS (FRN)
Gestion exhaustive des tiers amont, agréments et catalogue d'achats.

#### 1. Cycle de Vie à 6 Statuts
Un fournisseur évolue obligatoirement à travers les états suivants :
1. `BROUILLON` : Fiche en cours de saisie, aucune transaction d'achat autorisée.
2. `A_VALIDER` : Dossier complet soumis au responsable des achats pour contrôle.
3. `ACTIF` : Fournisseur agréé. Éligible pour l'émission de bons de commande, réceptions et factures.
4. `SUSPENDU` : Bloqué temporairement pour litige ou mise à jour documentaire. Commandes interdites, mais règlements de factures existantes autorisés.
5. `BLOQUE` : Exclusion formelle avec motif obligatoire consigné au registre (ex: fraude, litige grave). Toute commande interdite.
6. `ARCHIVE` : Fournisseur historique inactif, conservé pour intégrité fiscale et d'audit.

#### 2. Formulaire de Création Contextuel en 7 Étapes
1. **Identité** : Raison sociale, Sigle commercial, Pays d'origine (catalogue ISO), Type juridique (Entreprise, Particulier, Étranger).
2. **Contacts** : Enregistrement d'au moins un contact principal (Nom, Prénom, Poste, Téléphone direct, Email).
3. **Fiscalité** : Identifiant fiscal dynamique selon le pays (ex: NIU/RCCM au Cameroun, SIRET en France, ICE au Maroc).
4. **Périmètre Achats** : Catégories d'articles approvisionnés, devises acceptées.
5. **Modalités Financières** : Conditions de règlement (Comptant, 30j, 60j fin de mois), mode de paiement privilégié, RIB/IBAN bancaire ou compte Mobile Money.
6. **Pièces Justificatives (Documents)** : Téléversement de l'attestation fiscale, registre du commerce, grille tarifaire avec dates de validité.
7. **Revue & Validation** : Récapitulatif global avec vérification des 7 critères bloquants avant autorisation de soumission.

#### 3. Détection des Doublons & Réapprobation Automatique
* **Anti-doublon multicritère** : Contrôle en temps réel sur le numéro fiscal, le numéro de téléphone, l'email ou la similarité phonétique du nom commercial.
* **Règle de réapprobation automatique** : Si un fournisseur déjà `ACTIF` subit une modification sur ses coordonnées bancaires, sa raison sociale ou son identifiant fiscal, il repasse automatiquement en statut `A_VALIDER`.

---

### MODULE 3 : GESTION DES ACHATS (ACH)
La chaîne complète d'approvisionnement : De l'intention à la liquidation.

#### 1. Chaîne Opérationnelle
```
COMMANDE FOURNISSEUR (Engagement prévisionnel, aucun impact stock/trésorerie)
       ↓
RÉCEPTION EN STOCK (Entrée physique en stock au CMUP, émission du Bon de Réception)
       ↓
FACTURE FOURNISSEUR (Enregistrement de la dette, TVA déductible, liaison BL)
       ↓
RÈGLEMENT FINANCIER (Décaissement Trésorerie, solde de la facture, écritures comptables)
```

#### 2. Écrans & Fonctionnalités
* **Hub Achats** :
  * KPI en temps réel : Dépenses validées cumulées, Passif restant dû (dette fournisseurs), Nombre de brouillons.
  * Actions : Bouton [Nouvelle commande] et Bouton [Nouvelle facture directe].
  * Registre des pièces avec filtrage par statut et type.
* **Bon de Commande** :
  * Sélection du fournisseur agréé (statut `ACTIF`).
  * Panier d'achat avec articles du stock. Préremplissage du prix négocié issu de la fiche article-fournisseur.
  * Saisie de la date d'échéance de livraison promise.
* **Bon de Réception** :
  * Pointage des quantités reçues par rapport aux quantités commandées.
  * Prise en compte des réceptions partielles (génération de reliquat automatique).
  * Traçabilité : Saisie des numéros de lot, numéros de série et dates d'expiration des lots entrants.
* **Facturation & Règlements** :
  * Conversion en un clic d'un bon de réception en facture fournisseur (sans double saisie).
  * Enregistrement des règlements totaux ou fractionnés (avances, acomptes).
  * Pièce jointe : Numérisation/capture photo de la facture papier du fournisseur.

---

### MODULE 4 : GESTION DES CLIENTS & CRM (VEN / CRM)
Gestion du portefeuille client, encours financier et suivi relationnel.

#### 1. Fiche Client & Typologie
* **Catégorisation** : Particulier, Entreprise (B2B), Administration, Revendeur/Grossiste.
* **Paramètres commerciaux** : Catégorie tarifaire attribuée, remise contractuelle par défaut, plafond de crédit accordé (limite d'encours autorisée).
* **Historique 360°** : Vue consolidée regroupant devis émis, commandes en cours, factures impayées, livraisons en souffrance et journal des contacts.

#### 2. Pipeline Commercial (CRM)
* Gestion des leads et opportunités d'affaires.
* Étapes du pipeline : Prospect identifié → Contact établi → Proposition/Devis émis → Négociation → Gagné / Perdu.
* Rappels de relance et calendrier des rendez-vous.

---

### MODULE 5 : VENTES & FACTURATION (VEN)
Cycle de concrétisation du chiffre d'affaires et encaissement.

#### 1. Parcours de Vente
* **Devis / Proforma** : Émission d'une proposition chiffrée avec délai de validité. Impression ou partage direct en PDF.
* **Bon de Commande Client** : Réservation optionnelle des quantités en stock.
* **Facturation directe (POS / Facture Comptant)** : Déstockage immédiat, calcul de la TVA et taxes applicables, saisie de l'encaissement (Espèces, Chèque, Mobile Money, Virement), émission de facturette A5 ou ticket 80mm.
* **Avoirs & Retours Clients** : Traitement des retours marchandises avec réintégration en stock et émission d'une note de crédit/avoir.

---

### MODULE 6 : TRÉSORERIE & PAIEMENTS (TRE)
Pilotage des liquidités et des caisses physiques.

#### 1. Gestion Multi-Caisses & Banques
* Comptes de trésorerie paramétrables : Caisse principale, Caisse secondaire, Comptes bancaires, Comptes Mobile Money (Orange Money, MTN MoMo, Wave, etc.).
* Enregistrement des encaissements (ventes, apports) et décaissements (achats, salaires, charges d'exploitation).

#### 2. Opérations de Caisse & Rapprochement
* Transferts internes de compte à compte (ex: Remise d'espèces de la caisse vers la banque).
* Clôture de caisse quotidienne (X de caisse, Z de caisse) avec comptage physique des espèces et justification des écarts.
* Rapprochement bancaire manuel pour validation des relevés.

---

### MODULE 7 : COMPTABILITÉ & FINANCES (CPT)
Moteur comptable en partie double conforme SYSCOHADA et normes universelles.

#### 1. Principes Comptables Intégrés
* **Grand Livre & Balance Générale** : Tout flux d'achat, de vente, de paiement et de variation de stock génère automatiquement les écritures au journal adéquat (Achats, Ventes, Caisse, Banque, Opérations Diverses).
* **Partie double stricte** : Équilibre systématique Débit = Crédit sur chaque pièce.
* **États financiers automatisés** :
  * Compte de Résultat (Produits vs Charges).
  * Bilan simplifié (Actif immobilisé, Stocks, Créances, Trésorerie vs Capitaux, Dettes fournisseurs, Dettes fiscales).
  * Tableau des flux de trésorerie.

---

### MODULE 8 : RESSOURCES HUMAINES & PAIE (RH)
Administration du personnel et calcul des rémunérations.

#### 1. Fichier du Personnel
* Dossier employé : État civil, contrat de travail (CDI, CDD, Journalier, Stagiaire), poste occupé, date d'embauche, pièces d'identité numérisées.
* Gestion des congés et absences (justifiées, maladie, sans solde).

#### 2. Préparation & Calcul de la Paie
* Définition de la structure de rémunération : Salaire de base, primes de rendement, indemnités de transport, heures supplémentaires.
* Retenues salariales : Cotisations sociales obligatoires (CNPS/Caisse de retraite), retenues fiscales sur salaires (IRPP).
* Génération des bulletins de paie et liaison avec les décaissements de trésorerie.

---

### MODULE 9 : GESTION DE PRODUCTION (PRO)
Fabrication, transformation et suivi des coûts de revient.

#### 1. Nomenclatures de Fabrication (BOM - Bill of Materials)
* Recette d'assemblage : Définition des quantités de matières premières et composants requis pour produire 1 unité de produit fini.
* Intégration des coûts annexes : Main d'œuvre directe horaire, quote-part machine.

#### 2. Ordres de Fabrication (OF)
* Lancement d'un OF : Sortie automatique des matières premières du stock.
* Suivi des rebuts et déchets de fabrication.
* Clôture d'OF : Entrée du produit fini au stock valorisé au Coût Réel de Fabrication.

---

### MODULE 10 : LOGISTIQUE, LIVRAISONS & MAINTENANCE (LOG / MAI)
Exécution physique des opérations et maintenance du matériel.

#### 1. Expéditions & Livraisons
* Bons de livraison (BL) rattachés aux factures ou commandes de vente.
* Suivi des tournées, assignation des chauffeurs/véhicules, signature numérique du destinataire.

#### 2. Maintenance des Équipements
* Fiche de maintenance préventive et corrective pour le parc de machines et véhicules.
* Consignation des pannes, pièces d'usure remplacées (liées au stock de consommables) et coût d'intervention.

---

### MODULE 11 : QUALITÉ & SÉCURITÉ (QUA)
Conformité des approvisionnements et processus internes.

* Déclaration de non-conformité à réception fournisseur ou en cours de fabrication.
* Gestion du statut de stock « Bloqué Qualité » (articles physiquement isolés ne pouvant être vendus ni consommés).
* Plans d'actions correctives et préventives (CAPA).

---

### MODULE 12 : ADMINISTRATION, SÉCURITÉ & AUDIT
Contrôle d'accès et intégrité de la solution.

* **Gestion des Profils & Licences** : Définition des droits d'accès par utilisateur (Vendeur, Magasinier, Comptable, Manager, Administrateur). Blocage en mode lecture seule en cas d'expiration de licence.
* **Packs Métiers Activables** : L'administrateur peut activer ou masquer dynamiquement les modules selon l'activité de l'entreprise (Commerce de détail, Industrie, Prestation de services).
* **Sauvegarde & Restauration Locale** : Export complet de la base SQLite chiffrée sur carte SD / mémoire interne ou partage direct (WhatsApp/Email/Drive).
* **Journal d'Audit Immuable** : Horodatage précis de toute opération sensible (création d'utilisateur, changement de statut fournisseur, annulation de facture).

---

## 4. EXIGENCES NON-FONCTIONNELLES & DESIGN SYSTEM

### 4.1 Interface Graphique (Design System MISSA 360)
1. **Structure d'écran standardisée :**
   * **En-tête (MissaAppHeader / MissaTopAppBar)** : Hauteur 64dp, logo entreprise cadré (Crop alpha 0.09 en arrière-plan), icônes d'action 24dp sur zones tactiles 48dp minimum.
   * **Corps d'écran scrollable** : Marges horizontales 16dp, espacement vertical régulier (grille 8/12/16/24dp).
   * **Barre de navigation inférieure (MissaBarreModules)** : 5 onglets maximum avec effet de relief (shadow 12dp, coins arrondis supérieurs 18dp, dégradé contrasté).
2. **Typographie & Lisibilité :**
   * Tailles de police adaptées aux conditions de luminosité extérieures (Plein soleil) : minimum 11sp pour les libellés secondaires, 13-15sp pour les corps de texte, 18-24sp pour les montants financiers.
   * Masquage intelligent des identifiants techniques complexes au profit de libellés clairs.

### 4.2 Performance & Robustesse
* **Temps de réponse** : Affichage d'un écran ou d'une liste en moins de 100ms.
* **Empreinte mémoire** : Utilisation RAM inférieure à 120 Mo en fonctionnement nominal.
* **Taille de l'application (APK)** : Inférieure à 35 Mo tout compris (assets, polices et vecteurs).
* **Résistance aux pannes** : Transactions SQL atomiques `@Transaction` ; aucune écriture partielle possible en cas d'arrêt brutal de la batterie du smartphone.

---

## 5. TRAJECTOIRE D'IMPLÉMENTATION & FEUILLE DE ROUTE

| Phase | Modules / Périmètre | État actuel |
|---|---|---|
| **Socle V1** | Architecture Clean, Room v17, CI/CD GitHub Actions, Design System, Onboarding | **Livré (Vert)** |
| **Phase 1** | Référentiel Stock (STK) complet, Matrice des catégories, Valorisation CMUP | **Livré (Vert)** |
| **Phase 2** | Référentiel Fournisseurs (FRN) complet, Cycle 6 statuts, Anti-doublons, Formulaire 7 étapes | **Livré (Vert)** |
| **Phase 3** | Achats (ACH) : Bons de commande, Bons de réception, Facturation, Déductions TVA | **En cours d'ajustement UI** |
| **Phase 4** | Clients (CLI) & Ventes directes / Facturation (VEN) | **Prochaine étape** |
| **Phase 5** | Trésorerie multi-caisses (TRE) & Règlements clients/fournisseurs | Planifié |
| **Phase 6** | Comptabilité automatique SYSCOHADA (CPT) & États financiers | Planifié |
| **Phase 7** | Production (PRO), RH & Paie, Logistique, Qualité, Maintenance | Planifié |
