# QCM Template - Modèle de question à choix multiples

## Modèle canonique (à utiliser tel quel)

```
question: "<Contexte en 1 phrase, factuel, sans jugement.>

<Question claire, fermée, binaire ou à 2-4 options ?>"

header: "<Catégorie courte, max 30 chars>"

options:
  - label: "<Action recommandée>" (Recommandé)
    description: "<Implication concrète : fichiers, risque, réversibilité, validation>"
  - label: "<Alternative A>"
    description: "<Implication concrète différente>"
  - label: "<Alternative B>"
    description: "<Implication concrète différente>"
  - label: "<Reporter / autre jalon>"
    description: "Reporter la décision, créer un point dans .agent/PLAN.md à trancher plus tard."
```

L'option "Autre / réponse personnalisée" est **ajoutée automatiquement** par
l'outil `question` ; ne pas l'inclure manuellement.

## Exemples concrets

### Exemple 1 : Conflit de version de dépendance

```
question: "Le projet liste Forge 47.2.0 dans mods.toml mais le build résout
Effectively 47.3.0 en raison du classpath Gradle. Quelle version dois-je
cibler pour l'API EntityJoinWorldEvent ?

L'incohérence peut casser le mixin d'enregistrement d'entités."

header: "Version Forge"

options:
  - label: "Forcer 47.2.0 dans mods.toml" (Recommandé)
    description: "Aligne mods.toml avec le classpath effectif, supprime l'avertissement au chargement, build reproductible."
  - label: "Migrer vers 47.3.0"
    description: "Mise à jour de l'API, vérifier tous les mixins, refaire un build complet, risque de régression sur 1-2 jours."
  - label: "Garder les deux"
    description: "Impossible : Forge charge un seul mods.toml ; l'avertissement persistera."
```

### Exemple 2 : Périmètre flou

```
question: "Le jalon 4 demande 'équilibrer les dégâts de l'armure Fugu'.
J'ai trois axes possibles d'équilibrage.

Quel scope dois-je traiter dans ce jalon ?"

header: "Périmètre jalon 4"

options:
  - label: "Dégâts PvE uniquement" (Recommandé)
    description: "Modifier ArmorMaterial.toughness et damageProtection ; tester contre zombies et creepers. Hors scope PvP."
  - label: "Dégâts PvE + PvP"
    description: "Inclut le calcul de réduction en duel joueur, nécessite attributs vanilla + tests en arène."
  - label: "Équilibrage visuel uniquement"
    description: "Ajuster la teinte des particules de coup, pas de modification des dégâts réels."
  - label: "Reporter à un jalon dédié"
    description: "Créer une sous-tâche 'équilibrage Fugu v2' dans .agent/PLAN.md, sortir du jalon courant."
```

### Exemple 3 : Action irréversible

```
question: "Tu m'as demandé de nettoyer les assets obsolètes. J'ai détecté
3 textures non référencées et un modèle .json orphelin. Avant suppression
définitive, je veux confirmer le scope."

header: "Suppression assets"

options:
  - label: "Supprimer les 4 fichiers listés" (Recommandé)
    description: "Action irréversible : les fichiers partent dans la corbeille Gradle. Aucun lien dans le code."
  - label: "Déplacer dans archive/old/"
    description: "Préserve l'historique physique, occupation disque conservée, réversibilité totale."
  - label: "Demander un commit dédié avant"
    description: "Commit isolé 'chore: drop orphan assets', revue séparée, push bloqué tant que non validé."
  - label: "Tout garder"
    description: "Aucune suppression ; l'avertissement de build 'asset inutilisé' persistera."
```

### Exemple 4 : Choix d'ID de registre

```
question: "L'armure Fugu est en cours d'ajout. J'ai préparé deux options
d'ID qui respectent la convention snake_case du projet.

Quel ID utiliser pour CreativeTab, recipes, lang et modèles ?"

header: "ID armure Fugu"

options:
  - label: "fugu_armor" (Recommandé)
    description: "Cohérent avec fugu_helmet, fugu_chestplate déjà dans le code, alignement avec la convention des autres armures du mod."
  - label: "fugusuit"
    description: "Nom court, distinctif, mais incohérent avec le reste des items en fugu_*."
  - label: "fugu_set"
    description: "Générique, suit le pattern 'set' de certains mods, mais entre en conflit avec un tag forge existant."
```

## Longueurs et contraintes

| Champ | Contrainte | Source |
|---|---|---|
| `header` | max 30 caractères | contrainte outil `question` |
| `label` (option) | 1-5 mots | contrainte outil `question` |
| `description` (option) | 1 phrase, 1 ligne idéalement | bonne pratique |
| Nombre d'options | 2 à 4 (+ "Autre" auto) | contrainte outil `question` |
| Question | 1 phrase interrogative | bonne pratique |
| Contexte | 1 phrase factuelle avant la question | bonne pratique |

## Quand multi-questions

Si **plusieurs doutes indépendants** existent dans la même situation :

- ❌ Mauvais : un seul `question` avec 5 sous-questions.
- ✅ Bon : 2 ou 3 appels `question` **séparés**, avec un entête "Question
  1/X", "Question 2/X".

L'utilisateur peut ainsi répondre à son rythme et ne pas perdre le fil.

## Suffixe `(Recommandé)`

- Toujours suffixer `(Recommandé)` sur la première option.
- L'outil `question` peut ajouter automatiquement ce suffixe si
  `recommended: true` est présent sur l'option ; sinon, l'ajouter
  textuellement au label.
- Une seule option porte le suffixe `(Recommandé)`.
