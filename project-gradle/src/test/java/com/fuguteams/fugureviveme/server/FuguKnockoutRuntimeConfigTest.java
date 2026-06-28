package com.fuguteams.fugureviveme.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuguKnockoutRuntimeConfigTest {

    @Test
    void recordCarriesCustomBossTagAndRadiusForTests() {
        FuguKnockoutRuntime.RuntimeConfig custom = new FuguKnockoutRuntime.RuntimeConfig(
                1200, 3, 100, 3.0, 25.0,
                6000, 3, 42.0, "my_mod:custom_boss", 6000);
        assertEquals("my_mod:custom_boss", custom.prolongedBossTag());
        assertEquals(42.0, custom.prolongedBossSearchRadius(), 0.0001);
        assertNotNull(custom.allyReviveConfig());
        assertTrue(custom.allyReviveConfig().reviveMaxDistance() == 3.0);
    }

    @Test
    void recordAcceptsAnotherCustomBossTagWithoutMutatingExisting() {
        FuguKnockoutRuntime.RuntimeConfig first = new FuguKnockoutRuntime.RuntimeConfig(
                1200, 3, 100, 3.0, 25.0,
                6000, 3, 20.0, "mod_a:boss_a", 6000);
        FuguKnockoutRuntime.RuntimeConfig second = new FuguKnockoutRuntime.RuntimeConfig(
                1200, 3, 100, 3.0, 25.0,
                6000, 3, 20.0, "mod_b:boss_b", 6000);
        assertEquals("mod_a:boss_a", first.prolongedBossTag());
        assertEquals("mod_b:boss_b", second.prolongedBossTag());
    }
}
