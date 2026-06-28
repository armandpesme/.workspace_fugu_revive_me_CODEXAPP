package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutActionRegistryTest {

    @Test
    void startsAndRetrievesActionByTarget() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        UUID target = UUID.randomUUID();
        KoAction action = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0), 0);

        assertTrue(registry.start(action));
        assertEquals(1, registry.size());
        assertEquals(action, registry.get(target).orElseThrow());
    }

    @Test
    void duplicateTargetIsRefused() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        KoAction first = new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                100L, pos, pos, 0);

        assertTrue(registry.start(first));
        assertFalse(registry.start(new KoAction(
                target, Optional.empty(), ReviveActionType.SELF_REVIVE,
                200L, pos, pos, 0)));
        assertEquals(1, registry.size());
    }

    @Test
    void helperIndexIsMaintainedAndCancelled() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        KoAction action = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, pos, pos, 0);

        registry.start(action);
        KoAction found = registry.findActiveForHelper(helper).orElseThrow();
        assertEquals(action.targetUuid(), found.targetUuid());

        assertTrue(registry.cancelHelper(helper));
        assertEquals(0, registry.size());
        assertTrue(registry.findActiveForHelper(helper).isEmpty());
        assertTrue(registry.get(target).isEmpty());
    }

    @Test
    void cancellingUnknownTargetsIsNoop() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        assertFalse(registry.cancelTarget(UUID.randomUUID()));
        assertFalse(registry.cancelHelper(UUID.randomUUID()));
    }

    @Test
    void startRejectsReturnPendantAndNone() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        assertThrows(IllegalArgumentException.class, () -> registry.start(new KoAction(
                target, Optional.empty(), ReviveActionType.RETURN_PENDANT,
                100L, pos, pos, 0)));
        assertThrows(IllegalArgumentException.class, () -> registry.start(new KoAction(
                target, Optional.empty(), ReviveActionType.NONE,
                100L, pos, pos, 0)));
    }

    @Test
    void multipleHelpersTargetSamePlayer() {
        KnockoutActionRegistry registry = new KnockoutActionRegistry();
        UUID helper = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(0, 64, 0);
        KoAction first = new KoAction(
                target, Optional.of(helper), ReviveActionType.ALLY_REVIVE,
                100L, pos, pos, 0);

        assertTrue(registry.start(first));
        KoAction resolved = registry.findActiveForHelper(helper).orElseThrow();
        assertSame(first, resolved);
    }
}
