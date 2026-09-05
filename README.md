# Missa Business 360 — ERP mobile offline-first

<p>
  <img alt="Plateforme" src="https://img.shields.io/badge/plateforme-Android%208.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Licence" src="https://img.shields.io/badge/licence-Apache%202.0-blue">
</p>

**Missa Business 360** (`com.missa.b360`) est un **ERP complet et natif pour Android**, pensé
pour les TPE/PMI : il fonctionne **100 % hors-ligne**, couvre **13 modules métier**, parle
**5 langues** (FR · EN · ES · AR-RTL · ZH) et gère le **multi-site** avec des **profils
d'activité A–H**. Implémentation du cahier de charge **E9** (`e9-cahier-de-charge.md`).

> **Offline-first** : aucune donnée ne quitte le téléphone. L'application démarre
> **sans aucune donnée d'exemple** — tout est créé par l'utilisateur au fil de l'onboarding.

---

## 🧩 Les 13 modules

| Module | Barre du bas | Description |
|---|:---:|---|
| **9.1 Administration & Paramétrage** | ☰ | Réglages verrouillables, licence, sauvegarde, journal d'audit, utilisateurs & rôles, multi-site |
| **Vente** | ✅ | Devis → commande → facture, numérotation automatique, calculs taxes |
| **Stock** | ✅ | Produits (form. 1 page §7), mouvements entrées/sorties/ajustements + transferts entre entrepôts, alertes de seuil — persistance réelle, transactionnelle |
| **Clients** | ✅ | Fiches détaillées (NIF, contacts & adresses multiples, conditions de paiement), catégories, tarifs, fidélité |
| **Finances** | ✅ | Encaissements, dépenses, moyens de paiement |
| **Fournisseurs** | ➕ | Référentiel fournisseurs |
| **Achats** | ➕ | Commandes et réceptions fournisseurs |
| **Livraison** | ➕ | Suivi des livraisons |
| **Production** | ➕ | Ordres et suivi de production |
| **Services** | ➕ | Prestations de services |
| **RH** | ➕ | Employés et paie |
| **Projets** | ➕ | Suivi de projets |
| **Reporting** | ➕ | Tableaux de bord et indicateurs |

Navigation **RA-22** : menu ☰ (admin), cloche 🔔 (notifications internes), barre du bas
personnalisable (Vente · Stock · Clients · Finances par défaut) et bouton **➕** donnant
accès aux modules secondaires.

## 👥 Profils d'activité A–H (RA-20)

Choisi à l'onboarding et modifiable ensuite, le profil **active dynamiquement les modules**
pertinents pour le métier de l'entreprise (commerce, production, services…).

## 🌍 Internationalisation

5 langues intégrales, y compris **arabe avec mise en page RTL** : Français (défaut) ·
English · Español · العربية · 中文. Changement de langue à chaud (per-app language,
AppCompatDelegate).

## 🛠️ Stack technique

