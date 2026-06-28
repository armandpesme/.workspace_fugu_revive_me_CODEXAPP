# AGENTS.md

## Role

Ce fichier est la source de verite des agents pour ce workspace type Minecraft Forge. Il contient les consignes que les agents doivent charger a chaque session: architecture, workflow, commandes, limites de securite et conventions Forge. Mais elle peuvent etre ajuster et enrichie dans `./README.md`.
Pour tout question sur Environnement,Structure,Objectif,Contexte,Contraintes,Conditions d'Acceptation du project lire obligatoirement [README.md](./README.md)

Ne jamais déduire ou inventer  lorsque l’information existe dans
ces fichiers.

## Priorité des sources

1. Instruction humaine du fil courant.
2. `.codex/config.toml`pour Codex App (ou `.github/copilot-instructions.md` pour GitHub Copilot).
3. `./README.md`.
3. Ce fichier `./AGENTS.md`.
4. Configuration local.
4. Configuration globale.

## Objectif métier

read readme.md et plan.md pour comprendre le contexte, les objectifs métier, les règles de travail, le workflow Codex, les commandes de vérification, la configuration parallèle `.github`/`.codex`/`.antigravity`/`.claude`, les agents, skills et prompts disponibles.

## Règles de travail

- Pendant le développement, les mixins critiques doivent être configurés en mode strict : en cas d’échec, le jeu doit crash afin de générer un log exploitable et d’identifier le mixin responsable. (a retier avant le clean build)
- Ce garde-fou est retiré uniquement juste avant le clean build/release.
- Répondre en français.
- Avant modification : lire les fichiers concernés et chercher les usages avec `rg`/recherche locale.
- Préférer les patterns existants du mod ; éviter les abstractions inutiles.
- Ne jamais inventer une API Forge/Minecraft : vérifier localement, puis docs officielles ou exemples fiables si incertain.
- GitHub, dépôt distant, commits, branches, push/pull et backup sont autorisés quand la demande humaine les vise explicitement ou après build vérifié. Respecter le scope de la tâche, ne pas écraser les changements humains non compris et ne jamais force-push/reset/rebase/clean destructif sans demande explicite.
- Ne pas écraser de changements humains non compris.
- Garder les modifications dans `project-gradle/` sauf demande ou doc/procédure explicitement ciblée.
- Séparer strictement code `client`, `server` et `common`.
- Toute synchro client/serveur passe par packet ou capability explicite.
- Ne pas utiliser Fabric, Loom, NeoForge, Yarn ni anciennes APIs de registry.
- GeckoLib seulement si déjà présent ou explicitement requis par le jalon.
- Ne fais jamais confiance uniquement à ta mémoire interne.
- Vérifie toujours tes connaissances sur Internet, dans le workspace et dans les fichiers du projet.
- Ta mémoire interne peut être dépréciée ou incorrecte.
- Toute décision doit être fondée sur des sources vérifiées et une réflexion explicite.
- LLM/IA/agents utiliser en fonction de l'ADE ou IDE ou CLI dans le quel il se trouve il doit utiliser le dossier correspondant : `.github` ou `.codex` ou `.antigravity` ou `.claude`.

## Droit d’initiative contrôlé

L’agent peut prendre des décisions techniques locales lorsque cela permet de terminer le jalon sans modifier le design validé.

Il peut corriger un problème hors périmètre uniquement si cette correction est nécessaire pour :

* faire compiler le projet ;
* débloquer le jalon en cours ;
* corriger une erreur directement causée par ses modifications ;
* éviter un bug évident et limité.

Cependant, si une décision peut modifier le design, le gameplay, l’architecture générale, les documents protégés ou le périmètre du jalon, l’agent doit s’arrêter et poser une question à Armand.

La question doit être courte, cadrée et proposer plusieurs choix.

Format recommandé :

```md
Question pour validation :

Contexte :
...

Choix recommandés :
A. ...
B. ...
C. ...

Recommandation de l’agent :
...

Impact si on choisit A :
...
Impact si on choisit B :
...
Impact si on choisit C :
...
```

L’agent doit privilégier une question de validation plutôt qu’une décision autonome lorsqu’il existe un risque de dérive fonctionnelle.

Si l’agent continue sans poser de question, il doit documenter la décision prise dans `end-off.md`.

## Workflow Codex

- Utiliser les fichiers locaux et `rg` en premier niveau pour identifier les fichiers, symboles et configurations concernés.
- Si GitNexus est disponible, l’utiliser en deuxième niveau pour analyser l’architecture, les dépendances, les chaînes d’appel et l’impact potentiel des modifications.
- Utiliser la recherche web ou les autres MCP externes uniquement via une compétence ou un agent dédié lorsque les sources locales et GitNexus ne suffisent pas.
- Pour un changement de build, lancer une tâche Gradle pertinente.
- Pour une modification Java, compiler au minimum lorsque cela est raisonnable.
- Pour les assets et le datagen, vérifier les chemins, le `mod_id` et la validité des fichiers JSON.
- Si une vérification n’est pas exécutée, le signaler clairement.

## Documents protégés

Les fichiers suivants sont des sources de vérité et ne doivent pas être modifiés automatiquement par l’agent :

* `DESIGNE.md`
* `AGENTS.md`
* `README.md`

L’agent peut les lire et s’y référer, mais ne doit pas les modifier sauf demande explicite d’Armand.

Le fichier de suivi `HANDOFF.md` peut être mis à jour uniquement lorsqu’Armand le demande ou lorsqu’une commande/skill dédiée au handoff est utilisée.

