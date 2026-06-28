package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnPendantServiceTest {

    private static final ResourceLocation OVERWORLD = ResourceLocation.parse("minecraft:overworld");
    private static final ResourceLocation NETHER = ResourceLocation.parse("minecraft:the_nether");

    private static final ReturnPendantService.ReturnPendantConfig CONFIG =
            new ReturnPendantService.ReturnPendantConfig(OVERWORLD, 600, 6000, 0.1);

    private ReturnPendantService serviceWithClock(long clockValue) {
        return new ReturnPendantService(() -> clockValue, CONFIG);
    }

    private ReturnPendantService serviceWithAtomicClock(long initialValue) {
        AtomicLong clock = new AtomicLong(initialValue);
        return new ReturnPendantService(clock::get, CONFIG);
    }

    @Test
    void startCastSucceedsWhenAllConditionsHold() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        ReturnPendantLogic.StartVerdict verdict = service.startCast(
                player, OVERWORLD, false, Vec3.ZERO, 0);

        assertInstanceOf(ReturnPendantLogic.StartAllowed.class, verdict);
        assertTrue(service.isCasting(player));
    }

    @Test
    void startCastRejectsWhenOnCooldown() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        ReturnPendantLogic.StartVerdict verdict = service.startCast(
                player, OVERWORLD, true, Vec3.ZERO, 0);

        assertInstanceOf(ReturnPendantLogic.StartRejected.class, verdict);
        assertFalse(service.isCasting(player));
    }

    @Test
    void startCastRejectsWhenWrongDimension() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        ReturnPendantLogic.StartVerdict verdict = service.startCast(
                player, NETHER, false, Vec3.ZERO, 0);

        assertInstanceOf(ReturnPendantLogic.StartRejected.class, verdict);
        assertFalse(service.isCasting(player));
    }

    @Test
    void startCastRejectsWhenAlreadyCasting() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);
        ReturnPendantLogic.StartVerdict second = service.startCast(
                player, OVERWORLD, false, Vec3.ZERO, 0);

        assertInstanceOf(ReturnPendantLogic.StartRejected.class, second);
        assertEquals(ReturnPendantLogic.StartRejection.ALREADY_CASTING,
                ((ReturnPendantLogic.StartRejected) second).reason());
    }

    @Test
    void tickCastContinuesWhenConditionsHold() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 0, true);

        assertEquals(ReturnPendantLogic.TickOutcome.CONTINUE, verdict.outcome());
        assertTrue(service.isCasting(player));
    }

    @Test
    void tickCastCancelsOnMovement() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, new Vec3(1, 0, 0), 0, true);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertFalse(service.isCasting(player));
    }

    @Test
    void tickCastCancelsOnSlotChange() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 5, true);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertFalse(service.isCasting(player));
    }

    @Test
    void tickCastCancelsOnItemChange() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 0, false);

        assertEquals(ReturnPendantLogic.TickOutcome.CANCEL, verdict.outcome());
        assertFalse(service.isCasting(player));
    }

    @Test
    void tickCastCompletesOnDeadline() {
        AtomicLong clock = new AtomicLong(100L);
        ReturnPendantService service = new ReturnPendantService(clock::get, CONFIG);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        clock.set(700L);
        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 0, true);

        assertEquals(ReturnPendantLogic.TickOutcome.COMPLETE, verdict.outcome());
        assertFalse(service.isCasting(player));
    }

    @Test
    void tickCastReturnsNotCastingWhenNoActiveCast() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 0, true);

        assertEquals(ReturnPendantLogic.TickOutcome.NOT_CASTING, verdict.outcome());
    }

    @Test
    void cancelCastRemovesActiveCastWithoutCooldown() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);
        assertTrue(service.isCasting(player));

        service.cancelCast(player);

        assertFalse(service.isCasting(player));
    }

    @Test
    void cancelCastIsIdempotent() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player = UUID.randomUUID();

        service.cancelCast(player);
        service.cancelCast(player);

        assertFalse(service.isCasting(player));
    }

    @Test
    void isCastingReturnsFalseForUnknownPlayer() {
        ReturnPendantService service = serviceWithClock(100L);
        assertFalse(service.isCasting(UUID.randomUUID()));
    }

    @Test
    void onCompleteInvokesCooldownAndTeleportCallbacks() {
        AtomicLong clock = new AtomicLong(100L);
        List<UUID> cooldownApplied = new ArrayList<>();
        List<UUID> teleported = new ArrayList<>();
        ReturnPendantService service = new ReturnPendantService(clock::get, CONFIG);
        service.onCooldownApplied(cooldownApplied::add);
        service.onTeleportRequested(teleported::add);

        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        clock.set(700L);
        ReturnPendantLogic.TickVerdict verdict = service.tickCast(
                player, Vec3.ZERO, 0, true);

        assertEquals(ReturnPendantLogic.TickOutcome.COMPLETE, verdict.outcome());
        assertEquals(List.of(player), cooldownApplied);
        assertEquals(List.of(player), teleported);
    }

    @Test
    void onCancelDoesNotInvokeCallbacks() {
        AtomicLong clock = new AtomicLong(100L);
        List<UUID> cooldownApplied = new ArrayList<>();
        List<UUID> teleported = new ArrayList<>();
        ReturnPendantService service = new ReturnPendantService(clock::get, CONFIG);
        service.onCooldownApplied(cooldownApplied::add);
        service.onTeleportRequested(teleported::add);

        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);

        service.tickCast(player, new Vec3(1, 0, 0), 0, true);

        assertTrue(cooldownApplied.isEmpty());
        assertTrue(teleported.isEmpty());
    }

    @Test
    void explicitCancelCastDoesNotInvokeCallbacks() {
        List<UUID> cooldownApplied = new ArrayList<>();
        List<UUID> teleported = new ArrayList<>();
        ReturnPendantService service = serviceWithClock(100L);
        service.onCooldownApplied(cooldownApplied::add);
        service.onTeleportRequested(teleported::add);

        UUID player = UUID.randomUUID();
        service.startCast(player, OVERWORLD, false, Vec3.ZERO, 0);
        service.cancelCast(player);

        assertTrue(cooldownApplied.isEmpty());
        assertTrue(teleported.isEmpty());
    }

    @Test
    void multiplePlayersCanCastIndependently() {
        ReturnPendantService service = serviceWithClock(100L);
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        service.startCast(player1, OVERWORLD, false, Vec3.ZERO, 0);
        service.startCast(player2, OVERWORLD, false, new Vec3(100, 64, 100), 3);

        assertTrue(service.isCasting(player1));
        assertTrue(service.isCasting(player2));

        service.cancelCast(player1);
        assertFalse(service.isCasting(player1));
        assertTrue(service.isCasting(player2));
    }
}
