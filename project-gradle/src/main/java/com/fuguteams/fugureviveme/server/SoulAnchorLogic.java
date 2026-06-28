package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure functions describing the soul-anchor self-revive. The target is also
 * the helper: the player must hold a soul anchor in the hotbar and stand
 * still while a 5-second channel completes.
 */
public final class SoulAnchorLogic {

    private SoulAnchorLogic() {
    }

    public enum StartDenial {
        TARGET_NOT_IN_TEMPORARY_KO,
        TARGET_IN_PROLONGED_KO,
        ALREADY_ACTIVE,
        ANCHOR_NOT_IN_HOTBAR,
        PLAYER_TAKEN_DAMAGE_RECENTLY
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
            KnockoutPlayerSnapshot target,
            int anchorSlot,
            boolean actionAlreadyActive,
            boolean takenDamageRecently,
            long now,
            int durationTicks
    ) {
        Objects.requireNonNull(target, "target");
        if (target.state() == ReviveState.PROLONGED_KO || target.state() == ReviveState.FULLY_DOWNED) {
            return new StartDenied(StartDenial.TARGET_IN_PROLONGED_KO);
        }
        if (target.state() != ReviveState.TEMPORARY_KO) {
            return new StartDenied(StartDenial.TARGET_NOT_IN_TEMPORARY_KO);
        }
        if (actionAlreadyActive) {
            return new StartDenied(StartDenial.ALREADY_ACTIVE);
        }
        if (anchorSlot < 0 || anchorSlot > 8 || !target.hasSoulAnchorInHotbar()) {
            return new StartDenied(StartDenial.ANCHOR_NOT_IN_HOTBAR);
        }
        if (takenDamageRecently) {
            return new StartDenied(StartDenial.PLAYER_TAKEN_DAMAGE_RECENTLY);
        }
        KoAction action = new KoAction(
                target.playerUuid(),
                Optional.empty(),
                ReviveActionType.SELF_REVIVE,
                now + durationTicks,
                target.position().immutable(),
                target.position().immutable(),
                anchorSlot);
        return new StartAllowed(action);
    }

    public enum CancelReason {
        TARGET_STATE_CHANGED,
        PLAYER_TAKEN_DAMAGE,
        SLOT_CHANGED,
        ANCHOR_REMOVED,
        PLAYER_MOVED
    }

    public enum TickOutcome {
        CONTINUE,
        COMPLETE,
        CANCEL
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
            KnockoutPlayerSnapshot target,
            long now,
            double movementTolerance
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(target, "target");
        if (target.state() != ReviveState.TEMPORARY_KO) {
            return TickVerdict.cancel(CancelReason.TARGET_STATE_CHANGED);
        }
        if (target.hotbarSlot() != action.helperStartSlot()) {
            return TickVerdict.cancel(CancelReason.SLOT_CHANGED);
        }
        if (!target.hasSoulAnchorInHotbar()) {
            return TickVerdict.cancel(CancelReason.ANCHOR_REMOVED);
        }
        if (action.targetStartPosition().distSqr(target.position()) > movementTolerance * movementTolerance) {
            return TickVerdict.cancel(CancelReason.PLAYER_MOVED);
        }
        if (action.isExpired(now)) {
            return TickVerdict.complete();
        }
        return TickVerdict.continueRun();
    }
}
