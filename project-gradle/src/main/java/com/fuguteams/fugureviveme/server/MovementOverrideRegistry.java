package com.fuguteams.fugureviveme.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * In-memory tracker of the player's original movement speed before the
 * mod overrides it during a temporary KO. The values are never persisted
 * to disk: a server restart drops the tracker and players will respawn at
 * their vanilla default speed.
 */
public final class MovementOverrideRegistry {
    private final Map<UUID, Double> storedSpeeds = new HashMap<>();

    public void remember(UUID playerUuid, double currentBaseValue) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        storedSpeeds.putIfAbsent(playerUuid, currentBaseValue);
    }

    /**
     * Removes the stored speed for {@code playerUuid} and returns it.
     * <p>
     * Returns {@link OptionalDouble#empty()} when no entry was stored, in
     * which case the caller should not modify the player's movement
     * attribute (the registry was empty, so the attribute was not
     * overridden).
     *
     * @return the previously stored base movement speed, if any.
     */
    public OptionalDouble forget(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Double value = storedSpeeds.remove(playerUuid);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public boolean isRemembered(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return storedSpeeds.containsKey(playerUuid);
    }
}
