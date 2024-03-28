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

package io.blert.challenges.tob.rooms.verzik;

import com.google.common.collect.ImmutableSet;
import io.blert.challenges.tob.TheatreChallenge;
import io.blert.challenges.tob.TobNpc;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.core.*;
import io.blert.events.NpcAttackEvent;
import io.blert.events.tob.VerzikAttackStyleEvent;
import io.blert.events.tob.VerzikPhaseEvent;
import io.blert.events.tob.VerzikRedsSpawnEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
import java.util.*;

@Slf4j
public class VerzikDataTracker extends RoomDataTracker {
    private static final int P1_AUTO_ANIMATION = 8109;
    private static final int P2_AUTO_ANIMATION = 8114;
    private static final int P2_BOUNCE_ANIMATION = 8116;
    private static final int P3_TRANSITION_ANIMATION = 8118;

    private static final int P2_CABBAGE_PROJECTILE = 1583;
    private static final int P2_ZAP_PROJECTILE = 1585;
    private static final int P2_PURPLE_PROJECTILE = 1586;
    private static final int P2_MAGE_PROJECTILE = 1591;
    private static final int P3_RANGE_PROJECTILE = 1593;
    private static final int P3_MAGE_PROJECTILE = 1594;

    private static final int P1_ATTACK_SPEED = 14;
    private static final int P2_ATTACK_SPEED = 4;
    private static final int P2_TICKS_BEFORE_FIRST_ATTACK_AFTER_SPAWN = 3;
    private static final int P2_TICKS_BEFORE_FIRST_ATTACK_AFTER_REDS = 12;
    private static final int P2_ATTACKS_PER_REDS = 7;
    private static final int P3_ATTACK_SPEED = 7;
    private static final int P3_ENRAGED_ATTACK_SPEED = 5;
    private static final int P3_TICKS_BEFORE_FIRST_ATTACK = 12;
    private static final int P3_GREEN_BALL_TICK_DELAY = 12;
    private static final int P3_ATTACKS_BEFORE_SPECIAL = 4;

    private static final ImmutableSet<Integer> VERZIK_WEB_IDS = ImmutableSet.of(8376, 10837, 10854);
    private static final int VERZIK_YELLOW_OBJECT_ID = 1595;

    private BasicTrackedNpc verzik;

    private VerzikPhase phase;

    private int unidentifiedVerzikAttackTick;
    private int nextVerzikAttackTick;
    private @Nullable NpcAttack nextVerzikAttack;
    private int verzikAttacksUntilSpecial;
    private @Nullable VerzikSpecial activeSpecial;
    private VerzikSpecial nextSpecial;
    boolean enraged;

    private int redCrabsTick;
    private int redCrabSpawnCount;
    private final List<BasicTrackedNpc> pillars = new ArrayList<>();
    private final Set<VerzikCrab> explodingCrabs = new HashSet<>();
    private final Set<BasicTrackedNpc> specialCrabs = new HashSet<>();
    private final List<WorldPoint> yellowPools = new ArrayList<>();

    public VerzikDataTracker(TheatreChallenge manager, Client client) {
        super(manager, client, Room.VERZIK);
        this.phase = VerzikPhase.IDLE;
        this.unidentifiedVerzikAttackTick = -1;
        this.nextVerzikAttackTick = -1;
        this.nextVerzikAttack = null;
        this.verzikAttacksUntilSpecial = -1;
        this.redCrabsTick = -1;
        this.redCrabSpawnCount = 0;
        this.enraged = false;
        this.activeSpecial = null;
    }

    @Override
    protected void onRoomStart() {
        client.getNpcs().stream()
                .filter(npc -> TobNpc.isAnyVerzik(npc.getId()))
                .findFirst()
                .flatMap(npc -> TobNpc.withId(npc.getId()))
                .ifPresent(tobNpc -> {
                    if (tobNpc.isVerzikP1()) {
                        startVerzikPhase(VerzikPhase.P1, getTick(), true);
                    } else if (tobNpc.isVerzikP2()) {
                        startVerzikPhase(VerzikPhase.P2, getTick(), true);
                    } else if (tobNpc.isVerzikP3()) {
                        startVerzikPhase(VerzikPhase.P3, getTick(), true);
                    }
                });
    }

