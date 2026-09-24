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
- **Couleurs sémantiques** : dépense = rouge, gains/argent/compta = vert, production = orange,
  achats = jaune, vente = bleu, stock = gris.
- **Convention C7 — jamais de suppression physique** : statuts (ARCHIVE, DESACTIVE…) ou
  annulation par **contre-passation** (pièces et mouvements inverses). Seule exception :
  retrait d'un document joint fournisseur (tracé dans l'audit).
- **Traductions ×5 obligatoires** pour toute nouvelle clé `R.string` ; vérifier avec
  `python3 .github/scripts/verifier_traductions.py` (parité des 5 fichiers).
  Échapper les apostrophes `\'` et les `%` littéraux `%%`.
- **Pas de nouvelle dépendance** sans nécessité démontrée.
- **Barre du bas** : flottante par ombre, hauteur réduite, cibles ≥ 48 dp, identique partout.

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
- **Room** : migrations SQL obligatoires dans `AppDatabase` (version courante **17**),
  enregistrées dans `di/DatabaseModule`. `@Insert(onConflict = REPLACE)` — pas de `@Upsert`.
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

## Organisation du travail

- La branche `arena/01a0d29b-erp-360` est pilotée par l'agent Arena (sessions de spécification
  par module). Pour du travail local : créer une branche dédiée depuis la pointe à jour et
  fusionner par PR — ne jamais éditer en parallèle les mêmes fichiers sur deux agents
  (conflits de fusion garantis, ex. `strings.xml`).
- Chaque module se fait par phases : maquette/spec → validation → implémentation complète
  jusqu'à la CI verte.
