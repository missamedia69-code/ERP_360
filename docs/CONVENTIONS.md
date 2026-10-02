# Conventions de développement — Missa Business 360

Application ERP Android hors-ligne : Kotlin, Jetpack Compose, Hilt, Room, navigation Compose.
Un seul module Gradle (`app`). Cinq langues : `values` (fr), `values-en`, `values-es`, `values-zh`, `values-ar`.

## Règles non négociables (validées par le propriétaire)

- **Icônes** : uniquement les sets générés `Iv` (`ui/icons/IvIcons.kt`) et `StockIv`
  (`ui/stock/StockIcons.kt`), style Phosphor, via `painterResource(Iv.X)`.
  **Jamais** `androidx.compose.material.icons.*`, jamais de conversion SVG→ImageVector maison.
- **Toutes les icônes en NOIR** (`tint = MissaInk`). La couleur du module colore uniquement
  le header et le fond de la barre du bas. « Achats » = `Iv.CartArrowDown`.
- **Palette globale monochrome (validée propriétaire)** : tous les modules et l’onboarding utilisent
  le bleu nuit Stock `MissaInk` (`#101C43`) sur `MissaCanvas` (blanc `#FFFFFF`), cartes grises
  `OnbConfigCard` (`#F1F3F7`, sans bordure, rayon 12 dp), champs blancs contournés `MissaBorder`
  (`#CBD5E8`, rayon 10 dp).
  `AppModule.couleur` reste la source unique et vaut actuellement `MissaInk` pour chaque module ;
  `couleurPale` est une dilution à 10 % et `couleurDouce` à 26 %. Les futures différenciations
  colorées doivent être locales, explicites et validées, jamais réintroduites globalement.
- **Couleurs sémantiques uniquement** : dépense/erreur = rouge, gain/succès = vert, avertissement =
  orange. Elles ne servent jamais à identifier un module.
- **Convention C7 — jamais de suppression physique** : statuts (ARCHIVE, DESACTIVE…) ou
  annulation par **contre-passation** (pièces et mouvements inverses). Seule exception :
  retrait d'un document joint fournisseur (tracé dans l'audit).
- **Traductions ×5 obligatoires** pour toute nouvelle clé `R.string` ; vérifier avec
  `python3 .github/scripts/verifier_traductions.py` (parité des 5 fichiers).
  Échapper les apostrophes `\'` et les `%` littéraux `%%`.
- **Pas de nouvelle dépendance** sans nécessité démontrée.
- **Barre du bas** : flottante par ombre, hauteur réduite, cibles ≥ 48 dp, identique partout.
- **Densité compacte (validée propriétaire)** sur toute l'application :
  - jetons `MissaLayout` : marges écran 12 dp, écarts 8 dp, sections 12 dp, coins 12 dp ;
  - barre de titre 52 dp ;
  - formulaires via le kit `ui/components/MissaFormulaire.kt` : champs d'environ 44 dp avec le libellé au-dessus de la valeur, deux champs par rangée (`MissaRangee`), boutons 44 dp.
  - Ne pas réintroduire d'espacements de 16 à 24 dp, ni de champs `OutlinedTextField` de 56 dp.

## Design de référence (écran « Informations sur votre entreprise »)

Tout écran suit cet écran : fond blanc ; en-tête = flèche discrète à gauche + titre centré 19 sp gras
(`MissaTopAppBar`, toujours blanc, le paramètre `couleurFond` est ignoré) ; contenu en cartes grises
`MissaCarteSection` (titre numéroté ①②③, pastille « À compléter » ou « Valeur invalide », champs blancs) ;
listes de choix = `MissaSelecteurLigne` ; bouton principal `MissaBoutonPrincipal` (48 dp, désactivé = couleur à 35 %)
épinglé en bas (`MissaFormPied`) ; cartes de liste = `OnbConfigCard` sans bordure (`MissaPanel`). Une bordure de
carte n'est conservée que si elle porte un sens (niveau de risque).

## Formulaires (spécification validée)