Si une règle du design semble incorrecte, incomplète ou incompatible avec le code, l’agent doit le signaler dans `HANDOFF.md` ou dans sa réponse, sans modifier directement les documents protégés.

## EXEE Plan

- Plan canonique : `.agent/PLAN.md`.
- Le plan doit rester autosuffisant mais compact : état actuel, décisions actives, découvertes utiles, prochaine action.
- Après chaque `Gradle build` ou `clean build` réussi : mettre à jour `.agent/PLAN.md` avant la réponse finale avec commande, résultat, JAR produit si applicable et prochaine étape.
- Purger ou condenser l'historique terminé quand il n'aide plus la reprise.

## Verification

Depuis `project-gradle/`:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Utiliser `clean build` seulement sur demande explicite ou finalisation:

```powershell
.\gradlew.bat clean build
```

Regles de verification:

- Java: compiler au minimum; preferer `build` si plusieurs surfaces ou une integration Forge sont touchees.
- Gradle/TOML metadata: lancer une verification Gradle pertinente quand raisonnable.
- JSON/assets/data: valider le JSON, les namespaces, les chemins et la coherence avec les registry IDs.
- Runtime/crash: lire `logs/`, `crash-reports/` et charger `minecraft-log-tools` / `forge-runtime-debug` avant de corriger.
- Apres chaque `build` ou `clean build` Gradle reussi: mettre a jour `.agent/PLAN.md`, puis fournir un lien cliquable vers le JAR de `project-gradle/build/libs/`.
- Si une verification raisonnable n'est pas lancee, le dire explicitement avec la raison.

## Agents, prompts et skills

Agents GitHub Copilot: `.github/agents/*.agent.md`. Miroir Codex: `.codex/agents/*.toml`.

Agents locaux disponibles: `maestro`, `planner`, `explorer`, `architect`, `gameplay-engineer`, `geckolib-epicfight-compat`, `snipper`, `asset-manager`, `debugger`, `qa`, `runner`, `back-up-github`, `explorer-github`.

Prompts disponibles: `.github/prompts/forge-fix.prompt.md`, `.github/prompts/forge-plan.prompt.md`, `.github/prompts/forge-review.prompt.md`; miroirs Codex dans `.codex/prompts/`.

Skills actifs: lire le `SKILL.md` concerne dans `.agents/skills/<nom>/` au moment ou sa description correspond a la tache. Ne pas recopier les instructions de skills dans ce fichier. Skill local ajoute: `blockbench-modeling` pour Blockbench MCP.

Catalogue par familles:

- Cadrage, reprise, suivi: `braintime`, `forge-workspace-onboarding`, `exee-plan`, `handoff`, `retrospective-loop`, `workspace-agent-skill-maintenance`.
- Forge/Minecraft: `explore-doc`, `mod-architecture`, `forge-edit-scope`, `forge-build-check`, `forge-runtime-debug`, `forge-client-server-boundaries`, `forge-assets-integrity`, `forge-mixin-safety`, `forge-qa`, `minecraft-log-tools`, `forge-armor-variant-port`.
- GitNexus: `gitnexus-cli`, `gitnexus-guide`, `gitnexus-exploring`, `gitnexus-debugging`, `gitnexus-impact-analysis`, `gitnexus-refactoring`.
- Fichiers et formats: `context7-mcp`, `jq-json-patch`, `multi-format-patch`, `py-complex-patch`.

`.agents/skills/` est la source active commune. `.codex/skills/` est un miroir de compatibilite. `.github/skills/` reste vide pour eviter une double decouverte.

## 4. Spécifications techniques

Utiliser le système du projet existant.
Respect des IDs qui ont été choisis, à l'exception de la version que on va pouvoir modifier pour l'augmenter. On est passé sur une nouvelle version de ce mod.
Privilégier les outils efficaces et peu coûteux en tokens.

Utiliser en priorité :

- le terminal PowerShell ;
- les outils intégrés fiables ;
- les méthodes déjà éprouvées dans le projet.

## 5. Note sur l'Exécution

**Liberté Totale de Méthode :** Vous disposez d'une liberté absolue quant à la stratégie technique et à la manière de procéder pour accomplir ces tâches. Aucune procédure spécifique n'est imposée sur le "comment". Le seul critère d'évaluation est le succès complet de la mission et la qualité de l'intégration finale.

**Respect de l'existant :** Assurez-vous de ne pas altérer les contenu (armures) existantes du mod et de maintenir la cohérence avec les éléments déjà intégrés.

## Sources de référence

Privilégie les documents officiels, GitHub, Reddit, Curseforge Modrinth, Discord et Patreon.

### Règles pour l'agent de planification de projet :

-Lire le workspace avant toute décision.
Faire des recherches fiables avant de raisonner.
-Poser au moins 10 questions à choix + réponse personnalisée.
P-oser deux questions finales juste avant le plan.
-Écrire le plan uniquement si l’agent s’autoévalue à 95 % de confiance sur la qualité, la faisabilité et la compréhension.
-Le plan.md doit être autosuffisant pour qu'un n'importe quel IA puisse faire le projet avec uniquement ça.
-Le plan qui a été validé doit être rédigé et édité le fichier plan.md qui est dans .agent

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **.workspace_fugu_revive_me_CODEXAPP** (1513 symbols, 4029 relationships, 108 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "master"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/.workspace_fugu_revive_me_CODEXAPP/context` | Codebase overview, check index freshness |
| `gitnexus://repo/.workspace_fugu_revive_me_CODEXAPP/clusters` | All functional areas |
| `gitnexus://repo/.workspace_fugu_revive_me_CODEXAPP/processes` | All execution flows |
| `gitnexus://repo/.workspace_fugu_revive_me_CODEXAPP/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
