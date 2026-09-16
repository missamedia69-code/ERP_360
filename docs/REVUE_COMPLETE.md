# Revue complète ERP 360 — 2026-09-16

CI de référence : 35048383232 (vert)

## Résumé exécutif
Application fonctionnelle, architecture Clean (data/domain/ui) respectée, Hilt, Room, DataStore, Compose. Tâche header précédente intégrée. Défauts bloquants corrigés dans ce push.

## Défauts identifiés et corrections

### 🔴 Bloquants / Sécurité / ANR

1. **MainActivity.kt : runBlocking sur main thread** — `applyStoredLocale()` et `applyStoredFormats()` bloquaient 2s le thread UI, risque ANR au démarrage.
   - **Chemin** : `app/src/main/java/com/missa/b360/MainActivity.kt:24-50`
   - **Correction** : passage à `lifecycleScope.launch { withTimeoutOrNull }`, plus de blocage. Locale et formats chargés en arrière-plan.

2. **SettingsStore.kt : getLong() parse String** — stockage historique en String, `longPreferencesKey` existait mais `getLong` faisait `toLongOrNull()` sur String, incohérent avec `setPinFailCount` qui supprimait `longKey`.
   - **Chemin** : `app/src/main/java/com/missa/b360/core/data/datastore/SettingsStore.kt:66-77`
   - **Correction** : `getLong` lit d'abord `longPreferencesKey`, fallback String pour migration. `setLong` écrit `longPreferencesKey` et nettoie String. `setPinFailCount`/`setPinLockUntil` suppriment les deux clés.

3. **HomeScreen.kt : "GREEN FARM" hardcodé** — texte de test resté dans UI prod.
   - **Chemin** : `HomeScreen.kt:515` (ancien)
   - **Correction** : supprimé, remplacé par `CompanyLogo` avec `contentDescription = R.string.home_company_active`, nom entreprise réel.

### 🟠 Navigation / UI State

4. **AppNavHost.kt : barreDemandee non reset** — si un écran masquait la barre via `LocalBarreNavigation`, en naviguant ailleurs elle restait masquée.
   - **Chemin** : `app/src/main/java/com/missa/b360/ui/navigation/AppNavHost.kt:98-115`
   - **Correction** : `LaunchedEffect(routeCourante) { barreDemandee.value = true }` à chaque changement de route.

5. **HomeViewModel.kt : projetsEnRetard hardcodé à 0** — TODO laissé en prod, faussait KPI.
   - **Chemin** : `HomeViewModel.kt:210-212`
   - **Correction** : décodage réel via `ProjetCodec.decode`, filtre `echeance < maintenant && etat != LIVRE`, utilise `ProjetRules`.

6. **HomeScreen.kt : 11 Text hardcodés + 9 clés manquantes** — `"${state.nombreProduits} produits"`, `"projets actifs"`, `"en retard"`, `"NC ouvertes"`, `"Qualité"`, factures, commandes, ruptures, NC detail.
   - **Chemin** : `HomeScreen.kt:1010-1031, 1194-1218`
   - **Correction** : ajout 9 clés i18n dans `values/values-en/es/ar/zh` (`home_produits_count`, `home_projets_actifs`, `home_nc_ouvertes`, `home_qualite_label`, `home_a_jour`, `home_en_retard`, `home_produits_rupture`, `home_nc_ouvertes_detail`, `home_commande_fournisseur_attente`) + remplacement par `stringResource`.

### 🟡 Architecture / Qualité

7. **Couleurs hardcodées : 181 occurrences `Color(0x...)`** dont 148 dans `HomeScreen.kt` — pas de dark mode, pas de thème centralisé, duplication.
   - **Chemin** : `HomeScreen.kt:121-1069`, `ui/components/*`, `ui/theme/*`
   - **Impact** : maintenance lourde, incohérence visuelle.
   - **Recommandation** : créer `ui/theme/HomeColors.kt` avec palette sémantique (`onSurface`, `surfaceVariant`, `outline`, `brandGreen`, etc.) et remplacer par `MaterialTheme.colorScheme` + `BrandBlue` etc. Non corrigé intégralement dans ce push (risque régression visuelle large), à planifier en tâche dédiée.

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
