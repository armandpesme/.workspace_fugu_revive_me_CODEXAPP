package com.fuguteams.fugureviveme.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceIdValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:overworld",
            "fugubiomes:fugu_royaume_des_esprits",
            "fugu_revive_me:fugu_boss"
    })
    void acceptsCanonicalResourceLocations(String value) {
        assertTrue(ResourceIdValidator.isValid(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "overworld",
            "Minecraft:overworld",
            "minecraft:Overworld",
            "minecraft:bad path",
            "minecraft:one:two"
    })
    void rejectsNonCanonicalOrInvalidResourceLocations(String value) {
        assertFalse(ResourceIdValidator.isValid(value));
    }

    @Test
    void rejectsValuesThatAreNotStrings() {
        assertFalse(ResourceIdValidator.isValid(42));
    }
}
