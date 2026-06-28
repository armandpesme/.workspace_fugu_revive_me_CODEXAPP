package com.fuguteams.fugureviveme.server;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReturnPendantCastTest {

    @Test
    void recordCarriesAllFields() {
        UUID player = UUID.randomUUID();
        Vec3 pos = new Vec3(1, 2, 3);
        ReturnPendantCast cast = new ReturnPendantCast(player, pos, 4, 100L);

        assertEquals(player, cast.playerUuid());
        assertEquals(pos, cast.startPosition());
        assertEquals(4, cast.startSlot());
        assertEquals(100L, cast.deadline());
    }

    @Test
    void rejectsNullPlayerUuid() {
        assertThrows(NullPointerException.class, () ->
                new ReturnPendantCast(null, Vec3.ZERO, 0, 0L));
    }

    @Test
    void rejectsNullStartPosition() {
        assertThrows(NullPointerException.class, () ->
                new ReturnPendantCast(UUID.randomUUID(), null, 0, 0L));
    }

    @Test
    void rejectsNegativeDeadline() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReturnPendantCast(UUID.randomUUID(), Vec3.ZERO, 0, -1L));
    }

    @Test
    void acceptsZeroDeadline() {
        ReturnPendantCast cast = new ReturnPendantCast(UUID.randomUUID(), Vec3.ZERO, 0, 0L);
        assertEquals(0L, cast.deadline());
    }
}
