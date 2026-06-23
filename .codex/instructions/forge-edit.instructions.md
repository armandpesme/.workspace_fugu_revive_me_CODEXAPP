---
applyTo: "**/*.java,**/*.gradle,**/*.toml,**/*.json,**/*.mcmeta"
description: "Hooks de discipline avant/apres edition (transposes de .codex/hooks)."
---

# Discipline d'edition Forge 1.20.1

## Avant edition (before-edit)

- Verifier l'etat Git avant des modifications larges (pas ecraser un changement humain non lu).
- Lire **d'abord** les fichiers concernes.
- Chercher les usages des symboles touches avec `grep_search` / `usages`.
- Confirmer la version cible (Forge `47.4.x`, Java `17`, Minecraft `1.20.1`).

## Apres edition (after-edit)

- Compiler ou proposer la verification minimale selon le type de fichier :
  - Java : compile / `gradlew build`.
  - `build.gradle`, `gradle.properties`, `settings.gradle` : `gradlew tasks` ou build complet.
  - JSON / lang / recipes / loot / tags : valider JSON et chemins, regenerer datagen si utilise.
  - `mods.toml`, `pack.mcmeta` : verifier mod id, version, dependances.
- Si la verification n'a pas pu etre lancee, le **dire explicitement**.
- A la fin d'un cycle build : fournir le lien cliquable vers le JAR de `build/libs/`.
