# Fugu Revive Me V1 — Plan d’implémentation

> **Agents d’exécution :** utiliser `superpowers:subagent-driven-development`, avec un subagent neuf par jalon, revue de conformité puis revue qualité avant de continuer.

## État d’exécution — 2026-06-28

### Progression

- Fait : baseline humaine commitée sur `master` (`78ed7a5`).
- Fait : worktree créé sur `codex/fugu-revive-me-v1`.
- Fait : test Gradle initial réussi (`test NO-SOURCE`).
- Fait : index GitNexus dédié au worktree créé sous `.workspace_fugu_revive_me_CODEXAPP-v1`.
- Fait : Jalon 1 — fondation Forge et configuration, double revue validée.
- Fait : Jalon 2 — persistance SavedData clairsemée, réseau Forge, snapshots client et tests, double revue validée puis corrections qualité appliquées.
- Fait : Jalon 3 — interception de la mort, K.O. temporaire, restrictions serveur, relève alliée, Ancre d’Âme et respawn générique, double revue conformité/qualité, puis correctifs qualité appliqués.
- Fait : Jalon 4 — K.O. prolongé, recherche de boss taggé le plus proche, interdiction relève/ancre en PROLONGED, suivi de la mort et du despawn permanent du boss, résurrection sur position de K.O. y compris après reconnexion, timers continus joueur hors ligne, tests TDD avant code.
- Fait : Correctifs qualité Jalon 4 — résurrection après reconnexion (handler `PlayerLoggedInEvent`), suppression du double appel `tickExpirations` (Option A), suppression du code mort `extendOnFullyDowned`, extension de `transitionOnBossDespawn` à `FULLY_DOWNED`, ajout de `LOGGER` dans `ProlongedKoService`/`BossLinkRegistry`, conversion de `BossLinkRegistry` en structures concurrentes.
- Fait : Jalon 5 — pendentif de retour avec canalisation stricte 30 s, cooldown par stack NBT, annulation sans cooldown (mouvement > 0.1 bloc, dégât, changement slot/item, mort, dimension, déconnexion), téléport depuis dimension principale uniquement avec repli vanilla, tests TDD avant code.
- En cours : Jalon 6 — interfaces, VFX et assets.
- Reste : Jalons 6 à 7, QA runtime et release `1.0.1`.

### Surprises et discovery

- Le MDK de baseline ne contient finalement aucun fichier Java, uniquement Gradle et les ressources Forge minimales.
- L’index GitNexus doit être relancé après chaque jalon majeur pour connaître les nouveaux symboles.
- La première revue qualité a détecté l’icône d’effet manquante et un warning d’overlap non branché; les deux défauts sont corrigés et couverts par tests.
- Forge 47.4.20 accepte l’injection directe de `FMLJavaModLoadingContext` dans le constructeur `@Mod`.
- Les tests JUnit purs ne peuvent pas instancier un `SimpleChannel` Forge sans bus Forge complet; le channel réseau est donc initialisé lazy en runtime et vérifié par compilation/build + garde-fous source.
- `ClientboundTrackedKoVisual` ne doit jamais être envoyé avec `entityId=0` par défaut; quand l’entityId n’est pas connu, le service envoie seulement le snapshot self.
- 2026-06-28 / Jalon 3 : `LivingEntityUseItemEvent.Start` est l’event Forge 1.20.1 équivalent à `UseItemEvent.Start`; `net.minecraftforge.event.entity.player.UseItemEvent` n’existe pas.
- 2026-06-28 / Jalon 3 : `ServerTickEvent.phase` est un champ, pas une méthode (`event.phase != TickEvent.Phase.END`).
- 2026-06-28 / Jalon 3 : `EntityType` doit être importé de `net.minecraft.world.entity.EntityType` (et non de `Entity`); le tag est résolu via `TagKey.create(Registries.ENTITY_TYPE, id)`.
- 2026-06-28 / Jalon 3 : `ServerPlayer#teleportTo(ServerLevel, double, double, double, float, float)` retourne `void`, pas `boolean`.
- 2026-06-28 / Correctifs qualité Jalon 3 : les tests JUnit purs ne peuvent pas initialiser `Registries.ENTITY_TYPE` (Bootstrap Forge non appelé) ; la valeur exposée par `parseBossTag` est donc un `ResourceLocation` et non un `TagKey`, et la conversion est faite au point d’utilisation dans `findNearestBoss`.
- 2026-06-28 / Correctifs qualité Jalon 3 : en Java 17, `case X -> {}` (corps vide sur une nouvelle ligne) ne compile pas ; le `case CONTINUE` est conservé en `case CONTINUE -> { break; }` pour garantir la portabilité du `switch` rule.
- 2026-06-28 / Jalon 4 : `EntityLeaveLevelEvent` est l’event Forge 1.20.1 pour un boss qui quitte le niveau (mort, despawn ou décharge de chunk). Le service `ProlongedKoService.onBossDespawn` est exposé comme hook public et n’est appelé que pour les boss effectivement liés (`bossLinks.isLinkedToAnyPlayer`) afin d’éviter un faux positif sur décharge de chunk. La distinction chunk-unload vs despawn-permanent est une simplification documentée ; le test couvre l’orchestration du service, la détection runtime fine pourra être raffinée au Jalon 7.
- 2026-06-28 / Jalon 4 : pour permettre les tests JUnit sans instancier un `ServerPlayer` (final), un `ProlongedKoService.ResurrectionApplier` est introduit. La valeur par défaut côté Forge est `ServerPlayerResurrectionApplier` qui résout `ServerLevel` via `MinecraftServerGuard.getServer()` et applique teleport, santé, Mal de résurrection.

