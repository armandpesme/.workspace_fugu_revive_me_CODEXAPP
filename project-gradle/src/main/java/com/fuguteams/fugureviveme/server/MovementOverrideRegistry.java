package com.fuguteams.fugureviveme.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * In-memory tracker of the player's original movement speed before the
 * mod overrides it during a temporary KO. The values are never persisted
 * to disk: a server restart drops the tracker and players will respawn at
 * their vanilla default speed.
 */
public final class MovementOverrideRegistry {
    private static final double VANILLA_DEFAULT = 0.1;

    private final Map<UUID, Double> storedSpeeds = new HashMap<>();

    public double vanillaDefault() {
        return VANILLA_DEFAULT;
    }

    public void remember(UUID playerUuid, double currentBaseValue) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        storedSpeeds.putIfAbsent(playerUuid, currentBaseValue);
    }

    /**
     * @return true if a stored speed was present and got removed; the
     *         caller is expected to restore the attribute to
     *         {@link #vanillaDefault()}.
     */
    public boolean forget(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return storedSpeeds.remove(playerUuid) != null;
    }

    public boolean isRemembered(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return storedSpeeds.containsKey(playerUuid);
    }
}
