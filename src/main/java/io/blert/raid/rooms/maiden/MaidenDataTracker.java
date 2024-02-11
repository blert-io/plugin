/*
 * Copyright (c) 2023-2024 Alexei Frolov
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

package io.blert.raid.rooms.maiden;

import io.blert.events.MaidenBloodSplatsEvent;
import io.blert.events.MaidenCrabLeakEvent;
import io.blert.events.MaidenCrabSpawnEvent;
import io.blert.raid.*;
import io.blert.raid.rooms.BasicRoomNpc;
import io.blert.raid.rooms.Room;
import io.blert.raid.rooms.RoomDataTracker;
import io.blert.raid.rooms.RoomNpc;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j

public class MaidenDataTracker extends RoomDataTracker {
    private final static int MAIDEN_BLOODSPLAT_GRAPHIC_ID = 1579;
    private final static int MAIDEN_BLOOD_TRAIL_OBJECT_ID = 32984;

    private CrabSpawn currentSpawn = CrabSpawn.SEVENTIES;
    private final int[] spawnTicks = new int[3];

    private @Nullable BasicRoomNpc maiden;

    private final Set<GameObject> bloodTrails = new HashSet<>();
    private final Map<Integer, MaidenCrab> crabs = new HashMap<>();

    public MaidenDataTracker(RaidManager manager, Client client) {
        super(manager, client, Room.MAIDEN);
    }

    @Override
    protected void onRoomStart() {
        if (maiden != null) {
            raidManager.updateRaidMode(maiden.getRaidMode());
        }
    }

    @Override
    protected void onTick() {
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

        final int tick = getRoomTick();

        if (!bloodSplats.isEmpty()) {
            dispatchEvent(new MaidenBloodSplatsEvent(tick, bloodSplats));
        }

        if (maiden == null) {
            return;
        }

        for (MaidenCrab crab : crabs.values()) {
            NPC crabNpc = crab.getNpc();

            if (!crabNpc.isDead() && crabNpc.getWorldArea().distanceTo2D(maiden.getNpc().getWorldArea()) <= 1) {
                dispatchEvent(new MaidenCrabLeakEvent(tick, getWorldLocation(crabNpc), crab));
            }
        }
    }

    @Override
    protected void onGameObjectSpawn(GameObjectSpawned spawned) {
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

    @Override
    protected void onGameObjectDespawn(GameObjectDespawned despawned) {
        GameObject object = despawned.getGameObject();
        if (object.getId() == MAIDEN_BLOOD_TRAIL_OBJECT_ID) {
            bloodTrails.remove(object);
        }
    }

    @Override
    protected Optional<? extends RoomNpc> onNpcSpawn(NpcSpawned spawned) {
        NPC npc = spawned.getNpc();
        return TobNpc.withId(npc.getId()).flatMap(tobNpc -> {
            if (TobNpc.isMaiden(tobNpc.getId())) {
                startRoom();
                maiden = new BasicRoomNpc(npc, tobNpc, generateRoomId(npc),
                        new Hitpoints(tobNpc.getBaseHitpoints(raidManager.getRaidScale())));
                return Optional.of(maiden);
            }

            if (TobNpc.isMaidenMatomenos(tobNpc.getId())) {
                return handleMaidenCrabSpawn(npc);
            }

            if (TobNpc.isMaidenBloodSpawn(tobNpc.getId())) {
                return handleMaidenBloodSpawnSpawn(npc);
            }

            return Optional.empty();
        });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, RoomNpc roomNpc) {
        NPC npc = despawned.getNpc();
        crabs.remove(npc.hashCode());
        // Every maiden NPC despawn is final.
        return true;
    }

    private Optional<RoomNpc> handleMaidenBloodSpawnSpawn(NPC npc) {
        return TobNpc.withId(npc.getId()).map(tobNpc ->
                new BasicRoomNpc(npc, tobNpc, generateRoomId(npc),
                        new Hitpoints(tobNpc.getBaseHitpoints(raidManager.getRaidScale()))));
    }

    private Optional<MaidenCrab> handleMaidenCrabSpawn(NPC npc) {
        if (spawnTicks[currentSpawn.ordinal()] == 0) {
            markNewSpawn();
        } else if (spawnTicks[currentSpawn.ordinal()] != getRoomTick()) {
            currentSpawn = currentSpawn.next();
            markNewSpawn();
        }

        WorldPoint spawnLocation = WorldPoint.fromLocalInstance(client, Utils.getNpcSouthwestTile(npc));

        Optional<MaidenCrab> maybeCrab = MaidenCrab.fromSpawnLocation(
                raidManager.getRaidScale(), npc, generateRoomId(npc), currentSpawn, spawnLocation);
        if (maybeCrab.isPresent()) {
            MaidenCrab maidenCrab = maybeCrab.get();
            log.debug("Crab position: " + maidenCrab.getPosition() + " scuffed: " + maidenCrab.isScuffed());
            crabs.put(npc.hashCode(), maidenCrab);
            dispatchEvent(new MaidenCrabSpawnEvent(getRoomTick(), currentSpawn, maidenCrab));
        }

        return maybeCrab;
    }

    private void markNewSpawn() {
        spawnTicks[currentSpawn.ordinal()] = getRoomTick();
        log.debug("Maiden " + currentSpawn + " spawned on tick " + getRoomTick() + " (" + formattedRoomTime() + ")");
    }
}
