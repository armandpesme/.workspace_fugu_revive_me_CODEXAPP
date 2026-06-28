package com.fuguteams.fugureviveme.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {
    @Test
    void exposesTheRequestedValidatedDefaults() {
        assertEquals("fugubiomes:fugu_royaume_des_esprits", ServerConfig.DEATH_RESPAWN_DIMENSION_ID.getDefault());
        assertEquals(2428, ServerConfig.DEATH_RESPAWN_X.getDefault());
        assertEquals(66, ServerConfig.DEATH_RESPAWN_Y.getDefault());
        assertEquals(-1805, ServerConfig.DEATH_RESPAWN_Z.getDefault());
        assertEquals(0.0, ServerConfig.DEATH_RESPAWN_YAW.getDefault());
        assertEquals(0.0, ServerConfig.DEATH_RESPAWN_PITCH.getDefault());

        assertTrue(ServerConfig.TEMPORARY_KO_ENABLED.getDefault());
        assertEquals(KoBiomeDefaults.TEMPORARY_BIOMES, ServerConfig.TEMPORARY_KO_BIOMES.getDefault());
        assertEquals(1200, ServerConfig.TEMPORARY_KO_DURATION_TICKS.getDefault());
        assertEquals(3, ServerConfig.TEMPORARY_KO_MAX_HITS.getDefault());
        assertEquals(100, ServerConfig.TEMPORARY_KO_REVIVE_DURATION_TICKS.getDefault());
        assertEquals(3.0, ServerConfig.TEMPORARY_KO_REVIVE_MAX_DISTANCE.getDefault());
        assertEquals(25.0, ServerConfig.TEMPORARY_KO_REVIVED_HEALTH_PERCENT.getDefault());

        assertTrue(ServerConfig.PROLONGED_KO_ENABLED.getDefault());
        assertEquals(KoBiomeDefaults.PROLONGED_BIOMES, ServerConfig.PROLONGED_KO_BIOMES.getDefault());
        assertEquals(6000, ServerConfig.PROLONGED_KO_DURATION_TICKS.getDefault());
        assertEquals(3, ServerConfig.PROLONGED_KO_MAX_HITS.getDefault());
        assertEquals(20.0, ServerConfig.PROLONGED_KO_BOSS_SEARCH_RADIUS.getDefault());
        assertEquals("fugu_revive_me:fugu_boss", ServerConfig.PROLONGED_KO_BOSS_TAG.getDefault());

        assertEquals(6000, ServerConfig.RESURRECTION_SICKNESS_DURATION_TICKS.getDefault());
        assertEquals("minecraft:overworld", ServerConfig.RETURN_PENDANT_MAIN_DIMENSION.getDefault());
        assertEquals(600, ServerConfig.RETURN_PENDANT_CAST_TIME_TICKS.getDefault());
        assertEquals(6000, ServerConfig.RETURN_PENDANT_COOLDOWN_TICKS.getDefault());
        assertEquals(0.1, ServerConfig.RETURN_PENDANT_MOVEMENT_TOLERANCE.getDefault());
    }
}
