package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure functions describing the ally revive interaction. They consume
 * pre-built {@link KnockoutPlayerSnapshot} values and return a verdict
 * (allowed, denied, completed, cancelled) without touching the live
 * server state. The orchestrating service is responsible for applying
 * the verdict.
 */
public final class AllyReviveLogic {

    private AllyReviveLogic() {
    }

    public enum StartDenial {
        TARGET_NOT_IN_TEMPORARY_KO,
        TARGET_IN_PROLONGED_KO,
        ALREADY_ACTIVE,
        HELPER_DISTANCE_OUT_OF_RANGE,
        HELPER_SAME_AS_TARGET
    }

    public sealed interface StartOutcome permits StartAllowed, StartDenied {
    }

    public record StartAllowed(KoAction action) implements StartOutcome {
        public StartAllowed {
            Objects.requireNonNull(action, "action");
        }
    }

    public record StartDenied(StartDenial reason) implements StartOutcome {
        public StartDenied {
            Objects.requireNonNull(reason, "reason");
        }
    }

    public static StartOutcome evaluateStart(
            KnockoutPlayerSnapshot helper,
            KnockoutPlayerSnapshot target,
            boolean actionAlreadyActive,
            long now,
            int durationTicks,
            double maxDistance
    ) {
        Objects.requireNonNull(helper, "helper");
        Objects.requireNonNull(target, "target");
        if (helper.playerUuid().equals(target.playerUuid())) {
            return new StartDenied(StartDenial.HELPER_SAME_AS_TARGET);
        }
        if (target.state() == ReviveState.PROLONGED_KO || target.state() == ReviveState.FULLY_DOWNED) {
            return new StartDenied(StartDenial.TARGET_IN_PROLONGED_KO);
        }
        if (target.state() != ReviveState.TEMPORARY_KO) {
            return new StartDenied(StartDenial.TARGET_NOT_IN_TEMPORARY_KO);
        }
        if (actionAlreadyActive) {
            return new StartDenied(StartDenial.ALREADY_ACTIVE);
        }
        double distance = helper.position().distSqr(target.position());
        if (distance > maxDistance * maxDistance) {
            return new StartDenied(StartDenial.HELPER_DISTANCE_OUT_OF_RANGE);
        }
        KoAction action = new KoAction(
                target.playerUuid(),
                Optional.of(helper.playerUuid()),
                ReviveActionType.ALLY_REVIVE,
                now + durationTicks,
                helper.position().immutable(),
                target.position().immutable(),
                helper.hotbarSlot());
        return new StartAllowed(action);
    }

    public enum TickOutcome {
        CONTINUE,
        COMPLETE,
        CANCEL
    }

    public enum CancelReason {
        HELPER_MOVED,
        TARGET_MOVED,
        HELPER_LEFT_RADIUS,
        HELPER_CHANGED_SLOT,
        HELPER_TOO_FAR,
        HELPER_DAMAGED,
        HELPER_NOT_IN_TEMPORARY_HELPER_STATE,
        TARGET_STATE_CHANGED,
        DIMENSION_CHANGED,
        DEADLINE_REACHED
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
    }

    public static TickVerdict evaluateTick(
            KoAction action,
            KnockoutPlayerSnapshot helper,
            KnockoutPlayerSnapshot target,
            long now,
            double maxDistance,
            boolean helperTookDamage,
            double helperMoveTolerance
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(helper, "helper");
        Objects.requireNonNull(target, "target");
        if (helper.state() == ReviveState.DEAD_PENDING_TRANSFER || target.state() == ReviveState.DEAD_PENDING_TRANSFER) {
            return TickVerdict.cancel(CancelReason.HELPER_NOT_IN_TEMPORARY_HELPER_STATE);
        }
        if (!helper.dimension().equals(target.dimension())) {
            return TickVerdict.cancel(CancelReason.DIMENSION_CHANGED);
        }
        if (target.state() != ReviveState.TEMPORARY_KO) {
            return TickVerdict.cancel(CancelReason.TARGET_STATE_CHANGED);
        }
        if (helperTookDamage) {
            return TickVerdict.cancel(CancelReason.HELPER_DAMAGED);
        }
        double distance = helper.position().distSqr(target.position());
        if (distance > maxDistance * maxDistance) {
            return TickVerdict.cancel(CancelReason.HELPER_LEFT_RADIUS);
        }
        if (helper.hotbarSlot() != action.helperStartSlot()) {
            return TickVerdict.cancel(CancelReason.HELPER_CHANGED_SLOT);
        }
        if (action.helperStartPosition().distSqr(helper.position()) > helperMoveTolerance * helperMoveTolerance) {
            return TickVerdict.cancel(CancelReason.HELPER_MOVED);
        }
        if (action.targetStartPosition().distSqr(target.position()) > helperMoveTolerance * helperMoveTolerance) {
            return TickVerdict.cancel(CancelReason.TARGET_MOVED);
        }
        if (action.isExpired(now)) {
            return TickVerdict.complete();
        }
        return TickVerdict.continueRun();
    }
}
