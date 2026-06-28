package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;

import java.util.UUID;

public interface PacketSink {
    void sendSelf(UUID playerUuid, ClientboundReviveSnapshot packet);

    void sendTracking(UUID playerUuid, ClientboundTrackedKoVisual packet);

    void sendTo(UUID recipientUuid, ClientboundTrackedKoVisual packet);
}