- Bouton Enregistrer **désactivé tant que les champs requis sont vides**.
- Dates = petits calendriers (`DatePickerDialog` Material3, `@OptIn(ExperimentalMaterial3Api::class)`).
  Un sélecteur qui « ne déroule rien » est inacceptable : `DropdownMenu` réel.
- Sections dynamiques selon le contexte ; champs conditionnels ; libellés adaptés au type
  (ex. stock : minimum/maximum/sécurité ; les valeurs codées s'affichent avec leur signification lisible).
- Images : compression avant enregistrement (`ImageProduit`, `PieceJointeAchat` :
  JPEG ≤ 1024 px q70, PDF ≤ 8 Mo, stockage interne `filesDir`).

## Architecture

```
app/src/main/java/com/missa/b360/
├── core/data/          # Room : entity/, dao/, db/AppDatabase.kt
├── core/domain/        # model/ (règles pures + codecs JSON), usecase/
├── core/util/          # SequenceManager (numérotation DocType), Iso4217, etc.
├── ui/<module>/        # Screen + ViewModel (Hilt) par module
└── ui/navigation/      # AppNavHost, AppModule (routes + couleurs)
```

- **Règles métier = objets purs** dans `core/domain/model` (ex. `FournisseurRules`,
  `AchatCommandeRules`, `ProduitRules`) : sans Android, testées en JVM.
- **Numérotation** : `SequenceManager.next(DocType.X)` — préfixes : `FA` facture vente,
  `FFR` facture fournisseur, `BC` bon de commande, `RE` réception, `FRN` fournisseur, etc.
- **Room** : migrations SQL obligatoires dans `AppDatabase` (version courante **23**, chaîne
  `MIGRATION_1_2` → `MIGRATION_22_23`, **74 entités**), enregistrées dans `di/DatabaseModule`.
  Les schémas exportés (`app/schemas/`) sont commités automatiquement par la CI après un build vert
  (versions 8–11 et 13–20 jamais exportées : perdues). `@Insert(onConflict = REPLACE)` — pas de `@Upsert`.
  Enums persistés en TEXT (Room ≥ 2.6, support natif). Les colonnes ajoutées par migration
  doivent avoir un `DEFAULT` et correspondre exactement au schéma attendu par Room.
- Les `StateFlow` exposés aux écrans passent par `stateIn(viewModelScope, WhileSubscribed(5s), …)`.
- Pièces d'achat = `operation_records` (module `ACHATS`) avec payload JSON dans `notes`
  (`PurchaseRecordCodec`, `CommandeAchatCodec`, `ReceptionCodec`).

## Module Clients

- **Dossiers** `ui/clients/` : `list/`, `detail/`, `form/`, `account/`, `activity/`, `followups/`,
  `csvimport/` (jamais `import` : mot-clé Kotlin), `components/`. Un écran = un `ViewModel` + un `UiState`
  immuable ; aucun fichier > 400 lignes. Routes dans `ClientRoutes` (état de navigation dans la pile,
  jamais dans une variable locale) ; `clients?create=true` reste l'entrée de l'Accueil.
- **Règles pures** (`core/domain/model`) : `CreditPolicy` (risque NORMAL/ATTENTION/ELEVE/BLOQUE et verdict
  `canSell` ALLOW/WARN/BLOCK avec motif), `AgedBalanceRules` (5 tranches), `ClientMetricsRules`,
  `ClientLifecycleRules` (table de transitions de statut), `ClientFollowupRules`, `ClientBalanceRules`.
  Le risque seul ne bloque jamais une vente ; un blocage est une action manuelle confirmée.
- **Source unique des montants** : `client_balances`, recalculée dans la même transaction par
  `ClientBalanceUseCase` à chaque vente, encaissement (`client_payments`), avoir ou annulation ;
  `reconstruireTout()` et `ClientBalanceWorker` rattrapent les écarts. Liste, fiche, compte et Ventes lisent
  cette table ; ne jamais recalculer un encours dans un écran.
- **Jamais de suppression physique** : désactiver/archiver (statut). `client_followups` et `client_payments`
  ne sont jamais supprimées ; une erreur d'encaissement se corrige par une contre-passation.
- **Interface** : la couleur porte le risque mais toujours avec une icône (cadenas pour BLOQUE) ; cibles
  ≥ 48 dp ; pas de tableau à défilement horizontal ; icônes `Iv` ; montants dans la devise de l'entreprise.
- **Intégrations** : Ventes affiche `ClientCreditBanner` (risque + verdict) et lit l'encours de
  `client_balances` ; notifications `CLIENT_PROMESSE` (→ relances) et `CLIENT_LIMITE` (→ liste) ; le rappel
  « factures en retard » de l'Accueil ouvre `clients/relances` ; le catalogue expose « Relances clients » et
  « Balance âgée ».

## Chaîne d'achat (modules Achats + Fournisseurs)

- Cycle fournisseur : `BROUILLON → A_VALIDER → ACTIF → SUSPENDU/BLOQUE → ARCHIVE`
  (`FournisseurRules.transitions`). Commande validable seulement si fournisseur **ACTIF** ;
  paiement possible pour ACTIF/SUSPENDU/BLOQUE (obligations existantes).
- Réception bornée par la commande ; règlement numéroté `FFR…`, `-R2`, `-R3` ;
  annulation = contre-passation (`réf-ANN`), jamais de DELETE.
- Modification sensible d'un fournisseur ACTIF (fiscal, raison sociale, pays, conditions de
  paiement) → retour automatique en `A_VALIDER`.

