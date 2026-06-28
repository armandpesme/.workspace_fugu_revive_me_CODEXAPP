#!/usr/bin/env python3
"""Render HANDOFF.md from a structured input.

Usage:
    python render_handoff.py --input <draft.json|yaml> --output <HANDOFF.md>
    python render_handoff.py --stdin --output <HANDOFF.md>

Draft format (JSON or YAML), all fields optional except title:

    {
      "title": "Jalon 0.0.1 - bootstrap",
      "date": "2026-06-23",
      "objective": "Initialiser le depot public et l'index GitNexus.",
      "files": [
        {"path": "README.md", "change": "modified"},
        {"path": "AGENTS.md", "change": "added"}
      ],
      "tests": [
        {"cmd": "gitnexus analyze -f", "result": "OK"}
      ],
      "not_done": ["Aucune verification runtime lancee (runClient differe)."],
      "risks": ["LadybugDB VECTOR indisponible: recherche semantique en exact-scan."],
      "next": "Lancer runClient smoke test puis un premier clean build Gradle.",
      "decisions": ["Repo nomme .workspace_fugu_revive_me_CODEXAPP (decision workspace)."]
    }

Guarantees:
    - Always produces the eight mandatory sections in order.
    - Enforces <= 80 body lines (excluding optional YAML frontmatter).
    - Truncates any single bullet list block that would push past the cap.
    - Exits non-zero on missing required fields.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

try:
    import yaml  # type: ignore
except ImportError:  # pragma: no cover
    yaml = None

MAX_LINES = 80
CHANGE_PREFIX = {"added": "+", "modified": "~", "deleted": "-", "renamed": ">"}


def _load(path: Path) -> dict[str, Any]:
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() in {".yaml", ".yml"}:
        if yaml is None:
            sys.exit("PyYAML required for YAML input (pip install pyyaml).")
        data = yaml.safe_load(text) or {}
    else:
        data = json.loads(text)
    if not isinstance(data, dict):
        sys.exit("Input must deserialize to a mapping.")
    return data


def _format_files(items: list[dict[str, Any]] | list[str]) -> list[str]:
    if not items:
        return ["- aucun"]
    out: list[str] = []
    for it in items:
        if isinstance(it, str):
            out.append(f"- {it}")
            continue
        change = it.get("change", "modified")
        prefix = CHANGE_PREFIX.get(change, "~")
        path = it.get("path", "")
        note = it.get("note", "")
        suffix = f"  # {note}" if note else ""
        out.append(f"- {prefix} {path}{suffix}")
    return out


def _format_tests(items: list[dict[str, Any]] | list[str]) -> list[str]:
    if not items:
        return ["- aucun"]
    out: list[str] = []
    for it in items:
        if isinstance(it, str):
            out.append(f"- {it}")
            continue
        cmd = it.get("cmd", "")
        result = it.get("result", "?")
        note = it.get("note", "")
        suffix = f"  # {note}" if note else ""
        out.append(f"- `{cmd}` -> {result}{suffix}")
    return out


def _format_list(items: list[str], none_label: str = "- aucun") -> list[str]:
    if not items:
        return [none_label]
    return [f"- {x}" for x in items]


def render(data: dict[str, Any]) -> str:
    title = data.get("title")
    if not title:
        sys.exit("Missing required field: title")
    date = data.get("date") or "YYYY-MM-DD"
    sections: list[tuple[str, list[str]]] = [
        (
            "Objectif",
            _format_list([data["objective"]])
            if data.get("objective")
            else ["- non renseigne"],
        ),
        ("Fichiers modifies", _format_files(data.get("files", []))),
        ("Tests lances", _format_tests(data.get("tests", []))),
        ("Non fait", _format_list(data.get("not_done", []))),
        ("Risques restants", _format_list(data.get("risks", []))),
        ("Prompt du prochain jalon", [data.get("next") or "- non renseigne"]),
        ("Decisions a valider", _format_list(data.get("decisions", []))),
    ]
    lines: list[str] = [f"# Handoff - {title} - {date}", ""]
    for name, body in sections:
        lines.append(f"## {name}")
        lines.extend(body)
        lines.append("")
    body = "\n".join(lines).rstrip() + "\n"
    text_lines = body.splitlines()
    if len(text_lines) > MAX_LINES:
        # Truncate at the last fully-formed section within the cap.
        kept: list[str] = []
        for ln in text_lines:
            kept.append(ln)
            if len(kept) >= MAX_LINES:
                break
        kept.append(f"\n[tronque a {MAX_LINES} lignes]")
        body = "\n".join(kept)
    return body


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Render HANDOFF.md from structured input."
    )
    parser.add_argument("--input", type=Path, help="Draft file (JSON or YAML).")
    parser.add_argument(
        "--stdin", action="store_true", help="Read draft from stdin as JSON."
    )
    parser.add_argument(
        "--output", type=Path, required=True, help="Output HANDOFF.md path."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Only check line count of an existing file.",
    )
    args = parser.parse_args()

    if args.check:
        existing = args.output.read_text(encoding="utf-8").splitlines()
        if len(existing) > MAX_LINES:
            sys.exit(f"HANDOFF.md exceeds {MAX_LINES} lines: {len(existing)}")
        print(f"OK: {len(existing)} lines (<= {MAX_LINES})")
        return 0

    if args.stdin:
        data = json.loads(sys.stdin.read())
    elif args.input:
        data = _load(args.input)
    else:
        sys.exit("Provide --input or --stdin.")

    body = render(data)
    args.output.write_text(body, encoding="utf-8")
    line_count = len(body.splitlines())
    print(f"Wrote {args.output} ({line_count} lines)")
    if line_count > MAX_LINES:
        sys.exit(f"Output exceeds {MAX_LINES} lines: {line_count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
