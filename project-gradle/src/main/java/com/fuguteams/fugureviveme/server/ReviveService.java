package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KnockoutSavedData;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
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

    private void publishTransition(UUID playerUuid, OptionalInt entityId, KoRecord record, long serverGameTime) {
        if (entityId.isPresent()) {
            sync.publishTransition(playerUuid, entityId.getAsInt(), record, serverGameTime);
        } else {
            sync.publishTransition(playerUuid, record, serverGameTime);
        }
    }
}
