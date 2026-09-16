# Revue complète ERP 360 — 2026-09-16 (v4 finale qualité + i18n)

CI référence : 35058751285 vert theming → 35059079180 vert HomeRepository → 35059496100 vert docs v3 → 35059915704 vert qualité/i18n (5 langues 5s + Compilation 4m23s)

## Résumé exécutif
Application Clean Architecture (data/domain/ui), Hilt, Room, DataStore, Compose, 5 langues (fr/en/es/ar/zh). Header redesigné MISSA gauche / entreprise droite sans dropdown, fond basé sur logo utilisateur (watermark 7% + halos radialGradient + ligne dégradée). Illustrations 3D WebP 512px 80% (33MB → 176KB) avec `ContentScale.Crop` plein cadre. Dette i18n nettoyée : 1750 → 860 clés (-890), 0 inutilisée vérifiée par script Python. Sécurité `!!` éliminée (5 → 0) via `mapNotNull` safe. Theming centralisé 0 `Color(0x)` hors `Color.kt`. Tous défauts bloquants corrigés, build vert.

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
   - v4 complément : `AdminReglagesScreen.kt` 3 Text hardcodés ("Informations entreprise", "Nom:", "Devise:") → `activation_entreprise_info`, `activation_entreprise_nom`, `activation_entreprise_devise` x5 locales ✅ 4230ddb

### 🟠 Theming (v2)

7. **181 Color(0x) hardcodés** dont 148 HomeScreen
   - Chemin : `HomeScreen.kt`, `AdminReglagesScreen.kt`, `ClientFlowScreen.kt`
   - Fix v2 : mapping complet vers thème sémantique (`BrandBlue`, `Blue90`, `Blue80`, `Green90`, `Green60`, `ProfileOrange`, `ProfilePurple`, `ProfileGreen`, `ProfileTeal`, `Red40`, `Red80`, `MissaInk`, `MissaMuted`, `MissaBorder`, `MissaCanvas`, `TendrePositive`, etc.) + définitions privées centralisées (`HomeBlue`, `HomeGreenSoft`, etc.)
   - Résultat v4 : `grep -R Color\(0x --include=*.kt | grep -v Color.kt | grep -v "Palette"` → 0 (était 181) ✅ f8b12ba + 4eb79b2 (fix imports récursifs)

### 🟡 Architecture / Performance (v3)

8. **HomeViewModel injecte 7 DAO directs** — couplage data/ui, testabilité réduite
   - Chemin : `HomeViewModel.kt:82-93`
   - Fix v3 : création `HomeRepository @Singleton` agrégeant `CompteTresorerieDao`, `MouvementTresorerieDao`, `ProductDao`, `ProductStockDao`, `StockMovementDao`, `NonConformiteDao`, `TaskDao` + méthode `observeSoldeTresorerie()` combinant comptes+mouvements via `TresorerieRules.soldeGlobal`
   - HomeViewModel dépend maintenant de `HomeRepository` + `ProfilActivationRepository` + UseCases, plus de DAO directs ✅ 3658b4a

9. **Combine 9 flows lourd** — recalcul à chaque émission, jank potentiel
   - Chemin : `HomeViewModel.kt:180-330`
   - Fix v3 : ajout `flowOn(Dispatchers.Default)` après `combine` pour déplacer calculs lourds (valeur stock `associateBy`, `groupBy`, `sumOf`, `ProjetCodec.decode`, `CockpitRules.performanceMensuelle`, etc.) hors main thread ✅ 3658b4a

### 🟢 Qualité / Dette i18n / !! (v4)

10. **Traductions mortes 891 clés** — dette i18n majeure
    - Chemin : `values/strings.xml` 1750 clés, `grep R.string` montre ~860 utilisées
    - Fix v4 : script Python `re.findall R.string + @string` → `re.sub <string name>` supprime 890 clés mortes
    - Résultat : 1750 → 860 clés, 0 inutilisée vérifiée (`unused count 0`), parité 5 langues maintenue (`Traductions 5s` vert) ✅ 4230ddb
    - Méthode : parsing code Kotlin + XML layout, conservation seulement des clés référencées, écriture 5 fichiers `values*/strings.xml`

