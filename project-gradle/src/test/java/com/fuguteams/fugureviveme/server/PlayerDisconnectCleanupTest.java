package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisconnectCleanupTest {

    @Test
    void disconnectCleansAllTransientEntriesForTheLoggedOutPlayer() {
        UUID loggedOut = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        UUID otherTarget = UUID.randomUUID();
        UUID otherHelper = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);

        KnockoutActionRegistry actions = new KnockoutActionRegistry();
        actions.start(new KoAction(
                loggedOut, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, pos, pos, 0));
        actions.start(new KoAction(
                otherTarget, Optional.of(otherHelper), ReviveActionType.ALLY_REVIVE,
                100L, pos, pos, 0));

        MovementOverrideRegistry movement = new MovementOverrideRegistry();
        movement.remember(loggedOut, 0.1157);
        movement.remember(otherTarget, 0.1157);

        LastSafePositionTracker tracker = new LastSafePositionTracker();
        tracker.record(loggedOut, new BlockPos(1, 64, 1));
        tracker.record(otherTarget, new BlockPos(2, 64, 2));

        KnockoutDamageTracker damage = new KnockoutDamageTracker();
        damage.flag(loggedOut, 100L);
        damage.flag(otherTarget, 200L);

        KoEventHandlers.clearTransientPlayerState(actions, movement, tracker, damage, loggedOut);

        assertFalse(actions.get(loggedOut).isPresent());
        assertFalse(actions.get(helper).isPresent());
        assertFalse(movement.isRemembered(loggedOut));
        assertFalse(tracker.get(loggedOut).isPresent());
        assertFalse(damage.isFlagged(loggedOut));

        assertTrue(actions.get(otherTarget).isPresent());
        assertEquals(0.1157, movement.forget(otherTarget).orElseThrow(), 0.0001);
        assertEquals(new BlockPos(2, 64, 2), tracker.get(otherTarget).orElseThrow());
        assertTrue(damage.isFlagged(otherTarget));
    }

    @Test
    void disconnectTwiceIsIdempotent() {
        UUID player = UUID.randomUUID();
        KnockoutActionRegistry actions = new KnockoutActionRegistry();
        MovementOverrideRegistry movement = new MovementOverrideRegistry();
        LastSafePositionTracker tracker = new LastSafePositionTracker();
        KnockoutDamageTracker damage = new KnockoutDamageTracker();
        movement.remember(player, 0.20);
        damage.flag(player, 100L);

        KoEventHandlers.clearTransientPlayerState(actions, movement, tracker, damage, player);
        KoEventHandlers.clearTransientPlayerState(actions, movement, tracker, damage, player);

        assertFalse(movement.isRemembered(player));
        assertFalse(damage.isFlagged(player));
    }

    @Test
    void disconnectIgnoresPlayerWithNoTransientState() {
        UUID player = UUID.randomUUID();
        KnockoutActionRegistry actions = new KnockoutActionRegistry();
        MovementOverrideRegistry movement = new MovementOverrideRegistry();
        LastSafePositionTracker tracker = new LastSafePositionTracker();
        KnockoutDamageTracker damage = new KnockoutDamageTracker();

        KoEventHandlers.clearTransientPlayerState(actions, movement, tracker, damage, player);

        assertEquals(0, actions.size());
        assertFalse(movement.isRemembered(player));
    }
}
