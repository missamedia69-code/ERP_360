# Architecture du module Services (SER)

## Responsabilités et frontières

SER pilote le cycle opérationnel d'une demande de service : qualification, création d'un ordre d'intervention, affectation et créneau, exécution, temps, rapport et validation. Il conserve aussi les contrats/SLA et les équipements installés chez un client. Les références client et les noms de tiers sont conservés pour l'historique.

- **SER** : demandes, contrats, SLA, ordres, affectations, temps, rapports, pièces jointes et historique des équipements clients.
- **Clients** : identité, contacts et adresses. SER enregistre un instantané du nom client sur les documents et ne remplace pas le dossier client.
- **Stock** : disponibilité, réservation, consommation, retour et mouvements. SER n'injecte aucun DAO Stock, ne met pas à jour `product_stock`, `stock_movements` ou `product_stock_values`; l'intégration de pièces doit passer par un contrat/use case public de STK.
- **Vente** : devis et factures. L'ordre peut devenir « prêt à facturer », mais SER ne crée pas de facture et ne renseigne pas d'identifiant de facture sans intégration VENTE.
- **Trésorerie** : règlements et encaissements réels; aucun encaissement n'est saisi dans SER.
- **CPT** : pièces comptables officielles; SER n'écrit pas d'écriture comptable.
- **RH** : SER lit la liste des employés actifs et les absences pour contrôler une affectation. Les dossiers employés et la paie restent à RH.

## Cycle livré par le socle SER

1. Créer une demande liée à un client et la qualifier.
2. Convertir une demande qualifiée en ordre, ou créer un ordre direct.
3. Planifier l'ordre pour un technicien actif. Un créneau qui chevauche un autre ordre actif ou une absence RH est refusé. Les contrôles et l'affectation sont écrits dans une transaction Room.
4. Faire progresser l'ordre (`À planifier → Planifiée → En route → En cours`), saisir les minutes de travail, et soumettre un rapport de fin.
5. Le rapport met l'ordre en attente de validation. Une validation par une identité applicative distincte de celle ayant créé l'ordre exige le nom du représentant client et un URI de signature client conservé dans le rapport. Après validation, l'ordre passe à « prête à facturer »; aucune facture n'est créée.

Les permissions SER (`SERVICES`) et le verrou de licence sont contrôlés dans les use cases. Les écritures opérationnelles passent par les use cases et sont journalisées. Les dossiers ne sont pas supprimés par les parcours de ce module.

## Données et migration

La version Room 21 ajoute `service_contracts`, `customer_service_assets`, `service_requests`, `service_work_orders`, `service_timesheets`, `service_reports` et `service_attachments`, avec références vers les clients, employés et contrats. Les pièces jointes et signatures sont référencées par URI; les octets des médias ne sont pas stockés en Room. La migration 20→21 crée les tables et index sans supprimer ni convertir les anciennes lignes `operation_records` du module Services.

Les anciennes prestations génériques restent visibles dans une section distincte de l'écran pour conserver leur historique. Elles ne sont pas converties automatiquement en demandes ou ordres, car leur payload n'a pas les informations de provenance, de statut client ou d'exécution nécessaires.

## Limites explicites du palier actuel

- Le fonctionnement offline multi-appareil/synchronisé n'est pas livré ni vérifié. Les nouveaux enregistrements sont persistés localement dans Room.
- La sélection de fichiers permet de rattacher une image/photo et une image de signature par URI persistant; aucune signature dessinée, capture d'image intégrée, gestion de rétention média ni vérification juridique de signature n'est fournie.
- Aucun rapport PDF n'est généré.
- Les écrans de gestion des contrats, actifs, garanties, interventions récurrentes et tableaux SLA complets restent à réaliser. Les tables et règles de SLA constituent le socle de données; les dates d'échéance calculées sont affichées comme alertes simples.
- Les pièces, réservations, coûts de pièces, retours, factures et encaissements ne sont pas intégrés. Aucune écriture Stock, Vente, Trésorerie ou CPT n'est déclenchée par SER.
- Le dispatch intelligent, les itinéraires, les notifications clients, le portail client et la facturation automatique sont hors de ce palier.

## Prochaines phases recommandées

1. Compléter le terrain : fiche de rapport consultable, capture de signature et photos avec tests appareils, recherche/historique, rôles technicien/dispatch/validation et contrôles d'accès éprouvés.
2. Ajouter la réservation/consommation de pièces via Stock, puis l'émission de devis/factures via Vente et l'émission d'événements vers CPT, avec clés idempotentes et audit.
3. Ajouter les écrans contrats/SLA, garanties, équipements et SAV, ainsi que les règles de quotas/dépassement.
4. Ajouter une optimisation de planning et des rapports plus avancés seulement après mesure et tests des étapes précédentes.