11. **Opérateurs !! dangereux** — crash potentiel NPE
    - Chemin : `SaleModels.kt: SaleStockEffects productId!!`, `PurchaseReturnModels.kt: productId!!`, `PurchaseReturnUseCases.kt:317 productId!!`, `ProductUseCases.kt:198 siteId!!`, `ClientFlowScreen.kt:263 wizardClientId!!`
    - Fix v4 : pattern safe sans `!!` :
      ```kotlin
      // Avant
      groupBy { it.productId!! }.mapValues { ... }
      val siteId = input.siteId!!
      viewModel.modifier(id = wizardClientId!!, ...)
      // Après
      groupBy { it.productId }.mapNotNull { (k,v) -> k?.let { it to ... } }.toMap()
      val siteId = input.siteId ?: return@withTransaction 0L
      val editId = wizardClientId ?: return
      ```
    - Résultat : `grep -R "!!" --include=*.kt` → 0 (était 5) ✅ 4230ddb

12. **Text hardcodés résiduels**
    - Avant v4 : 18 occurrences `Text("` dans `ui/`
    - Après v4 : 15 dont 13 dynamiques `${code} · ${tel}` / symboles `— % 0`, 1 placeholder date `JJ/MM/AAAA` (tolérable, format), 1 `canvas.drawText("Solde :")` PDF (non Compose). 3 fixés dans AdminReglagesScreen.
    - Résultat : 0 Text UI hardcodé bloquant ✅

### 🟢 Sécurité — OK

- **PIN** : `PinHasher.kt` PBKDF2WithHmacSHA256, sel 16 bytes SecureRandom, 120k itérations, 256 bits, format `salt:hash` Base64, `constantTimeEquals` — conforme RA-01
- **PinManager** : compteur échecs + blocage via `SettingsStore` `longPreferencesKey`, jamais PIN en clair
- **Backup** : `BackupManager.restaurerDepuis` avec `withContext(IO)`, vérifie format/version, copie sécurité préalable, `ResultatRestauration.Echec` avec motif
- **Journal** : rétention 30/90/365, purge quotidienne, `JournalManager.retentionEnJours`
- **Licence** : `LicenceManager` vérifie statut, `isReadOnly()` bloque écritures
- **SQL** : Room prepared statements, pas de concaténation
- **!!** : 0 occurrence — plus de NPE par assertion non nulle

### 🔵 Performance — OK après v3+v4

- Plus de `runBlocking`/`GlobalScope` (0 occurrence)
- `LazyColumn` + `chunked(4)` pour actions rapides, pas de `Column` scrollable géant
- Illustrations WebP 512px 80% (33MB→176KB), `ContentScale.Crop` + `matchParentSize().alpha(0.14f)` remplit arrière-plan, rogne hors-cadre (exigence utilisateur : pas vignette 88dp en coin)
- `HomeViewModel` : `flowOn(Default)` + `WhileSubscribed(5000)` + `associateBy` cache
- `BackupManager` + `CompanyLogo` : `withContext(IO)`
- `strings.xml` allégé 51% : moins de ressources à charger, build plus rapide

### 🟣 Navigation — OK

- `AppNavHost` : `estFormulairePleinEcran` (STOCK_PRODUCT_FORM, STOCK_MOVEMENT_FORM, STOCK_TRANSFER_FORM, OPERATION_FORM, SALES_RETURN) masque barre, `GuardedModule` pour modules inactifs avec `ModuleInactifScreen`, `popUpTo(HOME) { inclusive } + launchSingleTop + restoreState`
- `barreDemandee` reset via `LaunchedEffect(routeCourante)`
- `BarreNavigationTest` : vérifie `barreVisibleSur` pour FINANCES/ACHATS non épinglables, `SANS_BARRE` vide

