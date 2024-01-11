package com.blert.events;

import com.blert.raid.Hitpoints;
import com.blert.raid.rooms.Room;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Optional;

public class NpcUpdateEvent extends Event {
    @Getter
    private final int roomId;
    @Getter
    private final int npcId;
    private final @Nullable Hitpoints hitpoints;

    public NpcUpdateEvent(Room room, int tick, WorldPoint point, int roomId, int npcId, @Nullable Hitpoints hitpoints) {
        super(EventType.NPC_UPDATE, room, tick, point);
        this.roomId = roomId;
        this.npcId = npcId;
        this.hitpoints = hitpoints;
    }

    public Optional<Hitpoints> getHitpoints() {
        return Optional.ofNullable(hitpoints);
    }

    @Override
    protected String eventDataString() {
        return "npc=" + npcId;
    }
}
