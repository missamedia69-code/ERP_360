# /verif — Vérification complète avant de déclarer une tâche terminée

Exécute dans cet ordre et rapporte chaque résultat (ne jamais annoncer « terminé » si une étape échoue) :

1. `python3 .github/scripts/verifier_traductions.py` — parité des 5 fichiers strings.xml
2. `python3 .github/scripts/verifier_cles_manquantes.py` — toute clé `R.string.*` référencée en Kotlin doit exister
3. `./gradlew testDebugUnitTest` (Windows : `.\gradlew.bat testDebugUnitTest`) — tests unitaires JVM
4. Si du code de production a changé : `./gradlew assembleDebug` — compilation complète

Termine par une phrase : ce qui a été exécuté, ce qui est vert, ce qui est rouge (avec le fichier:ligne exact).
