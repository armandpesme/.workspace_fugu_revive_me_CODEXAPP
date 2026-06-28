package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.ReviveState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutRespawnServiceTest {

    @Test
    void shouldTransferOnlyForPendingOrDeadStates() {
        KnockoutRespawnService service = new KnockoutRespawnService();
        assertTrue(service.shouldTransfer(ReviveState.PENDING_DEATH));
        assertTrue(service.shouldTransfer(ReviveState.PENDING_REVIVE));
        assertTrue(service.shouldTransfer(ReviveState.DEAD_PENDING_TRANSFER));
        assertFalse(service.shouldTransfer(ReviveState.ALIVE));
        assertFalse(service.shouldTransfer(ReviveState.TEMPORARY_KO));
        assertFalse(service.shouldTransfer(ReviveState.PROLONGED_KO));
        assertFalse(service.shouldTransfer(ReviveState.FULLY_DOWNED));
    }

    @Test
    void shouldTransferRejectsNullState() {
        KnockoutRespawnService service = new KnockoutRespawnService();
        assertFalse(service.shouldTransfer(null));
    }
}
