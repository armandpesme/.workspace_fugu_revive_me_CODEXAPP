package com.fuguteams.fugureviveme.server;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
 */
public final class BossLinkRegistry {
    private final Map<UUID, Set<UUID>> playersByBoss = new HashMap<>();

    public void link(UUID bossUuid, UUID playerUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Objects.requireNonNull(playerUuid, "playerUuid");
        playersByBoss.computeIfAbsent(bossUuid, ignored -> new LinkedHashSet<>())
                .add(playerUuid);
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
            playersByBoss.remove(bossUuid);
        }
        return removed;
    }

    public boolean unlinkBoss(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        return playersByBoss.remove(bossUuid) != null;
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
}
