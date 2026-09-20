# /migration — Ajouter une migration Room

Arguments : description du changement de schéma.

Checklist stricte (une colonne mal alignée = crash au démarrage, pas à la compilation) :

1. Modifier/créer les entités dans `core/data/entity/` (toute nouvelle colonne doit avoir une valeur par défaut Kotlin).
2. Dans `core/data/db/AppDatabase.kt` :
   - incrémenter `version`
   - ajouter la `MIGRATION_N_M+1` avec du SQL explicite :
     - `ALTER TABLE … ADD COLUMN` : type exact (`TEXT`/`INTEGER`/`REAL`), `NOT NULL` seulement avec `DEFAULT`
     - nouvelles tables : `CREATE TABLE IF NOT EXISTS` avec `id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL`, colonnes dans l'ordre de l'entité, types exacts (Boolean → `INTEGER NOT NULL`, enum → `TEXT NOT NULL`, Long/Int → `INTEGER`, Double → `REAL`, String? → `TEXT`)
     - index nommés `index_<table>_<colonne>` (Room vérifie aussi les index)
   - enregistrer les entités dans la liste `entities` et exposer les nouveaux DAO
3. Enregistrer la migration dans `di/DatabaseModule.kt` (`addMigrations(…)`) + providers `@Provides` des nouveaux DAO.
4. Reconvertir les données si nécessaire (ex. `UPDATE … SET statut='X' WHERE statut='Y'`).
5. Vérifier : `./gradlew assembleDebug testDebugUnitTest`, puis si possible lancer l'app sur émulateur depuis une installation existante (c'est la migration qui s'exécute au premier lancement).
