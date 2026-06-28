package com.fuguteams.fugureviveme.registry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCreativeTabsSourceTest {
    @Test
    void mainCreativeTabContainsBothModItems() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/fuguteams/fugureviveme/registry/ModCreativeTabs.java"));

        assertTrue(source.contains("Registries.CREATIVE_MODE_TAB"));
        assertTrue(source.contains("itemGroup.fugu_revive_me.main"));
        assertTrue(source.contains("output.accept(ModItems.SOUL_ANCHOR.get())"));
        assertTrue(source.contains("output.accept(ModItems.RETURN_PENDANT.get())"));
    }
}
