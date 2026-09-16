# Revue complète ERP 360 — 2026-09-16 (v3 finale)

CI référence : 35058751285 (vert theming) → 35059079180 (vert HomeRepository) → nouveau CI v3 en cours

## Résumé exécutif
Application Clean Architecture (data/domain/ui), Hilt, Room, DataStore, Compose, 5 langues. Header redesigné MISSA gauche / entreprise droite sans dropdown, fond basé sur logo utilisateur (watermark 7% + halos radialGradient + ligne dégradée). Illustrations 3D WebP 512px 80% (33MB → 176KB) avec `ContentScale.Crop` plein cadre. Tous défauts bloquants corrigés, theming centralisé, architecture découplée.

## Défauts corrigés — historique

### 🔴 Bloquants / Sécurité / ANR (v1)

1. **MainActivity.kt : runBlocking sur main** — ANR 2s
   - Chemin : `MainActivity.kt:24-50`
   - Fix : `lifecycleScope.launch { withTimeoutOrNull }` ✅ aef30c1

2. **SettingsStore.kt : getLong parse String** — incohérence `longPreferencesKey` vs String
   - Chemin : `SettingsStore.kt:80-95`
   - Fix : lecture `longPreferencesKey` + fallback String, écriture `longKey`, suppression double clé ✅ aef30c1

3. **GREEN FARM hardcodé** — texte test en prod
   - Fix : `CompanyLogo` + `R.string.home_company_active` ✅ 0414e9e

4. **AppNavHost barreDemandee non reset** — barre masquée reste masquée
   - Fix : `LaunchedEffect(routeCourante) { barreDemandee=true }` ✅ aef30c1

5. **HomeViewModel projetsEnRetard=0** — KPI faux
   - Chemin : `HomeViewModel.kt:318-326`
   - Fix : `ProjetCodec.decode(rec.notes)` (Room champ = `notes`), `ProjetRules.etat != LIVRE && echeance < maintenant` ✅ 5baf1eb

6. **11 Text hardcodés** — produits, projets actifs, en retard, NC ouvertes, Qualité, factures, commandes, ruptures
   - Fix : 9 clés i18n ajoutées dans 5 locales (`home_produits_count`, `home_projets_actifs`, `home_nc_ouvertes`, `home_qualite_label`, `home_a_jour`, `home_en_retard`, `home_produits_rupture`, `home_nc_ouvertes_detail`, `home_commande_fournisseur_attente`) ✅ aef30c1

### 🟠 Theming (v2)

7. **181 Color(0x) hardcodés** dont 148 HomeScreen
   - Chemin : `HomeScreen.kt`, `AdminReglagesScreen.kt`, `ClientFlowScreen.kt`
   - Fix v2 : mapping complet vers thème sémantique (`BrandBlue`, `Blue90`, `Blue80`, `Green90`, `Green60`, `ProfileOrange`, `ProfilePurple`, `ProfileGreen`, `ProfileTeal`, `Red40`, `Red80`, `MissaInk`, `MissaMuted`, `MissaBorder`, `MissaCanvas`, `TendrePositive`, etc.) + définitions privées centralisées (`HomeBlue`, `HomeGreenSoft`, etc.)
   - Résultat : `grep -R Color\(0x` hors `Color.kt` = 0 (était 181) ✅ f8b12ba + 4eb79b2 (fix imports récursifs)

### 🟡 Architecture / Performance (v3)

8. **HomeViewModel injecte 7 DAO directs** — couplage data/ui, testabilité réduite
   - Chemin : `HomeViewModel.kt:82-93`
   - Fix v3 : création `HomeRepository @Singleton` agrégeant `CompteTresorerieDao`, `MouvementTresorerieDao`, `ProductDao`, `ProductStockDao`, `StockMovementDao`, `NonConformiteDao`, `TaskDao` + méthode `observeSoldeTresorerie()` combinant comptes+mouvements via `TresorerieRules.soldeGlobal`
   - HomeViewModel dépend maintenant de `HomeRepository` + `ProfilActivationRepository` + UseCases, plus de DAO directs ✅ 3658b4a

9. **Combine 9 flows lourd** — recalcul à chaque émission, jank potentiel
   - Chemin : `HomeViewModel.kt:180-330`
   - Fix v3 : ajout `flowOn(Dispatchers.Default)` après `combine` pour déplacer calculs lourds (valeur stock `associateBy`, `groupBy`, `sumOf`, `ProjetCodec.decode`, `CockpitRules.performanceMensuelle`, etc.) hors main thread ✅ 3658b4a

10. **Traductions mortes 891 clés** — dette i18n
    - Chemin : `values/strings.xml`
    - État : non supprimé dans ce push pour éviter régression, mais script `find_unused_strings.py` disponible. Recommandation : nettoyage par lot de 100 clés avec vérification `R.string.` + tests. Dette documentée, non bloquante.

11. **Accessibilité** — `contentDescription = null` sur icônes décoratives OK, mais icônes fonctionnelles (menu, notifications) ont `stringResource`. `Text(text="+")` et `Text("—")` tolérés (symboles). Reste : `AccueilKpiCard` icône décorative null OK, mais ajouter `semantics { contentDescription }` pour KPI si besoin futur.

### 🟢 Sécurité — OK

