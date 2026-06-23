---
name: braintime
description: "Invocation explicite uniquement: utiliser seulement quand l'utilisateur appelle /braintime, $braintime ou demande explicitement Braintime. Interviewer l'utilisateur pour transformer une idee floue en objectif, contexte, contraintes, criteres d'acceptation, prompt de mission et plan autosuffisant dans .agent/PLAN.md, avant tout code."
---

# Skill: braintime

## Objectif

Transformer une idee floue en demande exploitable avant d'ecrire du code.

Cette skill sert a challenger l'utilisateur, clarifier le besoin, stabiliser le prompt de mission, puis creer ou modifier `.agent/PLAN.md` seulement quand le plan peut etre autosuffisant pour un agent sans etat.

## Activation stricte

- Utiliser cette skill seulement si l'utilisateur invoque explicitement `/braintime`, `$braintime`, `braintime`, ou demande clairement d'utiliser Braintime.
- Ne pas l'activer implicitement pour une simple demande vague; dans Codex App, `agents/openai.yaml` doit garder `policy.allow_implicit_invocation: false`.
- Pendant Braintime, ne pas modifier le code applicatif, les assets, les configs Forge ou les dependances.
- Les seules modifications autorisees pendant le workflow sont `.agent/PLAN.md` et, si necessaire, un fichier de planification explicitement cite par l'utilisateur.

## Sources a lire au demarrage

1. Lire `AGENTS.md` a la racine du depot.
2. Lire `.agent/Planification-rule.md` si le fichier existe.
3. Lire `.agent/PLAN.md` si le fichier existe, meme vide.
4. Lire les fichiers explicitement references par l'utilisateur avec `@`, un chemin, une erreur ou un log.
5. Inspecter seulement le contexte local necessaire pour poser de bonnes questions; ne pas explorer tout le depot sans raison.

## Deroulement obligatoire

### 1. Reformuler l'idee brute

Produire un court bloc:

```markdown
## Idee recue
- Intention probable:
- Zone floue:
- Decision a obtenir avant plan:
```

Si l'idee initiale manque, demander d'abord une phrase libre: `Quelle idee veux-tu transformer en plan concret ?`

### 2. Challenger les hypotheses

Avant les questions, nommer les angles morts possibles sans juger l'utilisateur:

- objectif trop large ou mal borne;
- confusion entre moyen et resultat attendu;
- dependance externe non verifiee;
- contrainte serveur, securite, performance ou compatibilite oubliee;
- critere d'acceptation trop vague;
- risque de construire une solution avant d'avoir defini le probleme.

Formuler au moins trois questions de challenge, par exemple:

- `Qu'est-ce qui rendrait cette idee inutile meme si elle etait implementee correctement ?`
- `Quel comportement dois-je absolument eviter ?`
- `Quelle hypothese, si elle est fausse, invalide le plan ?`

### 3. Poser au moins 10 questions a choix multiples

Poser au minimum 10 questions avant de produire le plan final.

Chaque question doit avoir:

- un titre clair;
- un objectif explicite;
- plusieurs choix predefinis;
- une option `Autre / reponse personnalisee`;
- si utile, une option `Je ne sais pas encore`.

Les questions doivent couvrir au minimum:

1. objectif exact;
2. resultat attendu;
3. comportement interdit;
4. utilisateurs ou acteurs concernes;
5. contexte et fichiers de reference;
6. perimetre technique;
7. contraintes de compatibilite;
8. contraintes serveur, securite ou performance;
9. priorites et compromis acceptables;
10. risques acceptables;
11. criteres d'acceptation;
12. validations attendues.

Format recommande:

```markdown
### Question 1 - Objectif principal

Objectif de la question: identifier la cible finale.

A. Corriger un bug existant  
B. Ajouter une fonctionnalite  
C. Refactoriser un systeme  
D. Clarifier une architecture  
E. Autre / reponse personnalisee: ...
```

### 4. Synthese de prompt avant plan

Apres les reponses, produire un `Prompt de mission` que l'utilisateur peut relire avant creation du plan:

```markdown
## Prompt de mission

Objectif:
<Ce qu'il faut construire ou resoudre.>

Contexte:
<Fichiers, docs, erreurs, logs, decisions et contraintes connues.>

Contraintes:
<Standards, conventions, securite, compatibilite, performances, perimetre.>

Acceptation:
<Ce qui doit etre vrai pour considerer la tache terminee.>
```

### 5. Validation utilisateur obligatoire

Avant d'ecrire ou modifier `.agent/PLAN.md`, poser explicitement ces deux questions:

1. `Je m'apprete a rediger ou modifier .agent/PLAN.md. As-tu quelque chose a ajouter, corriger ou preciser avant que je genere le plan final ?`
2. `Est-ce qu'un objectif, une contrainte, un risque, un fichier, une dependance ou un cas d'usage important a ete oublie ?`

Si l'utilisateur ajoute des informations, les integrer puis refaire une synthese courte avant d'ecrire le plan.

### 6. Seuil de confiance

Avant d'ecrire le plan final, s'autoevaluer.

Ecrire `.agent/PLAN.md` seulement si la confiance est au moins de 95 % sur:

- comprehension de l'objectif;
- faisabilite;
- perimetre;
- contraintes;
- risques;
- validations;
- capacite d'un agent sans etat a reprendre avec le depot et `.agent/PLAN.md`.

Si la confiance est inferieure a 95 %, ne pas ecrire de plan final. Produire a la place:

- informations manquantes;
- points incertains;
- questions restantes;
- proposition de plan provisoire marquee `NON VALIDEE`.

## Creation ou modification de `.agent/PLAN.md`

Quand le plan est valide:

1. Creer `.agent/PLAN.md` s'il n'existe pas.
2. Si le fichier existe, preserver l'historique utile; ne pas ecraser brutalement un plan encore valable.
3. Rendre le plan autosuffisant: un agent sans etat doit pouvoir reprendre uniquement avec le depot, `.agent/PLAN.md` et les fichiers references dans le plan.
4. Ne pas dependre d'un historique de conversation, d'une memoire implicite ou d'une decision non ecrite.
5. Documenter toute hypothese comme hypothese, pas comme fait.
6. Referencer les fichiers utiles par chemin relatif.

Sections minimales attendues dans `.agent/PLAN.md`:

```markdown
# PLAN - <titre court>

## Objectif

## Non-objectifs

## Contexte verifie

## Fichiers et references

## Contraintes

## Questions et reponses Braintime

## Hypotheses et points a verifier

## Plan d'execution

## Criteres d'acceptation

## Validations prevues

## Risques restants

## Progress

## Journal des decisions

## Surprises et decouvertes

## Retrospective

## Reprise agent sans etat
```

## Sortie finale du workflow

Apres creation ou modification du plan, repondre avec:

- le chemin du plan modifie;
- le niveau de confiance;
- les decisions majeures;
- les questions encore ouvertes, s'il y en a;
- la prochaine action conseillee, par exemple lancer le Plan mode natif de Codex avec le prompt de mission valide.

Ne pas commencer l'implementation dans la meme reponse sauf si l'utilisateur le demande explicitement apres validation du plan.