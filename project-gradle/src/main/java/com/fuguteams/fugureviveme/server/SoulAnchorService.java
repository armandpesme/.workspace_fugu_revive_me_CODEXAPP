package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Service that orchestrates the soul-anchor self-revive. The target and
 * helper are the same player; the service ensures the soul anchor remains
 * in the hotbar and the player stays still until the channel completes.
 */
public final class SoulAnchorService {
    private final KnockoutActionRegistry registry;
    private final ReviveService revive;
    private final KnockoutDamageTracker damage;
    private final LongSupplier clock;
    private final SoulAnchorConfig config;

    public SoulAnchorService(
            KnockoutActionRegistry registry,
            ReviveService revive,
            KnockoutDamageTracker damage,
            LongSupplier clock,
            SoulAnchorConfig config
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.revive = Objects.requireNonNull(revive, "revive");
        this.damage = Objects.requireNonNull(damage, "damage");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.config = Objects.requireNonNull(config, "config");
    }

    public SoulAnchorLogic.StartOutcome tryStart(KnockoutPlayerSnapshot target, int anchorSlot) {
        boolean alreadyActive = registry.get(target.playerUuid()).isPresent();
        SoulAnchorLogic.StartOutcome outcome = SoulAnchorLogic.evaluateStart(
                target,
                anchorSlot,
                alreadyActive,
                damage.consume(target.playerUuid()),
                clock.getAsLong(),
                config.castDurationTicks());
        if (outcome instanceof SoulAnchorLogic.StartAllowed allowed) {
            registry.start(allowed.action());
        }
        return outcome;
    }

    public Optional<SoulAnchorLogic.CancelReason> tick(KoTickInputs inputs) {
        Optional<SoulAnchorLogic.CancelReason> cancellation = Optional.empty();
        for (KoAction action : registry.all()) {
            if (action.type() != ReviveActionType.SELF_REVIVE) {
                continue;
            }
            KnockoutPlayerSnapshot snap = inputs.snapshotOf(action.targetUuid()).orElse(null);
            if (snap == null) {
                continue;
            }
            SoulAnchorLogic.TickVerdict verdict = SoulAnchorLogic.evaluateTick(
                    action, snap, clock.getAsLong(), config.movementTolerance());
            switch (verdict.outcome()) {
                case CANCEL -> {
                    registry.cancelTarget(action.targetUuid());
                    cancellation = verdict.reason();
                }
                case COMPLETE -> {
                    inputs.consumeSoulAnchor(action.targetUuid(), action.helperStartSlot());
                    inputs.applyResurrectionSickness(action.targetUuid());
                    revive.transitionToAlive(action.targetUuid());
                    KnockoutPlayerSnapshot finalSnap = inputs.snapshotOf(action.targetUuid()).orElseThrow();
                    inputs.restoreHealthToPercent(finalSnap, config.revivedHealthPercent());
                    registry.cancelTarget(action.targetUuid());
                }
                case CONTINUE -> {
                    // nothing
                }
            }
        }
        return cancellation;
    }

    public void cancelByTarget(UUID targetUuid) {
        registry.cancelTarget(targetUuid);
    }

    public record SoulAnchorConfig(
            int castDurationTicks,
            double movementTolerance,
            double revivedHealthPercent
    ) {
        public SoulAnchorConfig {
            if (castDurationTicks < 1) {
                throw new IllegalArgumentException("castDurationTicks must be >= 1");
            }
            if (movementTolerance < 0) {
                throw new IllegalArgumentException("movementTolerance must be >= 0");
            }
        }
    }

    /**
     * Bridge between the orchestrating layer and the pure logic: the tick
     * caller supplies live snapshots and side-effect handlers.
     */
    public interface KoTickInputs {
        Optional<KnockoutPlayerSnapshot> snapshotOf(UUID playerUuid);

        void applyResurrectionSickness(UUID targetUuid);

        void restoreHealthToPercent(KnockoutPlayerSnapshot snapshot, double percent);

        void consumeSoulAnchor(UUID targetUuid, int slot);
    }
}
