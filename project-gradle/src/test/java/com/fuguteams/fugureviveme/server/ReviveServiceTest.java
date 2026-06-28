package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReviveServiceTest {
    private static KoRecord record(ReviveState state, long deadline) {
        return new KoRecord(state, deadline, 0,
                ResourceLocation.parse("minecraft:overworld"), BlockPos.ZERO, Optional.empty());
    }

    @Test
    void releaseSpiritOnlyAcceptsProlongedOrFullyDowned() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        UUID prolonged = UUID.randomUUID();
        UUID temporary = UUID.randomUUID();
        ReviveService service = new ReviveService(
                () -> data,
                () -> 40L,
                new ReviveSyncService(sink),
                uuid -> uuid.equals(prolonged) ? OptionalInt.of(77) : OptionalInt.empty());
        data.put(prolonged, record(ReviveState.PROLONGED_KO, 100));
        data.put(temporary, record(ReviveState.TEMPORARY_KO, 100));
        data.setDirty(false);

        assertTrue(service.requestReleaseSpirit(prolonged));
        assertFalse(service.requestReleaseSpirit(temporary));
        assertFalse(service.requestReleaseSpirit(UUID.randomUUID()));
        assertEquals(ReviveState.PENDING_DEATH, data.get(prolonged).orElseThrow().state());
        assertEquals(ReviveState.TEMPORARY_KO, data.get(temporary).orElseThrow().state());
        assertEquals(2, sink.messages.size());
        assertEquals(77, sink.trackedVisuals().get(0).entityId());
    }

    @Test
    void expirationTransitionsOnlineOrOfflineRecordsAndIdleTickSendsNothing() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        long[] clock = {9};
        UUID due = UUID.randomUUID();
        ReviveService service = new ReviveService(
                () -> data,
                () -> clock[0],
                new ReviveSyncService(sink),
                uuid -> uuid.equals(due) ? OptionalInt.of(91) : OptionalInt.empty());
        data.put(due, record(ReviveState.FULLY_DOWNED, 10));
        data.setDirty(false);

        assertEquals(0, service.tickExpirations());
        assertEquals(0, sink.messages.size());
        assertFalse(data.isDirty());

        clock[0] = 10;
        assertEquals(1, service.tickExpirations());
        assertEquals(ReviveState.PENDING_DEATH, data.get(due).orElseThrow().state());
        assertEquals(2, sink.messages.size());
        assertEquals(91, sink.trackedVisuals().get(0).entityId());
    }

    @Test
    void transitionVisualUsesActualEntityIdInsteadOfPlaceholder() {
        RecordingSink sink = new RecordingSink();
        ReviveSyncService sync = new ReviveSyncService(sink);
        UUID player = UUID.randomUUID();

        sync.publishTransition(player, 42, record(ReviveState.PROLONGED_KO, 100), 10);

        ClientboundTrackedKoVisual visual = sink.messages.stream()
                .filter(ClientboundTrackedKoVisual.class::isInstance)
                .map(ClientboundTrackedKoVisual.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(42, visual.entityId());
    }

    private static final class RecordingSink implements PacketSink {
        private final List<Object> messages = new ArrayList<>();

        @Override
        public void sendSelf(UUID playerUuid, ClientboundReviveSnapshot packet) {
            messages.add(packet);
        }

        @Override
        public void sendTracking(UUID playerUuid, ClientboundTrackedKoVisual packet) {
            messages.add(packet);
        }

        @Override
        public void sendTo(UUID recipientUuid, ClientboundTrackedKoVisual packet) {
            messages.add(packet);
        }

        private List<ClientboundTrackedKoVisual> trackedVisuals() {
            return messages.stream()
                    .filter(ClientboundTrackedKoVisual.class::isInstance)
                    .map(ClientboundTrackedKoVisual.class::cast)
                    .toList();
        }
    }
}
