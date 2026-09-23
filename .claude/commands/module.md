# /module — Développer ou faire évoluer un module

Arguments : le module et le besoin.

Méthode imposée par le projet :

1. Lire `CLAUDE.md` (conventions) et le code existant du module (`ui/<module>/`, `core/domain/usecase/`, `core/domain/model/`).
2. **Règles d'abord** : écrire la logique métier en objet pur dans `core/domain/model/` (sans Android) + tests JVM dans `app/src/test/java/com/missa/b360/`.
3. **Use cases** : `core/domain/usecase/` — transactionnel via `database.withTransaction`, licence (`LicenceManager`), audit (`JournalManager`), notifications (`AppNotifier`). Convention C7 : jamais de DELETE physique.
4. **Data** : entités + DAO + migration (voir `/migration`) si nouveau schéma.
5. **UI** : Screen + ViewModel Hilt dans `ui/<module>/`. Icônes `Iv`/`StockIv` en noir, couleur du module via `AppModule`, `StateFlow` via `stateIn`.
6. **Traductions** : toutes les nouvelles clés dans les 5 langues (voir `/chaines`).
7. **Vérification** : `/verif` obligatoire avant de déclarer terminé.

Interdictions : dépendance Gradle nouvelle non justifiée, `Icons.*` Material, suppression physique, texte codé en dur, sélecteur qui ne déroule pas.
