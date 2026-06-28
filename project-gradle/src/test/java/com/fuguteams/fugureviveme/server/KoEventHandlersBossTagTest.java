package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoEventHandlersBossTagTest {

    @Test
    void parseBossTagAcceptsValidResourceLocation() {
        Optional<ResourceLocation> tag = KoEventHandlers.parseBossTag("fugu_revive_me:fugu_boss");
        assertTrue(tag.isPresent());
        assertEquals(ResourceLocation.parse("fugu_revive_me:fugu_boss"), tag.get());
    }

    @Test
    void parseBossTagAcceptsCustomValue() {
        Optional<ResourceLocation> tag = KoEventHandlers.parseBossTag("my_mod:custom_boss");
        assertTrue(tag.isPresent());
        assertEquals("my_mod:custom_boss", tag.get().toString());
    }

    @Test
    void parseBossTagRejectsNullEmptyAndBlank() {
        assertFalse(KoEventHandlers.parseBossTag(null).isPresent());
        assertFalse(KoEventHandlers.parseBossTag("").isPresent());
        assertFalse(KoEventHandlers.parseBossTag("   ").isPresent());
    }

    @Test
    void parseBossTagRejectsInvalidResourceLocation() {
        assertFalse(KoEventHandlers.parseBossTag("not a resource location").isPresent());
        assertFalse(KoEventHandlers.parseBossTag("Minecraft:invalid_namespace").isPresent());
    }
}
