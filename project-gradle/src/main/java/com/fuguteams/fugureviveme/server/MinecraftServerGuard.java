package com.fuguteams.fugureviveme.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Tiny utility for retrieving the live {@link MinecraftServer} in contexts
 * where the caller does not already have a reference. It exists to keep
 * the {@code KoEventHandlers} free of {@code ServerLifecycleHooks}
 * imports sprinkled across the file.
 */
public final class MinecraftServerGuard {
    private MinecraftServerGuard() {
        throw new AssertionError("MinecraftServerGuard is a static utility and must not be instantiated");
    }

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
