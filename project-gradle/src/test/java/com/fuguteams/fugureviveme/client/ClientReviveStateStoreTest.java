package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
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
        assertEquals(0.75F, store.koProgress(225));
        assertEquals(ReviveState.TEMPORARY_KO, store.snapshot().state());
    }

    @Test
    void trackingAliveVisualClearsStoredVisual() {
        ClientReviveStateStore store = new ClientReviveStateStore();
        store.accept(new ClientboundTrackedKoVisual(42, ReviveState.TEMPORARY_KO, ReviveActionType.NONE));
        assertEquals(ReviveState.TEMPORARY_KO, store.trackedVisual(42).orElseThrow().state());

        store.accept(new ClientboundTrackedKoVisual(42, ReviveState.ALIVE, ReviveActionType.NONE));

        assertEquals(Optional.empty(), store.trackedVisual(42));
    }

    @Test
    void progressKeepsOriginalObservedDurationAcrossSameDeadlineUpdates() {
        ClientReviveStateStore store = new ClientReviveStateStore();
        store.accept(new ClientboundReviveSnapshot(
                ReviveState.PROLONGED_KO, 1_000, 7_000, 0,
                ReviveActionType.NONE, 0, Optional.empty(), false), 100);
        store.accept(new ClientboundReviveSnapshot(
                ReviveState.PROLONGED_KO, 1_500, 7_000, 1,
                ReviveActionType.NONE, 0, Optional.empty(), false), 600);

        assertEquals(0.75F, store.koProgress(1_600));
    }
}
