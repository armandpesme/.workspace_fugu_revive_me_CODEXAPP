---
name: retrospective-loop
description: Active une boucle de rétrospective quand Codex répète deux fois la même erreur. Analyse la cause, extrait une règle durable, puis met à jour AGENTS.md avec une instruction concrète, courte et vérifiable.
---

# Retrospective Loop Skill

## Objectif

Transformer les erreurs répétées de Codex en règles projet durables dans `AGENTS.md`.

Cette skill doit être utilisée quand Codex répète deux fois le même type d’erreur, notamment :

- mauvaise hypothèse sur l’architecture du dépôt ;
- mauvais chemin de fichier ;
- mauvaise version de Minecraft, Forge, API ou dépendance ;
- convention projet ignorée ;
- correction déjà refusée ou déjà signalée ;
- modification qui casse une règle existante ;
- mauvaise compréhension d’un comportement observé localement.

## Contexte FuguDreams

Le projet FuguDreams est un projet de memo-RPG basé sur Minecraft `1.20.1`, Forge `47.4.20+`, avec un gros modpack auto-hébergé.

Le serveur vise environ `200` joueurs connectés constants.

Les API principales sont Epic Fight et GeckoLib.

KubeJS n’est pas utilisé comme solution principale.

L’équipe crée beaucoup de mods elle-même. Les règles ajoutées doivent donc favoriser la stabilité, la maintenabilité, la compatibilité Forge et la clarté pour les agents IA.

## Déclencheur obligatoire

Déclenche cette skill si l’une des conditions suivantes est vraie :

1. Le user signale deux fois la même erreur.
2. Deux corrections successives concernent la même cause.
3. Une commande, un test ou une lecture de fichier prouve que l’hypothèse précédente était fausse.
4. Codex revient à une solution déjà refusée.
5. Codex applique une règle générique alors qu’une règle projet spécifique existe ou devrait exister.

## Procédure obligatoire

Quand la skill est déclenchée :

1. Arrêter les modifications fonctionnelles en cours.
2. Identifier précisément l’erreur répétée.
3. Rechercher la preuve locale dans le dépôt avant d’écrire une règle.
4. Lire `AGENTS.md` le plus proche du périmètre concerné.
5. Vérifier si une règle équivalente existe déjà.
6. Ajouter ou modifier une seule règle durable.
7. Garder la règle courte, concrète, vérifiable et liée à une erreur réelle.
8. Afficher le diff final de `AGENTS.md`.
9. Reprendre la tâche seulement après avoir intégré la nouvelle règle.

## Format de rétrospective à produire

Avant de modifier `AGENTS.md`, produire ce bloc :

```markdown
## Rétrospective

### Erreur répétée

<Décrire l’erreur concrète en une phrase.>

### Hypothèse incorrecte

<Décrire ce que Codex a supposé à tort.>

### Preuve locale

<Fichier, commande, test, log ou extrait qui prouve la correction.>

### Règle durable à ajouter

<Formuler la règle en une phrase impérative.>

### Emplacement AGENTS.md

<Chemin du fichier AGENTS.md à modifier et section ciblée.>
```

## Format de règle à ajouter dans AGENTS.md

Ajouter les règles dans une section nommée :

```markdown
## Retours d’expérience Codex
```

Si la section n’existe pas, la créer.

Chaque règle doit respecter ce format :

```markdown
- [YYYY-MM-DD] <Règle impérative courte>. Preuve: `<chemin ou commande>`. Évite: <erreur concrète évitée>.
```

Exemple :

```markdown
- [2026-05-28] Pour FuguDreams, ne propose pas KubeJS comme solution principale de gameplay moddé. Preuve: `AGENTS.md > Contexte FuguDreams`. Évite: contourner les mods Forge custom du projet.
```

## Règles de qualité

Ne jamais ajouter :

- une règle vague ;
- une préférence personnelle non prouvée ;
- une règle basée uniquement sur une supposition ;
- une règle qui duplique une règle existante ;
- une règle trop longue ;
- une règle sans preuve locale.

Préférer :

- une règle spécifique au dépôt ;
- une règle actionnable par un agent ;
- une règle vérifiable par lecture, test, grep, build ou log ;
- une règle placée dans l’`AGENTS.md` le plus proche du code concerné.

## Commandes utiles

Chercher les fichiers AGENTS.md :

```bash
find .. -name AGENTS.md -print
```

Chercher une règle existante :

```bash
grep -RniE "Retours d’expérience|retrospective|Codex|KubeJS|Forge|Epic Fight|GeckoLib" . --include='AGENTS.md'
```

Afficher le diff après modification :

```bash
git diff -- AGENTS.md '**/AGENTS.md'
```

## Critère de réussite

La skill est réussie seulement si :

- la cause de l’erreur répétée est explicitement nommée ;
- une preuve locale est fournie ;
- `AGENTS.md` est réellement modifié ou une raison claire explique pourquoi il ne doit pas l’être ;
- la règle ajoutée empêche précisément la récidive ;
- le diff est affiché.
