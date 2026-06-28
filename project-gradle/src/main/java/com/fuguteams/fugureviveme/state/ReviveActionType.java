package com.fuguteams.fugureviveme.state;

public enum ReviveActionType {
    NONE,
    ALLY_REVIVE,
    SELF_REVIVE,
    RETURN_PENDANT;

    public static ReviveActionType fromWire(int value) {
        ReviveActionType[] values = values();
        if (value < 0 || value >= values.length) {
            throw new IllegalArgumentException("Unknown revive action type: " + value);
        }
        return values[value];
    }
}
