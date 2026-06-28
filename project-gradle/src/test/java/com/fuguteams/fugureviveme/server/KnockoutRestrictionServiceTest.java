package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.ReviveState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutRestrictionServiceTest {

    @Test
    void isRestrictedForTemporaryAndProlongedAndFullyDowned() {
        KnockoutRestrictionService service = new KnockoutRestrictionService();
        assertTrue(service.isRestricted(ReviveState.TEMPORARY_KO));
        assertTrue(service.isRestricted(ReviveState.PROLONGED_KO));
        assertTrue(service.isRestricted(ReviveState.FULLY_DOWNED));
        assertFalse(service.isRestricted(ReviveState.PENDING_REVIVE));
        assertFalse(service.isRestricted(ReviveState.PENDING_DEATH));
        assertFalse(service.isRestricted(ReviveState.DEAD_PENDING_TRANSFER));
        assertFalse(service.isRestricted(ReviveState.ALIVE));
        assertFalse(service.isRestricted(null));
    }

    @Test
    void overrideSpeedAppliesInAllRestrictedStates() {
        KnockoutRestrictionService service = new KnockoutRestrictionService();
        assertEquals(KnockoutRestrictionService.KO_WALK_SPEED,
                service.overrideSpeed(ReviveState.TEMPORARY_KO).orElseThrow());
        assertEquals(KnockoutRestrictionService.KO_WALK_SPEED,
                service.overrideSpeed(ReviveState.PROLONGED_KO).orElseThrow());
        assertEquals(KnockoutRestrictionService.KO_WALK_SPEED,
                service.overrideSpeed(ReviveState.FULLY_DOWNED).orElseThrow());
        assertTrue(service.overrideSpeed(ReviveState.ALIVE).isEmpty());
    }

    @Test
    void inputRestrictionsMatchState() {
        KnockoutRestrictionService service = new KnockoutRestrictionService();
        assertFalse(service.allowAttack(ReviveState.TEMPORARY_KO));
        assertFalse(service.allowItemUse(ReviveState.TEMPORARY_KO));
        assertFalse(service.allowBlockInteraction(ReviveState.TEMPORARY_KO));
        assertFalse(service.allowInventory(ReviveState.TEMPORARY_KO));
        assertFalse(service.allowTeleport(ReviveState.TEMPORARY_KO));
        assertFalse(service.allowAttack(ReviveState.PROLONGED_KO));
        assertFalse(service.allowItemUse(ReviveState.PROLONGED_KO));
        assertFalse(service.allowBlockInteraction(ReviveState.PROLONGED_KO));
        assertFalse(service.allowInventory(ReviveState.PROLONGED_KO));
        assertFalse(service.allowTeleport(ReviveState.PROLONGED_KO));
        assertFalse(service.allowAttack(ReviveState.FULLY_DOWNED));
        assertFalse(service.allowItemUse(ReviveState.FULLY_DOWNED));
        assertFalse(service.allowBlockInteraction(ReviveState.FULLY_DOWNED));
        assertFalse(service.allowInventory(ReviveState.FULLY_DOWNED));
        assertFalse(service.allowTeleport(ReviveState.FULLY_DOWNED));

        assertTrue(service.allowAttack(ReviveState.ALIVE));
        assertTrue(service.allowItemUse(ReviveState.ALIVE));
        assertTrue(service.allowBlockInteraction(ReviveState.ALIVE));
        assertTrue(service.allowInventory(ReviveState.ALIVE));
        assertTrue(service.allowTeleport(ReviveState.ALIVE));
    }

    @Test
    void chatAndCameraAlwaysAllowed() {
        KnockoutRestrictionService service = new KnockoutRestrictionService();
        for (ReviveState state : ReviveState.values()) {
            assertTrue(service.allowChat(state), "chat should be allowed in " + state);
            assertTrue(service.allowCamera(state), "camera should be allowed in " + state);
        }
    }
}