- **PIN** : `PinHasher.kt` PBKDF2WithHmacSHA256, sel 16 bytes SecureRandom, 120k itérations, 256 bits, format `salt:hash` Base64, `constantTimeEquals` — conforme RA-01
- **PinManager** : compteur échecs + blocage via `SettingsStore` `longPreferencesKey`, jamais PIN en clair
- **Backup** : `BackupManager.restaurerDepuis` avec `withContext(IO)`, vérifie format/version, copie sécurité préalable, `ResultatRestauration.Echec` avec motif
- **Journal** : rétention 30/90/365, purge quotidienne, `JournalManager.retentionEnJours`
- **Licence** : `LicenceManager` vérifie statut, `isReadOnly()` bloque écritures
- **SQL** : Room prepared statements, pas de concaténation

### 🔵 Performance — OK après v3

- Plus de `runBlocking`/`GlobalScope`
- `LazyColumn` + `chunked(4)` pour actions rapides, pas de `Column` scrollable géant
- Illustrations WebP 512px 80% (33MB→176KB), `ContentScale.Crop` + `matchParentSize().alpha(0.14f)` remplit arrière-plan, rogne hors-cadre (exigence utilisateur : pas vignette 88dp en coin)
- `HomeViewModel` : `flowOn(Default)` + `WhileSubscribed(5000)` + `associateBy` cache
- `BackupManager` + `CompanyLogo` : `withContext(IO)`

### 🟣 Navigation — OK

- `AppNavHost` : `estFormulairePleinEcran` (STOCK_PRODUCT_FORM, STOCK_MOVEMENT_FORM, STOCK_TRANSFER_FORM, OPERATION_FORM, SALES_RETURN) masque barre, `GuardedModule` pour modules inactifs avec `ModuleInactifScreen`, `popUpTo(HOME) { inclusive } + launchSingleTop + restoreState`
- `barreDemandee` reset via `LaunchedEffect(routeCourante)`
- `BarreNavigationTest` : vérifie `barreVisibleSur` pour FINANCES/ACHATS non épinglables, `SANS_BARRE` vide
- Manque : deep links notifications (non requis ERP), mais `NavArgument` avec `defaultValue` OK

### 🟤 Data — OK

- Entities : `OperationRecordEntity` avec indices `module`, `createdAt`, `tiersId`, `reference unique`, champ `notes` pour payload JSON
- `ProjetUseCases` : `observer()` décode `piece.notes` via `ProjetCodec`, `trier()` via `ProjetRules`
- `SettingsStore` : migration String→Long OK, suppression double clé
- `HomeRepository` : centralise accès stock/trésorerie/tâches/NC

### 🟡 UI — OK après theming

- Header : MISSA BUSINESS gauche (logo 36dp crop 9dp + textes ExtraBold 12.5sp + 360 vert), entreprise droite (logo cercle 40dp, border `MissaBorder`, shadow 2dp), notifications BadgedBox avec `Red40`, halos `radialGradient` vert/bleu + watermark logo 7% + ligne `horizontalGradient` bleu→vert
- KPI cards : illustration fond plein `ContentScale.Crop`, `alpha(0.14f)`, icône 36dp, `RoundedCornerShape(14dp)`, border `MissaBorder`
- `MissaBusinessDrawer` : `MissaBrandMark` 48dp, version `BuildConfig.VERSION_NAME`, `CompanyLogo` 42dp, sections Administration/Outils/Support, backup status `CloudDone` + `Green90`

## Vérifications finales

- `grep -R Color\(0x --include=*.kt | grep -v Color.kt` → 0 ✅
- `grep -rn 'Text(text = "' --include=*.kt | grep -v stringResource` → 3 (`+`, `—`, `0`) acceptables ✅
- `grep -R runBlocking|GlobalScope` → 0 (hors `withContext(IO)` légitime) ✅
- `grep GREEN FARM` → 0 ✅
- `grep projetsEnRetard.*= 0` → 0, remplacé par `count { ProjetCodec.decode }` ✅
- `grep -n "!!" --include=*.kt` → 5 (productId!! grouping + wizardClientId!! check) acceptables ✅
- CI : 35048383232 vert (header) → 35051321575 vert (payload fix) → 35058751285 vert (theming) → 35059079180 vert (HomeRepository) → v3 en cours

## Fichiers modifiés cumulés

- `MainActivity.kt` — lifecycleScope
- `SettingsStore.kt` — longPreferencesKey
- `HomeViewModel.kt` — projetsEnRetard réel + HomeRepository + flowOn
- `HomeRepository.kt` — nouveau, agrège DAO
- `AppNavHost.kt` — reset barreDemandee
- `HomeScreen.kt` — i18n + GREEN FARM supprimé + theming 0 Color(0x)
- `AdminReglagesScreen.kt` — theming Blue90/Green90/TendrePositive
- `ClientFlowScreen.kt` — Red80
- `values*/strings.xml` — 9 nouvelles clés x5 locales
- `drawable/illustration_*.webp` x14 — WebP 512px
- `docs/CHARTE_VISUELLE_3D.md` + `docs/REVUE_COMPLETE.md`

## Recommandations futures (non bloquantes)

1. Nettoyer 891 clés mortes par lot (script + tests)
2. Ajouter tests unitaires `HomeViewModel` (projets en retard, ruptures, valeur stock) + UI tests `HomeScreen`
3. Audit accessibilité `contentDescription` + `semantics`
4. Chiffrer backup `.db` (SQLCipher) + PIN biometrique optionnel
5. Deep links notifications + `AppModule` routes
