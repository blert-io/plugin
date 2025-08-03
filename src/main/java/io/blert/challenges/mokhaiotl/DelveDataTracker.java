/*
 * Copyright (c) 2025 Alexei Frolov
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

package io.blert.challenges.mokhaiotl;

import com.google.common.collect.ImmutableSet;
import io.blert.core.*;
import io.blert.events.NpcAttackEvent;
import io.blert.events.mokhaiotl.*;
import io.blert.util.Location;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DelveDataTracker extends DataTracker {
    private static final int MOKHAIOTL_AUTO_ANIMATION_ID = 12406;
    private static final int MOKHAIOTL_BALL_ANIMATION_ID = 12407;
    private static final int MOKHAIOTL_CHARGE_ANIMATION_ID = 12409;
    private static final int MOKHAIOTL_BLAST_ANIMATION_ID = 12411;
    private static final int MOKHAIOTL_MELEE_ANIMATION_ID = 12416;
    private static final int MOKHAIOTL_RACECAR_ANIMATION_ID = 12417;

    private static final int MOKHAIOTL_RANGED_BALL_PROJECTILE_ID = 3384;
    private static final int MOKHAIOTL_MAGE_BALL_PROJECTILE_ID = 3385;

    private static final int ACID_BLOOD_GAME_OBJECT_ID = 57283;
    private static final int ROCK_GAME_OBJECT_ID = 57286;

    private static final Set<Number> SHOCKWAVE_GRAPHICS_IDs = ImmutableSet.of(3405, 3406, 3407);
    private static final int RACECAR_TARGET_GRAPHICS_ID = 3415;

    private static final int[] HITPOINTS_BY_DELVE = new int[]{
            -1,  // 1-indexed
            525, // Delve 1
            550, // Delve 2
            575, // Delve 3
            600, // Delve 4
            625, // Delve 5
            650, // Delve 6
            650, // Delve 7
            675, // Delve 8+
    };
    private static final int SHIELD_HITPOINTS = 500;

    private enum PhaseChange {
        NONE,
        SHIELD_START,
        SHIELD_END,
    }

    private final Pattern delveEndRegex;
    private final int delve;

    private BasicTrackedNpc mokhaiotl = null;
    PhaseChange phaseChange;
    private int hitpointsBeforeShield;
    private int unidentifiedAttackTick;
    private int lastRacecarTick;
    private final Map<Projectile, Orb> activeOrbs = new HashMap<>();
    private final Set<GameObject> activeRocksAndSplats = new HashSet<>();
    private final List<GameObject> rocksAndSplatsSpawnedThisTick = new ArrayList<>();
    private final List<GameObject> rocksAndSplatsDespawnedThisTick = new ArrayList<>();
    private final Set<WorldPoint> shockwaveLocationsThisTick = new HashSet<>();
    private final Queue<Hitsplat> healsThisTick = new LinkedList<>();
    private final Queue<TrackedNpc> larvaeLeakedThisTick = new LinkedList<>();

    public static Stage delveToStage(int delve) {
        if (delve > 8) {
            return Stage.MOKHAIOTL_DELVE_8PLUS;
        }
        return Stage.values()[Stage.MOKHAIOTL_DELVE_1.ordinal() + delve - 1];
    }

    public DelveDataTracker(RecordableChallenge challenge, Client client, int delve) {
        super(challenge, client, delveToStage(delve));
        this.delve = delve;
        if (delve > 8) {
            this.delveEndRegex = Pattern.compile("Delve level: 8\\+ \\(" + delve + "\\) duration: ([0-9]{1,2}:[0-9]{2}(\\.[0-9]{2})?)");
        } else {
            this.delveEndRegex = Pattern.compile("Delve level: " + delve + " duration: ([0-9]{1,2}:[0-9]{2}(\\.[0-9]{2})?)");
        }
        this.unidentifiedAttackTick = -1;
        this.lastRacecarTick = -1;
        this.phaseChange = PhaseChange.NONE;
    }

    protected void start() {
        super.start();
        log.info("Starting delve {}", delve);

        // The client doesn't receive `GameObjectSpawned` events for acid already present
        // when entering the delve, so we have to manually grab our initial set.
        Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
        if (tiles.length == 0 || tiles[0].length == 0) {
            log.warn("No tiles available during delve start, cannot initialize splats");
            return;
        }

        try {
            for (Tile[] tileRow : tiles[0]) {
                for (Tile tile : tileRow) {
                    if (tile == null || tile.getGameObjects() == null) {
                        continue;
                    }
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject != null && gameObject.getId() == ACID_BLOOD_GAME_OBJECT_ID) {
                            activeRocksAndSplats.add(gameObject);
                            rocksAndSplatsSpawnedThisTick.add(gameObject);
                        }
                    }
                }
            }
            log.debug("Found {} splats on delve start", activeRocksAndSplats.size());
        } catch (Exception e) {
            log.warn("Error while initializing splats on delve start", e);
        }
    }

    @Override
    protected void onTick() {
        final int tick = getTick();

        checkForNewOrbs(tick);

        if (phaseChange == PhaseChange.SHIELD_START) {
            hitpointsBeforeShield = mokhaiotl.getHitpoints().getCurrent();
            mokhaiotl.setHitpoints(new Hitpoints(SHIELD_HITPOINTS));
        } else if (phaseChange == PhaseChange.SHIELD_END) {
            mokhaiotl.setHitpoints(new Hitpoints(hitpointsBeforeShield, HITPOINTS_BY_DELVE[Math.min(delve, 8)]));
        }

        if (!rocksAndSplatsSpawnedThisTick.isEmpty() || !rocksAndSplatsDespawnedThisTick.isEmpty()) {
            BiFunction<List<GameObject>, Integer, List<WorldPoint>> toWorldPoints = (objects, id) -> objects.stream()
                    .filter(o -> o.getId() == id)
                    .map(this::getWorldLocation)
                    .collect(Collectors.toList());
            List<WorldPoint> rocksSpawned = toWorldPoints.apply(rocksAndSplatsSpawnedThisTick, ROCK_GAME_OBJECT_ID);
            List<WorldPoint> splatsSpawned = toWorldPoints.apply(rocksAndSplatsSpawnedThisTick, ACID_BLOOD_GAME_OBJECT_ID);
            List<WorldPoint> rocksDespawned = toWorldPoints.apply(rocksAndSplatsDespawnedThisTick, ROCK_GAME_OBJECT_ID);
            List<WorldPoint> splatsDespawned = toWorldPoints.apply(rocksAndSplatsDespawnedThisTick, ACID_BLOOD_GAME_OBJECT_ID);
            dispatchEvent(new MokhaiotlObjectsEvent(getStage(), tick,
                    rocksSpawned, rocksDespawned, splatsSpawned, splatsDespawned));
        }

        while (!healsThisTick.isEmpty()) {
            Hitsplat hitsplat = healsThisTick.remove();
            TrackedNpc larva = larvaeLeakedThisTick.poll();
            if (larva == null) {
                log.warn("Mokhaiotl healed for {} without corresponding larva leak", hitsplat.getAmount());
                break;
            }
            dispatchEvent(new MokhaiotlLarvaLeakEvent(getStage(), tick, larva.getRoomId(), hitsplat.getAmount()));
        }

        if (!shockwaveLocationsThisTick.isEmpty()) {
            boolean isSlam =
                    mokhaiotl.getNpc().getId() == MokhaiotlNpc.MOKHAIOTL_BURROWED.getId()
                            || lastRacecarTick >= tick - 12;
            NpcAttack attack = isSlam ? NpcAttack.MOKHAIOTL_SLAM : NpcAttack.MOKHAIOTL_SHOCKWAVE;
            dispatchEvent(new NpcAttackEvent(getStage(), tick, getWorldLocation(mokhaiotl), attack, mokhaiotl));
            dispatchEvent(new MokhaiotlShockwaveEvent(getStage(), tick, new ArrayList<>(shockwaveLocationsThisTick)));
        }

        rocksAndSplatsSpawnedThisTick.clear();
        rocksAndSplatsDespawnedThisTick.clear();
        shockwaveLocationsThisTick.clear();
        healsThisTick.clear();
        larvaeLeakedThisTick.clear();
        phaseChange = PhaseChange.NONE;
    }

    @Override
    protected void onMessage(ChatMessage event) {
        String stripped = Text.removeTags(event.getMessage());
        Matcher matcher = delveEndRegex.matcher(stripped);

        if (matcher.find()) {
            try {
                String inGameTime = matcher.group(1);
                finish(inGameTime);
            } catch (Exception e) {
                log.warn("Could not parse timestamp from delve end message: {}", stripped);
                finish(true);
            }
        }
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();
        Optional<MokhaiotlNpc> maybeNpc = MokhaiotlNpc.withId(npc.getId());
        if (maybeNpc.isEmpty()) {
            return Optional.empty();
        }

        MokhaiotlNpc mokhaiotlNpc = maybeNpc.get();
        if (mokhaiotlNpc.isMokhaiotl()) {
            if (getState() == State.NOT_STARTED) {
                start();
            }

            if (mokhaiotl == null) {
                int hitpoints = HITPOINTS_BY_DELVE[Math.min(delve, 8)];
                mokhaiotl = new BasicTrackedNpc(npc, generateRoomId(npc), new Hitpoints(hitpoints));
            }
            return Optional.of(mokhaiotl);
        }

        if (mokhaiotlNpc.isLarva()) {
            return Optional.of(new BasicTrackedNpc(npc, generateRoomId(npc), new Hitpoints(2)));
        }

        if (mokhaiotlNpc == MokhaiotlNpc.VOLATILE_EARTH) {
            return Optional.of(new BasicTrackedNpc(npc, generateRoomId(npc), new Hitpoints(1)));
        }

        if (mokhaiotlNpc == MokhaiotlNpc.EARTHEN_SHIELD) {
            return Optional.of(new BasicTrackedNpc(npc, generateRoomId(npc), new Hitpoints(0)));
        }

        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, @Nullable TrackedNpc trackedNpc) {
        if (trackedNpc == null) {
            return false;
        }

        Optional<MokhaiotlNpc> maybeNpc = MokhaiotlNpc.withId(event.getNpc().getId());
        if (maybeNpc.isEmpty()) {
            return false;
        }

        MokhaiotlNpc mokhaiotlNpc = maybeNpc.get();
        if (mokhaiotlNpc.isLarva()) {
            NPC larva = trackedNpc.getNpc();
            WorldPoint mokhaiotlSouthwest = getWorldLocation(mokhaiotl);
            int mokhaiotlSize = mokhaiotl.getNpc().getComposition().getSize();
            WorldPoint mokhaiotlCenter = new WorldPoint(
                    mokhaiotlSouthwest.getX() + mokhaiotlSize / 2,
                    mokhaiotlSouthwest.getY() + mokhaiotlSize / 2,
                    mokhaiotlSouthwest.getPlane()
            );

            if (getWorldLocation(larva).equals(mokhaiotlCenter)) {
                larvaeLeakedThisTick.add(trackedNpc);
            }
        }

        return true;
    }

    @Override
    protected void onNpcChange(NpcChanged event) {
        Optional<MokhaiotlNpc> maybeBefore = MokhaiotlNpc.withId(event.getOld().getId());
        Optional<MokhaiotlNpc> maybeAfter = MokhaiotlNpc.withId(event.getNpc().getId());
        if (maybeBefore.isEmpty() || maybeAfter.isEmpty()) {
            return;
        }

        MokhaiotlNpc before = maybeBefore.get();
        MokhaiotlNpc after = maybeAfter.get();

        if (before == MokhaiotlNpc.MOKHAIOTL && after == MokhaiotlNpc.MOKHAIOTL_SHIELDED) {
            phaseChange = PhaseChange.SHIELD_START;
        } else if (before == MokhaiotlNpc.MOKHAIOTL_SHIELDED) {
            phaseChange = PhaseChange.SHIELD_END;
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied hitsplatApplied) {
        if (hitsplatApplied.getActor() != mokhaiotl.getNpc()) {
            return;
        }

        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.getHitsplatType() == HitsplatID.HEAL) {
            healsThisTick.add(hitsplat);
        }
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        if (event.getActor() != mokhaiotl.getNpc()) {
            return;
        }

        final int tick = getTick();
        NpcAttack attack;

        switch (event.getActor().getAnimation()) {
            case MOKHAIOTL_AUTO_ANIMATION_ID:
                attack = NpcAttack.MOKHAIOTL_AUTO;
                unidentifiedAttackTick = tick;
                break;
            case MOKHAIOTL_BALL_ANIMATION_ID:
                attack = NpcAttack.MOKHAIOTL_BALL;
                unidentifiedAttackTick = tick;
                break;
            case MOKHAIOTL_CHARGE_ANIMATION_ID:
                attack = NpcAttack.MOKHAIOTL_CHARGE;
                break;
            case MOKHAIOTL_MELEE_ANIMATION_ID:
                attack = NpcAttack.MOKHAIOTL_MELEE;
                break;
            case MOKHAIOTL_BLAST_ANIMATION_ID:
                attack = NpcAttack.MOKHAIOTL_BLAST;
                break;
            case MOKHAIOTL_RACECAR_ANIMATION_ID:
                if (lastRacecarTick > tick - 5) {
                    return;
                }
                attack = NpcAttack.MOKHAIOTL_RACECAR;
                lastRacecarTick = tick;
                break;
            default:
                log.debug("Unhandled Mokhaiotl animation: {}", event.getActor().getAnimation());
                return;
        }

        WorldPoint location = getWorldLocation(mokhaiotl);
        dispatchEvent(new NpcAttackEvent(getStage(), tick, location, attack, mokhaiotl));
    }

    @Override
    protected void onProjectile(ProjectileMoved event) {
        Projectile projectile = event.getProjectile();
        int elapsed = projectile.getEndCycle() - projectile.getStartCycle() - projectile.getRemainingCycles();

        MokhaiotlAttackStyleEvent.Style style;

        switch (projectile.getId()) {
            case Orb.MELEE_PROJECTILE_ID:
                style = MokhaiotlAttackStyleEvent.Style.MELEE;
                break;
            case Orb.RANGED_PROJECTILE_ID:
            case MOKHAIOTL_RANGED_BALL_PROJECTILE_ID:
                style = MokhaiotlAttackStyleEvent.Style.RANGED;
                break;
            case Orb.MAGE_PROJECTILE_ID:
            case MOKHAIOTL_MAGE_BALL_PROJECTILE_ID:
                style = MokhaiotlAttackStyleEvent.Style.MAGE;
                break;
            default:
                return;
        }

        if (elapsed == 0 && mokhaiotl != null && mokhaiotl.getNpc() != null) {
            boolean fromMokhaiotl =
                    mokhaiotl.getNpc().getWorldArea().contains(projectile.getSourcePoint());

            if (fromMokhaiotl && unidentifiedAttackTick != -1) {
                dispatchEvent(new MokhaiotlAttackStyleEvent(
                        getStage(), getTick(), style, unidentifiedAttackTick));
                unidentifiedAttackTick = -1;
            }
        } else if (projectile.getRemainingCycles() <= 3) {
            Orb orb = activeOrbs.get(projectile);
            if (orb != null && orb.isActive()) {
                orb.setLanded(getTick());
                WorldPoint sourcePoint = Location.getWorldLocation(client, projectile.getSourcePoint());
                dispatchEvent(new MokhaiotlOrbEvent(getStage(), orb.getLandedTick(), orb, sourcePoint));
            }
        }
    }

    @Override
    protected void onGameObjectSpawn(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        switch (object.getId()) {
            case ACID_BLOOD_GAME_OBJECT_ID:
            case ROCK_GAME_OBJECT_ID:
                if (activeRocksAndSplats.add(object)) {
                    rocksAndSplatsSpawnedThisTick.add(object);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onGameObjectDespawn(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        switch (object.getId()) {
            case ACID_BLOOD_GAME_OBJECT_ID:
            case ROCK_GAME_OBJECT_ID:
                if (activeRocksAndSplats.remove(object)) {
                    rocksAndSplatsDespawnedThisTick.add(object);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onGraphicsObjectCreation(GraphicsObjectCreated event) {
        GraphicsObject object = event.getGraphicsObject();
        if (SHOCKWAVE_GRAPHICS_IDs.contains(object.getId())) {
            WorldPoint wp = WorldPoint.fromLocal(client, object.getLocation());
            shockwaveLocationsThisTick.add(Location.getWorldLocation(client, wp));
        }
    }

    @Override
    protected boolean npcIgnoresCooldown(int npcId) {
        return MokhaiotlNpc.withId(npcId)
                .map(m -> m.isLarva() || m == MokhaiotlNpc.VOLATILE_EARTH)
                .orElse(false);
    }

    void checkForNewOrbs(int tick) {
        if (mokhaiotl == null) {
            return;
        }

        for (Projectile projectile : client.getProjectiles()) {
            if (!Orb.isOrb(projectile.getId())) {
                continue;
            }

            if (!activeOrbs.containsKey(projectile)) {
                boolean fromMokhaiotl =
                        mokhaiotl.getNpc().getWorldArea().contains(projectile.getSourcePoint());

                Orb.Source source = fromMokhaiotl
                        ? Orb.Source.MOKHAIOTL
                        : Orb.Source.BALL;
                activeOrbs.put(projectile, new Orb(projectile, source, tick));
            }
        }
    }
}
