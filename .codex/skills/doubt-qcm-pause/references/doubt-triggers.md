# Catalogue de déclencheurs (Doubt Triggers)

Ce catalogue liste les situations concrètes où l'agent **doit** déclencher
une pause QCM. Il complète la section "Quand déclencher la pause" de
`SKILL.md` avec des exemples spécifiques au workspace Forge 1.20.1.

## Catégorie A — APIs et mappings

| Signal | Exemple concret | Action |
|---|---|---|
| Nom de méthode vanilla non vérifié | `Entity.attackEntityFrom(DamageSource, float)` vs variante MCP | Vérifier mappings, puis questionner si divergence |
| Signature changée entre versions | Méthode `RenderPlayer` entre 1.20.1 et 1.20.4 | Demander version cible |
| Forge vs NeoForge vs Fabric | Code utilisant `DistExecutor` côté serveur | Demander loader |
| Yarn/MCP/Mojang ambigu | Deux mappings possibles pour un même symbole | Présenter les 2 et demander |
| GeckoLib vs Animation vanilla | Animation d'armure via GeckoLib ou vanilla | Demander préférence |

## Catégorie B — Conflit client/serveur

| Signal | Action |
|---|---|
| Classe marquée `@OnlyIn(Dist.CLIENT)` importée dans `common/` | Demander : déplacer en `client/` ou ajouter capability ? |
| `Minecraft.getInstance()` (client) dans code serveur | Demander : refactoriser en packet + capability ? |
| Render/UI en `common/` au lieu de `client/` | Demander : déplacement ? |
| Capability sans packet pour synchronisation | Demander : créer le packet maintenant ou différer ? |

## Catégorie C — Mixins critiques

| Signal | Action |
|---|---|
| Nouveau mixin sur une méthode vanilla sensible | Demander : nom + cible + refmap confirmé |
| Mixin `@Shadow` sur méthode privée | Demander : utiliser AccessTransformer ou réflexion ? |
| Modification du refmap | Demander : régénération complète ou patch ciblé ? |
| Mixin en mode dev (crash) ou release (warn) | Demander : laisser strict (dev) ou passer permissif (pre-release) ? |
| Conflit avec mixin d'un autre mod (Curios, Epic Fight) | Demander : ordre, précédence, fallback ? |

## Catégorie D — Assets et données

| Signal | Action |
|---|---|
| `mod_id` incohérent entre `mods.toml`, `pack.mcmeta`, chemins d'assets | Demander : canonicaliser lequel ? |
| Namespace d'un JSON en `minecraft:` au lieu de `fugu_revive_me:` | Demander : corriger partout ou cas isolé ? |
| Recette JSON sans `result` mais avec plusieurs patterns | Demander : valider la recipe avec le testeur, ou corriger le JSON ? |
| Texture manquante pour un item ajouté | Demander : mock 16x16, asset externe, ou bloquer la release ? |
| Lang key manquante (`item.fugu.helmet`) | Demander : générer depuis un template, ou revue manuelle ? |

## Catégorie E — Périmètre et jalon

| Signal | Action |
|---|---|
| Demande floue "améliore X" | Demander : critère d'acceptation, scope, mesure |
| Feature "ajouter une armure" sans préciser le set | Demander : nome, lore, tier, modèle, recette, attributs |
| Refactor suggéré mais non demandé | Demander : opportuniste (dans le jalon) ou dédié (nouveau jalon) ? |
| Nettoyage de code suggéré par l'agent | Demander : OK pour ce jalon ou reporter ? |
| Bug découvert pendant une autre tâche | Demander : fix immédiat ou ticket séparé ? |

## Catégorie F — Risque humain et design

| Signal | Action |
|---|---|
| Proposition contredit AGENTS.md | Demander :违背 la règle pour ce cas, ou respecter AGENTS.md ? |
| Proposition contredit une décision dans `.agent/PLAN.md` | Demander : mettre à jour le plan, ou rester sur la décision ? |
| Proposition contredit `HANDOFF.md` | Demander : nouveau jalon, ou continuité ? |
| Action irréversible (delete, push -f, reset, clean) | Demander : commit dédié d'abord, archive, ou OK direct ? |
| Modification d'un fichier de design (DESIGNE.md) | Demander : éditer, ou escalader au propriétaire ? |

## Catégorie G — Demande explicite utilisateur

| Phrase type | Action |
|---|---|
| "Pose-moi une question" | Déclencher QCM sur le prochain doute identifié |
| "J'attends ta validation" | Stopper, présenter l'état + question |
| "Demande-moi avant de faire" | Mémoriser pour la prochaine décision |
| "Dis-moi ce que tu hésites" | Lister les doutes non encore résolus en QCM |
| "Check avec moi" | Présenter un QCM sur le point en cours |

## Règle d'or

> En cas de doute, déclencher. Le coût d'une question inutile est très
> inférieur au coût d'une implémentation incorrecte ou d'une rupture de
> design validé.

Si **deux signaux** sont détectés, **un seul QCM** (le plus structurant) ;
les autres sont notés pour le tour suivant.
