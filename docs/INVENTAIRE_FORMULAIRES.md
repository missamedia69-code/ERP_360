# Inventaire des formulaires — Missa Business 360

*Relevé du 29/09/2026 sur `arena/01a0ee6d-erp-360`. Lecture du code source (`app/src/main/java/com/missa/b360/ui/`), sans exécution sur appareil.*

## Méthode et grille de contrôle

J'ai relevé toutes les fonctions `@Composable` qui contiennent au moins un champ de saisie (`OutlinedTextField`, `TextField` ou les petits composants maison `Champ`, `TreChamps`, `OnbChampTexte`…). Chaque formulaire est vérifié par rapport à la spécification des formulaires validée dans `CLAUDE.md` :

| Code | Règle (`CLAUDE.md` › Formulaires) |
|---|---|
| **R1** | Bouton Enregistrer **désactivé** tant que les champs requis sont vides |
| **R2** | Dates saisies par un **calendrier** (`DatePickerDialog`), jamais tapées au clavier |
| **R3** | Listes de choix = **vrai menu déroulant** (`DropdownMenu`) |
| **R4** | Texte visible passé par `R.string` (5 langues), **aucun texte écrit en dur** |
| **R5** | Clavier adapté : numérique pour les montants et quantités, téléphone, e-mail |

Légende : ✅ conforme · ⚠️ écart mineur · ❌ écart bloquant par rapport à la règle.

**Chiffres :** **60 formulaires** répartis sur 14 zones de l'application : 13 écrans (dont 3 à étapes : article, fiche client, fiche fournisseur) et 47 dialogues. Il faut y ajouter 6 petits composants de champ partagés et 6 champs de recherche seuls. Les dialogues de simple choix (profil, personnalisation de l'accueil, fiche entreprise) ne sont pas comptés.