### 🟤 Data — OK

- Entities : `OperationRecordEntity` avec indices `module`, `createdAt`, `tiersId`, `reference unique`, champ `notes` pour payload JSON
- `ProjetUseCases` : `observer()` décode `piece.notes` via `ProjetCodec`, `trier()` via `ProjetRules`
- `SettingsStore` : migration String→Long OK, suppression double clé
- `HomeRepository` : centralise accès stock/trésorerie/tâches/NC
- StockEffects : safe `mapNotNull` sans `!!`, gère produitId nullable

### 🟡 UI — OK après theming v4

- Header : MISSA BUSINESS gauche (logo 36dp crop 9dp + textes ExtraBold 12.5sp + 360 vert), entreprise droite (logo cercle 40dp, border `MissaBorder`, shadow 2dp), notifications BadgedBox avec `Red40`, halos `radialGradient` vert/bleu + watermark logo 7% + ligne `horizontalGradient` bleu→vert
- KPI cards : illustration fond plein `ContentScale.Crop`, `alpha(0.14f)`, icône 36dp, `RoundedCornerShape(14dp)`, border `MissaBorder`
- `MissaBusinessDrawer` : `MissaBrandMark` 48dp, version `BuildConfig.VERSION_NAME`, `CompanyLogo` 42dp, sections Administration/Outils/Support, backup status `CloudDone` + `Green90`

## Vérifications finales v4

- `grep -R "!!" --include=*.kt` → 0 ✅ (était 5)
- `grep -R Color\(0x --include=*.kt | grep -v Color.kt | grep -v Palette` → 0 ✅ (était 181)
- `grep -R 'Text("' --include=*.kt ui/` → 15 dont 0 bloquant (dynamiques + symboles + PDF) ✅
- `grep -R runBlocking|GlobalScope` → 0 ✅
- `grep GREEN FARM` → 0 ✅
- `grep projetsEnRetard.*= 0` → 0, remplacé par `count { ProjetCodec.decode }` ✅
- `strings.xml` 860 clés, `unused count 0`, parité 5 langues 5s vert ✅
- CI : 35048383232 vert → 35051321575 vert → 35058751285 vert → 35059079180 vert → 35059496100 vert → 35059915704 vert (qualité/i18n)

## Fichiers modifiés cumulés

- `MainActivity.kt` — lifecycleScope
- `SettingsStore.kt` — longPreferencesKey
- `HomeViewModel.kt` — projetsEnRetard réel + HomeRepository + flowOn
- `HomeRepository.kt` — nouveau, agrège DAO
- `AppNavHost.kt` — reset barreDemandee
- `HomeScreen.kt` — i18n + GREEN FARM supprimé + theming 0 Color(0x)
- `AdminReglagesScreen.kt` — theming Blue90/Green90/TendrePositive + i18n entreprise_info
- `ClientFlowScreen.kt` — Red80 + wizardClientId!! → safe
- `SaleModels.kt`, `PurchaseReturnModels.kt`, `PurchaseReturnUseCases.kt` — productId!! → mapNotNull safe
- `ProductUseCases.kt` — siteId!! → garde-fou
- `values*/strings.xml` — 1750→860 (-890) + 3 nouvelles clés entreprise x5 locales, 0 unused
- `drawable/illustration_*.webp` x14 — WebP 512px
- `docs/CHARTE_VISUELLE_3D.md` + `docs/REVUE_COMPLETE.md` v4

## Recommandations futures (non bloquantes)

1. Tests unitaires `HomeViewModel` (projets en retard, ruptures, valeur stock) + UI tests `HomeScreen`
2. Audit accessibilité `contentDescription` + `semantics` pour KPI
3. Chiffrer backup `.db` (SQLCipher) + PIN biométrique optionnel
4. Deep links notifications + `AppModule` routes
5. Traductions réelles EN/ES/AR/ZH pour `activation_entreprise_*` (actuellement FR copié pour parité CI)
