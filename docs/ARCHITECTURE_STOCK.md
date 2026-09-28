# Architecture du module Stock

> **État du code observé : 28 septembre 2026.** Cette note décrit le dépôt tel qu'il existe et distingue explicitement les capacités présentes des travaux encore à faire. Elle ne vaut pas affirmation que le module Stock est complet.

## 1. Rôle et frontières

Stock est le propriétaire des quantités physiques, des mouvements et des inventaires. Les modules Achats, Vente, Production et Maintenance doivent demander une opération Stock par un use case/contrat Stock ; ils ne doivent pas modifier `product_stock` ni insérer dans `stock_movements` eux-mêmes.

```text
Compose → ViewModel → UseCase → Repository → DAO → Room
                                  ↑
                    StockModuleRepository (lectures Stock)

Achats / Vente / Production / Maintenance → contrat Stock → use cases Stock transactionnels
```

Les `Entity` et projections actuellement partagées avec l'UI se trouvent principalement dans `core/data/entity`, `core/data/dao` et `ui/stock`. Le déplacement progressif des projections vers des types de données Stock dédiés est recommandé pour réduire le couplage à Room.

## 2. État fonctionnel et persistance

### Articles et extensions

- `ProductEntity` porte le type, le groupe d'articles optionnel, les capacités `vendable` / `achetable` / `stockable`, les prix, les seuils, le site principal, le fournisseur et les données descriptives.
- Les familles et extensions transverses sont persistées séparément (équipement, déchet, emballage, consignation, kit et composants de kit).
- Les catégories personnalisées sont stockées dans `product_categories`.
- Les règles de capacité existent dans `ProduitRules`; les groupes standards sont définis dans `GroupesStandards`.
- Les noms historiques d'enum comprennent actuellement `ACHATE_REVENDU` et `CONNOMMABLE`. Ne pas les renommer sans migration des données Room et traitement des valeurs sérialisées.

### Stocks, mouvements et sites

- `ProductStockEntity` est identifié par `(produitId, siteId)` et contient `quantite` et `valeur`.
- `StockMovementEntity` enregistre le produit, le site, le type, la quantité, le motif, la référence, le commentaire et les métadonnées de lot/série/péremption.
- Les types techniques comprennent `ENTREE`, `SORTIE`, `AJUSTEMENT`, `TRANSFERT_SORTIE` et `TRANSFERT_ENTREE`.
- Les DAO et use cases de transfert écrivent une paire de mouvements liée par référence.
- Le contrôle de stock négatif et les guards de licence / activation / journalisation sont présents dans les principaux use cases d'écriture.
- Les tables décrites ne portent pas encore de quantités réservées, bloquées ou en transit séparées ; « disponible à vendre » n'est donc pas encore un calcul physique moins réservations/blocages.

### Use cases transactionnels repérés

Les écritures courantes se trouvent notamment dans `core/domain/usecase/StockUseCases.kt` et `ProductUseCases.kt` : mouvement simple, transfert, inventaire sauvegardé, création produit avec stock initial. `StockService` existe dans `StockUseCases.kt` comme classe concrète injectée : il planifie et écrit les sorties de Vente et traite certains retours/compensations dans la transaction Room du document appelant. Ce n'est pas encore le contrat intermodule typé complet (réservations, entrées d'achat avec coût/source, quarantaine, événements).

## 3. Parcours de lecture et présentation

### Hub

- `StockHubRepository` agrège sites, produits, stocks, mouvements et groupes, puis délègue les KPI à `StockHubRules`.
- `StockHubViewModel` consomme ce repository.
- Le tableau de bord historique `StockAccueilViewModel` a encore son propre agrégat de flux et calcule des indicateurs/catégories. Cette duplication doit être résorbée afin que le hub ait une source de vérité unique ; ne pas ajouter de nouveaux KPI concurrents dans les ViewModels.

### ViewModels et progression du refactoring

