# Missa Business 360 — ERP mobile offline-first

<p>
  <img alt="Plateforme" src="https://img.shields.io/badge/plateforme-Android%208.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Licence" src="https://img.shields.io/badge/licence-Apache%202.0-blue">
  <img alt="Build" src="https://github.com/missamedia69-code/ERP_360/actions/workflows/android.yml/badge.svg?branch=main">
</p>

**Missa Business 360** (`com.missa.b360`) est un **ERP complet et natif pour Android**, pensé
pour les TPE/PMI : il fonctionne **100 % hors-ligne**, couvre **14 modules métier et support**,
parle **5 langues** (FR · EN · ES · AR-RTL · ZH) et gère le **multi-site** avec des **profils
d'activité**. Implémentation du cahier de charge **E9**.

> **Offline-first** : aucune donnée ne quitte le téléphone. L'application démarre
> **sans aucune donnée d'exemple** — tout est créé par l'utilisateur au fil de l'onboarding
> (après un splash vidéo d'accueil, `res/raw/splash_intro.mp4`, affiché une seule fois).
>
> **Sauvegarde Google exclue** : la base Room, ses annexes WAL/SHM, les réglages
> DataStore (dont l'empreinte du PIN) et les copies locales du dossier `backups/`
> ne sont pas téléversés (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
> Le transfert direct d'un téléphone à l'autre reste autorisé ; la **sauvegarde locale
> automatique** part au démarrage puis chaque jour (purge du journal), et la
> **restauration** se fait depuis la carte « Restaurer une sauvegarde » de la configuration
> d'onboarding (`BackupManager`, plafond de version lu sur la base réellement ouverte).

> **Branche de travail : `arena/01a0d35d-erp-360`** — issue de `main` (`e40dd3f`). Le socle est
> stable (base **v17**, chaîne de migrations 1→17) et la CI est verte : parité des traductions,
> clés `R.string` manquantes, `assembleDebug`, `testDebugUnitTest`, APK publié. Les écrans encore
> en placeholder sont listés explicitement dans le tableau des modules — aucun module n'est
> annoncé terminé à tort.
>
> Le travail inachevé reste hors de la ligne de travail : c'est le cas des écrans **Devis →
> Commande** (spec §20) et **Retours de vente / avoirs** (spec §22), dont les règles métier,
> codecs et tests JVM sont déjà livrés en main, mais dont les écrans sont encore des placeholders.

---

## 🧩 Modules — état au 24/09/2026

| Module | Barre | État | Description |
|---|:---:|:---:|---|
| **Accueil** | 🏠 | ✅ **Réel** | Tableau de bord (4 KPI avec popup courbe 14 j · 8 actions rapides épinglables · résumé · activités) — référence `HomeScreen.kt`, logo entreprise en filigrane (`CompanyLogo`) |
| **Vente** | ✅ | 🟡 **Partiel** | Vente directe + historique + CA réels (`SalesScreen`, 782 l., sélection/création client rapide, contrôle de stock) ; **Devis → commande** (spec §20) et **retours/avoirs** (spec §22) : règles + tests livrés, écrans encore placeholders |
| **Stock** | ✅ | ✅ **Réel** | Hub (`StockAccueilScreen` : valeur, tendances, entrées/sorties 30 j, alertes) + produits, groupes d'articles transverse, mouvements, transferts, inventaires, catégories, équipements, détail — **Phase E livrée** |
| **Clients** | ✅ | ✅ **Réel** | `ClientFlowScreen` (1 434 l.) : fiches NIF, contacts multiples, adresses, fidélité, catégories/prix — `ClientsScreen` câblé dans `AppNavHost` |
| **Finances** | ✅ | 🟡 **Partiel** | Encaissements/dépenses et comptabilité : écrans **Trésorerie** et **Comptabilité** réels ; liste d'opérations générique encore en placeholder |
| **Achats** | ➕ | ✅ **Réel** | `PurchasesScreen` (1 900 l.) : commandes → réceptions → factures → règlements (chaîne `ACH→STK`, contre-passation), reporting — **Phase F livrée** |
| **Fournisseurs** | ➕ | ✅ **Réel** | `FournisseursScreen` (2 622 l.) : cycle `BROUILLON → A_VALIDER → ACTIF → … → ARCHIVE`, contacts, comptes bancaires, documents |
| **Livraison · Logistique · Production · Services · RH · Projets · Comptabilité · Trésorerie · CRM · Qualité · Maintenance · Reporting · Tâches** | ➕ | ✅ **Réels** | Écrans réels (listes + formulaires + `ViewModel` branchés aux UseCases/Room) — profondeur par fonction suivie par `DestinationsFonctions` |
| **Notifications** | 🔔 | ⏳ Placeholder | `NotificationsScreen` (cloche du header, compteur d'activités inclus) |
| **9.1 Administration** | ☰ | 🟡 **Partiel** | **Réglages** (activation de profil, palier, modules personnalisés), **Sites** multi-site, **Référentiels** (moyens de paiement, taxes, unités) réels ; **Licence, Sauvegarde, Journal, Utilisateurs, À propos** = placeholders |

**Honnêteté du catalogue** : `ModuleSousElements` répertorie **132 fonctionnalités** sur les
14 modules ; `DestinationsFonctions` n'en marque **85 comme disponibles** (une fonction n'est
disponible que si elle ouvre un écran qui fonctionne). Les autres s'affichent comme *prévu*.

Navigation **RA-22** : menu ☰, cloche 🔔, barre du bas à **5 emplacements** — Accueil, **3 modules
du profil** (épinglables ; ordre d'usine `VEN → ACH → STK → TRE → CPT → PRO → SER → PRJ → LOG →
CRM → RH → QUA → MAI → REP`, plafondé par `AppModule.MAX_ONGLETS = 3`) et **Plus**.

---

## 🔀 Flux métier — règle d'or

> **Tu achètes ou tu produis → t'as forcément un stock.**

* `ACH → STK` et `PRO → STK` ajoutés automatiquement par `ModulesSocle.avecDependances()`
* **1er bloc : `A-S-V`** (négoce classique) — profil `ASV` en tête
* **2e bloc : `A-P-S-V`** (industrie) — profil `APSV` en 2e
* `AV (A+V sans stock)` dépriorisé (legacy)
* **Option `Vente sans stock`** (`SettingsStore.VENTE_SANS_STOCK`, `false` par défaut) : autorise
  `VEN` sans `STK` pour drop / achat à la commande / service. Toggle `Switch` dans l'onboarding
  (`OnbProfil`) et dans Admin › Réglages. Quand `OFF`, `VEN + ACH/PRO` sans `STK` → `STK`
  auto-ajouté. Stock négatif autorisé seulement si option `ON`.
* Les règles ci-dessus sont **déclarées** dans `DependancesModules` (`ModuleDependency`,
  condition `SAUF_SI_VENTE_SANS_STOCK`) et **contrôlées** par `ValidationProfil.validateProfileConfig()`
  (spec §7.2/§9) : toute configuration modifiable (profil CUSTOM notamment) qui violerait
  `ACH→STK` ou `PRO→STK` est bloquée. Le profil AV (legacy « Achat-Vente sans stock ») est un
  profil figé maintenu pour rétro-compatibilité et **non soumis** à cette validation — il est
  recommandé de migrer vers ASV.

---

## 🌍 Internationalisation

5 langues intégrales, y compris **arabe RTL** : Français (défaut) · English · Español · العربية · 中文.
Changement à chaud (per-app language). Deux garde-fous en CI :

* `python3 .github/scripts/verifier_traductions.py` — parité des cinq `strings.xml` : **1 622 clés
  chacune** au 24/09/2026 (clé absente, clé surnuméraire, doublon, apostrophe non échappée ou
  paramètre `%1$s` divergent font échouer la CI) ;
* `python3 .github/scripts/verifier_cles_manquantes.py` — toute clé `R.string` référencée dans le
  code doit exister (**1 597 référencées, toutes présentes**).

---

## 🛠️ Stack technique

| Couche | Choix |
|---|---|
| Langage / build | **Kotlin 2.3** · AGP 9.4 · Gradle 9.6 (Kotlin DSL, version catalog) |
| UI | **Jetpack Compose** + **Material 3** (BOM `2025.09.01`) — `MissaDesign` (`MissaCanvas` `#F8F9FD`, `MissaPanel` 14dp/bord `#CBD5E8`, `MissaTopAppBar` blanche), splash vidéo one-shot |
| Architecture | **MVVM + Clean** : `ui/` → `domain/usecase/` → `data/` |
| Persistance | **Room 2.8.4 (KSP)** — **53 tables**, base **v17**, migrations **1→17** |
| Réglages | **DataStore** + verrous d'amont + `VENTE_SANS_STOCK` |
| Injection | **Hilt 2.60.1** |
| Tâches de fond | **WorkManager** — purge du journal selon la rétention choisie (30 j / 90 j / 12 mois, défaut **12 mois**) + **sauvegarde locale AUTO quotidienne** (redoublée au démarrage) |
| Sécurité | PIN PBKDF2 (PBKDF2WithHmacSHA256, 120 000 itérations, sel 128 bits), aucune dépendance de chiffrement tierce · sauvegarde Google exclue (base, réglages, copies locales) |
| Cible | minSdk **26** · targetSdk **36** · versionName **0.1.0** |

---

## 🏗️ Structure du code

```
app/src/main/java/com/missa/b360/
├── MissaApp.kt · MainActivity.kt    # Hilt, WorkManager (purge journal + sauvegarde AUTO)
├── core/backup/        # BackupManager — export local, restauration (plafond de version
│                       #   lu sur la base réellement ouverte — RestaurationRules)
├── core/data/          # db (Room v17 · 53 tables · migrations 1→17) · dao (35 @Dao) ·
│                       #   entity (53 @Entity) · datastore (+ VENTE_SANS_STOCK) · repository
├── core/domain/model/  # ModulesSocle (avecDependances), Configuration (14 modules, 7 profils,
│                       #   ModuleSousElements : 132 fonctionnalités), DependancesModules /
│                       #   ValidationProfil (règle d'or + validation), ReglesGroupesArticles
│                       #   (groupes transverses), OptionsProfil, règles pures métier (…Rules)
├── core/domain/usecase/ # 30 UseCases
├── core/journal/       # JournalManager (rétention 30 j / 90 j / 365 j, défaut 365)
├── core/security/      # PinHasher (PBKDF2 120 000 it.), PinManager (PIN 4–6, jamais en clair)
├── core/util/          # SequenceManager (FA/FFR/BC/RE/FRN…), Iso4217, Fuseaux, PDF…
├── ui/onboarding/      # bienvenue (langue) → configuration (formats, rétention, sauvegardes,
│                       #   fuseau, restauration) → profil (ASV/APSV en tête) → entreprise →
│                       #   PIN (+ PinLockScreen) → terminé (+ carte licence)
├── ui/home/            # HomeScreen.kt — référence visuelle (1 777 l.)
├── ui/screens/         # SplashVideoScreen — intro vidéo one-shot (res/raw/splash_intro.mp4)
├── ui/components/      # MissaDesign · MissaAppScaffold (+ MissaAppHeader) · MissaBarreModules ·
│                       #   PlaceholderScreen (Scaffold + MissaTopAppBar + MissaEmptyState) ·
│                       #   CompanyLogo · illustrations 3D
├── ui/clients/         # ✅ ClientFlowScreen (1 434 l.) + dialogues + ClientsViewModel — câblé
├── ui/stock/           # ✅ hub + produits + mouvements + transferts + inventaires + alertes…
├── ui/sales/           # 🟡 SalesScreen réel (vente directe + historique) ; DevisCommande /
│                       #   ReturnSales = placeholders (SalesFlowScreen non câblé)
├── ui/purchases/       # ✅ PurchasesScreen (1 900 l.) — chaîne achat complète
├── ui/fournisseurs/    # ✅ FournisseursScreen (2 622 l.)
├── ui/<livraison|logistique|production|services|rh|projets|comptabilite|tresorerie|
│   crm|qualite|maintenance|operations(Reporting)|tasks>/   # ✅ écrans réels par module
├── ui/admin/           # 🟡 Reglages · Sites · Referentiels réels ; Licence · Sauvegarde ·
│                       #   Journal · Utilisateurs · À propos = placeholders
├── ui/notifications/   # ⏳ placeholder
└── ui/navigation/      # ModuleRegistry (18 écrans / 14 modules, barre MAX_ONGLETS = 3) ·
                        #   DestinationsFonctions (85/132 fonctions routées) · AppNavHost
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
* **Catalogue de fonctionnalités** : `ModuleSousElements` (132 entrées) → `DestinationsFonctions`
  (route réelle ou *à venir*) → `MissaFonctionsModule` dans la fiche module.

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
| **B — Onboarding** | bienvenue/langue → configuration → profil ASV/APSV → entreprise → PIN → licence | ✅ (flux ACH→STK + vente sans stock) |
| **C — Accueil** | `HomeScreen` (4 KPI + 8 actions + résumé + activités) + barre 5 items + splash vidéo | ✅ |
| **D — Placeholders** | Écrans en placeholder cohérent pour repartir propre | ✅ (jalon passé — reconstruction livrée module par module) |
| **E — Stock** | Hub Stock (valeur, alertes, actions) + articles, mouvements, transferts, inventaires | ✅ **livré** |
| **F — Achats** | Hub Achats (commandes, réceptions, factures, règlements) + Fournisseurs | ✅ **livré** |
| **G — Ventes** | Vente directe + historique ✅ ; devis → commande → facture et retours/avoirs ⏳ (règles + tests déjà en main) | 🟡 en cours |
| **H+** | Écrans réels des modules support (Livraison … Reporting, Tâches) ✅ ; Admin partiel (Licence, Sauvegarde, Journal, Utilisateurs, À propos ⏳) ; Notifications ⏳ ; liste d'opérations Finances ⏳ | 🟡 en cours |

---

## 🚀 Build & lancement

Prérequis : **Android Studio Quail 3 | 2026.1.3+** (AGP 9.4) et JDK 21.

```bash
git clone -b main https://github.com/missamedia69-code/ERP_360.git
./gradlew assembleDebug
./gradlew testDebugUnitTest          # 37 fichiers de test, 373 tests JVM
python3 .github/scripts/verifier_traductions.py
python3 .github/scripts/verifier_cles_manquantes.py
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
| **Traductions** | 5 `strings.xml` parité parfaite (1 622 clés chacune) |
| **Clés manquantes** | toute clé `R.string` référencée existe (1 597 référencées) |
| **Compilation et tests** | `assembleDebug` + `testDebugUnitTest` (JDK 21, SDK 36) |
| **APK** | artefact `erp360-debug-apk` **et** release fixe [`apk-latest`](https://github.com/missamedia69-code/ERP_360/releases/download/apk-latest/app-debug.apk) mise à jour à chaque poussée verte |

Dernière exécution verte vérifiée sur `main` : **35994735505** (commit `e40dd3f` —
*fix(sauvegarde): restaure enfin les sauvegardes de l'application*).

Téléchargement direct : onglet *Actions* → exécution → `erp360-debug-apk`, ou la release
`apk-latest` (lien ci-dessus).

---

*README tenu à jour à chaque boucle `Plan → Applique → Teste → Corrige`.*