### Decision log

- 2026-06-28 / Baseline : conserver l’ensemble du snapshot humain validé, puis isoler l’implémentation dans un worktree.
- 2026-06-28 / GitNexus : utiliser `--index-only` pour ne pas modifier automatiquement les documents protégés.
- 2026-06-28 / Configuration : matérialiser les valeurs serveur dans un snapshot immuable remplacé atomiquement lors des chargements/rechargements.
- 2026-06-28 / Assets : fournir immédiatement l’icône de tout effet visible enregistré et vérifier son intégrité dans le JAR.
- 2026-06-28 / Jalon 2 : encoder l’issue différée via les états persistants `PENDING_DEATH` / `PENDING_REVIVE` plutôt qu’un champ séparé, afin de garder `KoRecord` compact.
- 2026-06-28 / Jalon 2 : isoler les handlers client via `DistExecutor` et réflexion contrôlée, sans import du package client depuis le réseau common, pour réduire le risque dedicated server.
- 2026-06-28 / Jalon 2 : exposer un lookup optionnel `UUID -> entityId` dans `ReviveService` pour envoyer les visuals tracking seulement quand l’entité est connue.
- 2026-06-28 / Jalon 3 : découper l’orchestration en services purs testables (`KnockoutStateLogic`, `AllyReviveLogic`, `SoulAnchorLogic`) et services d’orchestration (`ReviveService`, `AllyReviveService`, `SoulAnchorService`) branchés par `KoEventHandlers` côté DEDICATED_SERVER uniquement.
- 2026-06-28 / Jalon 3 : les actions de revive (relève alliée, ancre) sont gérées en mémoire par `KnockoutActionRegistry`; aucune persistance disque, ce qui simplifie la reprise après un restart serveur et reste cohérent avec le statut “live interaction”.
- 2026-06-28 / Jalon 3 : la restriction de vitesse K.O. utilise une `MovementOverrideRegistry` qui stocke la valeur originale de l’attribut MOVEMENT_SPEED et la restaure à la sortie du K.O.; le `PlayerTickEvent` (Phase.END) est utilisé pour appliquer la valeur.
- 2026-06-28 / Jalon 3 : le `FuguKnockoutRuntime` est construit lors de `ServerStartedEvent` (bus FORGE) et expose les singletons aux handlers; il est reset entre deux sessions de jeu.
- 2026-06-28 / Jalon 3 : le `KnockoutRespawnService.shouldTransfer` n’accepte que PENDING_DEATH, PENDING_REVIVE et DEAD_PENDING_TRANSFER (les autres états ne déclenchent pas de respawn générique forcé).
- 2026-06-28 / Jalon 3 : le K.O. prolongé partage la même mécanique que le temporaire pour ce jalon (compte de coups, transition FULLY_DOWNED), mais le suivi de la mort du boss et du de spawn/reset permanent est repoussé au Jalon 4.
- 2026-06-28 / Correctifs qualité Jalon 3 : la signature `MovementOverrideRegistry.forget(UUID)` retourne désormais un `OptionalDouble` (valeur stockée si présente, vide sinon) ; le handler de mouvement utilise la valeur retournée pour restaurer la valeur d’origine, ce qui corrige la régression de vitesse pour les joueurs avec modificateurs (Soul Speed, armure, effets).
- 2026-06-28 / Correctifs qualité Jalon 3 : `FuguKnockoutRuntime.RuntimeConfig` inclut désormais `prolongedBossTag` (lu depuis `ServerConfig.PROLONGED_KO_BOSS_TAG`); `findNearestBoss` consomme le tag et le rayon depuis le `RuntimeConfig` au lieu de constantes hardcodées.
- 2026-06-28 / Correctifs qualité Jalon 3 : un handler `PlayerLoggedOutEvent` est ajouté dans `KoEventHandlers` pour vider toutes les entrées transitoires (`KnockoutActionRegistry`, `MovementOverrideRegistry`, `LastSafePositionTracker`, `KnockoutDamageTracker`) afin d’éviter les fuites mémoire à la déconnexion. Le nettoyage est implémenté par `clearTransientPlayerState` qui prend les registres individuellement, pour rester testable sans `MinecraftServer`.
- 2026-06-28 / Correctifs qualité Jalon 3 : un handler `ServerStoppedEvent` est ajouté pour appeler `FuguKnockoutRuntime.reset()`, ce qui garantit qu’un ancien runtime n’est pas retenu après l’arrêt de singleplayer.
- 2026-06-28 / Correctifs qualité Jalon 3 : suppression du code mort `KoPlayerProbe`, de `KnockoutActionRegistry.findActiveForHelper` (jamais appelé en production) et des imports inutilisés dans `KoEventHandlers`, `KnockoutDamageTracker`, `AllyReviveLogic`, `SoulAnchorLogic`, `KnockoutRestrictionService`, `AllyReviveService`.
- 2026-06-28 / Jalon 4 : interpréter « Sans boss, démarrer un K.O. temporaire neuf de 60 secondes » comme `TEMPORARY_KO` (choix A) avec `TEMPORARY_KO_DURATION_TICKS` (1200 = 60 s) plutôt que `PROLONGED_KO` raccourci (choix B). L’uniformité avec la mécanique existante est préférée, et le `K.O. temporaire neuf » est plus lisible côté client (overlay, jauge trois coups).
- 2026-06-28 / Jalon 4 : exposer la liste boss→joueurs via `BossLinkRegistry` in-memory séparé du `KnockoutSavedData.playersByBoss`, afin d’éviter qu’une écriture disque différée (chunk unload) fasse fuiter la liste de boss. Le registre est reconstruit à l’entrée en `PROLONGED_KO` (`linkBossToPlayer`) et nettoyé sur transition durable (mort boss, despawn, timeout, changement de dimension, résurrection).
- 2026-06-28 / Jalon 4 : la transition `PROLONGED → PENDING_REVIVE` est faite avec une deadline courte (`now + 1`) afin de signaler « respawn imminent » sans introduire un nouvel état. Le respawn effectif est piloté par `PlayerEvent.PlayerRespawnEvent` qui appelle `ProlongedKoService.resurrectOnKoPosition`.
- 2026-06-28 / Jalon 4 : la `KnockoutRestrictionService` est étendue à `PROLONGED_KO` et `FULLY_DOWNED` pour l’input (attaque, item, bloc, inventaire, téléport), conformément au plan. Le service de chat et la caméra restent toujours autorisés.
- 2026-06-28 / Jalon 4 : `AllyReviveLogic` et `SoulAnchorLogic` reçoivent un nouveau `StartDenial.TARGET_IN_PROLONGED_KO` pour rejeter explicitement la relève alliée et l’Ancre d’Âme pendant le K.O. prolongé, conformément à l’exigence « Interdire relève alliée et Ancre d’Âme ». `TARGET_NOT_IN_TEMPORARY_KO` reste utilisé pour les autres états non éligibles.
- 2026-06-28 / Jalon 4 : `BossLinkRegistry.unlinkBossByPlayer(UUID)` est ajouté pour permettre un nettoyage explicite par joueur (utilisé par `unlinkPlayer` quand le record n’existe plus) ; le reste du code passe par `unlink` ciblé ou `unlinkBoss` global.
- 2026-06-28 / Jalon 4 : `unlinkPlayer` n’est PAS appelé sur `PlayerLoggedOutEvent` afin de préserver la capacité de traiter une mort de boss pendant que le joueur est hors ligne (la transition `PENDING_REVIVE` est persistée disque et appliquée à la reconnexion via `PlayerRespawnEvent`).
- 2026-06-28 / Correctifs qualité Jalon 4 : un handler `PlayerEvent.PlayerLoggedInEvent` est ajouté dans `KoEventHandlers` pour appeler `ProlongedKoService.resurrectOnKoPosition` sur un joueur qui se reconnecte avec un record `PENDING_REVIVE`. La logique est extraite dans `KoEventHandlers.tryResurrectOnReconnect(ProlongedKoService, UUID)` pour rester testable sans `MinecraftServer`. Cela corrige le bug critique identifié en revue : un joueur dont le boss meurt pendant qu’il est hors ligne reste bloqué en `PENDING_REVIVE` indéfiniment.
- 2026-06-28 / Correctifs qualité Jalon 4 : le wire-up du tick serveur est factorisé dans `KoEventHandlers.tickServerState(FuguKnockoutRuntime, MinecraftServer)`. Le double appel `runtime.revive().tickExpirations()` puis `runtime.prolonged().tickExpirations()` (qui consommait deux fois `pollDue`) est supprimé : seul `revive().tickExpirations()` est appelé, car la méthode couvre déjà les trois états à deadline (`TEMPORARY_KO`, `PROLONGED_KO`, `FULLY_DOWNED`) via `ReviveState.hasExpiringKoDeadline()`. `ProlongedKoService.tickExpirations` reste en place pour ses tests unitaires et pour ne pas modifier l’API publique.
- 2026-06-28 / Correctifs qualité Jalon 4 : `ProlongedKoLogic.extendOnFullyDowned` est supprimé (code mort : la transition `FULLY_DOWNED` est gérée par `KnockoutStateLogic.applyHit` qui utilise déjà la même constante `FULLY_DOWNED_EXTENSION_TICKS = 6_000L`). Les tests associés `extendOnFullyDownedTransitionsToFullyDownedAndExtendsDeadline` et `extendOnFullyDownedRejectsNonKoStates` sont supprimés.
- 2026-06-28 / Correctifs qualité Jalon 4 : `ProlongedKoLogic.transitionOnBossDespawn` accepte désormais `PROLONGED_KO` ET `FULLY_DOWNED` (cohérence avec « despawn/reset permanent : mort définitive »). Un test `transitionOnBossDespawnMovesFullyDownedToPendingDeath` couvre le cas `FULLY_DOWNED` + despawn → `PENDING_DEATH`. Le test existant `transitionOnBossDespawnIgnoresNonProlongedStates` est renommé en `transitionOnBossDespawnIgnoresNonEligibleStates` et étendu pour exclure aussi `PENDING_REVIVE`.
- 2026-06-28 / Correctifs qualité Jalon 4 : un `Logger` Log4j est ajouté dans `ProlongedKoService` et `BossLinkRegistry`, avec des logs au niveau `INFO` pour les transitions durables (résurrection, mort/despawn boss, changement de dimension) et `DEBUG` pour les opérations de bas niveau (link/unlink, timeout fire).
- 2026-06-28 / Correctifs qualité Jalon 4 : `BossLinkRegistry` passe de `HashMap` à `ConcurrentHashMap`, et le set imbriqué utilise `Collections.newSetFromMap(new ConcurrentHashMap<>())` pour la thread-safety. Les 18 tests existants restent verts et 2 tests supplémentaires valident la robustesse concurrente (`concurrentLinkAndUnlinkDoNotCorruptState`, `playersLinkedToReturnsDefensiveCopyAfterConcurrentMutation`).
- 2026-06-28 / Jalon 5 : le pendentif de retour suit le pattern Logic/Service/Handlers existant. `ReturnPendantLogic` expose des fonctions pures (`evaluateStart`, `evaluateTick`) avec sealed interfaces pour les verdicts. `ReturnPendantService` orchestre les casts actifs in-memory (`ConcurrentHashMap<UUID, ReturnPendantCast>`) et expose des callbacks pour le cooldown et le téléport. Le cooldown est stocké sur le tag NBT `CooldownEnd` de l'`ItemStack`, donc indépendant par stack. La téléportation utilise `player.getRespawnPosition()` avec repli vers `server.overworld().getSharedSpawnPos()`. Le `KoEventHandlers` branche les events Forge (RightClickItem, LivingHurt, RightClickBlock, LeftClickBlock, PlayerChangedDimension, PlayerLoggedOut, LivingDeath, ServerTick) pour l'activation, l'annulation et le tick des casts.
- 2026-06-28 / Jalon 5 : `ReturnPendantLogic.StartRejection.ALREADY_CASTING` est ajouté pour rejeter explicitement un deuxième cast simultané par le même joueur, en cohérence avec le pattern `ALREADY_ACTIVE` de `SoulAnchorLogic`. Le `ReturnPendantLogic.TickOutcome.NOT_CASTING` est ajouté pour distinguer l'absence de cast d'un verdict normal.
- 2026-06-28 / Jalon 5 : le `RuntimeConfig` est étendu avec 4 nouveaux champs (`returnPendantMainDimension`, `returnPendantCastTimeTicks`, `returnPendantCooldownTicks`, `returnPendantMovementTolerance`) et la méthode `returnPendantConfig()` qui construit le `ReturnPendantService.ReturnPendantConfig`. Le `FuguKnockoutRuntimeConfigTest` est mis à jour pour inclure les nouveaux paramètres.

### Outcome et retrospective

- `.\gradlew.bat test` exécuté depuis le worktree `project-gradle/` : `BUILD SUCCESSFUL in 11s`, aucune source de test ou Java présente.
- Jalon 1 : `.\gradlew.bat test --rerun-tasks` et `.\gradlew.bat build --rerun-tasks` réussis; 23 tests, aucun échec.
- Jalon 2 : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 9s`, 47 tests, 0 échec, 0 erreur.
- Jalon 2 : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 8s`.
- Jalon 3 : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 10s`, 121 tests, 0 échec, 0 erreur.
- Jalon 3 : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 7s`.
- Artefacts produits : `project-gradle/build/libs/fugu_revive_me-1.0.1.jar` (131 568 bytes) et `project-gradle/build/libs/fugu_revive_me-1.0.1-sources.jar` (51 950 bytes).
- Correctifs qualité Jalon 3 (commit `fix(jalon-3)`) : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 10s`, 133 tests, 0 échec, 0 erreur. +12 tests (4 pour `parseBossTag`, 2 pour `RuntimeConfig`, 3 pour `clearTransientPlayerState`, 3 nouveaux pour `MovementOverrideRegistry.forget` dont le retour `OptionalDouble` et 1 pour `KnockoutDamageTracker.clear(UUID)`), -1 test renommé (`multipleHelpersTargetSamePlayer` → `singleHelperFindsActionByHelperUuid`).
- Correctifs qualité Jalon 3 (commit `fix(jalon-3)`) : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 8s`.
- Correctifs qualité Jalon 3 (commit `fix(jalon-3)`) : Artefacts produits : `project-gradle/build/libs/fugu_revive_me-1.0.1.jar` (131 765 bytes) et `project-gradle/build/libs/fugu_revive_me-1.0.1-sources.jar` (51 165 bytes).
- Jalon 4 (commit `feat(jalon-4)`) : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 12s`, 197 tests, 0 échec, 0 erreur. +64 tests (ProlongedKoLogicTest 20, BossLinkRegistryTest 18, ProlongedKoServiceTest 23, complétion des restrictions dans KnockoutRestrictionServiceTest, AllyReviveLogicTest, AllyReviveServiceTest, SoulAnchorLogicTest, SoulAnchorServiceTest).
- Jalon 4 (commit `feat(jalon-4)`) : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 20s`.
- Jalon 4 (commit `feat(jalon-4)`) : Artefacts produits : `project-gradle/build/libs/fugu_revive_me-1.0.1.jar` (149 811 bytes) et `project-gradle/build/libs/fugu_revive_me-1.0.1-sources.jar` (56 824 bytes).
- Correctifs qualité Jalon 4 (commit `fix(jalon-4)`) : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 14s`, 206 tests, 0 échec, 0 erreur. +9 tests (BossLinkRegistryTest +2 concurrent, ReviveServiceTest +3 wire-up/prolonged/expiration, ProlongedKoServiceTest +1 despawn FULLY_DOWNED, ReconnectResurrectionTest +4 nouveau fichier), -2 tests supprimés (`extendOnFullyDownedTransitionsToFullyDownedAndExtendsDeadline` et `extendOnFullyDownedRejectsNonKoStates`), 1 test renommé (`transitionOnBossDespawnIgnoresNonProlongedStates` → `transitionOnBossDespawnIgnoresNonEligibleStates`).
- Correctifs qualité Jalon 4 (commit `fix(jalon-4)`) : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 9s`.
- Correctifs qualité Jalon 4 (commit `fix(jalon-4)`) : Artefacts produits : `project-gradle/build/libs/fugu_revive_me-1.0.1.jar` (150 535 bytes) et `project-gradle/build/libs/fugu_revive_me-1.0.1-sources.jar` (57 360 bytes).
- Jalon 5 (commit `feat(jalon-5)`) : `.\gradlew.bat test` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 12s`, 248 tests, 0 échec, 0 erreur. +42 tests (ReturnPendantLogicTest 13, ReturnPendantServiceTest 16, ReturnPendantCastTest 5, ReturnPendantConfigTest 8).
- Jalon 5 (commit `feat(jalon-5)`) : `.\gradlew.bat build` exécuté depuis `project-gradle/` le 2026-06-28, `BUILD SUCCESSFUL in 8s`.
- Jalon 5 (commit `feat(jalon-5)`) : Artefacts produits : `project-gradle/build/libs/fugu_revive_me-1.0.1.jar` (167 679 bytes) et `project-gradle/build/libs/fugu_revive_me-1.0.1-sources.jar` (61 257 bytes).
- Revues : Jalon 1 conformité/qualité validées; Jalon 2 conformité validée; qualité validée après corrections sur frontière client/common et visuals tracking; Jalon 3 conformité validée; qualité validée après corrections sur la restauration de vitesse, la lecture de la config boss, la fuite mémoire à la déconnexion et le reset du runtime à l’arrêt; Jalon 4 conformité PARTIELLEMENT CONFORME (1 critique + 1 majeure) et qualité BON (1 majeur + 1 mineur) après corrections sur le handler de reconnexion, la suppression du double `tickExpirations`, l'extension de `transitionOnBossDespawn` à `FULLY_DOWNED`, la suppression du code mort et l'ajout de loggers/structures concurrentes.
- Risque restant : le `KoEventHandlers` n’a pas encore été couvert par un test d’intégration runtime (runClient/runServer) — à planifier avant la release. La détection chunk-unload vs despawn-permanent est volontairement simplifiée dans le wire-up (hook `onBossDespawn` appelé seulement si le boss est lié à un joueur) ; un affinage (par exemple via `MobSpawnEvent.AllowDespawn` ou un suivi `EntityJoinLevelEvent` / `EntityLeaveLevelEvent` chronométré) sera étudié au Jalon 7. Suite aux correctifs qualité Jalon 4, le wire-up de `onServerTick` ne nettoie plus le `BossLinkRegistry` sur timeout de KO (seul `ReviveService.tickExpirations` est appelé, qui ne touche pas le registre). Cela laisse un lien orphelin pour chaque joueur en `PROLONGED_KO`/`FULLY_DOWNED` qui expire par timeout. Le lien est ensuite ignoré par `onBossDeath`/`onBossDespawn` grâce au filtrage par état (`transitionOnBossDeath`/`transitionOnBossDespawn` ignorent les états non-`PROLONGED_KO`/`FULLY_DOWNED`) ; un nettoyage dédié pourra être ajouté au Jalon 7 si la fuite devient visible.
- Vérification non exécutée : `runServer` / `runClient`, car Jalon 4 reste infrastructure serveur et ces tâches nécessitent un smoke runtime long ou interactif; à planifier avant validation gameplay finale.

