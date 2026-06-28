package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of the player metadata that the revive services need in order
 * to make a decision. Built by the orchestrating layer from
 * {@link KoPlayerProbe} so the services themselves remain pure and easy
 * to test.
 */
public record KnockoutPlayerSnapshot(
        UUID playerUuid,
        ReviveState state,
        BlockPos position,
        ResourceLocation dimension,
        int hotbarSlot,
        boolean hasSoulAnchorInHotbar,
        float currentHealth,
        float maxHealth
) {
    public KnockoutPlayerSnapshot {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(dimension, "dimension");
    }
}