    @Override
    protected void onTick() {
        final int tick = getTick();

        if (tick == redCrabsTick) {
            if (redCrabSpawnCount == 1) {
                log.debug("Reds: {} ({})", tick, formattedRoomTime());
            }

            // TODO(frolv): Remove this in favor of the generic NPC spawn event.
            dispatchEvent(new VerzikRedsSpawnEvent(tick));
        }

        if (tick == nextVerzikAttackTick) {
            handleVerzikAttack(tick);
        }

        if (phase == VerzikPhase.P3) {
            checkForEnrage(tick);
        }

        if (activeSpecial != null && verzik.getNpc().getInteracting() != null) {
            // Once Verzik targets a player, her special attack has ended.
            if (activeSpecial == VerzikSpecial.YELLOWS) {
                nextVerzikAttackTick = tick + 7;
            } else {
                nextVerzikAttackTick = tick + 10;
            }

            activeSpecial = null;
            yellowPools.clear();
        }
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();
        final int tick = getTick();

        if (activeSpecial == null && VERZIK_WEB_IDS.contains(npc.getId())) {
            activeSpecial = VerzikSpecial.WEBS;
            return Optional.empty();
        }

        if (npc.getId() == NpcID.SUPPORTING_PILLAR) {
            BasicTrackedNpc pillar = new BasicTrackedNpc(npc, TobNpc.VERZIK_PILLAR, generateRoomId(npc), new Hitpoints(0));
            pillars.add(pillar);
            return Optional.of(pillar);
        }

        if (npc.getId() == NpcID.COLLAPSING_PILLAR) {
            // Find the pillar that collapsed and despawn it.
            pillars.stream()
                    .filter(pillar -> npc.getWorldArea().contains2D(pillar.getNpc().getWorldLocation()))
                    .findFirst()
                    .ifPresent(collapsed -> {
                        pillars.remove(collapsed);
                        despawnTrackedNpc(collapsed);
                    });
            return Optional.empty();
        }

        var maybeNpc = TobNpc.withId(npc.getId());
        if (maybeNpc.isEmpty()) {
            return Optional.empty();
        }
        TobNpc tobNpc = maybeNpc.get();

        if (tobNpc.isAnyVerzik()) {
            long roomId = verzik != null ? verzik.getRoomId() : generateRoomId(npc);
            verzik = new BasicTrackedNpc(npc, tobNpc, roomId, new Hitpoints(tobNpc, theatreChallenge.getRaidScale()));
            return Optional.of(verzik);
        }

        if (tobNpc.isVerzikCrab()) {
            WorldPoint point = getWorldLocation(npc);
            long roomId = generateRoomId(npc);
            VerzikCrab crab = VerzikCrab.fromSpawnedNpc(npc, tobNpc, roomId, point, theatreChallenge.getRaidScale(), phase);

            explodingCrabs.add(crab);
            return Optional.of(crab);
        }

        if (tobNpc.isVerzikAthanatos()) {
            BasicTrackedNpc purpleCrab = new BasicTrackedNpc(npc, tobNpc, generateRoomId(npc),
                    new Hitpoints(tobNpc, theatreChallenge.getRaidScale()));
            specialCrabs.add(purpleCrab);
            return Optional.of(purpleCrab);
        }

        if (tobNpc.isVerzikMatomenos()) {
            if (tick != redCrabsTick) {
                startNewRedsPhase(tick);
            }

            BasicTrackedNpc crab = new BasicTrackedNpc(npc, tobNpc, generateRoomId(npc),
                    new Hitpoints(tobNpc, theatreChallenge.getRaidScale()));
            specialCrabs.add(crab);
            return Optional.of(crab);
        }

        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, TrackedNpc trackedNpc) {
        NPC npc = event.getNpc();

        if (TobNpc.isVerzikMatomenos(npc.getId()) || TobNpc.isVerzikAthanatos(npc.getId())) {
            return trackedNpc instanceof BasicTrackedNpc && specialCrabs.remove(trackedNpc);
        }

        if (TobNpc.isVerzikCrab(npc.getId())) {
            return trackedNpc instanceof VerzikCrab && explodingCrabs.remove(trackedNpc);
        }

        if (TobNpc.isVerzikP1(npc.getId())) {
            final int tick = getTick();
            log.debug("P1: {} ({})", tick, formattedRoomTime());
            dispatchEvent(new VerzikPhaseEvent(tick, VerzikPhase.P2));
        }

        // Verzik despawns between phases, but it should not be counted as a final despawn until the end of the fight.
        return trackedNpc == verzik && phase == VerzikPhase.P3;
    }

    @Override
    protected void onNpcChange(NpcChanged changed) {
        NPC npc = changed.getNpc();
        int beforeId = changed.getOld().getId();

        final int tick = getTick();

        var maybeVerzik = TobNpc.withId(npc.getId());
        if (maybeVerzik.isEmpty()) {
            return;
        }
        TobNpc tobNpc = maybeVerzik.get();

        if (TobNpc.isVerzikIdle(beforeId) && tobNpc.isVerzikP1()) {
            startRoom();
            return;
        }

        if (TobNpc.isVerzikP1Transition(beforeId) && tobNpc.isVerzikP2()) {
            startVerzikPhase(VerzikPhase.P2, tick, false);

            // A transition from P1 to P2 does not spawn a new NPC. Simply reset Verzik's HP to its P2 value.
            verzik.setHitpoints(new Hitpoints(tobNpc, theatreChallenge.getRaidScale()));
        }
    }

