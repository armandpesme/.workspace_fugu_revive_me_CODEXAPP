package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class ReviveService {
    private final Supplier<KnockoutSavedData> data;
    private final LongSupplier overworldClock;
    private final ReviveSyncService sync;
    private final Function<UUID, OptionalInt> entityIdLookup;

    public ReviveService(
            Supplier<KnockoutSavedData> data,
            LongSupplier overworldClock,
            ReviveSyncService sync
    ) {
        this(data, overworldClock, sync, ignored -> OptionalInt.empty());
    }

    public ReviveService(
            Supplier<KnockoutSavedData> data,
            LongSupplier overworldClock,
            ReviveSyncService sync,
            Function<UUID, OptionalInt> entityIdLookup
    ) {
        this.data = Objects.requireNonNull(data, "data");
        this.overworldClock = Objects.requireNonNull(overworldClock, "overworldClock");
        this.sync = Objects.requireNonNull(sync, "sync");
        this.entityIdLookup = Objects.requireNonNull(entityIdLookup, "entityIdLookup");
    }

    public ReviveState snapshotState(UUID playerUuid) {
        return data.get().get(playerUuid).map(KoRecord::state).orElse(ReviveState.ALIVE);
    }

    public Optional<KoRecord> snapshot(UUID playerUuid) {
        return data.get().get(playerUuid);
    }

    public boolean requestReleaseSpirit(ServerPlayer player) {
        return requestReleaseSpirit(player.getUUID(), OptionalInt.of(player.getId()));
    }

    public boolean requestReleaseSpirit(UUID playerUuid) {
        return requestReleaseSpirit(playerUuid, entityIdLookup.apply(playerUuid));
    }

    private boolean requestReleaseSpirit(UUID playerUuid, OptionalInt entityId) {
        KnockoutSavedData savedData = data.get();
        Optional<KoRecord> current = savedData.get(playerUuid);
        if (current.isEmpty()) {
            return false;
        }
        ReviveState state = current.get().state();
        if (state != ReviveState.PROLONGED_KO && state != ReviveState.FULLY_DOWNED) {
            return false;
        }
        KoRecord transitioned = current.get().transitionTo(ReviveState.PENDING_DEATH);
        savedData.put(playerUuid, transitioned);
        publishTransition(playerUuid, entityId, transitioned, overworldClock.getAsLong());
        return true;
    }

    public int tickExpirations() {
        long now = overworldClock.getAsLong();
        KnockoutSavedData savedData = data.get();
        int transitions = 0;
        for (UUID playerUuid : savedData.pollDue(now)) {
            Optional<KoRecord> current = savedData.get(playerUuid);
            if (current.isEmpty() || !current.get().state().hasExpiringKoDeadline()) {
                continue;
            }
            KoRecord transitioned = current.get().transitionTo(ReviveState.PENDING_DEATH);
            savedData.put(playerUuid, transitioned);
            publishTransition(playerUuid, entityIdLookup.apply(playerUuid), transitioned, now);
            transitions++;
        }
        return transitions;
    }

    public void syncSelf(ServerPlayer player) {
        sync.sendSelf(player.getUUID(), data.get().get(player.getUUID()), overworldClock.getAsLong());
    }

    public void syncVisualTo(ServerPlayer recipient, ServerPlayer subject) {
        sync.sendVisualTo(recipient.getUUID(), subject.getId(), snapshotState(subject.getUUID()));
    }

    /**
     * Attempts to intercept a death by entering a knockout state.
     * <p>
     * Returns the freshly created record when the player can be knocked out,
     * or {@link Optional#empty()} when the death must follow the vanilla
     * path (outside an eligible biome or while carrying the resurrection
     * sickness effect).
     */
    public Optional<KoRecord> tryEnterKnockoutOnDeath(
            UUID playerUuid,
            ResourceLocation dimension,
            BlockPos position,
            BiomeKoClassifier.KoType biomeType,
            boolean hasResurrectionSickness,
            Optional<UUID> nearbyBoss,
            int durationTicks,
            int maxHits
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(biomeType, "biomeType");
        Objects.requireNonNull(nearbyBoss, "nearbyBoss");
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                dimension,
                position,
                biomeType,
                hasResurrectionSickness,
                nearbyBoss,
                overworldClock.getAsLong());
        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, durationTicks, maxHits);
        if (outcome instanceof KnockoutStateLogic.Created created) {
            KnockoutSavedData savedData = data.get();
            savedData.put(playerUuid, created.record());
            publishTransition(playerUuid, entityIdLookup.apply(playerUuid), created.record(),
                    overworldClock.getAsLong());
            return Optional.of(created.record());
        }
        return Optional.empty();
    }

    /**
     * Applies a hit to a knocked-out player. The record must already be in
     * a {@link ReviveState#TEMPORARY_KO} or {@link ReviveState#PROLONGED_KO}
     * state. Returns the updated record, or {@link Optional#empty()} when no
     * transition is applied (no record, already fully downed, or hit ignored).
     */
    public Optional<KoRecord> applyKnockoutHit(UUID playerUuid, int maxHits) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        KnockoutSavedData savedData = data.get();
        Optional<KoRecord> current = savedData.get(playerUuid);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        KnockoutStateLogic.HitOutcome outcome = KnockoutStateLogic.applyHit(current.get(), maxHits);
        if (outcome instanceof KnockoutStateLogic.Survived survived) {
            savedData.put(playerUuid, survived.nextRecord());
            publishTransition(playerUuid, entityIdLookup.apply(playerUuid), survived.nextRecord(),
                    overworldClock.getAsLong());
            return Optional.of(survived.nextRecord());
        }
        if (outcome instanceof KnockoutStateLogic.FullyDowned fullyDowned) {
            savedData.put(playerUuid, fullyDowned.nextRecord());
            publishTransition(playerUuid, entityIdLookup.apply(playerUuid), fullyDowned.nextRecord(),
                    overworldClock.getAsLong());
            return Optional.of(fullyDowned.nextRecord());
        }
        return Optional.empty();
    }

    public void transitionToAlive(UUID playerUuid) {
        KnockoutSavedData savedData = data.get();
        Optional<KoRecord> previous = savedData.get(playerUuid);
        if (previous.isEmpty()) {
            return;
        }
        savedData.transitionToAlive(playerUuid);
        long now = overworldClock.getAsLong();
        OptionalInt entityId = entityIdLookup.apply(playerUuid);
        if (entityId.isPresent()) {
            sync.publishAlive(playerUuid, entityId.getAsInt(), now);
        } else {
            sync.sendSelf(playerUuid, Optional.empty(), now);
        }
    }

    public void updateRecord(UUID playerUuid, KoRecord record) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(record, "record");
        KnockoutSavedData savedData = data.get();
        savedData.put(playerUuid, record);
        publishTransition(playerUuid, entityIdLookup.apply(playerUuid), record, overworldClock.getAsLong());
    }

    private void publishTransition(UUID playerUuid, OptionalInt entityId, KoRecord record, long serverGameTime) {
        if (entityId.isPresent()) {
            sync.publishTransition(playerUuid, entityId.getAsInt(), record, serverGameTime);
        } else {
            sync.publishTransition(playerUuid, record, serverGameTime);
        }
    }
}
