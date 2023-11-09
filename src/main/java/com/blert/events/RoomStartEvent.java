package com.blert.events;

import com.blert.raid.Room;
import lombok.Getter;

@Getter
public class RoomStartEvent extends Event {
    private final Room room;

    public RoomStartEvent(Room room) {
        super(EventType.ROOM_START, 0, null);
        this.room = room;
    }

    @Override
    protected String eventDataString() {
        return "room=" + room;
    }
}