`StockModuleRepository` et `StockModuleUseCases` ont été ajoutés comme façade data/use case pour les projections multi-tables. Les accès directs DAO ont été retirés de `StockAccueilViewModel` (lecture de session), `StockListeViewModel` (catégories), `StockDetailViewModel` (fiche agrégée), `ProductFormViewModel` (fournisseurs/extensions/photo/équipement) et `StockEquipementsViewModel` (extensions d'équipement). La persistance des extensions produit est maintenant groupée dans une transaction Room.

`StockMovementView` transporte désormais `produitId` depuis le SELECT joint ; le détail filtre ses mouvements par identifiant, plus par code produit.

Le travail restant inclut :

- `StockAccueilViewModel` calcule encore ses propres KPI/catégories en plus de `StockHubRules`; l'unification du hub reste à faire.
- `InventaireViewModel` conserve encore des DAO et une clôture séquentielle. Le prochain changement doit d'abord déplacer ses accès derrière la couche UseCase/Repository puis remplacer la clôture par un use case transactionnel sécurisé (voir §5).
- Les autres écrans/use cases Stock doivent continuer d'être audités pour détecter des dépendances UI → DAO.

Le déplacement d'un appel DAO ne suffit pas à rendre sûre la clôture d'inventaire : il faut préserver ou renforcer les gardes et l'atomicité.

## 4. Architecture cible

### Lecture

```text
UI Compose
  ↓
ViewModel sans DAO
  ↓
Use case de lecture Stock
  ↓
StockModuleRepository / StockHubRepository
  ↓
DAO Room
```

`StockModuleRepository` agrège les flux et expose des projections Stock utiles, sans devenir le lieu où se cachent les règles transactionnelles. Les règles métier restent dans des modèles purs et des use cases testables. Les transformations lourdes (valorisation, regroupement, tendances) ne doivent pas s'exécuter sur le Main Thread ; appliquer un dispatcher de calcul approprié aux flux concernés.

### Écriture

```text
Module consommateur
  ↓ commande métier typée et référencée
StockService
  ↓ validation, disponibilité, transaction/idempotence
Use cases Stock
  ↓
Repositories / DAO Room
```

Les commandes intermodules devront porter au minimum la source métier, le document source et sa ligne, le produit, le site, la quantité, la raison, les informations de lot/série/péremption quand elles s'appliquent, ainsi que l'identité de l'auteur. Les entrées d'achat devront aussi porter le coût unitaire afin de tenir à jour la valorisation/CUMP.

## 5. Dette et ordre de traitement

1. **Terminer le retrait des DAO des ViewModels** : les cinq écrans listés §3 sont migrés vers `StockModuleUseCases`/`StockModuleRepository`; `InventaireViewModel` et tout accès UI résiduel restent à auditer/refactorer.
2. **Créer `CloturerInventaireUseCase`** : vérifier état/autorisation/licence/activation, relire les quantités courantes, calculer et appliquer tous les écarts, écrire les mouvements et le journal, puis clôturer dans une seule transaction. Ajouter tests positifs/négatifs et rollback. Aucun événement `InventoryClosed` n'est actuellement identifié dans le dépôt.
3. **Terminé dans ce lot : `produitId` ajouté à `StockMovementView` et au SELECT Room** ; `StockDetailViewModel` filtre maintenant par identifiant.
4. **Unifier `StockAccueilViewModel` et `StockHubViewModel`** autour de `StockHubRepository` / `StockHubRules` et supprimer les KPI dupliqués.
5. **Corriger `CONNOMMABLE` sans casser les bases existantes** : introduire la nouvelle valeur, migration Room explicite, converters/sérialisateurs/tests/ressources, puis seulement retirer l'ancien identifiant.
6. **Déplacer les calculs lourds des flux** vers `Dispatchers.Default` lorsque le profil de mesure le justifie.
7. Avant d'étendre les parcours intermodules, concevoir les réservations, blocages/quarantaines, mouvements métiers structurés et suivi lot/série jusqu'aux lignes d'inventaire et de transfert.
8. Finaliser un contrat public `StockService` (entrées/sorties/réservations/transferts/quarantaine, validation des sites et événements), puis y intégrer Achats et Vente sans accès direct aux DAO Stock.

## 6. Invariants de contribution

- Toute modification de quantité doit avoir son mouvement correspondant dans la même transaction.
- Aucun stock négatif ; les opérations multi-lignes prévalident toutes les lignes avant écriture.
- Une répétition d'une même commande métier ne doit pas doubler le mouvement.
- Les références de documents et sources doivent rester auditables ; ne jamais supprimer silencieusement un mouvement validé.
- Les validations serveur rechargent produit, statut, capacité et disponibilité ; l'UI ne fait pas foi.
- Les mutations critiques contrôlent licence, activation du module et permissions adaptées.
- Ajouter des tests de règles pures et des tests transactionnels/Room pour les effets, puis compiler l'application.
