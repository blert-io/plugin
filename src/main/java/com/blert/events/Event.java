package com.blert.events;

import com.blert.raid.rooms.Room;
import joptsimple.internal.Strings;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class Event {
    private final @Getter EventType type;
    private final @Nullable Room room;
    private final @Getter int tick;
    private final @Nullable WorldPoint coords;

    protected Event(EventType type) {
        this(type, null, 0, null);
    }

    protected Event(EventType type, @Nullable Room room, int tick, @Nullable WorldPoint point) {
        this.type = type;
        this.room = room;
        this.tick = tick;
        this.coords = point;
    }

    public Optional<Room> getRoom() {
        return Optional.ofNullable(room);
    }

    public int getXCoord() {
        return coords != null ? coords.getX() : 0;
    }

    public int getYCoord() {
        return coords != null ? coords.getY() : 0;
    }

    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("Event(");
        string.append("type=").append(type);
        string.append(", tick=").append(tick);
        string.append(", coords=");
        if (coords != null) {
            string.append('(').append(coords.getX()).append(',').append(coords.getY()).append(')');
        } else {
            string.append("null");
        }
        string.append(", room=").append(room);

        String eventData = eventDataString();
        if (!Strings.isNullOrEmpty(eventData)) {
            string.append(", ").append(eventData);
        }

        string.append(")");
        return string.toString();
    }

    /**
     * Returns a string representation of the data tracked by the event. A null or empty string indicates no data.
     */
    protected abstract String eventDataString();
}
