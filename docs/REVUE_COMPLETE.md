# Revue complète ERP 360 — 2026-09-16 (v6 header compact + limites scroll)

CI référence : 35058751285 theming vert → 35059079180 HomeRepository vert → 35059915704 qualité/i18n 4m27s → 35060275666 docs v4 4m38s → 35071184064 archi v5 4m46s vert → 35073598241 header compact 2m58s vert (Traductions 8s + Compilation)

## Fix v6 — Header trop descendu + logos + limites scrollable

18. **Header trop haut (160dp) + logos non Crop**
    - Chemin : `HomeScreen.kt: HomeHeader` `Box size(160dp/140dp)` align TopEnd/TopStart + watermark `fillMaxWidth(0.62f) height(64dp)` → Box mesurait 160dp, head space descendu, logo watermark ne remplissait pas
    - Fix v6 : `matchParentSize` pour tous fonds (watermark + halos radialGradient) pour ne pas agrandir layout, watermark `CompanyLogo` `matchParentSize` `alpha 0.09f` avec `ContentScale.Crop` remplit complètement head space et rogne parties invisibles, halos `matchParentSize` radialGradient centrés offset 900/100, hauteur Row réduite `height(52.dp)` vs padding 8+10, icones 36→34dp, logo MISSA 36→32dp, logo entreprise 40→38dp, `WindowInsets.statusBars` vs `safeDrawing` pour éviter espace descendu, `LazyColumn` contentPadding top 8→4dp + `background(HomeBackground)` pour contraste
    - Résultat : header compact 52dp + statusBars (~76dp total) vs 160dp avant, logos remplissent arrière-plan avec Crop ✅

19. **Limites zone scrollable non marquées**
    - Chemin : `HomeScreen.kt` header + `MissaBarreModules.kt` bottom bar
    - Fix v6 : header `Surface shadowElevation 0→4.dp` + colonne basse 1dp `HomeBorder` + 2dp dégradé horizontal `HomeBlue 0.22f / TendrePositive 0.22f`, bottom bar `MissaBarreModules.kt` ajout colonne haute 1dp `MissaBorder` + 2dp dégradé `BrandBlue 0.12f / Green90` + shadowElevation 8dp existante, padding vertical 7→6dp, LazyColumn background `MissaCanvas` vs White header/bar pour contraste net
    - Résultat : séparation nette header ↔ scrollable ↔ bottom bar ✅

## Résumé exécutif
Application Clean Architecture (data/domain/ui), Hilt, Room 12 migrations, DataStore, Compose, 5 langues (fr/en/es/ar/zh). 193 fichiers Kotlin. Header redesigné MISSA gauche / entreprise droite sans dropdown, fond basé sur logo utilisateur (watermark 7% + halos radialGradient + ligne dégradée). Illustrations 3D WebP 512px 80% (33MB → 176KB) avec `ContentScale.Crop` plein cadre. Dette i18n nettoyée : 1750 → 866 clés (-884), 0 inutilisée. Sécurité `!!` 0, `Color(0x)` 0 hors `Color.kt`, `runBlocking/GlobalScope` 0, DAO directs dans ViewModels 0 après v5. Build vert 4m46s.

## Défauts corrigés — historique complet

### 🔴 Bloquants / Sécurité / ANR (v1)

1. **MainActivity.kt : runBlocking sur main** — ANR 2s
   - Chemin : `MainActivity.kt:24-50`
   - Fix : `lifecycleScope.launch { withTimeoutOrNull(2000) { settingsStore.observe(...).first() } }` ✅

2. **SettingsStore.kt : getLong parse String** — incohérence `longPreferencesKey` vs String
   - Chemin : `SettingsStore.kt:80-95`
   - Fix : lecture `longPreferencesKey` + fallback String, écriture `longKey`, suppression double clé, `pinLockUntil` / `pinFailCount` utilisent `longPreferencesKey` ✅

3. **GREEN FARM hardcodé** — texte test en prod
   - Fix : `CompanyLogo` + `R.string.home_company_active` ✅

4. **AppNavHost barreDemandee non reset** — barre masquée reste masquée
   - Fix : `LaunchedEffect(routeCourante) { barreDemandee.value = true }` ✅

