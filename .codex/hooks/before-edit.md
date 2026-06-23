# Hook: before-edit

## Intent

Bloquer les editions fragiles avant qu'elles touchent le workspace. Ce hook est une procedure executable par Codex App avant tout appel a un outil d'ecriture (`edit_file`, `apply_patch`, creation, suppression ou renommage de fichier).

## Trigger

- Phase: juste avant une modification de fichier.
- Scope prioritaire: `project-gradle/**`, `.codex/**`, `.github/**`, `.antigravity/**`, `.claude/**`, `datapacks/**`, `resourcepacks/**`, `config/**`.
- Ne pas declencher pour une lecture seule, une recherche ou une question factuelle sans edition.

## Procedure obligatoire

1. Identifier les fichiers cibles, les symboles ou ressources touches et le but exact de l'edition.
2. Charger le contexte local minimal si ce n'est pas deja fait dans le tour courant:
   - `AGENTS.md`;
   - `.agent/PLAN.md` quand il contient un etat utile;
   - instruction ou skill applicable au chemin modifie;
   - `README.md` seulement si la tache concerne la presentation utilisateur, le contexte metier propre au projet ou la documentation publique.
3. Lire les fichiers cibles avant edition. Ne jamais modifier un fichier qui n'a pas ete lu dans le tour courant.
4. Pour tout changement de signature, registry name, event, packet, capability, resource location, path d'asset, id JSON ou commande publique, chercher les usages locaux avec `rg` ou l'outil de recherche disponible.
5. Pour une modification large ou quand un fichier cible semble deja modifie, verifier l'etat local avec `git status --short` si Git est disponible. Ne pas inverser, nettoyer ou ecraser des changements humains non compris.
6. Confirmer les invariants du workspace avant de choisir l'edition:
   - Minecraft `1.20.1`;
   - Forge `47.4.x`;
   - Java `17`;
   - pas de Fabric, Loom, NeoForge, Yarn, registry legacy ou API inventee;
   - separation stricte client / server / common;
   - synchro client-serveur uniquement par packet ou capability explicite.
7. Si une API Forge/Minecraft/GeckoLib/Epic Fight est incertaine, chercher d'abord dans le projet et les sources locales. Si l'incertitude reste bloquante, utiliser l'agent/skill documentaire prevu (`explorer` ou `explore-doc`) avant edition.
8. Choisir la modification minimale coherente avec les patterns existants. Ne pas introduire de refactor, abstraction ou metadata sans lien direct avec la demande.
9. Definir la verification post-edition attendue selon les fichiers touches et conserver cette verification pour `after-edit.md`.

## Commandes utiles

Depuis la racine du workspace, selon besoin:

```powershell
git status --short
rg -n "NomDuSymbole|registry_name|resource_location" project-gradle/src
rg -n "modid|item_id|path_asset" project-gradle/src project-gradle/src/main/resources datapacks resourcepacks
```

Depuis `project-gradle/`, pour confirmer les taches disponibles avant un changement Gradle inhabituel:

```powershell
.\gradlew.bat tasks
```

## Conditions bloquantes

Arreter l'edition et expliquer le blocage si l'un de ces cas apparait:

- fichier cible inconnu ou non lu;
- changement humain non compris dans un fichier cible;
- API ou signature Forge/Minecraft incertaine sans source locale fiable;
- edition demandant une commande destructive, un reset Git, un checkout destructif, un secret ou une action reseau non approuvee;
- verification post-edition impossible a definir.

## Sortie attendue avant edition

Avant l'appel d'ecriture, Codex doit pouvoir resumer en une phrase interne ou utilisateur:

`before-edit OK: fichiers lus, usages verifies si necessaire, invariants Forge confirmes, verification post-edit choisie.`
