package com.fuguteams.fugureviveme.config;

import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoConfigSnapshot;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void biomeListsAcceptSingleAndLargeValidListsButRejectInvalidIds() {
        ForgeConfigSpec.ValueSpec temporaryBiomes = valueSpec("temporary_ko", "biomes");
        ForgeConfigSpec.ValueSpec prolongedBiomes = valueSpec("prolonged_ko", "biomes");
        List<String> largeList = IntStream.range(0, 10_000)
                .mapToObj(index -> "fugubiomes:configured_" + index)
                .toList();

        assertTrue(temporaryBiomes.test(List.of("fugubiomes:single")));
        assertTrue(prolongedBiomes.test(largeList));
        assertFalse(temporaryBiomes.test(List.of("not a resource location")));
        assertFalse(prolongedBiomes.test(List.of("Minecraft:invalid_namespace")));
    }

    @Test
    void numericValuesRejectValuesOutsideTheirDeclaredBounds() {
        assertFalse(valueSpec("temporary_ko", "duration_ticks").test(0));
        assertFalse(valueSpec("temporary_ko", "revive_max_distance").test(-0.1));
        assertFalse(valueSpec("temporary_ko", "revived_health_percent").test(100.1));
        assertFalse(valueSpec("death_respawn", "yaw").test(180.1));
        assertFalse(valueSpec("death_respawn", "pitch").test(-90.1));
        assertFalse(valueSpec("return_pendant", "movement_tolerance").test(-0.1));
    }

    @Test
    void overlappingValidBiomeListsAreAcceptedAndReportedByTheRuntimeSnapshot() {
        String overlap = "fugubiomes:configured_overlap";
        List<String> warnings = new ArrayList<>();

        assertTrue(valueSpec("temporary_ko", "biomes").test(List.of(overlap)));
        assertTrue(valueSpec("prolonged_ko", "biomes").test(List.of(overlap)));

        KoConfigSnapshot snapshot = KoConfigSnapshot.create(
                List.of(overlap),
                List.of(overlap),
                warnings::add
        );
        assertEquals(List.of(overlap), warnings);
        assertEquals(
                BiomeKoClassifier.KoType.PROLONGED,
                snapshot.classify(overlap).type()
        );
        assertTrue(snapshot.classify(overlap).overlap());
    }

    @Test
    void commentsDocumentUnitsAndValidationConstraints() {
        assertTrue(valueSpec("temporary_ko", "duration_ticks").getComment().contains("ticks"));
        assertTrue(valueSpec("temporary_ko", "revive_max_distance").getComment().contains("blocks"));
        assertTrue(valueSpec("temporary_ko", "revived_health_percent").getComment().contains("0 to 100"));
        assertTrue(valueSpec("temporary_ko", "biomes").getComment().contains("ResourceLocation"));
        assertTrue(valueSpec("return_pendant", "movement_tolerance").getComment().contains("blocks"));
    }

    private static ForgeConfigSpec.ValueSpec valueSpec(String... path) {
        return ServerConfig.SPEC.getSpec().get(List.of(path));
    }
}
