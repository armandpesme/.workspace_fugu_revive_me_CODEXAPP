package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.registry.ModEffects;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Service that orchestrates the Jalon 4 prolonged knockout flow: it
 * keeps the in-memory {@link BossLinkRegistry} in sync with the
 * durable {@code KoRecord} state, drives the timeout-based transitions,
 * handles the dimension change and boss death/despawn hooks, and
 * resurrects a player at the original K.O. position when the linked
 * boss dies.
 * <p>
 * The service depends on small pure suppliers so it can be exercised
 * in unit tests without spinning up a Minecraft server. The
 * player-facing side effects (teleport, health, sickness) are routed
 * through a {@link ResurrectionApplier} so the orchestration logic can
 * be unit tested without mocking {@link ServerPlayer}.
 */
public final class ProlongedKoService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Supplier<KnockoutSavedData> data;
    private final LongSupplier overworldClock;
    private final Function<UUID, OptionalInt> entityIdLookup;
    private final ReviveSyncService sync;
    private final BossLinkRegistry bossLinks;
    private final ResurrectionApplier resurrection;
    private final Config config;

    public ProlongedKoService(
            Supplier<KnockoutSavedData> data,
            LongSupplier overworldClock,
            Function<UUID, OptionalInt> entityIdLookup,
            ReviveSyncService sync,
            BossLinkRegistry bossLinks,
            ResurrectionApplier resurrection,
            Config config
    ) {
        this.data = Objects.requireNonNull(data, "data");
        this.overworldClock = Objects.requireNonNull(overworldClock, "overworldClock");
        this.entityIdLookup = Objects.requireNonNull(entityIdLookup, "entityIdLookup");
        this.sync = Objects.requireNonNull(sync, "sync");
        this.bossLinks = Objects.requireNonNull(bossLinks, "bossLinks");
        this.resurrection = Objects.requireNonNull(resurrection, "resurrection");
        this.config = Objects.requireNonNull(config, "config");
    }

    public int tickExpirations() {
        long now = overworldClock.getAsLong();
        KnockoutSavedData savedData = data.get();
        int transitions = 0;
        for (UUID playerUuid : savedData.pollDue(now)) {
            Optional<KoRecord> current = savedData.get(playerUuid);
            if (current.isEmpty()) {
                continue;
            }
            Optional<KoRecord> next = ProlongedKoLogic.transitionOnTimeout(current.get(), now);
            if (next.isEmpty()) {
                continue;
            }
            savedData.put(playerUuid, next.get());
            unlinkIfAny(current.get(), playerUuid);
            publishTransition(playerUuid, next.get(), now);
            transitions++;
            LOGGER.debug("Prolonged KO timeout fired player={} now={} nextState={}",
                    playerUuid, now, next.get().state());
        }
        return transitions;
    }

    public boolean tickDimensionChanges(UUID playerUuid, ResourceLocation currentDimension) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(currentDimension, "currentDimension");
        Optional<KoRecord> current = data.get().get(playerUuid);
        if (current.isEmpty()) {
            return false;
        }
        if (current.get().koDimension().equals(currentDimension)) {
            return false;
        }
        Optional<KoRecord> next = ProlongedKoLogic.transitionOnDimensionChange(current.get());
        if (next.isEmpty()) {
            return false;
        }
        data.get().put(playerUuid, next.get());
        unlinkIfAny(current.get(), playerUuid);
        publishTransition(playerUuid, next.get(), overworldClock.getAsLong());
        LOGGER.info("Prolonged KO aborted by dimension change player={} to={}", playerUuid, currentDimension);
        return true;
    }

    public boolean onBossDeath(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Set<UUID> players = bossLinks.playersLinkedTo(bossUuid);
        if (players.isEmpty()) {
            return false;
        }
        long now = overworldClock.getAsLong();
        boolean anyTransitioned = false;
        for (UUID playerUuid : players) {
            Optional<KoRecord> current = data.get().get(playerUuid);
            if (current.isEmpty()) {
                continue;
            }
            Optional<KoRecord> next = ProlongedKoLogic.transitionOnBossDeath(current.get(), now);
            if (next.isEmpty()) {
                continue;
            }
            data.get().put(playerUuid, next.get());
            bossLinks.unlink(bossUuid, playerUuid);
            publishTransition(playerUuid, next.get(), now);
            anyTransitioned = true;
            LOGGER.info("Prolonged KO boss died boss={} player={} nextState={}",
                    bossUuid, playerUuid, next.get().state());
        }
        return anyTransitioned;
    }

    public boolean onBossDespawn(UUID bossUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Set<UUID> players = bossLinks.playersLinkedTo(bossUuid);
        if (players.isEmpty()) {
            return false;
        }
        long now = overworldClock.getAsLong();
        boolean anyTransitioned = false;
        for (UUID playerUuid : players) {
            Optional<KoRecord> current = data.get().get(playerUuid);
            if (current.isEmpty()) {
                continue;
            }
            Optional<KoRecord> next = ProlongedKoLogic.transitionOnBossDespawn(current.get());
            if (next.isEmpty()) {
                continue;
            }
            data.get().put(playerUuid, next.get());
            bossLinks.unlink(bossUuid, playerUuid);
            publishTransition(playerUuid, next.get(), now);
            anyTransitioned = true;
            LOGGER.info("Prolonged KO boss despawned boss={} player={} nextState={}",
                    bossUuid, playerUuid, next.get().state());
        }
        return anyTransitioned;
    }

    public void linkBossToPlayer(UUID bossUuid, UUID playerUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid");
        Objects.requireNonNull(playerUuid, "playerUuid");
        bossLinks.link(bossUuid, playerUuid);
    }

    public void unlinkPlayer(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        KnockoutSavedData savedData = data.get();
        Optional<KoRecord> current = savedData.get(playerUuid);
        if (current.isEmpty()) {
            bossLinks.unlinkBossByPlayer(playerUuid);
            return;
        }
        unlinkIfAny(current.get(), playerUuid);
    }

    public boolean resurrectOnKoPosition(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        KnockoutSavedData savedData = data.get();
        Optional<KoRecord> record = savedData.get(playerUuid);
        if (record.isEmpty()) {
            return false;
        }
        KoRecord koRecord = record.get();
        if (koRecord.state() != ReviveState.PENDING_REVIVE) {
            return false;
        }
        ResurrectionResult result = resurrection.apply(playerUuid, koRecord.koDimension(),
                koRecord.koPosition(), config.resurrectionSicknessDurationTicks());
        if (!result.applied()) {
            LOGGER.info("Prolonged KO resurrection skipped for player={} (player offline)", playerUuid);
            return false;
        }
        savedData.transitionToAlive(playerUuid);
        unlinkIfAny(koRecord, playerUuid);
        sync.sendSelf(playerUuid, savedData.get(playerUuid), overworldClock.getAsLong());
        LOGGER.info("Prolonged KO resurrection applied player={} dimension={} pos={}",
                playerUuid, koRecord.koDimension(), koRecord.koPosition());
        return true;
    }

    private void unlinkIfAny(KoRecord previous, UUID playerUuid) {
        previous.linkedBossUuid().ifPresent(boss -> bossLinks.unlink(boss, playerUuid));
    }

    private void publishTransition(UUID playerUuid, KoRecord record, long now) {
        OptionalInt entityId = entityIdLookup.apply(playerUuid);
        if (entityId.isPresent()) {
            sync.publishTransition(playerUuid, entityId.getAsInt(), record, now);
        } else {
            sync.publishTransition(playerUuid, record, now);
        }
    }

    /**
     * Bridge between the orchestrating service and the live
     * {@link ServerPlayer}. The interface is intentionally narrow so
     * the unit tests can plug in a deterministic stub.
     */
    public interface ResurrectionApplier {
        ResurrectionResult apply(UUID playerUuid,
                                 ResourceLocation koDimension,
                                 BlockPos koPosition,
                                 int sicknessDurationTicks);
    }

    public record ResurrectionResult(boolean applied) {
        public static ResurrectionResult success() {
            return new ResurrectionResult(true);
        }

        public static ResurrectionResult skipped() {
            return new ResurrectionResult(false);
        }
    }

    /**
     * Default {@link ResurrectionApplier} that targets a real
     * {@link ServerPlayer} via the {@code @Mod}-side
     * {@link KoEventHandlers}.
     */
    public static final class ServerPlayerResurrectionApplier implements ResurrectionApplier {
        @Override
        public ResurrectionResult apply(UUID playerUuid,
                                        ResourceLocation koDimension,
                                        BlockPos koPosition,
                                        int sicknessDurationTicks) {
            ServerPlayer player = MinecraftServerGuard.getServer() == null
                    ? null
                    : MinecraftServerGuard.getServer().getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                return ResurrectionResult.skipped();
            }
            var target = MinecraftServerGuard.getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, koDimension));
            if (target != null) {
                double x = koPosition.getX() + 0.5;
                double y = koPosition.getY() + 0.1;
                double z = koPosition.getZ() + 0.5;
                player.teleportTo(target, x, y, z, 0F, 0F);
            }
            float maxHealth = player.getMaxHealth();
            player.setHealth(Math.max(1F, maxHealth * 0.25F));
            player.addEffect(new MobEffectInstance(
                    ModEffects.RESURRECTION_SICKNESS.get(),
                    sicknessDurationTicks));
            return ResurrectionResult.success();
        }
    }

    public record Config(int resurrectionSicknessDurationTicks) {
        public Config {
            if (resurrectionSicknessDurationTicks < 1) {
                throw new IllegalArgumentException("resurrectionSicknessDurationTicks must be >= 1");
            }
        }
    }
}
