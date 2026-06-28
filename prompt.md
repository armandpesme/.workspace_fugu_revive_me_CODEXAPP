# Fugu Revive Me — Résumé décisionnel du mod

## Objectif du mod

**Fugu Revive Me** est un mod Forge 1.20.1 pour Fugu'Dreams_Online.

Le mod ajoute un système de mort MMORPG basé sur :

* la mort normale par défaut ;
* un état de **K.O. temporaire** dans certains biomes ;
* un état de **K.O. prolongé** dans certaines zones de boss ;
* une résurrection par un autre joueur ;
* un item d’auto-résurrection nommé **Ancre d’Âme** ;
* un pendentif de téléportation vers le spawn vanilla du joueur ;
* un spawn de mort générique configuré côté serveur ;
* des interfaces Java Renderer pour les états de K.O.

Le terme **Chaos** ne doit pas être utilisé.
C’était une erreur de speech-to-text.
Le terme correct est **K.O.**

---

## Principe général de mort

Par défaut, un joueur qui meurt **meurt normalement**.

Le joueur n’entre en K.O. que si les conditions prévues sont remplies.

Si le joueur meurt dans une zone non autorisée au K.O., il meurt définitivement.

Quand un joueur meurt définitivement, il est envoyé vers le **spawn de mort générique** du mod.

Le mod ne touche pas à l’économie de mort vanilla :

* pas de gestion d’inventaire ;
* pas de gestion d’XP ;
* pas de gestion de drops ;
* pas de règle de pénalité spécifique.

Ces règles sont déjà gérées par les gamerules serveur existantes.

---

## Spawn de mort générique

Le mod possède un **spawn de mort générique**.

Il sert uniquement aux morts définitives.

Il est configuré côté serveur avec :

* une dimension ;
* des coordonnées X, Y, Z ;
* éventuellement une orientation.

Tous les joueurs qui meurent définitivement y sont envoyés.

Cela concerne :

* les morts hors zone de K.O. ;
* les morts après échec du K.O. temporaire ;
* les morts après échec du K.O. prolongé ;
* les morts après rechute pendant le Mal de résurrection ;
* les morts volontaires via Libérer l’Esprit ;
* les morts automatiques après timeout du K.O. prolongé.

---

## Biomes de K.O.

Le K.O. est activé uniquement dans des biomes listés explicitement dans la configuration serveur.

Il ne faut pas utiliser de système par préfixe.

La configuration serveur doit prévoir deux listes distinctes :

1. liste des biomes de **K.O. temporaire** ;
2. liste des biomes de **K.O. prolongé**.

Les biomes doivent être écrits explicitement sous forme d’identifiants complets :

```txt
modid:nom_du_biome
```

La configuration doit accepter :

* 0 biome ;
* 1 biome ;
* beaucoup de biomes ;
* 100 biomes ou plus.

Les biomes par défaut seront fournis plus tard par FuguTeams.

---

## K.O. temporaire

Le **K.O. temporaire** est l’état de K.O. classique.

Quand un joueur devrait mourir dans un biome de K.O. temporaire :

* il ne meurt pas immédiatement ;
* il tombe K.O. ;
* il peut ramper lentement ;
* il dispose d’un timer de **60 secondes** ;
* un autre joueur peut venir le relever ;
* il peut utiliser une **Ancre d’Âme** si elle est dans sa hotbar ;
* s’il n’est pas relevé à temps, il meurt définitivement ;
* s’il reçoit trop de coups, il meurt définitivement.

La durée validée pour le K.O. temporaire est **60 secondes**.

---

## K.O. prolongé

Le **K.O. prolongé** fait partie de la V1.

Il est prévu pour les zones de boss.

Quand un joueur devrait mourir dans un biome de K.O. prolongé :

* le mod cherche un boss proche ;
* le boss doit appartenir au tag `fugu_boss` ;
* le joueur est lié au boss le plus proche au moment exact où il tombe K.O.

Le tag recommandé est :

```txt
#fugu_revive_me:fugu_boss
```

Fichier datapack probable :

```txt
data/fugu_revive_me/tags/entity_types/fugu_boss.json
```

Si un boss `fugu_boss` est trouvé :

* le joueur entre en K.O. prolongé ;
* il attend l’issue du combat ;
* il peut être relevé si les conditions le permettent ;
* il peut cliquer sur **Libérer l’Esprit** ;
* si le boss lié meurt ou disparaît, le joueur peut être ressuscité automatiquement sur place ;
* si le joueur reste trop longtemps à terre, il meurt définitivement.

Si aucun boss `fugu_boss` n’est trouvé :

* le joueur ne reste pas bloqué ;
* il bascule automatiquement en **K.O. temporaire classique de 60 secondes**.

---

