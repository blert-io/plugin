package com.blert.events;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

public abstract class Event {
    private final @Getter EventType type;
    private final @Getter int tick;
    private final @Nullable WorldPoint coords;

    protected Event(EventType type, int tick, WorldPoint point) {
        this.type = type;
        this.tick = tick;
        this.coords = point;
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
            string.append("NULL");
        }

        String eventData = eventDataString();
        if (!eventData.isEmpty()) {
            string.append(", ").append(eventData);
        }

        string.append(")");
        return string.toString();
    }

    protected String eventDataString() { return ""; }
}
