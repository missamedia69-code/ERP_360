# Commande /boucle_modules — Implémentation itérative d'un module ERP_360

Cette commande guide l'implémentation complète, rigoureuse et autonome d'un module manquant.

## Étape 1 : Cadrage & Choix du Module
1. Consulter `docs/PRD_MISSA_BUSINESS_360.md` pour les spécifications fonctionnelles du module cible.
2. Vérifier si les entités Room existent déjà dans `core/data/entity/`.
3. Vérifier les écrans existants dans `ui/<nom_module>/` et la présence d'un `PlaceholderScreen`.

## Étape 2 : Cycle TDD & Métier Pur (Domain)
1. Créer ou compléter les règles métier pures dans `core/domain/model/<Module>Rules.kt`.
2. Écrire le test unitaire JUnit correspondant dans `app/src/test/java/com/missa/b360/<Module>RulesTest.kt`.
3. Lancer `./gradlew testDebugUnitTest` et s'assurer que les tests du module sont VERTS.

## Étape 3 : Données & Persistance (Room)
1. Si de nouvelles tables ou colonnes sont nécessaires :
   - Mettre à jour `AppDatabase.kt` (incrémenter la version, ex: 17 -> 18).
   - Suivre scrupuleusement la checklist dans `.claude/commands/migration.md`.
   - Créer le `Migration_X_Y.kt` et l'enregistrer dans `DatabaseMigrations.kt`.

## Étape 4 : Interface Utilisateur (Compose)
1. Implémenter l'interface complète dans `ui/<nom_module>/` :
   - **Hub / Tableau de bord** : KPI récapitulatifs, liste avec recherche/filtres.
   - **Fiche détaillée** : Vue 360°, cycle de vie / statuts, boutons d'action.
   - **Formulaires** : Création/édition par étapes avec validation stricte.
2. **RÈGLES DESIGN NON NÉGOCIABLES :**
   - Utiliser uniquement les icônes `Iv.*` (ou `StockIv.*`).
   - TOUTES les icônes en noir strict : `Color.Black` ou `MissaInk`.
   - Couleur caractéristique du module en bandeau `couleurPale` et barre du bas.
   - Hauteurs tactiles minimales à 48dp, espacements conformes à `CLAUDE.md`.

## Étape 5 : Internationalisation (5 langues)
1. Ajouter chaque nouvelle clé `R.string` dans les 5 fichiers :
   - `app/src/main/res/values/strings.xml` (FR - référence)
   - `app/src/main/res/values-en/strings.xml` (EN)
   - `app/src/main/res/values-es/strings.xml` (ES)
   - `app/src/main/res/values-zh/strings.xml` (ZH)
   - `app/src/main/res/values-ar/strings.xml` (AR)
2. Échapper systématiquement les apostrophes (`\'`).

## Étape 6 : Raccordement Navigation
1. Remplacer le `PlaceholderScreen` dans `AppNavHost.kt` par le nouvel écran réel.
2. Vérifier que la route passe par `GuardedModule`.

## Étape 7 : Vérification & Clôture de Boucle
1. Lancer la commande `/verif` (ou les scripts manuellement) :
   - `python3 .github/scripts/verifier_traductions.py`
   - `python3 .github/scripts/verifier_cles_manquantes.py`
   - `./gradlew testDebugUnitTest`
   - `./gradlew assembleDebug`
2. Si une étape échoue : corriger immédiatement avant toute autre action.
3. Quand tout est VERT : commit git clair et passage au module suivant.
