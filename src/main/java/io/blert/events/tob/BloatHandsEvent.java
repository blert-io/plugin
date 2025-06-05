package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BloatHandsEvent extends TobEvent {
    private final List<WorldPoint> hands;

    public static BloatHandsEvent drop(int tick, List<WorldPoint> hands) {
        return new BloatHandsEvent(EventType.BLOAT_HANDS_DROP, tick, hands);
    }

    public static BloatHandsEvent splat(int tick, List<WorldPoint> hands) {
        return new BloatHandsEvent(EventType.BLOAT_HANDS_SPLAT, tick, hands);
    }

    private BloatHandsEvent(EventType type, int tick, List<WorldPoint> hands) {
        super(type, Room.BLOAT, tick, null);
        this.hands = hands;
    }

    @Override
    public String eventDataString() {
        String hands = this.hands
                .stream()
                .map(wp -> "(" + wp.getX() + "," + wp.getY() + ")")
                .collect(Collectors.joining(","));
        return "hands=[" + hands + ']';
    }
}
