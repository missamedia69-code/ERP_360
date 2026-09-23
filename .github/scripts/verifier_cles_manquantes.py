#!/usr/bin/env python3
"""
Vérifie que toute clé R.string.X référencée dans le code Kotlin existe bien dans
app/src/main/res/values/strings.xml (fichier de référence français).

Complète verifier_traductions.py (qui vérifie la parité des 5 langues) en attrapant
les clés manquantes AVANT la compilation — erreur classique : écran réécrit,
clés ajoutées dans le Kotlin mais oubliées dans les strings.

Usage : python3 .github/scripts/verifier_cles_manquantes.py
Sortie : liste des clés manquantes ; code retour 1 si au moins une manque.
"""
import io
import os
import re
import sys

RACINE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VALEURS_FR = os.path.join(RACINE, "app", "src", "main", "res", "values")
SOURCE_KT = os.path.join(RACINE, "app", "src", "main", "java")

# Lookbehind : exclut android.R.string.* (ressources du framework).
MOTIF_REFERENCE = re.compile(r"(?<!android\.)R\.string\.([A-Za-z0-9_]+)")
MOTIF_DEFINITION = re.compile(r'<string name="([A-Za-z0-9_]+)"')


def main() -> int:
    # Android fusionne tous les values/*.xml : les définitions peuvent vivre
    # dans strings.xml ou dans un fichier dédié (ex. contact.xml).
    definies = set()
    for fichier in sorted(os.listdir(VALEURS_FR)):
        if fichier.endswith(".xml"):
            contenu = io.open(os.path.join(VALEURS_FR, fichier), encoding="utf-8").read()
            definies.update(MOTIF_DEFINITION.findall(contenu))

    referencees = {}
    for dossier, _, fichiers in os.walk(SOURCE_KT):
        for fichier in fichiers:
            if not fichier.endswith(".kt"):
                continue
            chemin = os.path.join(dossier, fichier)
            contenu = io.open(chemin, encoding="utf-8").read()
            for cle in MOTIF_REFERENCE.findall(contenu):
                referencees.setdefault(cle, []).append(
                    os.path.relpath(chemin, RACINE)
                )

    manquantes = sorted(cle for cle in referencees if cle not in definies)
    if manquantes:
        print("Clés R.string référencées mais ABSENTES de values/*.xml :")
        for cle in manquantes:
            print("  · %s  (utilisée dans %s)" % (cle, ", ".join(sorted(set(referencees[cle]))[:3])))
        print("Ajoutez-les dans les 5 langues (voir /chaines).")
        return 1

    print(
        "OK — %d clés référencées, toutes présentes (%d clés définies au total)."
        % (len(referencees), len(definies))
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
