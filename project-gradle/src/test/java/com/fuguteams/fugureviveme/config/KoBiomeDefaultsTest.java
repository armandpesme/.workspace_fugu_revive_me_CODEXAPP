package com.fuguteams.fugureviveme.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoBiomeDefaultsTest {
    private static final List<String> TIERS = List.of("high", "low", "mid", "special");
    private static final List<String> BIOME_NAMES = List.of(
            "acid",
            "apocalyse",
            "badlands",
            "biome_froid",
            "champinons",
            "crepuscule",
            "desert",
            "elfique",
            "feerique",
            "foret_sombre",
            "foret_temperee",
            "foret",
            "jungle",
            "lagon_calme",
            "lunaire",
            "marais_de_chair",
            "marais",
            "montagne",
            "oasis",
            "ocean",
            "prairie",
            "royaume_des_esprits",
            "sakura_ronin_dechu",
            "sakura_sacre",
            "savane",
            "space",
            "steppe"
    );

    @Test
    void temporaryKoBiomeListIsEmptyByDefault() {
        assertTrue(KoBiomeDefaults.TEMPORARY_BIOMES.isEmpty());
    }

    @Test
    void prolongedKoBiomesMatchThe108DatapackIdsExactlyAndUniquely() {
        Set<String> expected = TIERS.stream()
                .flatMap(tier -> BIOME_NAMES.stream()
                        .map(name -> "fugubiomes:fugu_" + tier + "_" + name))
                .collect(Collectors.toSet());

        assertEquals(108, KoBiomeDefaults.PROLONGED_BIOMES.size());
        assertEquals(108, new HashSet<>(KoBiomeDefaults.PROLONGED_BIOMES).size());
        assertEquals(expected, new HashSet<>(KoBiomeDefaults.PROLONGED_BIOMES));
    }

    @Test
    void everyProlongedBiomeIsAValidFugubiomesResourceLocation() {
        for (String biomeId : KoBiomeDefaults.PROLONGED_BIOMES) {
            ResourceLocation parsed = ResourceLocation.tryParse(biomeId);
            assertNotNull(parsed, biomeId);
            assertEquals("fugubiomes", parsed.getNamespace(), biomeId);
            assertEquals(biomeId, parsed.toString(), biomeId);
        }
    }
}
