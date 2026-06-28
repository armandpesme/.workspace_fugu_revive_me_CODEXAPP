#!/usr/bin/env python3
"""
Validateur local pour les questions QCM émises par le skill doubt-qcm-pause.

Vérifie qu'un QCM suit les conventions du skill :
  - 2 a 4 options (+ "Autre" ajoutee par l'outil question)
  - une seule option "(Recommandé)"
  - labels 1-5 mots
  - header <= 30 caracteres
  - question terminee par "?"
  - contexte precede la question

Usage:
  python validate_qcm.py --stdin       (lit le YAML depuis stdin)
  python validate_qcm.py path/to/qcm.yaml
  python validate_qcm.py --self-check  (valide le QCM exemple)
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Tuple

try:
    import yaml
except ImportError:
    print("PyYAML requis. Installer avec: pip install pyyaml", file=sys.stderr)
    sys.exit(2)


MAX_HEADER = 30
MAX_OPTIONS = 4
MIN_OPTIONS = 2
MAX_LABEL_WORDS = 5
RECO_SUFFIX = "(Recommandé)"


def split_question_context(question: str) -> Tuple[str, str]:
    """Sépare le contexte (avant la dernière phrase interrogative) de la question."""
    sentences = re.split(r"(?<=[.!?])\s+", question.strip())
    if not sentences:
        return "", question
    if sentences[-1].rstrip().endswith("?"):
        if len(sentences) == 1:
            return "", sentences[0]
        return " ".join(sentences[:-1]), sentences[-1]
    return " ".join(sentences), ""


def validate_qcm(data: dict) -> List[str]:
    """Retourne la liste des erreurs ; vide si OK."""
    errors: List[str] = []

    if not isinstance(data, dict):
        return ["Le QCM doit être un dictionnaire YAML."]

    question = data.get("question", "")
    if not isinstance(question, str) or not question.strip():
        errors.append("Champ 'question' manquant ou vide.")
    else:
        context, actual_q = split_question_context(question)
        if not actual_q:
            errors.append(
                "La question doit se terminer par '?'. "
                f"Reçu : {question[:80]!r}"
            )
        if not context and len(question.split()) > 20:
            errors.append(
                "Pas de contexte détecté (question > 20 mots). "
                "Préférer : 1 phrase de contexte + 1 question."
            )

    header = data.get("header", "")
    if not isinstance(header, str):
        errors.append("Champ 'header' doit être une chaîne.")
    elif len(header) > MAX_HEADER:
        errors.append(
            f"Header trop long ({len(header)}/{MAX_HEADER} chars) : {header!r}"
        )

    options = data.get("options", [])
    if not isinstance(options, list):
        errors.append("Champ 'options' doit être une liste.")
        return errors

    if len(options) < MIN_OPTIONS:
        errors.append(
            f"Trop peu d'options ({len(options)} < {MIN_OPTIONS}). "
            "Minimum 2 (l'outil 'question' ajoute 'Autre' automatiquement)."
        )
    if len(options) > MAX_OPTIONS:
        errors.append(
            f"Trop d'options ({len(options)} > {MAX_OPTIONS}). "
            "Voir references/anti-patterns.md (A7)."
        )

    reco_count = 0
    for idx, opt in enumerate(options):
        if not isinstance(opt, dict):
            errors.append(f"Option #{idx + 1} : doit être un dictionnaire.")
            continue
        label = opt.get("label", "")
        if not isinstance(label, str) or not label.strip():
            errors.append(f"Option #{idx + 1} : 'label' manquant ou vide.")
            continue
        word_count = len(label.split())
        if word_count > MAX_LABEL_WORDS:
            errors.append(
                f"Option #{idx + 1} : label trop long "
                f"({word_count} mots > {MAX_LABEL_WORDS}) : {label!r}"
            )
        if RECO_SUFFIX in label:
            reco_count += 1
        desc = opt.get("description", "")
        if not isinstance(desc, str) or not desc.strip():
            errors.append(
                f"Option #{idx + 1} : 'description' manquante ou vide."
            )

    if reco_count != 1:
        errors.append(
            f"Exactement 1 option doit porter le suffixe '{RECO_SUFFIX}' "
            f"(trouvé : {reco_count})."
        )

    if reco_count == 1 and options and RECO_SUFFIX not in options[0].get("label", ""):
        errors.append(
            f"L'option recommandée doit être en première position "
            f"(suffixe '{RECO_SUFFIX}' trouvé sur une option non-première)."
        )

    return errors


def self_check_example() -> List[str]:
    """Valide le QCM d'exemple canonique."""
    example = {
        "question": (
            "Le projet liste Forge 47.2.0 dans mods.toml mais le build résout "
            "Effectivement 47.3.0 en raison du classpath Gradle. "
            "Quelle version dois-je cibler pour l'API EntityJoinWorldEvent ?"
        ),
        "header": "Version Forge",
        "options": [
            {
                "label": "Forcer 47.2.0 (Recommandé)",
                "description": (
                    "Aligne mods.toml avec le classpath effectif, "
                    "build reproductible."
                ),
            },
            {
                "label": "Migrer vers 47.3.0",
                "description": (
                    "Mise à jour API, vérification mixins, "
                    "risque régression 1-2 jours."
                ),
            },
            {
                "label": "Garder les deux",
                "description": "Impossible : un seul mods.toml.",
            },
        ],
    }
    return validate_qcm(example)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Valide un QCM émis par le skill doubt-qcm-pause."
    )
    parser.add_argument(
        "path",
        nargs="?",
        help="Chemin vers un fichier YAML décrivant le QCM.",
    )
    parser.add_argument(
        "--stdin",
        action="store_true",
        help="Lit le YAML depuis stdin.",
    )
    parser.add_argument(
        "--self-check",
        action="store_true",
        help="Valide le QCM d'exemple canonique.",
    )
    args = parser.parse_args()

    if args.self_check:
        errors = self_check_example()
        if errors:
            print("ERREURS :", file=sys.stderr)
            for e in errors:
                print(f"  - {e}", file=sys.stderr)
            return 1
        print("OK - QCM d'exemple valide.")
        return 0

    if args.stdin:
        raw = sys.stdin.read()
    elif args.path:
        raw = Path(args.path).read_text(encoding="utf-8")
    else:
        parser.print_help(sys.stderr)
        return 2

    try:
        data = yaml.safe_load(raw)
    except yaml.YAMLError as e:
        print(f"YAML invalide : {e}", file=sys.stderr)
        return 1

    errors = validate_qcm(data if isinstance(data, dict) else {})
    if errors:
        print("ERREURS :", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1
    print("OK - QCM valide.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
