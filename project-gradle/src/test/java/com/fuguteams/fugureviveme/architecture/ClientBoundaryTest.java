package com.fuguteams.fugureviveme.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBoundaryTest {
    @Test
    void commonAndServerSourcesDoNotImportMinecraftClient() throws IOException {
        Path root = Path.of("src/main/java/com/fuguteams/fugureviveme");
        try (var files = Files.walk(root)) {
            assertTrue(files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().replace('\\', '/').contains("/client/"))
                    .noneMatch(ClientBoundaryTest::importsClientOnlyCode));
        }
    }

    private static boolean importsClientOnlyCode(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("import net.minecraft.client")
                    || source.contains("import com.fuguteams.fugureviveme.client");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
