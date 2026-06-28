package com.fuguteams.fugureviveme.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientUiFormatTest {
    @Test
    void formatTicksRoundsUpToDisplayedSeconds() {
        assertEquals("01:00", ClientUiFormat.formatTicks(1_200));
        assertEquals("00:01", ClientUiFormat.formatTicks(1));
        assertEquals("00:00", ClientUiFormat.formatTicks(0));
    }

    @Test
    void progressIsClamped() {
        assertEquals(1.0F, ClientUiFormat.progress(120, 100));
        assertEquals(0.5F, ClientUiFormat.progress(50, 100));
        assertEquals(0.0F, ClientUiFormat.progress(-5, 100));
        assertEquals(0.0F, ClientUiFormat.progress(50, 0));
    }

    @Test
    void timerColorsFollowTemporaryAndProlongedThresholds() {
        assertEquals(ClientUiFormat.COLOR_AMBER, ClientUiFormat.temporaryTimerColor(620));
        assertEquals(ClientUiFormat.COLOR_ORANGE, ClientUiFormat.temporaryTimerColor(400));
        assertEquals(ClientUiFormat.COLOR_RED, ClientUiFormat.temporaryTimerColor(200));
        assertEquals(ClientUiFormat.COLOR_AMBER, ClientUiFormat.prolongedTimerColor(2_500));
        assertEquals(ClientUiFormat.COLOR_ORANGE, ClientUiFormat.prolongedTimerColor(1_200));
        assertEquals(ClientUiFormat.COLOR_RED, ClientUiFormat.prolongedTimerColor(600));
    }
}
