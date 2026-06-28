---
name: doubt-qcm-pause
description: >-
  Met l'IA en pause et pose une question à choix multiples (QCM) à l'utilisateur
  dès qu'un doute, une ambiguïté, un risque de dérive fonctionnelle, un choix
  de design, un conflit de versions, une option métier non tranchée, ou toute
  décision pouvant modifier le périmètre/la qualité du livrable est détecté.
  Utiliser quand l'agent doit clarifier avant d'agir pour éviter une
  implémentation incorrecte, scope creep, ou rupture de design validé, et
  avant tout choix irréversible (rename, suppression, refactor large, push,
  release). À déclencher sur demande explicite (utilisateur hésitant, "pose
  une question", "j'attends ta validation") ou implicitement par l'agent
  lorsqu'il identifie un doute matériel. Complète AGENTS.md (Droit
  d'initiative contrôlé) en formalisant le canal de validation.
license: MIT
compatibility: copilot,opencode,antigravity,codex
metadata:
  short-description: Pause and ask the user a QCM when in doubt
  source: local-workspace
  adapted-for: forge-minecraft
  triggers: ambiguity, design choice, scope risk, irreversible action, missing spec
  variant: codex
  mirror-of: .agents/skills/doubt-qcm-pause
  codex-ui: agents/openai.yaml (interface + policy)
---

# Doubt QCM Pause

## Objectif

Ce skill formalise la règle du **Droit d'initiative contrôlé** définie dans
`AGENTS.md` : dès que l'agent identifie un doute matériel qui pourrait modifier
le design, le périmètre, la qualité ou la trajectoire du livrable, il **met
en pause son action**, **pose une question QCM structurée** à l'utilisateur,
**attend la réponse**, puis **reprend** avec la décision intégrée.

But : transformer chaque doute en une décision explicite, traçable et
réversible, afin de maximiser la probabilité de réussite de l'implémentation
ou de la réalisation du projet.

## Quand déclencher la pause

Déclencher ce skill **dès qu'au moins un** de ces signaux est détecté :

### Signaux techniques
- **API incertaine** : signature Forge/Minecraft/GeckoLib/Epic Fight non
  vérifiée localement, version Yarn/MCP/Forge non confirmée, comportement
  runtime non documenté.
- **Conflit de versions** : plusieurs versions plausibles d'une dépendance,
  d'un mixin, d'un mapping, d'un format JSON.
- **Incompatibilité pressentie** : code client appelé côté serveur, classe
  `common` utilisant une API dédiée, capability partagée sans packet.
- **Mixin critique** : modification d'un mixin sensible (refmap, accessor,
  access transformer) qui peut crasher le jeu si mal configuré.
- **Override de méthode vanilla** : nom, signature ou comportement à
  confirmer avant de surcharger.

### Signaux fonctionnels
- **Ambiguïté de design** : plusieurs interprétations viables du besoin, le
  choix modifie le gameplay, l'UI, ou l'architecture.
- **Critère d'acceptation flou** : "faire mieux", "plus rapide", "plus
  joli", sans seuil mesurable.
- **Périmètre flou** : feature demandée dont on ne sait pas si elle est
  in/out of scope du jalon.
- **Choix métier** : nom d'item, ID de registre, tag de mod, langue par
  défaut, palette de couleurs, équilibrage.

### Signaux de risque
- **Action irréversible** : suppression de fichier, `git reset --hard`,
  `git push -f`, clean build destructif, rebase, migration de schéma.
- **Blast radius élevé** : modification d'un fichier de configuration
  partagé, refactor d'une classe utilisée par N callers (vérifier via
  `gitnexus_impact`).
- **Conflit humain probable** : zone où l'utilisateur a déjà explicité un
  choix (AGENTS.md, README, HANDOFF, .agent/PLAN.md) et la proposition
  actuelle le contredit.
- **Doute non résolu après recherche** : après `rg`, `context7`, exploration
  GitNexus, une information critique reste manquante ou contradictoire.

### Demande explicite utilisateur
- L'utilisateur dit "pose une question", "j'attends ta validation",
  "demande-moi avant", "dis-moi ce que tu hésites", "demande confirmation",
  "check avec moi".

