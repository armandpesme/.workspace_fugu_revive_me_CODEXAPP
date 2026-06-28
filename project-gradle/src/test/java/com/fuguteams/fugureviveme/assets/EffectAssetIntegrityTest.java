package com.fuguteams.fugureviveme.assets;

import com.fuguteams.fugureviveme.registry.ModEffects;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectAssetIntegrityTest {
    private static final String EFFECT_ID = "fugu_revive_me:resurrection_sickness";
    private static final String TRANSLATION_KEY = "effect.fugu_revive_me.resurrection_sickness";
    private static final String TEXTURE_PATH =
            "/assets/fugu_revive_me/textures/mob_effect/resurrection_sickness.png";

    @Test
    void resurrectionSicknessHasRegistryTranslationAndValidTextureAssets() throws IOException {
        assertEquals("resurrection_sickness", ModEffects.RESURRECTION_SICKNESS_NAME);
        assertEquals(EFFECT_ID, "fugu_revive_me:" + ModEffects.RESURRECTION_SICKNESS_NAME);
        assertTranslationExists("/assets/fugu_revive_me/lang/en_us.json");
        assertTranslationExists("/assets/fugu_revive_me/lang/fr_fr.json");

        try (InputStream stream = getClass().getResourceAsStream(TEXTURE_PATH)) {
            assertNotNull(stream, TEXTURE_PATH);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "Texture must be a readable PNG");
            assertEquals(image.getWidth(), image.getHeight(), "Effect texture must be square");
            assertTrue(isPowerOfTwo(image.getWidth()), "Effect texture width must be a power of two");
        }
    }

    private void assertTranslationExists(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject translations = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            assertTrue(translations.has(TRANSLATION_KEY), path);
        }
    }

    private static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & value - 1) == 0;
    }
}
