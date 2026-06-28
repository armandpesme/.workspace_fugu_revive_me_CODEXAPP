package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure helpers that describe how {@link com.fuguteams.fugureviveme.state.KoRecord}
 * instances are built and updated when a death is intercepted, when a hit
 * is applied to a knocked-out player, and when the action completes.
 * <p>
 * The functions are intentionally side-effect-free so they can be exercised
 * in unit tests without spinning up the Minecraft server. The orchestration
 * (publishing the snapshot, persisting to {@code KnockoutSavedData}, ...)
 * is performed by the caller ({@link ReviveService} for the persistent
 * transitions, dedicated services for revive interactions).
 */
public final class KnockoutStateLogic {
    public static final long FULLY_DOWNED_EXTENSION_TICKS = 6_000L;

    private KnockoutStateLogic() {
    }

    public record OnDeathInputs(
            ResourceLocation dimension,
            BlockPos position,
            BiomeKoClassifier.KoType biomeType,
            boolean hasResurrectionSickness,
            Optional<UUID> nearbyBoss,
            long currentGameTime
    ) {
        public OnDeathInputs {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(biomeType, "biomeType");
            Objects.requireNonNull(nearbyBoss, "nearbyBoss");
            if (currentGameTime < 0) {
                throw new IllegalArgumentException("currentGameTime must be non-negative");
            }
        }
    }

    public sealed interface OnDeathOutcome permits Created, Rejected {
    }

    public record Created(KoRecord record) implements OnDeathOutcome {
        public Created {
            Objects.requireNonNull(record, "record");
        }
    }

    public record Rejected(RejectionReason reason) implements OnDeathOutcome {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }

    public enum RejectionReason {
        RESURRECTION_SICKNESS,
        BIOME_NOT_ELIGIBLE
    }

    public static OnDeathOutcome computeEntry(OnDeathInputs inputs, int durationTicks, int maxHits) {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("durationTicks must be at least 1");
        }
        if (maxHits < 1) {
            throw new IllegalArgumentException("maxHits must be at least 1");
        }
        if (inputs.hasResurrectionSickness()) {
            return new Rejected(RejectionReason.RESURRECTION_SICKNESS);
        }
        ReviveState initialState;
        switch (inputs.biomeType()) {
            case TEMPORARY -> initialState = ReviveState.TEMPORARY_KO;
            case PROLONGED -> initialState = ReviveState.PROLONGED_KO;
            default -> {
                return new Rejected(RejectionReason.BIOME_NOT_ELIGIBLE);
            }
        }
        Optional<UUID> boss = initialState == ReviveState.PROLONGED_KO ? inputs.nearbyBoss() : Optional.empty();
        long deadline = inputs.currentGameTime() + durationTicks;
        KoRecord record = new KoRecord(
                initialState,
                deadline,
                0,
                inputs.dimension(),
                inputs.position(),
                boss);
        return new Created(record);
    }

    public sealed interface HitOutcome permits Survived, FullyDowned, HitRejected {
    }

    public record Survived(KoRecord nextRecord) implements HitOutcome {
        public Survived {
            Objects.requireNonNull(nextRecord, "nextRecord");
        }
    }

    public record FullyDowned(KoRecord nextRecord) implements HitOutcome {
        public FullyDowned {
            Objects.requireNonNull(nextRecord, "nextRecord");
        }
    }

    public record HitRejected() implements HitOutcome {
        public static final HitRejected INSTANCE = new HitRejected();
    }

    public static HitOutcome applyHit(KoRecord current, int maxHits) {
        Objects.requireNonNull(current, "current");
        if (maxHits < 1) {
            throw new IllegalArgumentException("maxHits must be at least 1");
        }
        if (current.state() != ReviveState.TEMPORARY_KO && current.state() != ReviveState.PROLONGED_KO) {
            return new HitRejected();
        }
        int nextHits = current.hitsTaken() + 1;
        if (nextHits >= maxHits) {
            long extensionDeadline = current.deadlineGameTime() + FULLY_DOWNED_EXTENSION_TICKS;
            KoRecord transitioned = current
                    .withHitsTaken(nextHits)
                    .transitionTo(ReviveState.FULLY_DOWNED)
                    .withDeadline(extensionDeadline);
            return new FullyDowned(transitioned);
        }
        return new Survived(current.withHitsTaken(nextHits));
    }
}
