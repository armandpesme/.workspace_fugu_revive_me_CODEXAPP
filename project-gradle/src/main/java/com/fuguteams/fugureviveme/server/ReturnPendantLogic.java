package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReturnPendantLogic {

    private ReturnPendantLogic() {
    }

    public enum StartRejection {
        ON_COOLDOWN,
        WRONG_DIMENSION,
        ALREADY_CASTING
    }

    public sealed interface StartVerdict permits StartAllowed, StartRejected {
    }

    public record StartAllowed(ReturnPendantCast cast) implements StartVerdict {
        public StartAllowed {
            Objects.requireNonNull(cast, "cast");
        }
    }

    public record StartRejected(StartRejection reason) implements StartVerdict {
        public StartRejected {
            Objects.requireNonNull(reason, "reason");
        }
    }

    public static StartVerdict evaluateStart(
            UUID playerUuid,
            ResourceLocation currentDimension,
            ResourceLocation mainDimension,
            boolean onCooldown,
            Vec3 startPosition,
            int startSlot,
            long now,
            int castTimeTicks
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(currentDimension, "currentDimension");
        Objects.requireNonNull(mainDimension, "mainDimension");
        Objects.requireNonNull(startPosition, "startPosition");
        if (onCooldown) {
            return new StartRejected(StartRejection.ON_COOLDOWN);
        }
        if (!currentDimension.equals(mainDimension)) {
            return new StartRejected(StartRejection.WRONG_DIMENSION);
        }
        ReturnPendantCast cast = new ReturnPendantCast(
                playerUuid, startPosition, startSlot, now + castTimeTicks);
        return new StartAllowed(cast);
    }

    public enum CancelReason {
        MOVED,
        SLOT_CHANGED,
        ITEM_CHANGED
    }

    public enum TickOutcome {
        CONTINUE,
        COMPLETE,
        CANCEL,
        NOT_CASTING
    }

    public record TickVerdict(TickOutcome outcome, Optional<CancelReason> reason) {
        public TickVerdict {
            Objects.requireNonNull(outcome, "outcome");
            reason = Objects.requireNonNull(reason, "reason");
        }

        public static TickVerdict continueRun() {
            return new TickVerdict(TickOutcome.CONTINUE, Optional.empty());
        }

        public static TickVerdict complete() {
            return new TickVerdict(TickOutcome.COMPLETE, Optional.empty());
        }

        public static TickVerdict cancel(CancelReason cancelReason) {
            return new TickVerdict(TickOutcome.CANCEL, Optional.of(cancelReason));
        }

        public static TickVerdict notCasting() {
            return new TickVerdict(TickOutcome.NOT_CASTING, Optional.empty());
        }
    }

    public static TickVerdict evaluateTick(
            ReturnPendantCast cast,
            Vec3 currentPosition,
            int currentSlot,
            boolean itemMatches,
            long now,
            double movementTolerance
    ) {
        Objects.requireNonNull(cast, "cast");
        Objects.requireNonNull(currentPosition, "currentPosition");
        if (cast.startPosition().distanceTo(currentPosition) > movementTolerance) {
            return TickVerdict.cancel(CancelReason.MOVED);
        }
        if (currentSlot != cast.startSlot()) {
            return TickVerdict.cancel(CancelReason.SLOT_CHANGED);
        }
        if (!itemMatches) {
            return TickVerdict.cancel(CancelReason.ITEM_CHANGED);
        }
        if (now >= cast.deadline()) {
            return TickVerdict.complete();
        }
        return TickVerdict.continueRun();
    }
}
