package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side decision engine for restrictions applied to a knocked-out
 * player. The {@link com.fuguteams.fugureviveme.server.KoEventHandlers}
 * class subscribes to events and consults this service to decide whether
 * the event should be cancelled.
 * <p>
 * The service is intentionally stateless: it is a pure function of the
 * current {@link ReviveState} and the player's UUID.
 */
public final class KnockoutRestrictionService {
    public static final float KO_WALK_SPEED = 0.05F;

    /**
     * The set of states during which player input is restricted. The set
     * is intentionally narrow: only {@link ReviveState#TEMPORARY_KO} is
     * restricted. The prolonged KO flow is part of milestone 4 and keeps
     * its own dedicated restrictions there.
     */
    public static final Set<ReviveState> RESTRICTED_STATES = Set.of(ReviveState.TEMPORARY_KO);

    public KnockoutRestrictionService() {
    }

    public boolean isRestricted(ReviveState state) {
        return state != null && RESTRICTED_STATES.contains(state);
    }

    public Optional<Float> overrideSpeed(ReviveState state) {
        if (isRestricted(state)) {
            return Optional.of(KO_WALK_SPEED);
        }
        return Optional.empty();
    }

    public boolean allowAttack(ReviveState state) {
        return !isRestricted(state);
    }

    public boolean allowItemUse(ReviveState state) {
        return !isRestricted(state);
    }

    public boolean allowBlockInteraction(ReviveState state) {
        return !isRestricted(state);
    }

    public boolean allowInventory(ReviveState state) {
        return !isRestricted(state);
    }

    public boolean allowTeleport(ReviveState state) {
        return !isRestricted(state);
    }

    public boolean allowChat(ReviveState state) {
        // Chat is always allowed even in KO.
        return true;
    }

    public boolean allowCamera(ReviveState state) {
        return true;
    }
}
