package com.fuguteams.fugureviveme;

import com.fuguteams.fugureviveme.config.ServerConfig;
import com.fuguteams.fugureviveme.registry.ModEffects;
import com.fuguteams.fugureviveme.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FuguReviveMe.MOD_ID)
public final class FuguReviveMe {
    public static final String MOD_ID = "fugu_revive_me";

    @SuppressWarnings("removal")
    public FuguReviveMe() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
