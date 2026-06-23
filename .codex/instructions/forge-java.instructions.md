---
applyTo: "**/*.java"
description: "Regles Java Forge 1.20.1 pour ce workspace."
---

# Java Forge 1.20.1

- **Java 17** uniquement. Pas de feature Java > 17.
- Pas d'API Fabric / NeoForge / Yarn / registry legacy.
- `DeferredRegister` pour les registries, sauf pattern local existant deja valide.
- Events sur le bon bus : **mod event bus** (`FMLCommonSetupEvent`, `RegisterEvent`, etc.) vs **Forge event bus** (`PlayerEvent`, `LivingEvent`, etc.).
- Code client isole derriere `Dist.CLIENT` ou une classe client dediee referencee via `DistExecutor` / `@OnlyIn`.
- Toute synchro client/serveur passe par packet (`SimpleChannel`) ou capability explicite.
- Verifier imports et package avant de finaliser un patch.
- Ne pas inventer de classe, methode ou event Forge/Minecraft : sourcer si incertain.
