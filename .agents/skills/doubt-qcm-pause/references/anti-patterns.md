# Anti-patterns

Liste des erreurs fréquentes qui font perdre l'utilité de ce skill.

## A1 — Empiler plusieurs décisions dans un seul QCM

❌ Mauvais :

```
question: "J'ai 5 doutes sur cette PR. Pour chacun, choisis une option :
1) Version Forge : A/B/C ?
2) Périmètre armure : D/E ?
3) Texture : F/G ?
..."
```

✅ Bon : 3 appels `question` séparés, en série.

**Pourquoi** : l'utilisateur décroche au-delà de 4 options, et les
réponses sont moins réfléchies.

## A2 — Poser une question triviale

❌ Mauvais : demander "Quelle convention de nommage : camelCase ou
snake_case ?" alors qu'AGENTS.md impose snake_case.

✅ Bon : appliquer la règle, le dire explicitement.

**Pourquoi** : si la réponse est dans AGENTS.md / README / .agent/PLAN.md,
re-questionner = perte de temps + frustration + perte de confiance.

## A3 — Deviner l'option recommandée sans attendre

❌ Mauvais : l'utilisateur tarde à répondre, l'agent "prend l'option A par
défaut" et continue.

✅ Bon : attendre, ou si l'utilisateur dit "vas-y, fais au mieux", poser
un QCM "Tu confirmes l'option A ?".

**Pourquoi** : l'auto-substitution tue le skill. Si l'utilisateur a
délégué, le faire explicitement et tracer.

## A4 — Question sans contexte

❌ Mauvais :

```
question: "Quelle option ?"
options: [A, B, C, D]
```

✅ Bon :

```
question: "J'ai détecté que le mixin X touche la même méthode qu'un autre
mod. Quel choix pour éviter le conflit ?

Quelle option préfères-tu ?"
```

**Pourquoi** : sans contexte, l'utilisateur ne peut pas répondre
intelligemment, et l'agent doit quand même expliquer → 2x le travail.

## A5 — Question non documentée après réponse

❌ Mauvais : "OK pour option B, je continue." → la décision est perdue
pour le prochain agent.

✅ Bon : "Décision retenue : option B (mixin en mode strict dev). Tracé
dans .agent/PLAN.md §Journal des décisions." → reprise facilitée.

**Pourquoi** : `.agent/PLAN.md` est lu par les agents sans état. Une
décision non écrite = décision perdue.

## A6 — Suffixe `(Recommandé)` absent

❌ Mauvais : 4 options sans signal de recommandation.

✅ Bon : la première porte `(Recommandé)` + courte justification dans la
description.

**Pourquoi** : l'utilisateur veut souvent valider la recommandation. Le
suffixe fait gagner du temps.

## A7 — Trop d'options (>4)

❌ Mauvais : 6 options, l'utilisateur hésite 10 minutes.

✅ Bon : 2-4 options, le reste en "Autre / réponse personnalisée".

**Pourquoi** : au-delà de 4, la charge cognitive explose.

## A8 — Question binaire présentée comme QCM

❌ Mauvais :

```
question: "OK pour continuer ?"
options: [Oui, Non]
```

✅ Bon : utiliser une question fermée `Y/N` (texte simple), pas un QCM.

**Pourquoi** : le QCM est réservé aux décisions à choix multiples
significatives. Pour binaire, le texte est plus rapide.

## A9 — Question sur un point déjà décidé dans la session

❌ Mauvais : l'utilisateur a dit il y a 3 tours "toujours utiliser
FoxyMD pour les armures" → l'agent re-questionne.

✅ Bon : appliquer la décision, citer la source.

**Pourquoi** : l'utilisateur se sent ignoré, perd patience.

## A10 — QCM pendant une opération risquée en cours

❌ Mauvais : l'agent est au milieu d'un mixin appliqué, il envoie un QCM
qui suspend le build en cours.

✅ Bon : finaliser l'opération en cours (commit atomique), puis QCM pour
la suite.

**Pourquoi** : suspendre une écriture partielle = état corrompu.

## A11 — Pas de fallback "Reporter"

❌ Mauvais : forcer l'utilisateur à trancher maintenant, même si la
décision peut attendre.

✅ Bon : proposer "Reporter à un jalon dédié" en option, qui crée un
point dans `.agent/PLAN.md`.

**Pourquoi** : certaines décisions n'ont pas besoin d'être prises dans le
jalon courant. Offrir la sortie Reporter = moins de pression = meilleure
décision.

## A12 — Confondre question de cadrage et QCM de décision

❌ Mauvais : "Peux-tu me décrire le mod en 3 phrases ?" → ce n'est pas
un QCM.

✅ Bon : ce type de question = texte libre, pas `question`.

**Pourquoi** : `question` est pour les choix multiples. Pour le texte
libre, utiliser une réponse texte simple.

## Règle d'or des anti-patterns

> Une seule décision par QCM, contexte clair, recommandation visible,
> fallback Reporter, tracé obligatoire.
