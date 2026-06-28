package com.fuguteams.fugureviveme.registry;

import com.fuguteams.fugureviveme.FuguReviveMe;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FuguReviveMe.MOD_ID);

    public static final RegistryObject<MobEffect> RESURRECTION_SICKNESS =
            EFFECTS.register("resurrection_sickness", ResurrectionSicknessEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    private static final class ResurrectionSicknessEffect extends MobEffect {
        private ResurrectionSicknessEffect() {
            super(MobEffectCategory.HARMFUL, 0x73546F);
        }
    }
}