### Reprise agent sans état

Branche courante : `codex/jalon-5` basée sur `codex/jalon-4` (`f73476a`). Prochaine action : déléguer le Jalon 6 (interfaces, VFX et assets) à un agent stitch-designer puis gameplay-engineer et asset-manager, après une revue conformité/qualité distincte du Jalon 5.

## 1. Résumé

**Objectif :** livrer `fugu_revive_me` 1.0.1 sous Minecraft 1.20.1, Forge 47.4.20 et Java 17.

**Architecture :** serveur autoritaire, machine d’états persistante, synchronisation événementielle par packets, interfaces Java Renderer et aucune dépendance obligatoire à GeckoLib ou Epic Fight.

**État initial vérifié :**

- `project-gradle/` est un MDK `examplemod` sans gameplay Fugu.
- `.agent/PLAN.md` a été vidé pendant l’analyse; il doit recevoir ce plan.
- `.agents/PLAN.md` reste un pointeur vers le plan canonique.
- Le plan Transmog historique appartient à un autre dépôt et doit être ignoré.
- Le datapack local contient 199 biomes sous `fugubiomes`, dont 108 biomes prolongés explicites.

## 2. Contrats publics et décisions

### Identité et ressources

- Mod ID : `fugu_revive_me`.
- Package : `com.fuguteams.fugureviveme`.
- Items :
  - `fugu_revive_me:soul_anchor`
  - `fugu_revive_me:return_pendant`
