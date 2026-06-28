package com.fuguteams.fugureviveme.server;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossLinkRegistryTest {

    @Test
    void linkAddsPlayerToBossSet() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        registry.link(boss, player);

        assertEquals(Set.of(player), registry.playersLinkedTo(boss));
        assertTrue(registry.isLinkedToAnyPlayer(boss));
        assertEquals(1, registry.trackedBosses());
    }

    @Test
    void linkIsIdempotentForSamePlayer() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        registry.link(boss, player);
        registry.link(boss, player);

        assertEquals(Set.of(player), registry.playersLinkedTo(boss));
    }

    @Test
    void linkMultiplePlayersToSameBoss() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        registry.link(boss, first);
        registry.link(boss, second);

        assertEquals(Set.of(first, second), registry.playersLinkedTo(boss));
    }

    @Test
    void linkSamePlayerToTwoBossesKeepsBothLinks() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID bossA = UUID.randomUUID();
        UUID bossB = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        registry.link(bossA, player);
        registry.link(bossB, player);

        assertEquals(Set.of(player), registry.playersLinkedTo(bossA));
        assertEquals(Set.of(player), registry.playersLinkedTo(bossB));
        assertEquals(2, registry.trackedBosses());
    }

    @Test
    void unlinkRemovesSinglePlayerAndDropsBossWhenEmpty() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        registry.link(boss, first);
        registry.link(boss, second);

        assertTrue(registry.unlink(boss, first));
        assertEquals(Set.of(second), registry.playersLinkedTo(boss));
        assertTrue(registry.isLinkedToAnyPlayer(boss));

        assertTrue(registry.unlink(boss, second));
        assertEquals(Set.of(), registry.playersLinkedTo(boss));
        assertFalse(registry.isLinkedToAnyPlayer(boss));
        assertEquals(0, registry.trackedBosses());
    }

    @Test
    void unlinkMissingEntryReturnsFalse() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        assertFalse(registry.unlink(boss, player));
    }

    @Test
    void unlinkBossDropsAllPlayers() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        registry.link(boss, first);
        registry.link(boss, second);

        assertTrue(registry.unlinkBoss(boss));
        assertFalse(registry.isLinkedToAnyPlayer(boss));
        assertEquals(0, registry.trackedBosses());
    }

    @Test
    void unlinkBossOnMissingEntryReturnsFalse() {
        BossLinkRegistry registry = new BossLinkRegistry();
        assertFalse(registry.unlinkBoss(UUID.randomUUID()));
    }

    @Test
    void unlinkBossByPlayerRemovesFromAllBosses() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID bossA = UUID.randomUUID();
        UUID bossB = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        registry.link(bossA, player);
        registry.link(bossA, other);
        registry.link(bossB, player);

        boolean removed = registry.unlinkBossByPlayer(player);

        assertTrue(removed);
        assertEquals(Set.of(other), registry.playersLinkedTo(bossA));
        assertEquals(Set.of(), registry.playersLinkedTo(bossB));
    }

    @Test
    void unlinkBossByPlayerReturnsFalseWhenNotLinked() {
        BossLinkRegistry registry = new BossLinkRegistry();
        assertFalse(registry.unlinkBossByPlayer(UUID.randomUUID()));
    }

    @Test
    void unlinkBossByPlayerDropsEmptyBosses() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        registry.link(boss, player);

        registry.unlinkBossByPlayer(player);

        assertFalse(registry.isLinkedToAnyPlayer(boss));
        assertEquals(0, registry.trackedBosses());
    }

    @Test
    void unlinkBossByPlayerRejectsNullArgument() {
        BossLinkRegistry registry = new BossLinkRegistry();
        assertThrows(NullPointerException.class, () -> registry.unlinkBossByPlayer(null));
    }

    @Test
    void playersLinkedToReturnsEmptySetForUnknownBoss() {
        BossLinkRegistry registry = new BossLinkRegistry();
        Set<UUID> players = registry.playersLinkedTo(UUID.randomUUID());
        assertNotNull(players);
        assertTrue(players.isEmpty());
        assertFalse(registry.isLinkedToAnyPlayer(UUID.randomUUID()));
    }

    @Test
    void playersLinkedToReturnsImmutableSnapshot() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        registry.link(boss, first);
        Set<UUID> snapshot = registry.playersLinkedTo(boss);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(UUID.randomUUID()));
    }

    @Test
    void linkRejectsNullArguments() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> registry.link(null, player));
        assertThrows(NullPointerException.class, () -> registry.link(boss, null));
    }

    @Test
    void unlinkRejectsNullArguments() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> registry.unlink(null, player));
        assertThrows(NullPointerException.class, () -> registry.unlink(boss, null));
    }

    @Test
    void unlinkBossRejectsNullArgument() {
        BossLinkRegistry registry = new BossLinkRegistry();
        assertThrows(NullPointerException.class, () -> registry.unlinkBoss(null));
    }

    @Test
    void isLinkedToAnyPlayerRejectsNullArgument() {
        BossLinkRegistry registry = new BossLinkRegistry();
        assertThrows(NullPointerException.class, () -> registry.isLinkedToAnyPlayer(null));
    }

    @Test
    void concurrentLinkAndUnlinkDoNotCorruptState() throws InterruptedException {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        int playerCount = 64;
        UUID[] players = new UUID[playerCount];
        for (int i = 0; i < playerCount; i++) {
            players[i] = UUID.randomUUID();
        }
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger errors = new AtomicInteger();
        for (UUID player : players) {
            pool.submit(() -> {
                try {
                    registry.link(boss, player);
                } catch (RuntimeException exception) {
                    errors.incrementAndGet();
                }
            });
            pool.submit(() -> {
                try {
                    registry.unlink(boss, player);
                } catch (RuntimeException exception) {
                    errors.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        boolean done = pool.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(done, "concurrent tasks did not finish in time");
        assertEquals(0, errors.get(), "no concurrent exception expected");
        Set<UUID> snapshot = registry.playersLinkedTo(boss);
        assertTrue(snapshot.size() <= playerCount);
        assertTrue(new HashSet<>(snapshot).containsAll(snapshot));
    }

    @Test
    void playersLinkedToReturnsDefensiveCopyAfterConcurrentMutation() {
        BossLinkRegistry registry = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        registry.link(boss, first);
        Set<UUID> snapshot = registry.playersLinkedTo(boss);
        registry.unlink(boss, first);
        assertTrue(snapshot.contains(first));
    }
}
