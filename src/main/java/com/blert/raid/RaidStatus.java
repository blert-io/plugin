package com.blert.raid;

public enum RaidStatus {
    INACTIVE,
    ACTIVE,
    COMPLETE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
