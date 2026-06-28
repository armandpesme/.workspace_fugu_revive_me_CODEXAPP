package com.fuguteams.fugureviveme.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of boss-to-players links used by the Jalon 4
 * prolonged knockout flow.
 * <p>
 * The registry is intentionally narrow: it only tracks which players are
 * currently in prolonged KO with a given boss UUID, so the
 * {@link ProlongedKoService} can quickly enumerate the affected players
 * when the boss dies or despawns permanently. The state is never
 * persisted: a server restart drops every entry and the durable boss
 * link is recovered from the {@code linkedBossUuid} field of each
 * {@code KoRecord} in the {@code KnockoutSavedData}.
 * <p>
 * The backing map is a {@link ConcurrentHashMap} and the per-boss player
 * sets are backed by a concurrent map as well, so the registry is safe
 * to mutate from any thread even though Forge is normally
 * single-threaded for tick events. This avoids accidental races if a
 * future change introduces asynchronous work (async packet flush, etc.).
 */
public final class BossLinkRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<UUID, Set<UUID>> playersByBoss = new ConcurrentHashMap<>();

    public void link(UUID bossUuid, UUID playerUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Objects.requireNonNull(playerUuid, "playerUuid");
        playersByBoss.computeIfAbsent(bossUuid, ignored -> concurrentSet())
                .add(playerUuid);
        LOGGER.debug("Boss link registered boss={} player={}", bossUuid, playerUuid);
    }

    public boolean unlink(UUID bossUuid, UUID playerUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Objects.requireNonNull(playerUuid, "playerUuid");
        Set<UUID> players = playersByBoss.get(bossUuid);
        if (players == null) {
            return false;
        }
        boolean removed = players.remove(playerUuid);
        if (players.isEmpty()) {
            playersByBoss.remove(bossUuid, players);
        }
        if (removed) {
            LOGGER.debug("Boss link removed boss={} player={}", bossUuid, playerUuid);
        }
        return removed;
    }

    public boolean unlinkBoss(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Set<UUID> removed = playersByBoss.remove(bossUuid);
        if (removed != null && !removed.isEmpty()) {
            LOGGER.debug("Boss link cleared boss={} players={}", bossUuid, removed.size());
            return true;
        }
        return false;
    }

    public boolean unlinkBossByPlayer(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        boolean removed = false;
        for (Set<UUID> players : playersByBoss.values()) {
            if (players.remove(playerUuid)) {
                removed = true;
            }
        }
        playersByBoss.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (removed) {
            LOGGER.debug("Boss link cleared by player player={}", playerUuid);
        }
        return removed;
    }

    public Set<UUID> playersLinkedTo(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Set<UUID> players = playersByBoss.get(bossUuid);
        if (players == null) {
            return Set.of();
        }
        return Set.copyOf(players);
    }

    public boolean isLinkedToAnyPlayer(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Set<UUID> players = playersByBoss.get(bossUuid);
        return players != null && !players.isEmpty();
    }

    public int trackedBosses() {
        return playersByBoss.size();
    }

    private static Set<UUID> concurrentSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
}