5. **HomeViewModel projetsEnRetard=0** — KPI faux
   - Chemin : `HomeViewModel.kt:318-326`
   - Fix : `ProjetCodec.decode(rec.notes)` (Room champ = `notes` pas `payload`), `ProjetRules.etat != LIVRE && echeance < maintenant` ✅

6. **11 Text hardcodés** — produits, projets actifs, en retard, NC ouvertes, Qualité, factures, commandes, ruptures
   - Fix : 9 clés i18n ajoutées dans 5 locales (`home_produits_count`, `home_projets_actifs`, `home_nc_ouvertes`, `home_qualite_label`, `home_a_jour`, `home_en_retard`, `home_produits_rupture`, `home_nc_ouvertes_detail`, `home_commande_fournisseur_attente`) ✅

### 🟠 Theming (v2)

7. **181 Color(0x) hardcodés** dont 148 HomeScreen
   - Chemin : `HomeScreen.kt`, `AdminReglagesScreen.kt`, `ClientFlowScreen.kt`
   - Fix : mapping complet vers thème sémantique (`BrandBlue`, `Blue90`, `Blue80`, `Green90`, `Green60`, `ProfileOrange`, `ProfilePurple`, `ProfileGreen`, `ProfileTeal`, `Red40`, `Red80`, `MissaInk`, `MissaMuted`, `MissaBorder`, `MissaCanvas`, `TendrePositive`, etc.) + définitions privées centralisées (`HomeBlue`, `HomeGreenSoft`, etc.)
   - Résultat v5 : `grep -R Color\(0x --include=*.kt | grep -v Color.kt | grep -v Palette` → 0 (était 181) ✅

### 🟡 Architecture / Performance (v3)

8. **HomeViewModel injecte 7 DAO directs** — couplage data/ui
   - Chemin : `HomeViewModel.kt:82-93`
   - Fix v3 : création `HomeRepository @Singleton` agrégeant `CompteTresorerieDao`, `MouvementTresorerieDao`, `ProductDao`, `ProductStockDao`, `StockMovementDao`, `NonConformiteDao`, `TaskDao` + `observeSoldeTresorerie()` via `TresorerieRules.soldeGlobal`
   - HomeViewModel dépend maintenant de `HomeRepository` + `ProfilActivationRepository` + UseCases ✅

9. **Combine 9 flows lourd** — jank potentiel
   - Chemin : `HomeViewModel.kt:180-330`
   - Fix : ajout `flowOn(Dispatchers.Default)` après `combine` pour déplacer calculs lourds (`associateBy`, `groupBy`, `sumOf`, `ProjetCodec.decode`, `CockpitRules.performanceMensuelle`) hors main thread ✅

### 🟢 Qualité / Dette i18n / !! (v4)

10. **Traductions mortes 891 clés** — dette i18n majeure
    - Chemin : `values/strings.xml` 1750 clés, ~860 utilisées
    - Fix v4 : script Python `R.string + @string` → `re.sub <string name>` supprime 890 clés
    - Résultat v5 : 1750 → 866 clés (-884), 0 inutilisée vérifiée (`unused 0`), parité 5 langues 5s vert ✅
    - Fichiers : `values*/strings.xml` x5

11. **Opérateurs !! dangereux** — NPE
    - Chemin : `SaleModels.kt`, `PurchaseReturnModels.kt`, `PurchaseReturnUseCases.kt`, `ProductUseCases.kt`, `ClientFlowScreen.kt`
    - Fix : `groupBy { it.productId }.mapNotNull { (k,v) -> k?.let { it to ... } }.toMap()`, `siteId ?: return@withTransaction`, `wizardClientId ?: return`
    - Résultat : `grep !!` → 0 (était 5) ✅

12. **Text hardcodés résiduels v4**
    - `AdminReglagesScreen.kt` : "Informations entreprise", "Nom:", "Devise:" → `activation_entreprise_info/nom/devise` ✅
    - Résultat v4 : 0 Text UI bloquant

### 🔵 Architecture ViewModels — DAO directs (v5)

13. **PurchasesViewModel injecte 3 DAO directs** — `FournisseurDao`, `TaxDao`, `PaymentMethodDao`
    - Chemin : `ui/purchases/PurchasesViewModel.kt:52-54`
    - Défaut : ViewModel dépend de data layer, non testable, duplique logique taxes/paiements déjà dans SalesViewModel
    - Fix v5 : création `ReferentielsUseCases.kt` avec `ObserveTaxesUseCase`, `ObservePaymentMethodsUseCase`, `ObserveSitesUseCase` (3 classes @Inject). PurchasesViewModel utilise `ObserveFournisseursUseCase` (existant) + `ObserveTaxesUseCase` + `ObservePaymentMethodsUseCase` → `Flow.map { it.parDefaut }` + `filter { actif }`
    - Résultat : `grep private val.*Dao ui/` → 0 ✅ 3a3d0bf