- Effet visible : `fugu_revive_me:resurrection_sickness`.
- Tag boss : `#fugu_revive_me:fugu_boss`, sous `data/fugu_revive_me/tags/entity_types/`.
- Les états K.O. restent internes; absence d’enregistrement persistant signifie `ALIVE`.

### États et persistance

Créer `ReviveState`, `KoRecord`, `KnockoutSavedData` et `ReviveService`.

États persistants :

- `TEMPORARY_KO`
- `PROLONGED_KO`
- `FULLY_DOWNED`
- `PENDING_REVIVE`
- `PENDING_DEATH`
- `DEAD_PENDING_TRANSFER`

`KnockoutSavedData` est attaché à l’Overworld et ne contient que les joueurs actifs ou en attente. Il stocke des échéances absolues basées sur `overworld.getGameTime()`, jamais des compteurs décrémentés.

Contraintes de performance obligatoires :

- aucun scan de tous les joueurs ou de tout l’historique à chaque tick ;
- file de priorité pour les échéances ;
- index boss → joueurs uniquement en mémoire ;
- `setDirty()` seulement lors d’une transition durable ;
- aucun packet par tick ;
- suppression du record dès le retour définitif à l’état sain.

Cette structure suit les mécanismes documentés de [SavedData](https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/) et évite les limitations hors ligne des [capabilities joueur](https://docs.minecraftforge.net/en/1.20.1/datastorage/capabilities/).

### Configuration serveur

Valeurs par défaut :

- Spawn des morts : `fugubiomes:fugu_royaume_des_esprits`, `2428, 66, -1805`, yaw/pitch `0`.
- K.O. temporaire : liste vide, 1200 ticks, 3 coups, relève 100 ticks, distance 3 blocs, santé restaurée 25 %.
- K.O. prolongé : les 108 IDs locaux explicites, rayon boss 20 blocs, timeout 6000 ticks, 3 coups.
- Mal de résurrection : 6000 ticks.
- Pendentif : dimension `minecraft:overworld`, cast 600 ticks, cooldown 6000 ticks.

Toutes ces valeurs d’équilibrage sont configurables. Aucun préfixe n’est résolu à l’exécution. Un biome présent dans les deux listes utilise le K.O. prolongé et génère un avertissement.

### Réseau

Créer un `SimpleChannel` versionné conformément à [SimpleImpl](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/) :

- `ClientboundReviveSnapshot` : état du joueur, échéances, coups, action active et état du boss.
- `ClientboundTrackedKoVisual` : état visuel minimal pour les joueurs qui suivent l’entité.
- `ServerboundReleaseSpirit` : intention sans donnée autoritaire.

Le client interpole les timers depuis le couple temps serveur/échéance. Toute action reçue est revalidée côté serveur.

## 3. Jalons d’implémentation

### Jalon 0 — Baseline et isolation

- Écrire ce plan dans `.agent/PLAN.md`.
- Revoir les modifications humaines existantes, créer le commit snapshot validé, puis le worktree `codex/fugu-revive-me-v1`.
- Exécuter `.\gradlew.bat test` comme baseline.
- Réindexer GitNexus, puis effectuer l’analyse d’impact obligatoire avant chaque modification de symbole.
- Ne pas modifier `AGENTS.md`, `README.md` ou les autres documents protégés.

### Jalon 1 — Fondation Forge

Responsable : `architect`.

- Remplacer entièrement l’identité `examplemod`.
- Structurer les packages `config`, `registry`, `state`, `server`, `network`, `item`, `client` et `command`.
- Configurer `mods.toml`, le nom du JAR, les JAR sources et la version de développement.
- Enregistrer les deux items et le Mal de résurrection avec `DeferredRegister`.
- Ajouter config serveur, validations des IDs et extraction unique des 108 biomes vers une liste littérale.
- Produire les tests de configuration avant l’implémentation.

### Jalon 2 — Persistance et réseau

Responsable : `network-agent`.

- Implémenter le dépôt SavedData clairsemé, la file d’échéances et l’index boss.
- Persister position/dimension de K.O., boss lié, état, coups et issue différée.
- Maintenir en mémoire la dernière position sûre, échantillonnée toutes les 10 ticks sans écriture disque.
- En absence de position sûre, utiliser la résolution de spawn vanilla comme repli de K.O.
- Implémenter les snapshots et l’interpolation client sans synchronisation périodique.
- Ajouter des tests prouvant qu’un tick sans échéance ne modifie pas les données et n’émet aucun packet.

### Jalon 3 — Mort définitive et K.O. temporaire

Responsable : `gameplay-engineer`.

- Intercepter toute cause de mort dans un biome éligible, y compris vide et commandes.
- Le Mal de résurrection interdit toujours un nouveau K.O.
- Hors biome, conserver intégralement mort, drops, XP et écran vanilla.
- Après respawn définitif, transférer le joueur au spawn générique.
- K.O. temporaire : déplacement lent au sol, caméra et chat autorisés; attaque, inventaire, objets ordinaires, blocs et téléportation interdits.
- Troisième coup : mort définitive.
- Relève alliée : interaction au corps-à-corps pendant 5 secondes; annulation selon les conditions du prompt.
- Ancre d’Âme : hotbar uniquement, canalisation 5 secondes, annulation au moindre dégât, consommation seulement à la réussite.
- Toute résurrection restaure 25 % de vie et applique le Mal de résurrection.

### Jalon 4 — K.O. prolongé

Responsable : `gameplay-engineer`.

- Chercher une seule fois le type de boss taggé le plus proche dans 20 blocs.
- Sans boss, démarrer un K.O. temporaire neuf de 60 secondes.
- Interdire relève alliée et Ancre d’Âme.
- Au troisième coup, passer à `FULLY_DOWNED`; les dégâts suivants sont ignorés.
- Conserver l’échéance initiale de cinq minutes après le passage à terre complet.
- Mort réelle du boss : résurrection sur la position de K.O., y compris après reconnexion.
- Despawn/reset permanent : mort définitive; une simple décharge de chunk n’est pas un wipe.
- Timeout, changement de dimension ou Libérer l’Esprit : mort définitive.
- Les timers continuent joueur hors ligne mais s’arrêtent serveur éteint.

### Jalon 5 — Pendentif et restrictions

Responsable : `gameplay-engineer`.

- Item non empilable avec cooldown stocké sur l’ItemStack, donc indépendant du compte joueur.
- Canalisation stricte de 30 secondes.
- Annulation sans cooldown en cas de mouvement supérieur à 0,1 bloc, dégât, changement de slot/item, mort, dimension ou déconnexion.
- Téléportation seulement depuis la dimension principale configurée.
- Utiliser la résolution vanilla du point de spawn et son repli vers le spawn du monde.
- Appliquer le cooldown uniquement après succès.
- Centraliser les restrictions serveur afin que les contrôles client restent purement ergonomiques.

### Jalon 6 — Interfaces, VFX et assets

Responsables : `stitch-designer`, puis `gameplay-engineer` et `asset-manager`.

- K.O. temporaire : overlay `GuiGraphics` avec timer 60 secondes, jauge trois coups, disponibilité de l’Ancre, progression de relève et vignette de danger.
- K.O. prolongé : écran renderer non pausant avec état du boss, timeout, état complet et bouton Libérer l’Esprit.
- Permettre le chat; rejeter l’inventaire. Autoriser la rotation de caméra par glissement droit sur l’écran prolongé.
- Utiliser `DESIGNE.md` uniquement pour palette, proportions et style; ignorer ses règles gameplay contradictoires.
- Créer deux textures d’item originales, modèles, langues `fr_fr`/`en_us` et tag boss vide extensible par datapack.
- Utiliser les sons et particules vanilla adaptés pour la V1; aucun fichier audio ou shader lourd n’est requis.
- Afficher aux autres joueurs posture, particules et progression sans dépendance GeckoLib/Epic Fight.

### Jalon 7 — Commandes et QA finale

- Ajouter les commandes permission niveau 2 :
  - `/fugurevive set_ko temporary|prolonged <player>`
  - `/fugurevive clear_state <player>`
  - `/fugurevive release_spirit <player>`
  - `/fugurevive give_soul_anchor <player>`
  - `/fugurevive give_return_pendant <player>`
  - `/fugurevive debug_state <player>`
- Passer la version finale à `1.0.1`.
- Vérifier qu’aucun Mixin n’est présent. Si un Mixin devient indispensable, arrêter et demander validation avant ajout.
- Produire `fugu_revive_me-1.0.1.jar` et `fugu_revive_me-1.0.1-sources.jar`.

## 4. Stratégie subagents et Git

Pour chaque jalon :

1. fournir au subagent implémenteur le texte complet du jalon et le contexte nécessaire ;
2. appliquer TDD avant le code métier ;
3. exécuter l’analyse GitNexus `impact` avant modification et `detect_changes` avant commit ;
4. faire une revue de conformité par un subagent neuf ;
5. corriger puis refaire la revue jusqu’à validation ;
6. faire une revue qualité distincte ;
7. lancer `test` et `build`, mettre à jour `.agent/PLAN.md`, commit puis push du jalon validé.

Ne jamais lancer deux subagents d’implémentation en parallèle. Terminer par `renemy` avec verdict GO/NO-GO.

## 5. Tests et critères d’acceptation

### Tests automatisés

- Configuration : listes vides, 1 ID, 108 IDs, plus de 100 IDs, doublons et IDs invalides.
- Machine d’états : toutes les transitions, délais, coups et issues différées.
- Performance : 10 000 échéances futures sans scan global, écriture dirty ni packet sur un tick sans échéance.
- Mort : hors biome, temporaire, prolongée, vide, `/kill`, Mal de résurrection.
- Relève : réussite et chaque cause d’annulation.
- Ancre : hotbar, dégât, consommation atomique.
- Boss : plus proche, absent, mort, reset, chunk unload, joueur hors ligne.
- Pendentif : dimension, mouvement, dégâts, item changé, spawn invalide et cooldown par stack.
- Réseau : packet invalide, distance, état obsolète, doublon et client malveillant.
- Sérialisation SavedData et reprise après redémarrage.

### Vérifications

Depuis `project-gradle/` :

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat build
```

Puis :

- smoke test `runClient` ;
- serveur dédié avec deux clients ;
- validation des overlays à plusieurs GUI scales ;
- test reconnexion, serveur redémarré et boss résolu hors ligne ;
- vérification en présence d’Epic Fight, GeckoLib et du modpack sans intégration directe ;
- validation manuelle par Armand ;
- retrait de tout debug temporaire ;
- `.\gradlew.bat clean build`.

La livraison est acceptée uniquement si aucun item n’est perdu, aucune règle vanilla de drops/XP n’est remplacée, aucun packet n’est envoyé par tick, le client dédié démarre sans classe client chargée côté serveur et tous les scénarios multijoueur passent.
