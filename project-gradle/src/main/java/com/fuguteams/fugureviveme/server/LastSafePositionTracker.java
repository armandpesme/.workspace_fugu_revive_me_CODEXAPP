package com.fuguteams.fugureviveme.server;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * In-memory tracker of the last safe position recorded per player.
 * <p>
 * The store is purely server-side, sampled every 10 server ticks and never
 * persisted to disk. Entries are kept in a plain {@link HashMap} keyed by
 * {@link UUID}; lifecycle is managed by the server only.
 */
public final class LastSafePositionTracker {
    public static final int SAMPLE_INTERVAL_TICKS = 10;

    private final Map<UUID, BlockPos> lastSafePositions = new HashMap<>();

    public LastSafePositionTracker() {
    }

    /**
     * Returns the last recorded safe position for {@code playerUuid}, or
     * {@link Optional#empty()} when no entry is known.
     */
    public Optional<BlockPos> get(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return Optional.ofNullable(lastSafePositions.get(playerUuid));
    }

    /**
     * Records {@code position} as the new last safe spot for {@code playerUuid}.
     * The provided position is stored as an immutable copy to keep callers
     * from mutating tracked state by reference.
     */
    public void record(UUID playerUuid, BlockPos position) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(position, "position");
        lastSafePositions.put(playerUuid, position.immutable());
    }

    public boolean sample(UUID playerUuid, long gameTime, BlockPos position, boolean safePosition) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(position, "position");
        if (!safePosition || gameTime < 0 || gameTime % SAMPLE_INTERVAL_TICKS != 0) {
            return false;
        }
        record(playerUuid, position);
        return true;
    }

    public BlockPos resolveKoPosition(UUID playerUuid, Supplier<BlockPos> vanillaSpawnFallback) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(vanillaSpawnFallback, "vanillaSpawnFallback");
        return get(playerUuid).orElseGet(() -> vanillaSpawnFallback.get().immutable());
    }

    /**
     * Drops the tracked entry for {@code playerUuid}, if any. A no-op when
     * the player was never recorded.
     */
    public void forget(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        lastSafePositions.remove(playerUuid);
    }
}
