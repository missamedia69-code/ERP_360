# Missa Business 360

ERP Android natif, **100 % hors-ligne**, pour les TPE/PMI.

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin 2.3](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Licence Apache 2.0](https://img.shields.io/badge/licence-Apache%202.0-blue)
[![Build Android](https://github.com/missamedia69-code/ERP_360/actions/workflows/android.yml/badge.svg)](https://github.com/missamedia69-code/ERP_360/actions/workflows/android.yml)

Missa Business 360 (`com.missa.b360`) réunit achats, stock, production, ventes, services,
comptabilité, trésorerie et fonctions support dans une seule application mobile. Les données
restent sur le téléphone : aucune connexion n'est nécessaire au quotidien.

## Points clés

- **Hors-ligne d'abord** : base Room locale, aucun compte ni serveur requis. L'application
  démarre sans donnée d'exemple ; tout est créé pendant l'onboarding.
- **14 modules** (6 métier, 8 support) activés selon un **profil d'activité**, avec gestion
  **multi-site**.
- **5 langues** : français (par défaut), anglais, espagnol, chinois, arabe (RTL).
- **Intégrité comptable** : aucune suppression physique ; les annulations passent par des
  statuts ou des contre-passations.
- **Sécurité locale** : code PIN (PBKDF2), déverrouillage biométrique, sauvegarde Google exclue,
  sauvegarde locale automatique.
- **Documents professionnels** : génération et partage de PDF (factures, fiches).

## Modules

| Type | Modules |
|---|---|
| Métier | Achats (ACH), Stock (STK), Production (PRO), Vente (VEN), Services (SER), Projets (PRJ) |
| Support | Comptabilité (CPT), Trésorerie (TRE), Logistique (LOG), Reporting (REP), CRM, RH, Qualité (QUA), Maintenance (MAI) |

Écrans transverses : accueil (tableau de bord), clients, fournisseurs, finances, tâches,
notifications et administration (réglages, sites, référentiels).

Le **stock est le pivot** : tout achat ou toute production alimente un stock (`ACH → STK`,
`PRO → STK`). Seule l'option « vente sans stock » permet de vendre sans module Stock.

### Profils d'activité

| Profil | Usage |
|---|---|
| `ASV` | Négoce classique (achats, stock, ventes) |
| `APSV` | Fabrication / industrie |
| `AV` | Négoce sans stock (hérité, à migrer vers `ASV`) |
| `SER` | Prestations de service |
| `PRJ` | Société de projets / ingénierie |
| `FULL` | Tous les modules |
| `PERSONNEL` | Gestion personnelle des dépenses et des entrées |
| `CUSTOM` | Activation manuelle, validée par les règles de dépendance |

### Fonctionnalités à venir

Le catalogue (`ModuleSousElements`) liste davantage de fonctions que celles réellement livrées.
`DestinationsFonctions` ne route une fonction que si elle ouvre un écran fonctionnel, les autres
sont affichées comme « prévues ». Écrans encore en placeholder : retours/avoirs de vente,
administration (licence, sauvegarde, journal, utilisateurs, à propos) et
`OperationModuleScreen`. Les limites de chaque module sont décrites dans [`docs/`](docs).

## Stack technique

| Couche | Choix |
|---|---|
| Langage / build | Kotlin 2.3, AGP 9.4, Gradle 9.6 (Kotlin DSL, version catalog) |
| Interface | Jetpack Compose, Material 3, navigation Compose |
| Architecture | MVVM + Clean : `ui` → `domain/usecase` → `data` |
| Persistance | Room 2.8 (KSP), base v22, 71 entités, migrations 1→22, schémas exportés dans `app/schemas` |
| Réglages | DataStore |
| Injection | Hilt |
| Tâches de fond | WorkManager (purge du journal, sauvegarde locale quotidienne) |
| Sérialisation | kotlinx.serialization (JSON) |
| Cible | minSdk 26, targetSdk 36, Java 11 |

## Structure du dépôt

```
.
├── app/                        # Module Android unique
│   ├── schemas/                # Schémas Room exportés (commités par la CI)
│   └── src/
│       ├── main/java/com/missa/b360/
│       │   ├── core/           # data (Room), domain (règles pures, use cases), backup, documents,
│       │   │                   #   journal, licensing, numbering, security, sync, util, workers
│       │   ├── di/             # Modules Hilt
│       │   └── ui/             # Un package par module + onboarding, navigation, composants
│       ├── main/res/           # Ressources, 5 fichiers strings.xml
│       └── test/               # Tests unitaires JVM
├── branding/                   # Logo et script de génération des icônes
├── docs/                       # Documentation fonctionnelle et d'architecture
├── .github/
│   ├── workflows/android.yml   # Intégration continue
│   └── scripts/                # Contrôles des traductions et annotation des échecs de CI
└── gradle/                     # Wrapper et catalogue de versions
```

## Démarrage

### Prérequis

- JDK 21
- Android SDK (plateforme 36, build-tools 36.0.0) ou Android Studio récent

### Compiler et tester

```bash
./gradlew assembleDebug          # APK de débogage : app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # tests unitaires JVM
```

### Contrôle des traductions

Toute nouvelle clé `R.string` doit exister dans les cinq langues.

```bash
python3 .github/scripts/verifier_traductions.py      # parité des cinq strings.xml
python3 .github/scripts/verifier_cles_manquantes.py  # clés référencées mais absentes
```

### Installer l'APK

La CI publie à chaque poussée verte l'APK de débogage dans la release fixe
[`apk-latest`](https://github.com/missamedia69-code/ERP_360/releases/tag/apk-latest).

## Intégration continue

Le workflow [`android.yml`](.github/workflows/android.yml) s'exécute sur `main`, `arena/**` et
les pull requests :

1. contrôle des traductions ;
2. compilation (`assembleDebug`) ;
3. tests unitaires (`testDebugUnitTest`) ;
4. export des schémas Room et publication de l'APK.

## Documentation

| Document | Contenu |
|---|---|
| [`docs/PRD_MISSA_BUSINESS_360.md`](docs/PRD_MISSA_BUSINESS_360.md) | Exigences produit |
| [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) | Conventions de code, d'interface et de base de données |
| [`docs/fiscalite-multizones.md`](docs/fiscalite-multizones.md) | Fiscalité multi-zones et identifiants uniques |
| [`docs/ARCHITECTURE_STOCK.md`](docs/ARCHITECTURE_STOCK.md) | Module Stock |
| [`docs/ARCHITECTURE_PRODUCTION.md`](docs/ARCHITECTURE_PRODUCTION.md) | Module Production |
| [`docs/ARCHITECTURE_SERVICES.md`](docs/ARCHITECTURE_SERVICES.md) | Module Services |
| [`docs/ARCHITECTURE_COMPTABILITE.md`](docs/ARCHITECTURE_COMPTABILITE.md) | Module Comptabilité |
| [`docs/exemples/`](docs/exemples) | Exemple de facture professionnelle |

## Licence

Apache 2.0 — voir [LICENSE](LICENSE).
