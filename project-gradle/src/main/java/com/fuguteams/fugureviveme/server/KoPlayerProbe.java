package com.fuguteams.fugureviveme.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Minimal abstraction over a player used by the revive services. It exposes
 * only the data points needed to take a decision and to apply the result,
 * keeping the services free of direct Minecraft classes.
 * <p>
 * The probe is intentionally narrow: every revive service consumes the
 * fields it actually needs, and a fake implementation can be provided in
 * tests.
 */
public interface KoPlayerProbe {
    BlockPos positionOf(UUID playerUuid);

    ResourceLocation dimensionOf(UUID playerUuid);

    int currentHotbarSlot(UUID playerUuid);

    boolean hasItemInHotbarSlot(UUID playerUuid, int slot);

    /**
     * @return the number of health points currently displayed on the HUD.
     *         Implementations must return the player's actual current health
     *         (not the maximum) so that the revive services can decide
     *         whether the player is already at full health.
     */
    float currentHealth(UUID playerUuid);

    float maxHealth(UUID playerUuid);

    /**
     * @return true if the player took damage since the last call; the probe
     *         is expected to remember the call and clear its flag.
     */
    boolean consumeRecentDamageFlag(UUID playerUuid);
}
