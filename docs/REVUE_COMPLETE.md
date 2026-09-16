# Revue complète ERP 360 — 2026-09-16 (mise à jour 2)

CI de référence : 35051321575 (vert) → nouveau CI en cours pour theming

## Résumé exécutif
Application fonctionnelle, architecture Clean respectée (Hilt, Room, DataStore, Compose). Header redesigné (MISSA gauche / entreprise droite, fond basé sur logo utilisateur, watermark + halos, sans dropdown). Illustrations 3D WebP 512px (33MB → 176KB) avec `ContentScale.Crop` plein cadre. Défauts bloquants corrigés et poussés.

## Défauts identifiés et corrections — v1 (déjà poussé)

### 🔴 Bloquants / Sécurité / ANR

1. **MainActivity.kt : runBlocking sur main thread** — `applyStoredLocale()` et `applyStoredFormats()` bloquaient 2s le thread UI, risque ANR au démarrage.
   - **Chemin** : `app/src/main/java/com/missa/b360/MainActivity.kt:24-50`
   - **Correction** : passage à `lifecycleScope.launch { withTimeoutOrNull }`, plus de blocage. Locale et formats chargés en arrière-plan. ✅ Poussé aef30c1

2. **SettingsStore.kt : getLong() parse String** — stockage historique en String, `longPreferencesKey` existait mais `getLong` faisait `toLongOrNull()` sur String, incohérent avec `setPinFailCount` qui supprimait `longKey`.
   - **Chemin** : `app/src/main/java/com/missa/b360/core/data/datastore/SettingsStore.kt:66-77`
   - **Correction** : `getLong` lit d'abord `longPreferencesKey`, fallback String pour migration. `setLong` écrit `longPreferencesKey` et nettoie String. `setPinFailCount`/`setPinLockUntil` suppriment les deux clés. ✅ Poussé aef30c1

3. **HomeScreen.kt : "GREEN FARM" hardcodé** — texte de test resté dans UI prod.
   - **Chemin** : `HomeScreen.kt:515` (ancien)
   - **Correction** : supprimé, remplacé par `CompanyLogo` avec `contentDescription = R.string.home_company_active`, nom entreprise réel. ✅ Poussé 0414e9e

### 🟠 Navigation / UI State

4. **AppNavHost.kt : barreDemandee non reset** — si un écran masquait la barre via `LocalBarreNavigation`, en naviguant ailleurs elle restait masquée.
   - **Chemin** : `app/src/main/java/com/missa/b360/ui/navigation/AppNavHost.kt:98-115`
   - **Correction** : `LaunchedEffect(routeCourante) { barreDemandee.value = true }` à chaque changement de route. ✅ Poussé aef30c1

5. **HomeViewModel.kt : projetsEnRetard hardcodé à 0** — TODO laissé en prod, faussait KPI.
   - **Chemin** : `HomeViewModel.kt:210-212`
   - **Correction** : décodage réel via `ProjetCodec.decode(rec.notes)` (champ Room = `notes`, pas `payload`), filtre `echeance < maintenant && etat != LIVRE`, utilise `ProjetRules`. Fix compilation `payload` → `notes`. ✅ Poussé 5baf1eb

6. **HomeScreen.kt : 11 Text hardcodés + 9 clés manquantes** — `"${state.nombreProduits} produits"`, `"projets actifs"`, `"en retard"`, `"NC ouvertes"`, `"Qualité"`, factures, commandes, ruptures, NC detail.
   - **Chemin** : `HomeScreen.kt:1010-1031, 1194-1218`
   - **Correction** : ajout 9 clés i18n dans `values/values-en/es/ar/zh` (`home_produits_count`, `home_projets_actifs`, `home_nc_ouvertes`, `home_qualite_label`, `home_a_jour`, `home_en_retard`, `home_produits_rupture`, `home_nc_ouvertes_detail`, `home_commande_fournisseur_attente`) + remplacement par `stringResource`. ✅ Poussé aef30c1

### 🟡 Architecture / Qualité — v2 (nouveau push)

7. **Couleurs hardcodées : 181 occurrences `Color(0x...)` dont 148 dans `HomeScreen.kt`** — pas de dark mode, pas de thème centralisé, duplication.
   - **Chemin** : `HomeScreen.kt:121-1069`, `ui/components/*`, `ui/theme/*`
   - **Impact** : maintenance lourde, incohérence visuelle.
   - **Correction v2** : script de theming — mapping de tous les hex vers variables sémantiques du thème :
     - `0xFFEAF8EF`→`HomeGreenSoft`/`Green90`, `0xFFF28A16`→`HomeOrange`/`ProfileOrange`, `0xFFFFF1DF`→`HomeOrangeSoft`, `0xFF7047E8`→`HomePurple`/`ProfilePurple`, `0xFF4BAE27`→`MarqueVert`/`ProfileGreen`, `0xFFF1ECFF`→`HomePurpleSoft`, `0xFF00A5A5`→`HomeTeal`/`ProfileTeal`, `0xFF2563EB`→`HomeBlue`/`BrandBlue`, `0xFFEFF6FF`→`HomeBlueSoft`/`Blue90`, `0xFF16A34A`→`TendrePositive`, `0xFFECFDF5`→`Green90`, `0xFF7C3AED`→`ProfilePurple`, `0xFFF5F3FF`→`HomePurpleSoft`, `0xFFF59E0B`→`ProfileOrange`, `0xFFFFF7ED`→`HomeOrangeSoft`, `0xFF0D9488`→`ProfileTeal`, `0xFFF43F5E`→`HomeRed`/`Red40`, `0xFFFFF1F2`→`Red80`, `0xFFDCFCE7`→`Green90`, `0xFFDBEAFE`→`Blue80`, `0xFFF8FAFC`→`MissaCanvas`, `0xFFF0F9FF`→`Blue90`, `0xFFF0FDF4`→`Green90`, `0xFFE2E8F0`→`HomeBorder`/`MissaBorder`, `0xFF0F172A`→`HomeTextDark`/`MissaInk`, `0xFFEF4444`→`Red40`, `0xFFE6F8EC`→`Green90`, `0xFF3B82F6`→`BrandBlue`, `0xFF334155`→`HomeTextDark`, `0xFFCBD5E1`→`HomeBorder`, `0xFF94A3B8`→`HomeTextMuted`, `0xFF64748B`→`HomeTextMuted`, `0xFF8A94AA`→`HomeTextMuted`, etc.
   - **Résultat** : `grep -R Color\(0x` hors `Color.kt` → **0 occurrence** (était 181). Reste uniquement définitions légitimes dans `Color.kt` (29 couleurs). ✅ Fix poussé dans ce commit
   - **AdminReglagesScreen** : `Color(0xFFEFF6FF)`→`Blue90`, `Color(0xFFECFDF5)`→`Green90`, `Color(0xFF16A34A)`→`TendrePositive`
   - **ClientFlowScreen** : `Color(0xFFFFF9F9)`→`Red80`

