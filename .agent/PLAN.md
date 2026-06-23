# Spécification de modification visuelle — Interface `Station de transmogrification`

## État d'exécution Codex — 2026-06-22 — version `1.0.1`

### Progression

- Fait : lecture de `README.md`, `AGENTS.md`, `DESIGNE.md` et de cette spécification; exploration locale de `TransmogScreen`, `TransmogMenu`, `VoidFragmentItem`, lang/datagen et build Gradle.
- Fait : branche de travail `codex/transmog-aether-ui` créée depuis `main`.
- Fait : analyses GitNexus pré-édition sur `TransmogScreen`, `TransmogMenu`, `VoidFragmentItem`, `TranslationKeys`, `ModLanguageProvider`, puis méthodes `render`, `renderBg`, `renderPremiumPanel`, `onTake`, `removed`, `addTranslations`. Risque classe `TransmogMenu` : MEDIUM; autres cibles : LOW.
- Fait : refonte visuelle de `TransmogScreen` en panneau fullscreen GUI Scale 4 avec titre, labels, messages dynamiques, tooltips, cercle runique centré sur le résultat, jauge d'améthyste, équipement porté, inventaire/hotbar repositionnés et boutons `Valider` / `Annuler`.
- Fait : coordonnées des vrais slots déplacées dans `TransmogMenu` pour rester alignées avec les hitboxes et le rendu.
- Fait : actions serveur `Valider` / `Annuler` ajoutées via `clickMenuButton`; `Valider` donne le résultat, consomme fuel + Void si présent, puis renvoie les items restants; `Annuler` renvoie les items sans appliquer le résultat.
- Fait : tooltip Void ajouté, titre container mis à `Station de transmogrification`, `mod_version` incrémenté de `1.0.0` à `1.0.1`.
- Fait : feedback créé dans `docs/context/docs-2026-06-22-1.0.1-transmog.md`.
- Fait : correctif post-retour utilisateur sur `TransmogScreen` : suppression des widgets `Button` vanilla, boutons `Valider` / `Annuler` rendus et cliqués en Java renderer, repères hover sur inventaire/hotbar vrais slots, tooltips client-only sur les équipements portés.
- Fait : correctif post-retour coins + équipements portés : coins du panneau réorientés aux vrais angles, clic gauche/droit sur les équipements portés pour les envoyer côté serveur vers `Équipement à modifier` ou `Apparence à copier`.
- Fait : validation manuelle/runtime responsable projet reçue le 2026-06-22 avec retour ultra positif.
- Fait : conditions d'acceptation finales clôturées, `clean build` final réussi, feedback final mis à jour.
- Fait : condition d'acceptation README corrigée pour préciser le source jar versionné `*-sources.jar` et éviter la création d'un `mod-source.jar`.
- Reste : préparer la release/PR si demandé; aucun correctif UI bloquant connu.

### Surprises et discovery

- Le code actuel ne consommait pas le `Void` lors de la validation alors que la spécification indique qu'il doit être consommé. Décision prise : aligner la validation et la prise du résultat sur la règle documentée.
- L'améthyste est déjà consommée au placement quand la station n'a plus de fuel; `Annuler` ne rembourse donc pas ce shard, conformément au comportement actuel.
- Le projet n'a pas de tests Java (`compileTestJava NO-SOURCE`, `test NO-SOURCE`); la vérification automatisée repose sur compilation/build.
- Les Markdown ciblent Forge 1.20.1 / 47.4.x, mais le Gradle technique courant reste `minecraft_version=1.20` et `forge_version=46.0.12`. Cette migration de toolchain n'a pas été incluse pour limiter le risque.
- Le build génère `fugu_transmog-1.0.1-sources.jar`; il n'existe pas de tâche produisant littéralement `mod-source.jar`.
- Les cases `Équipement porté` sont un aperçu client dessiné, pas de vrais slots de menu. Les tooltips peuvent donc être ajoutés côté client sans risque; le déplacement direct d'un équipement porté vers un slot de transmo demanderait une logique serveur/menu ou packet dédié.
- L'inventaire et la hotbar sont déjà de vrais `Slot` dans `TransmogMenu`; le correctif conserve cette logique et ajoute seulement des cadres/hover renderer pour rendre les hitboxes lisibles.
- Le déplacement des équipements portés est possible sans packet custom en réutilisant `AbstractContainerMenu.clickMenuButton` : l'écran choisit l'action, mais `TransmogMenu` déplace l'item depuis le slot d'équipement joueur côté serveur.
- Les coins du panneau étaient dessinés avec une seule orientation haut-gauche; les coins droite/bas semblaient donc décalés vers l'intérieur.
- GitNexus `detect_changes(scope=staged)` classe ce correctif en risque `HIGH`, car `TransmogMenu` est central et touche les flows d'inventaire; revue locale : `quickMoveStack` n'est pas modifié, le nouveau transfert est borné à `clickMenuButton` et refuse les slots cibles occupés.
- Le nom attendu du source jar est le nom Gradle standard versionné : `fugu_transmog-1.0.1-sources.jar`. Ne pas créer ni demander `mod-source.jar` pour ce projet.

### Decision log

- 2026-06-22 / Jalon UI : garder l'implémentation dans l'écran et le menu existants, sans créer de GUI séparée, pour respecter la contrainte du projet.
- 2026-06-22 / Jalon UI : déplacer les vrais slots côté `TransmogMenu` plutôt que seulement les cadres visuels, afin d'éviter une désynchronisation hitbox/rendu.
- 2026-06-22 / Jalon logique : utiliser `AbstractContainerMenu.clickMenuButton` pour `Valider` / `Annuler`, ce qui garde les décisions d'inventaire côté serveur et évite une synchro custom.
- 2026-06-22 / Jalon Void : consommer `void_fragment` lors d'une validation réussie, car la spécification le demande explicitement.
- 2026-06-22 / Jalon build : ne pas migrer Minecraft/Forge vers 1.20.1/47.4.x dans cette passe; cette migration est séparée du besoin UI et augmente le risque.
- 2026-06-22 / Correctif UI : remplacer les boutons vanilla par une gestion `mouseClicked` + rendu custom, tout en continuant à appeler `handleInventoryButtonClick` pour garder la validation/annulation côté serveur.
- 2026-06-22 / Correctif UI : ne pas transformer les équipements portés en slots interactifs dans cette passe; afficher leurs tooltips seulement, car le clic-déplacement complet n'est pas client-only.
- 2026-06-22 / Correctif équipements portés : clic gauche sur un équipement porté remplit `Équipement à modifier` si vide, sinon `Apparence à copier` si vide; clic droit cible directement `Apparence à copier`. Le menu refuse si le slot cible est déjà occupé.
- 2026-06-22 / Correctif coins : utiliser un rendu de coin orienté par direction pour garder les traits cyan collés aux quatre angles du panneau.
- 2026-06-22 / Acceptation finale : considérer le retour utilisateur "ultra positif" comme validation runtime/manuelle du client; conserver `fugu_transmog-1.0.1-sources.jar` comme artefact source attendu.

