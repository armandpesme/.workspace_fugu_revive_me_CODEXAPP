package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.config.KoBiomeDefaults;
import com.fuguteams.fugureviveme.config.ServerConfig;
import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoConfigSnapshot;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ServerConfigRuntime {
    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile KoConfigSnapshot snapshot = KoConfigSnapshot.create(
            KoBiomeDefaults.TEMPORARY_BIOMES,
            KoBiomeDefaults.PROLONGED_BIOMES,
            ignored -> {
            }
    );

    private ServerConfigRuntime() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ServerConfigRuntime::onConfigLoading);
        modEventBus.addListener(ServerConfigRuntime::onConfigReloading);
    }

    public static BiomeKoClassifier.Result classify(String biomeId) {
        return snapshot.classify(biomeId);
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        refreshIfRelevant(event);
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        refreshIfRelevant(event);
    }

    private static void refreshIfRelevant(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ServerConfig.SPEC) {
            return;
        }

        snapshot = KoConfigSnapshot.create(
                ServerConfig.TEMPORARY_KO_BIOMES.get(),
                ServerConfig.PROLONGED_KO_BIOMES.get(),
                resourceLocation -> LOGGER.warn(
                        "Biome {} is configured for both temporary_ko and prolonged_ko; "
                                + "prolonged_ko takes priority.",
                        resourceLocation
                )
        );
    }
}
