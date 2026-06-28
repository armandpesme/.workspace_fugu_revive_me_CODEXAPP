package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.registry.ModEffects;
import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Server-only event handlers that orchestrate the Fugu Revive Me flow:
 * death interception, knock-out hit handling, ally revive, soul anchor
 * self-revive, spawn transfer after death, safe-position sampling, and
 * the temporary-KO restrictions.
 * <p>
 * The class is annotated with {@code @Mod.EventBusSubscriber(Dist.DEDICATED_SERVER)}
 * so it never runs on a dedicated server, preserving the mod's promise of
 * zero client-side state on the server. The orchestrating code is split
 * into small private methods so each Forge event is a thin adapter around
 * the pure services.
 */
@Mod.EventBusSubscriber(modid = com.fuguteams.fugureviveme.FuguReviveMe.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class KoEventHandlers {

    private static final int HOTBAR_SIZE = 9;

    private KoEventHandlers() {
        throw new AssertionError("KoEventHandlers is a static utility and must not be instantiated");
    }

    public static void register(IEventBus forgeBus) {
        Objects.requireNonNull(forgeBus, "forgeBus");
        forgeBus.register(KoEventHandlers.class);
    }

    // Death interception

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            handleNonPlayerDeath(event);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        String biomeId = currentBiomeId(level, player.blockPosition());
        BiomeKoClassifier.KoType koType = ServerConfigRuntime.classify(biomeId).type();
        if (koType == BiomeKoClassifier.KoType.NONE) {
            return;
        }
        boolean hasSickness = player.hasEffect(ModEffects.RESURRECTION_SICKNESS.get());
        FuguKnockoutRuntime service = FuguKnockoutRuntime.get();
        Optional<UUID> nearbyBoss = koType == BiomeKoClassifier.KoType.PROLONGED
                ? findNearestBoss(level, player.blockPosition(),
                        service.config().prolongedBossTag(),
                        service.config().prolongedBossSearchRadius())
                : Optional.empty();
        ProlongedKoLogic.InitialState initial = ProlongedKoLogic.selectInitialState(
                koType,
                nearbyBoss.isPresent(),
                service.config().temporaryDurationTicks(),
                service.config().prolongedDurationTicks());
        if (initial instanceof ProlongedKoLogic.RejectChoice) {
            return;
        }
        BiomeKoClassifier.KoType effectiveBiome;
        Optional<UUID> effectiveBoss;
        int durationTicks;
        int maxHits;
        if (initial instanceof ProlongedKoLogic.ProlongedChoice) {
            effectiveBiome = BiomeKoClassifier.KoType.PROLONGED;
            effectiveBoss = nearbyBoss;
            durationTicks = service.config().prolongedDurationTicks();
            maxHits = service.config().prolongedMaxHits();
        } else {
            effectiveBiome = BiomeKoClassifier.KoType.TEMPORARY;
            effectiveBoss = Optional.empty();
            durationTicks = service.config().temporaryDurationTicks();
            maxHits = service.config().temporaryMaxHits();
        }
        Optional<KoRecord> created = service.revive().tryEnterKnockoutOnDeath(
                player.getUUID(),
                level.dimension().location(),
                player.blockPosition(),
                effectiveBiome,
                hasSickness,
                effectiveBoss,
                durationTicks,
                maxHits);
        if (created.isPresent()) {
            event.setCanceled(true);
            player.setHealth(0.001F);
            player.invulnerableTime = 40;
            player.setLastHurtByPlayer(null);
            player.setLastHurtByMob(null);
            if (effectiveBoss.isPresent()) {
                service.prolonged().linkBossToPlayer(effectiveBoss.get(), player.getUUID());
            }
        }
    }

    private static void handleNonPlayerDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        FuguKnockoutRuntime runtime;
        try {
            runtime = FuguKnockoutRuntime.get();
        } catch (IllegalStateException uninitialised) {
            return;
        }
        if (!isBossEntity(entity, runtime.config().prolongedBossTag())) {
            return;
        }
        runtime.prolonged().onBossDeath(entity.getUUID());
    }

    private static boolean isBossEntity(Entity entity, String bossTagString) {
        if (entity instanceof ServerPlayer) {
            return false;
        }
        if (entity.getType() == null) {
            return false;
        }
        return parseBossTag(bossTagString)
                .map(id -> TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, id))
                .map(tag -> entity.getType().is(tag))
                .orElse(false);
    }

    // Hit handling

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        ReviveService service = runtime.revive();
        ReviveState state = service.snapshotState(player.getUUID());
        if (state == ReviveState.FULLY_DOWNED) {
            event.setCanceled(true);
            return;
        }
        if (state == ReviveState.TEMPORARY_KO || state == ReviveState.PROLONGED_KO) {
            event.setCanceled(true);
            int maxHits = state == ReviveState.TEMPORARY_KO
                    ? runtime.config().temporaryMaxHits()
                    : runtime.config().prolongedMaxHits();
            service.applyKnockoutHit(player.getUUID(), maxHits);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer attacker)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        if (!runtime.restrictions().allowAttack(runtime.revive().snapshotState(attacker.getUUID()))) {
            event.setCanceled(true);
        }
    }

    // Ally revive

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer helper)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        ReviveState targetState = runtime.revive().snapshotState(target.getUUID());
        if (targetState != ReviveState.TEMPORARY_KO) {
            return;
        }
        KnockoutPlayerSnapshot helperSnap = snapshotOf(helper);
        KnockoutPlayerSnapshot targetSnap = snapshotOf(target);
        AllyReviveService.AllyReviveConfig config = runtime.config().allyReviveConfig();
        AllyReviveLogic.StartOutcome outcome = runtime.ally().tryStart(helperSnap, targetSnap);
        if (outcome instanceof AllyReviveLogic.StartDenied denied) {
            if (denied.reason() == AllyReviveLogic.StartDenial.HELPER_DISTANCE_OUT_OF_RANGE) {
                helper.displayClientMessage(Component.literal("Target is too far to revive")
                        .withStyle(ChatFormatting.RED), true);
            }
        } else {
            event.setCanceled(true);
        }
    }

    // Soul anchor

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!isSoulAnchor(stack)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        ReviveState state = runtime.revive().snapshotState(player.getUUID());
        if (state != ReviveState.TEMPORARY_KO) {
            return;
        }
        int slot = player.getInventory().selected;
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            event.setCanceled(true);
            return;
        }
        KnockoutPlayerSnapshot snap = snapshotOf(player);
        runtime.soulAnchor().tryStart(snap, slot);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        if (!runtime.restrictions().allowBlockInteraction(runtime.revive().snapshotState(player.getUUID()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        if (!runtime.restrictions().allowItemUse(runtime.revive().snapshotState(player.getUUID()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
            if (!runtime.restrictions().allowInventory(runtime.revive().snapshotState(player.getUUID()))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTeleport(net.minecraftforge.event.entity.EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        if (!runtime.restrictions().allowTeleport(runtime.revive().snapshotState(player.getUUID()))) {
            event.setCanceled(true);
        }
    }

    // Respawn transfer

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        ReviveState previousState = runtime.revive().snapshotState(player.getUUID());
        if (previousState == ReviveState.PENDING_REVIVE) {
            runtime.prolonged().resurrectOnKoPosition(player.getUUID());
        } else if (runtime.respawn().shouldTransfer(previousState)) {
            runtime.revive().transitionToAlive(player.getUUID());
            runtime.respawn().transferToGenericSpawn(player, server);
        }
    }

    // Damage tracker (used by ally revive cancellation)

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
            runtime.damage().flag(player.getUUID(), player.level().getGameTime());
        }
    }

    // Server tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        runtime.revive().tickExpirations();
        runtime.prolonged().tickExpirations();
        runtime.ally().tick(forgeTickInputs(runtime));
        runtime.soulAnchor().tick(forgeSoulAnchorInputs(runtime));
        sampleSafePositions(runtime);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        runtime.ally().cancelByTarget(player.getUUID());
        runtime.ally().cancelByHelper(player.getUUID());
        runtime.soulAnchor().cancelByTarget(player.getUUID());
        runtime.prolonged().tickDimensionChanges(player.getUUID(), event.getTo().location());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FuguKnockoutRuntime runtime;
        try {
            runtime = FuguKnockoutRuntime.get();
        } catch (IllegalStateException uninitialised) {
            return;
        }
        clearTransientPlayerState(
                runtime.actionRegistry(),
                runtime.movementOverride(),
                runtime.tracker(),
                runtime.damage(),
                player.getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FuguKnockoutRuntime.reset();
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        FuguKnockoutRuntime runtime;
        try {
            runtime = FuguKnockoutRuntime.get();
        } catch (IllegalStateException uninitialised) {
            return;
        }
        if (!isBossEntity(entity, runtime.config().prolongedBossTag())) {
            return;
        }
        if (runtime.bossLinks().isLinkedToAnyPlayer(entity.getUUID())) {
            runtime.prolonged().onBossDespawn(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            applyMovementOverride(player);
        }
    }

    private static void applyMovementOverride(ServerPlayer player) {
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        ReviveState state = runtime.revive().snapshotState(player.getUUID());
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }
        if (runtime.restrictions().isRestricted(state)) {
            double current = speedAttr.getBaseValue();
            if (current > KnockoutRestrictionService.KO_WALK_SPEED) {
                runtime.movementOverride().remember(player.getUUID(), current);
                speedAttr.setBaseValue(KnockoutRestrictionService.KO_WALK_SPEED);
            }
        } else {
            OptionalDouble stored = runtime.movementOverride().forget(player.getUUID());
            if (stored.isPresent()) {
                speedAttr.setBaseValue(stored.getAsDouble());
            }
        }
    }

    // Helpers

    private static KnockoutPlayerSnapshot snapshotOf(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        boolean hasAnchor = isSoulAnchor(held) || hasSoulAnchorInHotbar(player);
        return new KnockoutPlayerSnapshot(
                player.getUUID(),
                FuguKnockoutRuntime.get().revive().snapshotState(player.getUUID()),
                player.blockPosition().immutable(),
                player.serverLevel().dimension().location(),
                player.getInventory().selected,
                hasAnchor,
                player.getHealth(),
                player.getMaxHealth());
    }

    private static boolean hasSoulAnchorInHotbar(ServerPlayer player) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (isSoulAnchor(player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSoulAnchor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && (com.fuguteams.fugureviveme.FuguReviveMe.MOD_ID + ":soul_anchor").equals(id.toString());
    }

    private static String currentBiomeId(ServerLevel level, BlockPos position) {
        var biomeHolder = level.getBiome(position);
        ResourceLocation id = biomeHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return id == null ? "" : id.toString();
    }

    private static Optional<UUID> findNearestBoss(ServerLevel level, BlockPos position, String bossTagString, double radius) {
        TagKey<EntityType<?>> bossTag = parseBossTag(bossTagString)
                .map(id -> TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, id))
                .orElse(null);
        if (bossTag == null) {
            return Optional.empty();
        }
        AABB search = new AABB(position).inflate(radius);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, search, e -> e.getType().is(bossTag));
        return entities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(position.getX(), position.getY(), position.getZ())))
                .map(Entity::getUUID);
    }

    static Optional<ResourceLocation> parseBossTag(String bossTagString) {
        if (bossTagString == null || bossTagString.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(bossTagString));
    }

    private static void sampleSafePositions(FuguKnockoutRuntime runtime) {
        MinecraftServer server = MinecraftServerGuard.getServer();
        if (server == null) {
            return;
        }
        long gameTime = server.overworld().getGameTime();
        if (gameTime % LastSafePositionTracker.SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ReviveState state = runtime.revive().snapshotState(player.getUUID());
            if (state != ReviveState.ALIVE) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            boolean safe = isSafe(player, pos);
            runtime.tracker().sample(player.getUUID(), gameTime, pos, safe);
        }
    }

    private static boolean isSafe(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && player.onGround();
    }

    private static AllyReviveService.KoTickInputs forgeTickInputs(FuguKnockoutRuntime runtime) {
        MinecraftServer server = MinecraftServerGuard.getServer();
        return new AllyReviveService.KoTickInputs() {
            @Override
            public Optional<KnockoutPlayerSnapshot> snapshotOf(UUID playerUuid) {
                ServerPlayer player = playerByUuid(server, playerUuid);
                return Optional.ofNullable(player).map(KoEventHandlers::snapshotOf);
            }

            @Override
            public void applyResurrectionSickness(UUID targetUuid) {
                ServerPlayer target = playerByUuid(server, targetUuid);
                if (target == null) {
                    return;
                }
                target.addEffect(new MobEffectInstance(
                        ModEffects.RESURRECTION_SICKNESS.get(),
                        runtime.config().resurrectionSicknessDurationTicks()));
            }

            @Override
            public void restoreHealthToPercent(KnockoutPlayerSnapshot snapshot, double percent) {
                ServerPlayer target = playerByUuid(server, snapshot.playerUuid());
                if (target == null) {
                    return;
                }
                float newHealth = (float) (snapshot.maxHealth() * (percent / 100.0));
                target.setHealth(newHealth);
            }
        };
    }

    private static SoulAnchorService.KoTickInputs forgeSoulAnchorInputs(FuguKnockoutRuntime runtime) {
        MinecraftServer server = MinecraftServerGuard.getServer();
        return new SoulAnchorService.KoTickInputs() {
            @Override
            public Optional<KnockoutPlayerSnapshot> snapshotOf(UUID playerUuid) {
                ServerPlayer player = playerByUuid(server, playerUuid);
                return Optional.ofNullable(player).map(KoEventHandlers::snapshotOf);
            }

            @Override
            public void applyResurrectionSickness(UUID targetUuid) {
                ServerPlayer target = playerByUuid(server, targetUuid);
                if (target == null) {
                    return;
                }
                target.addEffect(new MobEffectInstance(
                        ModEffects.RESURRECTION_SICKNESS.get(),
                        runtime.config().resurrectionSicknessDurationTicks()));
            }

            @Override
            public void restoreHealthToPercent(KnockoutPlayerSnapshot snapshot, double percent) {
                ServerPlayer target = playerByUuid(server, snapshot.playerUuid());
                if (target == null) {
                    return;
                }
                float newHealth = (float) (snapshot.maxHealth() * (percent / 100.0));
                target.setHealth(newHealth);
            }

            @Override
            public void consumeSoulAnchor(UUID targetUuid, int slot) {
                ServerPlayer target = playerByUuid(server, targetUuid);
                if (target == null) {
                    return;
                }
                ItemStack stack = target.getInventory().getItem(slot);
                if (isSoulAnchor(stack)) {
                    stack.shrink(1);
                }
            }
        };
    }

    private static ServerPlayer playerByUuid(MinecraftServer server, UUID playerUuid) {
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(playerUuid);
    }

    static void clearTransientPlayerState(
            KnockoutActionRegistry actionRegistry,
            MovementOverrideRegistry movementOverride,
            LastSafePositionTracker tracker,
            KnockoutDamageTracker damage,
            UUID playerUuid) {
        Objects.requireNonNull(actionRegistry, "actionRegistry");
        Objects.requireNonNull(movementOverride, "movementOverride");
        Objects.requireNonNull(tracker, "tracker");
        Objects.requireNonNull(damage, "damage");
        Objects.requireNonNull(playerUuid, "playerUuid");
        actionRegistry.cancelTarget(playerUuid);
        actionRegistry.cancelHelper(playerUuid);
        movementOverride.forget(playerUuid);
        tracker.forget(playerUuid);
        damage.clear(playerUuid);
    }
}
