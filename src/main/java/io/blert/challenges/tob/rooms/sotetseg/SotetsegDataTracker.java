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

package io.blert.challenges.tob.rooms.sotetseg;

import io.blert.challenges.tob.TheatreChallenge;
import io.blert.challenges.tob.TobNpc;
import io.blert.challenges.tob.rooms.Room;
import io.blert.challenges.tob.rooms.RoomDataTracker;
import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import io.blert.core.NpcAttack;
import io.blert.core.TrackedNpc;
import io.blert.events.NpcAttackEvent;
import io.blert.events.tob.SoteMazeProcEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
public class SotetsegDataTracker extends RoomDataTracker {
    private static final int MAZE_TELEPORT_ANIMATION = 1816;
    private static final int SOTE_MELEE_ANIMATION = 8138;
    private static final int SOTE_BALL_ANIMATION = 8139;
    private static final int SOTE_DEATH_BALL_PROJECTILE = 1604;
    private static final int SOTE_ATTACK_SPEED = 5;

    private final int[] mazeTicks = new int[]{-1, -1};
    private Maze maze = Maze.MAZE_66;
    private @Nullable NpcAttack attackThisTick = null;
    private int lastAttackTick = -1;
    private int deathBallSpawnTick = -1;
    private @Nullable BasicTrackedNpc sotetseg = null;

    public SotetsegDataTracker(TheatreChallenge manager, Client client) {
        super(manager, client, Room.SOTETSEG);
    }

    @Override
    protected void onRoomStart() {
    }

    @Override
    protected void onTick() {
        super.onTick();
        final int tick = getTick();

        if (mazeTicks[maze.ordinal()] == tick) {
            // Advance to the next maze after all the teleport animation handlers have run.
            maze = Maze.MAZE_33;
        }

        if (deathBallSpawnTick != -1 && tick == deathBallSpawnTick + 20) {
            // The ball takes 15 ticks to land, but add in a safety buffer.
            deathBallSpawnTick = -1;
        }

        if (attackThisTick != null && sotetseg != null) {
            if (deathBallSpawnTick == tick) {
                // A regular attack animation may have occurred at the same time as the death ball; the death ball
                // should take priority.
                attackThisTick = NpcAttack.TOB_SOTE_DEATH_BALL;
            }
            lastAttackTick = tick;
            dispatchEvent(new NpcAttackEvent(getStage(), tick, getWorldLocation(sotetseg), attackThisTick, sotetseg));
            attackThisTick = null;
        }
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned event) {
        NPC npc = event.getNpc();

        return TobNpc.withId(npc.getId())
                .filter(tobNpc -> TobNpc.isSotetsegIdle(tobNpc.getId()))
                .map(tobNpc -> {
                    sotetseg = new BasicTrackedNpc(npc, tobNpc, generateRoomId(npc),
                            new Hitpoints(tobNpc, theatreChallenge.getScale()));
                    return sotetseg;
                });
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned event, TrackedNpc trackedNpc) {
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
        final int tick = getTick();

        if (actor instanceof NPC) {
            if (sotetseg == null || actor != sotetseg.getNpc()) {
                return;
            }

            if (lastAttackTick != -1 && tick - lastAttackTick < SOTE_ATTACK_SPEED) {
                return;
            }

            if (animationId == SOTE_MELEE_ANIMATION) {
                attackThisTick = NpcAttack.TOB_SOTE_MELEE;
            } else if (animationId == SOTE_BALL_ANIMATION) {
                attackThisTick = NpcAttack.TOB_SOTE_BALL;
            }
            return;
        }

        if (!(actor instanceof Player) || animationId != MAZE_TELEPORT_ANIMATION) {
            return;
        }

        if (mazeTicks[maze.ordinal()] == -1) {
            mazeTicks[maze.ordinal()] = tick;
            dispatchEvent(new SoteMazeProcEvent(tick, maze));
            log.debug("Sotetseg {} procced on tick {} {}", maze, tick, formattedRoomTime());
        }
    }

    @Override
    protected void onProjectile(ProjectileMoved event) {
        if (event.getProjectile().getId() == SOTE_DEATH_BALL_PROJECTILE && deathBallSpawnTick == -1) {
            deathBallSpawnTick = getTick();
            attackThisTick = NpcAttack.TOB_SOTE_DEATH_BALL;
        }
    }
}
