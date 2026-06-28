package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectResurrectionTest {

    private static KoRecord record(ReviveState state, long deadline) {
        return new KoRecord(state, deadline, 0,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(10, 64, 10),
                Optional.empty());
    }

    private static ProlongedKoService service(KnockoutSavedData data,
                                              ProlongedKoService.ResurrectionApplier applier) {
        return new ProlongedKoService(
                () -> data,
                () -> 0L,
                ignored -> OptionalInt.empty(),
                new ReviveSyncService(new NoopSink()),
                new BossLinkRegistry(),
                applier,
                new ProlongedKoService.Config(6_000));
    }

    @Test
    void reconnectOnPendingReviveTriggersResurrectionAndCleansRecord() {
        KnockoutSavedData data = new KnockoutSavedData();
        AtomicReference<UUID> received = new AtomicReference<>();
        ProlongedKoService.ResurrectionApplier applier = (uuid, dim, pos, sickness) -> {
            received.set(uuid);
            return ProlongedKoService.ResurrectionResult.success();
        };
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PENDING_REVIVE, 0L));
        ProlongedKoService prolonged = service(data, applier);

        boolean applied = KoEventHandlers.tryResurrectOnReconnect(prolonged, player);

        assertTrue(applied);
        assertEquals(player, received.get());
        assertTrue(data.get(player).isEmpty());
    }

    @Test
    void reconnectOnPendingDeathDoesNotResurrect() {
        KnockoutSavedData data = new KnockoutSavedData();
        AtomicReference<UUID> received = new AtomicReference<>();
        ProlongedKoService.ResurrectionApplier applier = (uuid, dim, pos, sickness) -> {
            received.set(uuid);
            return ProlongedKoService.ResurrectionResult.success();
        };
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PENDING_DEATH, 0L));
        ProlongedKoService prolonged = service(data, applier);

        boolean applied = KoEventHandlers.tryResurrectOnReconnect(prolonged, player);

        assertFalse(applied);
        assertNull(received.get());
        assertTrue(data.get(player).isPresent());
    }

    @Test
    void reconnectOnUnknownPlayerIsNoOp() {
        KnockoutSavedData data = new KnockoutSavedData();
        AtomicReference<UUID> received = new AtomicReference<>();
        ProlongedKoService.ResurrectionApplier applier = (uuid, dim, pos, sickness) -> {
            received.set(uuid);
            return ProlongedKoService.ResurrectionResult.success();
        };
        ProlongedKoService prolonged = service(data, applier);

        boolean applied = KoEventHandlers.tryResurrectOnReconnect(prolonged, UUID.randomUUID());

        assertFalse(applied);
        assertNull(received.get());
    }

    @Test
    void reconnectWhenResurrectionSkippedKeepsRecord() {
        KnockoutSavedData data = new KnockoutSavedData();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PENDING_REVIVE, 0L));
        ProlongedKoService prolonged = service(data,
                (uuid, dim, pos, sickness) -> ProlongedKoService.ResurrectionResult.skipped());

        boolean applied = KoEventHandlers.tryResurrectOnReconnect(prolonged, player);

        assertFalse(applied);
        assertTrue(data.get(player).isPresent());
    }

    private static final class NoopSink implements PacketSink {

        @Override
        public void sendSelf(UUID playerUuid, ClientboundReviveSnapshot packet) {
        }

        @Override
        public void sendTracking(UUID playerUuid, ClientboundTrackedKoVisual packet) {
        }

        @Override
        public void sendTo(UUID recipientUuid, ClientboundTrackedKoVisual packet) {
        }
    }
}
