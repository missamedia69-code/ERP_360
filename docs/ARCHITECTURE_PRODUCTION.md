# Architecture du module Production (PRO)

## Frontières

PRO définit et exécute les lots de fabrication. STK reste l'unique propriétaire des quantités, des mouvements et de la valeur du stock. `SaveProductionOrderUseCase` n'injecte aucun DAO Stock : il transmet l'ordre au cas d'usage Stock `ProductionStockUseCase`, qui vérifie les matières et écrit, dans la transaction de l'ordre, les sorties matière et l'entrée produit fini. Une ligne de stock n'est jamais modifiée depuis l'UI ou le use case PRO.

- **PRO** : ordre, instantané de produit/composants, quantité, référence et état métier de premier palier.
- **STK** : dépôts source/destination, contrôles de disponibilité, mouvements physiques, stock et valeur matières/produits.
- **ACH / Fournisseurs** : non appelés automatiquement pour une matière manquante.
- **QUA / MAI / RH / VEN / CPT / TRE / PRJ / LOG** : pas d'événement ni d'écriture intermodule dans ce palier. Les écritures officielles restent à CPT; le coût matière stocké ici n'est pas une pièce comptable.

## Parcours actuellement livré

1. Choisir un produit fabricable et ajouter les articles composants autorisés.
2. Enregistrer un ordre brouillon sans effet Stock.
3. Lancer un nouvel ordre ou un brouillon. Le backend revalide le produit, les composants, la permission `PRODUCTION.VALIDATE`, l'activation PRO, la licence, les quantités et l'activation STK.
4. Dans une transaction, STK valide toutes les matières avant la première écriture, vérifie l'idempotence de la référence OF, prélève chaque composant sur un ou plusieurs dépôts (dépôt principal prioritaire, puis stock le plus élevé), retire quantités et valeurs au coût moyen pondéré, puis ajoute le produit fini au dépôt principal du produit (ou au site principal de repli). L'OF et ses mouvements sont atomiques; un refus de stock ne laisse ni OF validé ni mouvement partiel.
5. Le coût matière consommé et le dépôt de réception sont conservés dans le payload de l'OF. La valeur ajoutée au produit fini comprend uniquement les matières valorisées (pas la main-d'œuvre, les machines, la sous-traitance ou les frais indirects).

Les mouvements Stock utilisent les motifs `PRODUCTION_CONSUMPTION` et `PRODUCTION_OUTPUT`, partagent la référence OP et sont écrits seulement dans le cas d'usage STK. Les pièces non stockables/valorisées suivent les règles de groupes d'articles existantes.

## Limites importantes

Ce parcours est un **lot complet en une seule validation**, et non encore un système d'atelier complet. Il ne réserve pas les matières à la planification : elles sont contrôlées puis réellement consommées au lancement. La quantité composant saisie est la quantité réelle totale du lot; elle n'est pas calculée automatiquement par unité de nomenclature.

Ne sont pas encore livrés : catalogue de nomenclatures versionnées, gammes, postes/capacité, OF partiels et opérations, décompte temps opérateur, rebuts/sous-produits, contrôle qualité/quarantaine, lots/séries, MRP, demande d'achat, sous-traitance, sélection des dépôts au formulaire, coût complet ou écriture comptable. L'entrée produit fini est immédiatement disponible dans STK; aucune porte de contrôle qualité ne la bloque dans ce palier.

Le registre courant réutilise `operation_records`; il conserve les ordres historiques, mais n'offre pas encore les états détaillés de cycle de vie définis dans la spécification. La dénomination des tuiles/statuts ne doit pas faire passer les fonctions absentes pour livrées.

## Prochaines phases

1. Ajouter BOM et lignes versionnées avec quantité de référence, pertes et validité; associer les OF à un instantané de BOM et calculer les besoins.
2. Étendre le contrat STK avec de vraies réservations idempotentes, libération et consommation d'une réservation. Ajouter l'écran de planification et l'affectation explicite des dépôts.
3. Séparer lancement, consommation manuelle, déclarations bonnes/rebuts, réception produit fini et clôture; tests d'atomicité/rollback sur chaque étape.
4. Ajouter gammes, postes, temps, qualité, traçabilité lot/série et composants de coûts; intégrer ACH, QUA, MAI et RH derrière des contrats propres.
5. Publier des événements de coût/production aux modules CPT et REP; ne jamais créer des pièces CPT depuis PRO.

## Tests

Les règles pures couvrent la validité des quantités et l'agrégation de composants en doublon. La compilation/tests Android doivent également vérifier l'atomicité Room de lancement et la migration éventuelle de tout nouveau schéma. La mise à jour Stock multi-table est conçue pour l'appel transactionnel de l'ordre; ajouter des tests Room de rollback/duplication avant d'étendre le workflow.
