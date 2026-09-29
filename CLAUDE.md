# Missa B360 — ERP Android (conventions du projet)

Application ERP Android hors-ligne : Kotlin, Jetpack Compose, Hilt, Room, navigation Compose.
Un seul module Gradle (`app`). Cinq langues : `values` (fr), `values-en`, `values-es`, `values-zh`, `values-ar`.

## Règles non négociables (validées par le propriétaire)

- **Icônes** : uniquement les sets générés `Iv` (`ui/icons/IvIcons.kt`) et `StockIv`
  (`ui/stock/StockIcons.kt`), style Phosphor, via `painterResource(Iv.X)`.
  **Jamais** `androidx.compose.material.icons.*`, jamais de conversion SVG→ImageVector maison.
- **Toutes les icônes en NOIR** (`tint = MissaInk`). La couleur du module colore uniquement
  le header et le fond de la barre du bas. « Achats » = `Iv.CartArrowDown`.
- **Couleur de module** : source unique `AppModule.couleur` (ui/navigation). Déclinaisons :
  `couleurPale` alpha 0.45, `couleurDouce` alpha 0.26 (ne pas revenir à 14/12 %).
- **Couleurs sémantiques** (montants/indicateurs) : dépense = rouge, gains/argent = vert.
- **Palette des modules** (source de vérité = `AppModule` dans `ModuleRegistry.kt`, verrouillée par
  `ModuleRegistryTest`) : Vente bleu `#2563EB` · Stock gris `#6B7280` · Clients violet `#8B5CF6` ·
  Achats **et** Fournisseurs orange `#F28A16` (fond pâle fixe `#FFF2E2`) · Production violet `#8B5CF6` ·
  Trésorerie bleu nuit `#1E3A8A` · Comptabilité/Finances ardoise `#475569` · Services `#DB2777` ·
  Projets `#6366F1` · RH `#E11D48` · Livraison `#38BDF8` · CRM `#C026D3` · Qualité `#7C3AED` ·
  Maintenance `#B91C1C` · Logistique `#65A30D` · Reporting `#0E7490`. Changer une teinte = modifier
  `AppModule` **et** le test, avec validation du propriétaire.
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
- **Room** : migrations SQL obligatoires dans `AppDatabase` (version courante **21**, chaîne
  `MIGRATION_1_2` → `MIGRATION_20_21`, **67 tables**), enregistrées dans `di/DatabaseModule`.
  Les schémas exportés (`app/schemas/`) sont commités automatiquement par la CI après un build vert
  (versions 8–11 et 13–20 jamais exportées : perdues). `@Insert(onConflict = REPLACE)` — pas de `@Upsert`.
  Enums persistés en TEXT (Room ≥ 2.6, support natif). Les colonnes ajoutées par migration
  doivent avoir un `DEFAULT` et correspondre exactement au schéma attendu par Room.
- Les `StateFlow` exposés aux écrans passent par `stateIn(viewModelScope, WhileSubscribed(5s), …)`.
- Pièces d'achat = `operation_records` (module `ACHATS`) avec payload JSON dans `notes`
  (`PurchaseRecordCodec`, `CommandeAchatCodec`, `ReceptionCodec`).

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

- Les branches `arena/*` sont pilotées par l'agent Arena (une branche par session de
  spécification ; la CI tourne aussi sur `arena/**`). Pour du travail local : créer une branche dédiée depuis la pointe à jour et
  fusionner par PR — ne jamais éditer en parallèle les mêmes fichiers sur deux agents
  (conflits de fusion garantis, ex. `strings.xml`).
- Chaque module se fait par phases : maquette/spec → validation → implémentation complète
  jusqu'à la CI verte.
