package com.fuguteams.fugureviveme.state;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KnockoutSavedDataTest {
    private static KoRecord record(ReviveState state, long deadline, UUID boss) {
        return new KoRecord(state, deadline, 1,
                ResourceLocation.parse("minecraft:overworld"), BlockPos.ZERO, Optional.ofNullable(boss));
    }

    @Test
    void roundTripRebuildsBossAndDeadlineIndexes() {
        UUID player = UUID.randomUUID();
        UUID boss = UUID.randomUUID();
        KnockoutSavedData source = new KnockoutSavedData();
        source.put(player, record(ReviveState.PROLONGED_KO, 50, boss));

        CompoundTag serialized = source.save(new CompoundTag());
        KnockoutSavedData loaded = KnockoutSavedData.load(serialized);

        assertEquals(KnockoutSavedData.DATA_VERSION, serialized.getInt("DataVersion"));
        assertEquals(source.get(player), loaded.get(player));
        assertEquals(Set.of(player), loaded.playersLinkedToBoss(boss));
        assertEquals(List.of(player), loaded.pollDue(50));
    }

    @Test
    void replacementMakesOldDeadlineStaleAndRemovalCleansIndexes() {
        UUID player = UUID.randomUUID();
        UUID boss = UUID.randomUUID();
        KnockoutSavedData data = new KnockoutSavedData();
        data.put(player, record(ReviveState.TEMPORARY_KO, 10, boss));
        data.setDirty(false);
        data.put(player, record(ReviveState.TEMPORARY_KO, 20, boss));

        assertTrue(data.pollDue(10).isEmpty());
        assertEquals(List.of(player), data.pollDue(20));
        assertTrue(data.remove(player));
        assertEquals(Set.of(), data.playersLinkedToBoss(boss));
        assertTrue(data.get(player).isEmpty());
    }

    @Test
    void equalUpdateAndIdlePollDoNotMarkDirty() {
        UUID player = UUID.randomUUID();
        KoRecord record = record(ReviveState.FULLY_DOWNED, 100, null);
        KnockoutSavedData data = new KnockoutSavedData();
        data.put(player, record);
        data.setDirty(false);

        data.put(player, record);
        data.pollDue(99);

        assertFalse(data.isDirty());
    }

    @Test
    void aliveTransitionRemovesDurableRecord() {
        UUID player = UUID.randomUUID();
        KnockoutSavedData data = new KnockoutSavedData();
        data.put(player, record(ReviveState.TEMPORARY_KO, 10, null));

        data.transitionToAlive(player);

        assertTrue(data.get(player).isEmpty());
    }
}