8. **HomeViewModel injecte DAO directement** — `ProductDao`, `ProductStockDao`, `StockMovementDao`, `NonConformiteDao`, `TaskDao`, `CompteTresorerieDao`, `MouvementTresorerieDao` dans ViewModel, contourne UseCases/Repository.
   - **Chemin** : `HomeViewModel.kt:82-93`
   - **Impact** : testabilité réduite, couplage data/ui.
   - **Recommandation** : introduire `HomeRepository` agrégeant les flows, ou étendre `OperationUseCases` + `StockUseCases`. Noté, non bloquant pour ce push.

9. **Combine de 9 flows dans uiState** — `baseState + operations + tresorerie + taches + activation + stocks + mouvements + produits + nc` recalculé à chaque émission, potentiel jank.
   - **Chemin** : `HomeViewModel.kt:138-260`
   - **Recommandation** : mettre en cache `stocks.groupBy`, utiliser `SharingStarted.WhileSubscribed`, déjà présent, mais envisager `flowOn(Dispatchers.Default)` pour calculs lourds (valeur stock, ruptures).

10. **Traductions mortes : 891 clés non utilisées** signalées précédemment — dette i18n.
    - **Chemin** : `values/strings.xml` vs usage
    - **Action** : nettoyage futur via script `find_unused_strings.py`, pas dans ce push pour éviter suppression accidentelle.

11. **Accessibilité : contentDescription manquantes / Text hardcodés restants** — `"+"`, `"—"` tolérés, mais icônes sans description dans `AccueilKpiCard`.
    - **Recommandation** : audit `contentDescription = stringResource(...)`.

### 🟢 Sécurité (OK)

- **PIN** : `PinHasher.kt` utilise PBKDF2WithHmacSHA256, sel 128 bits, 120k itérations, `constantTimeEquals` — conforme RA-01.
- **Backup** : restauration vérifie format/version, copie de sécurité préalable.
- **Journal** : rétention 30/90/365, purge quotidienne.

### 🔵 Performance (OK avec réserves)

- Pas de `GlobalScope`, plus de `runBlocking`.
- `LazyColumn` utilisé, pas de `Column` scrollable géant.
- Images illustrations WebP (nouveaux `drawable/illustration_*.webp`) — bien compressées, mais `ContentScale.Crop` doit être appliqué (correction header précédente : remplir arrière-plan, rogner parties invisibles, pas vignette 88dp).

### Navigation

- `AppNavHost` gère `estFormulairePleinEcran` pour masquer barre, `GuardedModule` pour modules inactifs, `popUpTo HOME + launchSingleTop + restoreState` — pattern correct.
- Manque : deep links pour notifications, `NavArgument` avec `defaultValue` OK.

## Vérifications

- `grep -rn 'Text(text = "' --include="*.kt" | grep -v stringResource` → 3 restants (`"+"`, `"—"`, `"0"`) acceptables.
- `grep -R "runBlocking\|GlobalScope"` → 0 après fix.
- `grep "GREEN FARM"` → 0.
- `grep "projetsEnRetard.*= 0"` → supprimé.
- CI attendu vert (compilation + tests).

## Prochaines étapes recommandées

1. Theming complet : extraire 181 `Color(0x)` vers `MissaColors` + `MaterialTheme`.
2. Introduire `HomeRepository` pour découpler DAO.
3. Nettoyer 891 clés mortes i18n.
4. Ajouter `flowOn` et tests UI pour `HomeViewModel` (projets en retard).
5. Audit accessibilité et `ContentScale.Crop` sur illustrations header (déjà demandé).

## Fichiers modifiés dans ce push

- `MainActivity.kt` — lifecycleScope
- `SettingsStore.kt` — longPreferencesKey
- `HomeViewModel.kt` — projetsEnRetard réel
- `AppNavHost.kt` — reset barreDemandee
- `HomeScreen.kt` — i18n + GREEN FARM supprimé
- `values*/strings.xml` — 9 nouvelles clés
- Illustrations WebP + docs/CHARTE_VISUELLE_3D.md (tâche header)
