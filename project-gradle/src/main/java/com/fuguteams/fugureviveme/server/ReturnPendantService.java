package com.fuguteams.fugureviveme.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class ReturnPendantService {
    private final Map<UUID, ReturnPendantCast> activeCasts = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final ReturnPendantConfig config;
    private final List<Consumer<UUID>> cooldownListeners = new ArrayList<>();
    private final List<Consumer<UUID>> teleportListeners = new ArrayList<>();

    public ReturnPendantService(LongSupplier clock, ReturnPendantConfig config) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void onCooldownApplied(Consumer<UUID> listener) {
        cooldownListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void onTeleportRequested(Consumer<UUID> listener) {
        teleportListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public ReturnPendantLogic.StartVerdict startCast(
            UUID playerUuid,
            ResourceLocation currentDimension,
            boolean onCooldown,
            Vec3 startPosition,
            int startSlot
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (activeCasts.containsKey(playerUuid)) {
            return new ReturnPendantLogic.StartRejected(
                    ReturnPendantLogic.StartRejection.ALREADY_CASTING);
        }
        ReturnPendantLogic.StartVerdict verdict = ReturnPendantLogic.evaluateStart(
                playerUuid, currentDimension, config.mainDimension(),
                onCooldown, startPosition, startSlot,
                clock.getAsLong(), config.castTimeTicks());
        if (verdict instanceof ReturnPendantLogic.StartAllowed allowed) {
            activeCasts.put(playerUuid, allowed.cast());
        }
        return verdict;
    }

    public ReturnPendantLogic.TickVerdict tickCast(
            UUID playerUuid,
            Vec3 currentPosition,
            int currentSlot,
            boolean itemMatches
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ReturnPendantCast cast = activeCasts.get(playerUuid);
        if (cast == null) {
            return ReturnPendantLogic.TickVerdict.notCasting();
        }
        ReturnPendantLogic.TickVerdict verdict = ReturnPendantLogic.evaluateTick(
                cast, currentPosition, currentSlot, itemMatches,
                clock.getAsLong(), config.movementTolerance());
        switch (verdict.outcome()) {
            case CANCEL -> activeCasts.remove(playerUuid);
            case COMPLETE -> {
                activeCasts.remove(playerUuid);
                cooldownListeners.forEach(listener -> listener.accept(playerUuid));
                teleportListeners.forEach(listener -> listener.accept(playerUuid));
            }
            default -> {
            }
        }
        return verdict;
    }

    public void cancelCast(UUID playerUuid) {
        activeCasts.remove(playerUuid);
    }

    public boolean isCasting(UUID playerUuid) {
        return activeCasts.containsKey(playerUuid);
    }

    public ReturnPendantConfig config() {
        return config;
    }

    public record ReturnPendantConfig(
            ResourceLocation mainDimension,
            int castTimeTicks,
            int cooldownTicks,
            double movementTolerance
    ) {
        public ReturnPendantConfig {
            Objects.requireNonNull(mainDimension, "mainDimension");
            if (castTimeTicks < 1) {
                throw new IllegalArgumentException("castTimeTicks must be >= 1");
            }
            if (cooldownTicks < 1) {
                throw new IllegalArgumentException("cooldownTicks must be >= 1");
            }
            if (movementTolerance < 0) {
                throw new IllegalArgumentException("movementTolerance must be >= 0");
            }
        }
    }
}
