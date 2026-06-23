# Hook: after-edit

## Intent

Verifier les edits avant livraison et empecher Codex App d'annoncer un changement comme termine sans preuve minimale. Ce hook s'execute apres tout appel a un outil d'ecriture (`edit_file`, `apply_patch`, creation, suppression ou renommage de fichier).

## Trigger

- Phase: immediatement apres edition, avant la reponse finale.
- Scope: tout fichier modifie dans le workspace.
- Si plusieurs editions sont faites en rafale, executer cette procedure apres le dernier edit coherent du lot, puis relancer si un nouvel edit est necessaire.

## Procedure obligatoire

1. Lister mentalement ou explicitement les fichiers modifies et le comportement attendu.
2. Ne pas relire mecaniquement les fichiers qui viennent d'etre modifies: `apply_patch`/`edit_file` a deja valide l'application. Relire seulement si un diagnostic, conflit, doute de contexte ou sortie d'outil le rend utile.
3. Classer les fichiers touches et choisir la verification minimale pertinente:
   - Java: compiler au minimum depuis `project-gradle/` avec `.\gradlew.bat compileJava`; utiliser `.\gradlew.bat build` si le changement touche plusieurs surfaces, des tests, des ressources chargees au runtime ou une integration Forge importante.
   - Tests Java: lancer `.\gradlew.bat test` ou `.\gradlew.bat build`.
   - `build.gradle`, `settings.gradle`, `gradle.properties`: lancer `.\gradlew.bat tasks` ou `.\gradlew.bat build` selon le risque.
   - `mods.toml`, `pack.mcmeta`: verifier `modId`, versions, loader/dependances et lancer une verification Gradle raisonnable.
   - JSON, lang, recipes, loot, tags, models, datapacks, resourcepacks: valider le JSON avec un parseur structurel disponible et verifier les chemins, namespaces et ids (`assets/<modid>/...`, `data/<modid>/...`).
   - Documentation, prompts, agents, skills, hooks: verifier coherence des liens/chemins, absence de placeholder restant et alignement avec `AGENTS.md` / configuration cible.
4. Pour les assets et data Forge, confirmer que le chemin correspond au registry id. Exemple: un id plat `foo_bar` attend `assets/<modid>/models/item/foo_bar.json`.
5. Pour du code client/server, verifier qu'aucune classe client n'est referencee depuis common/server sans garde (`Dist.CLIENT`, classe client dediee, packet/capability explicite selon le besoin).
6. Pour tout build Gradle reussi (`build` ou `clean build`), executer aussi le hook `.codex/hooks/after-gradle-build-success.md` avant la reponse finale.
7. Si une verification echoue, ne pas masquer l'erreur. Identifier l'erreur principale, le fichier ou la cause probable, puis corriger si le correctif est dans le scope. Relancer ensuite la verification pertinente.
8. Si une verification raisonnable n'est pas lancee, l'indiquer explicitement dans la reponse finale avec la raison.

## Commandes de verification usuelles

Depuis `project-gradle/`:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat clean build
```

Pour valider un JSON avec PowerShell quand aucun outil specialise n'est deja utilise:

```powershell
Get-Content "chemin\fichier.json" -Raw | ConvertFrom-Json > $null
```

Pour confirmer qu'un placeholder a disparu:

```powershell
rg -n "place[h]older|T[O]DO|Suggested future act[i]on" .codex .github .antigravity .claude
```

## Conditions de livraison

La reponse finale doit contenir, de facon concise:

- les fichiers modifies;
- la verification lancee et son resultat, ou la raison precise si elle n'a pas ete lancee;
- le risque restant connu;
- apres un build Gradle produisant un JAR, un lien cliquable vers le JAR dans `project-gradle/build/libs/`.

## Conditions bloquantes

Ne pas annoncer `termine`, `GO` ou equivalent si l'un de ces cas reste vrai:

- compilation ou validation pertinente en echec;
- fichier JSON invalide;
- chemin asset/data incoherent avec le mod id ou le registry id;
- API Forge/Minecraft non verifiee alors qu'elle porte le changement;
- conflit avec des changements humains non compris;
- hook `after-gradle-build-success.md` requis mais non execute apres build reussi.
