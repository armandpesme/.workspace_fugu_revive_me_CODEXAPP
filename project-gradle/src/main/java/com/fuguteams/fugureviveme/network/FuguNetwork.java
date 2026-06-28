package com.fuguteams.fugureviveme.network;

import net.minecraftforge.network.NetworkDirection;

import java.util.List;

/**
 * Centralised network metadata for the Fugu Revive Me mod.
 * <p>
 * Exposes the protocol version and the ordered list of packet registrations
 * consumed by the Forge {@code SimpleChannel} setup.
 */
public final class FuguNetwork {

    public static final String PROTOCOL_VERSION = "1";

    private FuguNetwork() {
        throw new AssertionError("FuguNetwork is a static utility and must not be instantiated");
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
}