## Garde-fous du K.O. prolongé

Le K.O. prolongé doit rester léger et ne pas bloquer les joueurs.

Deux garde-fous sont validés.

### 1. Bouton Libérer l’Esprit

Dans l’interface de K.O. prolongé, le joueur peut cliquer sur :

```txt
Libérer l’Esprit
```

Effet :

* le joueur abandonne l’attente ;
* il meurt définitivement ;
* il est envoyé au spawn de mort générique.

### 2. Timeout de 5 minutes

Si un joueur reste en K.O. prolongé plus de **5 minutes** :

* son esprit est automatiquement libéré ;
* il meurt définitivement ;
* il est envoyé au spawn de mort générique.

---

## Système des 3 coups

Un joueur K.O. ne doit pas être invulnérable.

Le système validé est une jauge simple de **3 coups**.

Pendant le K.O. :

* chaque coup reçu retire un cran ;
* au troisième coup, l’état se dégrade ou se termine.

En K.O. temporaire :

* le troisième coup entraîne la mort définitive.

En K.O. prolongé :

* le troisième coup fait passer le joueur en état **à terre complet**.

---

## État à terre complet

L’état **à terre complet** est une version plus grave du K.O.

Il concerne surtout le K.O. prolongé.

Dans cet état :

* le joueur ne peut plus être relevé normalement ;
* il ne peut pas utiliser l’Ancre d’Âme ;
* il attend l’issue du combat ;
* il peut utiliser **Libérer l’Esprit** ;
* il peut être ressuscité automatiquement si le boss lié est vaincu ;
* il meurt définitivement si le timeout de 5 minutes expire.

---

## Résurrection par un autre joueur

Un joueur K.O. peut être relevé par un autre joueur.

La règle des 20 blocs ne doit pas être une condition dure au moment de la chute.

Un joueur peut venir de loin, s’approcher, puis relever le joueur K.O.

Pour relever un joueur K.O., il faut :

* être vivant ;
* être dans la même dimension ;
* être au corps-à-corps du joueur K.O. ;
* maintenir une interaction pendant **5 secondes**.

L’action est annulée si :

* le joueur qui relève s’éloigne trop ;
* le joueur qui relève meurt ;
* le joueur qui relève change de dimension ;
* le joueur K.O. reçoit son troisième coup fatal pendant l’incantation ;
* le joueur K.O. meurt définitivement ;
* le joueur K.O. libère son esprit.

Après une résurrection réussie :

* le joueur revient avec environ **25 % de sa vie maximale** ;
* il reçoit le **Mal de résurrection** pendant 5 minutes.

---

## Mal de résurrection

Le **Mal de résurrection** empêche les boucles infinies de K.O.

Durée validée :

```txt
5 minutes
```

Pendant ce debuff :

* le joueur peut continuer à jouer ;
* il ne peut plus bénéficier d’un nouveau K.O. ;
* s’il devrait retomber K.O., il meurt définitivement ;
* il est envoyé au spawn de mort générique.

---

## Ancre d’Âme

Le mod ajoute un item nommé :

```txt
Ancre d’Âme
```

Le mod ne gère pas sa distribution quotidienne.

La distribution sera gérée ailleurs, par exemple par le système de quêtes journalières.

Le rôle de Fugu Revive Me est uniquement de :

* déclarer l’item ;
* rendre l’item fonctionnel ;
* permettre son utilisation pendant le K.O. temporaire.

L’Ancre d’Âme est utilisable uniquement en **K.O. temporaire**.

Elle n’est pas utilisable :

* en K.O. prolongé ;
* en état à terre complet ;
* après mort définitive ;
* pendant le Mal de résurrection.

Conditions d’utilisation :

* le joueur est en K.O. temporaire ;
* l’Ancre d’Âme est dans sa hotbar ;
* le joueur lance une auto-résurrection de 5 secondes ;
* l’action est annulée si le joueur reçoit un coup ;
* l’item n’est consommé que si la résurrection réussit ;
* si l’incantation est annulée, l’item reste dans la hotbar.

Après réussite :

* le joueur revient avec environ 25 % de vie ;
* il reçoit le Mal de résurrection pendant 5 minutes.

---

## Restrictions du joueur K.O.

Quand un joueur est K.O., il peut uniquement :

* ramper lentement ;
* bouger la caméra ;
* voir l’interface K.O. ;
* écrire dans le chat ;
* attendre une résurrection ;
* utiliser l’Ancre d’Âme si et seulement s’il est en K.O. temporaire.

Il ne peut pas :

* attaquer ;
* utiliser des items classiques ;
* ouvrir librement son inventaire ;
* interagir avec des blocs ;
* utiliser le pendentif ;
* se téléporter ;
* lancer des sorts ou compétences ;
* jouer normalement.

