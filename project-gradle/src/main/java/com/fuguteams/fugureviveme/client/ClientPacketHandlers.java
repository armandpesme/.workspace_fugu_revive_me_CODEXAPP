package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
        throw new AssertionError("ClientPacketHandlers is a static utility and must not be instantiated");
    }

    public static void handleSnapshot(ClientboundReviveSnapshot packet) {
        ClientReviveStateStore.get().accept(packet, clientGameTime());
    }

    public static void handleTrackedVisual(ClientboundTrackedKoVisual packet) {
        ClientReviveStateStore.get().accept(packet);
    }

    private static long clientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
}
