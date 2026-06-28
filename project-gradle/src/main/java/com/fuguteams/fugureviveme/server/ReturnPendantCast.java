package com.fuguteams.fugureviveme.server;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

public record ReturnPendantCast(
        UUID playerUuid,
        Vec3 startPosition,
        int startSlot,
        long deadline
) {
    public ReturnPendantCast {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(startPosition, "startPosition");
        if (deadline < 0) {
            throw new IllegalArgumentException("deadline must be non-negative");
        }
    }
}