### Outcome et retrospective

- `.\gradlew.bat test` exécuté depuis `project-gradle/` : succès, build de classes OK, aucun test source.
- `.\gradlew.bat build` exécuté depuis `project-gradle/` : succès.
- Correctif post-retour :
  - `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-22 : succès (`test NO-SOURCE`, compilation OK).
  - `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-22 : succès; seuls warnings Javadoc existants `no comment`.
- Vérification finale avant réponse :
  - `.\gradlew.bat build` relancé depuis `project-gradle/` le 2026-06-22 : succès (`BUILD SUCCESSFUL in 9s`, `test NO-SOURCE`).
- Correctif coins + équipements portés :
  - `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-22 : succès (`test NO-SOURCE`, compilation OK).
  - `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-22 : succès; seuls warnings Javadoc existants `no comment`.
- Acceptation finale :
  - `.\gradlew.bat clean build` exécuté depuis `project-gradle/` le 2026-06-22 : succès (`BUILD SUCCESSFUL in 18s`, `test NO-SOURCE`).
  - Artefact source attendu confirmé : `project-gradle/build/libs/fugu_transmog-1.0.1-sources.jar`.
  - Aucun `mod-source.jar` attendu pour ce projet.
  - Mixins critiques vérifiés : `required: true`, `injectors.defaultRequire: 1`; aucune occurrence de `mixin.debug.countInjections`.
  - Feedback final mis à jour : `docs/context/docs-2026-06-22-1.0.1-transmog.md`.
  - Verdict QA : GO.
- Artefacts produits :
  - `project-gradle/build/libs/fugu_transmog-1.0.1.jar`
  - `project-gradle/build/libs/fugu_transmog-1.0.1-sources.jar`
  - `project-gradle/build/libs/fugu_transmog-1.0.1-javadoc.jar`
- Ressources lang JSON validées avec `ConvertFrom-Json` : succès.
- Risque restant : aucun bloquant connu; le projet n'a pas de tests gameplay automatisés et le toolchain Gradle reste `minecraft_version=1.20` / `forge_version=46.0.12` malgré les Markdown cible 1.20.1 / 47.4.x.

### Reprise agent sans état

Prochaine action concrète : préparer la release ou la PR depuis `codex/transmog-aether-ui` si demandé. Les artefacts finals sont dans `project-gradle/build/libs/`, notamment `fugu_transmog-1.0.1.jar` et `fugu_transmog-1.0.1-sources.jar`.

Projet : **FuguDreams / FuguTeams**
Contexte cible : **Minecraft 1.20.1 — Forge 47.4.20+**
Mod concerné : fork / rework fonctionnel du mod **Transmog**
Type de travail : modification d’une interface existante, principalement visuelle

---

## 1. Objectif

Modifier l’interface existante de transmogrification afin de produire une interface fullscreen plus lisible, plus qualitative visuellement et mieux adaptée au style MMORPG du projet FuguDreams.

L’interface à modifier est celle déjà présente dans le mod forké / reworké Transmog.

Il ne faut pas créer une nouvelle interface indépendante.

Il faut modifier l’interface existante déjà fonctionnelle.

L’objectif principal est visuel, ergonomique et textuel.

Les changements doivent améliorer :

* la lisibilité des slots ;
* la disposition générale ;
* les messages d’aide ;
* les tooltips ;
* les effets visuels ;
* la cohérence graphique ;
* la compréhension de l’action de transmogrification ;
* la lisibilité du résultat avant validation.

---

## 2. Contrainte principale

Le mod fonctionne déjà.

Il ne faut pas casser l’existant fonctionnel.

L’agent peut refactoriser l’interface si cela rend le rendu plus propre, mais uniquement si cela reste maîtrisé et ne casse pas :

* la logique existante des slots ;
* le comportement actuel de la transmogrification ;
* le comportement serveur ;
* le comportement client ;
* le retour des items dans l’inventaire ;
* l’utilisation actuelle de l’item `Void` ;
* les interactions déjà fonctionnelles du slot résultat.

Les modifications sont principalement visuelles.

Les changements fonctionnels autorisés concernent uniquement :

* les boutons `Valider` et `Annuler` ;
* les textes visibles ;
* les tooltips ;
* les messages dynamiques ;
* la description de l’item `Void` ;
* les effets visuels ;
* l’organisation visuelle de l’interface.

---

## 3. Résolution et échelle d’interface

L’interface doit être conçue pour :

* écran **1920 × 1080** ;
* affichage **fullscreen** ;
* **GUI Scale 4 prioritaire**.

La demande initiale mentionnait une taille d’interface `4 ou 3`, mais la décision retenue est :

> La validation visuelle prioritaire se fait en GUI Scale 4.

L’interface doit rester propre et lisible sur un grand écran, avec une fenêtre beaucoup plus grande qu’une interface vanilla classique.

---

## 4. Nom de l’interface

Le titre visible de l’interface doit être :

```text
Station de transmogrification
```

Ce titre doit être placé en haut de l’interface.

---

## 5. Structure générale de l’écran

La structure doit suivre strictement le croquis fourni.

L’interface doit être organisée en grandes zones :

1. **Zone haute**
   Zone principale de transmogrification.

2. **Zone médiane / séparation**
   Séparation visuelle horizontale entre la partie transmogrification et la partie inventaire.