## Quand NE PAS déclencher

- **Questions triviales** : orthographe d'un log, nom d'un paramètre local,
  format de date, choix déjà documenté dans le code.
- **Décisions réversibles et locales** : nom d'une variable locale, choix
  d'un opérateur, micro-optimisation sans impact design.
- **Cas couverts par AGENTS.md** : si la règle est explicite et non
  contredite (ex : "GeckoLib seulement si déjà présent"), ne pas
  re-questionner.
- **Re-running d'une décision déjà validée** : si l'utilisateur a déjà
  tranché un point dans la session ou dans `.agent/PLAN.md`, l'appliquer.

En cas de doute sur l'opportunité de déclencher : **déclencher**. Le coût
d'une question inutile est très inférieur au coût d'une implémentation
incorrecte.

## Format de la question QCM

Utiliser l'outil natif `question` de l'environnement. Structure :

1. **Une seule question à la fois** par appel `question`. Ne pas empiler
   plusieurs décisions dans un même QCM.
2. **Header (max 30 chars)** : étiquette courte de la catégorie (ex :
   "Mixin critique", "Périmètre jalon", "ID item", "Version Forge").
3. **Question** : phrase complète, terminée par `?`, contexte en 1 phrase
   au-dessus si nécessaire.
4. **2 à 4 options** : libellé (1-5 mots) + description courte expliquant
   l'implication technique ou fonctionnelle.
