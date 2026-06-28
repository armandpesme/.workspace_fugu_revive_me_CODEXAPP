package com.fuguteams.fugureviveme.network;

import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

public record ClientboundTrackedKoVisual(
        int entityId,
        ReviveState state,
        ReviveActionType actionType
) {
    public ClientboundTrackedKoVisual {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(actionType, "actionType");
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId must be non-negative");
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(state.ordinal());
        buffer.writeVarInt(actionType.ordinal());
    }

    public static ClientboundTrackedKoVisual decode(FriendlyByteBuf buffer) {
        return new ClientboundTrackedKoVisual(
                buffer.readVarInt(),
                ReviveState.fromWire(buffer.readVarInt()),
                ReviveActionType.fromWire(buffer.readVarInt()));
    }
}
