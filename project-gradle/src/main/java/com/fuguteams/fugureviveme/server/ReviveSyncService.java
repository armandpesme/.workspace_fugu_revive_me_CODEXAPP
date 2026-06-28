package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Optional;
import java.util.UUID;

public final class ReviveSyncService {
    private final PacketSink sink;

    public ReviveSyncService(PacketSink sink) {
        this.sink = sink;
    }

    public void publishTransition(UUID playerUuid, KoRecord record, long serverGameTime) {
        sink.sendSelf(playerUuid, snapshot(record, serverGameTime));
    }

    public void publishTransition(UUID playerUuid, int entityId, KoRecord record, long serverGameTime) {
        sink.sendSelf(playerUuid, snapshot(record, serverGameTime));
        sink.sendTracking(playerUuid, visual(entityId, record.state()));
    }

    public void sendSelf(UUID playerUuid, Optional<KoRecord> record, long serverGameTime) {
        sink.sendSelf(playerUuid, record
                .map(value -> snapshot(value, serverGameTime))
                .orElseGet(() -> aliveSnapshot(serverGameTime)));
    }

    public void sendVisualTo(UUID recipientUuid, int entityId, ReviveState state) {
        sink.sendTo(recipientUuid, visual(entityId, state));
    }

    private static ClientboundReviveSnapshot snapshot(KoRecord record, long serverGameTime) {
        return new ClientboundReviveSnapshot(
                record.state(),
                serverGameTime,
                record.deadlineGameTime(),
                record.hitsTaken(),
                ReviveActionType.NONE,
                0,
                record.linkedBossUuid(),
                false);
    }

    private static ClientboundReviveSnapshot aliveSnapshot(long serverGameTime) {
        return new ClientboundReviveSnapshot(
                ReviveState.ALIVE,
                serverGameTime,
                0,
                0,
                ReviveActionType.NONE,
                0,
                Optional.empty(),
                false);
    }

    private static ClientboundTrackedKoVisual visual(int entityId, ReviveState state) {
        return new ClientboundTrackedKoVisual(entityId, state, ReviveActionType.NONE);
    }
}
