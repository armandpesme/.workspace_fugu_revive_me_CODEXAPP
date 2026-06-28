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
    private long observedKoDurationTicks;
    private long observedActionDurationTicks;

    public static ClientReviveStateStore get() {
        return INSTANCE;
    }

    public void accept(ClientboundReviveSnapshot nextSnapshot, long clientGameTime) {
        long remainingKoTicks = Math.max(0L, nextSnapshot.deadlineGameTime() - nextSnapshot.serverGameTime());
        if (nextSnapshot.state() != snapshot.state()
                || nextSnapshot.deadlineGameTime() != snapshot.deadlineGameTime()
                || observedKoDurationTicks <= 0L) {
            observedKoDurationTicks = remainingKoTicks;
        } else {
            observedKoDurationTicks = Math.max(observedKoDurationTicks, remainingKoTicks);
        }
        long remainingActionTicks = Math.max(0L, nextSnapshot.actionDeadlineGameTime() - nextSnapshot.serverGameTime());
        if (nextSnapshot.actionType() != snapshot.actionType()
                || nextSnapshot.actionDeadlineGameTime() != snapshot.actionDeadlineGameTime()) {
            observedActionDurationTicks = remainingActionTicks;
        } else {
            observedActionDurationTicks = Math.max(observedActionDurationTicks, remainingActionTicks);
        }
        snapshot = nextSnapshot;
        receivedAtClientGameTime = clientGameTime;
    }

    public void accept(ClientboundTrackedKoVisual visual) {
        if (visual.state() == ReviveState.ALIVE) {
            trackedVisuals.remove(visual.entityId());
            return;
        }
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

    public float koProgress(long clientGameTime) {
        return ClientUiFormat.progress(remainingKoTicks(clientGameTime), observedKoDurationTicks);
    }

    public float actionProgress(long clientGameTime) {
        return ClientUiFormat.progress(remainingActionTicks(clientGameTime), observedActionDurationTicks);
    }
}
