package com.fuguteams.fugureviveme.state;

public enum ReviveState {
    ALIVE,
    TEMPORARY_KO,
    PROLONGED_KO,
    FULLY_DOWNED,
    PENDING_REVIVE,
    PENDING_DEATH,
    DEAD_PENDING_TRANSFER;

    public boolean hasExpiringKoDeadline() {
        return this == TEMPORARY_KO || this == PROLONGED_KO || this == FULLY_DOWNED;
    }

    public static ReviveState fromWire(int value) {
        ReviveState[] values = values();
        if (value < 0 || value >= values.length) {
            throw new IllegalArgumentException("Unknown revive state: " + value);
        }
        return values[value];
    }
}
