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

package io.blert.challenges.chambers.rooms.mystics;

import io.blert.challenges.chambers.CoxNpc;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks Lizardman Mystic room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 * 
 * TODO: Add Mystic-specific attack animations when identified.
 */
@Slf4j
public class MysticsDataTracker extends RoomDataTracker
{

    // TODO: update when you finalize Ice Demon animations from logging
    // private static final int ICE_DEMON_FREEZE_ANIMATION = 7596; // placeholder - needs verification
    // private static final int ICE_DEMON_STOMP_ANIMATION = ?;
    // private static final int ICE_DEMON_AUTO_ANIMATION = ?;

    private final Map<Integer, BasicTrackedNpc> Mystics = new HashMap<>(); // Track multiple Mystics by NPC ID
    private @Nullable NpcAttack attackThisTick = null;

    public MysticsDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[MysticsDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Update HP for all tracked Mystics
        for (Map.Entry<Integer, BasicTrackedNpc> entry : Mystics.entrySet())
        {
            BasicTrackedNpc currentMystic = entry.getValue();
            NPC npc = currentMystic.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Always log health info to debug - copy script's exact logging approach
            log.info(
                "[Mystic HP Debug] NPC \"{}\" (npcId={}, index={}) HR={}/{} at tick {}",
                npc.getName(),
                npc.getId(),
                npc.getIndex(),
                ratio,
                scale,
                tick
            );
            
            // Use script's exact condition check
            if (ratio > -1 && scale > 0)
            {
                double hpPercent = (ratio * 100.0) / scale;
                int updatedHitpoints = (int) (currentMystic.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentMystic.getHitpoints().getCurrent();
                
                log.info("[Mystic HP] Current HP: {}, Calculated HP: {}, Diff: {}, Base HP: {}", 
                         currentHitpoints, updatedHitpoints, Math.abs(currentHitpoints - updatedHitpoints), currentMystic.getHitpoints().getBase());
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 5)
                {
                    Hitpoints newHitpoints = currentMystic.getHitpoints().update(updatedHitpoints);
                    currentMystic.setHitpoints(newHitpoints);
                    
                    log.info(
                        "[Mystic HP] ✓ UPDATED from health ratio {}/{} (~{:.1f}% HP) = {} at tick {}",
                        ratio,
                        scale,
                        hpPercent,
                        updatedHitpoints,
                        tick
                    );
                } else {
                    // No significant change in HP - skipping update
                    // log.info("[Mystic HP] No significant change (diff={}) - skipping update", Math.abs(currentHitpoints - updatedHitpoints));
                }
            } else {
                // Log when health info is not available - match script behavior
                log.warn("[Mystic HP] Health ratio/scale not exposed: ratio={}, scale={} at tick {}", ratio, scale, tick);
            }
        }

        if (attackThisTick != null)
        {
            // Find which Mystic performed the attack (would need animation logic to determine this)
            // For now, just dispatch for all Mystics as this needs attack animation detection
            for (BasicTrackedNpc Mystic : Mystics.values())
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(Mystic.getNpc()),
                    attackThisTick,
                    Mystic
                ));
                break; // Only dispatch once until we can identify which specific Mystic attacked
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active - check this FIRST
        if (terminating()) {
            log.debug("[Mystics] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        // Log all NPC spawns in Mystic room for debugging
        // log.info("[Mystic Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());

        // Check if this NPC ID corresponds to any Mystic variant
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Mystic Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            // Check if it's any Mystic variant
            if (coxNpc == CoxNpc.SKELETAL_MYSTIC_1 || 
                coxNpc == CoxNpc.SKELETAL_MYSTIC_2 || 
                coxNpc == CoxNpc.SKELETAL_MYSTIC_3) {
                
                // Check if this specific NPC ID is already being tracked
                if (!Mystics.containsKey(npc.getId())) {
                    BasicTrackedNpc newMystic = new BasicTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    Mystics.put(npc.getId(), newMystic);
                    log.info("✓ Mystic tracked instance created: id={}, enum={}, base HP {} (scale={}) - Total Mystics: {}", 
                             npc.getId(), coxNpc, newMystic.getHitpoints().getBase(), getChallenge().getScale(), Mystics.size());
                    return Optional.of(newMystic);
                } else {
                    log.info("! Mystic NPC id={} already being tracked, ignoring duplicate spawn", npc.getId());
                }
            } else {
                log.debug("[Mystic Room] Non-Mystic CoxNpc: {} for id {}", coxNpc, npc.getId());
            }
        } else {
            log.debug("[Mystic Room] NPC id {} not found in CoxNpc enum", npc.getId());
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    // TODO: Make sure this is also dependent on hp being 0 otherwise it can die off player screen and cause issues
    {
        NPC npc = despawned.getNpc();
        BasicTrackedNpc removedMystic = Mystics.remove(npc.getId());
        if (removedMystic != null)
        {
            log.info("[Mystic] Despawned NPC id={} – removed from tracking. Remaining Mystics: {}", npc.getId(), Mystics.size());
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        // Check if the animation is from any of our tracked Mystics
        boolean isTrackedMystic = false;
        for (BasicTrackedNpc Mystic : Mystics.values())
        {
            if (actor == Mystic.getNpc())
            {
                isTrackedMystic = true;
                break;
            }
        }
        
        if (!isTrackedMystic)
        {
            return;
        }

        switch (actor.getAnimation())
        {
            // case ICE_DEMON_FREEZE_ANIMATION:
            //     attackThisTick = NpcAttack.COX_ICE_DEMON_FREEZE;
            //     break;
            // case ICE_DEMON_STOMP_ANIMATION:
            //     attackThisTick = NpcAttack.COX_ICE_DEMON_STOMP;
            //     break;
            // case ICE_DEMON_AUTO_ANIMATION:
            //     attackThisTick = NpcAttack.COX_ICE_DEMON_AUTO;
            //     break;
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
            // Check if heal is on any of our tracked Mystics
            for (BasicTrackedNpc Mystic : Mystics.values())
            {
                if (event.getActor() == Mystic.getNpc())
                {
                    setHealTick(getTick());
                    log.info("[Mystic HP] Heal hitsplat detected on NPC id={} at tick {}", Mystic.getNpc().getId(), getHealTick());
                    break;
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Mystic HP - using health ratio/scale instead
    }
}