    @Override
    protected void onAnimation(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            return;
        }

        final int npcId = ((NPC) actor).getId();
        final int animationId = actor.getAnimation();
        final int tick = getTick();

        if (phase == VerzikPhase.P2 && TobNpc.isVerzikP2(npcId) && animationId == P3_TRANSITION_ANIMATION) {
            startVerzikPhase(VerzikPhase.P3, tick, true);
            log.debug("P2: {} ({})", tick, formattedRoomTime());
            return;
        }

        if (verzik != null && actor == verzik.getNpc()) {
            if (animationId == P1_AUTO_ANIMATION && phase == VerzikPhase.P1) {
                nextVerzikAttackTick = tick + 1;
                nextVerzikAttack = NpcAttack.VERZIK_P1_AUTO;
                return;
            }

            if (animationId == P2_AUTO_ANIMATION && phase == VerzikPhase.P2) {
                nextVerzikAttackTick = tick;
                return;
            }

            if (animationId == P2_BOUNCE_ANIMATION && phase == VerzikPhase.P2) {
                WorldPoint point = getWorldLocation(verzik);
                dispatchEvent(new NpcAttackEvent(stage, tick, point, NpcAttack.VERZIK_P2_BOUNCE, verzik));
            }
        }
    }

    @Override
    protected void onProjectile(ProjectileMoved event) {
        final int tick = getTick();
        Projectile projectile = event.getProjectile();

        if (phase == VerzikPhase.P2 && tick == nextVerzikAttackTick && nextVerzikAttack == null) {
            switch (projectile.getId()) {
                case P2_CABBAGE_PROJECTILE:
                    nextVerzikAttack = NpcAttack.VERZIK_P2_CABBAGE;
                    break;
                case P2_ZAP_PROJECTILE:
                    if (verzikAttacksUntilSpecial <= 0) {
                        nextVerzikAttack = NpcAttack.VERZIK_P2_ZAP;
                        verzikAttacksUntilSpecial = 4;
                    }
                    break;
                case P2_PURPLE_PROJECTILE:
                    nextVerzikAttack = NpcAttack.VERZIK_P2_PURPLE;
                    break;
                case P2_MAGE_PROJECTILE:
                    nextVerzikAttack = NpcAttack.VERZIK_P2_MAGE;
                    break;
            }
        }

        if (phase == VerzikPhase.P3) {
            int totalCycles = projectile.getEndCycle() - projectile.getStartCycle();
            if (projectile.getRemainingCycles() != totalCycles) {
                return;
            }

            VerzikAttackStyleEvent.Style style;
            switch (event.getProjectile().getId()) {
                case P3_RANGE_PROJECTILE:
                    style = VerzikAttackStyleEvent.Style.RANGE;
                    break;
                case P3_MAGE_PROJECTILE:
                    style = VerzikAttackStyleEvent.Style.MAGE;
                    break;
                default:
                    return;
            }

            if (unidentifiedVerzikAttackTick != -1) {
                dispatchEvent(new VerzikAttackStyleEvent(tick, style, unidentifiedVerzikAttackTick));
                unidentifiedVerzikAttackTick = -1;
            }
        }
    }

    @Override
    protected void onGraphicsObjectCreation(GraphicsObjectCreated event) {
        GraphicsObject obj = event.getGraphicsObject();
        if (obj.getId() != VERZIK_YELLOW_OBJECT_ID) {
            return;
        }

        long totalYellows = theatreChallenge.getRaiders().stream().filter(Raider::isAlive).count();
        if (theatreChallenge.getRaidMode() == ChallengeMode.TOB_HARD) {
            totalYellows *= 3;
        }

        if (yellowPools.isEmpty()) {
            activeSpecial = VerzikSpecial.YELLOWS;
        }

        if (yellowPools.size() <= totalYellows) {
            yellowPools.add(WorldPoint.fromLocalInstance(client, obj.getLocation()));
        }
    }

    private void handleVerzikAttack(int tick) {
        switch (phase) {
            case P1:
                // No special handling required.
                break;

            case P2:
                if (verzikAttacksUntilSpecial > 0) {
                    verzikAttacksUntilSpecial--;
                }
                if (redCrabSpawnCount > 0 && verzikAttacksUntilSpecial == 0) {
                    // Last auto before the next reds phase.
                    nextVerzikAttackTick = -1;
                }
                break;

            case P3:
                if (unidentifiedVerzikAttackTick != -1) {
                    // No projectiles were recorded since the last Verzik attack, so it must be a melee.
                    dispatchEvent(new VerzikAttackStyleEvent(
                            tick, VerzikAttackStyleEvent.Style.MELEE, unidentifiedVerzikAttackTick));
                    unidentifiedVerzikAttackTick = -1;
                }

                if (verzikAttacksUntilSpecial == 0) {
                    verzikAttacksUntilSpecial = P3_ATTACKS_BEFORE_SPECIAL;
                    WorldPoint point = getWorldLocation(verzik);

                    switch (nextSpecial) {
                        case BALL:
                            // Green ball occurs alongside a regular Verzik attack, and delays her next attack by 12
                            // ticks total.
                            // TODO(frolv): `unidentifiedVerzikAttackTick` is deliberately not set here, as the green
                            // ball projectile seems to hide the regular attack projectiles. Investigate this further.
                            dispatchEvent(new NpcAttackEvent(stage, tick, point, NpcAttack.VERZIK_P3_BALL, verzik));
                            nextSpecial = nextSpecial.next();
                            verzikAttacksUntilSpecial += 1;
                            nextVerzikAttackTick += P3_GREEN_BALL_TICK_DELAY - attackSpeed();
                            break;

                        case CRABS:
                            // Crabs occur alongside a regular Verzik attack whose projectile is identifiable.
                            nextSpecial = nextSpecial.next();
                            verzikAttacksUntilSpecial += 1;
                            unidentifiedVerzikAttackTick = tick;
                            break;

                        case WEBS:
                        case YELLOWS:
                            NpcAttack attack = nextSpecial == VerzikSpecial.WEBS
                                    ? NpcAttack.VERZIK_P3_WEBS
                                    : NpcAttack.VERZIK_P3_YELLOWS;
                            dispatchEvent(new NpcAttackEvent(stage, tick, point, attack, verzik));

                            // Other specials pause the attack cycle until they are completed.
                            nextSpecial = nextSpecial.next();
                            nextVerzikAttackTick = -1;
                            nextVerzikAttack = null;
                            return;
                    }
                } else {
                    // Verzik is performing a regular attack on this tick, without a stacked special.
                    // Mark the attack as unidentified.
                    unidentifiedVerzikAttackTick = tick;
                }

                nextVerzikAttack = NpcAttack.VERZIK_P3_AUTO;
                verzikAttacksUntilSpecial--;
                break;

            default:
                nextVerzikAttack = null;
                nextVerzikAttackTick = -1;
                return;
        }

        if (nextVerzikAttack != null && verzik != null) {
            dispatchEvent(new NpcAttackEvent(stage, tick, getWorldLocation(verzik), nextVerzikAttack, verzik));
        }

        nextVerzikAttackTick += attackSpeed();
        nextVerzikAttack = null;
    }

    private void checkForEnrage(int tick) {
        if (enraged) {
            return;
        }

        if (verzik.getHitpoints().percentage() < 25.0 && verzik.getNpc().getOverheadText() != null) {
            enraged = true;
            if (activeSpecial != VerzikSpecial.WEBS) {
                nextVerzikAttackTick = tick + P3_ENRAGED_ATTACK_SPEED;
            }
        }
    }

    private void startVerzikPhase(VerzikPhase phase, int tick, boolean dispatchPhaseEvent) {
        this.phase = phase;
        nextVerzikAttack = null;
        unidentifiedVerzikAttackTick = -1;

        if (phase == VerzikPhase.P2) {
            nextVerzikAttackTick = tick + P2_TICKS_BEFORE_FIRST_ATTACK_AFTER_SPAWN;
        } else if (phase == VerzikPhase.P3) {
            nextVerzikAttackTick = tick + P3_TICKS_BEFORE_FIRST_ATTACK;
            verzikAttacksUntilSpecial = P3_ATTACKS_BEFORE_SPECIAL;
            nextSpecial = VerzikSpecial.CRABS;
            activeSpecial = null;
            enraged = false;
        } else {
            nextVerzikAttackTick = -1;
        }

        if (dispatchPhaseEvent && phase != VerzikPhase.P1) {
            dispatchEvent(new VerzikPhaseEvent(tick, phase));
        }
    }

    private void startNewRedsPhase(int tick) {
        redCrabsTick = tick;
        redCrabSpawnCount++;
        verzikAttacksUntilSpecial = P2_ATTACKS_PER_REDS;
        nextVerzikAttack = null;
        nextVerzikAttackTick = tick + P2_TICKS_BEFORE_FIRST_ATTACK_AFTER_REDS;
    }

    private int attackSpeed() {
        switch (phase) {
            case P1:
                return P1_ATTACK_SPEED;
            case P2:
                return P2_ATTACK_SPEED;
            case P3:
                return enraged ? P3_ENRAGED_ATTACK_SPEED : P3_ATTACK_SPEED;
            default:
                return -1;
        }
    }
}