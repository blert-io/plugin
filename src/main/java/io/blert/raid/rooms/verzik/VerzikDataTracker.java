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

package io.blert.raid.rooms.verzik;

import io.blert.events.VerzikPhaseEvent;
import io.blert.events.VerzikRedsSpawnEvent;
import io.blert.raid.RaidManager;
import io.blert.raid.TobNpc;
import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.RoomDataTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class VerzikDataTracker extends RoomDataTracker {
    private static final int P3_TRANSITION_ANIMATION = 8118;

    private VerzikPhase phase;

    private int redCrabsTick;
    private int redCrabSpawnCount;
    private final Set<NPC> redCrabs = new HashSet<>();

    public VerzikDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.VERZIK);
        this.phase = VerzikPhase.IDLE;
        this.redCrabsTick = -1;
        this.redCrabSpawnCount = 0;
    }

    @Override
    protected void onRoomStart() {
        this.phase = VerzikPhase.P1;
    }

    @Override
    protected void onTick() {
        final int tick = getRoomTick();

        if (tick == redCrabsTick) {
            if (redCrabSpawnCount == 1) {
                log.debug("Reds: {} ({})", tick, formattedRoomTime());
            }

            // TODO(frolv): Add `redCrabs` NPCs to the event?
            dispatchEvent(new VerzikRedsSpawnEvent(tick));
        }
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        final int tick = getRoomTick();

        if (TobNpc.isVerzikMatomenos(npc.getId())) {
            if (tick != redCrabsTick) {
                redCrabsTick = tick;
                redCrabSpawnCount++;
            }

            redCrabs.add(npc);
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();

        if (TobNpc.isVerzikMatomenos(npc.getId())) {
            redCrabs.remove(npc);
        }
    }

    @Subscribe
    private void onNpcChanged(NpcChanged changed) {
        int beforeId = changed.getOld().getId();
        int afterId = changed.getNpc().getId();

        final int tick = getRoomTick();

        if (TobNpc.isVerzikIdle(beforeId) && TobNpc.isVerzikP1(afterId)) {
            startRoom();
        } else if (TobNpc.isVerzikP1(beforeId) && TobNpc.isVerzikP2(afterId)) {
            phase = VerzikPhase.P2;
            dispatchEvent(new VerzikPhaseEvent(tick, phase));
            log.debug("P2: {} ({})", tick, formattedRoomTime());
        }
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        final int npcId = ((NPC) actor).getId();
        final int tick = getRoomTick();

        if (phase == VerzikPhase.P2 && TobNpc.isVerzikP2(npcId) && actor.getAnimation() == P3_TRANSITION_ANIMATION) {
            phase = VerzikPhase.P3;
            dispatchEvent(new VerzikPhaseEvent(tick, phase));
            log.debug("P3: {} ({})", tick, formattedRoomTime());
        }
    }
}
