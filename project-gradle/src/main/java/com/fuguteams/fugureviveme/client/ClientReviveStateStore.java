package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientReviveStateStore {
    private static final ClientReviveStateStore INSTANCE = new ClientReviveStateStore();
    private static final ClientboundReviveSnapshot ALIVE = new ClientboundReviveSnapshot(
            ReviveState.ALIVE, 0, 0, 0, ReviveActionType.NONE, 0, Optional.empty(), false);

    private final Map<Integer, ClientboundTrackedKoVisual> trackedVisuals = new HashMap<>();
    private ClientboundReviveSnapshot snapshot = ALIVE;
    private long receivedAtClientGameTime;

    public static ClientReviveStateStore get() {
        return INSTANCE;
    }

    public void accept(ClientboundReviveSnapshot nextSnapshot, long clientGameTime) {
        snapshot = nextSnapshot;
        receivedAtClientGameTime = clientGameTime;
    }

    public void accept(ClientboundTrackedKoVisual visual) {
        trackedVisuals.put(visual.entityId(), visual);
    }

    public ClientboundReviveSnapshot snapshot() {
        return snapshot;
    }

    public Optional<ClientboundTrackedKoVisual> trackedVisual(int entityId) {
        return Optional.ofNullable(trackedVisuals.get(entityId));
    }

    public long estimatedServerGameTime(long clientGameTime) {
        return snapshot.serverGameTime() + Math.max(0, clientGameTime - receivedAtClientGameTime);
    }

    public long remainingKoTicks(long clientGameTime) {
        return Math.max(0, snapshot.deadlineGameTime() - estimatedServerGameTime(clientGameTime));
    }

    public long remainingActionTicks(long clientGameTime) {
        return Math.max(0, snapshot.actionDeadlineGameTime() - estimatedServerGameTime(clientGameTime));
    }
}
