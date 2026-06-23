---
applyTo: "src/main/resources/**,**/data/**,**/assets/**"
description: "Regles ressources Forge 1.20.1 (assets, lang, recipes, tags, loot, datagen)."
---

# Assets & data Forge 1.20.1

- Respecter le **mod id** dans tous les chemins (`assets/<modid>/...`, `data/<modid>/...`).
- Pour un item a registry plat `foo_bar` : modele attendu a `assets/<modid>/models/item/foo_bar.json`. Si le JSON est dans un sous-dossier (`item/subdir/foo_bar.json`), l'ID registry doit etre `subdir/foo_bar`.
- JSON strictement valide (cles entre guillemets, virgules correctes, encodage UTF-8).
- Lang keys coherentes avec les registry names.
- Preferer **datagen** quand le projet l'utilise deja.
- Recipes / loot / tags : verifier conditions, ingredients et tags references.
- `pack.mcmeta` aligne sur le pack format de MC 1.20.1.
