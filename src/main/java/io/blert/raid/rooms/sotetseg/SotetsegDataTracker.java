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

package io.blert.raid.rooms.sotetseg;

import io.blert.events.SoteMazeProcEvent;
import io.blert.raid.Hitpoints;
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
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import java.util.Optional;

@Slf4j
public class SotetsegDataTracker extends RoomDataTracker {
    private static final int MAZE_TELEPORT_ANIMATION = 1816;

    private final int[] mazeTicks = new int[]{-1, -1};
    private Maze maze = Maze.MAZE_66;

    public SotetsegDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.SOTETSEG);
    }

    @Override
    protected void onRoomStart() {
    }

    @Override
    protected void onTick() {
        final int tick = getRoomTick();
        if (mazeTicks[maze.ordinal()] == tick) {
            // Advance to the next maze after all the teleport animation handlers have run.
            maze = Maze.MAZE_33;
        }
    }

    @Override
    protected Optional<? extends RoomNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();

        return TobNpc.withId(npc.getId())
                .filter(tobNpc -> TobNpc.isSotetsegIdle(tobNpc.getId()))
                .map(tobNpc -> new BasicRoomNpc(npc, tobNpc, generateRoomId(npc),
                        new Hitpoints(tobNpc, raidManager.getRaidScale())));
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, RoomNpc roomNpc) {
        return true;
    }

    @Override
    protected void onNpcChange(NpcChanged changed) {
        if (TobNpc.isSotetsegIdle(changed.getOld().getId()) && TobNpc.isSotetseg(changed.getNpc().getId())) {
            startRoom();
        }
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        Actor actor = event.getActor();
        int animationId = actor.getAnimation();
        if (!(actor instanceof Player) || animationId != MAZE_TELEPORT_ANIMATION) {
            return;
        }

        if (mazeTicks[maze.ordinal()] == -1) {
            final int tick = getRoomTick();

            mazeTicks[maze.ordinal()] = tick;
            dispatchEvent(new SoteMazeProcEvent(tick, maze));
            log.debug("Sotetseg {} procced on tick {} {}", maze, tick, formattedRoomTime());
        }
    }
}