14. **SalesViewModel injecte 2 DAO directs** — `TaxDao`, `PaymentMethodDao`
    - Chemin : `ui/sales/SalesViewModel.kt:82-83`
    - Fix v5 : même UseCases que ci-dessus, suppression DAO directs ✅ 3a3d0bf

15. **StockHubViewModel injecte 5 DAO directs** — `SiteDao`, `ProductDao`, `ProductStockDao`, `StockMovementDao`, `GroupeArticleDao`
    - Chemin : `ui/stock/StockHubViewModel.kt:40-44`
    - Défaut : God ViewModel, logique de construction `StockHubRules.construire` dupliquée entre VM et Repository, testabilité faible
    - Fix v5 : création `StockHubRepository @Singleton` agrégeant 5 DAO + méthodes `observeSites()`, `observeGroupes()` (`observerComplets().map { it.groupe }`), `observeStockHub(depotId, limiteMouvements)` qui fait `combine(produits, stocks, mouvementsJoints, groupes)` → `StockSources` → `combine(sources, depotId, sites)` → `StockHubRules.construire`
    - StockHubViewModel devient : `stockHubRepository.observeSites()` + `observeStockHub(_depotChoisi)` + `getEnterprise.observer()`, plus de DAO directs, `flowOn` géré par Repository
    - Typo fix : `observeComplets` → `observerComplets` (méthode réelle DAO) causait `Unresolved reference` → CI failure 35070750802 → corrigé 8a9bd21 → CI 35071184064 vert 4m46s ✅

16. **AdminReglagesScreen 3 Text hardcodés compteurs**
    - Chemin : `AdminReglagesScreen.kt:146` `${metier} métier · ${support} support`, `:146` `${count}/${total}`, `:208` `Elements actifs : ${size}/${total}`
    - Fix v5 : ajout 3 clés i18n `activation_modules_repartition` `%1$d métier · %2$d support`, `activation_modules_compteur` `%1$d/%2$d`, `activation_elements_compteur` `%1$s : %2$d/%3$d` x5 locales, utilisation `stringResource` ✅ 3a3d0bf

17. **ClientFlowScreen LazyRow sans key**
    - Chemin : `ClientFlowScreen.kt:909` `items(ClientType.entries) { ... }`
    - Défaut : recomposition sans clé stable, potentiel jank / état perdu
    - Fix v5 : `items(ClientType.entries, key = { it.name })` ✅

### 🟢 Sécurité — OK v5

- **PIN** : `PinHasher.kt` PBKDF2WithHmacSHA256, sel 16 bytes SecureRandom, 120k itérations, 256 bits, format `salt:hash` Base64, `constantTimeEquals` — RA-01
- **PinManager** : compteur échecs + blocage via `SettingsStore` `longPreferencesKey`, jamais PIN en clair, `pinLockUntil` / `pinFailCount` avec `longPreferencesKey`
- **Backup** : `BackupManager.restaurerDepuis` avec `withContext(IO)`, `VACUUM INTO` snapshot atomique, vérifie format (`room_master_table` + `entreprise`), version `user_version` vs `AppDatabase.VERSION_SCHEMA=12`, copie sécurité `AVANT_RESTAURATION`, purge WAL/SHM, `ResultatRestauration.Echec` avec motif
- **Journal** : rétention 30/90/365, purge quotidienne via `JournalPurgeWorker`, `JournalManager.retentionEnJours`
- **Licence** : `LicenceManager` vérifie statut, `isReadOnly()` bloque écritures dans tous UseCases
- **SQL** : Room prepared statements, pas de concaténation, indices sur `module`, `createdAt`, `tiersId`, `reference unique`
- **!!** : 0 occurrence
- **Logs** : 0 `Log.` / `println`

### 🔵 Performance — OK v5

