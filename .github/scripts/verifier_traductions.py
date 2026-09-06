#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Garde-fou des traductions d'ERP 360.

Règle du projet : chaque élément d'interface existe dans les cinq langues
(français, anglais, espagnol, arabe, chinois) et aucun texte n'est écrit en dur
dans le code. Ce script échoue — donc fait échouer la CI — si :

* une clé présente en français manque dans une autre langue, ou l'inverse ;
* une traduction n'emploie pas les mêmes paramètres (%1$s, %2$d…) que le
  français, ce qui provoquerait un plantage à l'exécution ;
* une chaîne française contient une apostrophe non échappée, refusée par aapt ;
* une clé est déclarée deux fois dans le même fichier.

Les avertissements (clés jamais utilisées dans le code) sont affichés sans
faire échouer la vérification : une chaîne peut légitimement servir plus tard.
"""

from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

RACINE = pathlib.Path(__file__).resolve().parents[2]
RESSOURCES = RACINE / "app" / "src" / "main" / "res"
SOURCES = RACINE / "app" / "src" / "main" / "java"

REFERENCE = "values"
TRADUCTIONS = ["values-en", "values-es", "values-ar", "values-zh"]

MOTIF_PARAMETRE = re.compile(r"%(\d+\$)?[sdf]")
MOTIF_APOSTROPHE = re.compile(r"(?<!\\)'")


def chaines(dossier: str) -> dict[str, str]:
    """Clés et valeurs brutes d'un strings.xml, en signalant les doublons."""
    fichier = RESSOURCES / dossier / "strings.xml"
    if not fichier.exists():
        erreur(f"{dossier}/strings.xml est introuvable.")
        return {}
    racine = ET.parse(fichier).getroot()
    trouvees: dict[str, str] = {}
    for noeud in racine.findall("string"):
        nom = noeud.get("name")
        if nom is None:
            continue
        if nom in trouvees:
            erreur(f"{dossier} : la clé « {nom} » est déclarée deux fois.")
        trouvees[nom] = "".join(noeud.itertext())
    return trouvees


ERREURS: list[str] = []


def erreur(message: str) -> None:
    ERREURS.append(message)


def parametres(valeur: str) -> list[str]:
    return sorted(MOTIF_PARAMETRE.findall(valeur.replace("%%", "")))


def main() -> int:
    reference = chaines(REFERENCE)
    if not reference:
        print("Aucune chaîne de référence : vérification impossible.", file=sys.stderr)
        return 1

    # Apostrophes : aapt refuse ' non échappée hors CDATA.
    brut = (RESSOURCES / REFERENCE / "strings.xml").read_text(encoding="utf-8")
    for ligne in brut.splitlines():
        corps = re.search(r"<string name=\"([^\"]+)\">(.*)</string>", ligne)
        if corps and MOTIF_APOSTROPHE.search(corps.group(2)):
            erreur(
                f"{REFERENCE} : apostrophe non échappée dans « {corps.group(1)} » "
                "— écrire \\' pour qu'aapt accepte la chaîne."
            )

    for langue in TRADUCTIONS:
        traduites = chaines(langue)
        manquantes = sorted(set(reference) - set(traduites))
        surnumeraires = sorted(set(traduites) - set(reference))
        for cle in manquantes:
            erreur(f"{langue} : « {cle} » n'est pas traduite.")
        for cle in surnumeraires:
            erreur(f"{langue} : « {cle} » n'existe pas en français.")
        for cle in sorted(set(reference) & set(traduites)):
            attendus = parametres(reference[cle])
            obtenus = parametres(traduites[cle])
            if attendus != obtenus:
                erreur(
                    f"{langue} : « {cle} » attend les paramètres {attendus} "
                    f"mais porte {obtenus}."
                )

    # Avertissement seulement : une clé peut être prévue pour un écran à venir.
    code = "\n".join(
        chemin.read_text(encoding="utf-8", errors="ignore")
        for chemin in SOURCES.rglob("*.kt")
    )
    utilisees = set(re.findall(r"R\.string\.(\w+)", code))
    orphelines = sorted(cle for cle in reference if cle not in utilisees)

    print(f"{len(reference)} chaînes × {len(TRADUCTIONS) + 1} langues vérifiées.")
    if orphelines:
        print(f"{len(orphelines)} clés jamais référencées dans le code :")
        for cle in orphelines[:30]:
            print(f"  · {cle}")
        if len(orphelines) > 30:
            print(f"  … et {len(orphelines) - 30} autres.")

    if ERREURS:
        print(f"\n{len(ERREURS)} problème(s) de traduction :", file=sys.stderr)
        for message in ERREURS:
            print(f"  ✗ {message}", file=sys.stderr)
        return 1

    print("Traductions cohérentes dans les cinq langues.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
