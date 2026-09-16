# Missa Business 360 — ERP mobile offline-first

<p>
  <img alt="Plateforme" src="https://img.shields.io/badge/plateforme-Android%208.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Licence" src="https://img.shields.io/badge/licence-Apache%202.0-blue">
  <!-- Dernière exécution verte vérifiée sur cette branche : 35089341325 (commit 056f11e). -->
  <img alt="Build" src="https://github.com/missamedia69-code/ERP_360/actions/workflows/android.yml/badge.svg?branch=arena/01a0a9d0-erp-360">
</p>

**Missa Business 360** (`com.missa.b360`) est un **ERP complet et natif pour Android**, pensé
pour les TPE/PMI : il fonctionne **100 % hors-ligne**, couvre **14 modules métier et support**,
parle **5 langues** (FR · EN · ES · AR-RTL · ZH) et gère le **multi-site** avec des **profils
d'activité**. Implémentation du cahier de charge **E9**.

> **Offline-first** : aucune donnée ne quitte le téléphone. L'application démarre
> **sans aucune donnée d'exemple** — tout est créé par l'utilisateur au fil de l'onboarding.
>
> **Sauvegarde Google exclue** : la base Room, ses annexes WAL/SHM, les réglages
> DataStore (dont l'empreinte du PIN) et les copies locales du dossier `backups/`
> ne sont pas téléversés (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
> Le transfert direct d'un téléphone à l'autre reste autorisé, et la restauration
> manuelle passe par **Admin › Sauvegarde**.

> **Branche de travail actuelle : `arena/01a0a9d0-erp-360` — version *Accueil seul*.** Créée depuis
> `390d9eb`, pointe de `arena/01a0a160-erp-360` sur `origin` (qui ne porte plus que cette branche et
> `main`). Le socle provient des itérations précédentes — `arena/01a0a0db-erp-360`, puis fusion de
> `arena/01a0773a-erp-360` : schéma Room 12, règles de validation des modules/profils, PIN à
> 4 chiffres — branches aujourd'hui supprimées d'`origin`. L'accueil est la référence visuelle
> figée ; tous les autres modules affichent un placeholder cohérent et seront reconstruits un par
> un dans la même charte.

---

## 🧩 Modules — état au 14/09/2026

| Module | Barre | État | Description |
|---|:---:|:---:|---|
| **Accueil** | 🏠 | ✅ **Réel** | Tableau de bord (4 KPI + 8 actions + résumé + activités) — référence `HomeScreen.kt` + logo `GREEN FARM` |
| **Vente** | ✅ | ⏳ Placeholder | Devis → commande → facture (prévu bloc `ACH-STK-VEN`) |
| **Stock** | ✅ | ⏳ Placeholder | Produits, **groupes d'articles transverse**, mouvements, transferts, inventaires — hub à reconstruire en 1er |
| **Clients** | ✅ | ⏳ Placeholder | Fiches NIF, contacts multiples |
| **Finances** | ✅ | ⏳ Placeholder | Encaissements/dépenses |
| **Achats** | ➕ | ⏳ Placeholder | *1er bloc* avec Stock — `ACH→STK` obligatoire |
| **Fournisseurs** | ➕ | ⏳ Placeholder | Référentiel fournisseurs |
| **Livraison / Logistique / Production / Services / RH / Projets / Trésorerie / Comptabilité / CRM / Qualité / Maintenance / Reporting** | ➕ | ⏳ Placeholder | Placeholder `MissaCanvas + MissaTopAppBar + MissaEmptyState` — reconstruction progressive |
| **9.1 Administration** | ☰ | ⏳ Placeholder | Réglages, licence, sauvegarde, journal, utilisateurs, multi-site |

Navigation **RA-22** : menu ☰, cloche 🔔, barre du bas à **5 emplacements** — Accueil, **3 modules
du profil** (épinglables ; ordre d'usine `VEN → ACH → STK → TRE → CPT → PRO → SER → PRJ → LOG → CRM →
RH → QUA → MAI → REP`, plafonné par `AppModule.MAX_ONGLETS = 3`) et **Plus** — plus le bouton **➕**.

---

## 🔀 Flux métier — règle d'or

> **Tu achètes ou tu produis → t'as forcément un stock.**

* `ACH → STK` et `PRO → STK` ajoutés automatiquement par `ModulesSocle.avecDependances()`
* **1er bloc : `A-S-V`** (négoce classique) — profil `ASV` en tête
* **2e bloc : `A-P-S-V`** (industrie) — profil `APSV` en 2e
* `AV (A+V sans stock)` dépriorisé (legacy)
* **Option `Vente sans stock`** (`SettingsStore.VENTE_SANS_STOCK`, `false` par défaut) : autorise `VEN` sans `STK` pour drop / achat à la commande / service. Toggle `Switch` dans l'onboarding (`OnbProfil`). Quand `OFF`, `VEN + ACH/PRO` sans `STK` → `STK` auto-ajouté. Stock négatif autorisé seulement si option `ON`.
* Les règles ci-dessus sont **déclarées** dans `DependancesModules` (`ModuleDependency`,
  condition `SAUF_SI_VENTE_SANS_STOCK`) et **contrôlées** par `ValidationProfil.validateProfileConfig()`
  (spec §7.2/§9) : toute configuration modifiable (profil CUSTOM notamment) qui violerait
  `ACH→STK` ou `PRO→STK` est bloquée. Le profil AV (legacy « Achat-Vente sans stock ») est un
  profil figé maintenu pour rétro-compatibilité et **non soumis** à cette validation — il est
  recommandé de migrer vers ASV.

---

## 🌍 Internationalisation

5 langues intégrales, y compris **arabe RTL** : Français (défaut) · English · Español · العربية · 中文. Changement à chaud (per-app language). `python3 .github/scripts/verifier_traductions.py` garantit la parité des cinq `strings.xml` — **866 clés** chacune au 16/09/2026 (clé absente, clé surnuméraire, doublon, apostrophe non échappée ou paramètre `%1$s` divergent font échouer la CI).

---

## 🛠️ Stack technique

| Couche | Choix |
|---|---|
| Langage / build | **Kotlin 2.3** · AGP 9.4 · Gradle Kotlin DSL (version catalog) |
| UI | **Jetpack Compose** + **Material 3** (BOM 2025.09) — `MissaDesign` (`MissaCanvas` `#F8F9FD`, `MissaPanel` 14dp/bord `#CBD5E8`, `MissaTopAppBar` blanche) |
| Architecture | **MVVM + Clean** : `ui/` → `domain/usecase/` → `data/` |
| Persistance | **Room 2.8 (KSP)** — 40 entités, base **v12**, migrations 1→12 |
| Réglages | **DataStore** + verrous d'amont + `VENTE_SANS_STOCK` |
| Injection | **Hilt 2.60** |
| Tâches de fond | **WorkManager** — purge du journal selon la rétention choisie (30 j / 90 j / 12 mois, défaut **12 mois**) |
| Sécurité | PIN PBKDF2 (PBKDF2WithHmacSHA256, 120 000 itérations, sel 128 bits), aucune dépendance de chiffrement tierce · sauvegarde Google exclue (base, réglages, copies locales) |
| Cible | minSdk **26** · targetSdk **36** |

---

## 🏗️ Structure du code

```
app/src/main/java/com/missa/b360/
├── core/data/          # db (Room v12) · dao · entity (40 @Entity) · datastore (+ VENTE_SANS_STOCK) · repository
├── core/domain/model/  # ModulesSocle (avecDependances), Configuration (14 modules, 7 profils),
│                       #   DependancesModules/ValidationProfil (règle d'or + validation),
│                       #   ReglesGroupesArticles (groupes transverses), OptionsProfil
├── ui/onboarding/      # langue → profil (ASV/APSV en tête) → entreprise → PIN (+ PinLockScreen)
├── ui/home/            # HomeScreen.kt — référence visuelle
├── ui/components/      # MissaDesign · MissaAppScaffold (+ MissaAppHeader) · MissaBarreModules ·
│                       #   PlaceholderScreen (Scaffold + MissaTopAppBar + MissaEmptyState)
├── ui/clients/         # ⏳ ClientsPlaceholderScreen câblé ; le parcours réel (ClientFlowScreen.kt,
│                       #   1431 l., composable ClientsScreen) est conservé mais non câblé
├── ui/stock|sales|purchases|...  # ⏳ écrans réduits à un stub PlaceholderScreen — ViewModels métier déjà réels
└── ui/navigation/      # ModuleRegistry (18 écrans / 14 modules, barre MAX_ONGLETS = 3) + AppNavHost
```

---

## 📐 Architecture — modules, écrans, profils et groupes d'articles

Complément de la doc `docs/fiscalite-multizones.md`. Décrit l'architecture des modules, les
écrans UI, les dépendances entre modules, les profils utilisateurs et le rôle **transverse
des groupes d'articles du module Stock**.

### Vue d'ensemble
* **6 modules Métier** (ACH, STK, PRO, VEN, SER, PRJ) + **8 modules Support** (CPT, TRE, LOG,
  REP, CRM, RH, QUA, MAI) — codes dans `core/domain/model/Configuration.kt` (`ModuleCode`).
* **18 écrans UI** enregistrés dans `ModuleRegistry.kt` (`AppModule`, équivalent `UiScreen` de
  la spec) : VENTE, STOCK, CLIENTS, FINANCES, ACHATS, FOURNISSEURS, LIVRAISON, LOGISTIQUE,
  PRODUCTION, SERVICES, PROJETS, COMPTABILITE, TRESORERIE, CRM, QUALITE, MAINTENANCE, REPORTING, RH.
* **7 profils** (`ProfilActivite`, équivalent `ProfileCode`) : AV, ASV, APSV, SER, PRJ, FULL, CUSTOM.
* **1 module central Stock** avec un système de groupes d'articles utilisé par tous les modules.

### Règles de dépendance (règle d'or)
Déclarées dans `DependancesModules` (spec §9 `ModuleDependency`, `moduleDependencies`) :

| Dépendance | Condition |
|---|---|
| ACH → STK | **Toujours** : tout achat alimente un stock (même « non stocké », via le référentiel) |
| PRO → STK | **Toujours** : la production consomme et produit du stock |
| VEN → STK | **Par défaut** ; tombe si option `vente_sans_stock` = `ON` (drop, services, produits digitaux) |

Ces règles sont **appliquées** automatiquement par `ModulesSocle.avecDependances()` dans le
pack, et **vérifiées** par `ValidationProfil.validateProfileConfig(modules, options)` (spec
§7.2) : toute violation **bloque l'enregistrement** de la configuration avec un message clair.
Exception : le profil **AV** (legacy, figé, à migrer vers ASV).

### Groupes d'articles — mécanisme transverse (spec §5.2, §8)
Le module Stock porte le référentiel `item_groups` + 6 tables d'extension (stock, achat, vente,
production, maintenance, comptabilité) ; `GroupeArticleDao.GroupeArticleComplet` les assemble.
Règles exposées par `ReglesGroupesArticles` (le groupe prime ; les fiches sans groupe retombent
sur `ProductType` via `ProduitRules`) :

| Module | Ce qu'il lit sur le groupe |
|---|---|
| ACH | `achetable`, comptes de charge / immobilisation |
| VEN | `vendable`, compte de produit |
| PRO | `produisible`, nomenclature requise |
| MAI | `equipementMaintenable` |
| STK | `stocke`, méthode et comptes de stock |
| CPT | `valorise`, comptes de stock/charge/produit |
| SER | familles de type prestation (`service`) |
| REP | agrégations par groupe (valeur, CA, marges) |
| LOG | familles stockées (expéditions) |

### Validation et options de profil (spec §7)
* `OptionsConfigProfil` — options par profil (`vente_sans_stock`, extensible) ; clés dans `OptionsProfil`.
* `ViolationProfil` — violations typées (`ACH_SANS_STOCK`, `PRO_SANS_STOCK`, `VEN_SANS_STOCK`)
  avec message prêt à l'affichage.

---

## 📈 Avancement

| Phase | Contenu | État |
|---|---|---|
| **A — Socle** | Gradle, Hilt, Room, DataStore, PIN, Licence, Journal, Séquences, Nav | ✅ |
| **B — Onboarding** | langue → profil ASV/APSV → entreprise → PIN → licence | ✅ (flux ACH→STK + vente sans stock) |
| **C — Accueil** | `HomeScreen` à l'identique maquette + barre 5 items + logo GREEN FARM | ✅ |
| **D — Placeholders** | 36 écrans en placeholder cohérent pour repartir propre | ✅ (actuel) |
| **E — Stock** | Hub Stock (valeur, alertes, actions) | ⏳ prochain |
| **F — Achats** | Hub Achats (fournisseurs, commandes, réceptions) | ⏳ |
| **G — Ventes** | Hub Ventes (devis, commandes, retours) + `venteSansStock` | ⏳ |
| **H+** | Production, Services, Projets, Finances... | ⏳ |

---

## 🚀 Build & lancement

Prérequis : **Android Studio Quail 3 | 2026.1.3+** (AGP 9.4) et JDK 21.

```bash
git clone -b arena/01a0a9d0-erp-360 https://github.com/missamedia69-code/ERP_360.git
./gradlew assembleDebug
./gradlew testDebugUnitTest
python3 .github/scripts/verifier_traductions.py
```

> Après `git clean -fdx`, recrée `local.properties` :
> `sdk.dir=<chemin de ton SDK Android>` — par exemple `D:/Android_Studio` sous Windows
> ou `~/Android/Sdk` sous Linux/macOS.

---

## 🔁 Intégration continue

`.github/workflows/android.yml` se déclenche sur toute poussée vers `main` ou `arena/**`, sur toute
pull request et en manuel (`workflow_dispatch`) ; une poussée successive annule l'exécution
précédente de la même branche (`concurrency`). Chaque exécution enchaîne :

| Étape | Ce qu'elle garantit |
|---|---|
| **Traductions** | 5 `strings.xml` parité parfaite |
| **Compilation et tests** | `assembleDebug` + `testDebugUnitTest` (JDK 21, SDK 36) |

Dernière exécution verte vérifiée : **35079663745** (branche `arena/01a0a160-erp-360`, commit
`390d9eb`, 3 min 27 s).

APK : onglet *Actions* → exécution → *erp360-debug-apk*.

---

*README tenu à jour à chaque boucle `Plan → Applique → Teste → Corrige`.*
