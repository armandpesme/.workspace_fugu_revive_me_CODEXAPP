---
name: handoff
description: "Mise a jour de HANDOFF.md a la racine: resume court et factuel d'un jalon (fichiers modifies, tests reellement lances, non-fait, risques, prompt du prochain jalon). Declenchee explicitement par /handoff, $handoff ou demande claire de handoff. Invocation explicite uniquement; ne pas declencher implicitement. Ne modifie pas le design global sauf si une decision validee le necessite."
license: MIT
compatibility: codex,opencode,copilot,antigravity
metadata:
  short-description: Resume court de jalon dans HANDOFF.md (80 lignes max).
  source: workspace
  adapted-for: forge-minecraft
---

# Skill: handoff

## Activation stricte

- Utiliser cette skill seulement si l'utilisateur invoque explicitement `/handoff`, `$handoff`, ou demande clairement de generer un handoff.
- Ne pas l'activer implicitement pour une demande vague.
- Dans Codex App, `agents/openai.yaml` doit garder `policy.allow_implicit_invocation: false`.

## Sortie obligatoire

- Fichier: `HANDOFF.md` a la racine du workspace.
- Maximum 80 lignes, hors entete YAML facultative.
- Format Markdown, sections fixes, pas de narration.
- Ecrase la version precedente, ne conserve pas l'historique.
- Ne pas modifier le design global (UI, modeles, structures) sauf si une decision validee est referencee.

## Sections obligatoires dans l'ordre

1. `# Handoff - <jalon court> - <YYYY-MM-DD>`
2. `## Objectif`: une phrase factuelle, sans narration.
3. `## Fichiers modifies`: chemins relatifs, un par ligne, prefixe par le type (`+` ajoute, `~` modifie, `-` supprime). Si vide, marquer `- aucun`.
4. `## Tests lances`: commandes reellement executees avec resultat (`OK` / `FAIL` / `SKIP`). Ne pas lister les tests non lances. Si aucun, marquer `- aucun`.
5. `## Non fait`: points du plan initial non traites ou partiellement traites. Sinon `- aucun`.
6. `## Risques restants`: bug, dette, regression possible, depense, ou dependance externe. Sinon `- aucun`.
7. `## Prompt du prochain jalon`: 3 a 8 lignes, actionnable, factual, sans question ouverte.
8. `## Decisions a valider`: choix pris en supposition qui demandent validation explicite. Sinon `- aucune`.

## Procedure

1. Lire `.agent/PLAN.md` (si present) pour le contexte du jalon.
2. Lire `git status --porcelain` et `git diff --name-only HEAD~1` (ou `HEAD` si premier commit) pour la liste reelle des fichiers.
3. Lire `git diff --stat` pour la portee.
4. Identifier les commandes reellement lancees dans la session courante (logs, historique, sortie agent). Ne jamais inventer une commande.
5. Completer chaque section a partir de faits, pas d'intentions.
6. Si une information manque (ex: tests non lances, decision implicite), l'ecrire dans `Non fait` ou `Risques restants`, pas dans une autre section.
7. Verifier la ligne finale: `wc -l HANDOFF.md` doit retourner `<= 80` (entete YAML facultative exclue).
8. Pour Codex: utiliser le script `scripts/render_handoff.py` si un brouillon est deja disponible, afin de garantir le format.

## Garanties

- Aucune information sensible (token, cle, mot de passe) ne doit apparaitre.
- Ne pas dupliquer `.agent/PLAN.md`; `HANDOFF.md` est un resume court, pas un plan.
- Si le workspace n'est pas un depot Git, ignorer les commandes Git et utiliser le diff observe dans la session.
- Si un jalon precedent existe dans `HANDOFF.md`, l'ecraser entierement.
