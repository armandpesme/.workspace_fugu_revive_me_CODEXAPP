package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure functions describing the Jalon 4 prolonged knockout rules.
 * <p>
 * The class is intentionally side-effect-free so the behaviour can be
 * exercised in unit tests without spinning up a Minecraft server. The
 * orchestrating service is responsible for applying the verdict and
 * publishing the corresponding snapshot.
 */
public final class ProlongedKoLogic {

    private ProlongedKoLogic() {
        throw new AssertionError("ProlongedKoLogic is a static utility and must not be instantiated");
    }

    public sealed interface InitialState permits ProlongedChoice, TemporaryChoice, RejectChoice {
    }

    public enum ProlongedChoice implements InitialState {
        INSTANCE
    }

    public enum TemporaryChoice implements InitialState {
        INSTANCE
    }

    public enum RejectChoice implements InitialState {
        INSTANCE
    }

    public static InitialState selectInitialState(
            BiomeKoClassifier.KoType classification,
            boolean bossFound,
            long tempDurationTicks,
            long prolongedDurationTicks
    ) {
        Objects.requireNonNull(classification, "classification");
        if (tempDurationTicks < 1) {
            throw new IllegalArgumentException("tempDurationTicks must be >= 1");
        }
        if (prolongedDurationTicks < 1) {
            throw new IllegalArgumentException("prolongedDurationTicks must be >= 1");
        }
        return switch (classification) {
            case NONE -> RejectChoice.INSTANCE;
            case PROLONGED -> bossFound ? ProlongedChoice.INSTANCE : TemporaryChoice.INSTANCE;
            case TEMPORARY -> TemporaryChoice.INSTANCE;
        };
    }

    public static Optional<KoRecord> transitionOnBossDeath(KoRecord current, long now) {
        Objects.requireNonNull(current, "current");
        if (now < 0) {
            throw new IllegalArgumentException("now must be non-negative");
        }
        if (current.state() != ReviveState.PROLONGED_KO) {
            return Optional.empty();
        }
        long shortDeadline = now + 1L;
        KoRecord next = current
                .transitionTo(ReviveState.PENDING_REVIVE)
                .withDeadline(shortDeadline);
        return Optional.of(next);
    }

    public static Optional<KoRecord> transitionOnBossDespawn(KoRecord current) {
        Objects.requireNonNull(current, "current");
        if (current.state() != ReviveState.PROLONGED_KO
                && current.state() != ReviveState.FULLY_DOWNED) {
            return Optional.empty();
        }
        return Optional.of(current.transitionTo(ReviveState.PENDING_DEATH));
    }

    public static Optional<KoRecord> transitionOnDimensionChange(KoRecord current) {
        Objects.requireNonNull(current, "current");
        if (current.state() != ReviveState.PROLONGED_KO && current.state() != ReviveState.FULLY_DOWNED) {
            return Optional.empty();
        }
        return Optional.of(current.transitionTo(ReviveState.PENDING_DEATH));
    }

    public static Optional<KoRecord> transitionOnTimeout(KoRecord current, long now) {
        Objects.requireNonNull(current, "current");
        if (now < 0) {
            throw new IllegalArgumentException("now must be non-negative");
        }
        if (now < current.deadlineGameTime()) {
            return Optional.empty();
        }
        if (current.state() != ReviveState.TEMPORARY_KO
                && current.state() != ReviveState.PROLONGED_KO
                && current.state() != ReviveState.FULLY_DOWNED) {
            return Optional.empty();
        }
        return Optional.of(current.transitionTo(ReviveState.PENDING_DEATH));
    }
}
