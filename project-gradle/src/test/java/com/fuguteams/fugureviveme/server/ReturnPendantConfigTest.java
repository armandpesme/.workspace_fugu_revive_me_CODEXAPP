package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReturnPendantConfigTest {

    private static final ResourceLocation OVERWORLD = ResourceLocation.parse("minecraft:overworld");

    @Test
    void configCarriesAllFields() {
        ReturnPendantService.ReturnPendantConfig config =
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, 6000, 0.1);

        assertEquals(OVERWORLD, config.mainDimension());
        assertEquals(600, config.castTimeTicks());
        assertEquals(6000, config.cooldownTicks());
        assertEquals(0.1, config.movementTolerance(), 0.0001);
    }

    @Test
    void rejectsNullMainDimension() {
        assertThrows(NullPointerException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(null, 600, 6000, 0.1));
    }

    @Test
    void rejectsZeroCastTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 0, 6000, 0.1));
    }

    @Test
    void rejectsNegativeCastTime() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, -1, 6000, 0.1));
    }

    @Test
    void rejectsZeroCooldown() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, 0, 0.1));
    }

    @Test
    void rejectsNegativeCooldown() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, -1, 0.1));
    }

    @Test
    void rejectsNegativeMovementTolerance() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, 6000, -0.1));
    }

    @Test
    void acceptsZeroMovementTolerance() {
        ReturnPendantService.ReturnPendantConfig config =
                new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, 6000, 0.0);
        assertEquals(0.0, config.movementTolerance(), 0.0001);
    }
}
