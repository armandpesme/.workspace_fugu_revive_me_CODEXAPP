package com.fuguteams.fugureviveme.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementOverrideRegistryTest {

    @Test
    void rememberKeepsFirstValueAndForgetReturnsTrueOnce() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        UUID player = UUID.randomUUID();

        registry.remember(player, 0.10);
        registry.remember(player, 0.99);

        assertTrue(registry.isRemembered(player));
        assertEquals(0.10, registry.vanillaDefault(), 0.001);
        assertTrue(registry.forget(player));
        assertFalse(registry.forget(player));
        assertFalse(registry.isRemembered(player));
    }

    @Test
    void vanillaDefaultMatchesMinecraftPlayerBaseline() {
        MovementOverrideRegistry registry = new MovementOverrideRegistry();
        assertEquals(0.1, registry.vanillaDefault(), 0.0001);
    }
}
