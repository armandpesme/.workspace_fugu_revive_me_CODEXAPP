package com.fuguteams.fugureviveme.network;

import com.fuguteams.fugureviveme.FuguReviveMe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Centralised network metadata for the Fugu Revive Me mod.
 * <p>
 * Exposes the protocol version and the ordered list of packet registrations
 * consumed by the Forge {@code SimpleChannel} setup.
 */
public final class FuguNetwork {

    public static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.parse(FuguReviveMe.MOD_ID + ":main");
    private static SimpleChannel channel;
    private static boolean registered;
    private static Consumer<ServerPlayer> releaseSpiritHandler = ignored -> {
    };

    private FuguNetwork() {
        throw new AssertionError("FuguNetwork is a static utility and must not be instantiated");
    }

    public static SimpleChannel channel() {
        if (channel == null) {
            channel = NetworkRegistry.newSimpleChannel(
                    CHANNEL_NAME,
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals);
        }
        return channel;
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        SimpleChannel activeChannel = channel();
        activeChannel.messageBuilder(ClientboundReviveSnapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundReviveSnapshot::encode)
                .decoder(ClientboundReviveSnapshot::decode)
                .consumerMainThread(FuguNetwork::handleSnapshot)
                .add();
        activeChannel.messageBuilder(ClientboundTrackedKoVisual.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundTrackedKoVisual::encode)
                .decoder(ClientboundTrackedKoVisual::decode)
                .consumerMainThread(FuguNetwork::handleTrackedVisual)
                .add();
        activeChannel.messageBuilder(ServerboundReleaseSpirit.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundReleaseSpirit::encode)
                .decoder(ServerboundReleaseSpirit::decode)
                .consumerMainThread(FuguNetwork::handleReleaseSpirit)
                .add();
        registered = true;
    }

    public static void setReleaseSpiritHandler(Consumer<ServerPlayer> nextReleaseSpiritHandler) {
        releaseSpiritHandler = Objects.requireNonNull(nextReleaseSpiritHandler, "nextReleaseSpiritHandler");
    }

    /**
     * Immutable description of a single packet bound to a {@link NetworkDirection}.
     *
     * @param id          stable wire identifier (must be non-negative)
     * @param packetClass payload class transported on the wire
     * @param direction   logical flow of the packet (clientbound / serverbound)
     */
    public record PacketRegistration(int id, Class<?> packetClass, NetworkDirection direction) {

        public PacketRegistration {
            if (id < 0) {
                throw new IllegalArgumentException("id must be non-negative, got " + id);
            }
            if (packetClass == null) {
                throw new IllegalArgumentException("packetClass must not be null");
            }
            if (direction == null) {
                throw new IllegalArgumentException("direction must not be null");
            }
        }
    }

    /**
     * Returns the ordered list of packet registrations used to bootstrap the
     * network channel. The order is significant: it defines the wire ids
     * shared with the client and must stay stable across releases.
     */
    public static List<PacketRegistration> registrations() {
        return List.of(
                new PacketRegistration(0, ClientboundReviveSnapshot.class, NetworkDirection.PLAY_TO_CLIENT),
                new PacketRegistration(1, ClientboundTrackedKoVisual.class, NetworkDirection.PLAY_TO_CLIENT),
                new PacketRegistration(2, ServerboundReleaseSpirit.class, NetworkDirection.PLAY_TO_SERVER)
        );
    }

    private static void handleSnapshot(
            ClientboundReviveSnapshot packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> invokeClientHandler("handleSnapshot", ClientboundReviveSnapshot.class, packet)));
        context.setPacketHandled(true);
    }

    private static void handleTrackedVisual(
            ClientboundTrackedKoVisual packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> invokeClientHandler("handleTrackedVisual", ClientboundTrackedKoVisual.class, packet)));
        context.setPacketHandled(true);
    }

    private static void handleReleaseSpirit(
            ServerboundReleaseSpirit ignored,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                releaseSpiritHandler.accept(sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void invokeClientHandler(String methodName, Class<?> parameterType, Object packet) {
        try {
            Class<?> handlerClass = Class.forName("com.fuguteams.fugureviveme.client.ClientPacketHandlers");
            handlerClass.getMethod(methodName, parameterType).invoke(null, packet);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to dispatch client packet handler " + methodName, exception);
        }
    }
}
