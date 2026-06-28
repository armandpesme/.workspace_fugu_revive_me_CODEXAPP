package com.fuguteams.fugureviveme.state;

import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory description of an in-progress revive interaction (ally revive or
 * soul anchor self-revive). Actions live only in the running server memory:
 * they are never persisted to disk because they are coupled to a live player
 * and a live helper.
 * <p>
 * The action captures the helper and target positions at start time so a
 * tick handler can compare them against the current positions and detect
 * cancellation conditions (helper moves, target moves, helper leaves the
 * revive radius, ...).
 */
public record KoAction(
        UUID targetUuid,
        Optional<UUID> helperUuid,
        ReviveActionType type,
        long deadlineGameTime,
        BlockPos helperStartPosition,
        BlockPos targetStartPosition,
        int helperStartSlot
) {
    public KoAction {
        Objects.requireNonNull(targetUuid, "targetUuid");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(helperStartPosition, "helperStartPosition");
        Objects.requireNonNull(targetStartPosition, "targetStartPosition");
        if (type == ReviveActionType.NONE) {
            throw new IllegalArgumentException("KoAction requires an active ReviveActionType");
        }
        if (type == ReviveActionType.ALLY_REVIVE && helperUuid.isEmpty()) {
            throw new IllegalArgumentException("Ally revive actions must declare a helper uuid");
        }
        if (type == ReviveActionType.SELF_REVIVE && helperUuid.isPresent()) {
            throw new IllegalArgumentException("Soul anchor self-revive must not carry a helper uuid");
        }
        if (type == ReviveActionType.RETURN_PENDANT) {
            throw new IllegalArgumentException("Return pendant is tracked separately");
        }
        if (deadlineGameTime < 0) {
            throw new IllegalArgumentException("deadlineGameTime must be non-negative");
        }
    }

    public boolean isExpired(long now) {
        return now >= deadlineGameTime;
    }

    public boolean involvesHelper(UUID candidate) {
        return helperUuid.isPresent() && helperUuid.get().equals(candidate);
    }
}
