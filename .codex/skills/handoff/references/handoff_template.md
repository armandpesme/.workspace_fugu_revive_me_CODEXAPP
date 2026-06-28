# Handoff template (80 lines max)

```markdown
# Handoff - <jalon> - <YYYY-MM-DD>

## Objectif
- <une phrase factuelle>

## Fichiers modifies
- <+|~|-> <chemin relatif>  # <note courte si utile>
- - aucun

## Tests lances
- `<commande>` -> OK|FAIL|SKIP  # <note>
- - aucun

## Non fait
- <point du plan non traite>
- - aucun

## Risques restants
- <bug, dette, regression, dependance externe>
- - aucun

## Prompt du prochain jalon
<3 a 8 lignes actionnables, factuelles, sans question ouverte.>

## Decisions a valider
- <choix pris en supposition qui demande validation explicite>
- - aucune
```

Regles:

- Remplacer `<...>` par le contenu reel ou supprimer la ligne.
- Une seule ligne par item de liste.
- Garder l'ordre des sections.
- Aucune section narrative ou explicative dans le fichier final.
- `wc -l HANDOFF.md` doit retourner `<= 80`.
