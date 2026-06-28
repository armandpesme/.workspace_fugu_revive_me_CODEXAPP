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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProlongedKoServiceTest {

    private static final ProlongedKoService.Config CONFIG =
            new ProlongedKoService.Config(6_000);
    private static final ProlongedKoService.ResurrectionApplier NOOP_RESURRECTION =
            (uuid, dimension, position, sickness) -> ProlongedKoService.ResurrectionResult.success();

    private static KoRecord record(ReviveState state, long deadline, Optional<UUID> boss) {
        return new KoRecord(
                state, deadline, 0,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                boss);
    }

    private static ProlongedKoService service(KnockoutSavedData data,
                                              BossLinkRegistry links,
                                              RecordingSink sink,
                                              long clock) {
        return new ProlongedKoService(
                () -> data, () -> clock, ignored -> OptionalInt.empty(),
                new ReviveSyncService(sink), links, NOOP_RESURRECTION, CONFIG);
    }

    @Test
    void tickExpirationsTransitionsDueRecordsToPendingDeath() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(UUID.randomUUID())));
        data.setDirty(false);
        ProlongedKoService idle = service(data, links, sink, 999L);
        assertEquals(0, idle.tickExpirations());
        assertFalse(data.isDirty());

        ProlongedKoService overdue = new ProlongedKoService(
                () -> data, () -> 1_000L, ignored -> OptionalInt.of(42),
                new ReviveSyncService(sink), links, NOOP_RESURRECTION, CONFIG);
        assertEquals(1, overdue.tickExpirations());
        assertEquals(ReviveState.PENDING_DEATH, data.get(player).orElseThrow().state());
        assertEquals(2, sink.messages.size());
    }

    @Test
    void tickExpirationsUnlinksBossWhenTimeoutFires() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, player);
        ProlongedKoService overdue = service(data, links, sink, 1_000L);

        overdue.tickExpirations();

        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void tickExpirationsOnIdleTickSendsNothing() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty()));
        ProlongedKoService idle = service(data, links, sink, 0L);

        assertEquals(0, idle.tickExpirations());
        assertTrue(sink.messages.isEmpty());
    }

    @Test
    void tickDimensionChangesMovesProlongedToPendingDeath() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        UUID boss = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, player);
        ProlongedKoService svc = service(data, links, sink, 0L);

        boolean transitioned = svc.tickDimensionChanges(
                player, ResourceLocation.parse("minecraft:the_nether"));

        assertTrue(transitioned);
        assertEquals(ReviveState.PENDING_DEATH, data.get(player).orElseThrow().state());
        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void tickDimensionChangesIsNoOpWhenSameDimension() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty()));
        ProlongedKoService svc = service(data, links, sink, 0L);

        assertFalse(svc.tickDimensionChanges(
                player, ResourceLocation.parse("minecraft:overworld")));
    }

    @Test
    void onBossDeathTransitionsAllLinkedPlayersToPendingRevive() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        data.put(first, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        data.put(second, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, first);
        links.link(boss, second);
        ProlongedKoService svc = service(data, links, sink, 5_000L);

        boolean transitioned = svc.onBossDeath(boss);

        assertTrue(transitioned);
        assertEquals(ReviveState.PENDING_REVIVE, data.get(first).orElseThrow().state());
        assertEquals(ReviveState.PENDING_REVIVE, data.get(second).orElseThrow().state());
        assertEquals(2, sink.messages.size());
        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void onBossDeathIsNoOpWhenNoPlayersLinked() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        ProlongedKoService svc = service(data, links, sink, 0L);

        assertFalse(svc.onBossDeath(UUID.randomUUID()));
        assertTrue(sink.messages.isEmpty());
    }

    @Test
    void onBossDespawnTransitionsAllLinkedPlayersToPendingDeath() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        data.put(first, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        data.put(second, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, first);
        links.link(boss, second);
        ProlongedKoService svc = service(data, links, sink, 0L);

        boolean transitioned = svc.onBossDespawn(boss);

        assertTrue(transitioned);
        assertEquals(ReviveState.PENDING_DEATH, data.get(first).orElseThrow().state());
        assertEquals(ReviveState.PENDING_DEATH, data.get(second).orElseThrow().state());
        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void onBossDespawnIsNoOpWhenNoPlayersLinked() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        ProlongedKoService svc = service(data, links, sink, 0L);

        assertFalse(svc.onBossDespawn(UUID.randomUUID()));
        assertTrue(sink.messages.isEmpty());
    }

    @Test
    void onBossDeathLeavesUnlinkedPlayersUntouched() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID linked = UUID.randomUUID();
        UUID unlinked = UUID.randomUUID();
        data.put(linked, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        data.put(unlinked, record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty()));
        links.link(boss, linked);
        ProlongedKoService svc = service(data, links, sink, 0L);

        svc.onBossDeath(boss);

        assertEquals(ReviveState.PENDING_REVIVE, data.get(linked).orElseThrow().state());
        assertEquals(ReviveState.PROLONGED_KO, data.get(unlinked).orElseThrow().state());
    }

    @Test
    void resurrectOnKoPositionCleansUpRecordAndUnlinksBoss() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        RecordingSink sink = new RecordingSink();
        RecordingResurrectionApplier applied = new RecordingResurrectionApplier();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        BlockPos pos = new BlockPos(10, 64, 10);
        data.put(player, new KoRecord(
                ReviveState.PENDING_REVIVE, 0L, 0,
                ResourceLocation.parse("minecraft:overworld"), pos, Optional.of(boss)));
        links.link(boss, player);
        ProlongedKoService svc = new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(),
                new ReviveSyncService(sink), links, applied, CONFIG);

        boolean appliedOk = svc.resurrectOnKoPosition(player);

        assertTrue(appliedOk);
        assertTrue(data.get(player).isEmpty());
        assertFalse(links.isLinkedToAnyPlayer(boss));
        assertEquals(List.of(player), applied.received);
        assertEquals(CONFIG.resurrectionSicknessDurationTicks(), applied.sicknessDuration);
    }

    @Test
    void resurrectOnKoPositionIsNoOpWhenStateIsNotPendingRevive() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        RecordingResurrectionApplier applied = new RecordingResurrectionApplier();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty()));
        ProlongedKoService svc = new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(),
                new ReviveSyncService(new RecordingSink()), links, applied, CONFIG);

        boolean appliedOk = svc.resurrectOnKoPosition(player);

        assertFalse(appliedOk);
        assertNotNull(data.get(player).orElseThrow());
        assertTrue(applied.received.isEmpty());
    }

    @Test
    void resurrectOnKoPositionDoesNotCleanUpWhenResurrectionSkipped() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        RecordingSink sink = new RecordingSink();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PENDING_REVIVE, 0L, Optional.of(boss)));
        links.link(boss, player);
        ProlongedKoService.ResurrectionApplier skip =
                (uuid, dimension, position, sickness) -> ProlongedKoService.ResurrectionResult.skipped();
        ProlongedKoService svc = new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(),
                new ReviveSyncService(sink), links, skip, CONFIG);

        assertFalse(svc.resurrectOnKoPosition(player));
        assertNotNull(data.get(player).orElseThrow());
        assertTrue(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void linkBossToPlayerStoresEntry() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        ProlongedKoService svc = service(data, links, new RecordingSink(), 0L);
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        svc.linkBossToPlayer(boss, player);

        assertTrue(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void unlinkPlayerRemovesEntryEvenWhenNoCurrentRecord() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        links.link(boss, player);
        ProlongedKoService svc = service(data, links, new RecordingSink(), 0L);

        svc.unlinkPlayer(player);

        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void unlinkPlayerCleansUpBasedOnRecord() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, player);
        ProlongedKoService svc = service(data, links, new RecordingSink(), 0L);

        svc.unlinkPlayer(player);

        assertFalse(links.isLinkedToAnyPlayer(boss));
    }

    @Test
    void configRejectsInvalidSicknessDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProlongedKoService.Config(0));
        assertThrows(IllegalArgumentException.class,
                () -> new ProlongedKoService.Config(-1));
    }

    @Test
    void pendingReviveTimeoutDoesNotFire() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PENDING_REVIVE, 1_000L, Optional.empty()));
        data.setDirty(false);
        ProlongedKoService svc = service(data, links, sink, 1_000L);

        assertEquals(0, svc.tickExpirations());
        assertFalse(data.isDirty());
    }

    @Test
    void bossDeathRespectsPlayerOfflineViaSavedData() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID boss = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        data.put(offline, record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss)));
        links.link(boss, offline);
        ProlongedKoService svc = service(data, links, sink, 0L);

        svc.onBossDeath(boss);

        assertEquals(ReviveState.PENDING_REVIVE, data.get(offline).orElseThrow().state());
        assertEquals(0, data.get(offline).orElseThrow().hitsTaken());
        assertFalse(links.isLinkedToAnyPlayer(boss));
        assertEquals(1, sink.messages.size());
    }

    @Test
    void timeoutIsComputedUsingOverworldClock() {
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        BossLinkRegistry links = new BossLinkRegistry();
        UUID player = UUID.randomUUID();
        data.put(player, record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty()));
        ProlongedKoService svc = new ProlongedKoService(
                () -> data, () -> 500L, ignored -> OptionalInt.empty(),
                new ReviveSyncService(sink), links, NOOP_RESURRECTION, CONFIG);

        assertEquals(0, svc.tickExpirations());
    }

    @Test
    void constructorRejectsNullArguments() {
        KnockoutSavedData data = new KnockoutSavedData();
        BossLinkRegistry links = new BossLinkRegistry();
        ReviveSyncService sync = new ReviveSyncService(new RecordingSink());
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                null, () -> 0L, ignored -> OptionalInt.empty(), sync, links, NOOP_RESURRECTION, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, null, ignored -> OptionalInt.empty(), sync, links, NOOP_RESURRECTION, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, () -> 0L, null, sync, links, NOOP_RESURRECTION, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(), null, links, NOOP_RESURRECTION, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(), sync, null, NOOP_RESURRECTION, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(), sync, links, null, CONFIG));
        assertThrows(NullPointerException.class, () -> new ProlongedKoService(
                () -> data, () -> 0L, ignored -> OptionalInt.empty(), sync, links, NOOP_RESURRECTION, null));
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
    }

    private static final class RecordingResurrectionApplier
            implements ProlongedKoService.ResurrectionApplier {
        private final List<UUID> received = new ArrayList<>();
        private int sicknessDuration;

        @Override
        public ProlongedKoService.ResurrectionResult apply(UUID playerUuid,
                                                          ResourceLocation koDimension,
                                                          BlockPos koPosition,
                                                          int sicknessDurationTicks) {
            received.add(playerUuid);
            sicknessDuration = sicknessDurationTicks;
            return ProlongedKoService.ResurrectionResult.success();
        }
    }
}