## Tests & CI

- Tests JVM : `./gradlew testDebugUnitTest` (règles pures, codecs, migrations logiques).
- Build complet : `./gradlew assembleDebug testDebugUnitTest`.
- CI GitHub Actions (`.github/workflows/android.yml`) : parité des traductions → compilation →
  tests → APK en artefact **et** dans la release fixe `apk-latest`
  (https://github.com/missamedia69-code/ERP_360/releases/download/apk-latest/app-debug.apk).
- **Catalogue honnête** : `DestinationsFonctions` ne route une fonctionnalité que si elle ouvre un
  écran réel (garde-fou `DestinationsFonctionsTest`) — jamais vers un `PlaceholderScreen`.
- **Ne jamais annoncer une fonctionnalité terminée sans CI verte** (ou build local vert).
  En cas de CI rouge sans accès aux logs : la page du run affiche les annotations fichier:ligne.

## Pièges connus (vécus)

- `collectAsStateWithLifecycle(initial = …)` n'existe pas → `stateIn` côté ViewModel.
- Le `content` de `LazyColumn` n'est **pas** `@Composable` : les appels composables vont dans
  les blocs `item { }` ; un builder `LazyListScope` ne doit pas être `@Composable`.
- Pas de smart-cast sur les propriétés déléguées Compose (`by collectAsStateWithLifecycle`) :
  copier dans une `val` locale.
- `stringResource` interdit dans les lambdas non-composables (`buildString`, etc.).
- Propriété injectée et fonction du ViewModel homonymes → ambiguïté : suffixer la propriété
  (`saveCommandeAchat`, `changerStatutUseCase`…).
- `matchParentSize` = BoxScope uniquement.
- **Deux classes de test homonymes** dans le même package (`com.missa.b360`) = « Redeclaration » :
  `compileDebugUnitTestKotlin` échoue alors que `assembleDebug` passe (cas vécu avec
  `ProductionRulesTest`). Un nom de classe de test unique par fichier.
- Journaux CI inaccessibles : `gh api repos/<owner>/<repo>/check-runs/<job-id>/annotations` donne
  les erreurs fichier:ligne (le téléchargement des logs/artefacts peut échouer).

## Organisation du travail

- `main` est la branche stable ; les développements se font sur des branches dédiées fusionnées
  par pull request (la CI tourne sur `main`, `arena/**` et les PR). Ne jamais éditer en parallèle
  les mêmes fichiers depuis deux branches (conflits garantis, ex. `strings.xml`).
- Chaque module se fait par phases : maquette/spec → validation → implémentation complète
  jusqu'à la CI verte.
