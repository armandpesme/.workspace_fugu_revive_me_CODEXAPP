# README

## Environnement

- Java 17 JDK : `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`
- Forge `1.20.1-47.4.x`
- Windows 11 Pro
- Minecraft `1.20.1`
- GeckoLib Forge `1.20.1-4.8.3`
- citresewn-1.20.1-5.jar
- Epic Fight Forge `20.14.17-1.20.1`
- Projet Gradle : `project-gradle/`

Ressource | URL |
|---|---|
| **Dépôt GitHub** | https://github.com/armandpesme/.workspace_fugu_revive_me_CODEXAPP |
| **Schéma GitNexus (UI locale)** | http://localhost:4747/

#### Structure

## Structure

| Dossier           | Rôle                                                                |
| ----------------- | ------------------------------------------------------------------- |
| `config/`         | Fichiers de configuration de Minecraft et des mods                  |
| `project-gradle/` | Projets Gradle des mods créés ou modifiés                           |
| `datapacks/`      | Datapacks créés ou modifiés                                         |
| `resourcepacks/`  | Resource packs créés ou modifiés                                    |
| `scripts/`        | Scripts enregistrés et utilisés par le workspace                    |
| `shaderpacks/`    | Shader packs créés ou modifiés                                      |
| `logs/`           | Journaux d’exécution de l’instance Modrinth utilisée pour les tests |
| `crash-reports/`  | Rapports de crash de l’instance Modrinth utilisée pour les tests    |
| `docs/`           | Tout fichier mods utile (API,runClient,ressources)

## Objectifs

Read `prompt.md` pour les objectifs de la tâche.

## Contexte

Read `prompt.md` pour le contexte de la tâche.

## Contraintes

Read `prompt.md` pour les contraintes de la tâche.

## Conditions d’acceptation

Une tâche ou un jalon est considéré comme terminé uniquement lorsque les conditions suivantes sont remplies :

1. L’objectif défini dans le prompt a été atteint.
2. Le plan annoncé a été entièrement exécuté, sauf blocage explicitement documenté.
3. Les erreurs détectées pendant le travail ont été corrigées ou clairement signalées.
4. Le code compile.
5. Un lancement du client de développement a été effectué avec succès lorsque la tâche impacte le comportement en jeu.
6. Les tests manuels demandés ont été effectués par le responsable du projet lorsque la tâche le nécessite.
7. Le retour de test est positif ou les problèmes restants sont clairement listés.
8. Les injecteurs critiques conservent leurs contraintes obligatoires en production, notamment `require = 1` ou une valeur minimale adaptée. Ils ne doivent pas être rendus permissifs pour masquer une erreur.
9. Les options de diagnostic réservées au développement, telles que `mixin.debug.countInjections`, ne doivent pas être activées dans un état final.
10. Le fichier `end-off.md` a été mis à jour via la commande/skill dédiée si Armand le demande.
11. Si un feedback long est demandé, créer un fichier dans `./docs/context/` au format `<date>-<version>-<modid>-feedback.md`.

La tâche ne peut pas être clôturée si un point bloquant est connu mais non signalé.

## Conditions de release finale

Une release finale est considérée comme prête uniquement lorsque les conditions suivantes sont remplies :

1. Toutes les conditions d’acceptation de tâche/jalon sont validées.
2. Un build de développement a été généré et validé.
3. Le fichier source JAR versionné `*-sources.jar` a été généré, par exemple `fugu_revive_me-1.0.1-sources.jar`.
4. Ne jamais créer de fichier littéral nommé `mod-source.jar`.
5. La version du mod a été incrémentée de `+0.0.1` si la release inclut des changements de code.
6. Un clean build final a été exécuté avec succès.
7. Les options de debug/dev ont été désactivées avant le build final.
8. Le résultat a été testé manuellement par le responsable du projet.
9. Le retour de test final est positif.
10. Un fichier feedback final a été créé dans `./docs/context/` au format `<date>-<version>-<modid>-feedback.md`.

La release peut être clôturée uniquement après validation de l’ensemble de ces étapes.
