# Protocole d'escalade

Quand l'utilisateur ne peut pas répondre (délégation, conflit, fatigue) ou
que la situation dépasse le QCM simple.

## E1 — L'utilisateur dit "je ne sais pas"

### Réponse attendue de l'agent

1. Proposer **un défaut sûr** : option la moins irréversible, conforme à
   AGENTS.md.
2. Lister **2-3 raisons** qui justifient ce défaut.
3. Demander confirmation :

```
question: "Tu délègues le choix ?

Je propose l'option A par défaut : <défaut>.
Justification : <raisons 1-2 phrases>.

Tu confirmes ce défaut, ou tu préfères reporter la décision ?"

header: "Délégation choix"

options:
  - label: "Confirmer défaut A" (Recommandé)
    description: "Appliquer l'option A, marquer 'à confirmer plus tard' dans .agent/PLAN.md."
  - label: "Reporter la décision"
    description: "Créer un point 'Décision à trancher' dans .agent/PLAN.md, sortir du jalon courant."
  - label: "Tu vas y réfléchir"
    description: "Stopper, attendre une réponse ultérieure."
```

## E2 — L'utilisateur est en colère ou contradictoire

### Réponse attendue de l'agent

1. **Stopper immédiatement** toute action.
2. **Une seule question de cadrage** :

```
question: "Je sens qu'on a un désalignement. Avant de continuer, j'ai
besoin d'un cadrage court.

Tu veux que je <option A> ou <option B> ?"

header: "Cadrage"

options:
  - label: "<Option A>" (Recommandé)
    description: "Reprendre le travail sur la base de l'option A."
  - label: "<Option B>"
    description: "Reprendre le travail sur la base de l'option B."
  - label: "Stopper la session"
    description: "Sauvegarder l'état dans HANDOFF.md, reprendre plus tard."
```

3. Ne pas défendre, ne pas justifier, ne pas proposer 3 alternatives.
4. Attendre la réponse.

## E3 — La décision invalide le plan

### Réponse attendue de l'agent

1. **Stopper l'implémentation**.
2. Présenter un QCM "Plan à corriger" :

```
question: "La décision D que tu viens de prendre invalide la section X
de .agent/PLAN.md. Comment veux-tu procéder ?"

header: "Plan à corriger"

options:
  - label: "Mettre à jour PLAN.md maintenant" (Recommandé)
    description: "Je patche .agent/PLAN.md avec la nouvelle direction, puis on reprend."
  - label: "Créer un nouveau PLAN.md dédié"
    description: "Garder le plan courant pour jalon N, créer plan_jalon_N+1.md."
  - label: "Continuer sans mettre à jour"
    description: "Risque de dérive documentée, je note 'PLAN.md désynchronisé' dans HANDOFF.md."
```

## E4 — Conflit avec AGENTS.md, README ou DESIGNE.md

### Réponse attendue de l'agent

1. **Ne pas modifier** AGENTS.md, README.md, DESIGNE.md.
2. Présenter le conflit explicitement :

```
question: "Ta demande contredit AGENTS.md (section X : '<citation
exacte>'). Comment procède-t-on ?"

header: "Conflit AGENTS.md"

options:
  - label: "Respecter AGENTS.md" (Recommandé)
    description: "Suivre la règle existante, ne pas dévier, ajouter une note dans HANDOFF.md."
  - label: "Suggérer un amendement à AGENTS.md"
    description: "Bloquer la tâche, créer un draft d'amendement, demander validation explicite."
  - label: "Dévier exceptionnellement"
    description: "Appliquer ta demande, documenter la déviation dans .agent/PLAN.md, demander validation explicite."
```

## E5 — Question touchant un document protégé

Liste des documents protégés (cf. AGENTS.md) :

- `DESIGNE.md`
- `AGENTS.md`
- `README.md`
- `HANDOFF.md` (sauf demande explicite)

### Réponse attendue de l'agent

1. **Ne jamais modifier** sans validation explicite.
2. Présenter la proposition d'amendement, pas la modification.
3. Demander validation :

```
question: "Tu m'as demandé de modifier <DOCUMENT>. Je ne peux le faire
qu'avec validation explicite car c'est un document protégé.

Tu confirmes la modification : <résumé du changement> ?"

header: "Doc protégé"

options:
  - label: "Confirmer la modification" 
    description: "Appliquer le changement exactement comme proposé."
  - label: "Reporter"
    description: "Créer un point 'Amendement <DOCUMENT>' dans .agent/PLAN.md."
  - label: "Annuler"
    description: "Ne pas modifier, traiter la demande autrement."
```

## E6 — Délégation silencieuse

Si l'utilisateur ne répond pas (timeout, "ok vas-y", "fais au mieux") :

1. **Choisir le défaut le plus sûr** :
   - conforme à AGENTS.md ;
   - réversible ;
   - tracé.
2. **L'annoncer explicitement** :

> "Tu n'as pas répondu, j'applique le défaut A : <description>. Si ce
> n'est pas ce que tu voulais, dis-le moi et je corrige."

3. **Tracer** dans `.agent/PLAN.md` (section "Hypothèses et points à
   vérifier") avec la mention "Décision par délégation — à confirmer".

## Règle d'or de l'escalade

> Toujours offrir une porte de sortie ("Reporter", "Stopper", "Annuler").
> Toujours proposer un défaut sûr. Toujours tracer la décision, même
> prise par délégation.
