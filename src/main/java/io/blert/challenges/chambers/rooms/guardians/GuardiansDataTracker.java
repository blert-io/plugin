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
 * Uses NPC health ratio/scale for HP tracking.
 * 
 * Guardians have multiple IDs representing different states:
 * - Guardian 1: live (7569) and dead (7571)
 * - Guardian 2: live (7570) and dead (7572)
 * 
 * Only live Guardian IDs are tracked during spawn. Dead Guardian IDs persist but are ignored for HP updates
 * when they have HR=-1/-1 to prevent unnecessary processing.
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

    private final Map<Integer, BasicTrackedNpc> guardians = new HashMap<>(); // Track Guardians by NPC ID (includes live and dead IDs)
    private final Map<Integer, Boolean> targetedGuardians = new HashMap<>(); // Track which Guardian IDs have been targeted
    private final Map<Integer, Boolean> attackedGuardians = new HashMap<>(); // Track which Guardian IDs have been attacked
    private @Nullable NpcAttack attackThisTick = null;
    private int counter = 0; // Counter for room completion logging
    private int lastPlayerAnimation = -1; // Track player's last animation

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
        
        // Check player targeting
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getInteracting() instanceof NPC)
        {
            NPC targetedNpc = (NPC) localPlayer.getInteracting();
            int npcId = targetedNpc.getId();
            
            // Check if this is a Guardian we're tracking and haven't logged targeting for yet
            if (guardians.containsKey(npcId) && !targetedGuardians.getOrDefault(npcId, false))
            {
                targetedGuardians.put(npcId, true);
                log.info("[Guardian Target] First time targeting Guardian id={} index={} at tick {}/{}",
                    npcId, targetedNpc.getIndex(), tick, getStartTick() + tick);
            }
        }
        
        // Check player attacking (based on player animation)
        if (localPlayer != null)
        {
            int currentAnimation = localPlayer.getAnimation();
            
            // Detect if player is performing an attack animation (not idle/moving)
            if (currentAnimation != -1 && currentAnimation != lastPlayerAnimation)
            {
                Actor interacting = localPlayer.getInteracting();
                if (interacting instanceof NPC)
                {
                    NPC attackedNpc = (NPC) interacting;
                    int npcId = attackedNpc.getId();
                    
                    // Check if this is a Guardian we're tracking and haven't logged attacking for yet
                    if (guardians.containsKey(npcId) && !attackedGuardians.getOrDefault(npcId, false))
                    {
                        attackedGuardians.put(npcId, true);
                        log.info("[Guardian Attack] First time attacking Guardian id={} index={} with animation {} at tick {}/{}",
                            npcId, attackedNpc.getIndex(), currentAnimation, tick, getStartTick() + tick);
                    }
                }
            }
            
            lastPlayerAnimation = currentAnimation;
        }

        // Update HP for all tracked Guardians
        int deadGuardiansCount = 0;
        int totalGuardiansCount = 0;
        
        for (Map.Entry<Integer, BasicTrackedNpc> entry : guardians.entrySet())
        {
            BasicTrackedNpc currentGuardian = entry.getValue();
            NPC npc = currentGuardian.getNpc();
            int ratio = npc.getHealthRatio();
            int scale = npc.getHealthScale();
            
            // Count this guardian
            totalGuardiansCount++;
            
            // Check if this Guardian is dead (either dead ID with HR=-1/-1 or 0 HP)
            if ((npc.getId() == 7571 || npc.getId() == 7572) && ratio == -1 && scale == -1)
            {
                // log.info("[Guardian HP] Dead Guardian id={} index={} - skipping HP update (HR=-1/-1)", npc.getId(), npc.getIndex());
                deadGuardiansCount++;
                continue;
            } else if (currentGuardian.getHitpoints().getCurrent() == 0) {
                deadGuardiansCount++;
            }
            
            // Use script's exact condition check
            if (ratio > -1 && scale > 0)
            {
                int updatedHitpoints = (int) (currentGuardian.getHitpoints().getBase() * (ratio / (double) scale));
                int currentHitpoints = currentGuardian.getHitpoints().getCurrent();
                
                // Check if this Guardian just died (HP reached 0)
                if (updatedHitpoints == 0 && currentHitpoints > 0)
                {
                    deadGuardiansCount++;
                }
                
                // log.info("[Guardian HP] Current HP: {}, Calculated HP: {}, Diff: {}, Base HP: {}", 
                //          currentHitpoints, updatedHitpoints, Math.abs(currentHitpoints - updatedHitpoints), currentGuardian.getHitpoints().getBase());
                
                // Only update if there's a significant change (similar to varbit logic)
                if (Math.abs(currentHitpoints - updatedHitpoints) > 0)
                {
                    Hitpoints newHitpoints = currentGuardian.getHitpoints().update(updatedHitpoints);
                    currentGuardian.setHitpoints(newHitpoints);
                    log.info(
                        "[Guardian HP] NPC ID: {}, Damaged: {} -> {} (-{}), ratio {}/{} at tick {}/{}",
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
        
        // Check if both Guardians are dead (we expect 2 Guardians total)
        if (totalGuardiansCount >= 2 && deadGuardiansCount >= 2)
        {
            counter += 1;
            // int tick_cycle_temp = (4 - ((getTick() + getStartTick()) % 4)) % 4;
            // log.info("[Guardian Debug] Guardians dead! Counter: {} Current Tick: {}, 4 tick cycle offset: {}, RoomEnd: {}", counter, getTick(), tick_cycle_temp, getTick() + tick_cycle_temp);
            if (counter == 3) // Log only once when both die
            {
                int tick_cycle = (4 - ((getTick() + getStartTick()) % 4)) % 4;
                log.info("[Guardian] Guardians dead! at tick: {}/{}, tick cycle offset: {}, RoomEnd: {}/{}", getTick(), getStartTick() + getTick(), tick_cycle, getTick() + tick_cycle, getStartTick() + getTick() + tick_cycle);
            }
        }

        if (attackThisTick != null)
        {
            // Find which Guardian performed the attack (would need animation logic to determine this)
            // For now, just dispatch for all Guardians as this needs attack animation detection
            for (BasicTrackedNpc Guardian : guardians.values())
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
            if (coxNpc == CoxNpc.GUARDIAN_1 || coxNpc == CoxNpc.GUARDIAN_2) {
                
                // Only track live Guardian IDs (7569, 7570), ignore dead IDs (7571, 7572)
                if ((npc.getId() == 7569 || npc.getId() == 7570) && !guardians.containsKey(npc.getId())) {
                    // Check if this specific NPC ID is already being tracked
                    BasicTrackedNpc newGuardian = new BasicTrackedNpc(
                        npc,
                        coxNpc,
                        generateRoomId(npc),
                        new Hitpoints(coxNpc.getBaseHitpoints())
                    );
                    guardians.put(npc.getId(), newGuardian);
                    targetedGuardians.put(npc.getId(), false); // Initialize targeting state
                    attackedGuardians.put(npc.getId(), false); // Initialize attacking state
                    log.info("✓ Guardian tracked instance created: id={}, index={}, enum={}, base HP {} (scale={}) - Total Guardians: {}", 
                                npc.getId(), npc.getIndex(), coxNpc, newGuardian.getHitpoints().getBase(), getChallenge().getScale(), guardians.size());
                    return Optional.of(newGuardian);
                } else {
                    log.debug("[Guardian Death ID] Ignoring dead Guardian ID {} (no spawn tracking for dead IDs)", npc.getId());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean onNpcDespawn(NpcDespawned despawned, @Nullable TrackedNpc trackedNpc)
    // TODO: Make sure this is also dependent on hp being 0 otherwise it can die off player screen and cause issues
    {
        NPC npc = despawned.getNpc();
        BasicTrackedNpc removedGuardian = guardians.remove(npc.getId());
        if (removedGuardian != null)
        {
            targetedGuardians.remove(npc.getId()); // Clean up targeting state
            attackedGuardians.remove(npc.getId()); // Clean up attacking state
            log.info("[Guardian] Despawned NPC id={} index={} – removed from tracking. Remaining Guardians: {}", npc.getId(), npc.getIndex(), guardians.size());
            if (guardians.size() == 0) {
                log.info("[Guardian] All Guardians despawned at tick {}/{}", getTick(), getStartTick() + getTick());
                int tick_cycle = (4 - ((getStartTick() + getTick()) % 4)) % 4;
                log.info("[Guardian] 4 tick cycle offset: {}, RoomEnd: {}/{}", tick_cycle, getTick() + tick_cycle, getTick() + tick_cycle + getStartTick());
            }
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
        for (BasicTrackedNpc Guardian : guardians.values())
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
            for (BasicTrackedNpc Guardian : guardians.values())
            {
                if (event.getActor() == Guardian.getNpc())
                {
                    setHealTick(getTick());
                    log.info("[Guardian HP] Heal hitsplat detected on NPC id={} index={} at tick {}", Guardian.getNpc().getId(), Guardian.getNpc().getIndex(), getHealTick());
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
