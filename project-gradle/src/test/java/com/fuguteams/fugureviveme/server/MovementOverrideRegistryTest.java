package com.fuguteams.fugureviveme.server;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementOverrideRegistryTest {

    @Test
    void rememberKeepsFirstValueAndForgetRemovesTheEntry() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        UUID player = UUID.randomUUID();

        registry.remember(player, 0.10);
        registry.remember(player, 0.99);

        assertTrue(registry.isRemembered(player));
        assertEquals(0.10, registry.forget(player).orElseThrow(), 0.001);
        assertFalse(registry.forget(player).isPresent());
        assertFalse(registry.isRemembered(player));
    }

    @Test
    void forgetReturnsEmptyWhenNothingIsStored() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        OptionalDouble result = registry.forget(UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    @Test
    void forgetReturnsCustomSpeedInsteadOfVanillaDefault() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        UUID player = UUID.randomUUID();
        double soulSpeedBoosted = 0.1157;

        registry.remember(player, soulSpeedBoosted);

        OptionalDouble restored = registry.forget(player);
        assertTrue(restored.isPresent());
        assertEquals(soulSpeedBoosted, restored.getAsDouble(), 0.0001);
    }

    @Test
    void forgetReturnsExactStoredDoubleWithoutClamping() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        UUID player = UUID.randomUUID();
        double customSpeed = 0.42;

        registry.remember(player, customSpeed);

        assertEquals(customSpeed, registry.forget(player).orElseThrow(), 0.0001);
    }
}
