package com.fuguteams.fugureviveme.network;

import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ClientboundReviveSnapshot(
        ReviveState state,
        long serverGameTime,
        long deadlineGameTime,
        int hitsTaken,
        ReviveActionType actionType,
        long actionDeadlineGameTime,
        Optional<UUID> linkedBossUuid,
        boolean bossAvailable
) {
    private static final int MAX_HITS = 1_000_000;

    public ClientboundReviveSnapshot {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(actionType, "actionType");
        linkedBossUuid = Objects.requireNonNull(linkedBossUuid, "linkedBossUuid");
        if (serverGameTime < 0 || deadlineGameTime < 0 || actionDeadlineGameTime < 0) {
            throw new IllegalArgumentException("Packet times must be non-negative");
        }
        if (hitsTaken < 0 || hitsTaken > MAX_HITS) {
            throw new IllegalArgumentException("hitsTaken outside packet bounds");
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(state.ordinal());
        buffer.writeLong(serverGameTime);
        buffer.writeLong(deadlineGameTime);
        buffer.writeVarInt(hitsTaken);
        buffer.writeVarInt(actionType.ordinal());
        buffer.writeLong(actionDeadlineGameTime);
        buffer.writeBoolean(linkedBossUuid.isPresent());
        linkedBossUuid.ifPresent(buffer::writeUUID);
        buffer.writeBoolean(bossAvailable);
    }

    public static ClientboundReviveSnapshot decode(FriendlyByteBuf buffer) {
        ReviveState state = ReviveState.fromWire(buffer.readVarInt());
        long serverGameTime = buffer.readLong();
        long deadlineGameTime = buffer.readLong();
        int hitsTaken = buffer.readVarInt();
        ReviveActionType actionType = ReviveActionType.fromWire(buffer.readVarInt());
        long actionDeadlineGameTime = buffer.readLong();
        Optional<UUID> boss = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        boolean bossAvailable = buffer.readBoolean();
        return new ClientboundReviveSnapshot(state, serverGameTime, deadlineGameTime, hitsTaken,
                actionType, actionDeadlineGameTime, boss, bossAvailable);
    }
}