3. **Zone basse gauche**
   Zone des équipements portés.

4. **Zone basse droite**
   Inventaire + hotbar.

5. **Zone basse gauche inférieure**
   Boutons `Valider` puis `Annuler`.

La structure générale attendue est :

```text
┌──────────────────────────────────────────────┐
│        Zone haute : transmogrification        │
│                                              │
│  Slot équipement   Slot résultat   Slot copie │
│  + messages        + runes         + messages │
│                                              │
│              Slot améthyste + jauge           │
├──────────────────────────────────────────────┤
│ Zone équipement porté │ Inventaire + hotbar   │
│                       │                       │
│ Boutons Valider/Annuler                       │
└──────────────────────────────────────────────┘
```

---

## 6. Zone haute : slots principaux

La zone haute contient trois slots principaux.

L’ordre exact est :

1. À gauche : **Équipement à modifier**
2. Au centre : **Aperçu du résultat**
3. À droite : **Apparence à copier**

Chaque slot principal doit avoir :

* un texte visible au-dessus ;
* un tooltip détaillé au survol.

---

## 7. Slot gauche — Équipement à modifier

Nom visible :

```text
Équipement à modifier
```

Fonction :

Ce slot reçoit l’arme ou la pièce d’équipement dont on veut changer l’apparence.

Tooltip recommandé :

```text
Placez ici l’arme ou la pièce d’équipement dont vous voulez modifier l’apparence.
```

Ce slot représente l’objet réel qui sera modifié si le joueur valide l’action.

---

## 8. Slot central — Aperçu du résultat

Nom visible :

```text
Aperçu du résultat
```

Fonction :

Ce slot affiche le résultat prévu de la transmogrification.

Il doit rester cohérent avec le comportement actuel du mod.

Règle importante :

> Le comportement interactif du slot résultat doit rester celui du mod actuel.

Il ne faut pas modifier arbitrairement son interaction.

Il ne faut pas décider que ce slot devient récupérable ou non récupérable si ce n’est pas déjà le comportement actuel du mod.

Tooltip recommandé :

```text
Affiche l’apparence finale prévue après transmogrification.
```

Effet visuel associé :

Un cercle de runes magiques doit entourer le slot `Aperçu du résultat`.

Le cercle de runes doit être :

* toujours visible ;
* animé en rotation ;
* plus lumineux ou légèrement pulsant quand le résultat est valide.

Ne pas remplacer le slot résultat par un aperçu 3D.

L’aperçu 3D est abandonné.

---

## 9. Slot droit — Apparence à copier

Nom visible :

```text
Apparence à copier
```

Fonction :

Ce slot reçoit l’arme ou la pièce d’équipement dont on veut copier l’apparence.

Tooltip recommandé :

```text
Placez ici l’arme ou la pièce d’équipement dont l’apparence doit être copiée.
```

Ce slot peut aussi recevoir l’item `Void`, qui existe déjà dans le mod de base.

---

## 10. Slot d’améthyste

Un slot de ravitaillement en améthyste doit être présent.

Position :

* au centre de l’interface ;
* sous le slot `Aperçu du résultat` ;
* conformément au croquis ;
* proche de la jauge d’améthyste.

Nom recommandé :

```text
Ravitaillement en améthyste
```

Tooltip recommandé :

```text
Placez ici l’améthyste utilisée pour alimenter la transmogrification.
```

---

## 11. Jauge d’améthyste

Une jauge d’améthyste doit être affichée près du slot d’améthyste.

Important :

> La jauge est liée visuellement au slot d’améthyste, pas directement au slot résultat.

Règles visuelles :

* la jauge est toujours visible ;
* elle indique le niveau d’améthyste disponible ;
* elle pulse surtout quand elle est pleine ou prête à valider ;
* si elle est vide, elle affiche un message clair.

Message recommandé si la jauge est vide :

```text
Rechargez en améthyste.
```

La jauge peut utiliser :

* des pulsations ;
* des couleurs saturées ;
* une transparence partielle ;
* un effet lumineux compatible avec la charte graphique.

---

## 12. Item `Void`

L’item `Void` existe déjà dans le mod de base.

Il ne faut pas le recréer.

Il ne faut pas inventer une nouvelle mécanique pour lui.

Comportement existant à conserver :

* le `Void` se place dans le slot `Apparence à copier` ;
* lorsqu’il est utilisé comme apparence, il rend invisible l’apparence de l’équipement placé dans le slot `Équipement à modifier` ;
* il est consommé lors de la validation.

Modifications demandées :

1. Modifier / améliorer la description de l’item `Void`.
2. Ajouter / améliorer le message d’avertissement affiché sous la zone concernée.

Description recommandée pour l’item `Void` :

```text
Utilisez cet item comme apparence pour rendre votre équipement invisible.
```

Message recommandé quand le `Void` est placé dans `Apparence à copier` :

```text
Le Void sera consommé. L’apparence de votre équipement deviendra invisible après validation.
```

Ce message doit être clair pour le joueur avant qu’il valide l’action.

---

## 13. Message “laisser vide pour retirer une apparence”

Un message doit informer le joueur qu’il peut laisser le slot `Apparence à copier` vide pour retirer une apparence déjà appliquée.

Condition exacte d’affichage :

Le message doit s’afficher uniquement si :

* le slot `Équipement à modifier` contient un item ;
* cet item possède déjà une apparence appliquée.

Le message doit s’afficher indépendamment de l’état du slot `Apparence à copier`.

Cela signifie que le message peut s’afficher même si le slot `Apparence à copier` est vide ou rempli.

Message recommandé :

```text
Laissez le slot d’apparence vide pour retirer l’apparence actuellement appliquée.
```

Ne pas afficher ce message si l’item placé dans `Équipement à modifier` n’a pas déjà d’apparence appliquée.

---

## 14. Messages dynamiques

Le croquis indique des zones de messages dynamiques à gauche et à droite de la zone haute.

Ces messages doivent servir à expliquer l’état actuel de l’interface au joueur.

Les messages doivent être courts, lisibles et contextuels.

Ils ne doivent pas surcharger l’écran.

Exemples de messages attendus :

