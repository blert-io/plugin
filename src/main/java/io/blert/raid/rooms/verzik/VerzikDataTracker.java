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
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class VerzikDataTracker extends RoomDataTracker {
    private static final int P3_TRANSITION_ANIMATION = 8118;

    private VerzikPhase phase;

    private BasicRoomNpc verzik;

    private int redCrabsTick;
    private int redCrabSpawnCount;
    private final Set<VerzikCrab> crabs = new HashSet<>();
    private final Set<BasicRoomNpc> redCrabs = new HashSet<>();

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

    @Override
    protected Optional<? extends RoomNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();
        final int tick = getRoomTick();

        var maybeNpc = TobNpc.withId(npc.getId());
        if (maybeNpc.isEmpty()) {
            return Optional.empty();
        }
        TobNpc tobNpc = maybeNpc.get();

        if (tobNpc.isAnyVerzik()) {
            long roomId = verzik != null ? verzik.getRoomId() : generateRoomId(npc);
            verzik = new BasicRoomNpc(npc, tobNpc, roomId, new Hitpoints(tobNpc, raidManager.getRaidScale()));
            return Optional.of(verzik);
        }

        if (tobNpc.isVerzikCrab()) {
            WorldPoint point = getWorldLocation(npc);
            long roomId = generateRoomId(npc);
            VerzikCrab crab = VerzikCrab.fromSpawnedNpc(npc, tobNpc, roomId, point, raidManager.getRaidScale(), phase);

            crabs.add(crab);
            return Optional.of(crab);
        }

        if (tobNpc.isVerzikMatomenos()) {
            if (tick != redCrabsTick) {
                redCrabsTick = tick;
                redCrabSpawnCount++;
            }

            BasicRoomNpc crab = new BasicRoomNpc(npc, tobNpc, generateRoomId(npc),
                    new Hitpoints(tobNpc, raidManager.getRaidScale()));
            redCrabs.add(crab);
            return Optional.of(crab);
        }

        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, RoomNpc roomNpc) {
        NPC npc = event.getNpc();

        if (TobNpc.isVerzikMatomenos(npc.getId())) {
            return roomNpc instanceof BasicRoomNpc && redCrabs.remove(roomNpc);
        }

        if (TobNpc.isVerzikCrab(npc.getId())) {
            return roomNpc instanceof VerzikCrab && crabs.remove(roomNpc);
        }

        // Verzik despawns between phases, but it should not be counted as a final despawn until the end of the fight.
        return roomNpc == verzik && phase == VerzikPhase.P3;
    }

    @Override
    protected void onNpcChange(NpcChanged changed) {
        NPC npc = changed.getNpc();
        int beforeId = changed.getOld().getId();

        final int tick = getRoomTick();

        var maybeVerzik = TobNpc.withId(npc.getId());
        if (maybeVerzik.isEmpty()) {
            return;
        }
        TobNpc tobNpc = maybeVerzik.get();

        if (TobNpc.isVerzikIdle(beforeId) && tobNpc.isVerzikP1()) {
            startRoom();
            return;
        }

        if (TobNpc.isVerzikP1(beforeId) && tobNpc.isVerzikP2()) {
            // A transition from P1 to P2 does not spawn a new NPC. Simply reset Verzik's HP to its P2 value.
            phase = VerzikPhase.P2;
            verzik.setHitpoints(new Hitpoints(tobNpc, raidManager.getRaidScale()));

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
