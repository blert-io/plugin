/*
 * Copyright (c) 2023 Alexei Frolov
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

package com.blert.raid.rooms.maiden;

import com.blert.events.MaidenBloodSplatsEvent;
import com.blert.events.MaidenCrabLeakEvent;
import com.blert.events.MaidenCrabSpawnEvent;
import com.blert.events.NpcUpdateEvent;
import com.blert.raid.*;
import com.blert.raid.rooms.Room;
import com.blert.raid.rooms.RoomDataTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j

public class MaidenDataTracker extends RoomDataTracker {
    private final static int MAIDEN_BLOODSPLAT_GRAPHIC_ID = 1579;
    private final static int MAIDEN_BLOOD_TRAIL_OBJECT_ID = 32984;

    private CrabSpawn currentSpawn = CrabSpawn.SEVENTIES;
    private final int[] spawnTicks = new int[3];

    private @Nullable NPC maiden = null;

    private final Set<GameObject> bloodTrails = new HashSet<>();
    private final Map<Integer, MaidenCrab> crabs = new HashMap<>();

    public MaidenDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.MAIDEN);
    }

    @Override
    protected void onRoomStart() {
        findMaidenNpc();
    }

    @Override
    protected void onTick() {
        findMaidenNpc();

        List<WorldPoint> bloodSplats = new ArrayList<>();

        // Search for active blood splats thrown by Maiden, and report them if they exist.
        for (GraphicsObject object : client.getGraphicsObjects()) {
            WorldPoint point = WorldPoint.fromLocalInstance(client, object.getLocation());
            if (Location.fromWorldPoint(point).inMaiden() && object.getId() == MAIDEN_BLOODSPLAT_GRAPHIC_ID) {
                bloodSplats.add(point);
            }
        }

        // Add any blood trails left by blood spawns to the list. In the future, this may be a separate event.
        for (GameObject trail : bloodTrails) {
            bloodSplats.add(WorldPoint.fromLocalInstance(client, trail.getLocalLocation()));
        }

        if (!bloodSplats.isEmpty()) {
            dispatchEvent(new MaidenBloodSplatsEvent(getRoomTick(), bloodSplats));
        }

        for (MaidenCrab crab : crabs.values()) {
            NPC crabNpc = crab.getNpc();
            WorldPoint point = WorldPoint.fromLocalInstance(client, Utils.getNpcSouthwestTile(crabNpc));
            Hitpoints hitpoints = crab.getHitpoints();

            dispatchNpcUpdate(new NpcUpdateEvent(
                    Room.MAIDEN, getRoomTick(), point, crabNpc.hashCode(), crabNpc.getId(), new Hitpoints(hitpoints)));

            if (maiden != null) {
                if (!crabNpc.isDead() && crabNpc.getWorldArea().distanceTo2D(maiden.getWorldArea()) <= 1) {
                    dispatchEvent(new MaidenCrabLeakEvent(getRoomTick(), point, crab));
                }
            }
        }
    }

    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned spawned) {
        // The blood trails left by blood spawns are game objects with stable hash codes, so store them in a set when
        // they spawn and remove them on de-spawn. The `onTick` handler will dispatch events with the active set of
        // blood trails.
        GameObject object = spawned.getGameObject();
        if (object.getId() != MAIDEN_BLOOD_TRAIL_OBJECT_ID) {
            return;
        }

        WorldPoint point = WorldPoint.fromLocalInstance(client, spawned.getTile().getLocalLocation());
        if (Location.fromWorldPoint(point).inMaiden()) {
            bloodTrails.add(object);
        }
    }

    @Subscribe
    private void onGameObjectDespawned(GameObjectDespawned despawned) {
        GameObject object = despawned.getGameObject();
        if (object.getId() == MAIDEN_BLOOD_TRAIL_OBJECT_ID) {
            bloodTrails.remove(object);
        }
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned spawned) {
        NPC npc = spawned.getNpc();

        if (TobNpc.isMaiden(npc.getId())) {
            startRoom();
            return;
        }

        if (TobNpc.isMaidenMatomenos(npc.getId())) {
            handleMaidenCrabSpawn(npc);
        } else if (TobNpc.isMaidenBloodSpawn(npc.getId())) {
            handleMaidenBloodSpawnSpawn(npc);
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned despawned) {
        NPC npc = despawned.getNpc();
        crabs.remove(npc.hashCode());
    }

    @Override
    protected void onHitsplat(HitsplatApplied hitsplatApplied) {
        Optional.ofNullable(crabs.get(hitsplatApplied.getActor().hashCode())).ifPresent(crab -> {
            crab.getHitpoints().drain(hitsplatApplied.getHitsplat().getAmount());
        });
    }

    private void handleMaidenBloodSpawnSpawn(NPC npc) {
    }

    private void handleMaidenCrabSpawn(NPC npc) {
        if (spawnTicks[currentSpawn.ordinal()] == 0) {
            markNewSpawn();
        } else if (spawnTicks[currentSpawn.ordinal()] != getRoomTick()) {
            currentSpawn = currentSpawn.next();
            markNewSpawn();
        }

        WorldPoint spawnLocation = WorldPoint.fromLocalInstance(client, Utils.getNpcSouthwestTile(npc));

        MaidenCrab.fromSpawnLocation(raidManager.getRaidScale(), npc, currentSpawn, spawnLocation)
                .ifPresent(maidenCrab -> {
                    log.debug("Crab position: " + maidenCrab.getPosition() + " scuffed: " + maidenCrab.isScuffed());
                    crabs.put(npc.hashCode(), maidenCrab);
                    dispatchEvent(new MaidenCrabSpawnEvent(getRoomTick(), currentSpawn, maidenCrab));
                });
    }

    private void markNewSpawn() {
        spawnTicks[currentSpawn.ordinal()] = getRoomTick();
        log.debug("Maiden " + currentSpawn + " spawned on tick " + getRoomTick() + " (" + formattedRoomTime() + ")");
    }

    private void findMaidenNpc() {
        client.getNpcs().stream().filter(npc -> TobNpc.isMaiden(npc.getId())).findFirst().ifPresent(npc -> {
            maiden = npc;
            TobNpc.withId(npc.getId()).ifPresent(tobNpc -> raidManager.updateRaidMode(tobNpc.getMode()));
        });
    }
}
