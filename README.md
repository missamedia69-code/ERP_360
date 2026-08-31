# Missa Business 360 — ERP mobile offline-first

Implémentation du cahier de charge **E9** (`e9-cahier-de-charge.md`) : ERP Android natif,
13 modules, 5 langues (FR · EN · ES · AR-RTL · ZH), multi-site, profils A–H, offline-first.

## Stack

Kotlin 2.3 · Jetpack Compose + Material 3 · MVVM + Clean · Room (KSP) · Hilt ·
DataStore · WorkManager · security-crypto · kotlinx-serialization — package `com.missa.b360`.

## Avancement (phasage E9 : A → K)

| Phase | Contenu | État |
|---|---|---|
| **A — Socle** | Gradle, Hilt, Room (17 entités), DAOs, SettingsStore + verrous, services transverses (PinManager, LicenceManager, JournalManager, SequenceManager, AppNotifier, BackupManager, PermissionChecker), navigation ☰/🔔/barre bas/➕ (RA-22), WorkManager purge journal (RA-18), 5 langues, tests unitaires | ✅ |
| **B — Onboarding** | langue → profil A–H → entreprise/défauts → PIN (RA-01) → email (RA-03) → licence essai 7 j (RA-04) → checklist (RA-11), verrou PIN (RA-02) | ✅ |
| **C — 9.1 Admin** | réglages (D4/RA-19), licence (RA-05/06), sauvegarde (RA-13), journal (RA-18), utilisateurs & rôles (D1/D2), multi-site (RA-21), à propos | ✅ |
| **D → K** | Clients, Stock, Vente, Fournisseurs/Achats, Finances, Livraison/Production/Services, RH/Projets, Reporting | ⏳ |

## Conventions clés (cahier de charge §5)

- 1 règle métier = 1 UseCase (`// RA-01`, `// RC-05`…)
- Annulation = **compensation** — jamais de DELETE physique sur les pièces métier
- Écriture unique du stock : `StockMovementWriter` (à venir, Phase E)
- Verrous d'amont (devise, taxes, numérotation, paiements) refusés si `locked` (RA-19)
- Numérotation atomique `SequenceManager.next(type)` en transaction Room
- PIN hashé PBKDF2 (jamais en clair), journal immuable purgé à 12 mois
- **Aucune donnée d'exemple** : l'app démarre vide
