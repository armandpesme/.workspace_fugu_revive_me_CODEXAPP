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

class SoulAnchorLogicTest {

    private static KnockoutPlayerSnapshot snapshot(
            UUID uuid, ReviveState state, BlockPos position, int slot, boolean anchorInHotbar) {
        return new KnockoutPlayerSnapshot(
                uuid, state, position,
                ResourceLocation.parse("minecraft:overworld"),
                slot, anchorInHotbar, 1F, 20F);
    }

    @Test
    void startAllowedWhenAllConditionsHold() {
        UUID target = UUID.randomUUID();
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, false, 0L, 100);

        SoulAnchorLogic.StartAllowed allowed = assertInstanceOf(SoulAnchorLogic.StartAllowed.class, outcome);
        assertEquals(target, allowed.action().targetUuid());
        assertEquals(Optional.empty(), allowed.action().helperUuid());
        assertEquals(0, allowed.action().helperStartSlot());
        assertEquals(100L, allowed.action().deadlineGameTime());
    }

    @Test
    void startDeniedWhenNotInTemporaryKo() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.PENDING_DEATH, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.TARGET_NOT_IN_TEMPORARY_KO, denied.reason());
    }

    @Test
    void startDeniedWhenInProlongedKo() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.PROLONGED_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void startDeniedWhenFullyDowned() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.FULLY_DOWNED, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.TARGET_IN_PROLONGED_KO, denied.reason());
    }

    @Test
    void startDeniedWhenAlreadyActive() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, true, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.ALREADY_ACTIVE, denied.reason());
    }

    @Test
    void startDeniedWhenAnchorNotInHotbar() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, false);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.ANCHOR_NOT_IN_HOTBAR, denied.reason());
    }

    @Test
    void startDeniedWhenSlotOutOfHotbar() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 9, false, false, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.ANCHOR_NOT_IN_HOTBAR, denied.reason());
    }

    @Test
    void startDeniedWhenDamageRecently() {
        KnockoutPlayerSnapshot snap = snapshot(UUID.randomUUID(), ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                snap, 0, false, true, 0L, 100);

        SoulAnchorLogic.StartDenied denied = assertInstanceOf(SoulAnchorLogic.StartDenied.class, outcome);
        assertEquals(SoulAnchorLogic.StartDenial.PLAYER_TAKEN_DAMAGE_RECENTLY, denied.reason());
    }

    @Test
    void tickContinuesWhenAllConditionsHold() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 50L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.CONTINUE, verdict.outcome());
        assertTrue(verdict.reason().isEmpty());
    }

    @Test
    void tickCancelsWhenSlotChanged() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, BlockPos.ZERO, 1, true);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 50L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(SoulAnchorLogic.CancelReason.SLOT_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenAnchorRemovedFromHotbar() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, false);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 50L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(SoulAnchorLogic.CancelReason.ANCHOR_REMOVED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenPlayerMoved() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, new BlockPos(1, 64, 0), 0, true);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 50L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(SoulAnchorLogic.CancelReason.PLAYER_MOVED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCancelsWhenTargetStateChanged() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.FULLY_DOWNED, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 50L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(SoulAnchorLogic.CancelReason.TARGET_STATE_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void tickCompletesWhenDeadlineReached() {
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, BlockPos.ZERO, BlockPos.ZERO, 0);
        KnockoutPlayerSnapshot snap = snapshot(target, ReviveState.TEMPORARY_KO, BlockPos.ZERO, 0, true);

        SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(action, snap, 100L, 0.1);

        assertEquals(SoulAnchorLogic.TickOutcome.COMPLETE, verdict.outcome());
    }
}
