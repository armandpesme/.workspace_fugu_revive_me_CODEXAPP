package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.network.ClientboundReviveSnapshot;
import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.network.FuguNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ForgePacketSink implements PacketSink {
    private final MinecraftServer server;

    public ForgePacketSink(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void sendSelf(UUID playerUuid, ClientboundReviveSnapshot packet) {
        player(playerUuid).ifPresent(player ->
                FuguNetwork.channel().send(PacketDistributor.PLAYER.with(() -> player), packet));
    }

    @Override
    public void sendTracking(UUID playerUuid, ClientboundTrackedKoVisual packet) {
        player(playerUuid).ifPresent(player ->
                FuguNetwork.channel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet));
    }

    @Override
    public void sendTo(UUID recipientUuid, ClientboundTrackedKoVisual packet) {
        player(recipientUuid).ifPresent(player ->
                FuguNetwork.channel().send(PacketDistributor.PLAYER.with(() -> player), packet));
    }

    private Optional<ServerPlayer> player(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return Optional.ofNullable(server.getPlayerList().getPlayer(playerUuid));
    }
}