**Résultat global :** 27 formulaires conformes sur les 5 règles, 14 avec seulement des écarts mineurs (⚠️), **19 avec au moins un ❌**. Parmi ces 19, 12 enfreignent une règle R1 à R4 (traduction, calendrier, bouton, liste) et 7 seulement la règle du clavier R5 (#17, #23, #42, #43, #57–#59).


## Mise à jour — nouveau design des formulaires (phase 4)

Les 60 formulaires utilisent maintenant le kit commun `ui/components/MissaFormulaire.kt`, inspiré de la maquette « GOOD UI/UX » :
- en-tête (titre, sous-titre, icône) ;
- sections numérotées ① ② ③ ;
- champs pleine largeur avec icône noire en tête ;
- champs courts deux par deux (`MissaRangee`) ;
- choix exclusifs en tuiles (`MissaChoixTuiles`, `MissaChoixPaiement`) ;
- cases à cocher claires ;
- gros bouton principal à la couleur du module ;
- mention « données enregistrées uniquement sur cet appareil ».

| Lot | Formulaires | Commit |
|---|---|---|
| 1 | #2, #51–#60 (sites, projets, tâches, RH, qualité, maintenance, livraison) | `9b2311e` |
| 2 | #37–#50 (trésorerie, comptabilité, production, services) | `c23af00` |
| 3 | #3–#16, #18–#23 (clients, vente, devis et commandes, stock) | `4cdf5ff` |
| 4 | #1, #17, #24–#36 (onboarding entreprise, fiche article, achats, fournisseurs) | `54c1267`, `a7a5f58` |

Effets sur la grille de contrôle :
- **R1** : les formulaires qui contrôlaient les champs seulement au clic (#15, #21, #22, #41, #46–#49…) ont maintenant un bouton grisé tant que les champs requis sont vides.
- **R2** : toutes les dates passent par le calendrier du kit (`MissaChampDate`, `MissaChampDateTexte`, `MissaChampDateHeure`).
- **R3** : toutes les listes passent par `MissaChampListe`.
- **R5** : le clavier est choisi par `MissaClavier` (ENTIER, DECIMAL, TELEPHONE, EMAIL).
- **Doublons supprimés** :
  - les trois dialogues « Nouvelle catégorie » (#18–#20) sont remplacés par `DialogueNouvelleCategorieStock` ;
  - `ChampDate`, `ChampDateAchat`, `ChampDateFour`, `DropdownChamp` et `Selecteur` sont redirigés vers le kit ;
  - `ChampsCategorie` et `ChampsBadge`, qui n'étaient plus appelés, sont supprimés.
- **Gardés volontairement hors du kit** :
  - les champs de recherche seuls et les filtres de liste ;
  - les dialogues de confirmation d'annulation (Achats, Projets, Livraison) ;
  - le champ téléphone avec indicatif pays de la fiche client ;
  - la recherche intégrée à `MissaSelecteur`.

Les tableaux ci-dessous restent le relevé **avant** la phase 4.

---

## 1. Onboarding

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 1 | Entreprise (étape d'onboarding) | `onboarding/OnbEntreprise.kt:117` | votre nom *(profil personnel)*, nom de l'entreprise, secteur, site principal, pays, devise, taux de taxe, téléphone, e-mail, adresse, e-mail de récupération, identifiants fiscaux par pays, logo | ⚠️ | — | ✅ | ✅ | ✅ | Le bouton n'est bloqué que pendant l'enregistrement (`!enregistrementEnCours`). Les champs requis sont contrôlés **au clic** (`OnboardingViewModel.enregistrerEntreprise`, message d'erreur). |
| — | PIN / verrou | `onboarding/OnbPin.kt`, `PinLockScreen.kt` | pavé numérique maison | ✅ | — | — | ✅ | ✅ | Pas un champ texte : saisie guidée de 4 à 6 chiffres. |
| — | Configuration | `onboarding/OnbConfiguration.kt` | formats, rétention, sauvegardes, fuseau | — | — | ✅ | ✅ | — | Uniquement des sélecteurs, aucun champ libre. |

## 2. Administration

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 2 | Nouveau site | `admin/AdminSitesScreen.kt:320` | nom\*, type, adresse | ✅ | — | ⚠️ | ✅ | — | Le type de site est choisi par boutons et non par un menu déroulant (acceptable). |
| — | Référentiels, Réglages | `admin/ReferentielsScreen.kt`, `AdminReglagesScreen.kt` | — | — | — | ✅ | ✅ | — | Uniquement des interrupteurs et des dialogues de choix. **La saisie d'une taxe, d'un moyen de paiement ou d'une unité n'existe pas encore sous forme de formulaire.** |
| — | Licence, Sauvegarde, Journal, Utilisateurs, À propos | `admin/*Screen.kt` | — | | | | | | **Écrans vides (placeholders)** : aucun formulaire. |

## 3. Clients

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 3 | Création rapide d'un client | `clients/ClientFormDialog.kt:72` | type, nom\*, indicatif pays + téléphone\*, e-mail, catégorie, site | ✅ | — | ✅ | ✅ | ✅ | Validation en direct (`isError`). |
| 4 | Fiche client › Identité et fiscalité | `clients/ClientFlowScreen.kt:977` | raison sociale\*, NIF, n° de TVA, assujetti/exonéré, motif d'exonération, taux spécifique, e-mail | ✅ | — | — | ✅ | ✅ | |
| 5 | Fiche client › Contacts (liste) | `clients/ClientFlowScreen.kt:1095` | liste + ajout | ✅ | — | — | ✅ | — | Le bouton est toujours actif (`enabled = true`), ce qui est logique : un contact est facultatif. |
| 6 | Contact client | `clients/ClientFlowScreen.kt:1145` | nom\*, rôle, téléphone, e-mail, principal | ✅ | — | — | ✅ | ✅ | |
| 7 | Fiche client › Commercial et adresses | `clients/ClientFlowScreen.kt:1169` | commercial, conditions de paiement, grille tarifaire, remise, remise max, plafond de crédit, segment, canal, territoire, compte, notes + adresses | ✅ | — | ⚠️ | ✅ | ✅ | Segment, canal, territoire et conditions sont saisis **en texte libre**. La spec demande des valeurs codées affichées de façon lisible. |
| 8 | Adresse client | `clients/ClientFlowScreen.kt:1251` | libellé, adresse\*, ville, principale | ✅ | — | — | ✅ | — | |
| 9 | Catégories de clients | `clients/ClientDialogs.kt:23` | nom\* | ✅ | — | — | ✅ | — | |
| 10 | Badges de fidélité | `clients/ClientDialogs.kt:108` | nom\*, remise %\* | ✅ | — | — | ✅ | ✅ | |
| 11 | Prix négocié | `clients/ClientPricesSection.kt:94` | article\*, prix\* | ✅ | — | ✅ | ✅ | ✅ | |
| 12 | Recherche avancée de clients | `clients/ClientFlowScreen.kt:1403` | texte, statut, catégorie, commercial, ville, **du / au** | — | ❌ | ✅ | ⚠️ | — | **Dates tapées au clavier** avec le texte indicatif « JJ/MM/AAAA », écrit en dur. |

## 4. Vente

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 13 | Vente directe (panier) | `sales/SalesScreen.kt:334` | client, catalogue et quantités, remise, mode de paiement | ✅ | — | ✅ | ✅ | ✅ | |
| 14 | Création rapide d'un client (vente) | `sales/SalesScreen.kt:576` | nom\*, téléphone\*, e-mail, adresse | ✅ | — | — | ✅ | ✅ | Fait doublon avec #3 (deux formulaires différents pour la même action). |
| 15 | Nouveau devis | `sales/DevisCommandeScreen.kt:395` | client\*, désignation\*, montant\* | ❌ | — | ✅ | ✅ | ✅ | **Le bouton Enregistrer est toujours actif**, même sans client, sans désignation ou sans montant. |
| 16 | Facturer une commande | `sales/DevisCommandeScreen.kt:463` | mode de paiement, montant payé | ⚠️ | — | ✅ | ✅ | ✅ | Actif dès qu'un moyen de paiement existe. Le montant saisi n'est pas contrôlé. |
| — | Retour / avoir, `SalesFlowScreen` | `sales/ReturnSaleScreen.kt`, `SalesFlowScreen.kt` | — | | | | | | **Écrans vides (placeholders)**. |

## 5. Stock

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 17 | **Article (2 étapes)** | `stock/ProductFormScreen.kt:76` | type, nom\*, unité\*, catégorie, référence, code-barres, marque, modèle, n° de série, image ; prix d'achat et de vente, remise max ; seuils mini/maxi/sécurité ; stock initial, dépôt ; blocs conditionnels équipement, déchet, emballage, consigne, kit ; 6 dates (acquisition, garantie, début/fin…) | ✅ | ✅ | ✅ | ⚠️ | ❌ | Dates au calendrier ✅, sections dynamiques ✅. **Aucun clavier numérique** : prix, seuils, poids, capacité, stock initial passent tous par le composant `Champ()`, qui ouvre un clavier texte. Flèches « → / ← » collées au texte en dur. |
| 18 | Nouvelle catégorie (depuis le formulaire article) | `stock/ProductFormScreen.kt:708` | nom\* | ✅ | — | — | ✅ | — | |
| 19 | Nouvelle catégorie (hub Stock) | `stock/StockAccueilScreen.kt:80` | nom | ⚠️ | — | — | ✅ | — | Bouton non désactivé ; le nom est contrôlé au clic. **Code copié à l'identique** dans #20. |
| 20 | Nouvelle catégorie (écran Catégories) | `stock/StockCategoriesScreen.kt:76` | nom | ⚠️ | — | — | ✅ | — | Copie de #19. |
| 21 | Mouvement de stock (entrée/sortie) | `stock/StockMovementFormScreen.kt:190` | article\*, quantité\*, motif | ❌ | — | ✅ | ⚠️ | ❌ | Bouton actif en permanence (`!busy`), message « champs requis » après coup. Quantité au clavier texte. « \* » ajouté à la main au libellé. |
| 22 | Transfert entre dépôts | `stock/StockMovementFormScreen.kt:44` | article\*, quantité\*, dépôt source\*, dépôt destination\*, observation | ❌ | — | ✅ | ⚠️ | ❌ | Mêmes écarts que #21. |
| 23 | Inventaire (comptage) | `stock/InventoryScreen.kt:59` | recherche, quantité comptée par ligne | — | — | — | ✅ | ❌ | Comptage au clavier texte. |

## 6. Achats

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 24 | Facture fournisseur (achat) | `purchases/PurchasesScreen.kt:925` | fournisseur\*, lignes (quantité, prix), TVA, mode de paiement, montant réglé, note, date | ✅ | ✅ | ✅ | ✅ | ✅ | Barre d'actions : brouillon et valider désactivés sans fournisseur ni ligne. |
| 25 | Traçabilité d'une ligne | `purchases/PurchasesScreen.kt:1326` | lot, n° de série, péremption | — | ✅ | — | ✅ | — | Péremption au calendrier (`ChampDateAchat`). |
| 26 | Bon de commande | `purchases/PurchasesScreen.kt:1376` | fournisseur\*, lignes\*, note, dates | ✅ | ✅ | ✅ | ✅ | ✅ | |
| 27 | Réception | `purchases/PurchasesScreen.kt:1582` | quantités reçues par ligne\*, note | ✅ | — | — | ✅ | ✅ | Validation seulement si au moins une quantité est supérieure à 0. |
| 28 | Règlement fournisseur | `purchases/PurchasesScreen.kt:1735` | montant\* (≤ reste dû), mode\* | ✅ | — | ✅ | ✅ | ✅ | Bon exemple : montant borné par le reste dû. |
| 29 | Création rapide d'un fournisseur | `purchases/PurchasesScreen.kt:790` | nom\*, téléphone\*, e-mail, adresse | ✅ | — | — | ✅ | ✅ | |

## 7. Fournisseurs

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 30 | **Fiche fournisseur (7 étapes)** | `fournisseurs/FournisseursScreen.kt:1804` | ① identité : raison sociale, nom commercial, type, pays, devise, adresse, téléphone, e-mail, site web · ② contacts · ③ fiscalité : identifiant, RCCM, n° de TVA, assujetti/exonéré, taux de retenue · ④ achats : catégories, délai, incoterm, montant et quantité minimum, types de déchets · ⑤ paiement : conditions, échéance, mode préféré · ⑥ documents · ⑦ validation | ✅ | ✅ | ✅ | ✅ | ⚠️ | Chaque étape est contrôlée par le ViewModel (`etapeSuivante`, 7 critères bloquants, détection des doublons). Le bouton n'est grisé que pendant l'enregistrement, mais une étape incomplète est bloquée : conforme à l'esprit de la règle. |
| 31 | Motif de blocage | `:1388` | motif\* | ✅ | — | — | ✅ | — | |
| 32 | Contact fournisseur | `:1415` | nom\*, prénom, fonction, téléphone, e-mail | ✅ | — | — | ✅ | ⚠️ | Pas de clavier téléphone ni e-mail. |
| 33 | Compte bancaire / Mobile Money | `:1453` | titulaire\*, banque, n° de compte, IBAN, BIC, opérateur, n° mobile (un des moyens\*) | ✅ | — | ✅ | ✅ | ⚠️ | Numéros au clavier texte. |
| 34 | Document justificatif | `:1523` | type, référence, date d'émission, date d'expiration, PDF | ❌ | ✅ | ✅ | ✅ | — | **Bouton Ajouter toujours actif**, même sans fichier ni référence. |
| 35 | Article du catalogue fournisseur | `:1661` | article\*, référence, prix, délai, quantité minimum | ✅ | — | ✅ | ✅ | ✅ | |
| 36 | Évaluation | `:1757` | note 0 à 5, commentaire | ✅ | — | — | ✅ | — | Note par défaut, donc toujours valide. |

## 8. Trésorerie

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 37 | Compte de trésorerie | `tresorerie/TresorerieDialogues.kt:56` | nom\*, établissement, numéro, solde initial | ✅ | — | ✅ | ✅ | ✅ | `TreChamps` filtre la frappe (chiffres uniquement) et ouvre un clavier décimal : **c'est le modèle à reprendre ailleurs**. |
| 38 | Mouvement (encaissement/dépense) | `:143` | sens, compte\*, montant\*, catégorie, libellé, tiers, référence | ✅ | — | ✅ | ✅ | ✅ | |
| 39 | Virement interne | `:320` | compte source\*, compte destination\*, montant\* | ✅ | — | ✅ | ✅ | ✅ | |

## 9. Comptabilité

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 40 | Profil comptable | `comptabilite/ComptabiliteScreen.kt:345` | code pays ISO, régime fiscal, mois de début d'exercice | ❌ | — | ❌ | ❌ | ❌ | **Tout est écrit en dur en français** (9 textes). Pays saisi au clavier au lieu du catalogue ISO existant (`Iso4217.paysDisponibles()`). Mois tapé au clavier au lieu d'une liste. Bouton toujours actif. |
| 41 | Nouvelle pièce OD | `:379` | libellé, montant, compte au débit, compte au crédit | ❌ | — | ✅ | ❌ | ❌ | Textes en dur (7), montant au clavier texte, bouton toujours actif. |

## 10. Production

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 42 | Ordre de fabrication | `production/ProductionScreen.kt:279` | produit fini\*, quantité à fabriquer\*, composants\* | ⚠️ | — | ✅ | ✅ | ❌ | Bouton bloqué sans produit ni composant, mais **la quantité n'est pas vérifiée** et se saisit au clavier texte. |
| 43 | Ajout d'un composant | `:462` | matière\*, quantité\* | ✅ | — | ✅ | ✅ | ❌ | Quantité au clavier texte. |

## 11. Services

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 44 | Nouvelle prestation | `services/ServicesScreen.kt:581` | client, intitulé\*, tarif\*, heures prévues | ✅ | — | ✅ | ✅ | ✅ | |
| 45 | Ajuster les heures | `:677` | heures passées | ✅ | — | — | ✅ | ✅ | |
| 46 | Demande de service | `services/ServiceFieldDialogs.kt:43` | client, type, priorité, motif, contact, téléphone | ❌ | — | ⚠️ | ❌ | ✅ | **Textes en dur (français)**, bouton toujours actif. Type et priorité affichés par du code en dur (`ServiceFieldComponents.kt:200-204`). |
| 47 | Ordre d'intervention | `:73` | client, intervention, priorité | ❌ | — | ✅ | ❌ | — | Textes en dur, bouton toujours actif. |
| 48 | Planifier et affecter | `:97` | technicien, **début et fin (« jj/MM/aaaa HH:mm »)** | ❌ | ❌ | ✅ | ❌ | — | **Date et heure tapées au clavier**, textes en dur. |
| 49 | Rapport d'intervention | `:130` | diagnostic, travaux, résolu, représentant du client, signature, photo | ❌ | — | — | ❌ | — | Textes en dur (10), bouton toujours actif. |
| 50 | Temps de travail | `:178` | minutes | ⚠️ | — | — | ❌ | ✅ | Textes en dur. |

## 12. Projets · Tâches

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 51 | Nouveau projet | `projets/ProjetsScreen.kt:461` | client, nom\*, responsable, budget | ✅ | ⚠️ | ✅ | ✅ | ✅ | **Aucune date** de début ni d'échéance (le module affiche pourtant « Planning projet »). |
| 52 | Actualiser un projet | `:541` | avancement %, consommé réel | ⚠️ | — | — | ✅ | ✅ | Bouton toujours actif ; la limite 0–100 % n'est pas appliquée dans l'interface. |
| 53 | Nouvelle tâche | `tasks/TasksScreen.kt:319` | titre\*, notes | ✅ | ⚠️ | — | ✅ | — | Pas d'échéance ni d'assignation. |

## 13. RH

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 54 | Nouvel employé | `rh/RhScreen.kt:329` | nom\*, téléphone\*, poste, salaire | ✅ | — | — | ✅ | ✅ | |
| 55 | Absence | `:397` | durée en jours\*, motif | ✅ | ⚠️ | — | ✅ | ✅ | **Pas de date de début ni de fin** : impossible de dater l'absence. Le module Services s'en sert pourtant pour refuser une affectation. |
| 56 | Avance sur salaire | `:453` | montant\*, motif | ✅ | — | — | ✅ | ✅ | |

## 14. Qualité · Maintenance · Livraison

| # | Formulaire | Emplacement | Champs | R1 | R2 | R3 | R4 | R5 | Remarques |
|---|---|---|---|:-:|:-:|:-:|:-:|:-:|---|
| 57 | Non-conformité | `qualite/QualiteScreen.kt:264` | titre\*, gravité (puces), description, coût estimé | ✅ | — | ✅ | ✅ | ❌ | Coût au clavier texte. |
| 58 | Nouvel équipement | `maintenance/MaintenanceScreen.kt:258` | nom\*, périodicité (jours) | ✅ | — | — | ✅ | ❌ | Périodicité au clavier texte. Pas de date de mise en service. |
| 59 | Intervention de maintenance | `:314` | description\*, coût des pièces | ✅ | ⚠️ | — | ✅ | ❌ | Coût au clavier texte ; pas de date d'intervention. |
| 60 | Nouveau bon de livraison | `livraison/LivraisonScreen.kt:348` | client (liste) ou nom libre\*, adresse, transporteur, nombre de colis | ✅ | ⚠️ | ✅ | ✅ | ✅ | Pas de date de livraison prévue. |

## 15. Composants partagés et champs de recherche

| Composant | Emplacement | Rôle | Remarque |
|---|---|---|---|
| `MissaSelecteurDialogue` / `MissaSelecteurLigne` | `components/MissaSelecteur.kt:85-273` | liste de choix avec recherche | ✅ vrai sélecteur |
| `MissaMenuDeroulant` | `components/MissaDesign.kt:254` | menu déroulant | ✅ |
| `DropdownChamp` | `stock/StockUi.kt:337` | menu déroulant Stock | ✅ |
| `ChampDate` / `ChampDateAchat` / `ChampDateFour` | `stock/ProductFormScreen.kt:764`, `purchases/PurchasesScreen.kt:884`, `fournisseurs/FournisseursScreen.kt:1646` | calendrier Material 3 | ✅ mais **trois copies** du même composant |
| `TreChamps` | `tresorerie/TresorerieDialogues.kt:395` | champ texte ou montant filtré | ✅ modèle à généraliser |
| `Champ` (Stock) | `stock/ProductFormScreen.kt:806` | champ texte | ❌ pas de paramètre de clavier (cause des écarts R5 de #17) |
| Recherche | Clients `:533`, Fournisseurs `:330` et `:527`, Achats `:667`, Vente `:661`, Stock (`StockSearchField`) | champ de recherche seul | — |

Aucun champ n'utilise `androidx.compose.material.icons.*` : les icônes sont bien `Iv` et `StockIv`.

---

## Synthèse des écarts (par priorité)

### ❌ Bloquants par rapport à la spec (12 formulaires : #12, #15, #21, #22, #34, #40, #41, #46–#50)

1. **Textes écrits en dur, non traduits (R4)** : **7 dialogues**. Services #46–#50 (29 textes, plus 10 dans `ServiceFieldComponents.kt`) et Comptabilité #40–#41 (26 textes). En anglais, espagnol, arabe ou chinois, ces écrans restent en français.
2. **Dates tapées au clavier (R2)** : #48 planification d'intervention (« jj/MM/aaaa HH:mm ») et #12 recherche de clients (« JJ/MM/AAAA »).
3. **Bouton Enregistrer toujours actif (R1)** : #15 devis, #21 mouvement de stock, #22 transfert, #34 document fournisseur, #40–#41 comptabilité, #46–#49 services.
4. **Liste de choix remplacée par une saisie libre (R3)** : #40 pays ISO et mois d'exercice.

### ⚠️ À corriger (qualité de saisie)

5. **Montants et quantités au clavier texte (R5)** : #17 article (environ 12 champs), #21–#23 stock, #41 OD, #42–#43 production, #57 qualité, #58–#59 maintenance. Il suffit d'ajouter un paramètre de clavier à `Champ()` et de reprendre le filtre de `TreChamps`.
6. **Dates manquantes là où le métier en a besoin** : absence RH (#55), projet (#51), tâche (#53), intervention de maintenance (#59), bon de livraison (#60).
7. **Contrôle seulement au clic** : onboarding entreprise (#1), catégories Stock (#19–#20), facturation d'une commande (#16), actualisation d'un projet (#52).
8. **Valeurs codées saisies en texte libre** : segment, canal, territoire et conditions de paiement du client (#7).

### ♻️ Code en double

9. Trois composants de date identiques (`ChampDate`, `ChampDateAchat`, `ChampDateFour`), deux dialogues « Nouvelle catégorie » (#19 et #20), trois créations rapides de tiers (#3, #14, #29). Un composant commun dans `ui/components/` éviterait que les corrections divergent.

### Formulaires encore absents (écrans vides)

Retours et avoirs de vente, `SalesFlowScreen`, `OperationFormScreen`, et côté administration : Licence, Sauvegarde, Journal, Utilisateurs (création d'utilisateur ou de rôle), Référentiels (saisie de taxes, moyens de paiement et unités).

---

## Plan de correction proposé

| Lot | Contenu | Formulaires | Effort |
|---|---|---|---|
| **A** | Services et Comptabilité : passer tous les textes par `R.string` (environ 65 clés × 5 langues), planification au calendrier et à l'horloge, boutons conditionnés | #40, #41, #46–#50 | moyen |
| **B** | Composants communs `MissaChampTexte(clavier = …)` et `MissaChampDate` dans `ui/components/`, puis remplacement de `Champ`, `ChampDate`, `ChampDateAchat` et `ChampDateFour` | #17, #21–#23, #42–#43, #57–#59 | moyen |
| **C** | Boutons Enregistrer conditionnés | #15, #16, #19–#22, #34, #52 | faible |
| **D** | Recherche de clients au calendrier ; pays et mois de l'exercice en liste | #12, #40 | faible |
| **E** | Champs de date métier manquants (absence, projet, tâche, maintenance, livraison) : **demande une migration Room** et une validation de la spec | #51, #53, #55, #59, #60 | élevé |