Si aucun équipement à modifier n’est placé :

```text
Placez un équipement à modifier.
```

Si aucune apparence à copier n’est placée :

```text
Placez une apparence à copier ou laissez vide pour retirer une apparence existante.
```

Si le `Void` est placé :

```text
Le Void sera consommé. L’apparence de votre équipement deviendra invisible après validation.
```

Si l’améthyste est vide :

```text
Rechargez en améthyste.
```

Si le résultat est valide :

```text
Transmogrification prête.
```

Les messages doivent être reformulés proprement, mais sans changer le sens fonctionnel.

---

## 15. Contours des slots remplis

Les slots fonctionnels de transmogrification doivent recevoir un contour visuel lorsqu’ils sont remplis.

Slots concernés :

* `Équipement à modifier` ;
* `Aperçu du résultat` ;
* `Apparence à copier` ;
* slot d’améthyste.

Les slots de l’inventaire, de la hotbar et des équipements portés ne sont pas concernés par cette règle spécifique, sauf si l’existant le fait déjà.

Style attendu :

* contour bleu turquoise saturé ;
* rendu électrique ;
* teinte claire ;
* léger dégradé ;
* transparence partielle possible ;
* effet lumineux lisible mais non agressif.

Le contour doit indiquer clairement que le slot contient un item utile à l’action en cours.

---

## 16. Effets visuels attendus

Les effets visuels attendus sont :

1. Cercle de runes magiques autour du slot `Aperçu du résultat`.
2. Animation de rotation du cercle de runes.
3. Runes plus lumineuses ou pulsantes quand le résultat est valide.
4. Jauge d’améthyste près du slot d’améthyste.
5. Pulsations et variations de couleur sur la jauge d’améthyste.
6. Message visible si la jauge est vide.
7. Contour bleu turquoise électrique sur les slots fonctionnels remplis.

Ces effets doivent rester lisibles, propres et compatibles avec une interface Minecraft modded.

Ne pas ajouter d’aperçu 3D.

Ne pas créer d’effet non demandé qui changerait la logique de l’interface.

---

## 17. Zone basse gauche — Équipement porté

En bas à gauche, l’interface doit afficher une zone dédiée aux équipements actuellement portés.

Cette zone doit contenir 5 cases :

1. Casque
2. Plastron
3. Jambières
4. Bottes
5. Main secondaire / offhand

Ces slots doivent permettre de représenter les pièces actuellement équipées par le joueur.

Ils peuvent être utilisés pour placer rapidement ces équipements dans les slots principaux si l’interface actuelle ou la logique existante le permet.

Ne pas retirer la main secondaire.

La main secondaire doit bien être incluse.

---

## 18. Zone basse droite — Inventaire et hotbar

En bas à droite, l’interface doit afficher :

* l’inventaire du joueur ;
* la hotbar.

Cette zone ne doit pas être une interface vanilla classique.

Elle doit être rendue dans le renderer de l’interface.

Cependant, elle doit conserver une taille équivalente à l’inventaire vanilla normal, car l’interface globale est beaucoup plus grande.

La hotbar doit être placée sous l’inventaire, conformément à la logique visuelle Minecraft habituelle.

Orthographe retenue :

```text
hotbar
```

---

## 19. Boutons Valider / Annuler

Les boutons doivent être placés en bas de l’interface, tout en bas à gauche, sous la zone des cases de l’inventaire / zone basse, conformément au croquis.

Ordre exact :

1. `Valider`
2. `Annuler`

Texte visible sur les boutons :

```text
Valider
```

```text
Annuler
```

Les boutons ne doivent afficher que ce texte court.

Les explications détaillées doivent être dans le Markdown, dans les tooltips ou dans la logique interne, mais pas affichées comme gros texte permanent sur les boutons.

### Comportement de `Valider`

Règle à écrire explicitement :

```text
Valider applique la transmogrification affichée dans l’Aperçu du résultat, puis renvoie dans l’inventaire tous les items encore présents dans l’interface.
```

Tooltip recommandé :

```text
Applique le résultat affiché, puis renvoie les items restants dans votre inventaire.
```

### Comportement de `Annuler`

Règle à écrire explicitement :

```text
Annuler ferme ou réinitialise l’interface, renvoie tous les items dans l’inventaire, et ne modifie aucun item.
```

Tooltip recommandé :

```text
Annule l’action, renvoie tous les items dans votre inventaire et ne modifie aucun équipement.
```

Aucune modification ne doit être appliquée si le joueur clique sur `Annuler`.

Aucun item ne doit être perdu avec `Valider` ou `Annuler`.

---

## 20. Charte graphique

La charte graphique demandée est :

* violet foncé saturé ;
* rose / pink flashy saturé électrique ;
* bleu turquoise saturé électrique.

Les couleurs doivent donner une ambiance magique, saturée, lisible et compatible MMORPG.

Codes HEX proposés à titre indicatif uniquement :

```text
Violet foncé saturé : #2A0A4A
Rose électrique : #FF3FD8
Bleu turquoise électrique : #23F7FF
```

Ces codes HEX ne sont pas obligatoires.

Ils servent uniquement de base de travail si l’agent a besoin de valeurs concrètes.

Ne pas les considérer comme une charte définitive imposée.

---

## 21. Règles de rendu

L’interface doit rester cohérente avec un rendu Java / renderer Minecraft.

L’interface ne doit pas dépendre d’un aperçu 3D.

L’interface doit éviter les ajouts visuels impossibles à maintenir.

Priorité :

1. lisibilité ;
2. stabilité ;
3. respect de l’existant ;
4. rendu propre ;
5. cohérence avec le croquis ;
6. absence de régression fonctionnelle.

---

## 22. Ce qu’il ne faut pas faire

Ne pas créer une nouvelle interface séparée.

Ne pas casser l’interface existante.

Ne pas recréer l’item `Void`.

Ne pas inventer une nouvelle mécanique pour le `Void`.

Ne pas ajouter un aperçu 3D.

Ne pas déplacer librement les zones sans respecter le croquis.

Ne pas transformer l’inventaire en interface vanilla brute.

Ne pas appliquer le contour turquoise à tous les slots de l’inventaire si ce n’est pas demandé.

