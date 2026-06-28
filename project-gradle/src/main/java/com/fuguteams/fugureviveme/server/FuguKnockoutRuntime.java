package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.config.ServerConfig;
import com.fuguteams.fugureviveme.network.FuguNetwork;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Singleton runtime container that owns the orchestrating services used by
 * the Forge event handlers. The runtime is rebuilt whenever the underlying
 * {@link MinecraftServer} changes (startup, singleplayer reload).
 * <p>
 * The runtime is intentionally narrow: it stores no per-player state. All
 * per-player data lives in the {@link KnockoutSavedData} or in the
 * in-memory action registries, both of which are recreated when the
 * server changes.
 */
public final class FuguKnockoutRuntime {
    private static volatile FuguKnockoutRuntime instance;
    private final MinecraftServer server;
    private final ReviveService revive;
    private final AllyReviveService ally;
    private final SoulAnchorService soulAnchor;
    private final KnockoutRespawnService respawn;
    private final KnockoutRestrictionService restrictions;
    private final KnockoutActionRegistry actionRegistry;
    private final KnockoutDamageTracker damage;
    private final LastSafePositionTracker tracker;
    private final MovementOverrideRegistry movementOverride;
    private final RuntimeConfig config;

    private FuguKnockoutRuntime(MinecraftServer server, RuntimeConfig config) {
        this.server = server;
        this.config = config;
        java.util.function.Supplier<KnockoutSavedData> dataSupplier = () -> KnockoutSavedData.get(server);
        LongSupplier overworldClock = () -> server.overworld().getGameTime();
        this.revive = new ReviveService(
                dataSupplier,
                overworldClock,
                new ReviveSyncService(new ForgePacketSink(server)),
                uuid -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    return player == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(player.getId());
                });
        this.actionRegistry = new KnockoutActionRegistry();
        this.damage = new KnockoutDamageTracker();
        this.tracker = new LastSafePositionTracker();
        this.movementOverride = new MovementOverrideRegistry();
        this.ally = new AllyReviveService(
                actionRegistry, revive, damage, overworldClock, config.allyReviveConfig());
        this.soulAnchor = new SoulAnchorService(
                actionRegistry, revive, damage, overworldClock, config.soulAnchorConfig());
        this.respawn = new KnockoutRespawnService();
        this.restrictions = new KnockoutRestrictionService();
    }

    public static FuguKnockoutRuntime get() {
        FuguKnockoutRuntime current = instance;
        if (current == null) {
            throw new IllegalStateException("FuguKnockoutRuntime is not initialised; "
                    + "call init() from FuguReviveMe.serverStarted or equivalent hook");
        }
        return current;
    }

    public static FuguKnockoutRuntime init(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        FuguKnockoutRuntime runtime = new FuguKnockoutRuntime(server, RuntimeConfig.fromServerConfig());
        instance = runtime;
        FuguNetwork.setReleaseSpiritHandler(runtime.revive::requestReleaseSpirit);
        return runtime;
    }

    public static void reset() {
        instance = null;
    }

    public ReviveService revive() {
        return revive;
    }

    public AllyReviveService ally() {
        return ally;
    }

    public SoulAnchorService soulAnchor() {
        return soulAnchor;
    }

    public KnockoutRespawnService respawn() {
        return respawn;
    }

    public KnockoutRestrictionService restrictions() {
        return restrictions;
    }

    public KnockoutActionRegistry actionRegistry() {
        return actionRegistry;
    }

    public KnockoutDamageTracker damage() {
        return damage;
    }

    public LastSafePositionTracker tracker() {
        return tracker;
    }

    public MovementOverrideRegistry movementOverride() {
        return movementOverride;
    }

    public RuntimeConfig config() {
        return config;
    }

    public MinecraftServer server() {
        return server;
    }

    /**
     * Materialised snapshot of the live config values, refreshed on every
     * server start. Storing the values instead of calling
     * {@link ServerConfig} directly keeps the runtime services free of
     * statics and easier to swap in tests.
     */
    public record RuntimeConfig(
            int temporaryDurationTicks,
            int temporaryMaxHits,
            int temporaryReviveDurationTicks,
            double temporaryReviveMaxDistance,
            double temporaryRevivedHealthPercent,
            int prolongedDurationTicks,
            int prolongedMaxHits,
            double prolongedBossSearchRadius,
            int resurrectionSicknessDurationTicks
    ) {
        public AllyReviveService.AllyReviveConfig allyReviveConfig() {
            return new AllyReviveService.AllyReviveConfig(
                    temporaryReviveDurationTicks,
                    temporaryReviveMaxDistance,
                    0.1,
                    temporaryRevivedHealthPercent);
        }

        public SoulAnchorService.SoulAnchorConfig soulAnchorConfig() {
            return new SoulAnchorService.SoulAnchorConfig(
                    temporaryReviveDurationTicks,
                    0.1,
                    temporaryRevivedHealthPercent);
        }

        public static RuntimeConfig fromServerConfig() {
            return new RuntimeConfig(
                    ServerConfig.TEMPORARY_KO_DURATION_TICKS.get(),
                    ServerConfig.TEMPORARY_KO_MAX_HITS.get(),
                    ServerConfig.TEMPORARY_KO_REVIVE_DURATION_TICKS.get(),
                    ServerConfig.TEMPORARY_KO_REVIVE_MAX_DISTANCE.get(),
                    ServerConfig.TEMPORARY_KO_REVIVED_HEALTH_PERCENT.get(),
                    ServerConfig.PROLONGED_KO_DURATION_TICKS.get(),
                    ServerConfig.PROLONGED_KO_MAX_HITS.get(),
                    ServerConfig.PROLONGED_KO_BOSS_SEARCH_RADIUS.get(),
                    ServerConfig.RESURRECTION_SICKNESS_DURATION_TICKS.get());
        }
    }
}