| Couche | Choix |
|---|---|
| Langage / build | **Kotlin 2.3** · AGP 9.4 · Gradle Kotlin DSL (version catalog) |
| UI | **Jetpack Compose** + **Material 3** (BOM 2025.09) |
| Architecture | **MVVM + Clean** : `ui/` → `domain/usecase/` → `data/` |
| Persistance | **Room 2.8 (KSP)** — 25 entités, schémas exportés, migrations manuelles |
| Réglages | **DataStore** (préférences + verrous d'amont) |
| Injection | **Hilt 2.60** (+ `hilt-navigation-compose`, `@HiltWorker`) |
| Tâches de fond | **WorkManager** (purge du journal à 12 mois, sauvegarde auto) |
| Sécurité | **security-crypto**, PIN hashé **PBKDF2** (jamais en clair) |
| Export | **kotlinx-serialization** (JSON) |
| Cible | minSdk **26** · targetSdk **36** |

## 🏗️ Structure du code

```
app/src/main/java/com/missa/b360/
├── MissaApp.kt / MainActivity.kt          # Application Hilt + splash vidéo
├── core/
│   ├── data/          # Room : db (25 entités, v6), dao, entity, datastore (SettingsStore + verrous)
│   ├── domain/        # model + usecase (1 règle métier = 1 UseCase, commentée // RA-xx)
│   ├── security/      # PinHasher (PBKDF2), PinManager (verrou RA-02)
│   ├── licensing/     # LicenceManager (essai 7 j RA-04, activation RA-05/06)
│   ├── numbering/     # SequenceManager — numérotation atomique en transaction Room
│   ├── journal/       # JournalManager — audit immuable (RA-18)
│   ├── backup/        # BackupManager (RA-13)
│   ├── notifications/ # AppNotifier — notifications internes
│   ├── permissions/   # PermissionChecker — rôles & droits (D1/D2)
│   └── workers/       # JournalPurgeWorker (purge 12 mois)
├── di/                # Modules Hilt
└── ui/
    ├── onboarding/    # langue → profil A–H → entreprise → PIN → email → licence → checklist
    ├── home/          # tableau de bord
    ├── navigation/    # NavHost, Routes, ModuleRegistry (13 modules)
    ├── admin/         # 7 écrans du module 9.1
    ├── clients/ · fournisseurs/ · sales/ · operations/   # modules métier
    └── components/ · theme/ · screens/
```

## 📈 Avancement (phasage E9 : A → K)

| Phase | Contenu | État |
|---|---|---|
| **A — Socle** | Gradle, Hilt, Room (schéma v6, migrations 1→6), DAOs, SettingsStore + verrous, services transverses (Pin, Licence, Journal, Séquences, Notifications, Sauvegarde, Permissions), navigation (RA-22), WorkManager purge (RA-18), 5 langues, tests unitaires | ✅ |
| **B — Onboarding** | langue → profil A–H → entreprise/défauts → PIN (RA-01) → email (RA-03) → licence essai 7 j (RA-04) → checklist (RA-11), verrou PIN (RA-02) | ✅ |
| **C — 9.1 Admin** | réglages (D4/RA-19), licence (RA-05/06), sauvegarde (RA-13), journal (RA-18), utilisateurs & rôles (D1/D2), multi-site (RA-21), à propos | ✅ |
| **D → K** | Clients · Stock · Vente · Fournisseurs/Achats · Finances · Livraison/Production/Services · RH/Projets · Reporting | ⏳ En cours |

## 📏 Conventions métier (cahier de charge §5)

- **1 règle métier = 1 UseCase**, commentée de sa référence (`// RA-01`, `// RC-05`…)
- Annulation = **compensation** — jamais de `DELETE` physique sur les pièces métier
- Écriture unique du stock : use cases transactionnels `RecordStockMovementUseCase` / `TransferStockUseCase` — vérification de disponibilité → mise à jour du stock → mouvement + journal dans une seule transaction (§43/§44)
- **Verrous d'amont** (devise, taxes, numérotation, paiements) : modification refusée une fois `locked` (RA-19)
- Numérotation atomique `SequenceManager.next(type)` **en transaction Room** (aucun doublon de n° de pièce)
- PIN hashé **PBKDF2** ; **journal d'audit immuable**, purgé automatiquement à 12 mois
- Multi-devise/taxes figées au premier usage ; sauvegarde locale chiffrable
- **Aucune donnée d'exemple** : la base démarre vide

## 🚀 Build & lancement

Prérequis : **Android Studio Quail 3 | 2026.1.3+** (AGP 9.4) et JDK 11+.

```bash
./gradlew assembleDebug      # compilation
./gradlew testDebugUnitTest  # tests unitaires des règles métier (UseCases)
```

Les tests couvrent notamment : validation clients (`ClientValidationTest`), pièces
opérationnelles (`OperationValidationTest`), calculateur de vente (`SaleCalculatorTest`)
et use cases du socle (`SocleUseCasesTest`).

## 📁 Annexes

- `branding/` — logo officiel `logo_missa.png` + script `gen_icons.ps1` de génération des icônes
- `app/schemas/` — schémas Room exportés (traçabilité des migrations)
- `LICENSE` — Apache License 2.0
