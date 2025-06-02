package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VerzikYellowsEvent extends TobEvent {
    private final List<WorldPoint> yellows;

    public VerzikYellowsEvent(int tick, List<WorldPoint> yellows) {
        super(EventType.VERZIK_YELLOWS, Room.VERZIK, tick, null);
        this.yellows = yellows;
    }

    @Override
    public String eventDataString() {
        String yellows = this.yellows
                .stream()
                .map(wp -> "(" + wp.getX() + "," + wp.getY() + ")")
                .collect(Collectors.joining(","));
        return "yellows=[" + yellows + ']';
    }
}
