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

package io.blert.challenges.chambers.rooms.thieving;

import io.blert.challenges.chambers.CoxNpc;
import io.blert.challenges.chambers.CoxChallenge;
import io.blert.challenges.chambers.RoomDataTracker;
import io.blert.core.BasicTrackedNpc;
import io.blert.core.Hitpoints;
import io.blert.core.NpcAttack;
import io.blert.core.RecordableChallenge;
import io.blert.core.Stage;
import io.blert.core.TrackedNpc;
import io.blert.events.NpcAttackEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Tracks Corrupted Scavenger room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 */
@Slf4j
public class ThievingDataTracker extends RoomDataTracker
{
    // private static final int CORRUPTED_SCAVENGER_HP_VARBIT = 6100;
    private static final int CORRUPTED_SCAVENGER_EAT_ANIMATION = 7496; // placeholder - needs verification
    private static final int CORRUPTED_SCAVENGER_SLEEP_ANIMATION = 7497; // placeholder - needs verification
    // private static final int CORRUPTED_SCAVENGER_STOMP_ANIMATION = ?;
    // private static final int CORRUPTED_SCAVENGER_AUTO_ANIMATION = ?;

    private @Nullable BasicTrackedNpc corruptedScavenger;
    private @Nullable NpcAttack attackThisTick = null;
    private boolean roomOver = false;

    public ThievingDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[ThievingDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Update HP using the direct NPC reference but log more details like the script
        final var currentThieving = corruptedScavenger; // Capture for null safety
        if (currentThieving != null)
        {
            NPC npc = currentThieving.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Use script's exact condition check
            if (ratio > -1 && scale > 0)
            {
                int updatedHitpoints = (int) (currentThieving.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentThieving.getHitpoints().getCurrent();
                
                if (updatedHitpoints <= 0) {
                    if (!roomOver) {
                        log.warn("[Thieving DataTracker] HP is {}, at tick {}/{}", updatedHitpoints, tick, getStartTick() + tick);
                        int postAnimationTick = tick + 1;
                        int tick_cycle = (4 - ((postAnimationTick + getStartTick()) % 4)) % 4;
                        log.warn("[Thieving DataTracker] tick cycle: {}, room end: {}/{}", tick_cycle, postAnimationTick + tick_cycle, getStartTick() + postAnimationTick + tick_cycle);
                        roomOver = true;
                        corruptedScavenger = null; // Clear reference on death
                    }
                }
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 0)
                {
                    Hitpoints newHitpoints = currentThieving.getHitpoints().update(updatedHitpoints);
                    currentThieving.setHitpoints(newHitpoints);
                    log.info(
                        "[Corrupted Scavenger HP] NPC ID: {}, Damaged: {} -> {} (-{}), ratio {}/{} at tick {}/{}",
                        npc.getId(),
                        currentHitpoints, 
                        updatedHitpoints, 
                        Math.abs(currentHitpoints - updatedHitpoints),
                        ratio,
                        scale,
                        tick,
                        getStartTick() + tick
                    );
                }
            }
        }

        if (attackThisTick != null)
        {
            if (currentThieving != null)
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(currentThieving.getNpc()),
                    attackThisTick,
                    currentThieving
                ));
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active - check this FIRST
        if (terminating()) {
            log.debug("[Corrupted Scavenger] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        
        // Log all NPC spawns in Corrupted Scavenger room for debugging
        // log.info("[Corrupted Scavenger Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());
        
        // Check if this NPC ID corresponds to Corrupted Scavenger
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Corrupted Scavenger Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            if (coxNpc == CoxNpc.CORRUPTED_SCAVENGER && corruptedScavenger == null) {
                CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
                corruptedScavenger = new BasicTrackedNpc(
                    npc,
                    coxNpc,
                    generateRoomId(npc),
                    new Hitpoints(coxNpc.getBaseHitpoints())
                );
                String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
                log.info(
                    "✓ Corrupted Scavenger tracked instance created: id={}, enum={}, base HP {} (scale={}){}",
                    npc.getId(),
                    coxNpc,
                    corruptedScavenger.getHitpoints().getBase(),
                    getChallenge().getScale(),
                    modeStatus
                );
                return Optional.of(corruptedScavenger);
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    {
        NPC npc = despawned.getNpc();
        if (corruptedScavenger != null && npc == corruptedScavenger.getNpc())
        {
            log.info("[Corrupted Scavenger] Despawned NPC id={} tick={}", npc.getId(), getTick());
            corruptedScavenger = null;
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        final var currentThieving = corruptedScavenger; // Capture for null safety
        if (currentThieving == null || actor != currentThieving.getNpc())
        {
            return;
        }

        switch (actor.getAnimation())
        {
            case CORRUPTED_SCAVENGER_EAT_ANIMATION:
                log.info("[Corrupted Scavenger] Eat animation detected at tick {}", getTick());
                // attackThisTick = NpcAttack.COX_CORRUPTED_SCAVENGER_FREEZE;
                break;
            case CORRUPTED_SCAVENGER_SLEEP_ANIMATION:
                log.info("[Corrupted Scavenger] Sleep animation detected at tick {}", getTick());
                // attackThisTick = NpcAttack.COX_CORRUPTED_SCAVENGER_SLEEP;
                break;
            default:
                break;
        }
    }

    @Override
    protected void onHitsplat(HitsplatApplied event)
    {
        if (event.getActor() instanceof NPC &&
            event.getHitsplat().getHitsplatType() == HitsplatID.HEAL)
        {
            final var currentThieving = corruptedScavenger; // Capture for null safety
            if (currentThieving != null && event.getActor() == currentThieving.getNpc())
            {
                setHealTick(getTick());
                log.info("[Corrupted Scavenger HP] Heal hitsplat detected at tick {}", getHealTick());
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Corrupted Scavenger HP - using health ratio/scale instead
    }
}
