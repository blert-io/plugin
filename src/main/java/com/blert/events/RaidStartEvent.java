package com.blert.events;

public class RaidStartEvent extends Event {
    public RaidStartEvent() {
        super(EventType.RAID_START, 0, null);
    }
}