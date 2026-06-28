package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientReviveStateStoreTest {
    @Test
    void interpolationUsesLocalElapsedTicksWithoutMutatingSnapshot() {
        ClientReviveStateStore store = new ClientReviveStateStore();
        ClientboundReviveSnapshot snapshot = new ClientboundReviveSnapshot(
                ReviveState.TEMPORARY_KO, 1_000, 1_100, 0,
                ReviveActionType.NONE, 0, Optional.empty(), false);
        store.accept(snapshot, 200);

        assertEquals(100, store.remainingKoTicks(200));
        assertEquals(75, store.remainingKoTicks(225));
        assertEquals(ReviveState.TEMPORARY_KO, store.snapshot().state());
    }
}
