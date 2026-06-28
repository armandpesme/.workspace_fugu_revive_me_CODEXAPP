package com.fuguteams.fugureviveme.client;

public final class ClientUiFormat {
    static final int COLOR_AMBER = 0xFFFFB347;
    static final int COLOR_ORANGE = 0xFFFF6B00;
    static final int COLOR_RED = 0xFFFF0000;

    private ClientUiFormat() {
        throw new AssertionError("ClientUiFormat is a static utility and must not be instantiated");
    }

    public static String formatTicks(long ticks) {
        long seconds = Math.max(0L, (ticks + 19L) / 20L);
        return "%02d:%02d".formatted(seconds / 60L, seconds % 60L);
    }

    public static float progress(long remainingTicks, long totalTicks) {
        if (totalTicks <= 0L) {
            return 0.0F;
        }
        float ratio = (float) remainingTicks / (float) totalTicks;
        return Math.max(0.0F, Math.min(1.0F, ratio));
    }

    public static int temporaryTimerColor(long remainingTicks) {
        long seconds = Math.max(0L, remainingTicks / 20L);
        if (seconds <= 10L) {
            return COLOR_RED;
        }
        if (seconds <= 30L) {
            return COLOR_ORANGE;
        }
        return COLOR_AMBER;
    }

    public static int prolongedTimerColor(long remainingTicks) {
        long seconds = Math.max(0L, remainingTicks / 20L);
        if (seconds <= 30L) {
            return COLOR_RED;
        }
        if (seconds <= 120L) {
            return COLOR_ORANGE;
        }
        return COLOR_AMBER;
    }
}
