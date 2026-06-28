package com.fuguteams.fugureviveme.state;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoActionTest {

    @Test
    void allyReviveRequiresHelperUuidAndCapturesStartPositions() {
        UUID target = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        BlockPos helperPos = new BlockPos(1, 64, 1);
        BlockPos targetPos = new BlockPos(2, 64, 2);

        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                200L, helperPos.immutable(), targetPos.immutable(), 0);

        assertEquals(target, action.targetUuid());
        assertEquals(Optional.of(helper), action.helperUuid());
        assertEquals(ReviveActionType.ALLY_REVIVE, action.type());
        assertEquals(helperPos, action.helperStartPosition());
        assertEquals(targetPos, action.targetStartPosition());
    }

    @Test
    void soulAnchorSelfReviveForbidsHelperUuid() {
        UUID target = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        BlockPos targetPos = new BlockPos(3, 64, 3);

        assertThrows(IllegalArgumentException.class, () -> new KoAction(
                target, Optional.of(helper), ReviveActionType.SELF_REVIVE,
                100L, targetPos.immutable(), targetPos.immutable(), 0));
    }

    @Test
    void allyReviveForbidsMissingHelperUuid() {
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);

        assertThrows(IllegalArgumentException.class, () -> new KoAction(
                target, Optional.empty(), ReviveActionType.ALLY_REVIVE,
                100L, pos.immutable(), pos.immutable(), 0));
    }

    @Test
    void returnPendantIsTrackedSeparately() {
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);

        assertThrows(IllegalArgumentException.class, () -> new KoAction(
                target, Optional.empty(), ReviveActionType.RETURN_PENDANT,
                100L, pos.immutable(), pos.immutable(), 0));
    }

    @Test
    void noneActionTypeIsRejected() {
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);

        assertThrows(IllegalArgumentException.class, () -> new KoAction(
                target, Optional.empty(), ReviveActionType.NONE,
                100L, pos.immutable(), pos.immutable(), 0));
    }

    @Test
    void isExpiredComparesDeadlineAgainstClock() {
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, pos.immutable(), pos.immutable(), 0);

        assertFalse(action.isExpired(99L));
        assertTrue(action.isExpired(100L));
        assertTrue(action.isExpired(200L));
    }

    @Test
    void involvesHelperMatchesOnlyRecordedHelper() {
        UUID target = UUID.randomUUID();
        UUID helper = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, pos.immutable(), pos.immutable(), 0);

        assertTrue(action.involvesHelper(helper));
        assertFalse(action.involvesHelper(target));
        assertFalse(action.involvesHelper(UUID.randomUUID()));
    }
}
