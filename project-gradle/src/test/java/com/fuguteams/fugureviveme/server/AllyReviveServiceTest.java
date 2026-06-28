package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllyReviveServiceTest {

    private static final AllyReviveService.AllyReviveConfig CONFIG =
            new AllyReviveService.AllyReviveConfig(100, 3.0, 0.1, 25.0);

    @Test
    void tryStartRegistersAllyActionWhenAllowed() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(sink));
        KnockoutDamageTracker damage = new KnockoutDamageTracker();
        AllyReviveService service = new AllyReviveService(
                registry, revive, damage, () -> 0L, CONFIG);

        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(2, 64, 0), 0);

        AllyReviveLogic.StartOutcome outcome = service.tryStart(helperSnap, targetSnap);

        assertInstanceOf(AllyReviveLogic.StartAllowed.class, outcome);
        assertEquals(1, registry.size());
    }

    @Test
    void tryStartRejectsWhenTargetNotInTemporaryKo() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(sink));
        AllyReviveService service = new AllyReviveService(
                registry, revive, new KnockoutDamageTracker(), () -> 0L, CONFIG);

        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.PENDING_DEATH, new BlockPos(2, 64, 0), 0);

        AllyReviveLogic.StartOutcome outcome = service.tryStart(helperSnap, targetSnap);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.TARGET_NOT_IN_TEMPORARY_KO, denied.reason());
    }

    @Test
    void tryStartRejectsWhenTargetInProlongedKo() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(sink));
        AllyReviveService service = new AllyReviveService(
                registry, revive, new KnockoutDamageTracker(), () -> 0L, CONFIG);

        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.PROLONGED_KO, new BlockPos(2, 64, 0), 0);

        AllyReviveLogic.StartOutcome outcome = service.tryStart(helperSnap, targetSnap);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void tickCompletesAndRestoresHealthAtDeadline() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        ReviveService revive = new ReviveService(
                () -> data, () -> 50L, new ReviveSyncService(sink));
        KnockoutDamageTracker damage = new KnockoutDamageTracker();
        AllyReviveService service = new AllyReviveService(
                registry, revive, damage, () -> 100L, CONFIG);

        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        data.put(target, new KoRecord(
                ReviveState.TEMPORARY_KO, 100L, 0,
                ResourceLocation.parse("minecraft:overworld"), new BlockPos(0, 64, 0), Optional.empty()));
        registry.start(new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0));

        Map<UUID, KnockoutPlayerSnapshot> snapshots = new HashMap<>();
        snapshots.put(helper, snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0));
        snapshots.put(target, snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0));
        List<UUID> sicknessApplied = new ArrayList<>();
        List<Float> healthRestored = new ArrayList<>();
        AllyReviveService.KoTickInputs inputs = new TestTickInputs(
                snapshots, sicknessApplied, healthRestored);

        service.tick(inputs);

        assertEquals(0, registry.size());
        assertTrue(data.get(target).isEmpty());
        assertEquals(List.of(target), sicknessApplied);
        assertEquals(1, healthRestored.size());
    }

    @Test
    void tickCancelsWhenHelperLeavesRadius() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        RecordingSink sink = new RecordingSink();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(sink));
        AllyReviveService service = new AllyReviveService(
                registry, revive, new KnockoutDamageTracker(), () -> 50L, CONFIG);

        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        data.put(target, new KoRecord(
                ReviveState.TEMPORARY_KO, 100L, 0,
                ResourceLocation.parse("minecraft:overworld"), new BlockPos(0, 64, 0), Optional.empty()));
        registry.start(new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0));

        Map<UUID, KnockoutPlayerSnapshot> snapshots = new HashMap<>();
        snapshots.put(helper, snapshot(helper, ReviveState.ALIVE, new BlockPos(5, 64, 0), 0));
        snapshots.put(target, snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0));
        AllyReviveService.KoTickInputs inputs = new TestTickInputs(
                snapshots, new ArrayList<>(), new ArrayList<>());

        service.tick(inputs);

        assertEquals(0, registry.size());
        assertTrue(data.get(target).isPresent());
        assertEquals(ReviveState.TEMPORARY_KO, data.get(target).orElseThrow().state());
    }

    private static KnockoutPlayerSnapshot snapshot(
            UUID uuid, ReviveState state, BlockPos position, int slot) {
        return new KnockoutPlayerSnapshot(
                uuid, state, position,
                ResourceLocation.parse("minecraft:overworld"),
                slot, false, 20F, 20F);
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

    private record TestTickInputs(
            Map<UUID, KnockoutPlayerSnapshot> snapshots,
            List<UUID> sicknessApplied,
            List<Float> healthRestored
    ) implements AllyReviveService.KoTickInputs {
        @Override
        public Optional<KnockoutPlayerSnapshot> snapshotOf(UUID playerUuid) {
            return Optional.ofNullable(snapshots.get(playerUuid));
        }

        @Override
        public void applyResurrectionSickness(UUID targetUuid) {
            sicknessApplied.add(targetUuid);
        }

        @Override
        public void restoreHealthToPercent(KnockoutPlayerSnapshot snapshot, double percent) {
            healthRestored.add((float) percent);
        }
    }
}