Ne pas modifier arbitrairement le comportement actuel du slot `Aperçu du résultat`.

Ne pas afficher les descriptions longues directement sur les boutons.

Ne pas faire perdre d’items au joueur.

Ne pas appliquer de modification si le joueur clique sur `Annuler`.

---

## 23. Résumé de disposition validée

Disposition finale validée :

```text
Haut gauche :
- Slot Équipement à modifier
- Texte visible au-dessus
- Tooltip détaillé
- Message dynamique associé

Haut centre :
- Slot Aperçu du résultat
- Texte visible au-dessus
- Tooltip détaillé
- Cercle de runes magique autour du slot
- Runes toujours visibles
- Runes plus lumineuses / pulsantes si résultat valide

Centre sous l’aperçu :
- Slot d’améthyste
- Jauge d’améthyste proche du slot d’améthyste
- Jauge toujours visible
- Jauge pulsante surtout si pleine ou prête à valider
- Message si vide : Rechargez en améthyste.

Haut droite :
- Slot Apparence à copier
- Texte visible au-dessus
- Tooltip détaillé
- Message dynamique associé
- Peut recevoir l’item Void existant

Bas gauche :
- Zone équipement porté
- 5 slots : casque, plastron, jambières, bottes, main secondaire/offhand

Bas droite :
- Inventaire joueur
- Hotbar sous l’inventaire
- Rendu par l’interface, pas vanilla brut
- Taille équivalente à l’inventaire vanilla normal

Tout en bas à gauche :
- Bouton Valider
- Bouton Annuler
```

---

## 24. Critères de réussite

Le travail est réussi si :

* l’interface respecte le croquis ;
* l’interface est fullscreen en 1920 × 1080 ;
* le GUI Scale 4 est prioritaire ;
* le titre affiché est `Station de transmogrification` ;
* les trois slots principaux sont dans le bon ordre ;
* le slot résultat est au centre ;
* l’aperçu 3D est absent ;
* le cercle de runes est autour du slot résultat ;
* le slot d’améthyste est sous le résultat ;
* la jauge est proche du slot d’améthyste ;
* le `Void` conserve sa logique existante ;
* la description du `Void` est améliorée ;
* les messages dynamiques sont clairs ;
* les boutons `Valider` et `Annuler` sont présents ;
* `Valider` applique bien le résultat ;
* `Annuler` ne modifie rien ;
* les items retournent bien dans l’inventaire ;
* aucun item n’est perdu ;
* l’inventaire et la hotbar sont visibles en bas à droite ;
* les équipements portés + offhand sont visibles en bas à gauche ;
* les slots fonctionnels remplis ont un contour turquoise électrique ;
* aucune logique fonctionnelle existante n’est cassée.

# Spécification visuelle détaillée — Interface moderne `Station de transmogrification`

## Objectif général

Créer une interface fullscreen moderne pour une station de transmogrification Minecraft moddé.

L’interface doit conserver la structure validée du croquis, mais avec une direction artistique moderne inspirée des interfaces de RPG récents, MMO stylisés et gacha RPG.

Le rendu doit évoquer une interface premium de jeu actuel :

* claire ;
* fluide ;
* lisible ;
* élégante ;
* animée ;
* magique ;
* moderne ;
* renderer-friendly.

L’interface ne doit pas ressembler à une interface médiévale ancienne, ni à une interface fantasy lourde avec beaucoup d’ornements.

Elle doit plutôt utiliser :

* formes géométriques simples ;
* panneaux semi-transparents ;
* dégradés ;
* opacités variables ;
* lueurs douces ;
* lignes fines ;
* effets magiques localisés ;
* textures très subtiles ;
* feedback utilisateur visible.

---

## Style artistique attendu

Direction artistique :

```text
Modern anime MMORPG / gacha RPG UI.
```

Inspirations visuelles :

```text
Blade & Soul, Genshin Impact, interfaces de gacha RPG modernes, menus d’action-RPG asiatiques récents.
```

Le style doit être :

* moderne ;
* propre ;
* légèrement futuriste ;
* magique ;
* premium ;
* minimaliste dans les formes ;
* riche uniquement dans les effets lumineux.

À éviter absolument :

* bordures médiévales lourdes ;
* ornements gothiques ;
* cadres anciens trop sculptés ;
* textures pierre / métal vieilli trop visibles ;
* style vieux MMO ;
* interface trop chargée ;
* rendu mobile app ;
* rendu web dashboard classique.

---

## Résolution et format

Format cible :

```text
1920 × 1080
16:9
fullscreen
GUI Scale 4 prioritaire
```

L’interface occupe une grande partie de l’écran, mais doit garder de l’air autour des zones.

Le design doit respirer.

Ne pas coller tous les éléments aux bords.

Prévoir des marges extérieures régulières.

---

## Palette de couleurs

Palette principale :

```text
Fond principal : violet très foncé saturé
Accents principaux : rose / magenta électrique
Accents secondaires : turquoise / cyan électrique
Texte principal : blanc légèrement violet
Texte secondaire : lavande clair
Fond des panneaux : violet noir semi-transparent
```

Codes HEX indicatifs :

```text
Violet fond profond : #12051F
Violet panneau : #1D0B32
Violet clair transparent : #42205F
Magenta électrique : #FF3FD8
Rose lumineux : #FF72E8
Cyan électrique : #23F7FF
Cyan doux : #73FFFF
Texte blanc-violet : #F4E9FF
Texte secondaire lavande : #CDB7E8
Erreur douce / alerte : #FF6BBA
Validation : #4DFFE1
```

Ces codes peuvent être ajustés, mais l’intention doit rester :

* violet foncé saturé dominant ;
* rose/magenta pour la magie et les effets ;
* cyan/turquoise pour les contours actifs et feedbacks ;
* blanc/lavande pour le texte.

---

## Matières et transparences

L’interface doit utiliser un rendu moderne en couches.

Les panneaux doivent être semi-transparents, avec un effet verre / HUD magique.

Utiliser :

```text
background rgba(18, 5, 31, 0.82)
panel rgba(29, 11, 50, 0.72)
secondary panel rgba(66, 32, 95, 0.35)
border rgba(255, 63, 216, 0.45)
active border rgba(35, 247, 255, 0.95)
```

