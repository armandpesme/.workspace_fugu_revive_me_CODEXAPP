package com.fuguteams.fugureviveme.command;

import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuguDebugCommandsTest {
    @Test
    void testRecordUsesTargetPositionAndConfiguredDuration() {
        KoRecord record = FuguDebugCommands.createTestRecord(
                ReviveState.PROLONGED_KO,
                6_000,
                ResourceLocation.parse("minecraft:overworld"),
                new BlockPos(10, 64, -4),
                200);

        assertEquals(ReviveState.PROLONGED_KO, record.state());
        assertEquals(6_200, record.deadlineGameTime());
        assertEquals(ResourceLocation.parse("minecraft:overworld"), record.koDimension());
        assertEquals(new BlockPos(10, 64, -4), record.koPosition());
        assertTrue(record.linkedBossUuid().isEmpty());
    }

    @Test
    void testRecordRejectsNonKoStates() {
        assertThrows(IllegalArgumentException.class, () -> FuguDebugCommands.createTestRecord(
                ReviveState.ALIVE,
                1,
                ResourceLocation.parse("minecraft:overworld"),
                BlockPos.ZERO,
                0));
    }

    @Test
    void commandsKeepExpectedPermissionAndLiteralNames() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/fuguteams/fugureviveme/command/FuguDebugCommands.java"));

        assertTrue(source.contains("REQUIRED_PERMISSION_LEVEL = 4"));
        assertTrue(source.contains("Commands.literal(\"fugurevive\")"));
        assertTrue(source.contains("Commands.literal(\"kotemporaire\")"));
        assertTrue(source.contains("Commands.literal(\"kopermanant\")"));
    }
}
