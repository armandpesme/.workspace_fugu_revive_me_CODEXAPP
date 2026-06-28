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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockoutStateLogicTest {

    @Test
    void temporaryBiomeCreatesTemporaryRecordWithConfiguredDeadline() {
        UUID player = UUID.randomUUID();
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(10, 64, -10),
                BiomeKoClassifier.KoType.TEMPORARY,
                false,
                Optional.empty(),
                1_000L);

        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, 1_200, 3);

        KnockoutStateLogic.Created created = assertInstanceOf(KnockoutStateLogic.Created.class, outcome);
        KoRecord record = created.record();
        assertEquals(ReviveState.TEMPORARY_KO, record.state());
        assertEquals(0, record.hitsTaken());
        assertEquals(2_200L, record.deadlineGameTime());
        assertEquals(Optional.empty(), record.linkedBossUuid());
    }

    @Test
    void prolongedBiomeStoresBossUuidWhenPresent() {
        UUID player = UUID.randomUUID();
        UUID boss = UUID.randomUUID();
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                BiomeKoClassifier.KoType.PROLONGED,
                false,
                Optional.of(boss),
                100L);

        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, 6_000, 3);

        KnockoutStateLogic.Created created = assertInstanceOf(KnockoutStateLogic.Created.class, outcome);
        assertEquals(ReviveState.PROLONGED_KO, created.record().state());
        assertEquals(Optional.of(boss), created.record().linkedBossUuid());
        assertEquals(6_100L, created.record().deadlineGameTime());
    }

    @Test
    void resurrectionSicknessAlwaysRejectsKoEntry() {
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                BiomeKoClassifier.KoType.TEMPORARY,
                true,
                Optional.empty(),
                0L);

        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, 1_200, 3);

        KnockoutStateLogic.Rejected rejected = assertInstanceOf(KnockoutStateLogic.Rejected.class, outcome);
        assertEquals(KnockoutStateLogic.RejectionReason.RESURRECTION_SICKNESS, rejected.reason());
    }

    @Test
    void nonEligibleBiomeRejectsKoEntry() {
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                BiomeKoClassifier.KoType.NONE,
                false,
                Optional.empty(),
                0L);

        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, 1_200, 3);

        KnockoutStateLogic.Rejected rejected = assertInstanceOf(KnockoutStateLogic.Rejected.class, outcome);
        assertEquals(KnockoutStateLogic.RejectionReason.BIOME_NOT_ELIGIBLE, rejected.reason());
    }

    @Test
    void hitBelowMaxIncrementsHitCounterAndKeepsState() {
        KoRecord record = new KoRecord(
                ReviveState.TEMPORARY_KO, 1_000L, 0,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                Optional.empty());

        KnockoutStateLogic.HitOutcome outcome = KnockoutStateLogic.applyHit(record, 3);

        KnockoutStateLogic.Survived survived = assertInstanceOf(KnockoutStateLogic.Survived.class, outcome);
        assertEquals(1, survived.nextRecord().hitsTaken());
        assertEquals(ReviveState.TEMPORARY_KO, survived.nextRecord().state());
        assertEquals(1_000L, survived.nextRecord().deadlineGameTime());
    }

    @Test
    void hitAtMaxTransitionsToFullyDownedAndExtendsDeadline() {
        KoRecord record = new KoRecord(
                ReviveState.TEMPORARY_KO, 1_000L, 2,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                Optional.empty());

        KnockoutStateLogic.HitOutcome outcome = KnockoutStateLogic.applyHit(record, 3);

        KnockoutStateLogic.FullyDowned fullyDowned = assertInstanceOf(KnockoutStateLogic.FullyDowned.class, outcome);
        assertEquals(3, fullyDowned.nextRecord().hitsTaken());
        assertEquals(ReviveState.FULLY_DOWNED, fullyDowned.nextRecord().state());
        assertEquals(1_000L + KnockoutStateLogic.FULLY_DOWNED_EXTENSION_TICKS,
                fullyDowned.nextRecord().deadlineGameTime());
    }

    @Test
    void hitOnFullyDownedIsRejected() {
        KoRecord record = new KoRecord(
                ReviveState.FULLY_DOWNED, 1_000L, 3,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                Optional.empty());

        assertInstanceOf(KnockoutStateLogic.HitRejected.class, KnockoutStateLogic.applyHit(record, 3));
    }

    @Test
    void computeEntryRejectsInvalidConfig() {
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                BiomeKoClassifier.KoType.TEMPORARY,
                false,
                Optional.empty(),
                0L);

        assertThrows(IllegalArgumentException.class, () -> KnockoutStateLogic.computeEntry(inputs, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> KnockoutStateLogic.computeEntry(inputs, 100, 0));
    }

    @Test
    void prolongedBiomeWithoutBossRecordsEmptyBossUuid() {
        KnockoutStateLogic.OnDeathInputs inputs = new KnockoutStateLogic.OnDeathInputs(
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(0, 64, 0),
                BiomeKoClassifier.KoType.PROLONGED,
                false,
                Optional.empty(),
                0L);

        KnockoutStateLogic.OnDeathOutcome outcome = KnockoutStateLogic.computeEntry(inputs, 6_000, 3);

        KnockoutStateLogic.Created created = assertInstanceOf(KnockoutStateLogic.Created.class, outcome);
        assertTrue(created.record().linkedBossUuid().isEmpty());
        assertEquals(ReviveState.PROLONGED_KO, created.record().state());
    }
}
