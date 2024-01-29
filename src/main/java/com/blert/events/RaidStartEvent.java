package com.blert.events;

import com.blert.raid.Mode;
import lombok.Getter;

import java.util.List;

@Getter
public class RaidStartEvent extends Event {
    private final List<String> party;
    private final Mode mode;

    public RaidStartEvent(List<String> party, Mode mode) {
        super(EventType.RAID_START);
        this.party = party;
        this.mode = mode;
    }

    @Override
    protected String eventDataString() {
        return "party=" + party.toString() + ", mode=" + mode;
    }
}