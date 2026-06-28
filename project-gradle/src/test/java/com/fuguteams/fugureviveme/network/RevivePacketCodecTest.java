package com.fuguteams.fugureviveme.network;

import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RevivePacketCodecTest {
    @Test
    void snapshotCodecRoundTripsEveryField() {
        ClientboundReviveSnapshot expected = new ClientboundReviveSnapshot(
                ReviveState.FULLY_DOWNED, 100, 180, 3,
                ReviveActionType.ALLY_REVIVE, 160, Optional.of(UUID.randomUUID()), true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        expected.encode(buffer);

        assertEquals(expected, ClientboundReviveSnapshot.decode(buffer));
    }

    @Test
    void visualCodecRoundTrips() {
        ClientboundTrackedKoVisual expected =
                new ClientboundTrackedKoVisual(42, ReviveState.PROLONGED_KO, ReviveActionType.SELF_REVIVE);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        expected.encode(buffer);

        assertEquals(expected, ClientboundTrackedKoVisual.decode(buffer));
    }

    @Test
    void codecsRejectOutOfBoundsValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClientboundReviveSnapshot(ReviveState.ALIVE, 0, 0, -1,
                        ReviveActionType.NONE, 0, Optional.empty(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientboundTrackedKoVisual(-1, ReviveState.ALIVE, ReviveActionType.NONE));

        FriendlyByteBuf invalidEnum = new FriendlyByteBuf(Unpooled.buffer());
        invalidEnum.writeVarInt(1);
        invalidEnum.writeVarInt(999);
        assertThrows(IllegalArgumentException.class, () -> ClientboundTrackedKoVisual.decode(invalidEnum));
    }

    @Test
    void serverboundIntentHasNoPayload() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ServerboundReleaseSpirit.INSTANCE.encode(buffer);

        assertEquals(0, buffer.readableBytes());
        assertSame(ServerboundReleaseSpirit.INSTANCE, ServerboundReleaseSpirit.decode(buffer));
    }
}
