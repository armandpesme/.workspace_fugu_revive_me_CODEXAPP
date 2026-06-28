package com.fuguteams.fugureviveme.network;

import net.minecraftforge.network.NetworkDirection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuguNetworkTest {
    @Test
    void protocolAndRegistrationsAreExplicitAndSequential() {
        assertEquals("1", FuguNetwork.PROTOCOL_VERSION);
        assertEquals(List.of(
                new FuguNetwork.PacketRegistration(0, ClientboundReviveSnapshot.class, NetworkDirection.PLAY_TO_CLIENT),
                new FuguNetwork.PacketRegistration(1, ClientboundTrackedKoVisual.class, NetworkDirection.PLAY_TO_CLIENT),
                new FuguNetwork.PacketRegistration(2, ServerboundReleaseSpirit.class, NetworkDirection.PLAY_TO_SERVER)
        ), FuguNetwork.registrations());
    }

    @Test
    void sourceDeclaresVersionedSimpleChannelAndMessageHandlers() throws IOException {
        String networkClass = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/fuguteams/fugureviveme/network/FuguNetwork.java"));
        String forgeSink = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/fuguteams/fugureviveme/server/ForgePacketSink.java"));

        assertTrue(networkClass.contains("NetworkRegistry.newSimpleChannel"));
        assertTrue(networkClass.contains("PROTOCOL_VERSION::equals"));
        assertTrue(networkClass.contains("messageBuilder(ClientboundReviveSnapshot.class"));
        assertTrue(networkClass.contains("messageBuilder(ClientboundTrackedKoVisual.class"));
        assertTrue(networkClass.contains("messageBuilder(ServerboundReleaseSpirit.class"));
        assertTrue(networkClass.contains("if (registered)"));
        assertTrue(forgeSink.contains("PacketDistributor.PLAYER"));
        assertTrue(forgeSink.contains("PacketDistributor.TRACKING_ENTITY_AND_SELF"));
    }

    @Test
    void modConstructorRegistersNetworkChannel() throws IOException {
        String modClass = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/fuguteams/fugureviveme/FuguReviveMe.java"));

        assertTrue(modClass.contains("FuguNetwork.register()"));
    }
}
