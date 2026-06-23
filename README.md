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
- notre repo:

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

## Contexte

## Contraintes

## Conditions d’acceptation

Une tâche est considérée comme terminée lorsque les conditions suivantes sont remplies :

1. L’objectif défini a été atteint et le plan a été entièrement exécuté.
2. Les erreurs détectées ont été corrigées.
3. Un lancement du client de développement a été effectué avec succès.
4. Un build de développement a été généré et validé.
5. Le fichier source jar versionné `*-sources.jar` a également été généré, par exemple `fugu_transmog-1.0.1-sources.jar`. Ne pas créer de fichier littéral `mod-source.jar`.
6. Le résultat a été testé manuellement par le responsable du projet.
7. Le retour de test est positif.
8. Les injecteurs critiques conservent leurs contraintes obligatoires en production, notamment require = 1 ou une valeur minimale adaptée. Ils ne doivent pas être rendus permissifs avant la release.
9. Les options de diagnostic réservées au développement, telles que mixin.debug.countInjections, ont été désactivées avant le build final.
10. La version du mod a été incrémentée (+0.0.1)
11. Un clean build final a été exécuté avec succès.
12. creation d'un fichier feedback dans `./docs/context/` <docs+date+version+modid>.md

La tâche peut être clôturée uniquement après la validation de l’ensemble de ces étapes.
