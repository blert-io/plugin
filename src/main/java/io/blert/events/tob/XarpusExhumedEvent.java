package io.blert.events.tob;

import io.blert.challenges.tob.rooms.Room;
import io.blert.events.EventType;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

@Getter
public class XarpusExhumedEvent extends TobEvent {
    private final int spawnTick;
    private final int healAmount;
    private final List<Integer> healTicks;

    public XarpusExhumedEvent(int tick, WorldPoint point, int spawnTick, int healAmount, List<Integer> healTicks) {
        super(EventType.XARPUS_EXHUMED, Room.XARPUS, tick, point);
        this.spawnTick = spawnTick;
        this.healAmount = healAmount;
        this.healTicks = healTicks;
    }

    @Override
    public String eventDataString() {
        return "spawnTick=" + spawnTick +
                ", healAmount=" + healAmount +
                ", healTicks=" + healTicks;
    }
}
