package com.fuguteams.fugureviveme.assets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAssetIntegrityTest {
    private static final Map<String, String> ITEMS = Map.of(
            "soul_anchor", "item.fugu_revive_me.soul_anchor",
            "return_pendant", "item.fugu_revive_me.return_pendant"
    );

    @Test
    void registeredItemsHaveModelsTexturesAndTranslations() throws IOException {
        JsonObject enUs = readJson("/assets/fugu_revive_me/lang/en_us.json");
        JsonObject frFr = readJson("/assets/fugu_revive_me/lang/fr_fr.json");
        for (Map.Entry<String, String> item : ITEMS.entrySet()) {
            assertTrue(enUs.has(item.getValue()), item.getValue());
            assertTrue(frFr.has(item.getValue()), item.getValue());
            assertModel(item.getKey());
            assertTexture(item.getKey());
        }
    }

    @Test
    void bossTagIsPresentAndExtensibleByDatapack() throws IOException {
        JsonObject tag = readJson("/data/fugu_revive_me/tags/entity_types/fugu_boss.json");

        assertTrue(tag.has("replace"), "Boss tag must define replace");
        assertEquals(false, tag.get("replace").getAsBoolean(), "Boss tag must be extensible");
        assertTrue(tag.has("values"), "Boss tag must define values");
        assertTrue(tag.getAsJsonArray("values").isEmpty(), "Default boss tag is intentionally empty");
    }

    private static void assertModel(String itemName) throws IOException {
        JsonObject model = readJson("/assets/fugu_revive_me/models/item/" + itemName + ".json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("fugu_revive_me:item/" + itemName,
                model.getAsJsonObject("textures").get("layer0").getAsString());
    }

    private static void assertTexture(String itemName) throws IOException {
        String path = "/assets/fugu_revive_me/textures/item/" + itemName + ".png";
        try (InputStream stream = ItemAssetIntegrityTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "Texture must be a readable PNG");
            assertEquals(16, image.getWidth(), "Item texture width");
            assertEquals(16, image.getHeight(), "Item texture height");
        }
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream stream = ItemAssetIntegrityTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
