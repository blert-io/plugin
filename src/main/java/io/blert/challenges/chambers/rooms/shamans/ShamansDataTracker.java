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

package io.blert.challenges.chambers.rooms.shamans;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks Lizardman Shaman room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 * 
 * TODO: Add Shaman-specific attack animations when identified.
 */
@Slf4j
public class ShamansDataTracker extends RoomDataTracker
{

    // TODO: update when you finalize Ice Demon animations from logging
    // private static final int ICE_DEMON_FREEZE_ANIMATION = 7596; // placeholder - needs verification
    // private static final int ICE_DEMON_STOMP_ANIMATION = ?;
    // private static final int ICE_DEMON_AUTO_ANIMATION = ?;

    private final Map<Integer, BasicTrackedNpc> shamans = new HashMap<>(); // Track multiple Shamans by NPC ID
    private @Nullable NpcAttack attackThisTick = null;

    public ShamansDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[ShamansDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Update HP for all tracked Shamans
        for (Map.Entry<Integer, BasicTrackedNpc> entry : shamans.entrySet())
        {
            BasicTrackedNpc currentShaman = entry.getValue();
            NPC npc = currentShaman.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Use script's exact condition check
            if (ratio > -1 && scale > 0)
            {
                int updatedHitpoints = (int) (currentShaman.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentShaman.getHitpoints().getCurrent();
                
                // log.info("[Shaman HP] Current HP: {}, Calculated HP: {}, Diff: {}, Base HP: {}", 
                //             currentHitpoints, updatedHitpoints, Math.abs(currentHitpoints - updatedHitpoints), currentShaman.getHitpoints().getBase());
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 0)
                {
                    Hitpoints newHitpoints = currentShaman.getHitpoints().update(updatedHitpoints);
                    currentShaman.setHitpoints(newHitpoints);
                    log.info(
                        "[Shaman HP] NPC ID: {}, Damaged: {} -> {} (-{}), ratio {}/{} at tick {}/{}",
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
            // Find which Shaman performed the attack (would need animation logic to determine this)
            // For now, just dispatch for all Shamans as this needs attack animation detection
            for (BasicTrackedNpc shaman : shamans.values())
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(shaman.getNpc()),
                    attackThisTick,
                    shaman
                ));
                break; // Only dispatch once until we can identify which specific Shaman attacked
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active - check this FIRST
        if (terminating()) {
            log.debug("[Shamans] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        // Log all NPC spawns in Shaman room for debugging
        // log.info("[Shaman Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());

        // Check if this NPC ID corresponds to any Shaman variant
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Shaman Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            // Check if it's any Shaman variant
            if ((coxNpc == CoxNpc.LIZARDMAN_SHAMAN_1 || coxNpc == CoxNpc.LIZARDMAN_SHAMAN_2) 
                    && !shamans.containsKey(npc.getId())) {
                CoxChallenge coxChallenge = (CoxChallenge) getChallenge();
                BasicTrackedNpc newShaman = new BasicTrackedNpc(
                    npc,
                    coxNpc,
                    generateRoomId(npc),
                    new Hitpoints(coxNpc.getBaseHitpoints())
                );
                shamans.put(npc.getId(), newShaman);
                String modeStatus = coxChallenge.isChallengeMode() ? " [Challenge Mode]" : " [Normal Mode]";
                log.info("✓ Shaman tracked instance created: id={}, enum={}, base HP {} (scale={}) - Total Shamans: {}{}", 
                            npc.getId(), coxNpc, newShaman.getHitpoints().getBase(), getChallenge().getScale(), shamans.size(), modeStatus);
                return Optional.of(newShaman);
            }
        } else {
            log.debug("[Shaman Room] NPC id {} not found in CoxNpc enum", npc.getId());
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    // TODO: Make sure this is also dependent on hp being 0 otherwise it can die off player screen and cause issues
    {
        NPC npc = despawned.getNpc();
        BasicTrackedNpc removedShaman = shamans.remove(npc.getId());
        if (removedShaman != null)
        {
            log.info("[Shaman] Despawned NPC id={}. Remaining Shamans: {}", npc.getId(), shamans.size());
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        // Check if the animation is from any of our tracked Shamans
        boolean isTrackedShaman = false;
        for (BasicTrackedNpc shaman : shamans.values())
        {
            if (actor == shaman.getNpc())
            {
                isTrackedShaman = true;
                break;
            }
        }
        
        if (!isTrackedShaman)
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
            // Check if heal is on any of our tracked Shamans
            for (BasicTrackedNpc shaman : shamans.values())
            {
                if (event.getActor() == shaman.getNpc())
                {
                    setHealTick(getTick());
                    log.info("[Shaman HP] Heal hitsplat detected on NPC id={} at tick {}", shaman.getNpc().getId(), getHealTick());
                    break;
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Shaman HP - using health ratio/scale instead
    }
}
