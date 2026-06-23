# Hook: after-gradle-build-success

## Intent

Forcer la mise a jour de `.agent/PLAN.md` apres tout Gradle build reussi ou clean build reussi, afin que le travail reste reprenable par un agent sans etat.

## Trigger

- Phase: apres execution d'une commande terminal ou shell.
- Condition de succes: code de sortie `0`.
- Commandes ciblees:
  - `gradlew build`, `./gradlew build`, `.\gradlew.bat build`, `gradlew.bat build`, `gradle.bat build`;
  - `gradlew clean build`, `./gradlew clean build`, `.\gradlew.bat clean build`, `gradlew.bat clean build`, `gradle.bat clean build`;
  - variantes equivalentes lancees depuis `project-gradle/`.

## Action obligatoire

1. Charger le skill `.codex/skills/exee-plan/SKILL.md`.
2. Mettre a jour `.agent/PLAN.md` avant la reponse finale.
3. Ajouter au minimum: commande exacte, dossier d'execution, resultat, type de build, JAR produit si connu, risques restants et prochaine action.
4. Conserver le lien cliquable vers le JAR dans la reponse finale quand un JAR est produit dans `project-gradle/build/libs/`.

## Si la mise a jour est impossible

- Ne pas annoncer le jalon comme totalement livre.
- Expliquer le blocage et la mise a jour attendue dans la reponse finale.
