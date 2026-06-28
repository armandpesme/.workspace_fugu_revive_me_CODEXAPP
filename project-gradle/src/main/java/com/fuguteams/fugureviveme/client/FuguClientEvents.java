package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

public final class FuguClientEvents {
    private FuguClientEvents() {
        throw new AssertionError("FuguClientEvents is a static utility and must not be instantiated");
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FuguClientEvents::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(FuguClientEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(TrackedKoEffects::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, TrackedKoEffects::onRenderPlayerPre);
        MinecraftForge.EVENT_BUS.addListener(TrackedKoEffects::onRenderPlayerPost);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("temporary_ko", TemporaryKoOverlay::render);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ReviveState state = ClientReviveStateStore.get().snapshot().state();
        boolean needsScreen = state == ReviveState.PROLONGED_KO || state == ReviveState.FULLY_DOWNED;
        if (minecraft.screen instanceof ChatScreen) {
            return;
        }
        if (needsScreen && !(minecraft.screen instanceof ProlongedKoScreen)) {
            minecraft.setScreen(new ProlongedKoScreen());
        } else if (!needsScreen && minecraft.screen instanceof ProlongedKoScreen) {
            minecraft.setScreen(null);
        }
    }
}
