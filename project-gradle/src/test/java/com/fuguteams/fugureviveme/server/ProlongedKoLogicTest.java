package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.BiomeKoClassifier;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProlongedKoLogicTest {

    private static KoRecord record(ReviveState state, long deadline, Optional<UUID> boss) {
        return new KoRecord(
                state, deadline, 0,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                boss);
    }

    @Test
    void selectInitialStateReturnsProlongedWhenBiomeIsProlongedAndBossFound() {
        ProlongedKoLogic.InitialState initial = ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.PROLONGED, true, 1_200L, 6_000L);
        assertInstanceOf(ProlongedKoLogic.ProlongedChoice.class, initial);
    }

    @Test
    void selectInitialStateReturnsTemporaryWhenBiomeProlongedWithoutBoss() {
        ProlongedKoLogic.InitialState initial = ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.PROLONGED, false, 1_200L, 6_000L);
        assertInstanceOf(ProlongedKoLogic.TemporaryChoice.class, initial);
    }

    @Test
    void selectInitialStateReturnsTemporaryWhenBiomeIsTemporaryRegardlessOfBoss() {
        ProlongedKoLogic.InitialState withBoss = ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, true, 1_200L, 6_000L);
        ProlongedKoLogic.InitialState withoutBoss = ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, false, 1_200L, 6_000L);
        assertInstanceOf(ProlongedKoLogic.TemporaryChoice.class, withBoss);
        assertInstanceOf(ProlongedKoLogic.TemporaryChoice.class, withoutBoss);
    }

    @Test
    void selectInitialStateReturnsRejectWhenBiomeIsNotEligible() {
        ProlongedKoLogic.InitialState initial = ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.NONE, true, 1_200L, 6_000L);
        assertInstanceOf(ProlongedKoLogic.RejectChoice.class, initial);
    }

    @Test
    void selectInitialStateRejectsNullClassification() {
        assertThrows(NullPointerException.class, () -> ProlongedKoLogic.selectInitialState(
                null, false, 1_200L, 6_000L));
    }

    @Test
    void selectInitialStateRejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, () -> ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, false, 0L, 6_000L));
        assertThrows(IllegalArgumentException.class, () -> ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, false, -1L, 6_000L));
        assertThrows(IllegalArgumentException.class, () -> ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, false, 1_200L, 0L));
        assertThrows(IllegalArgumentException.class, () -> ProlongedKoLogic.selectInitialState(
                BiomeKoClassifier.KoType.TEMPORARY, false, 1_200L, -1L));
    }

    @Test
    void transitionOnBossDeathMovesProlongedToPendingReviveWithShortDeadline() {
        UUID boss = UUID.randomUUID();
        KoRecord current = record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss));
        long now = 5_000L;

        Optional<KoRecord> next = ProlongedKoLogic.transitionOnBossDeath(current, now);

        assertTrue(next.isPresent());
        assertEquals(ReviveState.PENDING_REVIVE, next.get().state());
        assertEquals(now + 1L, next.get().deadlineGameTime());
        assertEquals(Optional.of(boss), next.get().linkedBossUuid());
    }

    @Test
    void transitionOnBossDeathIgnoresNonProlongedStates() {
        for (ReviveState state : ReviveState.values()) {
            if (state == ReviveState.PROLONGED_KO || state == ReviveState.ALIVE) {
                continue;
            }
            KoRecord current = record(state, 1_000L, Optional.empty());
            assertFalse(ProlongedKoLogic.transitionOnBossDeath(current, 0L).isPresent(),
                    "expected no transition for " + state);
        }
    }

    @Test
    void transitionOnBossDeathRejectsNegativeNow() {
        KoRecord current = record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> ProlongedKoLogic.transitionOnBossDeath(current, -1L));
    }

    @Test
    void transitionOnBossDespawnMovesProlongedToPendingDeath() {
        UUID boss = UUID.randomUUID();
        KoRecord current = record(ReviveState.PROLONGED_KO, 1_000L, Optional.of(boss));

        Optional<KoRecord> next = ProlongedKoLogic.transitionOnBossDespawn(current);

        assertTrue(next.isPresent());
        assertEquals(ReviveState.PENDING_DEATH, next.get().state());
        assertEquals(1_000L, next.get().deadlineGameTime());
    }

    @Test
    void transitionOnBossDespawnIgnoresNonProlongedStates() {
        KoRecord temporary = record(ReviveState.TEMPORARY_KO, 1_000L, Optional.empty());
        KoRecord fullyDowned = record(ReviveState.FULLY_DOWNED, 1_000L, Optional.empty());
        KoRecord pending = record(ReviveState.PENDING_DEATH, 1_000L, Optional.empty());
        assertFalse(ProlongedKoLogic.transitionOnBossDespawn(temporary).isPresent());
        assertFalse(ProlongedKoLogic.transitionOnBossDespawn(fullyDowned).isPresent());
        assertFalse(ProlongedKoLogic.transitionOnBossDespawn(pending).isPresent());
    }

    @Test
    void transitionOnDimensionChangeMovesProlongedAndFullyDownedToPendingDeath() {
        KoRecord prolonged = record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty());
        KoRecord fullyDowned = record(ReviveState.FULLY_DOWNED, 1_000L, Optional.empty());

        assertEquals(ReviveState.PENDING_DEATH,
                ProlongedKoLogic.transitionOnDimensionChange(prolonged).orElseThrow().state());
        assertEquals(ReviveState.PENDING_DEATH,
                ProlongedKoLogic.transitionOnDimensionChange(fullyDowned).orElseThrow().state());
    }

    @Test
    void transitionOnDimensionChangeIgnoresTemporaryAndOtherStates() {
        KoRecord temporary = record(ReviveState.TEMPORARY_KO, 1_000L, Optional.empty());
        KoRecord pending = record(ReviveState.PENDING_DEATH, 1_000L, Optional.empty());
        KoRecord pendingRevive = record(ReviveState.PENDING_REVIVE, 1_000L, Optional.empty());
        assertFalse(ProlongedKoLogic.transitionOnDimensionChange(temporary).isPresent());
        assertFalse(ProlongedKoLogic.transitionOnDimensionChange(pending).isPresent());
        assertFalse(ProlongedKoLogic.transitionOnDimensionChange(pendingRevive).isPresent());
    }

    @Test
    void transitionOnTimeoutMovesDueExpiringStatesToPendingDeath() {
        for (ReviveState state : new ReviveState[]{
                ReviveState.TEMPORARY_KO,
                ReviveState.PROLONGED_KO,
                ReviveState.FULLY_DOWNED}) {
            KoRecord current = record(state, 1_000L, Optional.empty());
            Optional<KoRecord> next = ProlongedKoLogic.transitionOnTimeout(current, 1_000L);
            assertTrue(next.isPresent(), "expected transition for " + state);
            assertEquals(ReviveState.PENDING_DEATH, next.get().state());
        }
    }

    @Test
    void transitionOnTimeoutDoesNothingWhenBeforeDeadline() {
        KoRecord current = record(ReviveState.TEMPORARY_KO, 1_000L, Optional.empty());
        assertFalse(ProlongedKoLogic.transitionOnTimeout(current, 999L).isPresent());
    }

    @Test
    void transitionOnTimeoutIgnoresNonExpiringStatesEvenWhenPastDeadline() {
        for (ReviveState state : new ReviveState[]{
                ReviveState.PENDING_REVIVE,
                ReviveState.PENDING_DEATH,
                ReviveState.DEAD_PENDING_TRANSFER}) {
            KoRecord current = record(state, 0L, Optional.empty());
            assertFalse(ProlongedKoLogic.transitionOnTimeout(current, 10_000L).isPresent(),
                    "expected no transition for " + state);
        }
    }

    @Test
    void transitionOnTimeoutRejectsNegativeNow() {
        KoRecord current = record(ReviveState.TEMPORARY_KO, 1_000L, Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> ProlongedKoLogic.transitionOnTimeout(current, -1L));
    }

    @Test
    void extendOnFullyDownedTransitionsToFullyDownedAndExtendsDeadline() {
        KoRecord current = record(ReviveState.PROLONGED_KO, 1_000L, Optional.empty());
        KoRecord next = ProlongedKoLogic.extendOnFullyDowned(current);
        assertEquals(ReviveState.FULLY_DOWNED, next.state());
        assertEquals(1, next.hitsTaken());
        assertEquals(1_000L + KnockoutStateLogic.FULLY_DOWNED_EXTENSION_TICKS, next.deadlineGameTime());
    }

    @Test
    void extendOnFullyDownedRejectsNonKoStates() {
        KoRecord alive = record(ReviveState.PENDING_REVIVE, 1_000L, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> ProlongedKoLogic.extendOnFullyDowned(alive));
    }
}
