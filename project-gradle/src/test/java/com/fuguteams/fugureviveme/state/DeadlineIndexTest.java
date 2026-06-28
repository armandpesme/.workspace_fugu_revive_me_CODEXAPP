package com.fuguteams.fugureviveme.state;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlineIndexTest {
    @Test
    void pollBeforeTenThousandFutureDeadlinesOnlyConsultsHead() {
        DeadlineIndex index = new DeadlineIndex();
        Map<UUID, Long> current = new ConcurrentHashMap<>();
        for (int i = 0; i < 10_000; i++) {
            UUID id = new UUID(0, i + 1L);
            long deadline = 100_000L + i;
            current.put(id, deadline);
            index.schedule(id, deadline);
        }

        assertEquals(List.of(), index.pollDue(99_999L, current::get));
        assertEquals(1, index.lastPollHeadChecks());
    }

    @Test
    void staleDeadlineIsDiscardedAtPop() {
        DeadlineIndex index = new DeadlineIndex();
        UUID player = UUID.randomUUID();
        index.schedule(player, 10);
        index.schedule(player, 20);

        assertTrue(index.pollDue(10, id -> 20L).isEmpty());
        assertEquals(List.of(player), index.pollDue(20, id -> 20L));
    }
}
