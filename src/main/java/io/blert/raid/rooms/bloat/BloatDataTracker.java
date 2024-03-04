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
import io.blert.events.NpcAttackEvent;
import io.blert.raid.Hitpoints;
import io.blert.raid.NpcAttack;
import io.blert.raid.RaidManager;
import io.blert.raid.TobNpc;
import io.blert.raid.rooms.BasicRoomNpc;
import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.RoomDataTracker;
import io.blert.raid.rooms.RoomNpc;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import java.util.Optional;

@Slf4j

public class BloatDataTracker extends RoomDataTracker {
    private static final int BLOAT_DOWN_ANIMATION = 8082;
    private static final int BLOAT_DOWN_CYCLE_TICKS = 32;
    private static final int BLOAT_STOMP_TICK = 3;

    private enum State {
        WALKING,
        DOWN,
    }

    private State state;
    private BasicRoomNpc bloat;

    private int currentDown;
    private int currentDownTick;
    private int lastDownTick;
    private int lastUpTick;

    public BloatDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.BLOAT, true);
        state = State.WALKING;
        currentDown = 0;
        lastDownTick = -1;
        lastUpTick = -1;
        bloat = null;
    }

    @Override
    protected void onRoomStart() {
        if (bloat != null && bloat.getNpc().getAnimation() == BLOAT_DOWN_ANIMATION) {
            state = State.DOWN;
        } else {
            state = State.WALKING;
        }
    }

    @Override
    protected void onTick() {
        final int tick = getRoomTick();

        if (state == State.DOWN) {
            if (lastDownTick == tick) {
                currentDownTick = BLOAT_DOWN_CYCLE_TICKS;
            } else {
                currentDownTick--;
            }

            if (currentDownTick == BLOAT_STOMP_TICK && bloat != null) {
                WorldPoint point = getWorldLocation(bloat);
                dispatchEvent(new NpcAttackEvent(getRoom(), tick, point, NpcAttack.BLOAT_STOMP, bloat));
            }
        }
    }

    @Override
    protected Optional<? extends RoomNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();

        return TobNpc.withId(npc.getId())
                .filter(tobNpc -> TobNpc.isBloat(tobNpc.getId()))
                .map(tobNpc -> {
                    bloat = new BasicRoomNpc(npc, tobNpc, generateRoomId(npc),
                            new Hitpoints(tobNpc, raidManager.getRaidScale()));
                    return bloat;
                });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, RoomNpc roomNpc) {
        return roomNpc == bloat;
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
        state = State.DOWN;
        lastDownTick = tick;
        log.debug("Bloat down {} tick {}", currentDown, lastDownTick);

        dispatchEvent(new BloatDownEvent(tick, getWorldLocation(bloat), tick - lastUpTick));
    }

    private void handleBloatUp(NPC bloat, int tick) {
        lastUpTick = tick;
        state = State.WALKING;
        log.debug("Bloat up {} tick {}", currentDown, lastUpTick);

        dispatchEvent(new BloatUpEvent(tick, getWorldLocation(bloat)));
    }
}
