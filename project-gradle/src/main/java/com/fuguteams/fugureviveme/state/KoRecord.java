package com.fuguteams.fugureviveme.state;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record KoRecord(
        ReviveState state,
        long deadlineGameTime,
        int hitsTaken,
        ResourceLocation koDimension,
        BlockPos koPosition,
        Optional<UUID> linkedBossUuid
) {
    private static final String STATE = "State";
    private static final String DEADLINE = "Deadline";
    private static final String HITS = "Hits";
    private static final String DIMENSION = "Dimension";
    private static final String POSITION = "Position";
    private static final String BOSS = "Boss";

    public KoRecord {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(koDimension, "koDimension");
        Objects.requireNonNull(koPosition, "koPosition");
        linkedBossUuid = Objects.requireNonNull(linkedBossUuid, "linkedBossUuid");
        if (state == ReviveState.ALIVE) {
            throw new IllegalArgumentException("ALIVE is represented by the absence of a saved record");
        }
        if (deadlineGameTime < 0) {
            throw new IllegalArgumentException("deadlineGameTime must be non-negative");
        }
        if (hitsTaken < 0) {
            throw new IllegalArgumentException("hitsTaken must be non-negative");
        }
    }

    public KoRecord transitionTo(ReviveState nextState) {
        return new KoRecord(nextState, deadlineGameTime, hitsTaken, koDimension, koPosition, linkedBossUuid);
    }

    public KoRecord withDeadline(long nextDeadlineGameTime) {
        return new KoRecord(state, nextDeadlineGameTime, hitsTaken, koDimension, koPosition, linkedBossUuid);
    }

    public KoRecord withHitsTaken(int nextHitsTaken) {
        return new KoRecord(state, deadlineGameTime, nextHitsTaken, koDimension, koPosition, linkedBossUuid);
    }

    public KoRecord withLinkedBoss(Optional<UUID> nextLinkedBossUuid) {
        return new KoRecord(state, deadlineGameTime, hitsTaken, koDimension, koPosition, nextLinkedBossUuid);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(STATE, state.name());
        tag.putLong(DEADLINE, deadlineGameTime);
        tag.putInt(HITS, hitsTaken);
        tag.putString(DIMENSION, koDimension.toString());
        tag.putLong(POSITION, koPosition.asLong());
        linkedBossUuid.ifPresent(uuid -> tag.putUUID(BOSS, uuid));
        return tag;
    }

    public static Optional<KoRecord> load(CompoundTag tag) {
        try {
            if (!tag.contains(STATE, Tag.TAG_STRING)
                    || !tag.contains(DEADLINE, Tag.TAG_LONG)
                    || !tag.contains(HITS, Tag.TAG_INT)
                    || !tag.contains(DIMENSION, Tag.TAG_STRING)
                    || !tag.contains(POSITION, Tag.TAG_LONG)) {
                return Optional.empty();
            }
            ReviveState state = ReviveState.valueOf(tag.getString(STATE));
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
            if (dimension == null) {
                return Optional.empty();
            }
            Optional<UUID> boss = tag.hasUUID(BOSS) ? Optional.of(tag.getUUID(BOSS)) : Optional.empty();
            return Optional.of(new KoRecord(
                    state,
                    tag.getLong(DEADLINE),
                    tag.getInt(HITS),
                    dimension,
                    BlockPos.of(tag.getLong(POSITION)),
                    boss));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }
}
