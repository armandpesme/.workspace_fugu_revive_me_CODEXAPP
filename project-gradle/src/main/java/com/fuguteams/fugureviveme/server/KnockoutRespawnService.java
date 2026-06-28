package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.config.ServerConfig;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates the teleport of a freshly respawned player to the mod's
 * generic death spawn point. This service is intentionally thin: it only
 * wraps a few Minecraft APIs and exposes a small, testable decision
 * function for {@link #shouldTransfer(ReviveState)}.
 * <p>
 * The actual teleport goes through {@link ServerPlayer#teleportTo(ServerLevel, double, double, double, float, float)}
 * so the player keeps the vanilla respawn screen and inventory.
 */
public final class KnockoutRespawnService {

    public KnockoutRespawnService() {
    }

    /**
     * Returns true when the player state indicates that the respawn should
     * be redirected to the mod's generic spawn point.
     */
    public boolean shouldTransfer(ReviveState previousState) {
        if (previousState == null) {
            return false;
        }
        return previousState == ReviveState.PENDING_DEATH
                || previousState == ReviveState.PENDING_REVIVE
                || previousState == ReviveState.DEAD_PENDING_TRANSFER;
    }

    public Optional<UUID> transferToGenericSpawn(ServerPlayer player, MinecraftServer server) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(server, "server");
        ResourceLocation dimensionId = ResourceLocation.tryParse(ServerConfig.DEATH_RESPAWN_DIMENSION_ID.get());
        if (dimensionId == null) {
            return Optional.empty();
        }
        ServerLevel target = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimensionId));
        if (target == null) {
            return Optional.empty();
        }
        double x = ServerConfig.DEATH_RESPAWN_X.get();
        double y = ServerConfig.DEATH_RESPAWN_Y.get();
        double z = ServerConfig.DEATH_RESPAWN_Z.get();
        float yaw = (float) ServerConfig.DEATH_RESPAWN_YAW.get().doubleValue();
        float pitch = ServerConfig.DEATH_RESPAWN_PITCH.get().floatValue();
        player.teleportTo(target, x, y, z, yaw, pitch);
        return Optional.of(player.getUUID());
    }
}
