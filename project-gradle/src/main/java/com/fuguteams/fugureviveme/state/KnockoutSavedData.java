package com.fuguteams.fugureviveme.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class KnockoutSavedData extends SavedData {
    public static final int DATA_VERSION = 1;
    private static final String DATA_NAME = "fugu_revive_me_knockouts";
    private static final String VERSION = "DataVersion";
    private static final String RECORDS = "Records";
    private static final String PLAYER = "Player";
    private static final String RECORD = "Record";

    private final Map<UUID, KoRecord> records = new HashMap<>();
    private final Map<UUID, Set<UUID>> playersByBoss = new HashMap<>();
    private final DeadlineIndex deadlines = new DeadlineIndex();

    public static KnockoutSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(KnockoutSavedData::load, KnockoutSavedData::new, DATA_NAME);
    }

    public static KnockoutSavedData load(CompoundTag root) {
        KnockoutSavedData data = new KnockoutSavedData();
        if (root.getInt(VERSION) > DATA_VERSION) {
            return data;
        }
        ListTag list = root.getList(RECORDS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (!entry.hasUUID(PLAYER) || !entry.contains(RECORD, Tag.TAG_COMPOUND)) {
                continue;
            }
            UUID playerUuid;
            try {
                playerUuid = entry.getUUID(PLAYER);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            KoRecord.load(entry.getCompound(RECORD))
                    .ifPresent(record -> data.install(playerUuid, record));
        }
        data.setDirty(false);
        return data;
    }

    public Optional<KoRecord> get(UUID playerUuid) {
        return Optional.ofNullable(records.get(playerUuid));
    }

    public void put(UUID playerUuid, KoRecord record) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(record, "record");
        KoRecord previous = records.get(playerUuid);
        if (record.equals(previous)) {
            return;
        }
        if (previous != null) {
            unindexBoss(playerUuid, previous);
        }
        install(playerUuid, record);
        setDirty();
    }

    public boolean remove(UUID playerUuid) {
        KoRecord previous = records.remove(playerUuid);
        if (previous == null) {
            return false;
        }
        unindexBoss(playerUuid, previous);
        setDirty();
        return true;
    }

    public void transitionToAlive(UUID playerUuid) {
        remove(playerUuid);
    }

    public Set<UUID> playersLinkedToBoss(UUID bossUuid) {
        return Set.copyOf(playersByBoss.getOrDefault(bossUuid, Set.of()));
    }

    public List<UUID> pollDue(long now) {
        return deadlines.pollDue(now, this::currentExpiringDeadline);
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt(VERSION, DATA_VERSION);
        ListTag list = new ListTag();
        records.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag serialized = new CompoundTag();
                    serialized.putUUID(PLAYER, entry.getKey());
                    serialized.put(RECORD, entry.getValue().save());
                    list.add(serialized);
                });
        root.put(RECORDS, list);
        return root;
    }

    private void install(UUID playerUuid, KoRecord record) {
        records.put(playerUuid, record);
        record.linkedBossUuid().ifPresent(boss ->
                playersByBoss.computeIfAbsent(boss, ignored -> new HashSet<>()).add(playerUuid));
        if (record.state().hasExpiringKoDeadline()) {
            deadlines.schedule(playerUuid, record.deadlineGameTime());
        }
    }

    private void unindexBoss(UUID playerUuid, KoRecord record) {
        record.linkedBossUuid().ifPresent(boss -> {
            Set<UUID> players = playersByBoss.get(boss);
            if (players != null) {
                players.remove(playerUuid);
                if (players.isEmpty()) {
                    playersByBoss.remove(boss);
                }
            }
        });
    }

    private Long currentExpiringDeadline(UUID playerUuid) {
        KoRecord record = records.get(playerUuid);
        return record != null && record.state().hasExpiringKoDeadline()
                ? record.deadlineGameTime()
                : null;
    }
}
