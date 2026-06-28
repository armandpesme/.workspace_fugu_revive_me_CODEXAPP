---
description: "Mise a jour de HANDOFF.md: resume court et factuel d'un jalon (80 lignes max). Declenchee par /handoff uniquement."
mode: subagent
permission:
  edit: allow
  bash: allow
  task: deny
  webfetch: deny
  read: allow
  glob: allow
  grep: allow
  skill: allow
  question: deny
---

# Handoff — Resume court de jalon

## Role

Produire ou mettre a jour `HANDOFF.md` a la racine du workspace a partir d'un jalon termine. Format strict, factuel, 80 lignes max.

## Declenchement

Utiliser seulement si l'utilisateur invoque `/handoff`, `$handoff` ou demande clairement un handoff. Ne pas declencher implicitement.

## Sortie

- Fichier: `HANDOFF.md` (racine du workspace).
- Maximum 80 lignes, ecrase la version precedente.
- Sections obligatoires dans l'ordre:
  1. `# Handoff - <jalon> - <YYYY-MM-DD>`
  2. `## Objectif`
  3. `## Fichiers modifies` (prefixes `+`/`~`/`-`)
  4. `## Tests lances` (commandes reellement executees)
  5. `## Non fait`
  6. `## Risques restants`
  7. `## Prompt du prochain jalon` (3 a 8 lignes)
  8. `## Decisions a valider`

## Procedure

1. Lire `.agent/PLAN.md` (si present) pour le contexte.
2. Lire `git status --porcelain` et `git diff --name-only HEAD~1` (ou `HEAD`) pour la liste reelle des fichiers.
3. Identifier les commandes reellement lancees dans la session (logs, historique).
4. Completer chaque section a partir de faits, pas d'intentions.
5. Si une information manque, l'ecrire dans `Non fait` ou `Risques restants`.
6. Verifier la ligne finale: `wc -l HANDOFF.md` doit retourner `<= 80`.
7. Optionnel: utiliser le script `python3 .agents/skills/handoff/scripts/render_handoff.py --input draft.json --output HANDOFF.md` pour garantir le format.

## Garanties

- Aucune information sensible (token, cle, mot de passe).
- Ne pas dupliquer `.agent/PLAN.md`; `HANDOFF.md` est un resume court.
- Si le workspace n'est pas Git, ignorer les commandes Git et utiliser le diff observe.
- Ne pas modifier le design global sauf si une decision validee est referencee dans `Decisions a valider`.

## Skills

`handoff` (canonique dans `.agents/skills/handoff/`).
