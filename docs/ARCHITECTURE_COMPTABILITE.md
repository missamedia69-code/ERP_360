# Architecture Comptabilité (CPT)

## Rôle et frontière

CPT est propriétaire du registre comptable. Vente, Achats, Stock et Trésorerie ne doivent ni appeler `AccountingDao` ni écrire directement dans ses tables. Une intégration publiera un `AccountingPostingEvent` à `CreateAccountingVoucherUseCase.fromEvent`; CPT résout la règle active par `eventType`, choisit le journal et les comptes, calcule les lignes, contrôle l'équilibre, puis conserve le lien source et la clé d'idempotence.

Le chemin d'une pièce est :

1. événement métier source validé, avec une clé stable et unique ;
2. règle comptable CPT configurée pour le référentiel sélectionné ;
3. pièce `DRAFT` équilibrée, persistée en transaction avec ses lignes ;
4. validation via `PostAccountingVoucherUseCase` par un utilisateur ayant les permissions requises ;
5. état `POSTED` immuable ; correction par extourne, jamais par réécriture des lignes historiques.

Les journaux configurés avec `requiresApproval` refusent l'auto-validation par le même utilisateur. Les périodes `CLOSED`/`LOCKED` rejettent toute nouvelle saisie ou comptabilisation. Des triggers SQLite empêchent la modification/suppression des pièces et lignes comptabilisées, y compris en contournant les use cases.

## Données persistées

- `accounting_settings` : référentiel, pays, libellé de régime fiscal et premier mois d'exercice ;
- `accounting_accounts` : comptes configurables, classes, sens normal, comptes auxiliaires et imputabilité ;
- `accounting_journals` et `accounting_posting_rules` : journaux et correspondance événement → lignes comptables ;
- `accounting_periods` : périodes ouvertes, clôturées ou verrouillées ;
- `accounting_vouchers` et `accounting_entry_lines` : pièces, références, montants débit/crédit, acteurs, audit et dimensions auxiliaires.

La base passe de la version 18 à 19 pour le registre et de 19 à 20 pour les règles. Aucune migration destructive n'est utilisée.

## État de livraison et limites

Le socle SYSCOHADA révisé installé depuis CPT est un **petit gabarit de départ**, non un plan exhaustif ni certifié. Le pays et le régime peuvent être enregistrés, mais leurs règles fiscales locales et exports déclaratifs ne sont pas fournis. Les use cases permettent la configuration des comptes, journaux et règles ; l'écran expose actuellement l'initialisation, le profil pays/régime et une saisie OD simple à deux lignes.

Les modules source ne sont pas encore raccordés à `AccountingPostingEvent`, et aucune règle métier n'est préchargée. La TVA de l'ancien tableau de bord reste une estimation issue des opérations, pas un calcul de déclaration. Ne pas présenter ce tableau, le gabarit ou les flux actuels comme des états SYSCOHADA légaux.

## Suites prévues

1. raccorder un événement validé de Vente, puis Achats, Stock et Trésorerie, sans accès direct aux tables CPT ;
2. faire valider les mappings comptables et taxes pour chaque pays/régime avec des exemples réconciliés ;
3. ajouter auxiliaires clients/fournisseurs, lettrage, grand livre, balance et clôture annuelle ;
4. produire et tester les états financiers/export uniquement pour les référentiels réellement pris en charge ;
5. poursuivre ensuite les immobilisations, amortissements et l'analytique.