---

## Pendentif de téléportation

Le mod ajoute un pendentif de téléportation.

Le pendentif téléporte le joueur vers son **point de spawn vanilla actuel**.

La destination du pendentif n’est pas configurée dans le fichier serveur du mod.

Le mod ne maintient pas son propre système de coordonnées pour le pendentif.

Le pendentif utilise le système vanilla existant :

* lit ;
* point de spawn vanilla du joueur ;
* mécanique vanilla ou modifiée qui change le spawn du joueur.

Les PNJ, quêtes ou autres systèmes externes peuvent modifier le spawn vanilla du joueur si nécessaire.

Le pendentif est utilisable uniquement dans la **dimension principale**.

S’il est utilisé dans une autre dimension, il ne doit pas fonctionner.

Utilisation du pendentif :

* incantation d’environ **30 secondes** ;
* barre d’incantation ;
* particules, VFX ou effets visuels simples ;
* son d’incantation ;
* téléportation vers le spawn vanilla du joueur ;
* effet visuel d’arrivée ;
* son d’arrivée ;
* cooldown visible sur l’item.

La V1 doit privilégier une solution simple et stable : particules, sons, cooldown vanilla/mod standard.

---

## Interfaces Java Renderer

Le mod doit prévoir deux interfaces principales en Java Renderer.

L’objectif est d’avoir un rendu moderne, lisible, MMORPG, sans surcharger la V1 avec des assets complexes.

### Interface de K.O. temporaire

Elle doit afficher :

* timer de 60 secondes ;
* ambiance visuelle de danger ;
* effet de vision sanguine ou follow-view sanguin ;
* jauge des 3 coups restants ;
* indication que le joueur peut être relevé ;
* indication que l’Ancre d’Âme est utilisable si elle est dans la hotbar ;
* feedback clair si une résurrection est en cours.

### Interface de K.O. prolongé

Elle doit afficher :

* état plus grave que le K.O. temporaire ;
* information que le joueur attend l’issue du combat ;
* bouton **Libérer l’Esprit** ;
* timer maximum de 5 minutes ;
* état du lien au boss si disponible ;
* indication que la victoire du boss ou des alliés décidera du sort du joueur ;
* jauge ou état des 3 coups / état à terre complet.

---

## Lisibilité pour les autres joueurs

Les autres joueurs doivent pouvoir identifier rapidement un joueur K.O.

Solutions souhaitées :

* posture au sol ;
* animation de rampage si faisable proprement ;
* particules discrètes ;
* effet visuel de joueur sauvable ;
* son de mise à terre ;
* feedback clair pendant une résurrection.

La V1 doit rester soft-compatible.

Il ne faut pas surcharger le brief avec Epic Fight ou GeckoLib.

Le mod doit privilégier :

* événements Forge standards ;
* états serveur-client propres ;
* effets simples ;
* particules ;
* sons ;
* cooldowns ;
* debuffs ;
* overlays Java Renderer.

Les animations avancées pourront être ajoutées seulement si elles sont propres, stables et peu invasives.

---

## Compatibilité et philosophie technique

Le mod doit rester compatible avec un gros modpack.

Priorités :

1. stabilité ;
2. multijoueur ;
3. compatibilité soft ;
4. lisibilité joueur ;
5. maintenabilité ;
6. pas de hooks invasifs inutiles ;
7. pas de dépendances lourdes inutiles ;
8. pas de modification de l’économie de mort vanilla.

Le mod ne doit pas essayer de remplacer les systèmes de combat ou d’animation existants.

Il doit utiliser des mécaniques Minecraft / Forge standards autant que possible.

---

## Points encore à préciser plus tard

Les points suivants restent à définir ou affiner :

* rayon de recherche du boss `fugu_boss` ; 
  -> 20 blocks validé pour la V1 ;
* comportement exact si le boss despawn sans mort explicite ;
  -> Pas de mort explicite c'est whipe ou reset de boss donc mort.
* comportement si le joueur se déconnecte en K.O. ; 
  -> Ce reconnecte k.o temporaire sinon mort (reconnection point de respawn des mort)
* comportement si le joueur change de dimension pendant un état surveillé ;
  -> mort immédiate si changement de dimension pendant K.O. (tp zone des morts)
* niveau visuel exact des interfaces Java Renderer ;
  -> FULL Java Renderer, Premium Retour utilisateur, Premium 
* détails du cooldown du pendentif ;
  -> Cooldown indépendant des comptes visuels sur l'item et uniquement sur l' item. 
* durée exacte du cooldown du pendentif ; 
  -> Cooldown configurable dans le fichier config-server, par défaut 5 minutes. 
* nom interne des items, effets et états ;
  -> read `Fugu Revive Me — Noms internes proposés`

