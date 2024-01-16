package com.blert.raid;

public enum RaidState {
    INACTIVE,
    ACTIVE,
    COMPLETE;

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public boolean isActive() {
        return !isInactive();
    }
}
