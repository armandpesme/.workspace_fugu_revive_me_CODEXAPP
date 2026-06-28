package com.fuguteams.fugureviveme.state;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KoRecordTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.parse("minecraft:overworld");
    private static final BlockPos POSITION = new BlockPos(12, 64, -8);

    @Test
    void rejectsAliveAndNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new KoRecord(ReviveState.ALIVE, 20, 0, DIMENSION, POSITION, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new KoRecord(ReviveState.TEMPORARY_KO, -1, 0, DIMENSION, POSITION, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new KoRecord(ReviveState.TEMPORARY_KO, 20, -1, DIMENSION, POSITION, Optional.empty()));
    }

    @Test
    void roundTripsNbtAndKeepsImmutableTransitions() {
        UUID boss = UUID.randomUUID();
        KoRecord original = new KoRecord(
                ReviveState.PROLONGED_KO, 120, 2, DIMENSION, POSITION, Optional.of(boss));

        KoRecord loaded = KoRecord.load(original.save()).orElseThrow();
        KoRecord transitioned = loaded.transitionTo(ReviveState.PENDING_DEATH);

        assertEquals(original, loaded);
        assertEquals(ReviveState.PROLONGED_KO, original.state());
        assertEquals(ReviveState.PENDING_DEATH, transitioned.state());
        assertEquals(120, transitioned.deadlineGameTime());
        assertEquals(Optional.of(boss), transitioned.linkedBossUuid());
    }

    @Test
    void malformedNbtIsRejectedWithoutThrowing() {
        assertTrue(KoRecord.load(new net.minecraft.nbt.CompoundTag()).isEmpty());
    }
}
