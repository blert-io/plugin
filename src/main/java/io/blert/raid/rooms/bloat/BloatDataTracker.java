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

package io.blert.raid.rooms.bloat;

import io.blert.events.BloatDownEvent;
import io.blert.events.BloatUpEvent;
import io.blert.raid.RaidManager;
import io.blert.raid.TobNpc;
import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.RoomDataTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;

@Slf4j

public class BloatDataTracker extends RoomDataTracker {
    private static final int BLOAT_DOWN_ANIMATION = 8082;

    int currentDown;
    int lastDownTick;
    int lastUpTick;

    public BloatDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.BLOAT, true);
        currentDown = 0;
        lastUpTick = 0;
    }

    @Override
    protected void onRoomStart() {
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) actor;
        if (!TobNpc.isBloat(npc.getId())) {
            return;
        }

        final int tick = getRoomTick();
        if (npc.getAnimation() == BLOAT_DOWN_ANIMATION) {
            handleBloatDown(npc, tick);
        } else if (npc.getAnimation() == -1) {
            handleBloatUp(npc, tick);
        }
    }

    private void handleBloatDown(NPC bloat, int tick) {
        currentDown++;
        lastDownTick = tick;
        log.debug("Bloat down {} tick {}", currentDown, lastDownTick);

        WorldPoint point = WorldPoint.fromLocalInstance(client, bloat.getLocalLocation());
        dispatchEvent(new BloatDownEvent(tick, point, tick - lastUpTick));
    }

    private void handleBloatUp(NPC bloat, int tick) {
        lastUpTick = tick;
        log.debug("Bloat up {} tick {}", currentDown, lastUpTick);

        WorldPoint point = WorldPoint.fromLocalInstance(client, bloat.getLocalLocation());
        dispatchEvent(new BloatUpEvent(tick, point));
    }
}
