package com.fuguteams.fugureviveme.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutDamageTrackerTest {

    @Test
    void flagAndConsumeReturnsTrueThenFalse() {
        KnockoutDamageTracker tracker = new KnockoutDamageTracker();
        UUID player = UUID.randomUUID();

        assertFalse(tracker.consume(player));
        tracker.flag(player, 100L);
        assertTrue(tracker.isFlagged(player));
        assertTrue(tracker.consume(player));
        assertFalse(tracker.consume(player));
        assertFalse(tracker.isFlagged(player));
    }

    @Test
    void clearRemovesEveryFlag() {
        KnockoutDamageTracker tracker = new KnockoutDamageTracker();
        UUID player = UUID.randomUUID();
        tracker.flag(player, 100L);
        tracker.flag(UUID.randomUUID(), 200L);

        tracker.clear();

        assertTrue(tracker.consume(player) == false);
        assertFalse(tracker.isFlagged(player));
    }
}