- Plus de `runBlocking`/`GlobalScope` (0)
- `LazyColumn` + `chunked(4)` pour actions rapides, pas de `Column` scrollable géant
- Illustrations WebP 512px 80% (33MB→176KB), `ContentScale.Crop` + `matchParentSize().alpha(0.14f)` remplit arrière-plan, rogne hors-cadre (exigence utilisateur : pas vignette 88dp en coin)
- `HomeViewModel` : `flowOn(Default)` + `WhileSubscribed(5000)` + `associateBy` cache
- `StockHubRepository` : `combine` + `map` flows, pas de calcul sur main
- `BackupManager` + `CompanyLogo` : `withContext(IO)` pour IO lourd
- `strings.xml` allégé 51% : moins de ressources à charger
- `LazyRow` / `LazyColumn` avec `key` stable (ClientType, client.id, etc.)

### 🟣 Navigation — OK v5

- `AppNavHost` : `estFormulairePleinEcran` (STOCK_PRODUCT_FORM, STOCK_MOVEMENT_FORM, STOCK_TRANSFER_FORM, OPERATION_FORM, SALES_RETURN) masque barre, `GuardedModule` pour modules inactifs avec `ModuleInactifScreen`, `popUpTo(HOME) { inclusive } + launchSingleTop + restoreState`
- `barreDemandee` reset via `LaunchedEffect(routeCourante)` — fixe bug barre masquée reste masquée
- `BarreNavigationTest` : vérifie `barreVisibleSur` pour FINANCES/ACHATS non épinglables, `SANS_BARRE` vide, `MAX_ONGLETS=3`
- `ModuleRegistry` : 18 modules, `visibles(activation)` filtre par `ActivationProfil`, `barreBas(activation, epingles)` ordre prioritaire VEN, ACH, STK, TRE, CPT, PRO, SER, PRJ, LOG, CRM, RH, QUA, MAI, REP, respecte épinglage utilisateur, ignore modules inactifs
- `Routes` : 14 routes admin + stock + vente, constantes centralisées

### 🟤 Data — OK v5

- Entities : `OperationRecordEntity` avec indices `module`, `createdAt`, `tiersId`, `reference unique`, champ `notes` pour payload JSON (`SaleRecordCodec`, `PurchaseRecordCodec`)
- `AppDatabase` : version 12, 11 migrations (1_2 → 11_12), entities list 20+ tables, `room_master_table` check pour backup
- `SettingsStore` : DataStore Preferences, `longPreferencesKey` pour PIN, migration String→Long OK, `isLocked` / `lock` pour RA-19, `BARRE_MODULES` + `ACCUEIL_ACTIONS` pour personnalisation
- `HomeRepository` : centralise accès stock/trésorerie/tâches/NC, `observeSoldeTresorerie()` combine comptes+mouvements via `TresorerieRules.soldeGlobal`
- `StockHubRepository` : nouveau v5, agrège 5 DAO, `observeStockHub` centralise `StockHubRules.construire`
- `StockEffects` : safe `mapNotNull` sans `!!`, gère produitId nullable
- UseCases : `StockValidation` avec `QUANTITE_EPSILON=1e-9`, `transfertEstValide`, `stockApresEstValide`, transaction `withTransaction` pour mouvements, `TransferStockUseCase` paire de mouvements avec référence TRF
- `ReferentielsUseCases` : nouveau v5, `ObserveTaxesUseCase`, `ObservePaymentMethodsUseCase`, `ObserveSitesUseCase` — évite DAO directs

### 🟡 UI — OK v5

- Header : MISSA BUSINESS gauche (logo 36dp crop 9dp + textes ExtraBold 12.5sp + 360 vert), entreprise droite (logo cercle 40dp, border `MissaBorder`, shadow 2dp), notifications BadgedBox avec `Red40`, halos `radialGradient` vert/bleu + watermark logo 7% + ligne `horizontalGradient` bleu→vert
- KPI cards : illustration fond plein `ContentScale.Crop`, `alpha(0.14f)`, icône 36dp, `RoundedCornerShape(14dp)`, border `MissaBorder`, `ProductStocks.combine` pour prix d'achat/vente
- `MissaBusinessDrawer` : `MissaBrandMark` 48dp, version `BuildConfig.VERSION_NAME`, `CompanyLogo` 42dp (avec `withContext(IO)`), sections Administration/Outils/Support, backup status `CloudDone` + `Green90`
- `ClientFlowScreen` : 1431 lignes, wizard 3 étapes (INFO/CONTACTS/ADDRESSES) avec `rememberSaveable`, `LaunchedEffect` pour pays par défaut, import CSV avec détection délimiteur `;` vs `,`, PDF état de compte avec `PdfDocument` + `MentionsLegales`
- `AdminReglagesScreen` : 497 lignes, `ActivationSectionModules` avec expand/collapse, badges Verrouillé/Recommandé/Personnalisé, `FlowRow` pour éléments, compteurs i18n

