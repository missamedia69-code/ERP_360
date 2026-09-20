# /chaines — Ajouter des clés de traduction dans les 5 langues

Arguments : la liste des clés et leur texte français.

Procédure obligatoire :

1. Vérifier d'abord que la clé n'existe pas déjà : `grep -c 'name="ma_cle"' app/src/main/res/values/strings.xml`
2. Insérer la clé dans les **cinq** fichiers, au même endroit logique (près des clés du même module) :
   - `app/src/main/res/values/strings.xml` (français)
   - `app/src/main/res/values-en/strings.xml` (anglais)
   - `app/src/main/res/values-es/strings.xml` (espagnol)
   - `app/src/main/res/values-zh/strings.xml` (chinois)
   - `app/src/main/res/values-ar/strings.xml` (arabe)
3. Échappement Android : apostrophes `\'`, `%` littéral `%%`, arguments positionnels `%1$s` / `%1$d` / `%1$.1f`
4. Valider : `python3 .github/scripts/verifier_traductions.py`

Ne jamais laisser un écran avec du texte codé en dur : toute chaîne visible passe par `R.string`.
