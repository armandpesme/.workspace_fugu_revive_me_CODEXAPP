package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllyReviveLogicTest {

    private static KnockoutPlayerSnapshot snapshot(
            UUID uuid, ReviveState state, BlockPos position, int slot) {
        return new KnockoutPlayerSnapshot(
                uuid, state, position,
                ResourceLocation.parse("minecraft:overworld"),
                slot, false, 20F, 20F);
    }

    @Test
    void startAllowedWhenTargetInTemporaryAndHelperClose() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(2, 64, 0), 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helperSnap, targetSnap, false, 0L, 100, 3.0);

        AllyReviveLogic.StartAllowed allowed = assertInstanceOf(AllyReviveLogic.StartAllowed.class, outcome);
        assertEquals(target, allowed.action().targetUuid());
        assertEquals(Optional.of(helper), allowed.action().helperUuid());
        assertEquals(100L, allowed.action().deadlineGameTime());
    }

    @Test
    void startDeniedWhenTargetNotInTemporaryKo() {
        KnockoutPlayerSnapshot helper = snapshot(UUID.randomUUID(), ReviveState.ALIVE, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot target = snapshot(UUID.randomUUID(), ReviveState.PENDING_DEATH, BlockPos.ZERO, 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper, target, false, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.TARGET_NOT_IN_TEMPORARY_KO, denied.reason());
    }

    @Test
    void startDeniedWhenTargetInProlongedKo() {
        KnockoutPlayerSnapshot helper = snapshot(UUID.randomUUID(), ReviveState.ALIVE, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot target = snapshot(UUID.randomUUID(), ReviveState.PROLONGED_KO, BlockPos.ZERO, 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper, target, false, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void startDeniedWhenTargetFullyDowned() {
        KnockoutPlayerSnapshot helper = snapshot(UUID.randomUUID(), ReviveState.ALIVE, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot target = snapshot(UUID.randomUUID(), ReviveState.FULLY_DOWNED, BlockPos.ZERO, 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper, target, false, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void startDeniedWhenHelperTooFar() {
        KnockoutPlayerSnapshot helper = snapshot(UUID.randomUUID(), ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot target = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, new BlockPos(10, 64, 0), 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper, target, false, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.HELPER_DISTANCE_OUT_OF_RANGE, denied.reason());
    }

    @Test
    void startDeniedWhenActionAlreadyActive() {
        KnockoutPlayerSnapshot helper = snapshot(UUID.randomUUID(), ReviveState.ALIVE, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot target = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper, target, true, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.ALREADY_ACTIVE, denied.reason());
    }

    @Test
    void startDeniedWhenHelperSameAsTarget() {
        UUID same = UUID.randomUUID();
        KnockoutPlayerSnapshot snap = snapshot(same, ReviveState.ALIVE, BlockPos.ZERO, 0);

        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                snap, snap, false, 0L, 100, 3.0);

        AllyReviveLogic.StartDenied denied = assertInstanceOf(AllyReviveLogic.StartDenied.class, outcome);
        assertEquals(AllyReviveLogic.StartDenial.HELPER_SAME_AS_TARGET, denied.reason());
    }

    @Test
    void tickContinuesWhenAllConditionsHold() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CONTINUE, verdict.outcome());
        assertTrue(verdict.reason().isEmpty());
    }

    @Test
    void tickCancelsWhenHelperTakesDamage() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, true, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.HELPER_DAMAGED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenHelperLeavesRadius() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(5, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.HELPER_LEFT_RADIUS, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenHelperMoved() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(1, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.HELPER_MOVED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenTargetMoved() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(1, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.TARGET_MOVED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenHelperChangedSlot() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 3);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.HELPER_CHANGED_SLOT, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenTargetStateChanged() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.FULLY_DOWNED, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.TARGET_STATE_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenDimensionChanged() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = new KnockoutPlayerSnapshot(
                helper, ReviveState.ALIVE, new BlockPos(0, 64, 0),
                ResourceLocation.parse("minecraft:the_nether"), 0, false, 20F, 20F);
        KnockoutPlayerSnapshot targetSnap = new KnockoutPlayerSnapshot(
                target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0),
                ResourceLocation.parse("minecraft:overworld"), 0, false, 1F, 20F);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 50L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(AllyReviveLogic.CancelReason.DIMENSION_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCompletesWhenDeadlineReached() {
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot helperSnap = snapshot(helper, ReviveState.ALIVE, new BlockPos(0, 64, 0), 0);
        KnockoutPlayerSnapshot targetSnap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(0, 64, 0), 0);

        AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                action, helperSnap, targetSnap, 100L, 3.0, false, 0.1);

        assertEquals(AllyReviveLogic.TickOutcome.COMPLETE, verdict.outcome());
    }
}
