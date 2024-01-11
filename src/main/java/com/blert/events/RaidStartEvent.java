package com.blert.events;

public class RaidStartEvent extends Event {
    public RaidStartEvent() {
        super(EventType.RAID_START);
    }

    @Override
    protected String eventDataString() {
        return null;
    }
}