Effets attendus :

* fond légèrement flouté ou assombri ;
* panneaux semi-transparents ;
* contours fins ;
* dégradés diagonaux très subtils ;
* lueurs diffuses ;
* particules très légères ;
* textures géométriques discrètes.

Ne pas utiliser de texture lourde.

Ne pas faire un cadre sculpté.

---

## Structure globale

L’interface est composée d’un grand panneau principal fullscreen.

Le panneau est divisé en deux grandes zones :

1. zone haute : transmogrification ;
2. zone basse : équipement porté + inventaire + hotbar + boutons.

Un séparateur horizontal fin sépare les deux zones.

Schéma général :

```text
┌─────────────────────────────────────────────────────────────┐
│                    Station de transmogrification             │
│                                                             │
│  Équipement à modifier     Aperçu du résultat     Apparence │
│        [slot]                  [slot]              à copier │
│     [message]            [améthyste + jauge]      [message] │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│ Équipement porté                  Inventaire                │
│ [armure + offhand]                [grille + hotbar]         │
│                                                             │
│ [Valider] [Annuler]                                        │
└─────────────────────────────────────────────────────────────┘
```

La disposition doit rester fidèle au croquis utilisateur.

---

## Panneau principal

Le panneau principal doit être grand, moderne et léger.

Caractéristiques :

```text
Position : centré
Largeur : environ 92 % de l’écran
Hauteur : environ 88 % de l’écran
Coins : légèrement arrondis
Fond : violet noir semi-transparent
Bordure : fine, magenta/violet, très discrète
Ombre : douce, diffuse
```

Le panneau ne doit pas avoir de grosses bordures décoratives.

Il peut avoir :

* coins géométriques simples ;
* petits losanges lumineux ;
* fines lignes d’accent ;
* traits horizontaux ;
* particules de fond.

---

## Titre principal

Texte :

```text
Station de transmogrification
```

Position :

```text
Haut centre de l’interface
```

Style :

* police propre, lisible, élégante ;
* pas trop médiévale ;
* blanc légèrement lavande ;
* léger glow magenta ;
* encadrement minimaliste avec lignes fines.

Proposition visuelle :

```text
Une ligne fine horizontale traverse partiellement le haut.
Le titre est centré dans une petite capsule transparente.
Deux petits losanges magenta encadrent le titre.
```

Ne pas utiliser de bannière fantasy lourde.

---

## Zone haute — organisation

La zone haute contient trois pôles alignés horizontalement :

1. gauche : équipement à modifier ;
2. centre : aperçu du résultat ;
3. droite : apparence à copier.

Chaque pôle possède :

* un label au-dessus ;
* un slot principal ;
* éventuellement un message dynamique en dessous ;
* des effets visuels propres.

Les trois pôles doivent être visuellement alignés.

Le slot central peut être légèrement plus mis en avant.

---

## Slot gauche — Équipement à modifier

Label visible :

```text
Équipement à modifier
```

Position :

```text
Haut gauche de la zone haute
```

Slot :

```text
Grand carré
Fond sombre semi-transparent
Contour cyan électrique si actif ou rempli
Intérieur avec motif géométrique discret
Icône + ou symbole de dépôt au centre si vide
```

Style :

* carré moderne ;
* angles légèrement arrondis ;
* contour fin mais lumineux ;
* effet cyan électrique ;
* halo faible autour du slot.

État vide :

```text
Contour cyan faible
Icône + au centre
Fond sombre
```

État rempli :

```text
Contour cyan plus lumineux
Liseré animé discret
Glow plus fort
Item affiché au centre
```

Tooltip :

```text
Placez ici l’arme ou la pièce d’équipement dont vous voulez modifier l’apparence.
```

---

## Message dynamique gauche

Position :

```text
Sous le slot Équipement à modifier
```

Texte par défaut :

```text
Placez un équipement à modifier.
```

Style :

* petit panneau semi-transparent ;
* icône information à gauche ;
* texte lavande clair ;
* bordure fine violette ;
* aucun gros ornement.

Comportement visuel :

```text
Le panneau peut apparaître avec un fade-in léger.
Le texte peut changer selon l’état du slot.
```

---

## Slot central — Aperçu du résultat

Label visible :

```text
Aperçu du résultat
```

Position :

```text
Haut centre de la zone haute
```

Ce slot est le point focal de l’interface.

Il doit être visuellement plus important que les deux autres, sans changer la structure.

Slot :

```text
Carré central
Contour cyan électrique
Fond violet noir semi-transparent
Motif géométrique discret
Item résultat affiché si disponible
```

Comportement :

```text
Le comportement interactif doit rester celui du mod existant.
Ne pas décider arbitrairement s’il est récupérable ou non.
```

---

## Cercle de runes autour du résultat

Autour du slot `Aperçu du résultat`, afficher un cercle magique moderne.

Style attendu :

```text
Cercle fin
Runes discrètes
Lignes géométriques
Segments circulaires
Particules magenta
Glow rose/magenta
```

Le cercle doit être élégant et moderne.

Il ne doit pas ressembler à un vieux cercle médiéval chargé.

États :

### État inactif

```text
Cercle visible mais peu lumineux
Rotation lente
Opacité réduite
Quelques particules faibles
```

### État valide

```text
Cercle plus lumineux
Pulsation douce
Rotation légèrement plus visible
Glow magenta plus fort
Petites particules supplémentaires
```

Animation recommandée :

```text
rotation continue lente
pulsation alpha entre 70 % et 100 %
petites particules ponctuelles autour du cercle
```

---

## Flèches de flux visuel

Entre les slots, afficher des flèches ou indicateurs de flux.

Disposition :

```text
Slot gauche → slot résultat ← slot droit
```

Style :

* flèches magenta ;
* lignes fines ;
* effet lumineux ;
* forme géométrique simple ;
* pas de grosses flèches médiévales.

Comportement :

```text
Les flèches peuvent s’illuminer quand les slots concernés sont remplis.
```

---

## Slot d’améthyste

Position :

```text
Sous le slot Aperçu du résultat
Au centre de l’interface
```

