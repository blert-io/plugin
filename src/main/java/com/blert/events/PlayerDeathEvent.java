package com.blert.events;

import com.blert.raid.rooms.Room;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class PlayerDeathEvent extends Event {
    final String username;

    public PlayerDeathEvent(Room room, int tick, WorldPoint point, String username) {
        super(EventType.PLAYER_DEATH, room, tick, point);
        this.username = username;
    }

    @Override
    protected String eventDataString() {
        return "player=" + username;
    }
}
