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

package io.blert.challenges.chambers.rooms.guardians;

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
 * Tracks Lizardman Guardian room events, spawns, HP changes, and attacks with full lifecycle management.
 * Uses NPC health ratio/scale for HP tracking instead of varbits.
 * 
 * TODO: Add Guardian-specific attack animations when identified.
 */
@Slf4j
public class GuardiansDataTracker extends RoomDataTracker
{

    // TODO: update when you finalize Ice Demon animations from logging
    // private static final int ICE_DEMON_FREEZE_ANIMATION = 7596; // placeholder - needs verification
    // private static final int ICE_DEMON_STOMP_ANIMATION = ?;
    // private static final int ICE_DEMON_AUTO_ANIMATION = ?;

    private final Map<Integer, BasicTrackedNpc> Guardians = new HashMap<>(); // Track multiple Guardians by NPC ID
    private @Nullable NpcAttack attackThisTick = null;

    public GuardiansDataTracker(RecordableChallenge challenge, Stage stage, Client client)
    {
        super(challenge, stage, client);
        log.info("[GuardiansDataTracker] Initialized for stage {} with challenge scale {}", stage, challenge.getScale());
    }

    @Override
    protected void onTick()
    {
        super.onTick();

        final int tick = getTick();

        // Update HP for all tracked Guardians
        for (Map.Entry<Integer, BasicTrackedNpc> entry : Guardians.entrySet())
        {
            BasicTrackedNpc currentGuardian = entry.getValue();
            NPC npc = currentGuardian.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Always log health info to debug - copy script's exact logging approach
            log.info(
                "[Guardian HP Debug] NPC \"{}\" (npcId={}, index={}) HR={}/{} at tick {}",
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
                int updatedHitpoints = (int) (currentGuardian.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentGuardian.getHitpoints().getCurrent();
                
                log.info("[Guardian HP] Current HP: {}, Calculated HP: {}, Diff: {}, Base HP: {}", 
                         currentHitpoints, updatedHitpoints, Math.abs(currentHitpoints - updatedHitpoints), currentGuardian.getHitpoints().getBase());
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 5)
                {
                    Hitpoints newHitpoints = currentGuardian.getHitpoints().update(updatedHitpoints);
                    currentGuardian.setHitpoints(newHitpoints);
                    
                    log.info(
                        "[Guardian HP] ✓ UPDATED from health ratio {}/{} (~{:.1f}% HP) = {} at tick {}",
                        ratio,
                        scale,
                        hpPercent,
                        updatedHitpoints,
                        tick
                    );
                } else {
                    log.info("[Guardian HP] No significant change (diff={}) - skipping update", Math.abs(currentHitpoints - updatedHitpoints));
                }
            } else {
                // Log when health info is not available - match script behavior
                log.warn("[Guardian HP] Health ratio/scale not exposed: ratio={}, scale={} at tick {}", ratio, scale, tick);
            }
        }

        if (attackThisTick != null)
        {
            // Find which Guardian performed the attack (would need animation logic to determine this)
            // For now, just dispatch for all Guardians as this needs attack animation detection
            for (BasicTrackedNpc Guardian : Guardians.values())
            {
                dispatchEvent(new NpcAttackEvent(
                    getStage(),
                    tick,
                    getWorldLocation(Guardian.getNpc()),
                    attackThisTick,
                    Guardian
                ));
                break; // Only dispatch once until we can identify which specific Guardian attacked
            }
        }

        attackThisTick = null;
    }

    @Override
    protected Optional<? extends TrackedNpc> onNpcSpawn(NpcSpawned spawned)
    {
        // Only track NPCs if this room tracker is still active - check this FIRST
        if (terminating()) {
            log.debug("[Guardians] Ignoring NPC spawn {} - room tracker is terminating", spawned.getNpc().getId());
            return Optional.empty();
        }
        
        NPC npc = spawned.getNpc();
        // Log all NPC spawns in Guardian room for debugging
        // log.info("[Guardian Room] NPC spawned: id={}, name='{}'", npc.getId(), npc.getName());

        // Check if this NPC ID corresponds to any Guardian variant
        Optional<CoxNpc> coxNpcOpt = CoxNpc.withId(npc.getId());
        if (coxNpcOpt.isPresent()) {
            CoxNpc coxNpc = coxNpcOpt.get();
            log.info("[Guardian Room] Found CoxNpc enum: {} for NPC id {}", coxNpc, npc.getId());
            
            // Check if it's any Guardian variant
            if (coxNpc == CoxNpc.GUARDIAN_1 || 
                coxNpc == CoxNpc.GUARDIAN_2) {
                
                // Check if this specific NPC ID is already being tracked
                if (!Guardians.containsKey(npc.getId())) {
                    BasicTrackedNpc newGuardian = new BasicTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    Guardians.put(npc.getId(), newGuardian);
                    log.info("✓ Guardian tracked instance created: id={}, enum={}, base HP {} (scale={}) - Total Guardians: {}", 
                             npc.getId(), coxNpc, newGuardian.getHitpoints().getBase(), getChallenge().getScale(), Guardians.size());
                    return Optional.of(newGuardian);
                } else {
                    log.info("! Guardian NPC id={} already being tracked, ignoring duplicate spawn", npc.getId());
                }
            } else {
                log.debug("[Guardian Room] Non-Guardian CoxNpc: {} for id {}", coxNpc, npc.getId());
            }
        } else {
            log.debug("[Guardian Room] NPC id {} not found in CoxNpc enum", npc.getId());
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    // TODO: Make sure this is also dependent on hp being 0 otherwise it can die off player screen and cause issues
    {
        NPC npc = despawned.getNpc();
        BasicTrackedNpc removedGuardian = Guardians.remove(npc.getId());
        if (removedGuardian != null)
        {
            log.info("[Guardian] Despawned NPC id={} – removed from tracking. Remaining Guardians: {}", npc.getId(), Guardians.size());
            return true;
        }
        return false;
    }

    @Override
    protected void onAnimation(AnimationChanged event)
    {
        Actor actor = event.getActor();
        // Check if the animation is from any of our tracked Guardians
        boolean isTrackedGuardian = false;
        for (BasicTrackedNpc Guardian : Guardians.values())
        {
            if (actor == Guardian.getNpc())
            {
                isTrackedGuardian = true;
                break;
            }
        }
        
        if (!isTrackedGuardian)
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
            // Check if heal is on any of our tracked Guardians
            for (BasicTrackedNpc Guardian : Guardians.values())
            {
                if (event.getActor() == Guardian.getNpc())
                {
                    setHealTick(getTick());
                    log.info("[Guardian HP] Heal hitsplat detected on NPC id={} at tick {}", Guardian.getNpc().getId(), getHealTick());
                    break;
                }
            }
        }
    }

    @Override
    protected void onVarbit(VarbitChanged event)
    {
        // No longer using varbits for Guardian HP - using health ratio/scale instead
    }
}
