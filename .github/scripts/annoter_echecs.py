#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Transforme un échec de build en annotations GitHub lisibles.

Les journaux bruts d'Actions ne sont pas toujours accessibles (stockage blob
filtré selon les réseaux). Ce script relit la sortie Gradle et les rapports
JUnit, puis republie l'essentiel sous deux formes toujours consultables :

* des annotations « ::error » attachées au fichier et à la ligne fautifs,
  visibles dans l'onglet Actions comme via `gh run view` ;
* un résumé Markdown dans le récapitulatif d'exécution.

Usage : annoter_echecs.py [journal-gradle...]
"""

from __future__ import annotations

import os
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

RACINE = pathlib.Path(__file__).resolve().parents[2]
RESULTATS = RACINE / "app" / "build" / "test-results"

# e: file:///.../OnbEntreprise.kt:506:42 Unresolved reference 'format'.
MOTIF_KOTLIN = re.compile(
    r"^(?P<gravite>[ew]): file://(?P<fichier>[^\s:]+):(?P<ligne>\d+):(?P<colonne>\d+)\s+(?P<message>.*)$"
)
# app/src/main/res/values/strings.xml:12: AAPT: error: ...
MOTIF_AAPT = re.compile(r"^(?P<fichier>[^\s:]+\.xml):(?P<ligne>\d+):\s+(?P<message>.*error.*)$")
# ERROR: /chemin/fichier.png: AAPT: error: file failed to compile.
MOTIF_RESSOURCE = re.compile(
    r"^(?:ERROR:\s*)?(?P<fichier>\S+\.(?:png|jpg|webp|xml)):?\s*(?:AAPT:)?\s*(?P<message>.*(?:error|failed).*)$",
    re.IGNORECASE,
)
# Dernier filet : uniquement les lignes qui désignent vraiment une cause
# d'échec. Une version trop large captait chaque « > Task :app:… » et noyait
# l'information utile sous quarante annotations sans intérêt.
MOTIFS_GENERIQUES = (
    re.compile(r"^> Task \S+ FAILED\s*$"),
    re.compile(r"^FAILURE: (?P<message>.+)$"),
    re.compile(r"^Caused by: (?P<message>.+)$"),
    re.compile(r"^Execution failed for task (?P<message>.+)$"),
    # Erreurs de génération de code (KSP, Dagger, javac), invisibles pour les
    # motifs Kotlin car préfixées d'un chemin de fichier généré.
    re.compile(r"^.*\berror:\s+(?P<message>.+)$"),
    re.compile(r"^\s*\[Dagger/\w+\]\s*(?P<message>.+)$"),
)

LIMITE = 30


def relatif(chemin: str) -> str:
    """Chemin relatif au dépôt, seul format compris par les annotations."""
    try:
        return str(pathlib.Path(chemin).resolve().relative_to(RACINE))
    except ValueError:
        return chemin.lstrip("/")


def echapper(texte: str) -> str:
    return texte.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def annoter(message: str, fichier: str | None = None, ligne: str | None = None,
            colonne: str | None = None) -> None:
    position = ""
    if fichier:
        position = f" file={fichier}"
        if ligne:
            position += f",line={ligne}"
        if colonne:
            position += f",col={colonne}"
    print(f"::error{position}::{echapper(message)}")


def erreurs_compilation(journaux: list[pathlib.Path]) -> list[str]:
    resume: list[str] = []
    vues: set[tuple[str, str, str]] = set()
    for journal in journaux:
        if not journal.exists():
            continue
        for brute in journal.read_text(encoding="utf-8", errors="ignore").splitlines():
            ligne = brute.strip()
            trouve = MOTIF_KOTLIN.match(ligne)
            if trouve and trouve.group("gravite") == "e":
                cle = (trouve.group("fichier"), trouve.group("ligne"), trouve.group("message"))
                if cle in vues:
                    continue
                vues.add(cle)
                fichier = relatif(trouve.group("fichier"))
                annoter(
                    trouve.group("message"),
                    fichier,
                    trouve.group("ligne"),
                    trouve.group("colonne"),
                )
                resume.append(
                    f"| Compilation | `{fichier}:{trouve.group('ligne')}` | "
                    f"{trouve.group('message')} |"
                )
                continue
            trouve = MOTIF_AAPT.match(ligne)
            if trouve:
                cle = (trouve.group("fichier"), trouve.group("ligne"), trouve.group("message"))
                if cle in vues:
                    continue
                vues.add(cle)
                fichier = relatif(trouve.group("fichier"))
                annoter(trouve.group("message"), fichier, trouve.group("ligne"))
                resume.append(
                    f"| Ressources | `{fichier}:{trouve.group('ligne')}` | "
                    f"{trouve.group('message')} |"
                )
                continue

            trouve = MOTIF_RESSOURCE.match(ligne)
            if trouve and "warning" not in ligne.lower():
                cle = (trouve.group("fichier"), trouve.group("message"))
                if cle in vues:
                    continue
                vues.add(cle)
                fichier = relatif(trouve.group("fichier"))
                annoter(trouve.group("message").strip(), fichier)
                resume.append(f"| Ressources | `{fichier}` | {trouve.group('message').strip()} |")
                continue

            # Dernier filet : sans lui, un échec inconnu ne laisse aucune trace
            # exploitable, les journaux bruts n'étant pas téléchargeables.
            for motif in MOTIFS_GENERIQUES:
                trouve = motif.match(ligne)
                if not trouve:
                    continue
                message = (
                    trouve.groupdict().get("message") or trouve.group(0)
                ).strip()
                if not message or message in vues:
                    break
                vues.add(message)
                annoter(message[:400])
                resume.append(f"| Build | — | {message[:400]} |")
                break
    return resume


def echecs_de_tests() -> list[str]:
    resume: list[str] = []
    for rapport in sorted(RESULTATS.rglob("TEST-*.xml")):
        try:
            racine = ET.parse(rapport).getroot()
        except ET.ParseError:
            continue
        for cas in racine.iter("testcase"):
            for defaut in list(cas.findall("failure")) + list(cas.findall("error")):
                classe = (cas.get("classname") or "").rsplit(".", 1)[-1]
                nom = cas.get("name") or "?"
                brut = (defaut.get("message") or defaut.text or "").strip()
                message = brut.splitlines()[0] if brut else "échec sans message"
                annoter(f"{classe} › {nom} : {message}")
                resume.append(f"| Test | `{classe}.{nom}` | {message} |")
    return resume


def main() -> int:
    journaux = [pathlib.Path(chemin) for chemin in sys.argv[1:]]
    lignes = erreurs_compilation(journaux) + echecs_de_tests()

    if not lignes:
        annoter(
            "Le build a échoué sans erreur reconnaissable : consultez le journal complet "
            "de l'étape en échec."
        )
        lignes.append("| ? | — | Échec sans erreur identifiable dans le journal |")

    recap = os.environ.get("GITHUB_STEP_SUMMARY")
    if recap:
        with open(recap, "a", encoding="utf-8") as sortie:
            sortie.write("## Échecs détectés\n\n")
            sortie.write("| Nature | Où | Message |\n|---|---|---|\n")
            for ligne in lignes[:LIMITE]:
                sortie.write(ligne + "\n")
            if len(lignes) > LIMITE:
                sortie.write(f"\n_… et {len(lignes) - LIMITE} autre(s)._\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
