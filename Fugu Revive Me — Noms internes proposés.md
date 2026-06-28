# Fugu Revive Me — Noms internes proposés
A ajuster selon les besoins du mod, mais ces noms internes sont proposés pour la cohérence et la clarté.

## Mod ID

```txt
fugu_revive_me
```

Nom affiché :

```txt
Fugu Revive Me
```

---

## Items

| Élément gameplay           | Nom affiché joueur  | ID interne recommandé |
| -------------------------- | ------------------- | --------------------- |
| Pendentif de téléportation | Pendentif de Retour | `return_pendant`      |
| Item d’auto-résurrection   | Ancre d’Âme         | `soul_anchor`         |

IDs complets :

```txt
fugu_revive_me:return_pendant
fugu_revive_me:soul_anchor
```

Classes Java probables :

```java
ReturnPendantItem
SoulAnchorItem
```

---

## Effets / debuffs

| Effet gameplay                                   | Nom affiché joueur  | ID interne recommandé   |
| ------------------------------------------------ | ------------------- | ----------------------- |
| Mal de résurrection                              | Mal de résurrection | `resurrection_sickness` |
| État K.O. si géré comme effet visible            | K.O.                | `knocked_out`           |
| État à terre complet si géré comme effet visible | À terre complet     | `downed_state`          |

IDs complets :

```txt
fugu_revive_me:resurrection_sickness
fugu_revive_me:knocked_out
fugu_revive_me:downed_state
```

Classes Java probables :

```java
ResurrectionSicknessEffect
KnockedOutEffect
DownedStateEffect
```

Recommandation :
`resurrection_sickness` doit être un vrai effet/debuff visible.
`knocked_out` et `downed_state` peuvent être des états internes synchronisés plutôt que de vrais effets potion, selon ce qui est le plus propre techniquement.

---

## États internes du joueur

Enum recommandé :

```java
ReviveState
```

Valeurs recommandées :

```java
ALIVE
TEMPORARY_KO
PROLONGED_KO
FULLY_DOWNED
DEAD_PENDING_TRANSFER
REVIVING
SELF_REVIVING
SPIRIT_RELEASED
```

Signification :

| État                    | Rôle                                                                     |
| ----------------------- | ------------------------------------------------------------------------ |
| `ALIVE`                 | Joueur vivant, état normal.                                              |
| `TEMPORARY_KO`          | K.O. temporaire de 60 secondes.                                          |
| `PROLONGED_KO`          | K.O. prolongé lié à un boss.                                             |
| `FULLY_DOWNED`          | État à terre complet après 3 coups en K.O. prolongé.                     |
| `DEAD_PENDING_TRANSFER` | Mort définitive en attente de transfert vers le spawn de mort générique. |
| `REVIVING`              | Un autre joueur est en train de relever ce joueur.                       |
| `SELF_REVIVING`         | Le joueur utilise une Ancre d’Âme.                                       |
| `SPIRIT_RELEASED`       | Le joueur a libéré son esprit ou a dépassé le timeout.                   |

---

## Données persistantes / capability

Nom recommandé :

```java
RevivePlayerData
```

Champs probables :

```java
ReviveState state
int koTicksRemaining
int prolongedKoTicksRemaining
int hitsTakenWhileKo
UUID linkedBossUuid
BlockPos koPosition
ResourceKey<Level> koDimension
UUID reviverUuid
int reviveProgressTicks
boolean spiritReleased
```

---

## Tags

Tag boss validé :

```txt
#fugu_revive_me:fugu_boss
```

Fichier probable :

```txt
data/fugu_revive_me/tags/entity_types/fugu_boss.json
```

---

## Config serveur

Nom de fichier recommandé :

```txt
fugu_revive_me-server.toml
```

Clés recommandées :

```toml
[death_spawn]
dimension = "minecraft:overworld"
x = 0.5
y = 80.0
z = 0.5
yaw = 0.0
pitch = 0.0

[temporary_ko]
enabled = true
biomes = []
duration_seconds = 60
max_hits = 3
revive_duration_seconds = 5
revived_health_percent = 25.0

[prolonged_ko]
enabled = true
biomes = []
max_duration_seconds = 300
max_hits_before_fully_downed = 3
boss_search_radius = 96.0
boss_tag = "fugu_revive_me:fugu_boss"

[resurrection_sickness]
duration_seconds = 300

[return_pendant]
cast_duration_seconds = 30
cooldown_seconds = 300
main_dimension = "minecraft:overworld"
```

---

## Packets réseau

Noms recommandés :

```java
ClientboundReviveStatePacket
ClientboundKoTimerPacket
ClientboundReviveProgressPacket
ServerboundReleaseSpiritPacket
ServerboundSelfRevivePacket
ServerboundStartRevivePacket
ServerboundCancelRevivePacket
```

---

## Interfaces Java Renderer

Classes probables :

```java
TemporaryKoOverlay
ProlongedKoOverlay
ReviveProgressOverlay
ReturnPendantCastOverlay
```

IDs internes d’overlay :

```txt
temporary_ko_overlay
prolonged_ko_overlay
revive_progress_overlay
return_pendant_cast_overlay
```

---

## Sons

IDs recommandés :

```txt
fugu_revive_me:ko_enter
fugu_revive_me:ko_hit
fugu_revive_me:revive_start
fugu_revive_me:revive_complete
fugu_revive_me:spirit_release
fugu_revive_me:return_pendant_cast
fugu_revive_me:return_pendant_departure
fugu_revive_me:return_pendant_arrival
```

---

## Particules / VFX simples

IDs recommandés :

```txt
fugu_revive_me:ko_marker
fugu_revive_me:revive_spark
fugu_revive_me:spirit_release
fugu_revive_me:return_pendant_charge
fugu_revive_me:return_pendant_arrival
```

---

## Commandes debug utiles

Commandes internes possibles pour test serveur :

```txt
/fugurevive set_ko temporary <player>
/fugurevive set_ko prolonged <player>
/fugurevive clear_state <player>
/fugurevive release_spirit <player>
/fugurevive give_soul_anchor <player>
/fugurevive give_return_pendant <player>
/fugurevive debug_state <player>
```

Ces commandes doivent être réservées aux opérateurs ou au debug.
