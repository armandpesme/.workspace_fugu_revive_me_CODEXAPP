package com.fuguteams.fugureviveme.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * In-memory tracker of players that took damage since the last call. Each
 * event handler calls {@link #flag(UUID)} when {@code LivingHurtEvent} fires
 * on a player; the revive tick handler calls {@link #consume(UUID)} to
 * decide whether the helper took damage and clear the flag.
 */
public final class KnockoutDamageTracker {
    private final Map<UUID, Long> damageByPlayer = new HashMap<>();

    public void flag(UUID playerUuid, long gameTime) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        damageByPlayer.put(playerUuid, gameTime);
    }

    public boolean consume(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return damageByPlayer.remove(playerUuid) != null;
    }

    public boolean isFlagged(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return damageByPlayer.containsKey(playerUuid);
    }

    public void clear(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        damageByPlayer.remove(playerUuid);
    }

    public void clear() {
        damageByPlayer.clear();
    }
}
