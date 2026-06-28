package com.fuguteams.fugureviveme;

import com.fuguteams.fugureviveme.config.ServerConfig;
import com.fuguteams.fugureviveme.network.FuguNetwork;
import com.fuguteams.fugureviveme.registry.ModEffects;
import com.fuguteams.fugureviveme.registry.ModItems;
import com.fuguteams.fugureviveme.server.FuguKnockoutRuntime;
import com.fuguteams.fugureviveme.server.KoEventHandlers;
import com.fuguteams.fugureviveme.server.ServerConfigRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FuguReviveMe.MOD_ID)
public final class FuguReviveMe {
    public static final String MOD_ID = "fugu_revive_me";

    public FuguReviveMe(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ServerConfigRuntime.register(modEventBus);
        FuguNetwork.register();
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(FuguReviveMe.class);
        KoEventHandlers.register(forgeBus);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        FuguKnockoutRuntime.init(server);
    }
}
