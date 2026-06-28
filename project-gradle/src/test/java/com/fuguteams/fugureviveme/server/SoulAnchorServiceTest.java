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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulAnchorServiceTest {

    private static final SoulAnchorService.SoulAnchorConfig CONFIG =
            new SoulAnchorService.SoulAnchorConfig(100, 0.1, 25.0);

    @Test
    void tryStartRegistersSelfReviveWhenAllConditionsHold() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(new RecordingSink()));
        SoulAnchorService service = new SoulAnchorService(
                registry, revive, new KnockoutDamageTracker(), () -> 0L, CONFIG);

        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot snap = new KnockoutPlayerSnapshot(
                target, ReviveState.TEMPORARY_KO, BlockPos.ZERO,
                ResourceLocation.parse("minecraft:overworld"),
                0, true, 1F, 20F);

        SoulAnchorLogic.StartOutcome outcome = service.tryStart(snap, 0);

        assertInstanceOf(SoulAnchorLogic.StartAllowed.class, outcome);
        assertEquals(1, registry.size());
    }

    @Test
    void tryStartRejectsWhenAnchorNotInHotbar() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(new RecordingSink()));
        SoulAnchorService service = new SoulAnchorService(
                registry, revive, new KnockoutDamageTracker(), () -> 0L, CONFIG);

        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot snap = new KnockoutPlayerSnapshot(
                target, ReviveState.TEMPORARY_KO, BlockPos.ZERO,
                ResourceLocation.parse("minecraft:overworld"),
                0, false, 1F, 20F);

        SoulAnchorLogic.StartOutcome outcome = service.tryStart(snap, 0);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.ANCHOR_NOT_IN_HOTBAR, denied.reason());
    }

    @Test
    void tryStartRejectsWhenTargetInProlongedKo() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(new RecordingSink()));
        SoulAnchorService service = new SoulAnchorService(
                registry, revive, new KnockoutDamageTracker(), () -> 0L, CONFIG);

        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot snap = new KnockoutPlayerSnapshot(
                target, ReviveState.PROLONGED_KO, BlockPos.ZERO,
                ResourceLocation.parse("minecraft:overworld"),
                0, true, 1F, 20F);

        SoulAnchorLogic.StartOutcome outcome = service.tryStart(snap, 0);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void tickCompletesConsumesAnchorAndRestoresHealth() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        ReviveService revive = new ReviveService(
                () -> data, () -> 100L, new ReviveSyncService(new RecordingSink()));
        SoulAnchorService service = new SoulAnchorService(
                registry, revive, new KnockoutDamageTracker(), () -> 100L, CONFIG);

        UUID target = UUID.randomUUID();
        data.put(target, new KoRecord(
                ReviveState.TEMPORARY_KO, 100L, 0,
                ResourceLocation.parse("minecraft:overworld"), BlockPos.ZERO, Optional.empty()));
        registry.start(new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0));

        Map<UUID, KnockoutPlayerSnapshot> snapshots = new HashMap<>();
        snapshots.put(target, new KnockoutPlayerSnapshot(
                target, ReviveState.TEMPORARY_KO, BlockPos.ZERO,
                ResourceLocation.parse("minecraft:overworld"),
                0, true, 1F, 20F));
        List<UUID> sickness = new ArrayList<>();
        List<Float> health = new ArrayList<>();
        List<SlotConsumption> consumed = new ArrayList<>();
        SoulAnchorService.KoTickInputs inputs = new TestTickInputs(snapshots, sickness, health, consumed);

        service.tick(inputs);

        assertEquals(0, registry.size());
        assertTrue(data.get(target).isEmpty());
        assertEquals(List.of(target), sickness);
        assertEquals(1, health.size());
        assertEquals(List.of(new SlotConsumption(target, 0)), consumed);
    }

    @Test
    void tickCancelsWhenAnchorRemoved() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        KnockoutSavedData data = new KnockoutSavedData();
        ReviveService revive = new ReviveService(
                () -> data, () -> 0L, new ReviveSyncService(new RecordingSink()));
        SoulAnchorService service = new SoulAnchorService(
                registry, revive, new KnockoutDamageTracker(), () -> 50L, CONFIG);

        UUID target = UUID.randomUUID();
        data.put(target, new KoRecord(
                ReviveState.TEMPORARY_KO, 100L, 0,
                ResourceLocation.parse("minecraft:overworld"), BlockPos.ZERO, Optional.empty()));
        registry.start(new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0));

        Map<UUID, KnockoutPlayerSnapshot> snapshots = new HashMap<>();
        snapshots.put(target, new KnockoutPlayerSnapshot(
                target, ReviveState.TEMPORARY_KO, BlockPos.ZERO,
                ResourceLocation.parse("minecraft:overworld"),
                0, false, 1F, 20F));
        SoulAnchorService.KoTickInputs inputs = new TestTickInputs(
                snapshots, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        service.tick(inputs);

        assertEquals(0, registry.size());
        assertTrue(data.get(target).isPresent());
    }

    private record SlotConsumption(UUID target, int slot) {
    }

    private static final class RecordingSink implements PacketSink {
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

    private record TestTickInputs(
            Map<UUID, KnockoutPlayerSnapshot> snapshots,
            List<UUID> sicknessApplied,
            List<Float> healthRestored,
            List<SlotConsumption> consumedSlots
    ) implements SoulAnchorService.KoTickInputs {
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

        @Override
        public void consumeSoulAnchor(UUID targetUuid, int slot) {
            consumedSlots.add(new SlotConsumption(targetUuid, slot));
        }
    }
}
