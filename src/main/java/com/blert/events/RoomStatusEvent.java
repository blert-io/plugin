package com.blert.events;

import com.blert.raid.rooms.Room;
import lombok.Getter;

@Getter
public class RoomStatusEvent extends Event {
    public enum Status {
        STARTED,
        COMPLETED,
        WIPED,
    }

    private final Status status;

    public RoomStatusEvent(Room room, int tick, Status status) {
        super(EventType.ROOM_STATUS, room, tick, null);
        this.status = status;
    }

    @Override
    protected String eventDataString() {
        return "status=" + status;
    }
}