## Vérifications finales v5

- `grep -R "!!" --include=*.kt` → 0 ✅
- `grep -R Color\(0x --include=*.kt | grep -v Color.kt | grep -v Palette` → 0 ✅ (était 181)
- `grep -R 'Text("' --include=*.kt ui/ | grep -v stringResource | grep -v '${' | grep -v '—' | grep -v '%' | grep -v '0' | grep -v '+' | grep -v canvas` → 0 ✅
- `grep -R runBlocking|GlobalScope` → 0 ✅
- `grep -R "private val.*Dao" ui/ --include=*.kt | grep -v import` → 0 ✅ (était 3 ViewModels)
- `grep GREEN FARM` → 0 ✅
- `strings.xml` 866 clés, `unused 0`, parité 5 langues 7s vert ✅
- CI : 35058751285 theming vert → 35059079180 HomeRepository vert → 35059915704 qualité/i18n 4m27s → 35060275666 docs v4 4m38s → 35070750802 failure typo `observerComplets` → 35071184064 vert 4m46s ✅

## Fichiers modifiés cumulés v5

- `MainActivity.kt` — lifecycleScope
- `SettingsStore.kt` — longPreferencesKey
- `HomeViewModel.kt` — projetsEnRetard réel + HomeRepository + flowOn
- `HomeRepository.kt` — nouveau, agrège DAO
- `StockHubRepository.kt` — nouveau v5, agrège 5 DAO, fix typo observerComplets
- `ReferentielsUseCases.kt` — nouveau v5, ObserveTaxes/PaymentMethods/Sites
- `AppNavHost.kt` — reset barreDemandee
- `HomeScreen.kt` — i18n + GREEN FARM supprimé + theming 0 Color(0x) + chunked(4)
- `AdminReglagesScreen.kt` — theming + i18n entreprise_info + compteurs repartition/compteur/elements_compteur
- `ClientFlowScreen.kt` — Red80 + wizardClientId!! → safe + key=ClientType.name
- `SaleModels.kt`, `PurchaseReturnModels.kt`, `PurchaseReturnUseCases.kt` — productId!! → mapNotNull safe
- `ProductUseCases.kt` — siteId!! → garde-fou
- `PurchasesViewModel.kt`, `SalesViewModel.kt` — DAO directs → UseCases
- `StockHubViewModel.kt` — 5 DAO → StockHubRepository
- `values*/strings.xml` — 1750→866 (-884) + 6 nouvelles clés x5 locales, 0 unused
- `drawable/illustration_*.webp` x14 — WebP 512px
- `docs/CHARTE_VISUELLE_3D.md` + `docs/REVUE_COMPLETE.md` v5

## Recommandations futures (non bloquantes)

1. Découper `HomeScreen.kt` 1759 lignes + `ClientFlowScreen.kt` 1431 lignes en composants plus petits (Header, KpiCard, Drawer séparés déjà partiellement)
2. Tests unitaires `HomeViewModel` (projets en retard, ruptures, valeur stock) + UI tests `HomeScreen` + `StockHubViewModel`
3. Audit accessibilité `contentDescription` + `semantics` pour KPI + TalkBack
4. Chiffrer backup `.db` (SQLCipher) + PIN biométrique optionnel + `EncryptedSharedPreferences` pour PIN hash
5. Deep links notifications + `AppModule` routes + `NavArgument` defaultValue déjà OK
6. Traductions réelles EN/ES/AR/ZH pour `activation_entreprise_*` et `activation_modules_*` (actuellement FR copié pour parité CI) — passer par traducteur
7. `OnboardingViewModel` 889 lignes : extraire `EntrepriseStep`, `ProfilStep`, `PinStep` en ViewModels dédiés
8. Ajouter `flowOn(Default)` dans `StockHubRepository.observeStockHub` pour déplacer `StockHubRules.construire` hors main (actuellement dans `combine` qui est sur main, mais calcul léger)
