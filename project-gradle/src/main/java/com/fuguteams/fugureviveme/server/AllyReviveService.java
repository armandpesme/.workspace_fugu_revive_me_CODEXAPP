package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.ReviveState;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Service that orchestrates the ally revive interaction. It owns the
 * {@link KnockoutActionRegistry} entry lifecycle and delegates the
 * decision logic to {@link AllyReviveLogic}.
 * <p>
 * The service is intentionally side-effect-light: the underlying
 * {@link ReviveService} is responsible for persisting the player record
 * and publishing the snapshot once the revive completes.
 */
public final class AllyReviveService {
    private final KnockoutActionRegistry registry;
    private final ReviveService revive;
    private final KnockoutDamageTracker damage;
    private final LongSupplier clock;
    private final AllyReviveConfig config;

    public AllyReviveService(
            KnockoutActionRegistry registry,
            ReviveService revive,
            KnockoutDamageTracker damage,
            LongSupplier clock,
            AllyReviveConfig config
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.revive = Objects.requireNonNull(revive, "revive");
        this.damage = Objects.requireNonNull(damage, "damage");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.config = Objects.requireNonNull(config, "config");
    }

    public AllyReviveLogic.StartOutcome tryStart(KnockoutPlayerSnapshot helper, KnockoutPlayerSnapshot target) {
        boolean alreadyActive = registry.get(target.playerUuid()).isPresent();
        AllyReviveLogic.StartOutcome outcome = AllyReviveLogic.evaluateStart(
                helper,
                target,
                alreadyActive,
                clock.getAsLong(),
                config.reviveDurationTicks(),
                config.reviveMaxDistance());
        if (outcome instanceof AllyReviveLogic.StartAllowed allowed) {
            registry.start(allowed.action());
        }
        return outcome;
    }

    public Optional<AllyReviveLogic.CancelReason> tick(
            KoTickInputs inputs
    ) {
        Optional<AllyReviveLogic.CancelReason> cancellation = Optional.empty();
        for (com.fuguteams.fugureviveme.state.KoAction action : registry.all()) {
            if (action.type() != com.fuguteams.fugureviveme.state.ReviveActionType.ALLY_REVIVE) {
                continue;
            }
            UUID helperUuid = action.helperUuid().orElseThrow();
            KnockoutPlayerSnapshot helperSnap = inputs.snapshotOf(helperUuid).orElse(null);
            KnockoutPlayerSnapshot targetSnap = inputs.snapshotOf(action.targetUuid()).orElse(null);
            if (helperSnap == null || targetSnap == null) {
                continue;
            }
            AllyReviveLogic.TickVerdict verdict = AllyReviveLogic.evaluateTick(
                    action,
                    helperSnap,
                    targetSnap,
                    clock.getAsLong(),
                    config.reviveMaxDistance(),
                    damage.consume(helperUuid),
                    config.helperMoveTolerance());
            switch (verdict.outcome()) {
                case CANCEL -> {
                    registry.cancelTarget(action.targetUuid());
                    cancellation = verdict.reason();
                }
                case COMPLETE -> {
                    completeRevive(action.targetUuid(), inputs);
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

    public void cancelByHelper(UUID helperUuid) {
        registry.cancelHelper(helperUuid);
    }

    private void completeRevive(UUID targetUuid, KoTickInputs inputs) {
        inputs.applyResurrectionSickness(targetUuid);
        revive.transitionToAlive(targetUuid);
        KnockoutPlayerSnapshot snap = inputs.snapshotOf(targetUuid).orElseThrow();
        inputs.restoreHealthToPercent(snap, config.revivedHealthPercent());
    }

    public record AllyReviveConfig(
            int reviveDurationTicks,
            double reviveMaxDistance,
            double helperMoveTolerance,
            double revivedHealthPercent
    ) {
        public AllyReviveConfig {
            if (reviveDurationTicks < 1) {
                throw new IllegalArgumentException("reviveDurationTicks must be >= 1");
            }
            if (reviveMaxDistance < 0) {
                throw new IllegalArgumentException("reviveMaxDistance must be >= 0");
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
    }
}