Label visible :

```text
Ravitaillement en améthyste
```

Slot :

```text
Petit carré
Contour cyan
Fond sombre transparent
Icône améthyste si item présent
```

Le slot d’améthyste doit être clairement lié à la jauge.

Ne pas placer la jauge près du résultat uniquement.

La jauge doit être près du slot d’améthyste.

---

## Jauge d’améthyste

Position :

```text
À côté du slot d’améthyste
Proche visuellement du slot d’améthyste
Sous l’aperçu du résultat
```

Style :

```text
Barre horizontale moderne
Fond violet sombre
Remplissage magenta / rose électrique
Bordure fine cyan ou magenta
Extrémités géométriques simples
```

État normal :

```text
Jauge visible
Remplissage selon le niveau d’améthyste
Texte possible : 85 %
```

État plein ou prêt :

```text
Pulsation douce
Glow rose/magenta plus visible
Petit effet de lumière qui traverse la jauge
```

État vide :

```text
Jauge presque éteinte
Message affiché : Rechargez en améthyste.
Pulsation d’alerte douce
Couleur moins agressive qu’une erreur classique
```

Message :

```text
Rechargez en améthyste.
```

---

## Slot droit — Apparence à copier

Label visible :

```text
Apparence à copier
```

Position :

```text
Haut droite de la zone haute
```

Slot :

```text
Grand carré
Fond sombre semi-transparent
Contour cyan électrique si actif ou rempli
Motif géométrique discret
Icône + si vide
```

Tooltip :

```text
Placez ici l’arme ou la pièce d’équipement dont l’apparence doit être copiée.
```

Ce slot peut recevoir l’item `Void` existant.

---

## Message dynamique droit

Position :

```text
Sous le slot Apparence à copier
```

Message recommandé :

```text
Laissez le slot d’apparence vide pour retirer l’apparence actuellement appliquée.
```

Style :

* panneau semi-transparent ;
* icône information à gauche ;
* texte lavande ;
* bordure fine violette ;
* coins légèrement arrondis.

Condition importante :

```text
Afficher ce message uniquement si le slot Équipement à modifier contient un item qui possède déjà une apparence appliquée.
Ce message s’affiche indépendamment de l’état du slot Apparence à copier.
```

---

## Comportement visuel de l’item Void

L’item `Void` existe déjà.

Ne pas le créer.

Ne pas inventer de nouvelle logique.

Quand le `Void` est placé dans `Apparence à copier`, afficher un message d’avertissement.

Message recommandé :

```text
Le Void sera consommé. L’apparence de votre équipement deviendra invisible après validation.
```

Style du message Void :

* panneau semi-transparent ;
* accent magenta ;
* icône d’avertissement douce ;
* pas de rouge agressif ;
* texte très lisible.

Description item recommandée :

```text
Utilisez cet item comme apparence pour rendre votre équipement invisible.
```

---

## Séparateur central horizontal

Entre la zone haute et la zone basse, ajouter une ligne de séparation.

Style :

```text
Ligne fine horizontale
Dégradé magenta vers transparent
Petit losange central
Opacité modérée
```

Le séparateur doit structurer l’interface sans l’alourdir.

---

## Zone basse gauche — Équipement porté

Titre :

```text
Équipement porté
```

Position :

```text
Bas gauche de l’interface
```

Contenu :

```text
5 slots au total
```

Slots :

1. casque ;
2. plastron ;
3. jambières ;
4. bottes ;
5. main secondaire / offhand.

Disposition :

```text
Les 4 slots d’armure sont alignés horizontalement.
Le slot main secondaire est séparé légèrement à droite.
```

Label du slot offhand :

```text
Main secondaire
```

Style :

* petits carrés modernes ;
* fond semi-transparent ;
* icônes gris-violet discrètes ;
* bordures fines ;
* très léger glow au survol.

État vide :

```text
Icône silhouette grisée
Fond sombre
Contour faible
```

État survolé :

```text
Contour cyan doux
Légère montée d’opacité
```

État sélectionné :

```text
Contour cyan plus visible
Petit glow externe
```

---

## Zone basse droite — Inventaire

Titre :

```text
Inventaire
```

Position :

```text
Bas droite de l’interface
```

Contenu :

```text
Inventaire joueur
Hotbar sous l’inventaire
```

Important :

```text
Ce n’est pas une interface vanilla brute.
C’est un rendu intégré dans le renderer de l’interface.
La taille globale doit rester équivalente à l’inventaire vanilla normal.
```

Style inventaire :

* grille propre ;
* cases simples ;
* fond violet noir semi-transparent ;
* bordures fines lavande/violet ;
* léger motif géométrique dans les cases ;
* pas de gros effets sur chaque case.

Hotbar :

```text
Placée sous l’inventaire
Même style que l’inventaire
Peut avoir des numéros discrets 1 à 9 si utile
```

Survol d’un slot inventaire :

```text
Contour cyan doux
Fond légèrement plus clair
```

Slot sélectionné :

```text
Contour cyan plus lumineux
Halo très léger
```

---

## Boutons d’action

Position :

```text
Tout en bas à gauche de l’interface
Sous la zone équipement porté
```

Ordre exact :

```text
Valider
Annuler
```

Bouton `Valider` :

```text
Texte : Valider
Couleur dominante : cyan / turquoise
Icône possible : coche
Style : moderne, lumineux, responsive
```

Bouton `Annuler` :

```text
Texte : Annuler
Couleur dominante : magenta / rose
Icône possible : croix
Style : moderne, lumineux, responsive
```

Les boutons doivent être :

* larges ;
* lisibles ;
* modernes ;
* légèrement anguleux ou rectangulaires ;
* avec coins modérément arrondis ;
* sans ornement ancien.

États visuels :

### Normal

```text
Fond semi-transparent coloré
Bordure lumineuse faible
Texte blanc
```

### Hover

```text
Fond plus opaque
Glow plus fort
Léger effet de brillance
```

### Pressed

```text
Fond plus sombre
Bouton légèrement compressé visuellement
Glow réduit pendant un court instant
```

### Disabled

```text
Opacité réduite
Couleur désaturée
Aucun glow fort
```

---

## Comportement de Valider

