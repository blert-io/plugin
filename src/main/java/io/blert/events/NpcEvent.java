/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.events;

import io.blert.core.Hitpoints;
import io.blert.challenges.tob.rooms.Room;
import io.blert.core.TrackedNpc;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class NpcEvent extends Event {
    private final long roomId;
    private final int npcId;
    private final Hitpoints hitpoints;
    private final TrackedNpc.Properties properties;

    public static NpcEvent spawn(Room room, int tick, WorldPoint point, TrackedNpc trackedNpc) {
        return new NpcEvent(EventType.NPC_SPAWN, room, tick, point, trackedNpc);
    }

    public static NpcEvent update(Room room, int tick, WorldPoint point, TrackedNpc trackedNpc) {
        return new NpcEvent(EventType.NPC_UPDATE, room, tick, point, trackedNpc);
    }

    public static NpcEvent death(Room room, int tick, WorldPoint point, TrackedNpc trackedNpc) {
        return new NpcEvent(EventType.NPC_DEATH, room, tick, point, trackedNpc);
    }

    protected NpcEvent(EventType type, Room room, int tick, WorldPoint point, TrackedNpc trackedNpc) {
        super(type, room, tick, point);
        this.roomId = trackedNpc.getRoomId();
        this.npcId = trackedNpc.getNpcId();
        this.hitpoints = trackedNpc.getHitpoints();
        this.properties = trackedNpc.getProperties();
    }

    @Override
    protected String eventDataString() {
        return "npc=(npcId=" + npcId + ", roomId=" + roomId + ", hitpoints=" + hitpoints + ")";
    }
}