5. **Toujours proposer** : une option **"Autre / réponse personnalisée"**
   (ajoutée automatiquement par l'outil `question`).
6. **Recommandation explicite** : la première option doit être la
   recommandation de l'agent, marquée **(Recommandé)** en suffixe.

### Modèle (voir `references/qcm-template.md`)

```text
question: "<contexte 1 phrase>

<question claire et binaire ou à choix fermé ?>"

options:
  - label: "<Action recommandée>" (Recommandé)
    description: "<Implication concrète : fichiers touchés, risque, réversibilité>"
  - label: "<Alternative 1>"
    description: "<Implication concrète>"
  - label: "<Alternative 2>"
    description: "<Implication concrète>"
  - label: "<Reporter la décision>"
    description: "<Conséquence : point à trancher plus tard dans .agent/PLAN.md>"
```

## Workflow obligatoire

### Étape 1 — Détection

Identifier le doute. Citer explicitement dans la réponse à l'utilisateur
**pourquoi** la pause est déclenchée (référence à AGENTS.md, signal
technique, fonctionnel ou de risque).

### Étape 2 — Pré-collecte silencieuse

Avant d'appeler `question`, l'agent doit avoir fait **au minimum** :

1. Lu les fichiers locaux concernés (`rg`, lecture ciblée).
2. Vérifié `.agent/PLAN.md`, `AGENTS.md`, `README.md` pour une décision
   déjà prise.
3. Si l'API est incertaine : tenté une vérification `context7` ou
   GitNexus (`gitnexus_query` / `gitnexus_impact`).
4. Construit mentalement 2 à 4 options viables avec leurs implications.

Si la pré-collecte révèle que la question est triviale ou déjà tranchée,
**ne pas appeler** `question` : appliquer directement et le dire
explicitement.

### Étape 3 — Question QCM

Appeler l'outil `question` avec une **seule** question structurée. Si
plusieurs doutes indépendants existent, **les poser en plusieurs
questions successives**, jamais en un seul gros QCM.

### Étape 4 — Attente

Ne pas continuer le travail tant que la réponse n'est pas reçue. Ne pas
deviner l'intention. Ne pas auto-substituer la première option si
l'utilisateur n'a pas cliqué.

### Étape 5 — Intégration

Une fois la réponse obtenue :

1. Reformuler brièvement la décision retenue (1-2 phrases).
2. Documenter la décision dans `.agent/PLAN.md` (section "Journal des
   décisions") ou `HANDOFF.md` si le jalon l'exige.
3. Reprendre le travail avec la décision intégrée.
4. Si la décision invalide le plan : mettre à jour `.agent/PLAN.md`
   avant de continuer.

### Étape 6 — Tracé

Toute pause QCM doit laisser une trace :

- soit dans `.agent/PLAN.md` (Décisions / Questions résolues) ;
- soit dans la réponse texte si la session est courte ;
- soit dans `HANDOFF.md` lors d'un handoff.

## Escalade

Si l'utilisateur ne sait pas répondre ou délègue :

1. Proposer un **défaut sûr** (option la moins irréversible, conforme à
   AGENTS.md et au design validé).
2. Documenter explicitement la délégation dans `.agent/PLAN.md` (section
   "Hypothèses et points à vérifier").
3. Poursuivre avec le défaut en l'étiquetant clairement comme "décision
   par défaut à confirmer".

Si l'utilisateur est en colère, pressé, ou contradictoire :

1. **Stopper** l'action.
2. Poser **une** question de cadrage : "Tu veux que je [option A] ou
   [option B] ?".
3. Ne pas présumer.

## Bonnes pratiques

- **Une décision = une question**. Ne jamais poser 5 décisions en un seul
  appel.
- **Court et actionnable**. Header < 30 chars, label 1-5 mots, description
  1 phrase.
- **Recommandation visible**. Toujours indiquer la recommandation de
  l'agent en première option avec le suffixe `(Recommandé)`.
- **Contexte avant la question**. 1 phrase de contexte, puis la question.
- **Pas de double QCM**. Si tu hésites entre 2 formulations, choisis-en
  une. La question parfaite n'existe pas, la question posée vaut mieux
  que la question évitée.
- **Tracé obligatoire**. Une décision non documentée est une décision
  perdue.

## Anti-patterns

Voir `references/anti-patterns.md` pour la liste complète. Les plus
fréquents :

- **Empiler 6 questions dans un seul appel** `question` → l'utilisateur
  décroche.
- **Poser une question alors que la décision est déjà dans AGENTS.md** →
  perte de temps, frustration.
- **Deviner l'option (A) sans attendre** → auto-validation, défaite du
  skill.
- **Poser une question sur un point trivial** (nom de variable locale) →
  bruit, l'utilisateur perd confiance.
- **Ne pas documenter la décision retenue** → l'agent suivant ne peut
  pas reprendre.

## Références

- **Modèle QCM complet** : `references/qcm-template.md`
- **Catalogue de situations déclencheuses** : `references/doubt-triggers.md`
- **Anti-patterns détaillés** : `references/anti-patterns.md`
- **Protocole d'escalade** : `references/escalation-protocol.md`
- **Règle source** : `AGENTS.md` (section "Droit d'initiative contrôlé")

## Intégration au workspace

Ce skill complète :

- `AGENTS.md` (règle de droit d'initiative) → ajoute le canal formel.
- `.agent/PLAN.md` (plan canonique) → reçoit le journal des décisions.
- `HANDOFF.md` (handoff inter-agents) → reçoit les décisions à reprendre.
- Skill `braintime` (cadrage initial) → ce skill prend le relais en cours
  d'implémentation sur des questions ponctuelles.
- Skill `define-goal` (objectifs) → ce skill s'occupe des décisions
  techniques/fonctionnelles en cours de route.

## Compatibilité multi-IDE

| IDE | Mécanisme | Version dédiée |
|---|---|---|
| GitHub Copilot / VS Code | Scan `.agents/skills/` | `.agents/skills/doubt-qcm-pause/` |
| OpenCode | Scan `.agents/skills/` + `.opencode/skills/` | `.opencode/skills/doubt-qcm-pause/` |
| Anti-Gravity (Google) | Scan `.agents/skills/` | `.agents/skills/doubt-qcm-pause/` |
| Codex (OpenAI) | Scan `.agents/skills/` + `.codex/skills/` | `.codex/skills/doubt-qcm-pause/` |

Les trois versions sont **fonctionnellement identiques** ; seules
diffèrent les métadonnées de frontmatter et l'interface Codex
(`agents/openai.yaml`). Toujours modifier la source active dans
`.agents/skills/doubt-qcm-pause/` puis **propager** aux miroirs.
