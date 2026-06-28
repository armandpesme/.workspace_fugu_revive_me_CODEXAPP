package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnPendantLogicTest {

    private static final ResourceLocation OVERWORLD = ResourceLocation.parse("minecraft:overworld");
    private static final ResourceLocation NETHER = ResourceLocation.parse("minecraft:the_nether");

    @Test
    void evaluateStartAllowsWhenAllConditionsHold() {
        UUID player = UUID.randomUUID();
        Vec3 pos = new Vec3(10, 64, 20);

        ReturnPendantLogic.StartVerdict verdict = ReturnPendantLogic.evaluateStart(
                player, OVERWORLD, OVERWORLD, false, pos, 3, 100L, 600);

        ReturnPendantLogic.StartAllowed allowed = assertInstanceOf(
                ReturnPendantLogic.StartAllowed.class, verdict);
        assertEquals(player, allowed.cast().playerUuid());
        assertEquals(pos, allowed.cast().startPosition());
        assertEquals(3, allowed.cast().startSlot());
        assertEquals(700L, allowed.cast().deadline());
    }

    @Test
    void evaluateStartRejectsWhenOnCooldown() {
        UUID player = UUID.randomUUID();
        Vec3 pos = Vec3.ZERO;

        ReturnPendantLogic.StartVerdict verdict = ReturnPendantLogic.evaluateStart(
                player, OVERWORLD, OVERWORLD, true, pos, 0, 100L, 600);

        ReturnPendantLogic.StartRejected rejected = assertInstanceOf(
                ReturnPendantLogic.StartRejected.class, verdict);
        assertEquals(ReturnPendantLogic.StartRejection.ON_COOLDOWN, rejected.reason());
    }

    @Test
    void evaluateStartRejectsWhenWrongDimension() {
        UUID player = UUID.randomUUID();
        Vec3 pos = Vec3.ZERO;

        ReturnPendantLogic.StartVerdict verdict = ReturnPendantLogic.evaluateStart(
                player, NETHER, OVERWORLD, false, pos, 0, 100L, 600);

        ReturnPendantLogic.StartRejected rejected = assertInstanceOf(
                ReturnPendantLogic.StartRejected.class, verdict);
        assertEquals(ReturnPendantLogic.StartRejection.WRONG_DIMENSION, rejected.reason());
    }

    @Test
    void evaluateStartRejectsCooldownBeforeDimension() {
        UUID player = UUID.randomUUID();
        Vec3 pos = Vec3.ZERO;

        ReturnPendantLogic.StartVerdict verdict = ReturnPendantLogic.evaluateStart(
                player, NETHER, OVERWORLD, true, pos, 0, 100L, 600);

        ReturnPendantLogic.StartRejected rejected = assertInstanceOf(
                ReturnPendantLogic.StartRejected.class, verdict);
        assertEquals(ReturnPendantLogic.StartRejection.ON_COOLDOWN, rejected.reason());
    }

    @Test
    void evaluateTickContinuesWhenAllConditionsHold() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 2, 700L);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, Vec3.ZERO, 2, true, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CONTINUE, verdict.outcome());
        assertTrue(verdict.reason().isEmpty());
    }

    @Test
    void evaluateTickCancelsWhenMovedBeyondTolerance() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);
        Vec3 moved = new Vec3(0.2, 0, 0);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, moved, 0, true, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(ReturnPendantLogic.CancelReason.MOVED, verdict.reason().orElseThrow());
    }

    @Test
    void evaluateTickContinuesWhenMovementWithinTolerance() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);
        Vec3 tiny = new Vec3(0.05, 0, 0);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, tiny, 0, true, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CONTINUE, verdict.outcome());
    }

    @Test
    void evaluateTickCancelsWhenSlotChanged() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, Vec3.ZERO, 3, true, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(ReturnPendantLogic.CancelReason.SLOT_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void evaluateTickCancelsWhenItemChanged() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, Vec3.ZERO, 0, false, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(ReturnPendantLogic.CancelReason.ITEM_CHANGED, verdict.reason().orElseThrow());
    }

    @Test
    void evaluateTickCompletesWhenDeadlineReached() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, Vec3.ZERO, 0, true, 700L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.COMPLETE, verdict.outcome());
    }

    @Test
    void evaluateTickCompletesWhenDeadlineExceeded() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, Vec3.ZERO, 0, true, 800L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.COMPLETE, verdict.outcome());
    }

    @Test
    void evaluateTickChecksMovementBeforeSlotBeforeItem() {
        UUID player = UUID.randomUUID();
        ReturnPendantCast cast = new ReturnPendantCast(player, Vec3.ZERO, 0, 700L);
        Vec3 moved = new Vec3(1, 0, 0);

        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, moved, 5, false, 500L, 0.1);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertEquals(ReturnPendantLogic.CancelReason.MOVED, verdict.reason().orElseThrow());
    }
}
