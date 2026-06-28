package com.fuguteams.fugureviveme.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Function;

public final class DeadlineIndex {
    private final PriorityQueue<Entry> deadlines =
            new PriorityQueue<>(Comparator.comparingLong(Entry::deadlineGameTime));
    private int lastPollHeadChecks;

    public void schedule(UUID playerUuid, long deadlineGameTime) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (deadlineGameTime < 0) {
            throw new IllegalArgumentException("deadlineGameTime must be non-negative");
        }
        deadlines.add(new Entry(playerUuid, deadlineGameTime));
    }

    public List<UUID> pollDue(long now, Function<UUID, Long> currentDeadline) {
        Objects.requireNonNull(currentDeadline, "currentDeadline");
        List<UUID> due = new ArrayList<>();
        lastPollHeadChecks = 0;
        while (!deadlines.isEmpty()) {
            lastPollHeadChecks++;
            Entry head = deadlines.peek();
            if (head.deadlineGameTime() > now) {
                break;
            }
            deadlines.remove();
            Long actualDeadline = currentDeadline.apply(head.playerUuid());
            if (actualDeadline != null && actualDeadline == head.deadlineGameTime()) {
                due.add(head.playerUuid());
            }
        }
        return List.copyOf(due);
    }

    public int lastPollHeadChecks() {
        return lastPollHeadChecks;
    }

    private record Entry(UUID playerUuid, long deadlineGameTime) {
    }
}