["nom de l'item pendantif"]
	#Channel duration in ticks (20 ticks = 1 second)
	#Range: 20 ~ 12000
	cast_time_ticks = 200
	#Cooldown applied after a successful teleport, in ticks
	#Range: 0 ~ 72000
	cooldown_ticks = 6000

[death_respawn]
	#Dimension id where players are teleported after respawn (server-side).
	#Example: fugubiomes:fugu_royaume_des_esprits
	dimension_id = "fugubiomes:fugu_royaume_des_esprits"
	#Respawn X coordinate
	#Range: -30000000 ~ 30000000
	x = 2428
	#Respawn Y coordinate
	#Range: -64 ~ 1024
	y = 66
	#Respawn Z coordinate
	#Range: -30000000 ~ 30000000
	z = -1805 

* liste des biomes par défaut ;
  -> tout biome fugu_biomes:fugu_special_*, fugu_biomes:fugu_low_*, fugu_biomes:fugu_mid_*, fugu_biomes:fugu_high_*, (list exact voir datapack `./datapack/BiomeFugu_datapack_v2.6/*`) est par défaut en K.O. prolongé, sauf si on la retirer la liste des biomes de K.O. prolongé dans le fichier config-server.

## Boss `fugu_boss`

Le K.O. prolongé ne repose pas sur une liste d’IDs de boss.

Le mod cherche simplement les entités proches qui possèdent le tag boss prévu :

`fugu_boss`

ou, en version namespacée recommandée :

`#fugu_revive_me:fugu_boss`

Le contenu exact du tag est géré ailleurs par FuguTeams, via datapack, configuration monde, système de spawn ou autre logique de map.

Le mod ne doit pas inventer, maintenir ou préremplir une liste de boss.

Il doit uniquement :

- vérifier si une entité proche correspond au tag boss ;
- choisir le boss taggé le plus proche au moment où le joueur tombe en K.O. prolongé ;
- lier le joueur à ce boss précis ;
- ressusciter le joueur si ce boss est vaincu ou disparaît ;
- basculer en K.O. temporaire si aucun boss taggé n’est trouvé. 

---

## Résumé ultra-court

Fugu Revive Me ajoute un système de mort MMORPG.

Le joueur meurt normalement par défaut.

Dans certains biomes listés côté serveur, il peut tomber K.O. au lieu de mourir.

En K.O. temporaire, il a 60 secondes pour être relevé ou utiliser une Ancre d’Âme.

En K.O. prolongé, il est lié au boss `fugu_boss` le plus proche et attend l’issue du combat, avec un bouton Libérer l’Esprit et un timeout de 5 minutes.

Après résurrection, il reçoit un Mal de résurrection de 5 minutes. S’il retombe K.O. pendant ce malus, il meurt définitivement.

Toutes les morts définitives envoient le joueur au spawn de mort générique configuré.

Le pendentif téléporte vers le spawn vanilla du joueur, uniquement depuis la dimension principale.

Le mod ne touche pas à l’inventaire, l’XP, les drops ou l’économie de mort vanilla.


## SFX / VFX de résurrection

Chaque résurrection réussie doit déclencher un feedback visuel et sonore clair.

Sources possibles de résurrection :

- résurrection par un allié ;
- résurrection automatique après défaite du boss lié en K.O. prolongé ;
- auto-résurrection via l’Ancre d’Âme.

Le mod doit utiliser un effet de résurrection commun pour garantir une identité visuelle cohérente.

### Effet commun recommandé

Quand le joueur est ressuscité :

- particules lumineuses autour du joueur ;
- effet vertical léger, comme une remontée d’âme ;
- flash doux au sol ;
- son de résurrection court et identifiable ;
- animation ou transition de relèvement si possible ;
- disparition de l’overlay K.O. ;
- application du Mal de résurrection.

### Variantes selon la source

#### Résurrection par allié

- effet commun de résurrection ;
- léger lien visuel entre l’allié et le joueur relevé ;
- son de soin / aide.

#### Résurrection par boss vaincu

- effet commun de résurrection ;
- variante plus large ou plus solennelle ;
- particules spirituelles ou dorées ;
- impression de victoire / libération.

#### Résurrection par Ancre d’Âme

- effet commun de résurrection ;
- variante plus personnelle ;
- particules centrées sur le joueur ;
- effet autour de l’item ou de la hotbar ;
- son plus mystique ou cristallin.

### Recommandation V1

Pour la V1, utiliser le même SFX/VFX de base pour les trois cas, avec seulement une variation légère si simple à faire.

Priorité :

1. effet visible et compréhensible ;
2. pas trop coûteux ;
3. pas de dépendance lourde ;
4. rendu compatible multijoueur ;
5. pas d’assets complexes obligatoires.