Tooltip recommandé :

```text
Applique le résultat affiché, puis renvoie les items restants dans votre inventaire.
```

Comportement fonctionnel :

```text
Valider applique la transmogrification affichée dans l’Aperçu du résultat.
Ensuite, tous les items encore présents dans l’interface retournent dans l’inventaire du joueur.
Aucun item ne doit être perdu.
```

Feedback visuel au clic :

```text
Flash cyan doux
Pulsation courte sur le bouton
Pulsation sur le cercle de runes
Jauge d’améthyste brièvement lumineuse
```

---

## Comportement de Annuler

Tooltip recommandé :

```text
Annule l’action, renvoie tous les items dans votre inventaire et ne modifie aucun équipement.
```

Comportement fonctionnel :

```text
Annuler ferme ou réinitialise l’interface.
Tous les items retournent dans l’inventaire.
Aucune modification n’est appliquée.
Aucun item ne doit être perdu.
```

Feedback visuel au clic :

```text
Flash magenta doux
Fade-out court de l’interface ou retour à l’état neutre
Aucun effet de validation
```

---

## États globaux de l’interface

### État initial

```text
Slots principaux vides
Cercle de runes visible mais faible
Jauge d’améthyste visible
Messages d’aide visibles
Bouton Valider désactivé ou peu lumineux si action impossible
Bouton Annuler disponible
```

### État équipement placé

```text
Slot Équipement à modifier lumineux
Message gauche mis à jour
Si l’équipement possède déjà une apparence, afficher le message “laisser vide pour retirer une apparence”
```

### État apparence placée

```text
Slot Apparence à copier lumineux
Flèche vers le résultat plus visible
Slot résultat commence à afficher l’aperçu
```

### État résultat valide

```text
Cercle de runes plus lumineux
Runes pulsantes
Bouton Valider activé
Jauge d’améthyste prête ou lumineuse
Message : Transmogrification prête.
```

### État améthyste vide

```text
Jauge faible ou vide
Message : Rechargez en améthyste.
Bouton Valider désactivé si la logique actuelle exige de l’améthyste
```

### État Void placé

```text
Slot Apparence à copier lumineux
Message spécial Void affiché
Accent magenta doux
Le joueur comprend que le Void sera consommé
```

---

## Animations recommandées

Les animations doivent être simples et réalisables dans un renderer.

Ne pas prévoir d’animation complexe impossible à maintenir.

Animations utiles :

```text
Fade-in des panneaux : 120 à 180 ms
Hover slot : 80 à 120 ms
Glow slot actif : pulsation lente
Rotation cercle de runes : continue lente
Pulsation résultat valide : douce
Pulsation jauge pleine : douce
Flash Valider : court
Flash Annuler : court
```

Valeurs indicatives :

```text
Durée hover : 100 ms
Durée clic bouton : 80 ms
Durée fade message : 150 ms
Rotation rune : 1 tour toutes les 8 à 14 secondes
Pulsation alpha : sinusoïde lente
```

---

## Priorités UX

L’utilisateur doit comprendre immédiatement :

1. quel item il modifie ;
2. quelle apparence il copie ;
3. quel sera le résultat ;
4. s’il manque de l’améthyste ;
5. si le Void sera consommé ;
6. comment valider ;
7. comment annuler sans risque.

L’interface doit fournir un retour utilisateur clair :

* slot rempli ;
* slot valide ;
* résultat prêt ;
* erreur douce ;
* action impossible ;
* validation réussie ;
* annulation sûre.

---

## Contraintes renderer-friendly

Le design doit pouvoir être rendu par un renderer Java Minecraft.

Favoriser :

* rectangles ;
* coins arrondis simples ;
* lignes fines ;
* dégradés ;
* opacité ;
* textures procédurales simples ;
* particules légères ;
* rotation d’un cercle de runes ;
* overlays transparents ;
* icônes simples.

Éviter :

* trop d’ornements dessinés à la main ;
* bordures complexes ;
* PNG massifs obligatoires ;
* effets 3D ;
* animations trop lourdes ;
* blur plein écran trop coûteux ;
* particules excessives ;
* shaders indispensables.

---

## Prompt compact pour générateur UI

```text
Create a fullscreen 1920x1080 modern anime MMORPG / gacha RPG style UI for a Minecraft modded transmogrification station.

The interface must be a large semi-transparent dark violet panel with clean geometric shapes, glass-like panels, subtle gradients, opacity layers, electric cyan slot highlights, magenta magical accents, and a modern premium game UI feel.

Do not make it old fantasy, medieval, gothic, or heavily ornate. Use simple shapes, thin lines, soft glow, transparent panels, and elegant effects.

Title at top center: “Station de transmogrification”.

Top section has three main slots aligned horizontally:
Left: “Équipement à modifier”.
Center: “Aperçu du résultat”.
Right: “Apparence à copier”.

Each slot has a visible label above it. The center result slot is surrounded by a modern magical rune circle, always visible, with magenta glow and subtle rotation. It becomes brighter and softly pulsates when the result is valid.

Under the center result slot, place an amethyst supply slot labeled “Ravitaillement en améthyste”, with a nearby horizontal amethyst gauge. The gauge uses magenta/pink fill and cyan/magenta borders. Add the message “Rechargez en améthyste.” when empty.

Under the left slot, add a semi-transparent info message panel: “Placez un équipement à modifier.”

Under the right slot, add a semi-transparent info message panel: “Laissez le slot d’apparence vide pour retirer l’apparence actuellement appliquée.”

Bottom left section: “Équipement porté”, with 5 slots: helmet, chestplate, leggings, boots, and “Main secondaire” offhand.

Bottom right section: “Inventaire”, with a stylized renderer-based inventory grid and hotbar underneath. It should keep the footprint of normal Minecraft inventory but look integrated into the modern UI, not vanilla.

Bottom left under the equipment section: two modern action buttons in this exact order: “Valider” then “Annuler”. Valider is cyan/turquoise with a check icon. Annuler is magenta/pink with an X icon.

Use saturated dark violet, electric pink/magenta, and electric turquoise/cyan. Make the interface readable, modern, clean, responsive, magical, and renderer-friendly.
```
