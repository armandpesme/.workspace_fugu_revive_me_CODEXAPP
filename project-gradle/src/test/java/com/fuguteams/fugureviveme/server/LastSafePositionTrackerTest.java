package com.fuguteams.fugureviveme.server;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastSafePositionTrackerTest {
    @Test
    void unknownPlayerHasNoTransientFallback() {
        assertTrue(new LastSafePositionTracker().get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void samplesOnlySafePositionsEveryTenTicks() {
        LastSafePositionTracker tracker = new LastSafePositionTracker();
        UUID player = UUID.randomUUID();

        assertFalse(tracker.sample(player, 9, new BlockPos(1, 64, 1), true));
        assertTrue(tracker.get(player).isEmpty());

        assertFalse(tracker.sample(player, 10, new BlockPos(2, 64, 2), false));
        assertTrue(tracker.get(player).isEmpty());

        assertTrue(tracker.sample(player, 10, new BlockPos(3, 64, 3), true));
        assertEquals(new BlockPos(3, 64, 3), tracker.get(player).orElseThrow());
    }

    @Test
    void resolvesLastSafePositionBeforeVanillaSpawnFallback() {
        LastSafePositionTracker tracker = new LastSafePositionTracker();
        UUID player = UUID.randomUUID();
        BlockPos fallback = new BlockPos(10, 70, 10);
        AtomicInteger fallbackCalls = new AtomicInteger();

        assertEquals(fallback, tracker.resolveKoPosition(player, () -> {
            fallbackCalls.incrementAndGet();
            return fallback;
        }));
        assertEquals(1, fallbackCalls.get());

        tracker.record(player, new BlockPos(4, 65, 4));

        assertEquals(new BlockPos(4, 65, 4), tracker.resolveKoPosition(player, () -> {
            fallbackCalls.incrementAndGet();
            return fallback;
        }));
        assertEquals(1, fallbackCalls.get());
    }
}
