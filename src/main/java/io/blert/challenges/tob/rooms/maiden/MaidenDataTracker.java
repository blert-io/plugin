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

package io.blert.challenges.tob.rooms.maiden;

import io.blert.challenges.tob.Location;
import io.blert.challenges.tob.NpcAttack;
import io.blert.challenges.tob.RaidManager;
import io.blert.challenges.tob.TobNpc;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import io.blert.core.TrackedNpc;
import io.blert.core.TrackedNpcCollection;
import io.blert.events.MaidenBloodSplatsEvent;
import io.blert.events.MaidenCrabLeakEvent;
import io.blert.events.NpcAttackEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j

public class MaidenDataTracker extends RoomDataTracker {
    private final static int MAIDEN_BLOODSPLAT_GRAPHIC_ID = 1579;
    private final static int MAIDEN_BLOOD_TRAIL_OBJECT_ID = 32984;

    private final static int MAIDEN_BLOOD_THROW_ANIMATION = 8091;
    private final static int MAIDEN_AUTO_ANIMATION = 8092;

    private final static WorldPoint MAIDEN_WORLD_LOCATION = new WorldPoint(3162, 4444, 0);

    private CrabSpawn currentSpawn = CrabSpawn.SEVENTIES;
    private final int[] spawnTicks = new int[3];
    private boolean crabsSpawnedThisTick = false;

    private @Nullable BasicTrackedNpc maiden;

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
            bloodSplats.add(getWorldLocation(trail));
        }

        final int tick = getTick();

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

        crabsSpawnedThisTick = false;
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

        if (Location.fromWorldPoint(getWorldLocation(object)).inMaiden()) {
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
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned) {
        NPC npc = spawned.getNpc();
        return TobNpc.withId(npc.getId()).flatMap(tobNpc -> {
            if (TobNpc.isMaiden(tobNpc.getId())) {
                startRoom();

                // Due to the loading line at Maiden (thanks Jagex), use a static location for her ID.
                long roomId = TrackedNpcCollection.npcRoomId(getTick(), npc.getId(), MAIDEN_WORLD_LOCATION);
                maiden = new BasicTrackedNpc(npc, tobNpc, roomId,
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
    protected boolean onNpcDespawn(NpcDespawned despawned, TrackedNpc trackedNpc) {
        NPC npc = despawned.getNpc();
        crabs.remove(npc.hashCode());
        // Every maiden NPC despawn is final.
        return true;
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        Actor actor = event.getActor();
        if (maiden == null || actor != maiden.getNpc()) {
            return;
        }

        final int tick = getTick();

        NpcAttack attack;
        switch (actor.getAnimation()) {
            case MAIDEN_BLOOD_THROW_ANIMATION:
                attack = NpcAttack.MAIDEN_BLOOD_THROW;
                break;
            case MAIDEN_AUTO_ANIMATION:
                attack = NpcAttack.MAIDEN_AUTO;
                break;
            default:
                return;
        }

        dispatchEvent(new NpcAttackEvent(getRoom(), tick, getWorldLocation(actor), attack, maiden));
    }

    private Optional<TrackedNpc> handleMaidenBloodSpawnSpawn(NPC npc) {
        return TobNpc.withId(npc.getId()).map(tobNpc ->
                new BasicTrackedNpc(npc, tobNpc, generateRoomId(npc),
                        new Hitpoints(tobNpc.getBaseHitpoints(raidManager.getRaidScale()))));
    }

    private Optional<MaidenCrab> handleMaidenCrabSpawn(NPC npc) {
        if (!crabsSpawnedThisTick) {
            markNewSpawn();
            crabsSpawnedThisTick = true;
        }

        WorldPoint spawnLocation = getWorldLocation(npc);

        Optional<MaidenCrab> maybeCrab = MaidenCrab.fromSpawnLocation(
                raidManager.getRaidScale(), npc, generateRoomId(npc), currentSpawn, spawnLocation);
        if (maybeCrab.isPresent()) {
            MaidenCrab maidenCrab = maybeCrab.get();
            log.debug("Crab position: " + maidenCrab.getPosition() + " scuffed: " + maidenCrab.isScuffed());
            crabs.put(npc.hashCode(), maidenCrab);
        }

        return maybeCrab;
    }

    private void markNewSpawn() {
        if (maiden != null) {
            // Correct the crab spawn based on Maiden's current hitpoints in case the room was started late.
            // The percentage thresholds are set higher than the spawn in case Maiden has already healed when
            // joining the room, but not high enough to be falsely triggered by a deep proc.
            double hp = maiden.getHitpoints().percentage();
            if (hp < 39.0) {
                currentSpawn = CrabSpawn.THIRTIES;
            } else if (hp < 59.0) {
                currentSpawn = CrabSpawn.FIFTIES;
            } else {
                currentSpawn = CrabSpawn.SEVENTIES;
            }
        }

        spawnTicks[currentSpawn.ordinal()] = getTick();
        log.debug("Maiden " + currentSpawn + " spawned on tick " + getTick() + " (" + formattedRoomTime() + ")");
    }
}
